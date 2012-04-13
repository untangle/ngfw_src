/**
 * $Id: GraphGenerator.java,v 1.00 2012/04/12 22:21:53 dmorris Exp $
 */
package com.untangle.uvm.reports;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class GraphGenerator
{
    private static final Logger logger = Logger.getLogger(GraphGenerator.class);

    private static String reportBase;

    public static void main(String[] args)
        throws IOException
    {
        if (args.length < 2) {
            System.err.println("usage: GraphGenerator reportBase dateBase");
            System.exit(-1);
        }

        reportBase = args[0];
        String dateBase = args[1];
        traverse(new File(reportBase + "/" + dateBase));
    }

    private static void traverse(File dir)
    {
        for (File f : dir.listFiles()) {
            if (f.isDirectory() && !f.getName().equals(".") && !f.getName().equals("..")) {
                traverse(f);
            } else if (f.getName().equals("report.xml")) {
                ReportXmlHandler h = new ReportXmlHandler();
                try {
                    logger.info("processing: " + f);
                    FileInputStream fis = new FileInputStream(f);
                    XMLReader xr = XMLReaderFactory.createXMLReader();
                    xr.setContentHandler(h);
                    xr.parse(new InputSource(fis));
                    generateGraphs(h.getReport());
                } catch (SAXException exn) {
                    logger.warn("could not read report.xml", exn);
                } catch (IOException exn) {
                    logger.warn("could not read report.xml", exn);
                }
             }
         }
    }

    private static void generateGraphs(ApplicationData ad)
    {
        for (Section s : ad.getSections()) {
            if (s instanceof SummarySection) {
                SummarySection ss = (SummarySection)s;
                for (SummaryItem si : ss.getSummaryItems()) {
                    if (si instanceof Chart) {
                        Chart c = (Chart)si;
                        try {
                            c.generate(reportBase);
                        } catch (Exception exn) {
                            logger.warn("Could not generate graph for " + c.getName(), exn);
                        }
                    }
                }
            }
        }
    }
}