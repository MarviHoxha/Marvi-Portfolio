package svgGeneration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Small demo entry point for generating an SVG from scratch.
 */
public class SvgDemoMain {
    public static void main(String[] args) throws IOException {
        // This is the first tiny CLI behavior: --help prints instructions instead of creating a file.
        if (args.length > 0 && "--help".equals(args[0])) {
            printHelp();
            return;
        }

        CliOptions options;
        try {
            options = parseOptions(args);
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
            printHelp();
            return;
        }

        // In the future this data should come from a real Henshin rule.
        // For now we create a small example diagram by hand.
        SvgDiagram diagram = new SvgDiagram("Rule: ExampleTransformation");
        diagram.addNode(new SvgNode("lhs", "LHS pattern", SvgNodeType.PATTERN));
        diagram.addNode(new SvgNode("rule", "Rule action", SvgNodeType.ACTION));
        diagram.addNode(new SvgNode("rhs", "RHS result", SvgNodeType.RESULT));
        diagram.addEdge(new SvgEdge("lhs", "rule", "matches"));
        diagram.addEdge(new SvgEdge("rule", "rhs", "creates"));

        // Places the nodes in one row so the demo does not need manual x/y coordinates.
        diagram.applyHorizontalLayout(40, 80, 50);

        SvgGenerator generator = new SvgGenerator(580, 220);
        generator.addDiagram(diagram);

        if (Files.exists(options.outputPath()) && !options.overwrite()) {
            System.out.println("Error: Output file already exists: " + options.outputPath());
            System.out.println("Use --overwrite to replace it.");
            return;
        }

        generator.saveToFile(options.outputPath());

        if (options.inputPath() != null) {
            System.out.println("Input path accepted: " + options.inputPath().toAbsolutePath());
            System.out.println("Note: Henshin input parsing is not connected yet.");
        }
        System.out.println("SVG generated: " + options.outputPath().toAbsolutePath());
    }

    private static CliOptions parseOptions(String[] args) {
        Path inputPath = null;
        Path outputPath = getDefaultOutputPath(args);
        boolean overwrite = false;

        for (int i = 0; i < args.length; i++) {
            String argument = args[i];
            if ("--input".equals(argument)) {
                inputPath = readPathArgument(args, ++i, "--input");
            } else if ("--output".equals(argument)) {
                outputPath = readPathArgument(args, ++i, "--output");
            } else if ("--overwrite".equals(argument)) {
                overwrite = true;
            } else if (!argument.startsWith("--") && args.length == 1) {
                outputPath = Path.of(argument);
            } else {
                throw new IllegalArgumentException("Unknown argument: " + argument);
            }
        }

        validateOutputPath(outputPath);
        validateInputPath(inputPath);
        return new CliOptions(inputPath, outputPath, overwrite);
    }

    private static Path getDefaultOutputPath(String[] args) {
        // If no file name is given, use a default demo output file.
        if (args.length == 0) {
            return Path.of("generated-rule.svg");
        }
        return Path.of("generated-rule.svg");
    }

    private static Path readPathArgument(String[] args, int index, String optionName) {
        if (index >= args.length) {
            throw new IllegalArgumentException(optionName + " needs a path after it.");
        }
        return Path.of(args[index]);
    }

    private static void validateOutputPath(Path outputPath) {
        String fileName = outputPath.getFileName().toString().toLowerCase();
        if (!fileName.endsWith(".svg")) {
            throw new IllegalArgumentException("Output file must end with .svg: " + outputPath);
        }
    }

    private static void validateInputPath(Path inputPath) {
        if (inputPath == null) {
            return;
        }

        String fileName = inputPath.getFileName().toString().toLowerCase();
        if (!fileName.endsWith(".henshin")) {
            throw new IllegalArgumentException("Input file must end with .henshin: " + inputPath);
        }
        if (!Files.exists(inputPath)) {
            throw new IllegalArgumentException("Input file does not exist: " + inputPath);
        }
    }

    private static void printHelp() {
        System.out.println("Usage: SvgDemoMain [--input input-file.henshin] [--output output-file.svg]");
        System.out.println("       SvgDemoMain [output-file.svg]");
        System.out.println("Creates a simple demo SVG diagram from scratch.");
        System.out.println("If no output file is given, generated-rule.svg is used.");
        System.out.println("Use --overwrite to replace an existing output file.");
        System.out.println("The input path is accepted now, but real Henshin parsing will be connected later.");
    }

    private record CliOptions(Path inputPath, Path outputPath, boolean overwrite) {
    }
}
