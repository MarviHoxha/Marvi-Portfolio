package henshinIntegration;

/**
 * Represents one attribute condition of a Henshin rule, for example "financial covering: x>amount".
 * - id: internal XMI id from the .henshin file.
 * - name: the condition name, may be empty.
 * - text: the condition expression that must be true for the rule to apply.
 * - layout: x/y position read from .henshin_diagram, empty if Eclipse has no position for it.
 */
public record HenshinCondition(String id, String name, String text, HenshinLayout layout) {
}
