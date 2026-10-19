package svgGeneration;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the simple diagram data that should be converted into SVG.
 */
public class SvgDiagram {
    private final String title;
    private final List<SvgNode> nodes;
    private final List<SvgEdge> edges;

    public SvgDiagram() {
        this("Untitled diagram");
    }

    public SvgDiagram(String title) {
        this.title = title;
        this.nodes = new ArrayList<>();
        this.edges = new ArrayList<>();
    }

    public String getTitle() {
        return title;
    }

    public void addNode(SvgNode node) {
        nodes.add(node);
    }

    public void addEdge(SvgEdge edge) {
        edges.add(edge);
    }

    public List<SvgNode> getNodes() {
        return nodes;
    }

    public List<SvgEdge> getEdges() {
        return edges;
    }

    public SvgNode findNodeById(String id) {
        return nodes.stream()
                .filter(node -> node.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown SVG node id: " + id));
    }

    // Simple first layout: every node is placed next to the previous one.
    // Later this can be replaced by a smarter layout for real Henshin diagrams.
    public void applyHorizontalLayout(int startX, int y, int gap) {
        for (int i = 0; i < nodes.size(); i++) {
            SvgNode node = nodes.get(i);
            int x = startX + i * (node.width() + gap);
            nodes.set(i, node.withPosition(x, y));
        }
    }
}
