package henshinIntegration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests the command-line tool by calling HenshinSvgMain.run directly.
 */
class HenshinSvgMainTest {
    private static final Path BANK = Path.of("src/test/resources/henshin/bank");
    private static final String HENSHIN = BANK.resolve("bank.henshin").toString();

    private record Result(int exitCode, String out, String err) {
    }

    private static Result run(String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int exitCode = HenshinSvgMain.run(args,
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        return new Result(exitCode, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    }

    @Test
    void helpIsPrinted() {
        Result result = run("--help");

        assertEquals(0, result.exitCode());
        assertTrue(result.out().contains("Usage: HenshinSvgMain"));
        assertTrue(result.out().contains("--all"));
    }

    @Test
    void listsAllRules() {
        Result result = run("--input", HENSHIN, "--list");

        assertEquals(0, result.exitCode());
        assertTrue(result.out().contains("- createAccount"));
        assertTrue(result.out().contains("- transferMoney"));
        assertTrue(result.out().contains("- deleteAllAccounts"));
        assertTrue(result.out().contains("- DELEAT"));
    }

    @Test
    void exportsOneRuleToTheGivenFile(@TempDir Path temp) throws IOException {
        Path output = temp.resolve("transfer.svg");

        Result result = run("--input", HENSHIN, "--rule", "transferMoney", "--output", output.toString());

        assertEquals(0, result.exitCode(), result.err());
        assertTrue(Files.readString(output).contains("Rule transferMoney("));
        assertTrue(result.out().contains("Exported transferMoney"));
    }

    @Test
    void exportsAllRulesIntoAFolderWithConsistentNames(@TempDir Path temp) {
        Path folder = temp.resolve("svg-out");

        Result result = run("--input", HENSHIN, "--all", "--output", folder.toString());

        assertEquals(0, result.exitCode(), result.err());
        for (String rule : new String[] {"createAccount", "transferMoney", "deleteAllAccounts", "DELEAT"}) {
            assertTrue(Files.isRegularFile(folder.resolve("bank_" + rule + ".svg")), "missing " + rule);
        }
        assertTrue(result.out().contains("[1/4]"));
        assertTrue(result.out().contains("Done: 4 of 4 rules exported."));
    }

    @Test
    void exportsSeveralChosenRules(@TempDir Path temp) {
        Result result = run("--input", HENSHIN, "-r", "createAccount", "-r", "DELEAT", "-o", temp.toString());

        assertEquals(0, result.exitCode(), result.err());
        assertTrue(Files.exists(temp.resolve("bank_createAccount.svg")));
        assertTrue(Files.exists(temp.resolve("bank_DELEAT.svg")));
        assertFalse(Files.exists(temp.resolve("bank_transferMoney.svg")));
    }

    @Test
    void unknownRuleShowsTheAvailableRules() {
        Result result = run("--input", HENSHIN, "--rule", "noSuchRule");

        assertEquals(1, result.exitCode());
        assertTrue(result.err().contains("rule not found: noSuchRule"));
        assertTrue(result.err().contains("Available rules: createAccount, transferMoney, deleteAllAccounts, DELEAT"));
    }

    @Test
    void existingFileIsNotOverwrittenWithoutFlag(@TempDir Path temp) throws IOException {
        Path output = temp.resolve("rule.svg");
        Files.writeString(output, "keep me");

        Result result = run("--input", HENSHIN, "--rule", "DELEAT", "--output", output.toString());

        assertEquals(1, result.exitCode());
        assertTrue(result.err().contains("already exists"));
        assertEquals("keep me", Files.readString(output));
    }

    @Test
    void existingFileIsReplacedWithOverwrite(@TempDir Path temp) throws IOException {
        Path output = temp.resolve("rule.svg");
        Files.writeString(output, "old");

        Result result = run("--input", HENSHIN, "--rule", "DELEAT", "--output", output.toString(), "--overwrite");

        assertEquals(0, result.exitCode(), result.err());
        assertTrue(Files.readString(output).startsWith("<?xml"));
    }

    @Test
    void oneSvgFileForSeveralRulesIsRejected(@TempDir Path temp) {
        Result result = run("--input", HENSHIN, "--all", "--output", temp.resolve("all.svg").toString());

        assertEquals(1, result.exitCode());
        assertTrue(result.err().contains("Give a folder instead"));
    }

    @Test
    void missingInputFileIsReported(@TempDir Path temp) {
        Result result = run("--input", temp.resolve("missing.henshin").toString(), "--list");

        assertEquals(1, result.exitCode());
        assertTrue(result.err().contains("Input file does not exist"));
    }

    @Test
    void wrongInputExtensionIsReported() {
        Result result = run("--input", "notes.txt", "--list");

        assertEquals(1, result.exitCode());
        assertTrue(result.err().contains("Input file must end with .henshin"));
    }

    @Test
    void missingActionIsReported() {
        Result result = run("--input", HENSHIN);

        assertEquals(1, result.exitCode());
        assertTrue(result.err().contains("Nothing to do"));
    }

    @Test
    void unknownOptionIsReported() {
        Result result = run("--input", HENSHIN, "--colour", "red");

        assertEquals(1, result.exitCode());
        assertTrue(result.err().contains("Unknown argument: --colour"));
    }

    @Test
    void diagramFileIsFoundNextToTheInput() {
        Path diagram = HenshinSvgMain.findDiagramNextTo(BANK.resolve("bank.henshin"));

        assertEquals("bank.henshin_diagram", diagram.getFileName().toString());
    }

    @Test
    void worksWithoutADiagramFile(@TempDir Path temp) throws IOException {
        Files.copy(BANK.resolve("bank.henshin"), temp.resolve("bank.henshin"));
        Files.copy(BANK.resolve("bank.ecore"), temp.resolve("bank.ecore"));

        Result result = run("--input", temp.resolve("bank.henshin").toString(), "--rule", "createAccount",
                "--output", temp.resolve("out.svg").toString());

        assertEquals(0, result.exitCode(), result.err());
        assertTrue(result.out().contains("no .henshin_diagram file found"));
        assertTrue(Files.exists(temp.resolve("out.svg")));
    }

    @Test
    void oldPositionalArgumentsStillWork(@TempDir Path temp) {
        Path output = temp.resolve("old.svg");

        Result result = run(HENSHIN, BANK.resolve("bank.henshin_diagram").toString(), "createAccount", output.toString());

        assertEquals(0, result.exitCode(), result.err());
        assertTrue(Files.exists(output));
        assertTrue(result.out().contains("Nodes"));
    }
}
