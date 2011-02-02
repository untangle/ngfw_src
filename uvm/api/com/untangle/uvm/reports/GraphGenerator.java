/*
 * $HeadURL: svn://chef/branch/prod/hellokitty/work/src/uvm/api/com/untangle/uvm/RemoteReportingManager.java $
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
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