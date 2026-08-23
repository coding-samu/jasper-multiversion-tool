package it.jmt.worker611;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.xml.JRXmlWriter;
import java.io.File;

public class Main {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("FATAL: Parametri mancanti. Richiesti: [compile|decompile] [source] [dest]");
            System.exit(1);
        }

        String action = args[0];
        String source = args[1];
        String dest = args[2];

        try {
            if ("compile".equals(action)) {
                JasperCompileManager.compileReportToFile(source, dest);
                System.out.println("SUCCESS");
            } else if ("decompile".equals(action)) {
                JasperReport report = (JasperReport) JRLoader.loadObject(new File(source));
                JRXmlWriter.writeReport(report, dest, "UTF-8");
                System.out.println("SUCCESS");
            } else {
                System.err.println("FATAL: Azione sconosciuta: " + action);
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }
}
