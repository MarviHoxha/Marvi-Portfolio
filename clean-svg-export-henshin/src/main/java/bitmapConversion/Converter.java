package bitmapConversion;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Base64;

/**
 * contains methods that convert Elements
 */
public class Converter {
    /**
     * decodes base64 and writes the result into a .png file
     * @param bitmapData the base64 data to be decoded
     * @return boolean that says if decoding has worked sucessfully
     */
    public static boolean decodebase64(String bitmapData) {
        byte[] decodedBytes = Base64.getDecoder().decode(bitmapData);
        try(OutputStream os = new FileOutputStream("decodedImg.png")) {
            os.write(decodedBytes);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * main method handeling the conversion from bitmap to vector graphic
     * @param nodes list of bitmaps
     * @param list dictonary of existing already converted bitmaps
     * @param doc document we are editing
     * @param vectorTracer the Tracer to be used to convert from bitmap to vector graphic
     */
    static void convertAllBitmapsToVectorGraphics(NodeList nodes, VectorGraphicList list, Document doc, VectorTracer vectorTracer) {
        for (int i = 0; i < nodes.getLength(); i++) {
            String pathOfSVG = Extractor.getVectorGraphic(nodes, i, list, vectorTracer);
            Inserter.insertPaths(pathOfSVG, doc, nodes, i);
        }
    }
}
