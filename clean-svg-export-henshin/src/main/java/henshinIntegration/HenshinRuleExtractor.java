package henshinIntegration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Reads the real Henshin XML files and converts one rule into a small Java model.
 * - reads .henshin for rule logic, nodes, edges, attributes, and mappings.
 * - reads .henshin_diagram for Eclipse/GMF node positions.
 * - reads imported .ecore files for containment references.
 * - combines that information into one HenshinRule object.
 */
public class HenshinRuleExtractor {
    /**
     * reads the Henshin files and builds the Java model for one requested rule
     * @param henshinPath path to the .henshin file that contains the rule logic
     * @param diagramPath path to the .henshin_diagram file with the Eclipse layout, may be null
     * @param ruleName name of the rule to extract, for example "createAccount"
     * @return a HenshinRule that combines logic, layout, nodes, edges, and forbidden conditions
     * @throws IOException if one of the XML files cannot be read
     */
    public HenshinRule extract(Path henshinPath, Path diagramPath, String ruleName) throws IOException {
        Document henshinDocument = parseXml(henshinPath);
        Set<String> containmentReferences = extractContainmentReferences(henshinDocument, henshinPath);

        // - this gives us the Eclipse x/y coordinates for the rule frame and Henshin nodes.
        Document diagramDocument = diagramPath == null ? null : parseXml(diagramPath);
        Map<String, HenshinLayout> layouts = diagramDocument == null ? Map.of() : extractLayouts(diagramDocument);

        // - find the specific rule requested by the caller.
        Element ruleElement = findRule(henshinDocument, ruleName)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleName));
        HenshinLayout ruleLayout = diagramDocument == null
                ? HenshinLayout.empty()
                : extractRuleLayout(diagramDocument, ruleElement.getAttribute("xmi:id"));


        Element lhs = firstChildElement(ruleElement, "lhs")
                .orElseThrow(() -> new IllegalArgumentException("Rule has no LHS: " + ruleName));
        Element rhs = firstChildElement(ruleElement, "rhs")
                .orElseThrow(() -> new IllegalArgumentException("Rule has no RHS: " + ruleName));

        // - mappings connect LHS elements to their corresponding RHS elements.
        // - if a LHS node has a mapping to RHS, it is preserved.
        Map<String, String> originToImage = extractMappings(ruleElement);
        Map<String, String> imageToOrigin = reverseMap(originToImage);

        List<HenshinParameter> parameters = extractParameters(ruleElement);
        List<HenshinNode> nodes = new ArrayList<>();
        List<HenshinEdge> edges = new ArrayList<>();
        extractGraph(lhs, rhs, originToImage, layouts, containmentReferences, false, nodes, edges);

        // Multi-rules (for example "delete every Account of this client") are stored inside the rule.
        // Their nodes that are copies of main-rule nodes are not drawn twice; only the extra ones are added.
        List<HenshinCondition> conditions = new ArrayList<>(extractConditions(ruleElement, layouts));
        addMultiRules(ruleElement, id -> imageToOrigin.getOrDefault(id, id),
                nodes, edges, conditions, layouts, containmentReferences);

        // Give every edge its Eclipse route and label positions, so the SVG matches the diagram editor.
        if (diagramDocument != null) {
            EdgeLayouts edgeLayouts = extractEdgeLayouts(diagramDocument);
            edges.replaceAll(edge -> edge.withLayout(edgeLayouts.find(edge)));
        }

        return new HenshinRule(ruleElement.getAttribute("name"), parameters, ruleLayout, nodes, edges, conditions);
    }

    /**
     * reads one LHS/RHS pair (of the main rule or of a multi-rule) and adds its nodes and edges
     * @param lhs the LHS graph element
     * @param rhs the RHS graph element
     * @param originToImage map from LHS origin id to RHS image id
     * @param layouts map from node id to its Eclipse layout
     * @param containmentReferences set of "Class/reference" names that are containment references
     * @param multi true when the graph belongs to a multi-rule
     * @param nodes the node list to add to
     * @param edges the edge list to add to
     */
    private void extractGraph(
            Element lhs,
            Element rhs,
            Map<String, String> originToImage,
            Map<String, HenshinLayout> layouts,
            Set<String> containmentReferences,
            boolean multi,
            List<HenshinNode> nodes,
            List<HenshinEdge> edges) {
        Map<String, String> imageToOrigin = reverseMap(originToImage);
        Set<String> mappedImages = new HashSet<>(originToImage.values());

        // - read raw nodes separately from LHS and RHS.
        // - afterwards we merge them into one visual node list with actions.
        Map<String, RawNode> lhsNodes = extractRawNodes(lhs);
        Map<String, RawNode> rhsNodes = extractRawNodes(rhs);

        nodes.addAll(combineNodes(lhsNodes, rhsNodes, originToImage, mappedImages, layouts, multi));
        edges.addAll(combineEdges(lhs, rhs, originToImage, imageToOrigin, containmentReferences, multi));

        // Negative application conditions are stored inside the LHS as "Not" formulas.
        // Eclipse shows them as blue "forbid" elements in the rule diagram.
        // We add them after the normal LHS/RHS extraction because they are not normal rule creations or deletions.
        addForbiddenConditions(lhs, nodes, edges, layouts, containmentReferences, multi);
    }

    /**
     * adds the nodes, edges, and conditions of all multi-rules inside a rule (also nested multi-rules)
     * @param parentRule the rule or multi-rule element that may contain multiRules children
     * @param parentDisplayId turns an id of the parent rule into the id of the node that is drawn for it
     * @param nodes the node list to add to
     * @param edges the edge list to add to
     * @param conditions the condition list to add to
     * @param layouts map from node id to its Eclipse layout
     * @param containmentReferences set of "Class/reference" names that are containment references
     */
    private void addMultiRules(
            Element parentRule,
            UnaryOperator<String> parentDisplayId,
            List<HenshinNode> nodes,
            List<HenshinEdge> edges,
            List<HenshinCondition> conditions,
            Map<String, HenshinLayout> layouts,
            Set<String> containmentReferences) {
        for (Element multiRule : childElements(parentRule, "multiRules")) {
            Optional<Element> lhs = firstChildElement(multiRule, "lhs");
            Optional<Element> rhs = firstChildElement(multiRule, "rhs");
            if (lhs.isEmpty() || rhs.isEmpty()) {
                continue;
            }

            // multiMappings connect a parent node to its copy inside the multi-rule.
            // Example: the Client of deleteAllAccounts appears again in the multi-rule,
            // but it is the same Client, so we draw it only once.
            Map<String, String> copyToDisplayId = new HashMap<>();
            for (Element mapping : childElements(multiRule, "multiMappings")) {
                copyToDisplayId.put(
                        stripReference(mapping.getAttribute("image")),
                        parentDisplayId.apply(stripReference(mapping.getAttribute("origin"))));
            }

            Map<String, String> originToImage = extractMappings(multiRule);
            List<HenshinNode> multiNodes = new ArrayList<>();
            List<HenshinEdge> multiEdges = new ArrayList<>();
            extractGraph(lhs.get(), rhs.get(), originToImage, layouts, containmentReferences, true, multiNodes, multiEdges);

            for (HenshinNode node : multiNodes) {
                if (!copyToDisplayId.containsKey(node.id())) {
                    nodes.add(node);
                }
            }

            for (HenshinEdge edge : multiEdges) {
                HenshinEdge moved = new HenshinEdge(
                        edge.id(),
                        copyToDisplayId.getOrDefault(edge.sourceNodeId(), edge.sourceNodeId()),
                        copyToDisplayId.getOrDefault(edge.targetNodeId(), edge.targetNodeId()),
                        edge.type(),
                        edge.action(),
                        edge.containment(),
                        true);
                // An edge between two main-rule nodes that the main rule already draws is just a copy.
                boolean alreadyDrawn = edges.stream().anyMatch(existing ->
                        existing.sourceNodeId().equals(moved.sourceNodeId())
                                && existing.targetNodeId().equals(moved.targetNodeId())
                                && existing.type().equals(moved.type())
                                && existing.action() == moved.action());
                if (!alreadyDrawn) {
                    edges.add(moved);
                }
            }

            conditions.addAll(extractConditions(multiRule, layouts));

            Map<String, String> imageToOrigin = reverseMap(originToImage);
            UnaryOperator<String> displayId = id -> {
                String lhsId = imageToOrigin.getOrDefault(id, id);
                return copyToDisplayId.getOrDefault(lhsId, copyToDisplayId.getOrDefault(id, lhsId));
            };
            addMultiRules(multiRule, displayId, nodes, edges, conditions, layouts, containmentReferences);
        }
    }

    /**
     * reads the attribute conditions declared directly on a rule, for example "x>amount"
     * @param ruleElement the rule or multi-rule element
     * @param layouts map from element id to its Eclipse layout
     * @return the list of conditions in document order
     */
    private List<HenshinCondition> extractConditions(Element ruleElement, Map<String, HenshinLayout> layouts) {
        List<HenshinCondition> conditions = new ArrayList<>();
        for (Element condition : childElements(ruleElement, "attributeConditions")) {
            String id = condition.getAttribute("xmi:id");
            conditions.add(new HenshinCondition(
                    id,
                    condition.getAttribute("name"),
                    condition.getAttribute("conditionText"),
                    layouts.getOrDefault(id, HenshinLayout.empty())));
        }
        return conditions;
    }

    /**
     * lists the names of all rules in a .henshin file, in file order
     * @param henshinPath path to the .henshin file
     * @return the rule names, for example [createAccount, transferMoney]
     * @throws IOException if the file cannot be read or is not valid XML
     */
    public List<String> listRules(Path henshinPath) throws IOException {
        Document document = parseXml(henshinPath);
        List<String> names = new ArrayList<>();
        NodeList units = document.getElementsByTagName("units");
        for (int i = 0; i < units.getLength(); i++) {
            Element unit = (Element) units.item(i);
            if ("henshin:Rule".equals(unit.getAttribute("xsi:type"))) {
                names.add(unit.getAttribute("name"));
            }
        }
        return names;
    }

    /**
     * parses an XML file into a DOM document with DOCTYPE declarations disabled for safety
     * @param path path of the XML file to parse
     * @return the parsed DOM document
     * @throws IOException if the file cannot be read or parsed
     */
    private Document parseXml(Path path) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setNamespaceAware(false);
            return factory.newDocumentBuilder().parse(path.toFile());
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Could not read XML file: " + path, e);
        }
    }

    /**
     * searches the document for the units element that holds the requested rule
     * @param document the parsed .henshin document
     * @param ruleName name of the rule to find, or null/blank to take the first rule
     * @return the matching rule element, or an empty optional if none was found
     */
    private Optional<Element> findRule(Document document, String ruleName) {
        NodeList units = document.getElementsByTagName("units");
        for (int i = 0; i < units.getLength(); i++) {
            Element unit = (Element) units.item(i);
            if ("henshin:Rule".equals(unit.getAttribute("xsi:type"))
                    && (ruleName == null || ruleName.isBlank() || ruleName.equals(unit.getAttribute("name")))) {
                return Optional.of(unit);
            }
        }
        return Optional.empty();
    }

    /**
     * reads the parameters declared by a rule, for example "client" or "accountId"
     * @param ruleElement the rule element to read the parameters from
     * @return the list of parameters with name, kind, and type
     */
    private List<HenshinParameter> extractParameters(Element ruleElement) {
        // Only direct children: a multi-rule inside the rule repeats parameters like "client",
        // and those must not show up twice in the title.
        List<HenshinParameter> parameters = new ArrayList<>();
        for (Element parameter : childElements(ruleElement, "parameters")) {
            parameters.add(new HenshinParameter(
                    parameter.getAttribute("name"),
                    parameter.getAttribute("kind"),
                    simpleHrefName(firstChildElement(parameter, "type")
                            .map(type -> type.getAttribute("href"))
                            .orElse(""))));
        }
        return parameters;
    }

    /**
     * reads the mappings declared directly on an element (a rule, multi-rule, or NAC)
     * - only direct children are read, so NAC and multi-rule mappings do not leak into the rule mappings.
     * @param ruleElement the element that contains the mappings
     * @return a map from origin id to image id
     */
    private Map<String, String> extractMappings(Element ruleElement) {
        Map<String, String> mappings = new HashMap<>();
        for (Element mapping : childElements(ruleElement, "mappings")) {
            mappings.put(stripReference(mapping.getAttribute("origin")), stripReference(mapping.getAttribute("image")));
        }
        return mappings;
    }

    /**
     * builds the inverse of a map so values can be looked up by their former keys
     * @param source the map to reverse
     * @return a new map where each value points back to its key
     */
    private Map<String, String> reverseMap(Map<String, String> source) {
        Map<String, String> reversed = new HashMap<>();
        for (Map.Entry<String, String> entry : source.entrySet()) {
            reversed.put(entry.getValue(), entry.getKey());
        }
        return reversed;
    }

    /**
     * follows the .ecore imports of a Henshin file and collects all containment reference names
     * @param henshinDocument the parsed .henshin document
     * @param henshinPath path of the .henshin file, used to resolve relative .ecore paths
     * @return the set of reference names that are containment references
     * @throws IOException if an imported .ecore file cannot be read
     */
    private Set<String> extractContainmentReferences(Document henshinDocument, Path henshinPath) throws IOException {
        Set<String> containmentReferences = new HashSet<>();
        NodeList imports = henshinDocument.getElementsByTagName("imports");

        for (int i = 0; i < imports.getLength(); i++) {
            Element importElement = (Element) imports.item(i);
            String href = importElement.getAttribute("href");
            if (href == null || href.isBlank() || !href.contains(".ecore")) {
                continue;
            }

            // Henshin imports usually look like "bank.ecore#/".
            // We resolve that relative to the .henshin file location.
            String ecoreFileName = href.split("#", 2)[0];
            Path ecorePath = henshinPath.getParent().resolve(ecoreFileName).normalize();
            if (ecorePath.toFile().isFile()) {
                containmentReferences.addAll(extractContainmentReferencesFromEcore(parseXml(ecorePath)));
            }
        }

        return containmentReferences;
    }

    /**
     * reads one .ecore document and collects all containment references as "Class/reference"
     * - the class name is needed because two classes can have references with the same name,
     *   for example Bank/clients (containment) and Manager/clients (normal reference).
     * @param ecoreDocument the parsed .ecore document
     * @return the set of "Class/reference" names marked as containment="true"
     */
    private Set<String> extractContainmentReferencesFromEcore(Document ecoreDocument) {
        Set<String> containmentReferences = new HashSet<>();
        NodeList classifiers = ecoreDocument.getElementsByTagName("eClassifiers");

        for (int i = 0; i < classifiers.getLength(); i++) {
            Element classifier = (Element) classifiers.item(i);
            String className = classifier.getAttribute("name");
            for (Element feature : childElements(classifier, "eStructuralFeatures")) {
                // In Ecore, containment="true" means a composition/ownership reference.
                // We use this later to draw diamond-style arrows.
                if ("ecore:EReference".equals(feature.getAttribute("xsi:type"))
                        && "true".equals(feature.getAttribute("containment"))) {
                    containmentReferences.add(className + "/" + feature.getAttribute("name"));
                }
            }
        }

        return containmentReferences;
    }

    /**
     * reads the direct child nodes of a graph element into raw nodes keyed by their id
     * @param graphElement the graph element (lhs, rhs, or a NAC conclusion) to read nodes from
     * @return a map from node id to raw node, keeping the document order
     */
    private Map<String, RawNode> extractRawNodes(Element graphElement) {
        Map<String, RawNode> nodes = new LinkedHashMap<>();
        NodeList nodeElements = graphElement.getElementsByTagName("nodes");
        for (int i = 0; i < nodeElements.getLength(); i++) {
            Element node = (Element) nodeElements.item(i);
            if (node.getParentNode() != graphElement) {
                continue;
            }
            String id = node.getAttribute("xmi:id");
            nodes.put(id, new RawNode(
                    id,
                    node.getAttribute("name"),
                    simpleHrefName(firstChildElement(node, "type").map(type -> type.getAttribute("href")).orElse("")),
                    extractAttributes(node)));
        }
        return nodes;
    }

    /**
     * reads the attribute rows that belong directly to one node
     * @param nodeElement the node element to read the attributes from
     * @return the list of attributes with name and value
     */
    private List<HenshinAttribute> extractAttributes(Element nodeElement) {
        List<HenshinAttribute> attributes = new ArrayList<>();
        NodeList attributeElements = nodeElement.getElementsByTagName("attributes");
        for (int i = 0; i < attributeElements.getLength(); i++) {
            Element attribute = (Element) attributeElements.item(i);
            if (attribute.getParentNode() != nodeElement) {
                continue;
            }
            String name = firstChildElement(attribute, "type")
                    .map(type -> simpleHrefName(type.getAttribute("href")))
                    .orElse("");
            attributes.add(new HenshinAttribute(name, attribute.getAttribute("value")));
        }
        return attributes;
    }

    /**
     * merges LHS and RHS raw nodes into one visual node list and assigns preserve/delete/create actions
     * @param lhsNodes raw nodes read from the LHS graph
     * @param rhsNodes raw nodes read from the RHS graph
     * @param originToImage map from LHS origin id to RHS image id
     * @param mappedImages set of RHS ids that are mapped from the LHS
     * @param layouts map from node id to its Eclipse layout
     * @param multi true when the nodes belong to a multi-rule
     * @return the combined list of visual nodes with their actions
     */
    private List<HenshinNode> combineNodes(
            Map<String, RawNode> lhsNodes,
            Map<String, RawNode> rhsNodes,
            Map<String, String> originToImage,
            Set<String> mappedImages,
            Map<String, HenshinLayout> layouts,
            boolean multi) {
        List<HenshinNode> nodes = new ArrayList<>();

        // - if mapped to RHS: preserve.
        // - if not mapped to RHS: delete.
        for (RawNode lhsNode : lhsNodes.values()) {
            RawNode rhsNode = rhsNodes.get(originToImage.get(lhsNode.id()));
            if (rhsNode == null) {
                nodes.add(toNode(lhsNode, lhsNode.attributes(), HenshinAction.DELETE, layouts, multi));
            } else {
                // A preserved node can change attribute values, for example credit=x becomes credit=x-amount.
                List<HenshinAttribute> attributes = mergeAttributes(lhsNode.attributes(), rhsNode.attributes());
                nodes.add(toNode(lhsNode, attributes, HenshinAction.PRESERVE, layouts, multi));
            }
        }

        // - if they were not produced by a LHS mapping, they are new elements: Create.
        for (RawNode rhsNode : rhsNodes.values()) {
            if (!mappedImages.contains(rhsNode.id())) {
                nodes.add(toNode(rhsNode, rhsNode.attributes(), HenshinAction.CREATE, layouts, multi));
            }
        }

        return nodes;
    }

    /**
     * combines the LHS and RHS attributes of a preserved node into one list of attribute rows
     * - same value on both sides: shown once, for example "id=fromId".
     * - different values: shown as a change, for example "credit=x->x-amount".
     * - only in the RHS: the rule sets this value, shown as "name=value".
     * @param lhsAttributes attributes of the LHS node
     * @param rhsAttributes attributes of the mapped RHS node
     * @return the merged attribute rows in LHS order, followed by RHS-only attributes
     */
    private List<HenshinAttribute> mergeAttributes(List<HenshinAttribute> lhsAttributes, List<HenshinAttribute> rhsAttributes) {
        Map<String, String> rhsValues = new LinkedHashMap<>();
        for (HenshinAttribute attribute : rhsAttributes) {
            rhsValues.put(attribute.name(), attribute.value());
        }

        List<HenshinAttribute> merged = new ArrayList<>();
        for (HenshinAttribute attribute : lhsAttributes) {
            String newValue = rhsValues.remove(attribute.name());
            if (newValue != null && !newValue.equals(attribute.value())) {
                merged.add(new HenshinAttribute(attribute.name(), attribute.value(), newValue));
            } else {
                merged.add(attribute);
            }
        }
        for (Map.Entry<String, String> remaining : rhsValues.entrySet()) {
            merged.add(new HenshinAttribute(remaining.getKey(), remaining.getValue()));
        }
        return merged;
    }

    /**
     * builds a final HenshinNode from a raw node, its action, and its layout
     * @param rawNode the raw node read from the XML
     * @param attributes the attribute rows to show for this node
     * @param action the action assigned to this node
     * @param layouts map from node id to its Eclipse layout
     * @param multi true when the node belongs to a multi-rule
     * @return the assembled HenshinNode, using an empty layout if none is known
     */
    private HenshinNode toNode(RawNode rawNode, List<HenshinAttribute> attributes, HenshinAction action,
                               Map<String, HenshinLayout> layouts, boolean multi) {
        return new HenshinNode(
                rawNode.id(),
                rawNode.name(),
                rawNode.type(),
                action,
                attributes,
                layouts.getOrDefault(rawNode.id(), HenshinLayout.empty()),
                multi);
    }

    /**
     * merges LHS and RHS edges into one visual edge list and assigns preserve/delete/create actions
     * @param lhs the LHS graph element
     * @param rhs the RHS graph element
     * @param originToImage map from LHS origin id to RHS image id
     * @param imageToOrigin map from RHS image id back to LHS origin id
     * @param containmentReferences set of "Class/reference" names that are containment references
     * @param multi true when the edges belong to a multi-rule
     * @return the combined list of visual edges with their actions
     */
    private List<HenshinEdge> combineEdges(
            Element lhs,
            Element rhs,
            Map<String, String> originToImage,
            Map<String, String> imageToOrigin,
            Set<String> containmentReferences,
            boolean multi) {
        List<RawEdge> lhsEdges = extractRawEdges(lhs);
        List<RawEdge> rhsEdges = extractRawEdges(rhs);
        Map<EdgeSignature, RawEdge> rhsBySignature = new HashMap<>();

        for (RawEdge rhsEdge : rhsEdges) {
            rhsBySignature.put(new EdgeSignature(rhsEdge.sourceId(), rhsEdge.targetId(), rhsEdge.type()), rhsEdge);
        }

        Set<String> preservedRhsEdgeIds = new HashSet<>();
        List<HenshinEdge> edges = new ArrayList<>();

        // Edge action logic mirrors the node action logic:
        for (RawEdge lhsEdge : lhsEdges) {
            EdgeSignature mappedSignature = new EdgeSignature(
                    originToImage.getOrDefault(lhsEdge.sourceId(), lhsEdge.sourceId()),
                    originToImage.getOrDefault(lhsEdge.targetId(), lhsEdge.targetId()),
                    lhsEdge.type());
            RawEdge matchingRhsEdge = rhsBySignature.get(mappedSignature);

            if (matchingRhsEdge == null) {
                edges.add(toEdge(lhsEdge, HenshinAction.DELETE, containmentReferences, multi));
            } else {
                preservedRhsEdgeIds.add(matchingRhsEdge.id());
                edges.add(toEdge(lhsEdge, HenshinAction.PRESERVE, containmentReferences, multi));
            }
        }

        // RHS edges that were not matched from LHS are created by the rule.
        for (RawEdge rhsEdge : rhsEdges) {
            if (!preservedRhsEdgeIds.contains(rhsEdge.id())) {
                edges.add(toEdge(canonicalizeRhsEdge(rhsEdge, imageToOrigin), HenshinAction.CREATE, containmentReferences, multi));
            }
        }

        return edges;
    }

    /**
     * finds the negative application conditions of a rule and turns them into forbid nodes and edges
     * @param lhs the LHS graph element that contains the NAC formulas
     * @param nodes the node list to add forbidden nodes to
     * @param edges the edge list to add forbidden edges to
     * @param layouts map from node id to its Eclipse layout
     * @param containmentReferences set of "Class/reference" names that are containment references
     * @param multi true when the NAC belongs to a multi-rule
     */
    private void addForbiddenConditions(
            Element lhs,
            List<HenshinNode> nodes,
            List<HenshinEdge> edges,
            Map<String, HenshinLayout> layouts,
            Set<String> containmentReferences,
            boolean multi) {
        NodeList formulas = lhs.getElementsByTagName("formula");

        for (int i = 0; i < formulas.getLength(); i++) {
            Element formula = (Element) formulas.item(i);

            // Henshin stores NACs as henshin:Not formulas.
            // In the bank example this is the "there must not already be an account with this id" part.
            if (!"henshin:Not".equals(formula.getAttribute("xsi:type"))) {
                continue;
            }

            // The forbidden pattern is inside child/conclusion, and its mappings are on child.
            // We convert that conclusion graph into FORBID nodes and edges for the SVG.
            firstChildElement(formula, "child").ifPresent(child ->
                    firstChildElement(child, "conclusion").ifPresent(conclusion ->
                            addForbiddenConclusion(child, conclusion, nodes, edges, layouts, containmentReferences, multi)));
        }
    }

    /**
     * adds the nodes and edges of one NAC conclusion as forbid elements, skipping nodes shared with the LHS
     * @param nestedCondition the henshin:NestedCondition element that owns the conclusion and its mappings
     * @param conclusion the conclusion graph that holds the forbidden pattern
     * @param nodes the node list to add forbidden nodes to
     * @param edges the edge list to add forbidden edges to
     * @param layouts map from node id to its Eclipse layout
     * @param containmentReferences set of "Class/reference" names that are containment references
     * @param multi true when the NAC belongs to a multi-rule
     */
    private void addForbiddenConclusion(
            Element nestedCondition,
            Element conclusion,
            List<HenshinNode> nodes,
            List<HenshinEdge> edges,
            Map<String, HenshinLayout> layouts,
            Set<String> containmentReferences,
            boolean multi) {
        // NAC mappings connect existing LHS nodes to nodes inside the forbidden pattern.
        // Example: the forbidden Bank is actually the same Bank as in the normal LHS,
        // so we do not draw a second Bank box. Only the extra forbidden Account box is drawn.
        Map<String, String> forbiddenImageToOrigin = reverseMap(extractMappings(nestedCondition));
        Map<String, RawNode> forbiddenNodes = extractRawNodes(conclusion);

        for (RawNode forbiddenNode : forbiddenNodes.values()) {
            // Only nodes without a mapping are really extra forbidden nodes.
            if (!forbiddenImageToOrigin.containsKey(forbiddenNode.id())) {
                nodes.add(toNode(forbiddenNode, forbiddenNode.attributes(), HenshinAction.FORBID, layouts, multi));
            }
        }

        for (RawEdge forbiddenEdge : extractRawEdges(conclusion)) {
            // If an edge starts at a mapped NAC node, connect it back to the already drawn LHS node.
            // That is why the blue "forbid accounts" edge starts at Bank in the final SVG.
            RawEdge canonicalEdge = canonicalizeRhsEdge(forbiddenEdge, forbiddenImageToOrigin);
            edges.add(toEdge(canonicalEdge, HenshinAction.FORBID, containmentReferences, multi));
        }
    }

    /**
     * rewrites an edge so its source and target ids point to LHS origin nodes instead of image nodes
     * @param rhsEdge the edge whose endpoints should be translated back to origin ids
     * @param imageToOrigin map from image id to origin id
     * @return a new raw edge with translated source and target ids
     */
    private RawEdge canonicalizeRhsEdge(RawEdge rhsEdge, Map<String, String> imageToOrigin) {
        return new RawEdge(
                rhsEdge.id(),
                imageToOrigin.getOrDefault(rhsEdge.sourceId(), rhsEdge.sourceId()),
                imageToOrigin.getOrDefault(rhsEdge.targetId(), rhsEdge.targetId()),
                rhsEdge.type(),
                rhsEdge.reference());
    }

    /**
     * reads the direct child edges of a graph element into raw edges
     * @param graphElement the graph element (lhs, rhs, or a NAC conclusion) to read edges from
     * @return the list of raw edges with source id, target id, and type
     */
    private List<RawEdge> extractRawEdges(Element graphElement) {
        List<RawEdge> edges = new ArrayList<>();
        NodeList edgeElements = graphElement.getElementsByTagName("edges");
        for (int i = 0; i < edgeElements.getLength(); i++) {
            Element edge = (Element) edgeElements.item(i);
            if (edge.getParentNode() != graphElement) {
                continue;
            }
            String href = firstChildElement(edge, "type").map(type -> type.getAttribute("href")).orElse("");
            edges.add(new RawEdge(
                    edge.getAttribute("xmi:id"),
                    stripReference(edge.getAttribute("source")),
                    stripReference(edge.getAttribute("target")),
                    simpleHrefName(href),
                    qualifiedReferenceName(href)));
        }
        return edges;
    }

    /**
     * builds a final HenshinEdge from a raw edge, its action, and the containment information
     * @param rawEdge the raw edge read from the XML
     * @param action the action assigned to this edge
     * @param containmentReferences set of "Class/reference" names that are containment references
     * @param multi true when the edge belongs to a multi-rule
     * @return the assembled HenshinEdge with its containment flag set
     */
    private HenshinEdge toEdge(RawEdge rawEdge, HenshinAction action, Set<String> containmentReferences, boolean multi) {
        return new HenshinEdge(
                rawEdge.id(),
                rawEdge.sourceId(),
                rawEdge.targetId(),
                rawEdge.type(),
                action,
                containmentReferences.contains(rawEdge.reference()),
                multi);
    }

    /**
     * extracts "Class/reference" from an Ecore reference href
     * @param href the href value, for example "bank.ecore#//Bank/clients"
     * @return the class and reference name, for example "Bank/clients", or "" if the href is blank
     */
    private String qualifiedReferenceName(String href) {
        if (href == null || href.isBlank()) {
            return "";
        }
        String path = href.substring(href.indexOf('#') + 1);
        String[] parts = path.split("/");
        if (parts.length < 2) {
            return path;
        }
        return parts[parts.length - 2] + "/" + parts[parts.length - 1];
    }

    /**
     * reads the x/y/width/height of every node from the .henshin_diagram file
     * @param diagramDocument the parsed .henshin_diagram document
     * @return a map from semantic node id to its Eclipse layout
     */
    private Map<String, HenshinLayout> extractLayouts(Document diagramDocument) {
        Map<String, HenshinLayout> layouts = new HashMap<>();
        NodeList elements = diagramDocument.getElementsByTagName("element");

        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);
            // Nodes and attribute conditions both get an x/y position inside the rule frame.
            String elementType = element.getAttribute("xmi:type");
            if (!"henshin:Node".equals(elementType) && !"henshin:AttributeCondition".equals(elementType)) {
                continue;
            }

            // A .henshin_diagram entry points back to a semantic node in .henshin.
            // Example: href="bank.henshin#_someNodeId".
            // We keep only the id after "#", then store its x/y bounds.
            Element shape = (Element) element.getParentNode();
            String semanticId = semanticIdFromHref(element.getAttribute("href"));
            firstChildElement(shape, "layoutConstraint")
                    .ifPresent(bounds -> layouts.put(semanticId, new HenshinLayout(
                            parseInt(bounds.getAttribute("x")),
                            parseInt(bounds.getAttribute("y")),
                            parseInt(bounds.getAttribute("width")),
                            parseInt(bounds.getAttribute("height")))));
        }

        return layouts;
    }

    /**
     * reads the x/y/width/height of one rule frame from the .henshin_diagram file
     * @param diagramDocument the parsed .henshin_diagram document
     * @param ruleId the xmi id of the rule whose frame layout is requested
     * @return the rule frame layout, or an empty layout if it is not present
     */
    private HenshinLayout extractRuleLayout(Document diagramDocument, String ruleId) {
        NodeList elements = diagramDocument.getElementsByTagName("element");

        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);
            if (!"henshin:Rule".equals(element.getAttribute("xmi:type"))) {
                continue;
            }
            if (!ruleId.equals(semanticIdFromHref(element.getAttribute("href")))) {
                continue;
            }

            Element shape = (Element) element.getParentNode();
            return firstChildElement(shape, "layoutConstraint")
                    .map(bounds -> new HenshinLayout(
                            parseInt(bounds.getAttribute("x")),
                            parseInt(bounds.getAttribute("y")),
                            parseInt(bounds.getAttribute("width")),
                            parseInt(bounds.getAttribute("height"))))
                    .orElse(HenshinLayout.empty());
        }

        return HenshinLayout.empty();
    }

    /**
     * reads the route and label positions of every edge in the .henshin_diagram file
     * - each notation:Connector points to a henshin:Edge and to the two node shapes it connects.
     * - type 6001 is the relation-name label, type 6002 the «action» label.
     * @param diagramDocument the parsed .henshin_diagram document
     * @return the edge layouts, findable by edge id or by the two node ids
     */
    private EdgeLayouts extractEdgeLayouts(Document diagramDocument) {
        // Shapes are referenced by their notation id; we need the semantic node id behind each one.
        Map<String, String> shapeToNode = new HashMap<>();
        NodeList elements = diagramDocument.getElementsByTagName("element");
        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);
            if ("henshin:Node".equals(element.getAttribute("xmi:type"))) {
                Element shape = (Element) element.getParentNode();
                shapeToNode.put(shape.getAttribute("xmi:id"), semanticIdFromHref(element.getAttribute("href")));
            }
        }

        Map<String, HenshinEdgeLayout> byId = new HashMap<>();
        Map<String, HenshinEdgeLayout> byNodes = new HashMap<>();
        NodeList connectors = diagramDocument.getElementsByTagName("edges");
        for (int i = 0; i < connectors.getLength(); i++) {
            Element connector = (Element) connectors.item(i);
            Optional<Element> semantic = firstChildElement(connector, "element");
            if (semantic.isEmpty()) {
                continue;
            }

            HenshinEdgeLayout.Offset nameOffset = null;
            HenshinEdgeLayout.Offset actionOffset = null;
            for (Element label : childElements(connector, "children")) {
                HenshinEdgeLayout.Offset offset = firstChildElement(label, "layoutConstraint")
                        .map(location -> new HenshinEdgeLayout.Offset(
                                parseInt(location.getAttribute("x")), parseInt(location.getAttribute("y"))))
                        .orElse(null);
                if ("6001".equals(label.getAttribute("type"))) {
                    nameOffset = offset;
                } else if ("6002".equals(label.getAttribute("type"))) {
                    actionOffset = offset;
                }
            }

            HenshinEdgeLayout layout = new HenshinEdgeLayout(
                    firstChildElement(connector, "bendpoints").map(b -> parseBendpoints(b.getAttribute("points"))).orElse(List.of()),
                    firstChildElement(connector, "sourceAnchor").map(a -> parseAnchor(a.getAttribute("id"))).orElse(null),
                    firstChildElement(connector, "targetAnchor").map(a -> parseAnchor(a.getAttribute("id"))).orElse(null),
                    nameOffset,
                    actionOffset,
                    true);

            byId.put(semanticIdFromHref(semantic.get().getAttribute("href")), layout);
            String source = shapeToNode.get(connector.getAttribute("source"));
            String target = shapeToNode.get(connector.getAttribute("target"));
            if (source != null && target != null) {
                byNodes.putIfAbsent(source + "->" + target, layout);
            }
        }
        return new EdgeLayouts(byId, byNodes);
    }

    /**
     * parses GMF relative bendpoints such as "[35, 20, -127, -75]$[120, 71, -42, -24]"
     * @param points the points attribute value
     * @return the bendpoints in order, or an empty list if the value cannot be read
     */
    private List<HenshinEdgeLayout.Bendpoint> parseBendpoints(String points) {
        List<HenshinEdgeLayout.Bendpoint> result = new ArrayList<>();
        if (points == null || points.isBlank()) {
            return result;
        }
        for (String point : points.split("\\$")) {
            String[] numbers = point.replace("[", "").replace("]", "").split(",");
            if (numbers.length != 4) {
                return List.of();
            }
            try {
                result.add(new HenshinEdgeLayout.Bendpoint(
                        Integer.parseInt(numbers[0].trim()), Integer.parseInt(numbers[1].trim()),
                        Integer.parseInt(numbers[2].trim()), Integer.parseInt(numbers[3].trim())));
            } catch (NumberFormatException e) {
                return List.of();
            }
        }
        return result;
    }

    /**
     * parses a GMF identity anchor id such as "(0.43,0.81)"
     * @param id the anchor id value
     * @return the anchor, or null when the value cannot be read (Eclipse then uses the box center)
     */
    private HenshinEdgeLayout.Anchor parseAnchor(String id) {
        if (id == null) {
            return null;
        }
        String[] numbers = id.replace("(", "").replace(")", "").split(",");
        if (numbers.length != 2) {
            return null;
        }
        try {
            return new HenshinEdgeLayout.Anchor(Double.parseDouble(numbers[0].trim()), Double.parseDouble(numbers[1].trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * returns the first direct child element of a parent that has the given tag name
     * @param parent the element whose children are searched
     * @param tagName the tag name to look for
     * @return the first matching child element, or an empty optional if none exists
     */
    private Optional<Element> firstChildElement(Element parent, String tagName) {
        return childElements(parent, tagName).stream().findFirst();
    }

    /**
     * returns all direct child elements of a parent that have the given tag name
     * - unlike getElementsByTagName, this does not look into deeper levels.
     * @param parent the element whose children are searched
     * @param tagName the tag name to look for
     * @return the matching child elements in document order
     */
    private List<Element> childElements(Element parent, String tagName) {
        List<Element> result = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element child && tagName.equals(child.getTagName())) {
                result.add(child);
            }
        }
        return result;
    }

    /**
     * extracts the readable name part after the last "/" or "#" of an href
     * @param href the href value to shorten, for example "bank.ecore#//Account"
     * @return the simple name, or an empty string if the href is null or blank
     */
    private String simpleHrefName(String href) {
        if (href == null || href.isBlank()) {
            return "";
        }
        int index = Math.max(href.lastIndexOf('/'), href.lastIndexOf('#'));
        return href.substring(index + 1);
    }

    /**
     * extracts the semantic id after the "#" of a diagram href that points into the .henshin file
     * @param href the href value, for example "bank.henshin#_someNodeId"
     * @return the id after the "#", or the whole href if no "#" is present
     */
    private String semanticIdFromHref(String href) {
        int index = href.lastIndexOf('#');
        return index >= 0 ? href.substring(index + 1) : href;
    }

    /**
     * removes the leading "#" from an XMI reference so it becomes a plain id
     * @param value the reference value to clean, may be null
     * @return the value without "#", or an empty string if the value is null
     */
    private String stripReference(String value) {
        return value == null ? "" : value.replace("#", "");
    }

    /**
     * parses an integer attribute, treating a missing or blank value as zero
     * @param value the string value to parse
     * @return the parsed integer, or 0 if the value is null or blank
     */
    private int parseInt(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        return Integer.parseInt(value);
    }

    /**
     * Temporary node data read straight from the XML before LHS/RHS merging.
     */
    private record RawNode(String id, String name, String type, List<HenshinAttribute> attributes) {
    }

    /**
     * Temporary edge data read straight from the XML before LHS/RHS merging.
     */
    private record RawEdge(String id, String sourceId, String targetId, String type, String reference) {
    }

    /**
     * The edge layouts of a diagram, found by edge id first and by the two node ids as a fallback.
     */
    private record EdgeLayouts(Map<String, HenshinEdgeLayout> byId, Map<String, HenshinEdgeLayout> byNodes) {
        HenshinEdgeLayout find(HenshinEdge edge) {
            HenshinEdgeLayout layout = byId.get(edge.id());
            if (layout == null) {
                layout = byNodes.get(edge.sourceNodeId() + "->" + edge.targetNodeId());
            }
            return layout == null ? HenshinEdgeLayout.empty() : layout;
        }
    }

    /**
     * Source/target/type key used to match an LHS edge to its RHS counterpart.
     */
    private record EdgeSignature(String sourceId, String targetId, String type) {
    }
}
