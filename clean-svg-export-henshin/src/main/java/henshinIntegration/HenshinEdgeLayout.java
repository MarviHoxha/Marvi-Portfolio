package henshinIntegration;

import java.util.List;

/**
 * Stores how Eclipse draws one edge, read from the .henshin_diagram file.
 * - bendpoints: the GMF relative bendpoints, including the first and last point.
 * - sourceAnchor/targetAnchor: where the edge is pinned inside the node box (0..1), or null for the box center.
 * - nameOffset/actionOffset: where the relation-name label and the «action» label sit, relative to the edge middle.
 *   null means Eclipse uses its default position.
 * - inDiagram: true when the .henshin_diagram file has a connector for this edge (Eclipse only draws those).
 */
public record HenshinEdgeLayout(
        List<Bendpoint> bendpoints,
        Anchor sourceAnchor,
        Anchor targetAnchor,
        Offset nameOffset,
        Offset actionOffset,
        boolean inDiagram) {

    /**
     * returns a layout with no Eclipse information, so the visualizer uses its defaults
     * @return an empty edge layout
     */
    public static HenshinEdgeLayout empty() {
        return new HenshinEdgeLayout(List.of(), null, null, null, null, false);
    }

    /**
     * One GMF relative bendpoint: an offset from the source reference point and one from the target reference point.
     */
    public record Bendpoint(int sourceX, int sourceY, int targetX, int targetY) {
    }

    /**
     * A fixed anchor position inside a node box, as fractions of its width and height.
     */
    public record Anchor(double x, double y) {
    }

    /**
     * A label offset in pixels.
     */
    public record Offset(int x, int y) {
    }
}
