package svgGeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SvgDemoMainTest {
    @Test
    void generatesSvgFileFromCommandLine(@TempDir Path tempDirectory) throws Exception {
        Path outputPath = tempDirectory.resolve("diagram.svg");

        ProcessResult result = runSvgDemoMain("--output", outputPath.toString(), "--overwrite");

        assertEquals(0, result.exitCode());
        assertTrue(Files.exists(outputPath));
        assertTrue(Files.readString(outputPath).contains("Rule: ExampleTransformation"));
    }

    @Test
    void generatedSvgContainsExpectedDiagramContent(@TempDir Path tempDirectory) throws Exception {
        Path outputPath = tempDirectory.resolve("diagram.svg");

        runSvgDemoMain("--output", outputPath.toString(), "--overwrite");

        String svg = Files.readString(outputPath);

        assertTrue(svg.contains("<svg"));
        assertTrue(svg.contains("marker id=\"arrowhead\""));
        assertTrue(svg.contains("Rule: ExampleTransformation"));
        assertTrue(svg.contains("LHS pattern"));
        assertTrue(svg.contains("Rule action"));
        assertTrue(svg.contains("RHS result"));
        assertTrue(svg.contains("matches"));
        assertTrue(svg.contains("creates"));
    }

    @Test
    void printsHelpFromCommandLine() throws Exception {
        ProcessResult result = runSvgDemoMain("--help");

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("Usage: SvgDemoMain"));
    }

    @Test
    void rejectsOutputPathWithoutSvgExtension() throws Exception {
        ProcessResult result = runSvgDemoMain("--output", "wrong-file.txt");

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("Output file must end with .svg"));
    }

    @Test
    void rejectsMissingInputFile(@TempDir Path tempDirectory) throws Exception {
        Path missingInputPath = tempDirectory.resolve("missing.henshin");
        Path outputPath = tempDirectory.resolve("diagram.svg");

        ProcessResult result = runSvgDemoMain(
                "--input", missingInputPath.toString(),
                "--output", outputPath.toString());

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("Input file does not exist"));
    }

    @Test
    void rejectsExistingOutputWithoutOverwrite(@TempDir Path tempDirectory) throws Exception {
        Path outputPath = tempDirectory.resolve("diagram.svg");
        Files.writeString(outputPath, "already here");

        ProcessResult result = runSvgDemoMain("--output", outputPath.toString());

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("Output file already exists"));
        assertTrue(result.output().contains("Use --overwrite to replace it."));
    }

    @Test
    void replacesExistingOutputWithOverwrite(@TempDir Path tempDirectory) throws Exception {
        Path outputPath = tempDirectory.resolve("diagram.svg");
        Files.writeString(outputPath, "already here");

        ProcessResult result = runSvgDemoMain("--output", outputPath.toString(), "--overwrite");

        assertEquals(0, result.exitCode());
        assertTrue(result.output().contains("SVG generated"));
        assertTrue(Files.readString(outputPath).contains("Rule: ExampleTransformation"));
    }

    private ProcessResult runSvgDemoMain(String... arguments) throws Exception {
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classPath = Path.of("build", "classes", "java", "main").toString();

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command().add(javaExecutable);
        processBuilder.command().add("-cp");
        processBuilder.command().add(classPath);
        processBuilder.command().add("svgGeneration.SvgDemoMain");
        processBuilder.command().addAll(java.util.List.of(arguments));
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();

        return new ProcessResult(exitCode, output);
    }

    private record ProcessResult(int exitCode, String output) {
    }
}
