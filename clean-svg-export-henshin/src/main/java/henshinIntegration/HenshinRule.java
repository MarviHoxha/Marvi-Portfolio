package henshinIntegration;

import java.util.List;

/**
 * Java representation of one extracted Henshin rule.
 * - name: rule name from the .henshin file, for example "createAccount".
 * - parameters: variables declared by the rule.
 * - layout: the rule container position/size from the .henshin_diagram file.
 * - nodes: all visible Henshin nodes after combining LHS/RHS information (including multi-rule nodes).
 * - edges: all visible Henshin edges after combining LHS/RHS information (including multi-rule edges).
 * - conditions: attribute conditions such as "x>amount".
 */
public record HenshinRule(
        String name,
        List<HenshinParameter> parameters,
        HenshinLayout layout,
        List<HenshinNode> nodes,
        List<HenshinEdge> edges,
        List<HenshinCondition> conditions) {
    /**
     * creates a rule without attribute conditions
     */
    public HenshinRule(String name, List<HenshinParameter> parameters, HenshinLayout layout,
                       List<HenshinNode> nodes, List<HenshinEdge> edges) {
        this(name, parameters, layout, nodes, edges, List.of());
    }
}
