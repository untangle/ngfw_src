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
import com.metavize.mvvm.reporting.Util;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.*;
import org.apache.log4j.Logger;
import org.jfree.chart.*;
import org.xml.sax.*;
import org.xml.sax.helpers.XMLReaderFactory;


public class TranReporter {

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
	MvvmTransformHandler mth = new MvvmTransformHandler();
	
	InputStream is = ucl.getResourceAsStream("META-INF/report-files");
	if (null == is) {
	    logger.warn("No reports for: " + ucl.getURLs()[0]);
	    return;
	}
	BufferedReader br = new BufferedReader(new InputStreamReader(is));
	
	for (String line = br.readLine(); null != line; line = br.readLine()) {
	    StringTokenizer tok = new StringTokenizer(line, ":");
	    if (!tok.hasMoreTokens()) { continue; }
	    String resourceOrClassname = tok.nextToken();
	    if (!tok.hasMoreTokens()) { continue; }
	    String type = tok.nextToken();
	    if (type.equalsIgnoreCase("summarizer")) {
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
		    logger.warn("No such class: " + className);
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
		    logger.debug("Found graph: " + className);
		    String dailyFile = new File(tranDir, name + "--daily.jpg").getCanonicalPath();
		    processReportGraph(reportGraph, conn, dailyFile, Util.lastday, Util.midnight);
		} catch (Exception x) {
		    logger.warn("No such class: " + className);
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
        String result = reportSummarizer.getSummaryHtml(conn, startTime, endTime);
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


    
    private void processReportGraph(ReportGraph reportGraph, Connection conn, String fileName, Timestamp startTime, Timestamp endTime)
        throws IOException, JRScriptletException, SQLException, ClassNotFoundException
    {


	/*
        String graphName = "AllOutgoingTrafficByPortGraph";
        ReportGraph g1 = new TrafficByPortGraph(slet, graphName,
                                                "Outgoing Traffic", false,
                                                true, true,
                                                true, false);
        JFreeChart c1 = g1.doInternal(conn);
        // JPeg
        String jpegFile = base + "-allinbyport.jpg";
        File f =  new File(jpegFile);
        String fileName = f.getName();
        logger.debug("Exporting report to: " + jpegFile);
        ChartUtilities.saveChartAsJPEG(new File(jpegFile), c1, 400, 300);
        result.append("<img SRC=\"").append(fileName).append("\" WIDTH=\"400\" HEIGHT=\"300\">");
        result.append("<p>\n");

        graphName = "AllIncomingTrafficByPortGraph";
        ReportGraph g2 = new TrafficByPortGraph(slet, graphName,
                                                "Incoming Traffic", false,
                                                true, true,
                                                false, true);
        JFreeChart c2 = g2.doInternal(conn);
        // JPeg
        jpegFile = base + "-alloutbyport.jpg";
        logger.debug("Exporting report to: " + jpegFile);
        f =  new File(jpegFile);
        fileName = f.getName();
        ChartUtilities.saveChartAsJPEG(f, c2, 400, 300);
        result.append("<img SRC=\"").append(fileName).append("\" WIDTH=\"400\" HEIGHT=\"300\">");
        result.append("<p>\n");
	*/

        // HACK ALERT XXX
        if (endTime.getTime() - startTime.getTime() <= (1000 * 60 * 60 * 24 + 1000 * 60)) {
            //graphName = "AllTrafficDayByMinuteGraph";
            //ReportGraph g3 = new TrafficDayByMinuteGraph(slet, graphName,
            //                                             "Traffic",
            //                                             true, true,
            //                                             "Outgoing", "Incoming", "Total");
	    Map<String,Object> params = new HashMap<String,Object>();
	    params.put(ReportGraph.PARAM_REPORT_START_DATE, Util.lastday);
	    params.put(ReportGraph.PARAM_REPORT_END_DATE, Util.midnight);
	    JRDefaultScriptlet scriptlet = new FakeScriptlet(conn, params);
            JFreeChart jFreeChart = reportGraph.doInternal(conn, scriptlet, "Traffic");
            // JPeg
            //jpegFile = base + "-alldaybymin.jpg";
            logger.debug("Exporting report to: " + fileName);
            //f =  new File(fileName);
            //fileName = f.getName();
            ChartUtilities.saveChartAsJPEG(new File(fileName), jFreeChart, 400, 300);
            //result.append("<img SRC=\"").append(fileName).append("\" WIDTH=\"400\" HEIGHT=\"300\">");
            //result.append("<p>\n");
        }

        // ...


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
