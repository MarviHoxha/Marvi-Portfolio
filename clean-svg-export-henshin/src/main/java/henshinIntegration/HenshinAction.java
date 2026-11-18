package henshinIntegration;

/**
 Describes what a Henshin rule does with one node or edge.
  - PRESERVE: the element exists in the LHS and RHS, connected by a Henshin mapping.
  - CREATE: the element exists only in the RHS, so the rule creates it.
  - DELETE: the element exists only in the LHS, so the rule removes it.
  - FORBID: the element is part of a negative condition and must not exist.
 */
public enum HenshinAction {
    PRESERVE,
    CREATE,
    DELETE,
    FORBID
}
