package henshinIntegration;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Command-line tool that exports Henshin rules as SVG files.
 * - lists the rules of a .henshin file (--list).
 * - exports one or more rules (--rule) or all rules (--all).
 * - uses the .henshin_diagram file next to the input for the Eclipse layout, if there is one.
 * - never overwrites files unless --overwrite is given.
 * - keeps going when one rule fails and reports a summary at the end.
 * Exit code: 0 when everything worked, 1 when there was any error.
 */
public class HenshinSvgMain {
    /**
     * starts the tool and ends the program with the exit code of {@link #run}
     * @param args the command-line arguments, see --help
     */
    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /**
     * runs the tool; separate from main so tests can call it without ending the JVM
     * @param args the command-line arguments
     * @param out where normal messages go
     * @param err where error messages go
     * @return 0 when everything worked, 1 when there was any error
     */
    public static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 0) {
            printHelp(out);
            return 1;
        }

        CliOptions options;
        try {
            options = CliOptions.parse(args);
        } catch (IllegalArgumentException e) {
            err.println("Error: " + e.getMessage());
            err.println("Use --help to see all options.");
            return 1;
        }

        if (options.help()) {
            printHelp(out);
            return 0;
        }

        HenshinRuleExtractor extractor = new HenshinRuleExtractor();
        List<String> availableRules;
        try {
            availableRules = extractor.listRules(options.input());
        } catch (IOException e) {
            err.println("Error: could not read " + options.input() + ": " + e.getMessage());
            return 1;
        }

        if (options.listRules()) {
            out.println("Rules in " + options.input().getFileName() + ":");
            if (availableRules.isEmpty()) {
                out.println("  (no rules found)");
            }
            for (String rule : availableRules) {
                out.println("  - " + rule);
            }
            return 0;
        }

        List<String> selectedRules = options.allRules() ? availableRules : options.rules();
        if (selectedRules.isEmpty()) {
            err.println("Error: " + options.input().getFileName() + " contains no rules.");
            return 1;
        }
        List<String> unknownRules = selectedRules.stream().filter(rule -> !availableRules.contains(rule)).toList();
        if (!unknownRules.isEmpty()) {
            err.println("Error: rule not found: " + String.join(", ", unknownRules));
            err.println("Available rules: " + String.join(", ", availableRules));
            return 1;
        }

        Path diagram = options.diagram() != null ? options.diagram() : findDiagramNextTo(options.input());
        if (diagram == null) {
            out.println("Note: no .henshin_diagram file found next to the input; nodes get automatic positions.");
        }

        List<Path> outputs;
        try {
            outputs = outputPaths(options, selectedRules);
        } catch (IllegalArgumentException e) {
            err.println("Error: " + e.getMessage());
            return 1;
        }

        return exportRules(options, diagram, selectedRules, outputs, extractor, out, err);
    }

    /**
     * exports every selected rule; a failing rule is reported and the others still run
     * @return 0 when all rules were exported, 1 otherwise
     */
    private static int exportRules(
            CliOptions options,
            Path diagram,
            List<String> rules,
            List<Path> outputs,
            HenshinRuleExtractor extractor,
            PrintStream out,
            PrintStream err) {
        HenshinSvgVisualizer visualizer = new HenshinSvgVisualizer();
        int failed = 0;

        for (int i = 0; i < rules.size(); i++) {
            String ruleName = rules.get(i);
            Path target = outputs.get(i);
            String progress = rules.size() > 1 ? "[%d/%d] ".formatted(i + 1, rules.size()) : "";

            if (Files.exists(target) && !options.overwrite()) {
                err.println(progress + "Skipped " + ruleName + ": " + target
                        + " already exists. Use --overwrite to replace it.");
                failed++;
                continue;
            }

            try {
                HenshinRule rule = extractor.extract(options.input(), diagram, ruleName);
                if (options.verbose()) {
                    printRule(rule, options.input(), diagram, out);
                }
                visualizer.save(rule, target);
                out.println(progress + "Exported " + ruleName + " -> " + target.toAbsolutePath());
            } catch (IOException | RuntimeException e) {
                err.println(progress + "Failed " + ruleName + ": " + e.getMessage());
                failed++;
            }
        }

        if (rules.size() > 1) {
            out.println("Done: " + (rules.size() - failed) + " of " + rules.size() + " rules exported.");
        }
        return failed == 0 ? 0 : 1;
    }

    /**
     * works out the output file for every selected rule
     * - --output ending in .svg: that file (only allowed for one rule).
     * - --output folder, or no --output: <model>_<rule>.svg inside that folder (default: current folder).
     * @param options the checked options
     * @param rules the selected rule names
     * @return one output path per rule, in the same order
     */
    static List<Path> outputPaths(CliOptions options, List<String> rules) {
        Path output = options.output();

        if (output != null && output.getFileName().toString().toLowerCase().endsWith(".svg")) {
            if (rules.size() != 1) {
                throw new IllegalArgumentException("--output is a single .svg file, but " + rules.size()
                        + " rules are exported. Give a folder instead.");
            }
            createFolder(output.toAbsolutePath().getParent());
            return List.of(output);
        }

        Path folder = output == null ? Path.of("") : output;
        if (output != null) {
            if (Files.exists(output) && !Files.isDirectory(output)) {
                throw new IllegalArgumentException("--output must be a .svg file or a folder: " + output);
            }
            createFolder(output);
        }

        String model = withoutExtension(options.input().getFileName().toString());
        List<Path> paths = new ArrayList<>();
        for (String rule : rules) {
            paths.add(folder.resolve(model + "_" + safeFileName(rule) + ".svg"));
        }
        return paths;
    }

    /**
     * looks for a .henshin_diagram file with the same name next to the .henshin file
     * @param input the .henshin file
     * @return the diagram file, or null if there is none
     */
    static Path findDiagramNextTo(Path input) {
        String name = withoutExtension(input.getFileName().toString()) + ".henshin_diagram";
        Path parent = input.toAbsolutePath().getParent();
        Path diagram = parent == null ? Path.of(name) : parent.resolve(name);
        return Files.isRegularFile(diagram) ? diagram : null;
    }

    private static void createFolder(Path folder) {
        if (folder == null) {
            return;
        }
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not create output folder " + folder + ": " + e.getMessage());
        }
    }

    private static String withoutExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    /**
     * replaces characters that are not safe in file names, so any rule name gives a valid file
     * @param name the rule name
     * @return the name with unsafe characters replaced by "_"
     */
    private static String safeFileName(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    /**
     * prints the command-line usage and examples
     * @param out where to print
     */
    private static void printHelp(PrintStream out) {
        out.println("""
                Usage: HenshinSvgMain --input <file.henshin> (--list | --rule <name> | --all) [options]

                Exports Henshin transformation rules as SVG diagrams, without Eclipse.

                Options:
                  -i, --input <file>     the .henshin file to read (required)
                  -l, --list             list the rules in the file
                  -r, --rule <name>      export this rule (repeat for several rules)
                  -a, --all              export all rules
                  -o, --output <path>    a .svg file (one rule) or a folder (several rules)
                                         default: <model>_<rule>.svg in the current folder
                  -d, --diagram <file>   the .henshin_diagram file with the Eclipse layout
                                         default: <model>.henshin_diagram next to the input, if it exists
                      --overwrite        replace output files that already exist
                  -v, --verbose          print the extracted nodes, edges, and conditions
                  -h, --help             show this help

                Examples:
                  HenshinSvgMain --input bank.henshin --list
                  HenshinSvgMain --input bank.henshin --rule transferMoney --output transfer.svg
                  HenshinSvgMain --input bank.henshin --all --output svg-out --overwrite

                Exit code: 0 when everything worked, 1 when there was an error.""");
    }

    /**
     * prints the extracted rule, its parameters, nodes, edges, and conditions so the extraction can be inspected
     * @param rule the extracted rule to print
     * @param henshinFile path of the source .henshin file
     * @param diagramFile path of the source .henshin_diagram file, may be null
     * @param out where to print
     */
    private static void printRule(HenshinRule rule, Path henshinFile, Path diagramFile, PrintStream out) {
        out.println("Henshin file : " + henshinFile.toAbsolutePath());
        out.println("Diagram file : " + (diagramFile == null ? "(none)" : diagramFile.toAbsolutePath()));
        out.println("Rule         : " + rule.name());
        out.printf("Rule layout  : x=%d y=%d w=%d h=%d%n",
                rule.layout().x(),
                rule.layout().y(),
                rule.layout().width(),
                rule.layout().height());
        out.println();

        out.println("Parameters");
        for (HenshinParameter parameter : rule.parameters()) {
            out.printf("- %s (%s, %s)%n", parameter.name(), emptyAsUnknown(parameter.kind()), emptyAsUnknown(parameter.type()));
        }
        out.println();

        out.println("Nodes");
        for (HenshinNode node : rule.nodes()) {
            out.printf("- %s%s %s :%s at x=%d y=%d w=%d h=%d%n",
                    actionLabel(node.action()),
                    node.multi() ? " [multi]" : "",
                    emptyAsUnnamed(node.name()),
                    emptyAsUnknown(node.type()),
                    node.layout().x(),
                    node.layout().y(),
                    node.layout().width(),
                    node.layout().height());

            for (HenshinAttribute attribute : node.attributes()) {
                out.printf("  attribute %s = %s%s%n",
                        emptyAsUnknown(attribute.name()),
                        emptyAsUnknown(attribute.value()),
                        attribute.changes() ? " -> " + attribute.newValue() : "");
            }
        }
        out.println();

        out.println("Edges");
        for (HenshinEdge edge : rule.edges()) {
            out.printf("- %s%s %s -> %s :%s%s%n",
                    actionLabel(edge.action()),
                    edge.multi() ? " [multi]" : "",
                    edge.sourceNodeId(),
                    edge.targetNodeId(),
                    emptyAsUnknown(edge.type()),
                    edge.containment() ? " [containment]" : "");
        }

        if (!rule.conditions().isEmpty()) {
            out.println();
            out.println("Conditions");
            for (HenshinCondition condition : rule.conditions()) {
                out.printf("- %s: %s%n", emptyAsUnknown(condition.name()), emptyAsUnknown(condition.text()));
            }
        }
        out.println();
    }

    /**
     * returns a plain-text stereotype label for an action, for console output
     * @param action the action to label
     * @return the action label such as "&lt;&lt;preserve&gt;&gt;"
     */
    private static String actionLabel(HenshinAction action) {
        return switch (action) {
            case PRESERVE -> "<<preserve>>";
            case CREATE -> "<<create>>";
            case DELETE -> "<<delete>>";
            case FORBID -> "<<forbid>>";
        };
    }

    private static String emptyAsUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private static String emptyAsUnnamed(String value) {
        return value == null || value.isBlank() ? "(unnamed)" : value;
    }
}
