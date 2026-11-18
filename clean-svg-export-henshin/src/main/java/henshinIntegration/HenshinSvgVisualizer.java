package henshinIntegration;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.InputSource;

/**
 * Creates an SVG of one Henshin rule that looks like the Henshin diagram editor in Eclipse.
 * - same font (Tahoma 9px), colors, box sizes, and rule frame as Eclipse.
 * - node and condition positions come from the .henshin_diagram file.
 * - edges follow the Eclipse bendpoints and anchors, and their labels use the Eclipse offsets.
 * - containment edges get a filled diamond at the source, every edge gets an open arrow at the target.
 */
public class HenshinSvgVisualizer {
    /** Space around the rule frame. */
    private static final int MARGIN = 10;
    /** Height of the rule title strip. */
    private static final int HEADER_HEIGHT = 22;
    /** Eclipse places nodes relative to this point inside the rule frame. */
    private static final int CONTENT_X = 5;
    private static final int CONTENT_Y = 29;
    /** Node box layout, measured from the Eclipse editor at 100% zoom. */
    private static final int NODE_HEADER_HEIGHT = 23;
    private static final int ATTRIBUTE_ROW_HEIGHT = 16;
    private static final int ATTRIBUTE_TEXT_X = 23;
    private static final int CONDITION_HEIGHT = 38;
    /** Shift of the shadow box that Eclipse draws behind multi-rule nodes. */
    private static final int MULTI_SHADOW = 5;

    private static final String FONT_FAMILY = "Tahoma, Verdana, 'DejaVu Sans', Arial, sans-serif";

    /**
     * renders a rule to SVG and writes the result into a file
     * @param rule the extracted rule to visualize
     * @param outputPath path of the .svg file to write
     * @throws IOException if the file cannot be written
     */
    public void save(HenshinRule rule, Path outputPath) throws IOException {
        String svg = toSvg(rule);
        // Never write a broken file: check that the generated text is well-formed XML first.
        validateXml(svg);
        Files.writeString(outputPath, svg);
    }

    /**
     * checks that the generated SVG text is well-formed XML
     * @param svg the SVG document text
     * @throws IllegalStateException if the text cannot be parsed as XML
     */
    void validateXml(String svg) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.newDocumentBuilder().parse(new InputSource(new StringReader(svg)));
        } catch (Exception e) {
            throw new IllegalStateException("Generated SVG is not valid XML: " + e.getMessage(), e);
        }
    }

    /**
     * renders the whole rule diagram into one SVG document string
     * @param rule the extracted rule to visualize
     * @return the complete SVG document as text
     */
    public String toSvg(HenshinRule rule) {
        // Position everything first, so the frame can grow to fit every box.
        Map<String, Box> nodeBoxes = positionNodes(rule);
        Map<String, Box> conditionBoxes = positionConditions(rule, nodeBoxes);
        Box frame = calculateFrame(rule, nodeBoxes, conditionBoxes);
        int width = (int) Math.ceil(frame.x() + frame.width() + MARGIN);
        int height = (int) Math.ceil(frame.y() + frame.height() + MARGIN);

        StringBuilder svg = new StringBuilder();
        svg.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" viewBox="0 0 %d %d">
                <rect width="100%%" height="100%%" fill="#ffffff"/>
                <g font-family="%s" font-size="9" fill="#000000">
                """.formatted(width, height, width, height, FONT_FAMILY));

        appendFrame(svg, rule, frame);
        appendNodes(svg, rule, nodeBoxes);
        appendConditions(svg, rule, conditionBoxes);
        // Eclipse draws connections on a layer above the node boxes.
        appendEdges(svg, rule, nodeBoxes);

        svg.append("</g>\n</svg>\n");
        return svg.toString();
    }

    /**
     * appends the rule frame: white rounded box, title strip with the rule icon, and the rule title
     * @param svg the builder that collects the SVG output
     * @param rule the rule whose title is drawn
     * @param frame the position and size of the frame
     */
    private void appendFrame(StringBuilder svg, HenshinRule rule, Box frame) {
        double x = frame.x();
        double y = frame.y();
        svg.append("""
                <rect x="%s" y="%s" width="%s" height="%s" rx="4" fill="#ffffff" stroke="#000000" stroke-width="1.2"/>
                <line x1="%s" y1="%s" x2="%s" y2="%s" stroke="#000000" stroke-width="1"/>
                <path d="M %s %s h 11 M %s %s h 11 M %s %s l 6.5 6.5 l -6.5 6.5" fill="none" stroke="#000000" stroke-width="1.3"/>
                <text x="%s" y="%s" font-style="italic">%s</text>
                """.formatted(
                n(x), n(y), n(frame.width()), n(frame.height()),
                n(x), n(y + HEADER_HEIGHT), n(x + frame.width()), n(y + HEADER_HEIGHT),
                n(x + 3), n(y + 9), n(x + 3), n(y + 14), n(x + 10), n(y + 5),
                n(x + 21), n(y + 15), escape(ruleTitle(rule))));
    }

    /**
     * appends one box per node: «action» row, name row, separator, and one row per attribute
     * - multi-rule nodes get a second box behind them, like the stacked look in Eclipse.
     * @param svg the builder that collects the SVG output
     * @param rule the rule whose nodes are drawn
     * @param nodeBoxes map from node id to its box
     */
    private void appendNodes(StringBuilder svg, HenshinRule rule, Map<String, Box> nodeBoxes) {
        for (HenshinNode node : rule.nodes()) {
            Box box = nodeBoxes.get(node.id());
            Style style = Style.of(node.action());
            svg.append("<g>\n");
            if (node.multi()) {
                svg.append("""
                        <rect x="%s" y="%s" width="%s" height="%s" fill="%s" stroke="%s" stroke-opacity="0.6"/>
                        """.formatted(n(box.x() + MULTI_SHADOW), n(box.y() + MULTI_SHADOW),
                        n(box.width()), n(box.height()), style.fill(), style.line()));
            }
            svg.append("""
                    <rect x="%s" y="%s" width="%s" height="%s" fill="%s" stroke="%s"/>
                    <text x="%s" y="%s" fill="%s">%s</text>
                    <text x="%s" y="%s">%s</text>
                    <line x1="%s" y1="%s" x2="%s" y2="%s" stroke="%s"/>
                    """.formatted(
                    n(box.x()), n(box.y()), n(box.width()), n(box.height()), style.fill(), style.line(),
                    n(box.x() + 2), n(box.y() + 9), style.text(), escape(actionLabel(node.action(), node.multi())),
                    n(box.x() + 2), n(box.y() + 20), escape(nodeLabel(node)),
                    n(box.x()), n(box.y() + NODE_HEADER_HEIGHT), n(box.x() + box.width()), n(box.y() + NODE_HEADER_HEIGHT),
                    style.line()));

            double baseline = box.y() + NODE_HEADER_HEIGHT + 13;
            for (HenshinAttribute attribute : sortedAttributes(node)) {
                svg.append("""
                        <rect x="%s" y="%s" width="8" height="5" rx="1" fill="#ffffff" stroke="#6e6e6e" stroke-width="0.8"/>
                        <text x="%s" y="%s">%s</text>
                        """.formatted(
                        n(box.x() + 9), n(baseline - 6),
                        n(box.x() + ATTRIBUTE_TEXT_X), n(baseline), escape(attributeLabel(attribute))));
                baseline += ATTRIBUTE_ROW_HEIGHT;
            }
            svg.append("</g>\n");
        }
    }

    /**
     * appends one yellow note per attribute condition, with a "?" icon, the condition name, and its expression
     * @param svg the builder that collects the SVG output
     * @param rule the rule whose conditions are drawn
     * @param conditionBoxes map from condition id to its box
     */
    private void appendConditions(StringBuilder svg, HenshinRule rule, Map<String, Box> conditionBoxes) {
        for (HenshinCondition condition : rule.conditions()) {
            Box box = conditionBoxes.get(condition.id());
            svg.append("""
                    <g>
                    <rect x="%s" y="%s" width="%s" height="%s" rx="3" fill="#fff8c8" stroke="#e8c050"/>
                    <text x="%s" y="%s" font-size="12" fill="#a0a0a0">?</text>
                    <text x="%s" y="%s" font-style="italic">%s</text>
                    <text x="%s" y="%s">%s</text>
                    </g>
                    """.formatted(
                    n(box.x()), n(box.y()), n(box.width()), n(box.height()),
                    n(box.x() + 6), n(box.y() + 12),
                    n(box.x() + 21), n(box.y() + 11), escape(conditionTitle(condition)),
                    n(box.x() + 3), n(box.y() + 25), escape(emptyAsUnknown(condition.text()))));
        }
    }

    /**
     * appends every edge as a line along its Eclipse route, with arrow, optional diamond, and two labels
     * - edges that the diagram file leaves out are not drawn, because Eclipse does not show them either.
     * @param svg the builder that collects the SVG output
     * @param rule the rule whose edges are drawn
     * @param nodeBoxes map from node id to its box
     */
    private void appendEdges(StringBuilder svg, HenshinRule rule, Map<String, Box> nodeBoxes) {
        // When the rule is in the diagram file, Eclipse shows only the edges that have a connector there.
        boolean ruleInDiagram = rule.layout().width() > 0
                || rule.edges().stream().anyMatch(edge -> edge.layout() != null && edge.layout().inDiagram());
        for (HenshinEdge edge : rule.edges()) {
            if (ruleInDiagram && (edge.layout() == null || !edge.layout().inDiagram())) {
                continue;
            }
            Box source = nodeBoxes.get(edge.sourceNodeId());
            Box target = nodeBoxes.get(edge.targetNodeId());
            if (source == null || target == null) {
                continue;
            }

            Style style = Style.of(edge.action());
            List<Point> route = route(edge, source, target);
            StringBuilder points = new StringBuilder();
            for (Point point : route) {
                points.append(points.isEmpty() ? "" : " ").append(n(point.x())).append(',').append(n(point.y()));
            }
            svg.append("<g>\n");
            svg.append("<polyline points=\"%s\" fill=\"none\" stroke=\"%s\"/>\n".formatted(points, style.line()));

            // Open arrow at the target end.
            Point end = route.get(route.size() - 1);
            Point beforeEnd = route.get(route.size() - 2);
            Point back = beforeEnd.minus(end).unit();
            Point side = back.normal();
            svg.append("<polyline points=\"%s %s %s\" fill=\"none\" stroke=\"%s\"/>\n".formatted(
                    xy(end.plus(back.times(7)).plus(side.times(3.5))),
                    xy(end),
                    xy(end.plus(back.times(7)).minus(side.times(3.5))),
                    style.line()));

            // Filled diamond at the source end for containment references.
            if (edge.containment()) {
                Point start = route.get(0);
                Point forward = route.get(1).minus(start).unit();
                Point normal = forward.normal();
                svg.append("<polygon class=\"diamond\" points=\"%s %s %s %s\" fill=\"%s\" stroke=\"%s\"/>\n".formatted(
                        xy(start),
                        xy(start.plus(forward.times(8)).plus(normal.times(3.5))),
                        xy(start.plus(forward.times(16))),
                        xy(start.plus(forward.times(8)).minus(normal.times(3.5))),
                        style.line(), style.line()));
            }

            HenshinEdgeLayout layout = edge.layout() == null ? HenshinEdgeLayout.empty() : edge.layout();
            HenshinEdgeLayout.Offset actionOffset = layout.actionOffset() != null
                    ? layout.actionOffset() : new HenshinEdgeLayout.Offset(0, -10);
            HenshinEdgeLayout.Offset nameOffset = layout.nameOffset() != null
                    ? layout.nameOffset() : new HenshinEdgeLayout.Offset(0, 10);
            Point actionCenter = labelCenter(route, actionOffset);
            Point nameCenter = labelCenter(route, nameOffset);
            svg.append("""
                    <text x="%s" y="%s" text-anchor="middle" fill="%s">%s</text>
                    <text x="%s" y="%s" text-anchor="middle">%s</text>
                    </g>
                    """.formatted(
                    n(actionCenter.x()), n(actionCenter.y() + 3), style.text(), escape(actionLabel(edge.action(), edge.multi())),
                    n(nameCenter.x()), n(nameCenter.y() + 3), escape(emptyAsUnknown(edge.type()))));
        }
    }

    /**
     * computes the points of an edge the way the GMF editor does
     * - bendpoints are stored relative to the source and target reference points and blended by their position.
     * - bendpoints that end up inside the source or target box are dropped (GMF does the same).
     * - the first and last point are where the line leaves the source box and enters the target box.
     * @param edge the edge with its layout
     * @param source the source node box
     * @param target the target node box
     * @return the points of the line, at least two
     */
    private List<Point> route(HenshinEdge edge, Box source, Box target) {
        HenshinEdgeLayout layout = edge.layout() == null ? HenshinEdgeLayout.empty() : edge.layout();
        if (source == target) {
            // A self-reference: a small loop on the right side of the box.
            double right = source.x() + source.width();
            double middle = source.y() + source.height() / 2;
            return List.of(new Point(right, middle - 8), new Point(right + 20, middle - 8),
                    new Point(right + 20, middle + 8), new Point(right, middle + 8));
        }

        Point sourceReference = anchorPoint(source, layout.sourceAnchor());
        Point targetReference = anchorPoint(target, layout.targetAnchor());

        List<Point> middle = new ArrayList<>();
        List<HenshinEdgeLayout.Bendpoint> bendpoints = layout.bendpoints();
        for (int i = 1; i < bendpoints.size() - 1; i++) {
            HenshinEdgeLayout.Bendpoint bendpoint = bendpoints.get(i);
            double weight = i / (double) (bendpoints.size() - 1);
            Point point = new Point(
                    (sourceReference.x() + bendpoint.sourceX()) * (1 - weight) + (targetReference.x() + bendpoint.targetX()) * weight,
                    (sourceReference.y() + bendpoint.sourceY()) * (1 - weight) + (targetReference.y() + bendpoint.targetY()) * weight);
            if (!source.contains(point) && !target.contains(point)) {
                middle.add(point);
            }
        }

        List<Point> route = new ArrayList<>();
        route.add(source.exitPoint(sourceReference, middle.isEmpty() ? targetReference : middle.get(0)));
        route.addAll(middle);
        route.add(target.exitPoint(targetReference, middle.isEmpty() ? sourceReference : middle.get(middle.size() - 1)));
        return route;
    }

    /**
     * returns the point a line is pinned to inside a box: the Eclipse anchor if there is one, else the center
     * @param box the node box
     * @param anchor the anchor as fractions of the box size, or null
     * @return the reference point
     */
    private Point anchorPoint(Box box, HenshinEdgeLayout.Anchor anchor) {
        if (anchor == null) {
            return new Point(box.x() + box.width() / 2, box.y() + box.height() / 2);
        }
        return new Point(box.x() + anchor.x() * box.width(), box.y() + anchor.y() * box.height());
    }

    /**
     * computes the center of an edge label like GMF does
     * - starts at the middle of the line (middle segment, or middle point for an odd number of points).
     * - the offset is turned along with the line direction, so labels stay on the same side of the line.
     * @param route the points of the edge
     * @param offset the label offset from the diagram file
     * @return the center of the label
     */
    private Point labelCenter(List<Point> route, HenshinEdgeLayout.Offset offset) {
        int size = route.size();
        Point segmentStart = route.get(size / 2 - 1);
        Point segmentEnd = route.get(size / 2);
        Point reference = size % 2 == 0
                ? new Point((segmentStart.x() + segmentEnd.x()) / 2, (segmentStart.y() + segmentEnd.y()) / 2)
                : segmentEnd;
        double angle = Math.atan2(segmentEnd.y() - segmentStart.y(), segmentEnd.x() - segmentStart.x());
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Point(
                reference.x() + offset.x() * cos - offset.y() * sin,
                reference.y() + offset.x() * sin + offset.y() * cos);
    }

    /**
     * computes the box of every node
     * - the position comes from the diagram file, relative to the inside of the rule frame.
     * - the size is the stored size, or the size Eclipse gives the box to fit its text.
     * @param rule the rule whose nodes are positioned
     * @return a map from node id to its box
     */
    private Map<String, Box> positionNodes(HenshinRule rule) {
        Map<String, Box> boxes = new HashMap<>();
        int fallbackIndex = 0;
        for (HenshinNode node : rule.nodes()) {
            HenshinLayout layout = node.layout();
            double width = layout.width() > 0 ? layout.width() : nodeWidth(node);
            double height = layout.height() > 0 ? layout.height() : nodeHeight(node);
            double x;
            double y;
            if (layout.x() == 0 && layout.y() == 0) {
                // No Eclipse position known: put the box on a simple grid.
                x = 10 + (fallbackIndex % 3) * 140;
                y = 10 + (fallbackIndex / 3) * 90;
                fallbackIndex++;
            } else {
                x = layout.x();
                y = layout.y();
            }
            boxes.put(node.id(), new Box(MARGIN + CONTENT_X + x, MARGIN + CONTENT_Y + y, width, height));
        }
        return boxes;
    }

    /**
     * computes the box of every attribute condition
     * - uses the Eclipse position when the diagram file has one.
     * - otherwise places the boxes in a row below the lowest node.
     * @param rule the rule whose conditions are positioned
     * @param nodeBoxes the node boxes, used for the fallback row
     * @return a map from condition id to its box
     */
    private Map<String, Box> positionConditions(HenshinRule rule, Map<String, Box> nodeBoxes) {
        Map<String, Box> boxes = new HashMap<>();
        double fallbackY = nodeBoxes.values().stream()
                .mapToDouble(box -> box.y() + box.height())
                .max()
                .orElse(MARGIN + CONTENT_Y) + 20;
        double fallbackX = MARGIN + CONTENT_X + 10;

        for (HenshinCondition condition : rule.conditions()) {
            HenshinLayout layout = condition.layout();
            double width = layout.width() > 0 ? layout.width() : Math.max(
                    21 + textWidth(conditionTitle(condition), true) + 8,
                    3 + textWidth(emptyAsUnknown(condition.text()), false) + 8);
            double height = layout.height() > 0 ? layout.height() : CONDITION_HEIGHT;
            Box box;
            if (layout.x() == 0 && layout.y() == 0) {
                box = new Box(fallbackX, fallbackY, width, height);
                fallbackX += width + 20;
            } else {
                box = new Box(MARGIN + CONTENT_X + layout.x(), MARGIN + CONTENT_Y + layout.y(), width, height);
            }
            boxes.put(condition.id(), box);
        }
        return boxes;
    }

    /**
     * returns the width Eclipse gives a node box: wide enough for its longest row
     * @param node the node
     * @return the box width in pixels
     */
    private double nodeWidth(HenshinNode node) {
        double width = Math.max(textWidth(actionLabel(node.action(), node.multi()), false), textWidth(nodeLabel(node), false)) + 8;
        for (HenshinAttribute attribute : node.attributes()) {
            width = Math.max(width, ATTRIBUTE_TEXT_X + textWidth(attributeLabel(attribute), false) + 10);
        }
        return Math.ceil(Math.max(width, 40));
    }

    /**
     * returns the attributes of a node sorted by name, the order the Eclipse editor shows them in
     * @param node the node
     * @return the sorted attributes
     */
    private List<HenshinAttribute> sortedAttributes(HenshinNode node) {
        return node.attributes().stream()
                .sorted(Comparator.comparing((HenshinAttribute attribute) -> emptyAsUnknown(attribute.name())))
                .toList();
    }

    /**
     * returns the height Eclipse gives a node box: the header plus at least one attribute row
     * @param node the node
     * @return the box height in pixels
     */
    private double nodeHeight(HenshinNode node) {
        return NODE_HEADER_HEIGHT + 1 + Math.max(1, node.attributes().size()) * ATTRIBUTE_ROW_HEIGHT;
    }

    /**
     * computes the rule frame
     * - uses the Eclipse frame size when the diagram file has one.
     * - grows so the title and every box fit completely (nothing is cut off).
     * @param rule the rule
     * @param nodeBoxes the node boxes
     * @param conditionBoxes the condition boxes
     * @return the frame box
     */
    private Box calculateFrame(HenshinRule rule, Map<String, Box> nodeBoxes, Map<String, Box> conditionBoxes) {
        double width = rule.layout().width();
        double height = rule.layout().height();
        width = Math.max(width, 21 + textWidth(ruleTitle(rule), true) + 10);
        for (Map<String, Box> boxes : List.of(nodeBoxes, conditionBoxes)) {
            for (Box box : boxes.values()) {
                width = Math.max(width, box.x() + box.width() - MARGIN + 15);
                height = Math.max(height, box.y() + box.height() - MARGIN + 15);
            }
        }
        height = Math.max(height, CONTENT_Y + 30);
        return new Box(MARGIN, MARGIN, Math.ceil(width), Math.ceil(height));
    }

    /**
     * measures text in the Eclipse font (Tahoma 9px)
     * @param text the text
     * @param italic true for the italic variant
     * @return the width in pixels
     */
    static double textWidth(String text, boolean italic) {
        return TextMeasure.width(text, italic);
    }

    /**
     * returns the text of one attribute row
     * @param attribute the attribute to show
     * @return "name=value", or "name=old->new" when the rule changes the value
     */
    private String attributeLabel(HenshinAttribute attribute) {
        String label = emptyAsUnknown(attribute.name()) + "=" + emptyAsUnknown(attribute.value());
        return attribute.changes() ? label + "->" + attribute.newValue() : label;
    }

    /**
     * returns the second row of a node box, for example ":Account" or "a:Account"
     * @param node the node
     * @return the name and type text
     */
    private String nodeLabel(HenshinNode node) {
        String name = node.name() == null || node.name().isBlank() ? "" : node.name();
        return name + ":" + emptyAsUnknown(node.type());
    }

    /**
     * returns the first line of a condition note, like Eclipse: "Condition" and the condition name
     * @param condition the condition
     * @return for example "Condition financial covering"
     */
    private String conditionTitle(HenshinCondition condition) {
        return condition.name() == null || condition.name().isBlank() ? "Condition" : "Condition " + condition.name();
    }

    /**
     * builds the header title of the rule, including its parameter list
     * @param rule the rule whose title is built
     * @return a title such as "Rule createAccount(in client:EString, in accountId:EInt)"
     */
    private String ruleTitle(HenshinRule rule) {
        if (rule.parameters().isEmpty()) {
            return "Rule " + rule.name();
        }

        StringBuilder title = new StringBuilder("Rule ");
        title.append(rule.name());
        title.append("(");
        for (int i = 0; i < rule.parameters().size(); i++) {
            HenshinParameter parameter = rule.parameters().get(i);
            if (i > 0) {
                title.append(", ");
            }
            title.append(parameterKind(parameter));
            title.append(" ");
            title.append(parameter.name());
            if (parameter.type() != null && !parameter.type().isBlank()) {
                title.append(":");
                title.append(parameter.type());
            }
        }
        title.append(")");
        return title.toString();
    }

    /**
     * returns the lowercase kind of a parameter, defaulting to "var" when it is missing
     * @param parameter the parameter whose kind is requested
     * @return the parameter kind in lowercase, or "var" if none is set
     */
    private String parameterKind(HenshinParameter parameter) {
        if (parameter.kind() == null || parameter.kind().isBlank()) {
            return "var";
        }
        return parameter.kind().toLowerCase(Locale.ROOT);
    }

    /**
     * returns the stereotype label of an action, for example «preserve» or «create»
     * - multi-rule elements get a star like in Eclipse, for example «delete*».
     * @param action the action to label
     * @param multi true when the element belongs to a multi-rule
     * @return the guillemet-wrapped action label
     */
    private String actionLabel(HenshinAction action, boolean multi) {
        String name = switch (action) {
            case PRESERVE -> "preserve";
            case CREATE -> "create";
            case DELETE -> "delete";
            case FORBID -> "forbid";
        };
        return "«" + name + (multi ? "*" : "") + "»";
    }

    /**
     * replaces a null or blank value with the text "unknown"
     * @param value the value to check
     * @return the value, or "unknown" if it is null or blank
     */
    private String emptyAsUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    /**
     * escapes the XML special characters of a text so it is safe inside the SVG
     * @param value the text to escape
     * @return the escaped text
     */
    private String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /**
     * formats a coordinate with at most one decimal and a dot, for example "12" or "12.5"
     * @param value the number
     * @return the number as SVG text
     */
    private static String n(double value) {
        double rounded = Math.round(value * 10) / 10.0;
        if (rounded == Math.rint(rounded)) {
            return Long.toString((long) rounded);
        }
        return String.format(Locale.ROOT, "%.1f", rounded);
    }

    /**
     * formats a point as "x,y" for SVG point lists
     * @param point the point
     * @return the point as SVG text
     */
    private static String xy(Point point) {
        return n(point.x()) + "," + n(point.y());
    }

    /**
     * The Eclipse colors for one action: line, light fill, and «action» text.
     */
    private record Style(String line, String fill, String text) {
        static Style of(HenshinAction action) {
            return switch (action) {
                case PRESERVE -> new Style("#808080", "#ececec", "#808080");
                case CREATE -> new Style("#5bbf45", "#e3fbdc", "#38b02a");
                case DELETE -> new Style("#e04040", "#fcdada", "#e82020");
                case FORBID -> new Style("#1f1fd8", "#dcdcfa", "#1010f0");
            };
        }
    }

    /**
     * A box on the canvas, able to compute where a line leaves it.
     */
    private record Box(double x, double y, double width, double height) {
        /**
         * tells whether a point lies strictly inside the box
         * @param point the point
         * @return true if the point is inside
         */
        boolean contains(Point point) {
            return point.x() > x && point.x() < x + width && point.y() > y && point.y() < y + height;
        }

        /**
         * returns where a line from a point inside the box toward another point crosses the box border
         * @param from the start point, inside the box or on its border
         * @param toward the point the line heads to
         * @return the point on the border
         */
        Point exitPoint(Point from, Point toward) {
            double dx = toward.x() - from.x();
            double dy = toward.y() - from.y();
            double tx = dx > 0 ? (x + width - from.x()) / dx : dx < 0 ? (x - from.x()) / dx : Double.POSITIVE_INFINITY;
            double ty = dy > 0 ? (y + height - from.y()) / dy : dy < 0 ? (y - from.y()) / dy : Double.POSITIVE_INFINITY;
            double t = Math.min(tx, ty);
            if (Double.isInfinite(t) || t < 0) {
                return from;
            }
            return new Point(from.x() + t * dx, from.y() + t * dy);
        }
    }

    /**
     * A simple x/y point with the few vector operations needed for arrows and labels.
     */
    private record Point(double x, double y) {
        Point plus(Point other) {
            return new Point(x + other.x, y + other.y);
        }

        Point minus(Point other) {
            return new Point(x - other.x, y - other.y);
        }

        Point times(double factor) {
            return new Point(x * factor, y * factor);
        }

        Point unit() {
            double length = Math.hypot(x, y);
            return length == 0 ? new Point(1, 0) : new Point(x / length, y / length);
        }

        Point normal() {
            return new Point(-y, x);
        }
    }

    /**
     * Measures text with the Tahoma font Eclipse uses, or a similar sans-serif font if Tahoma is missing.
     */
    private static final class TextMeasure {
        static {
            // Measuring text must never open a window or a Dock icon.
            if (System.getProperty("java.awt.headless") == null) {
                System.setProperty("java.awt.headless", "true");
            }
        }

        private static final FontRenderContext CONTEXT = new FontRenderContext(null, true, true);
        private static final Font REGULAR = loadFont();
        private static final Font ITALIC = REGULAR == null ? null : REGULAR.deriveFont(Font.ITALIC);

        private TextMeasure() {
        }

        private static Font loadFont() {
            try {
                Font tahoma = new Font("Tahoma", Font.PLAIN, 9);
                if ("Tahoma".equalsIgnoreCase(tahoma.getFamily(Locale.ROOT))) {
                    return tahoma.deriveFont(9f);
                }
                return new Font(Font.SANS_SERIF, Font.PLAIN, 9).deriveFont(9f);
            } catch (Throwable e) {
                // No usable fonts on this system: fall back to an estimate.
                return null;
            }
        }

        static double width(String text, boolean italic) {
            Font font = italic ? ITALIC : REGULAR;
            if (font == null) {
                return text.length() * 5.0;
            }
            return font.getStringBounds(text, CONTEXT).getWidth();
        }
    }
}
