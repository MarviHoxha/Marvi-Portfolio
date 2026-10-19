package svgGeneration;

/**
 * Represents one labeled box in a generated SVG diagram.
 */
public record SvgNode(String id, String label, SvgNodeType type, int x, int y, int width, int height) {
    public SvgNode(String id, String label) {
        this(id, label, SvgNodeType.PATTERN);
    }

    public SvgNode(String id, String label, SvgNodeType type) {
        // New nodes start without a real position. The layout step fills x and y later.
        this(id, label, type, 0, 0, 130, 60);
    }

    public SvgNode withPosition(int x, int y) {
        return new SvgNode(id, label, type, x, y, width, height);
    }
}
