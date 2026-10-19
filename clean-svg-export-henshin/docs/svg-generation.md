# SVG Generation Prototype

This module creates a simple SVG diagram from structured Java data. It is the
first prototype for the SRS requirement that the system shall generate SVG
output.

## Current Purpose

The current implementation does not parse real Henshin files yet. Instead, it
builds a small example diagram in Java and exports it as SVG.

The prototype demonstrates that we can:

- create SVG files from scratch
- represent diagram data as nodes and edges
- apply a simple horizontal layout
- style different node types with different colors
- write the SVG to a configurable output path
- validate CLI input
- avoid overwriting files unless `--overwrite` is used
- validate generated SVG as XML before saving

## Main Classes

- `SvgDemoMain`: command-line entry point for the demo prototype
- `SvgDiagram`: stores the diagram title, nodes, and edges
- `SvgNode`: represents one labeled box in the diagram
- `SvgEdge`: represents one connection between two nodes
- `SvgNodeType`: defines semantic node types and their colors
- `SvgGenerator`: converts the diagram model into SVG XML

## Example Commands

Run with the default output file:

```powershell
java -cp build\classes\java\main svgGeneration.SvgDemoMain
```

Run with explicit input and output paths:

```powershell
java -cp build\classes\java\main svgGeneration.SvgDemoMain --input example.henshin --output generated-rule.svg
```

Overwrite an existing output file:

```powershell
java -cp build\classes\java\main svgGeneration.SvgDemoMain --output generated-rule.svg --overwrite
```

Show help:

```powershell
java -cp build\classes\java\main svgGeneration.SvgDemoMain --help
```

## Requirement Status

This prototype contributes to the following SRS requirements:

- SRS-02: accepts input and output paths at CLI level
- SRS-03: provides a help command
- SRS-04: validates CLI input
- SRS-05: shows clear error messages
- SRS-08: validates input file existence
- SRS-16: generates SVG output
- SRS-32/SRS-34: applies basic layout and spacing
- SRS-40: validates generated SVG as XML before saving
- SRS-41: saves generated output and prints the output path
- SRS-42: prevents accidental overwriting unless `--overwrite` is used
- SRS-52: separates diagram data, layout, and SVG generation logic
- SRS-53: includes automated CLI tests

## Limitations

The current prototype is not the full Henshin export pipeline.

Still missing:

- real `.henshin` parsing
- transformation rule selection
- Henshin/EMF/GMF integration
- real layout extraction from Henshin models
- advanced SVG layout correction
- full SVG standard validation beyond XML parsing
