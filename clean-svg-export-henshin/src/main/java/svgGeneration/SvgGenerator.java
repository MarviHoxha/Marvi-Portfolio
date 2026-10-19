package svgGeneration;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;

/**
 * Creates simple SVG diagrams directly as XML text.
 */
public class SvgGenerator {
    private final int width;
    private final int height;
    private final StringBuilder body;

    public SvgGenerator(int width, int height) {
        this.width = width;
        this.height = height;
        this.body = new StringBuilder();
    }

    public void addRectangle(int x, int y, int width, int height, String label) {
        addRectangle(x, y, width, height, label, SvgNodeType.PATTERN);
    }

    public void addRectangle(int x, int y, int width, int height, String label, SvgNodeType type) {
        body.append("  <rect x=\"")
                .append(x)
                .append("\" y=\"")
                .append(y)
                .append("\" width=\"")
                .append(width)
                .append("\" height=\"")
                .append(height)
                .append("\" fill=\"")
                .append(type.getFillColor())
                .append("\" stroke=\"black\" stroke-width=\"2\"/>\n");

        body.append("  <text x=\"")
                .append(x + 12)
                .append("\" y=\"")
                .append(y + (height / 2) + 5)
                .append("\" font-family=\"Arial\" font-size=\"14\">")
                .append(escapeXml(label))
                .append("</text>\n");
    }

    public void addArrow(int startX, int startY, int endX, int endY) {
        body.append("  <line x1=\"")
                .append(startX)
                .append("\" y1=\"")
                .append(startY)
                .append("\" x2=\"")
                .append(endX)
                .append("\" y2=\"")
                .append(endY)
                .append("\" stroke=\"black\" stroke-width=\"2\" marker-end=\"url(#arrowhead)\"/>\n");
    }

    public void addDiagram(SvgDiagram diagram) {
        addTitle(diagram.getTitle());

        // First draw all boxes, then draw the arrows on top of the diagram.
        for (SvgNode node : diagram.getNodes()) {
            addRectangle(node.x(), node.y(), node.width(), node.height(), node.label(), node.type());
        }

        for (SvgEdge edge : diagram.getEdges()) {
            SvgNode source = diagram.findNodeById(edge.sourceNodeId());
            SvgNode target = diagram.findNodeById(edge.targetNodeId());

            // Connect the middle-right side of the source box to the middle-left side of the target box.
            int startX = source.x() + source.width();
            int startY = source.y() + source.height() / 2;
            int endX = target.x();
            int endY = target.y() + target.height() / 2;

            addArrow(startX, startY, endX, endY);
            addText((startX + endX) / 2 - 20, startY - 10, edge.label());
        }
    }

    public void addTitle(String title) {
        body.append("  <text x=\"40\" y=\"40\" font-family=\"Arial\" font-size=\"20\" font-weight=\"bold\" fill=\"white\">")
                .append(escapeXml(title))
                .append("</text>\n");
    }

    public void addText(int x, int y, String text) {
        body.append("  <text x=\"")
                .append(x)
                .append("\" y=\"")
                .append(y)
                .append("\" font-family=\"Arial\" font-size=\"12\" fill=\"white\">")
                .append(escapeXml(text))
                .append("</text>\n");
    }

    public String build() {
        // The marker definition creates the arrowhead used by all line elements.
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <svg width="%d" height="%d" viewBox="0 0 %d %d" xmlns="http://www.w3.org/2000/svg">
                  <defs>
                    <marker id="arrowhead" markerWidth="10" markerHeight="7" refX="10" refY="3.5" orient="auto">
                      <polygon points="0 0, 10 3.5, 0 7" fill="black"/>
                    </marker>
                  </defs>
                %s</svg>
                """.formatted(width, height, width, height, body);
    }

    public void saveToFile(Path outputPath) throws IOException {
        String svg = build();
        validateSvgXml(svg);
        Files.writeString(outputPath, svg);
    }

    private void validateSvgXml(String svg) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.newDocumentBuilder().parse(new InputSource(new StringReader(svg)));
        } catch (Exception e) {
            throw new IllegalStateException("Generated SVG is not valid XML.", e);
        }
    }

    private String escapeXml(String text) {
        // Text must be escaped so labels containing characters like < or & still produce valid SVG.
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
