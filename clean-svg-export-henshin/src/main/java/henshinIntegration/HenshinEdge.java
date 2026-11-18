package henshinIntegration;

/**
 Represents one arrow in a Henshin rule diagram.
 - id: internal XMI id from the .henshin file.
 - sourceNodeId: id of the node where the arrow starts.
 - targetNodeId: id of the node where the arrow ends.
 - type: simplified reference name, for example "accounts", "clients", or "type".
 - action: preserve/create/delete, calculated from LHS/RHS edge comparison.
 - containment: true when the matching Ecore reference (class + name) has containment="true".
 *  visualizer uses containment to decide whether an edge should get a diamond marker.
 - multi: true when the edge belongs to a multi-rule (Eclipse shows it as «delete*», «create*», ...).
 - layout: bendpoints, anchors, and label positions from the .henshin_diagram file.
 */
public record HenshinEdge(
        String id,
        String sourceNodeId,
        String targetNodeId,
        String type,
        HenshinAction action,
        boolean containment,
        boolean multi,
        HenshinEdgeLayout layout) {
    /**
     * creates an edge without Eclipse layout information
     */
    public HenshinEdge(String id, String sourceNodeId, String targetNodeId, String type,
                       HenshinAction action, boolean containment, boolean multi) {
        this(id, sourceNodeId, targetNodeId, type, action, containment, multi, HenshinEdgeLayout.empty());
    }

    /**
     * creates an edge of the main rule (not a multi-rule)
     */
    public HenshinEdge(String id, String sourceNodeId, String targetNodeId, String type,
                       HenshinAction action, boolean containment) {
        this(id, sourceNodeId, targetNodeId, type, action, containment, false);
    }

    /**
     * returns a copy of this edge with the given Eclipse layout
     * @param newLayout the layout to use
     * @return the same edge with the new layout
     */
    public HenshinEdge withLayout(HenshinEdgeLayout newLayout) {
        return new HenshinEdge(id, sourceNodeId, targetNodeId, type, action, containment, multi, newLayout);
    }
}
