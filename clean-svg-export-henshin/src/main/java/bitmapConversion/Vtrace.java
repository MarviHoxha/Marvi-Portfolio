package bitmapConversion;

import java.util.Map;

/**
 * Implementation of the VectorTracer Interface using the external conversion tool Vtracer
 */
public class Vtrace implements VectorTracer {
    /**
     * converts a png file to an svg file using the command line tool Vtracer
     * @param pngPath path to the png file
     * @param bitmapData base64 encoded String of png to be converted for key saving in list
     * @param list list of already converted or manually added vectorgraphics
     * @return the path to the svg file that was created or identified
     */
    @Override
    public String convertPNGtoSVG(String pngPath, String bitmapData, Map<String, String> list) {
        //wir gehen davon aus das wenn diese Methode aufgerufen wird dann auch schon gecheckt wurde das Datei noch nicht existiert
        String svgPath = list.size() + ".svg";
        try {
            String pathToVtracer = "./vtracer";
            ProcessBuilder pb = new ProcessBuilder(
                    pathToVtracer, "--input", pngPath, "--output", svgPath
            );
            Process process = pb.start();
            int exit = process.waitFor();
            if (exit == 0) {
                System.out.println("Conversion successful");
                //falls dem so ist wollen wir den Dateinamen mit seiner bitmapData in unsere Map schreiben
                list.put(bitmapData, svgPath);
            } else {
                System.out.println("Conversion failed");
            }
            return svgPath;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * checks if Vtracer is aviable and usable in the current envoirment
     * @return true if Vtracer is aviable and usable, false if not
     */
    @Override
    public boolean checkVectorTracerCompatibility() {
        try {
            String pathToVtracer = "./vtracer";
            ProcessBuilder pb = new ProcessBuilder(
                    pathToVtracer, "--version"
            );
            Process process = pb.start();
            int exit = process.waitFor();
            if (exit == 0) {
                System.out.println("VTracer is installed");
                return true;
            } else {
                System.out.println("VTracer is not installed");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
