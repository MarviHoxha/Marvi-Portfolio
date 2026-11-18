package henshinIntegration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests the Henshin extraction with the bank example in src/test/resources/henshin/bank.
 */
class HenshinRuleExtractorTest {
    private static final Path BANK = Path.of("src/test/resources/henshin/bank");
    private static final Path HENSHIN = BANK.resolve("bank.henshin");
    private static final Path DIAGRAM = BANK.resolve("bank.henshin_diagram");

    private HenshinRule extract(String ruleName) throws IOException {
        return new HenshinRuleExtractor().extract(HENSHIN, DIAGRAM, ruleName);
    }

    private static HenshinNode nodeOfType(HenshinRule rule, String type, HenshinAction action) {
        return rule.nodes().stream()
                .filter(node -> node.type().equals(type) && node.action() == action)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No " + action + " node of type " + type));
    }

    private static HenshinEdge edge(HenshinRule rule, String sourceType, String type, String targetType) {
        return rule.edges().stream()
                .filter(edge -> edge.type().equals(type)
                        && typeOf(rule, edge.sourceNodeId()).equals(sourceType)
                        && typeOf(rule, edge.targetNodeId()).equals(targetType))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No edge " + sourceType + "." + type + " -> " + targetType));
    }

    private static String typeOf(HenshinRule rule, String nodeId) {
        return rule.nodes().stream()
                .filter(node -> node.id().equals(nodeId))
                .map(HenshinNode::type)
                .findFirst()
                .orElse("?");
    }

    @Test
    void containmentDependsOnClassAndReferenceName() throws IOException {
        HenshinRule rule = extract("createAccount");

        // Bank.clients is a containment reference in bank.ecore, Manager.clients is not.
        assertTrue(edge(rule, "Bank", "clients", "Client").containment());
        assertFalse(edge(rule, "Manager", "clients", "Client").containment());
        // Bank.accounts is containment, Client.accounts is not.
        assertTrue(edge(rule, "Bank", "accounts", "Account").containment());
        assertFalse(edge(rule, "Client", "accounts", "Account").containment());
    }

    @Test
    void createAccountHasCreateAndForbidParts() throws IOException {
        HenshinRule rule = extract("createAccount");

        assertEquals(5, rule.nodes().size());
        nodeOfType(rule, "Account", HenshinAction.CREATE);
        nodeOfType(rule, "Account", HenshinAction.FORBID);
        assertEquals(List.of("client", "accountId"),
                rule.parameters().stream().map(HenshinParameter::name).toList());
    }

    @Test
    void transferMoneyShowsChangedAttributes() throws IOException {
        HenshinRule rule = extract("transferMoney");

        List<HenshinAttribute> credits = rule.nodes().stream()
                .flatMap(node -> node.attributes().stream())
                .filter(attribute -> attribute.name().equals("credit"))
                .toList();

        assertEquals(2, credits.size());
        assertTrue(credits.contains(new HenshinAttribute("credit", "x", "x-amount")));
        assertTrue(credits.contains(new HenshinAttribute("credit", "y", "y+amount")));

        // Unchanged attributes stay without a new value.
        assertTrue(rule.nodes().stream()
                .flatMap(node -> node.attributes().stream())
                .filter(attribute -> attribute.name().equals("id"))
                .noneMatch(HenshinAttribute::changes));
    }

    @Test
    void transferMoneyHasAttributeConditionsWithEclipsePositions() throws IOException {
        HenshinRule rule = extract("transferMoney");

        assertEquals(2, rule.conditions().size());
        HenshinCondition first = rule.conditions().get(0);
        assertEquals("financial covering", first.name());
        assertEquals("x>amount", first.text());
        assertEquals(30, first.layout().x());
        assertEquals(206, first.layout().y());
        assertEquals("amount>0", rule.conditions().get(1).text());
    }

    @Test
    void deleteAllAccountsIncludesMultiRuleOnce() throws IOException {
        HenshinRule rule = extract("deleteAllAccounts");

        // Bank, Manager, Client from the main rule + the extra Account from the multi-rule.
        assertEquals(4, rule.nodes().size());
        HenshinNode account = nodeOfType(rule, "Account", HenshinAction.DELETE);
        assertTrue(account.multi());
        assertNotEquals(0, account.layout().x());

        // The multi-rule copies of Bank, Manager, Client are not drawn again.
        assertEquals(3, rule.nodes().stream().filter(node -> !node.multi()).count());

        // Multi-rule edges connect the Account to the main-rule Bank and Client.
        HenshinEdge accounts = edge(rule, "Bank", "accounts", "Account");
        assertTrue(accounts.multi());
        assertEquals(HenshinAction.DELETE, accounts.action());
        assertTrue(edge(rule, "Account", "owner", "Client").multi());

        // The three main-rule edges are not duplicated by the multi-rule copies.
        assertEquals(5, rule.edges().size());
    }

    @Test
    void parametersOfMultiRulesAreNotDuplicated() throws IOException {
        HenshinRule rule = extract("deleteAllAccounts");

        assertEquals(1, rule.parameters().size());
        assertEquals("client", rule.parameters().get(0).name());
    }

    @Test
    void deleteRuleWithoutParameters() throws IOException {
        HenshinRule rule = extract("DELEAT");

        assertEquals(1, rule.nodes().size());
        assertEquals(HenshinAction.DELETE, rule.nodes().get(0).action());
        assertTrue(rule.parameters().isEmpty());
        assertTrue(rule.edges().isEmpty());
    }

    @Test
    void unknownRuleNameIsRejected() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> extract("noSuchRule"));
        assertTrue(error.getMessage().contains("noSuchRule"));
    }
}
