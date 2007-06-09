/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.reporting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import com.untangle.mvvm.tran.Scanner;
import com.untangle.mvvm.tran.TransformContext;
import net.sf.jasperreports.engine.JRDefaultScriptlet;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JRScriptletException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRHtmlExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporterParameter;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;

/**
 * Runs reports.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
public class TranReporter
{
    public static final float CHART_QUALITY_JPEG = .9f;  // for JPEG
    public static final int CHART_COMPRESSION_PNG = 0;  // for PNG
    public static final int CHART_WIDTH = 600;
    public static final int CHART_HEIGHT = 200;

    private final Logger logger = Logger.getLogger(getClass());
    public static final String ICON_DESC = "IconDesc42x42.png";
    public static final String ICON_ORG = "IconOrg42x42.png";
    private static final String SUMMARY_FRAGMENT_DAILY = "sum-daily.html";
    private static final String SUMMARY_FRAGMENT_WEEKLY = "sum-weekly.html";
    private static final String SUMMARY_FRAGMENT_MONTHLY = "sum-monthly.html";

    private final TransformContext tctx;
    private final String tranName;

    private final Settings settings;
    private final File tranDir;

    private Map<String,Object> extraParams = new HashMap<String,Object>();

    TranReporter(File outputDir, TransformContext tctx, Settings settings)
    {
        this.tctx = tctx;
        this.settings = settings;
        this.tranName = tctx.getTransformDesc().getName();
        this.tranDir = new File(outputDir, tranName);
    }

    public void process(Connection conn) throws Exception
    {
        tranDir.mkdir();

        File imagesDir = new File(tranDir, "images");
        File globalImagesDir = new File(tranDir, "../images");
        Scanner scanner = null;

        InputStream is = tctx.getResourceAsStream("META-INF/report-files");
        if (null == is) {
            logger.warn("No reports for: " + tranName);
            return;
        } else {
            logger.info("Beginning generation for: " + tranName);
        }

        // Need to do the parameters & scanners first.
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        for (String line = br.readLine(); null != line; line = br.readLine()) {
            if (line.startsWith("#")) {
                continue;
            }
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
                try {
                    Class scannerClass = getClass().getClassLoader()
                        .loadClass(resourceOrClassname);
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

        // Icons.  We have to be a bit tricky to get the name right.
        String tname = tctx.getTransformDesc().getClassName().replace('.', '/');
        String rdir = tname.substring(0, tname.lastIndexOf("/")) + "/gui/";

        is = tctx.getResourceAsStream(rdir + ICON_ORG);
        if (is == null) {
            logger.warn("No icon_org for: " + rdir + ICON_ORG);
        } else {
            imagesDir.mkdir();
            FileOutputStream fos = new FileOutputStream(new File(imagesDir, ICON_ORG));
            byte[] buf = new byte[256];
            int count;
            while ((count = is.read(buf)) > 0) {
                fos.write(buf, 0, count);
            }
            fos.close();
            is.close();
        }
        is = tctx.getResourceAsStream(rdir + ICON_DESC);
        if (is == null) {
            logger.warn("No icon_desc for: " + rdir + ICON_DESC);
        } else {
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

        // Now do everything else.
        is = tctx.getResourceAsStream("META-INF/report-files");
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
                try {
                    Class reportClass = getClass().getClassLoader().loadClass(className);
                    ReportSummarizer reportSummarizer;

                    if (true == settings.getDaily()) {
                        logger.debug("Found daily summarizer: " + className);
                        reportSummarizer = (ReportSummarizer) reportClass.newInstance();
                        String dailyFile = new File(tranDir, SUMMARY_FRAGMENT_DAILY).getCanonicalPath();
                        processReportSummarizer(reportSummarizer, conn, dailyFile, Util.lastday, Util.midnight);
                    }

                    if (true == settings.getWeekly()) {
                        logger.debug("Found weekly summarizer: " + className);
                        reportSummarizer = (ReportSummarizer) reportClass.newInstance();
                        String weeklyFile = new File(tranDir, SUMMARY_FRAGMENT_WEEKLY).getCanonicalPath();
                        processReportSummarizer(reportSummarizer, conn, weeklyFile, Util.lastweek, Util.midnight);
                    }

                    if (true == settings.getMonthly()) {
                        logger.debug("Found monthly summarizer: " + className);
                        reportSummarizer = (ReportSummarizer) reportClass.newInstance();
                        String monthlyFile = new File(tranDir, SUMMARY_FRAGMENT_MONTHLY).getCanonicalPath();
                        processReportSummarizer(reportSummarizer, conn, monthlyFile, Util.lastmonth, Util.midnight);
                    }
                } catch (Exception x) {
                    logger.error("Unable to summarize", x);
                }
            } else if (type.equalsIgnoreCase("summaryGraph")) {
                String className = resourceOrClassname;
                String outputName = type;
                try {
                    Class reportClass = getClass().getClassLoader().loadClass(className);
                    ReportGraph reportGraph;
                    String name = tok.nextToken();
                    reportGraph = (ReportGraph) reportClass.newInstance();
                    reportGraph.setExtraParams(extraParams);

                    if (true == settings.getDaily()) {
                        logger.debug("Found daily graph: " + className);
                        String dailyFile = new File(tranDir, name + "--daily.png").getCanonicalPath();
                        processReportGraph(reportGraph, conn, dailyFile, Util.REPORT_TYPE_DAILY, Util.lastday, Util.midnight);
                    }

                    if (true == settings.getWeekly()) {
                        logger.debug("Found weekly graph: " + className);
                        String weeklyFile = new File(tranDir, name + "--weekly.png").getCanonicalPath();
                        processReportGraph(reportGraph, conn, weeklyFile, Util.REPORT_TYPE_WEEKLY, Util.lastweek, Util.midnight);
                    }

                    if (true == settings.getMonthly()) {
                        logger.debug("Found monthly graph: " + className);
                        String monthlyFile = new File(tranDir, name + "--monthly.png").getCanonicalPath();
                        processReportGraph(reportGraph, conn, monthlyFile, Util.REPORT_TYPE_MONTHLY, Util.lastmonth, Util.midnight);
                    }
                } catch (Exception x) {
                    logger.error("Unable to generate summary graph", x);
                    x.printStackTrace();
                }
            } else if (type.equalsIgnoreCase("userSummary")) {
                String resource = resourceOrClassname;
                if (!tok.hasMoreTokens()) { continue; }
                String reportName = tok.nextToken();
                String reportFile = new File(tranDir, reportName).getCanonicalPath();

                String[] userNames;

                if (true == settings.getDaily()) {
                    userNames = getUserNames(conn, Util.lastday, Util.midnight);
                    processUserReports(resource, conn, userNames, reportFile, "--daily", Util.lastday, Util.midnight);
                }

                if (true == settings.getWeekly()) {
                    userNames = getUserNames(conn, Util.lastweek, Util.midnight);
                    processUserReports(resource, conn, userNames, reportFile, "--weekly", Util.lastweek, Util.midnight);
                }

                if (true == settings.getMonthly()) {
                    userNames = getUserNames(conn, Util.lastmonth, Util.midnight);
                    processUserReports(resource, conn, userNames, reportFile, "--monthly", Util.lastmonth, Util.midnight);
                }
            } else if (type.equalsIgnoreCase("hNameSummary")) {
                String resource = resourceOrClassname;
                if (!tok.hasMoreTokens()) { continue; }
                String reportName = tok.nextToken();
                String reportFile = new File(tranDir, reportName).getCanonicalPath();

                // reuse processUserReports to process hName reports
                // - hName reports use different sql queries but
                //   otherwise, are the same as uName reports
                String[] hostNames;

                if (true == settings.getDaily()) {
                    hostNames = getHostNames(conn, Util.lastday, Util.midnight);
                    processUserReports(resource, conn, hostNames, reportFile, "--daily", Util.lastday, Util.midnight);
                }

                if (true == settings.getWeekly()) {
                    hostNames = getHostNames(conn, Util.lastweek, Util.midnight);
                    processUserReports(resource, conn, hostNames, reportFile, "--weekly", Util.lastweek, Util.midnight);
                }

                if (true == settings.getMonthly()) {
                    hostNames = getHostNames(conn, Util.lastmonth, Util.midnight);
                    processUserReports(resource, conn, hostNames, reportFile, "--monthly", Util.lastmonth, Util.midnight);
                }
            } else {
                String resource = resourceOrClassname;
                String outputName = type;
                String outputFile = new File(tranDir, outputName).getCanonicalPath();
                String outputImages = globalImagesDir.getCanonicalPath();

                if (true == settings.getDaily()) {
                    processReport(resource, conn, outputFile + "--daily", outputImages, Util.lastday, Util.midnight);
                }

                if (true == settings.getWeekly()) {
                    processReport(resource, conn, outputFile + "--weekly", outputImages, Util.lastweek, Util.midnight);
                }

                if (true == settings.getMonthly()) {
                    processReport(resource, conn, outputFile + "--monthly", outputImages, Util.lastmonth, Util.midnight);
                }
            }
        }
        is.close();

        String mktName = tctx.getTransformDesc().getDisplayName();
        // HACK O RAMA XXXXXXXXX
        if (mktName.startsWith("Untangle Reports"))
            mktName = "Untangle Platform";
        logger.debug("Writing transform name: " + mktName);
        FileOutputStream fos = new FileOutputStream(new File(tranDir, "name"));
        PrintWriter pw = new PrintWriter(fos);
        pw.println(mktName);
        pw.close();

        logger.debug("copying report-files");
        is = tctx.getResourceAsStream("META-INF/report-files");
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
    private void processReportSummarizer(ReportSummarizer reportSummarizer,
                                         Connection conn, String fileName,
                                         Timestamp startTime,
                                         Timestamp endTime)
        throws IOException
    {
        String result = reportSummarizer.getSummaryHtml(conn, startTime, endTime, extraParams);
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fileName)));
        bw.write(result);
        bw.close();
    }

    private String[] getUserNames(Connection conn, Timestamp startTime, Timestamp endTime) throws Exception
    {
        ArrayList<String> uNameList = new ArrayList();
        String sql = "SELECT DISTINCT username FROM events.mvvm_lookup_evt WHERE time_stamp >= ? AND time_stamp < ?";

        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startTime);
            ps.setTimestamp(2, endTime);

            ResultSet rs = ps.executeQuery();
            String uName;
            while (true == rs.next()) {
                uName = rs.getString(1);
                if (null != uName)
                    uNameList.add(uName);
            }
            rs.close();

            ps.close();
        } catch (SQLException exn) {
            logger.error("unable to retrieve list of user names (" + startTime + ", " + endTime + ")", exn);
        }

        return (String[]) uNameList.toArray(new String[uNameList.size()]);
    }

    private String[] getHostNames(Connection conn, Timestamp startTime,
                                  Timestamp endTime)
        throws Exception
    {
        ArrayList<String> hNameList = new ArrayList();
        String sql = "SELECT DISTINCT hname FROM reports.sessions WHERE time_stamp >= ? AND time_stamp < ? AND client_intf=1";

        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startTime);
            ps.setTimestamp(2, endTime);

            ResultSet rs = ps.executeQuery();
            String hName;
            while (true == rs.next()) {
                hName = rs.getString(1);
                if (null != hName)
                    hNameList.add(hName);
            }
            rs.close();

            ps.close();
        } catch (SQLException exn) {
            logger.error("unable to retrieve list of host names (" + startTime + ", " + endTime + ")", exn);
        }

        return (String[]) hNameList.toArray(new String[hNameList.size()]);
    }

    private void processUserReports(String resource, Connection conn,
                                    String[] userNames, String baseTag,
                                    String periodTag, Timestamp startTime,
                                    Timestamp endTime)
        throws Exception
    {
        logger.debug("From: " + startTime + " To: " + endTime);

        // startTime, endTime, and userName parameters are defined
        // in template files so that a report can reference these parameters

        Map<String, Object> baseParams = new HashMap<String, Object>();
        baseParams.put("startTime", startTime);
        baseParams.put("endTime", endTime);

        Object paramValue;
        for (String paramName : extraParams.keySet()) {
            paramValue = extraParams.get(paramName);
            baseParams.put(paramName, paramValue);
        }

        /* to override default of 500 max rows per report,
         * add a parameter line like this to report-files:
         *
         * REPORT_MAX_COUNT:parameter:5000
         *
         * (where REPORT_MAX_COUNT is basis of JRParameter.REPORT_MAX_COUNT and
         *  for this report, a max of 5000 rows instead of 500 rows)
         *
         * (JRParameter.REPORT_MAX_COUNT is a constant JasperReport string)
         */
        paramValue = extraParams.get(JRParameter.REPORT_MAX_COUNT);
        int maxRows;
        if (null == paramValue) {
            // use default max rows per report
            maxRows = Util.MAX_ROWS_PER_REPORT;
        } else {
            // override default max rows per report
            maxRows = Integer.valueOf((String) paramValue).intValue();
        }
        logger.debug("max rows per report: " + maxRows);
        // if key is already present, replace its original value
        // else if not key is not present, add it and its value
        baseParams.put(JRParameter.REPORT_MAX_COUNT, maxRows);

        Map<String, Object> params = new HashMap<String, Object>();
        String baseName;
        for (String userName : userNames) {
            // strip all dir seps and spaces from name
            // because this name is used to generate the filename
            baseName = baseTag + "--" + userName.replaceAll("[/ ]","") + periodTag;

            params.putAll(baseParams);
            // JasperReports automatically adds quotes to this literal value
            params.put("userName", userName);

            InputStream jasperIs = tctx.getResourceAsStream(resource);
            if (null == jasperIs) {
                logger.warn("No such resource: " + resource);
                return;
            }

            logger.debug("Filling report");
            JasperPrint print = JasperFillManager.fillReport(jasperIs, params, conn);

            // PDF
            String pdfFile = baseName + ".pdf";
            logger.debug("Exporting report to: " + pdfFile);
            JasperExportManager.exportReportToPdfFile(print, pdfFile);

            // HTML
            String htmlFile = baseName + ".html";
            logger.debug("Exporting report to: " + htmlFile);
            JRHtmlExporter exporter = new JRHtmlExporter();
            exporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
            exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, htmlFile);
            exporter.exportReport();
            // Was: JasperExportManager.exportReportToHtmlFile(print, htmlFile);

            params.clear(); // reset for next user report
        }
    }

    private void processReport(String resource, Connection conn, String base,
                               String imagesDir, Timestamp startTime,
                               Timestamp endTime)
        throws Exception
    {
        logger.debug("From: " + startTime + " To: " + endTime);

        InputStream jasperIs = tctx.getResourceAsStream(resource);
        if (null == jasperIs) {
            logger.warn("No such resource: " + resource);
            return;
        }

        Map<String, Object> params = new HashMap<String, Object>();
        Object paramValue;
        params.put("startTime", startTime);
        params.put("endTime", endTime);
        for (String paramName : extraParams.keySet()) {
            paramValue = extraParams.get(paramName);
            params.put(paramName, paramValue);
        }

        /* to override default of 500 max rows per report,
         * add a parameter line like this to report-files:
         *
         * REPORT_MAX_COUNT:parameter:5000
         *
         * (where REPORT_MAX_COUNT is basis of JRParameter.REPORT_MAX_COUNT and
         *  for this report, a max of 5000 rows instead of 500 rows)
         *
         * (JRParameter.REPORT_MAX_COUNT is a constant JasperReport string)
         */
        paramValue = extraParams.get(JRParameter.REPORT_MAX_COUNT);
        int maxRows;
        if (null == paramValue) {
            // use default max rows per report
            maxRows = Util.MAX_ROWS_PER_REPORT;
        } else {
            // override default max rows per report
            maxRows = Integer.valueOf((String) paramValue).intValue();
        }
        logger.debug("max rows per report: " + maxRows);
        // if key is already present, replace its original value
        // else if not key is not present, add it and its value
        params.put(JRParameter.REPORT_MAX_COUNT, maxRows);

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

    private void processReportGraph(ReportGraph reportGraph, Connection conn,
                                    String fileName, int type,
                                    Timestamp startTime, Timestamp endTime)
        throws IOException, JRScriptletException, SQLException,
               ClassNotFoundException
    {
        Map<String,Object> params = new HashMap<String,Object>();
        params.put(ReportGraph.PARAM_REPORT_START_DATE, startTime);
        params.put(ReportGraph.PARAM_REPORT_END_DATE, endTime);
        params.put(ReportGraph.PARAM_REPORT_TYPE, type);
        JRDefaultScriptlet scriptlet = new FakeScriptlet(conn, params);
        JFreeChart jFreeChart = reportGraph.doInternal(conn, scriptlet);
        logger.debug("Exporting report to: " + fileName);
        //ChartUtilities.saveChartAsJPEG(new File(fileName), CHART_QUALITY, jFreeChart, CHART_WIDTH, CHART_HEIGHT);
        ChartUtilities.saveChartAsPNG(new File(fileName), jFreeChart,
                                      CHART_WIDTH, CHART_HEIGHT, null, false,
                                      CHART_COMPRESSION_PNG);
    }

    private class FakeScriptlet extends JRDefaultScriptlet
    {
        Map<String,Object> ourParams;

        FakeScriptlet(Connection con, Map params) {
            ourParams = params;
        }

        public Object getParameterValue(String paramName)
            throws JRScriptletException
        {
            Object found = null;
            if (ourParams != null) {
                found = ourParams.get(paramName);
            }
            if (found != null) {
                return found;
            }
            // return super.getParameterValue(paramName);
            return null;
        }
    }
}
