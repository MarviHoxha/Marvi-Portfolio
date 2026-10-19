package svgGeneration;

/**
 * Describes the meaning of a node in the generated diagram.
 */
public enum SvgNodeType {
    // Each type has a color so generated diagrams can show different meanings visually.
    PATTERN("#e8f1ff"),
    ACTION("#fff4cc"),
    RESULT("#e7f6e7");

    private final String fillColor;

    SvgNodeType(String fillColor) {
        this.fillColor = fillColor;
    }

    public String getFillColor() {
        return fillColor;
    }
}
