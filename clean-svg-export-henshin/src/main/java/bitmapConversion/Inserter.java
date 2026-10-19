package bitmapConversion;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * handles insertions into Document / SVG file
 */
public class Inserter {
    /**
     * loads a given SVG file, of the already converted bitmap, and adds those paths to the document
     * @param pathOfSVG path of the SVG of the converted bitmap
     * @param doc the document of the SVG we would like to edit
     * @param nodes list of all the bitmaps in doc
     * @param i current bitmap inside the node list we are working on
     */
    static void insertPaths(String pathOfSVG, Document doc, NodeList nodes, int i) {
        //egal welches, das jeweilige SVG laden
        Document docNewSVG = IO.loadSVG(pathOfSVG);
        //SVG extrahieren
        NodeList vectorData = Extractor.extractByTag(docNewSVG, "path");
        //manche vektorIcons werden auch mehrere paths haben.
        for(int j = 0; j < vectorData.getLength(); j++) {
            Node source = vectorData.item(j);
            //true heißt mit seinen Attributen (d,fill,transform,...)
            Node importedNode = doc.importNode(source, true);
            String transform = importedNode.getAttributes().getNamedItem("transform").getNodeValue();
            int[] coord = Extractor.extractIntsFromTransform(transform);
            coord[0] = coord[0] + Extractor.extractX(nodes, i);
            coord[1] = coord[1] + Extractor.extractY(nodes, i);
            String newTransform = "translate(" + coord[0] + "," + coord[1] + ")";
            importedNode.getAttributes().getNamedItem("transform").setNodeValue(newTransform);
            Node ElterKnoten = doc.getElementsByTagName("svg").item(0);
            //und dann das neue Element unter dem Elternknoten SVG abspeichern
            ElterKnoten.appendChild(importedNode);
        }
    }
}
