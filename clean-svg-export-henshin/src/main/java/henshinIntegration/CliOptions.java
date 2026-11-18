package henshinIntegration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The command-line options of HenshinSvgMain.
 * - input: the .henshin file to read.
 * - diagram: the .henshin_diagram file with the Eclipse layout, or null to look for one next to the input.
 * - rules: rule names given with --rule (can be several).
 * - allRules: true for --all.
 * - listRules: true for --list.
 * - output: a .svg file (one rule) or a folder (several rules), or null for the current folder.
 * - overwrite: true when existing output files may be replaced.
 * - verbose: true when the extracted rule details should be printed.
 * - help: true for --help; all other fields can then be empty.
 */
public record CliOptions(
        Path input,
        Path diagram,
        List<String> rules,
        boolean allRules,
        boolean listRules,
        Path output,
        boolean overwrite,
        boolean verbose,
        boolean help) {

    /**
     * reads and checks the command-line arguments
     * - new style: --input bank.henshin --rule transferMoney --output transfer.svg
     * - old style (still supported): bank.henshin bank.henshin_diagram transferMoney [transfer.svg]
     * @param args the arguments from main
     * @return the checked options
     * @throws IllegalArgumentException with a clear message if the arguments are wrong
     */
    public static CliOptions parse(String[] args) {
        if (isOldStyle(args)) {
            return parseOldStyle(args);
        }

        Path input = null;
        Path diagram = null;
        List<String> rules = new ArrayList<>();
        boolean allRules = false;
        boolean listRules = false;
        Path output = null;
        boolean overwrite = false;
        boolean verbose = false;

        for (int i = 0; i < args.length; i++) {
            String argument = args[i];
            switch (argument) {
                case "--help", "-h" -> {
                    return new CliOptions(null, null, List.of(), false, false, null, false, false, true);
                }
                case "--input", "-i" -> input = Path.of(readValue(args, ++i, argument));
                case "--diagram", "-d" -> diagram = Path.of(readValue(args, ++i, argument));
                case "--rule", "-r" -> rules.add(readValue(args, ++i, argument));
                case "--all", "-a" -> allRules = true;
                case "--list", "-l" -> listRules = true;
                case "--output", "-o" -> output = Path.of(readValue(args, ++i, argument));
                case "--overwrite" -> overwrite = true;
                case "--verbose", "-v" -> verbose = true;
                default -> throw new IllegalArgumentException("Unknown argument: " + argument);
            }
        }

        if (input == null) {
            throw new IllegalArgumentException("Missing input file. Use --input <file.henshin>.");
        }
        checkFile(input, ".henshin", "Input file");
        if (diagram != null) {
            checkFile(diagram, ".henshin_diagram", "Diagram file");
        }

        int actions = (listRules ? 1 : 0) + (allRules ? 1 : 0) + (rules.isEmpty() ? 0 : 1);
        if (actions == 0) {
            throw new IllegalArgumentException("Nothing to do. Use --list, --rule <name>, or --all.");
        }
        if (actions > 1) {
            throw new IllegalArgumentException("Use only one of --list, --rule, or --all at a time.");
        }

        return new CliOptions(input, diagram, List.copyOf(rules), allRules, listRules, output, overwrite, verbose, false);
    }

    /**
     * tells whether the arguments use the old positional form: henshin diagram rule [output]
     * @param args the arguments from main
     * @return true if there are 3 or 4 arguments and none of them is an option
     */
    private static boolean isOldStyle(String[] args) {
        if (args.length < 3 || args.length > 4) {
            return false;
        }
        for (String argument : args) {
            if (argument.startsWith("-")) {
                return false;
            }
        }
        return true;
    }

    /**
     * reads the old positional form, which always printed the rule details
     * @param args henshin file, diagram file, rule name, optional output file
     * @return the checked options
     */
    private static CliOptions parseOldStyle(String[] args) {
        Path input = Path.of(args[0]);
        Path diagram = Path.of(args[1]);
        checkFile(input, ".henshin", "Input file");
        checkFile(diagram, ".henshin_diagram", "Diagram file");
        Path output = args.length == 4 ? Path.of(args[3]) : null;
        return new CliOptions(input, diagram, List.of(args[2]), false, false, output, true, true, false);
    }

    /**
     * returns the value that follows an option, for example the path after --input
     * @param args all arguments
     * @param index position of the value
     * @param option the option name, used in the error message
     * @return the value
     */
    private static String readValue(String[] args, int index, String option) {
        if (index >= args.length || args[index].startsWith("--")) {
            throw new IllegalArgumentException(option + " needs a value after it.");
        }
        return args[index];
    }

    /**
     * checks that a file has the right extension and exists
     * @param path the file to check
     * @param extension the required extension, for example ".henshin"
     * @param label how the file is called in error messages
     */
    private static void checkFile(Path path, String extension, String label) {
        if (!path.getFileName().toString().toLowerCase().endsWith(extension)) {
            throw new IllegalArgumentException(label + " must end with " + extension + ": " + path);
        }
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException(label + " does not exist: " + path);
        }
    }
}
