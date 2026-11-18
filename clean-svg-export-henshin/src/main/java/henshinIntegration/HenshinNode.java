package henshinIntegration;

import java.util.List;

/**
 * Represents one box in a Henshin rule diagram.
 * - id: internal XMI id from the .henshin file.
 * - name: optional node name from the Henshin model.
 * - type: Ecore type name, for example Bank, Client, Account, Class, or Method.
 * - action: preserve/create/delete, calculated from LHS/RHS/mappings.
 * - attributes: attribute rows displayed inside the node box.
 * - layout: x/y position read from .henshin_diagram.
 * - multi: true when the node belongs to a multi-rule (Eclipse shows it as «delete*», «create*», ...).
 */
public record HenshinNode(
        String id,
        String name,
        String type,
        HenshinAction action,
        List<HenshinAttribute> attributes,
        HenshinLayout layout,
        boolean multi) {
    /**
     * creates a node of the main rule (not a multi-rule)
     */
    public HenshinNode(String id, String name, String type, HenshinAction action,
                       List<HenshinAttribute> attributes, HenshinLayout layout) {
        this(id, name, type, action, attributes, layout, false);
    }
}
