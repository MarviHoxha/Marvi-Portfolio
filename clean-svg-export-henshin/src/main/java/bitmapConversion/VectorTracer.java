package bitmapConversion;

import java.util.Map;

/**
 * Defines the functionality for a vector tracer that can convert PNG images to SVG format.
 */
public interface VectorTracer {
    public String convertPNGtoSVG(String pngPath, String bitmapData, Map<String, String> list);

    public boolean checkVectorTracerCompatibility();
}
