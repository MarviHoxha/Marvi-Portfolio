package bitmapConversion;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

/**
 * handels file reads and writes
 */
public class IO {
    /**
     * loads a given SVG file to a live Document
     * @param path path of the SVG file
     * @return live document of the loaded SVG
     */
    public static Document loadSVG(String path) {
        try {
            File svgFile = new File(path);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(svgFile);
            document.getDocumentElement().normalize();
            return document;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //updated basiernedn auf den liveänderungen die wir gemacht haben

    /**
     * updates the loaded SVG file with the changes of the live document
     * @param document the live document to be written to the SVG file
     * @param filePath path of the SVG file
     */
    public static void updateDocument(Document document, String filePath) {
        try{
            Transformer transformer = TransformerFactory.newInstance().newTransformer();

        transformer.transform(
                new DOMSource(document),
                new StreamResult(new File(filePath))
        );} catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * removes all bitmaps in a given NodeList / Document and therefor in the SVG file
     * @param nodes NodeList in which the Bitmaps are located
     */
    public static void removeBitmaps(NodeList nodes) {
        //Da es eine live Liste ist, würde er indexe überspringen, deshalb von hinten anfangen
        for(int i = nodes.getLength() - 1; i >= 0 ; i--) {
            Element element = (Element) nodes.item(i);
            element.getParentNode().removeChild(element);
        }
    }
}
