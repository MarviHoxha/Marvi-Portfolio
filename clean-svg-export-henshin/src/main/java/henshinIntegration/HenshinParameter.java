package henshinIntegration;

/**
 * Represents one parameter declared on a Henshin rule.
 * - name: the variable name, for example "client" or "accountId".
 * - kind: Henshin direction/kind, for example IN or VAR.
 * - type: simplified Ecore type name, for example EString or EInt.
 *  shown in the rule title, Rule createAccount(var client, var accountId).
 */
public record HenshinParameter(String name, String kind, String type) {
}
