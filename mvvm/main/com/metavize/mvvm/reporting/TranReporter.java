/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.reporting;


import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.jar.*;

import com.metavize.mvvm.engine.MvvmTransformHandler;
import com.metavize.mvvm.reporting.summary.*;
import com.metavize.mvvm.security.Tid;
import com.metavize.mvvm.tran.Scanner;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.*;
import org.apache.log4j.Logger;
import org.jfree.chart.*;
import org.xml.sax.*;
import org.xml.sax.helpers.XMLReaderFactory;


public class TranReporter {

    public static final float CHART_QUALITY_JPEG = .9f;  // for JPEG
    public static final int CHART_COMPRESSION_PNG = 0;  // for PNG
    public static final int CHART_WIDTH = 600;
    public static final int CHART_HEIGHT = 200;

    private static final Logger logger = Logger.getLogger(Reporter.class);
    public static final String ICON_DESC = "IconDesc42x42.png";
    public static final String ICON_ORG = "IconOrg42x42.png";
    private static final String SUMMARY_FRAGMENT_DAILY = "sum-daily.html";
    private static final String SUMMARY_FRAGMENT_WEEKLY = "sum-weekly.html";
    private static final String SUMMARY_FRAGMENT_MONTHLY = "sum-monthly.html";

    private final String tranName;
    private final URLClassLoader ucl;
    private final JarFile jf;
    private final File tranDir;

    private Map<String,Object> extraParams = new HashMap<String,Object>();

    TranReporter(File outputDir, String tranName, JarFile jf, URLClassLoader ucl)
    {
        this.tranName = tranName;
        // These next two are the same, just different forms.
        this.jf = jf;
        this.ucl = ucl;

        tranDir = new File(outputDir, tranName);
    }


    public void process(Connection conn) throws Exception
    {
        tranDir.mkdir();

        File imagesDir = new File(tranDir, "images");
        File globalImagesDir = new File(tranDir, "../images");
        MvvmTransformHandler mth = new MvvmTransformHandler(null);
        Scanner scanner = null;

        InputStream is = ucl.getResourceAsStream("META-INF/report-files");
        if (null == is) {
            logger.warn("No reports for: " + ucl.getURLs()[0]);
            return;
        } else {
            logger.info("Beginning generation for: " + tranName);
        }

        // Need to do the parameters & scanners first.
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        for (String line = br.readLine(); null != line; line = br.readLine()) {
            if (line.startsWith("#"))
                continue;
            StringTokenizer tok = new StringTokenizer(line, ":");
            if (!tok.hasMoreTokens()) { continue; }
            String resourceOrClassname = tok.nextToken();
            if (!tok.hasMoreTokens()) { continue; }
            String type = tok.nextToken();
            if (type.equalsIgnoreCase("parameter")) {
                String paramName = resourceOrClassname;
                if (!tok.hasMoreTokens())
                    throw new Error("Missing parameter value");
                // XXX String only right now.
                String paramValue = tok.nextToken();
                extraParams.put(paramName, paramValue);
            }
            else if (type.equalsIgnoreCase("scanner")) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                try {
                    Class scannerClass = cl.loadClass(resourceOrClassname);
                    scanner = (Scanner) scannerClass.newInstance();
                } catch (Exception x) {
                    logger.warn("No such class: " + resourceOrClassname);
                }
                // Ugly Constants. XXXX
                extraParams.put("scanner", scanner);
                extraParams.put("virusVendor", scanner.getVendorName());
                extraParams.put("spamVendor", scanner.getVendorName());
            }
        } 
        is.close();

        // Now do everything else.
        is = ucl.getResourceAsStream("META-INF/report-files");
        br = new BufferedReader(new InputStreamReader(is));
        for (String line = br.readLine(); null != line; line = br.readLine()) {
            if (line.startsWith("#"))
                continue;
            StringTokenizer tok = new StringTokenizer(line, ":");
            if (!tok.hasMoreTokens()) { continue; }
            String resourceOrClassname = tok.nextToken();
            if (!tok.hasMoreTokens()) { continue; }
            String type = tok.nextToken();
            if (type.equalsIgnoreCase("parameter") ||
                type.equalsIgnoreCase("scanner")) {
                continue;
            } else if (type.equalsIgnoreCase("summarizer")) {
                String className = resourceOrClassname;
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                try {
                    Class reportClass = cl.loadClass(className);
                    ReportSummarizer reportSummarizer;
                    reportSummarizer = (ReportSummarizer) reportClass.newInstance();
                    logger.debug("Found summarizer: " + className);
                    String dailyFile = new File(tranDir, SUMMARY_FRAGMENT_DAILY).getCanonicalPath();
                    processReportSummarizer(reportSummarizer, conn, dailyFile, Util.lastday, Util.midnight);
                    reportSummarizer = (ReportSummarizer) reportClass.newInstance();
                    String weeklyFile = new File(tranDir, SUMMARY_FRAGMENT_WEEKLY).getCanonicalPath();
                    processReportSummarizer(reportSummarizer, conn, weeklyFile, Util.lastweek, Util.midnight);
                    reportSummarizer = (ReportSummarizer) reportClass.newInstance();
                    String monthlyFile = new File(tranDir, SUMMARY_FRAGMENT_MONTHLY).getCanonicalPath();
                    processReportSummarizer(reportSummarizer, conn, monthlyFile, Util.lastmonth, Util.midnight);
                } catch (Exception x) {
                    logger.error("Unable to summarize", x);
                }
            }
            else if (type.equalsIgnoreCase("summaryGraph")) {
                String className = resourceOrClassname;
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                String outputName = type;
                try {
                    Class reportClass = cl.loadClass(className);
                    ReportGraph reportGraph;
                    String name = tok.nextToken();
                    reportGraph = (ReportGraph) reportClass.newInstance();
		    reportGraph.setExtraParams(extraParams);
                    logger.debug("Found graph: " + className);
                    String dailyFile = new File(tranDir, name + "--daily.png").getCanonicalPath();
                    processReportGraph(reportGraph, conn, dailyFile, Util.REPORT_TYPE_DAILY, Util.lastday, Util.midnight);
                    String weeklyFile = new File(tranDir, name + "--weekly.png").getCanonicalPath();
                    processReportGraph(reportGraph, conn, weeklyFile, Util.REPORT_TYPE_WEEKLY, Util.lastweek, Util.midnight);
                    String monthlyFile = new File(tranDir, name + "--monthly.png").getCanonicalPath();
                    processReportGraph(reportGraph, conn, monthlyFile, Util.REPORT_TYPE_MONTHLY, Util.lastmonth, Util.midnight);
                } catch (Exception x) {
                    logger.error("Unable to generate summary graph", x);
		    x.printStackTrace();
                }
            }
            else {
                String resource = resourceOrClassname;
                String outputName = type;
                String outputFile = new File(tranDir, outputName).getCanonicalPath();
                String outputImages = globalImagesDir.getCanonicalPath();
                processReport(resource, conn, outputFile + "--daily", outputImages, Util.lastday, Util.midnight);
                processReport(resource, conn, outputFile + "--weekly", outputImages, Util.lastweek, Util.midnight);
                processReport(resource, conn, outputFile + "--monthly", outputImages, Util.lastmonth, Util.midnight);
            }
        }
        is.close();

        // We can't use ucl.getResourceAsStream(ICON_ORG); since we don't know the path.
        for (Enumeration e = jf.entries(); e.hasMoreElements(); ) {
            JarEntry je = (JarEntry)e.nextElement();
            String name = je.getName();
            // System.out.println("Jar contains " + name);
            if (name.endsWith(File.separator + ICON_ORG)) {
                is = jf.getInputStream(je);
                imagesDir.mkdir();
                FileOutputStream fos = new FileOutputStream(new File(imagesDir, ICON_ORG));
                byte[] buf = new byte[256];
                int count;
                while ((count = is.read(buf)) > 0) {
                    fos.write(buf, 0, count);
                }
                fos.close();
                is.close();
            } else if (name.endsWith(File.separator + ICON_DESC)) {
                is = jf.getInputStream(je);
                imagesDir.mkdir();
                FileOutputStream fos = new FileOutputStream(new File(imagesDir, ICON_DESC));
                byte[] buf = new byte[256];
                int count;
                while ((count = is.read(buf)) > 0) {
                    fos.write(buf, 0, count);
                }
                fos.close();
                is.close();
            }
        }

        is = ucl.getResourceAsStream("META-INF/mvvm-transform.xml");
        XMLReader xr = XMLReaderFactory.createXMLReader();
        xr.setContentHandler(mth);
        xr.parse(new InputSource(is));
        is.close();

        String mktName = mth.getTransformDesc(new Tid()).getDisplayName();
        // HACK O RAMA XXXXXXXXX
        if (mktName.startsWith("EdgeReport"))
            mktName = "EdgeGuard Appliance";
        logger.debug("Writing transform name: " + mktName);
        FileOutputStream fos = new FileOutputStream(new File(tranDir, "name"));
        PrintWriter pw = new PrintWriter(fos);
        pw.println(mktName);
        pw.close();

        logger.debug("copying report-files");
        is = ucl.getResourceAsStream("META-INF/report-files");
        br = new BufferedReader(new InputStreamReader(is));
        fos = new FileOutputStream(new File(tranDir, "report-files"));
        pw = new PrintWriter(fos);
        for (String line = br.readLine(); null != line; line = br.readLine()) {
            pw.println(line);
        }
        is.close();
        pw.close();
    }


    ////////////////////////////////////////////////////
    // PROCESSORS //////////////////////////////////////
    ////////////////////////////////////////////////////

    // Used for both general and specific transforms.
    private void processReportSummarizer(ReportSummarizer reportSummarizer, Connection conn, String fileName, Timestamp startTime, Timestamp endTime)
        throws IOException
    {
        String result = reportSummarizer.getSummaryHtml(conn, startTime, endTime, extraParams);
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fileName)));
        bw.write(result);
        bw.close();
    }

    private void processReport(String resource, Connection conn, String base, String imagesDir, Timestamp startTime, Timestamp endTime)
        throws Exception
    {
        logger.debug("From: " + startTime + " To: " + endTime);

        InputStream jasperIs = ucl.getResourceAsStream(resource);
        if (null == jasperIs) {
            logger.warn("No such resource: " + resource);
            return;
        }

        Map params = new HashMap();
        params.put("startTime", startTime);
        params.put("endTime", endTime);
        for (String paramName : extraParams.keySet()) {
            Object paramValue = extraParams.get(paramName);
            params.put(paramName, paramValue);
        }
        params.put(JRParameter.REPORT_MAX_COUNT, Util.MAX_ROWS_PER_REPORT);
        logger.debug("Filling report");
        JasperPrint print = JasperFillManager.fillReport(jasperIs, params, conn);

        // PDF
        String pdfFile = base + ".pdf";
        logger.debug("Exporting report to: " + pdfFile);
        JasperExportManager.exportReportToPdfFile(print, pdfFile);

        // HTML
        String htmlFile = base + ".html";
        logger.debug("Exporting report to: " + htmlFile);
        JRHtmlExporter exporter = new JRHtmlExporter();
        exporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
        exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, htmlFile);
        exporter.setParameter(JRHtmlExporterParameter.IMAGES_DIR_NAME, imagesDir);
        exporter.setParameter(JRHtmlExporterParameter.IMAGES_URI, "../images/");
        exporter.exportReport();
        // Was: JasperExportManager.exportReportToHtmlFile(print, htmlFile);
    }



    private void processReportGraph(ReportGraph reportGraph, Connection conn, String fileName, int type, Timestamp startTime, Timestamp endTime)
        throws IOException, JRScriptletException, SQLException, ClassNotFoundException
    {
        Map<String,Object> params = new HashMap<String,Object>();
        params.put(ReportGraph.PARAM_REPORT_START_DATE, startTime);
        params.put(ReportGraph.PARAM_REPORT_END_DATE, endTime);
        params.put(ReportGraph.PARAM_REPORT_TYPE, type);
        JRDefaultScriptlet scriptlet = new FakeScriptlet(conn, params);
        JFreeChart jFreeChart = reportGraph.doInternal(conn, scriptlet);
        logger.debug("Exporting report to: " + fileName);
        //ChartUtilities.saveChartAsJPEG(new File(fileName), CHART_QUALITY, jFreeChart, CHART_WIDTH, CHART_HEIGHT);
	ChartUtilities.saveChartAsPNG(new File(fileName), jFreeChart, CHART_WIDTH, CHART_HEIGHT, null, false, CHART_COMPRESSION_PNG);
    }


    private class FakeScriptlet extends JRDefaultScriptlet
    {
        Map<String,Object> ourParams;

        FakeScriptlet(Connection con, Map params) {
            ourParams = params;
        }

        public Object getParameterValue(String paramName) throws JRScriptletException
        {
            Object found = null;
            if (ourParams != null) {
                found = ourParams.get(paramName);
            }
            if (found != null)
                return found;
            // return super.getParameterValue(paramName);
            return null;
        }
    }

}
