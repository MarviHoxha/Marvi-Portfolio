package henshinIntegration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Tests the SVG output for the rules of the bank example.
 */
class HenshinSvgVisualizerTest {
    private static final Path BANK = Path.of("src/test/resources/henshin/bank");

    private String svgFor(String ruleName) throws IOException {
        HenshinRule rule = new HenshinRuleExtractor().extract(
                BANK.resolve("bank.henshin"), BANK.resolve("bank.henshin_diagram"), ruleName);
        return new HenshinSvgVisualizer().toSvg(rule);
    }

    private static Document parse(String svg) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"createAccount", "transferMoney", "deleteAllAccounts", "DELEAT"})
    void everyBankRuleProducesValidXml(String ruleName) throws Exception {
        Document document = parse(svgFor(ruleName));
        assertTrue(document.getDocumentElement().getTagName().equals("svg"));
    }

    @Test
    void longTitleFitsInsideTheCanvas() throws Exception {
        String svg = svgFor("transferMoney");
        Element root = parse(svg).getDocumentElement();
        int width = Integer.parseInt(root.getAttribute("width"));

        String title = "Rule transferMoney(in client:EString, in fromId:EInt, in toId:EInt, "
                + "in amount:EDouble, var x:EDouble, var y:EDouble)";
        assertTrue(svg.contains(title));
        // The title starts 21px into the frame, and the frame starts after the 10px margin.
        double needed = 10 + 21 + HenshinSvgVisualizer.textWidth(title, true);
        assertTrue(width >= needed, "canvas width " + width + " is too small for the title");
    }

    @Test
    void changedAttributesAndConditionsAreDrawn() throws IOException {
        String svg = svgFor("transferMoney");

        assertTrue(svg.contains("credit=x-&gt;x-amount"));
        assertTrue(svg.contains("credit=y-&gt;y+amount"));
        assertTrue(svg.contains("financial covering"));
        assertTrue(svg.contains("x&gt;amount"));
        assertTrue(svg.contains("amount&gt;0"));
    }

    @Test
    void multiRuleElementsGetAStar() throws IOException {
        String svg = svgFor("deleteAllAccounts");

        assertTrue(svg.contains("«delete*»"));
        assertFalse(svg.contains("in client:EString, in client:EString"));
    }

    @Test
    void nonContainmentEdgesHaveNoDiamond() throws IOException {
        String svg = svgFor("createAccount");

        // Bank.clients, Bank.managers, Bank.accounts (create) and the forbidden Bank.accounts are containment.
        long diamonds = svg.lines().filter(line -> line.contains("class=\"diamond\"")).count();
        assertTrue(diamonds == 4, "expected 4 containment diamonds but found " + diamonds);
    }

    @Test
    void usesTheEclipseLookAndRoute() throws IOException {
        String svg = svgFor("createAccount");

        // Same font as the Eclipse editor, no serif or bold text, and conditions read "Condition <name>".
        assertTrue(svg.contains("font-family=\"Tahoma"));
        assertFalse(svg.contains("Times New Roman"));
        assertFalse(svg.contains("font-weight"));
        assertTrue(svgFor("transferMoney").contains("Condition financial covering"));

        // Bank -> Manager runs straight from the Bank's right side to the Manager's left side at y = 10 + 29 + 37.
        assertTrue(svg.matches("(?s).*points=\"[0-9.]+,76 [0-9.]+,76\".*"),
                "Bank -> Manager edge should be a horizontal line at y=76");
    }
}
