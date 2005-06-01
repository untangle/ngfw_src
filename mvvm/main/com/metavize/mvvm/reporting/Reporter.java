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
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import com.metavize.mvvm.engine.MvvmTransformHandler;
import com.metavize.mvvm.reporting.summary.*;
import com.metavize.mvvm.security.Tid;
import net.sf.jasperreports.engine.JRDefaultScriptlet;
import net.sf.jasperreports.engine.JRScriptletException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;


public class Reporter
{
    private static final Logger logger = Logger.getLogger(Reporter.class);

    private static File outputDir;

    private final Timestamp now;
    private final Timestamp yesterday;
    private final Timestamp lastweek;

    private StringBuilder dailySums;
    private StringBuilder weeklySums;


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


    private class TranReporter {
        private final String tranName;
        private final URLClassLoader ucl;
        private final File tranDir;

        TranReporter(String tranName, URLClassLoader ucl)
        {
            this.tranName = tranName;
            this.ucl = ucl;

            tranDir = new File(outputDir, tranName);
        }
        // private methods --------------------------------------------------------

        private void process(Connection conn) throws Exception
        {
            tranDir.mkdir();

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
                        Class sumClass = cl.loadClass(className);
                        ReportSummarizer s = (ReportSummarizer) sumClass.newInstance();
                        logger.debug("Found summarizer " + className);
                        dailySums.append(processSummarizer(s, conn, yesterday, now));
                        s = (ReportSummarizer) sumClass.newInstance();
                        weeklySums.append(processSummarizer(s, conn, lastweek, now));
                    } catch (Exception x) {
                        logger.warn("No such class: " + className);
                    }
                } else {
                    String resource = resourceOrClassname;
                    String outputName = type;
                    String outputFile = new File(tranDir, outputName).getCanonicalPath();
                    processReport(resource, conn, outputFile + "-daily", yesterday, now);
                    processReport(resource, conn, outputFile + "-weekly", lastweek, now);
                }
            }
            is.close();

            MvvmTransformHandler mth = new MvvmTransformHandler();
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

        private void processReport(String resource, Connection conn, String base,
                                   Timestamp startTime, Timestamp endTime)
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
            logger.debug("Filling report");
            JasperPrint print = JasperFillManager.fillReport(jasperIs, params, conn);

            // PDF
            String pdfFile = base + ".pdf";
            logger.debug("Exporting report to: " + pdfFile);
            JasperExportManager.exportReportToPdfFile(print, pdfFile);

            // HTML
            String htmlFile = base + ".html";
            logger.debug("Exporting report to: " + htmlFile);
            JasperExportManager.exportReportToHtmlFile(print, htmlFile);
        }
    }


    // constructors ----------------------------------------------------------

    private Reporter(File outputDir)
    {
        Calendar c;

        this.outputDir = outputDir;

        now = new Timestamp(System.currentTimeMillis());
        c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -1);
        yesterday = new Timestamp(c.getTimeInMillis());
        c = Calendar.getInstance();
        c.add(Calendar.WEEK_OF_YEAR, -1);
        lastweek = new Timestamp(c.getTimeInMillis());

        dailySums = new StringBuilder();
        weeklySums = new StringBuilder();
    }

    // main -------------------------------------------------------------------

    public static void main(String[] args)
    {
        if (1 > args.length) {
            logger.warn("usage: reporter dir [mars]");
            System.exit(1);
        }

        Connection conn = null;
        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection("jdbc:postgresql://localhost/mvvm",
                                               "metavize", "foo");
            Reporter r = new Reporter(new File(args[0]));

            r.doIt(conn, args);
        } catch (ClassNotFoundException exn) {
            logger.warn("Could not load the Postgres JDBC driver");
            System.exit(1);
        } catch (IOException exn) {
            logger.warn("IOException writing reports");
            exn.printStackTrace();
            System.exit(1);
        } catch (SQLException exn) {
            logger.warn("Could not get JDBC connection");
            exn.printStackTrace();
            System.exit(1);
        } catch (JRScriptletException exn) {
            logger.warn("Unexpected Jasper exception");
            exn.printStackTrace();
            System.exit(1);
        }
    }

    private void doIt(Connection conn, String[] args)
        throws IOException, JRScriptletException, SQLException, ClassNotFoundException
    {
        // General summarization
        ReportSummarizer s = new GeneralSummarizer();
        dailySums.append(processSummarizer(s, conn, yesterday, now));
        weeklySums.append(processSummarizer(s, conn, lastweek, now));

        Thread ct = Thread.currentThread();
        ClassLoader oldCl = ct.getContextClassLoader();

        for (int i = 1; i < args.length; i++) {
            File f = new File(args[i]);

            // assume file is "tranname-transform.mar"
            String fn = f.getName();
            String tranName = fn.substring(0, fn.length() - 4);

            try {
                URLClassLoader ucl = new URLClassLoader(new URL[] { f.toURL() });
                ct.setContextClassLoader(ucl);
                logger.debug("Running TranReporter for " + tranName);
                new TranReporter(tranName, ucl).process(conn);
            } catch (Exception exn) {
                logger.warn("bad mar: " + f);
                exn.printStackTrace();
            }
        }

        dailySums.append("</table><p><br>\n");
        weeklySums.append("</table><p><br>\n");

        dailySums.append("<b>Daily Summary Graphs</b><p>");
        weeklySums.append("<b>Weekly Summary Graphs</b><p>");

        // Graphs.
        String graphsFile = new File(outputDir, "graphs").getCanonicalPath();
        dailySums.append(processGraphs(conn, graphsFile + "-daily", yesterday, now));
        weeklySums.append(processGraphs(conn, graphsFile + "-weekly", lastweek, now));

        String sumFile = new File(outputDir, "summarization").getCanonicalPath();
        emitSummarization(sumFile + "-daily.html", dailySums);
        emitSummarization(sumFile + "-weekly.html", weeklySums);
    }

    // Used for both general and specific transforms.
    private String processSummarizer(ReportSummarizer s, Connection conn,
                                     Timestamp startTime, Timestamp endTime)

    {
        String result = s.getSummaryHtml(conn, startTime, endTime);
        return result + "<p>\n";
    }

    private String processGraphs(Connection conn, String base, Timestamp startTime, Timestamp endTime)
        throws IOException, JRScriptletException, SQLException, ClassNotFoundException
    {
        StringBuilder result = new StringBuilder();
        Map<String,Object> params = new HashMap<String,Object>();
        params.put(ReportGraph.PARAM_REPORT_START_DATE, startTime);
        params.put(ReportGraph.PARAM_REPORT_END_DATE, endTime);
        JRDefaultScriptlet slet = new FakeScriptlet(conn, params);

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

        // HACK ALERT XXX
        if (endTime.getTime() - startTime.getTime() <= (1000 * 60 * 60 * 24 + 1000 * 60)) {
            graphName = "AllTrafficDayByMinuteGraph";
            ReportGraph g3 = new TrafficDayByMinuteGraph(slet, graphName,
                                                         "Traffic",
                                                         true, true,
                                                         "Outgoing", "Incoming", "Total");
            JFreeChart c3 = g3.doInternal(conn);
            // JPeg
            jpegFile = base + "-alldaybymin.jpg";
            logger.debug("Exporting report to: " + jpegFile);
            f =  new File(jpegFile);
            fileName = f.getName();
            ChartUtilities.saveChartAsJPEG(new File(jpegFile), c3, 400, 300);
            result.append("<img SRC=\"").append(fileName).append("\" WIDTH=\"400\" HEIGHT=\"300\">");
            result.append("<p>\n");
        }

        // ...


        return result.toString();
    }

    private void emitSummarization(String fileName, StringBuilder summary)
        throws IOException
    {
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fileName)));
        bw.write(summary.toString());
        bw.close();
    }
}
