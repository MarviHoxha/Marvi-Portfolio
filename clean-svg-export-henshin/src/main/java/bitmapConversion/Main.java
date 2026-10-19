package bitmapConversion;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;


public class Main {
    public static void main(String[] args) {
        Document doc = IO.loadSVG("bankTest.svg");
        System.out.println("Root-Element: " + doc.getDocumentElement().getNodeName());
        NodeList nodes = Extractor.extractByTag(doc, "image");
        VectorGraphicList list = new VectorGraphicList();
        VectorTracer vtrace = new Vtrace();
        Converter.convertAllBitmapsToVectorGraphics(nodes, list, doc, vtrace);
        IO.removeBitmaps(nodes);
        IO.updateDocument(doc, "updatedImg.svg");
        //die Liste mit den bitmaps speichern
        list.saveList();

    }
}

//TODO: Was passiert wenn als pathofSVG einfach null rauskommt, wird dann das Icon einfach nicht gemalt oder stützt das Program ab ?

//Was ist wenn vtracer nicht funktioniert, nicht installiert ist / insttallierbar ist ? Dann muss das automatisch deaktiviert werden
//vtracer moduar als interface, damit falls es doch noch etwas besseres gibt, damit es dann einfacher ist es auszutauschen
//hamgrest wohl so ein Projekt um henshin files direkt in SVG konvertieren zu können
//dahinterstehende Grafik Plugins nochmal analysieren, ob die nicht headless funktionalität zum SVG export haben könnten
//einzelne Module aufteilen z.B. für Textboxen, CLi interface, so dass diese komplett unabhänig voneinander funktionieren
//Es gibt eine Library die die argumente die man ja nacher beim CLI Interface mit dem Nutzer braucht deutlich vereinfacht
//Jetzt schon sollten für die einzelenen Module Interfaces geschrieben werden, auch z.B. für dieses damit wir dann direkt mit den Interfaces entwicklen und die Module nachher auch zusammenpassen
