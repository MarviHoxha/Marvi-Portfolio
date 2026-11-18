package henshinIntegration;

/**
  Represents one attribute line inside a Henshin node box.
  - name: the structural feature name from the Ecore model, for example "id" or "name".
  - value: the expression used by the Henshin rule, for example "accountId" or "client".
  - newValue: the new expression when a preserved node changes the attribute, for example "x-amount".
    null when the attribute does not change.
 */
public record HenshinAttribute(String name, String value, String newValue) {
    /**
     * creates an attribute whose value is not changed by the rule
     * @param name the attribute name
     * @param value the attribute value expression
     */
    public HenshinAttribute(String name, String value) {
        this(name, value, null);
    }

    /**
     * tells whether the rule changes this attribute
     * @return true if the attribute has a new value in the RHS
     */
    public boolean changes() {
        return newValue != null;
    }
}
