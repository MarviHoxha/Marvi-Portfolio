package bitmapConversion;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * handels extraction of data from SVG files
 */
public class Extractor {
    /**
     * extracts nodes from a document / SVG via tag name
     * @param document the document from which it should be extracted
     * @param tag the tag of the data to be extracted
     * @return a node list containing all nodes with the given tag name inside the document
     */
    public static NodeList extractByTag(Document document, String tag) {
        NodeList nodes = document.getElementsByTagName(tag);
        return nodes;
    }

    /**
     * extracts the x coordinate of a node, specifcally used for bitmap coordinates
     * @param nodes list of all bitmaps in the document
     * @param index
     * @return X coordinate of the bitmap as integer
     */
    public static int extractX(NodeList nodes, int index) {
        Element element = (Element) nodes.item(index);
        String x = element.getAttribute("x");
        return Integer.parseInt(x);
    }
    /**
     * extracts the y coordinate of a node, specifcally used for bitmap coordinates
     * @param nodes list of all bitmaps in the document
     * @param index
     * @return Y coordinate of the bitmap as integer
     */
    public static int extractY(NodeList nodes, int index) {
        Element element = (Element) nodes.item(index);
        String y = element.getAttribute("y");
        return Integer.parseInt(y);
    }

    /**
     * takes a bitmap inside a document / SVG and isolates the raw base64 encoding of the bitmap
     * @param nodes list of all bitmaps in the document
     * @param index
     * @return cleaned raw base64 encoding of the bitmap as String
     */
    public static String getBitmapData(NodeList nodes, int index) {
        String data = (((Element) nodes.item(index)).getAttribute("xlink:href"));
        //regEx um data:image/png;base64, zu entfernen
        Pattern pattern = Pattern.compile("(,)(.*)", Pattern.DOTALL);
        // &#10; Zeilenumbrüche innerhalb der data
        Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            //Zeilenumbrüche könnte beim Decodieren stören.
            String cleaned = matcher.group(2).replaceAll("\\R", "");
            return cleaned;
        }
        return null;
    }

    /**
     * extracts Integers from the transform tag of a path, specifically used for the translation of the vector graphics
     * @param transform String that contains the integer coordinates
     * @return an array of integers, with x and y coodinate of a path
     */
    public static int[] extractIntsFromTransform(String transform) {
        Pattern pattern = Pattern.compile("(translate\\()([0-9])\\,([0-9])\\)");
        Matcher matcher = pattern.matcher(transform);
        if (matcher.find()) {
            String x = matcher.group(2);
            String y = matcher.group(3);
            int[] results = new int[2];
            results[0] = Integer.parseInt(x);
            results[1] = Integer.parseInt(y);
            return results;
        }
        return null;
    }

    /**
     * provides the file path of the SVG file that a given bitmap is to be replaced with
     * @param nodes list of all bitmaps in the document
     * @param i
     * @param list the constant dictionary keeping track of all already converted bitmaps
     * @param vectorTracer the vectorTracer that should be used for bitmap conversion
     * @return the file path of the SVG file that a given bitmap is to be replaced with
     */
    static String getVectorGraphic(NodeList nodes, int i, VectorGraphicList list, VectorTracer vectorTracer) {
        String bitmapData = getBitmapData(nodes, i);
        System.out.println(bitmapData);
        System.out.println(Converter.decodebase64(bitmapData));
        String pathOfSVG = VectorGraphicList.bitmapDataLookUp(bitmapData, list.convertedBitmaps);
        if (pathOfSVG == null && vectorTracer.checkVectorTracerCompatibility()) {
            pathOfSVG = vectorTracer.convertPNGtoSVG("decodedImg.png", bitmapData, list.convertedBitmaps);
        } else {
            System.out.println("lade Bitmap aus Liste");
        }
        return pathOfSVG;
    }
}
