package svgGeneration;

/**
 * Represents one arrow between two SVG nodes.
 */
public record SvgEdge(String sourceNodeId, String targetNodeId, String label) {
}
