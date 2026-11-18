package henshinIntegration;

/**
 Stores the visual position of a Henshin node from the .henshin_diagram file.
 - x/y: where Eclipse placed the node in the graphical diagram.
  - width/height: optional size values from the diagram file.
  - If width/height are missing, the visualizer estimates a compact box size.
 */
public record HenshinLayout(int x, int y, int width, int height) {

    public static HenshinLayout empty() {
        return new HenshinLayout(0, 0, 0, 0);
    }
}

/**
 Used when a Henshin node has no matching notation/layout entry.
 - x=0 and y=0 = no real Eclipse position known.
 - the visualizer then falls back to a simple automatic position.
 */