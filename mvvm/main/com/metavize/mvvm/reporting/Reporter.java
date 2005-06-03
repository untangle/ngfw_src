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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.metavize.mvvm.engine.MvvmTransformHandler;
import com.metavize.mvvm.reporting.summary.*;
import com.metavize.mvvm.security.Tid;
import net.sf.jasperreports.engine.JRDefaultScriptlet;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JRScriptletException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRHtmlExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporterParameter;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;


public class Reporter
{
    private static final String SYMLINK_CMD = "/bin/ln -s";

    private static final String ICON_DESC = "IconDesc42x42.png";
    private static final String ICON_ORG = "IconOrg42x42.png";
    private static final String SUMMARY_FRAGMENT_DAILY = "sum-daily.html";
    private static final String SUMMARY_FRAGMENT_WEEKLY = "sum-weekly.html";
    private static final String SUMMARY_FRAGMENT_MONTHLY = "sum-monthly.html";

    private static final Logger logger = Logger.getLogger(Reporter.class);

    // The base, containing one directory for each day's generated reports
    private static File outputBaseDir;

    // Today
    private static File outputDir;

    private final Calendar reportNow;
    private final Timestamp midnight;
    private final Timestamp lastday;
    private final Timestamp lastweek;
    private final Timestamp lastmonth;

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
        private final JarFile jf;
        private final File tranDir;

        TranReporter(String tranName, JarFile jf, URLClassLoader ucl)
        {
            this.tranName = tranName;
            // These next two are the same, just different forms.
            this.jf = jf;
            this.ucl = ucl;

            tranDir = new File(outputDir, tranName);
        }
        // private methods --------------------------------------------------------

        private void process(Connection conn) throws Exception
        {
            tranDir.mkdir();

            File imagesDir = new File(tranDir, "images");
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
                        Class sumClass = cl.loadClass(className);
                        ReportSummarizer s = (ReportSummarizer) sumClass.newInstance();
                        logger.debug("Found summarizer " + className);
                        String dailyFile = new File(tranDir, SUMMARY_FRAGMENT_DAILY).getCanonicalPath();
                        processSummarizer(s, conn, dailyFile, lastday, midnight);
                        s = (ReportSummarizer) sumClass.newInstance();
                        String weeklyFile = new File(tranDir, SUMMARY_FRAGMENT_WEEKLY).getCanonicalPath();
                        processSummarizer(s, conn, weeklyFile, lastweek, midnight);
                        s = (ReportSummarizer) sumClass.newInstance();
                        String monthlyFile = new File(tranDir, SUMMARY_FRAGMENT_MONTHLY).getCanonicalPath();
                        processSummarizer(s, conn, monthlyFile, lastmonth, midnight);
                    } catch (Exception x) {
                        logger.warn("No such class: " + className);
                    }
                } else {
                    String resource = resourceOrClassname;
                    String outputName = type;
                    String outputFile = new File(tranDir, outputName).getCanonicalPath();
                    String outputImages = imagesDir.getCanonicalPath();
                    processReport(resource, conn, outputFile + "-daily", outputImages, lastday, midnight);
                    processReport(resource, conn, outputFile + "-weekly", outputImages, lastweek, midnight);
                    processReport(resource, conn, outputFile + "-monthly", outputImages, lastmonth, midnight);
                }
            }
            is.close();

            // System.out.println("Looking for icons for " + mth.getTransformDesc(new Tid()).getDisplayName());

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

        private void processReport(String resource, Connection conn, String base, String imagesDir,
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
            JRHtmlExporter exporter = new JRHtmlExporter();
            exporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
            exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, htmlFile);
            exporter.setParameter(JRHtmlExporterParameter.IMAGES_DIR_NAME, imagesDir);
            exporter.exportReport();
                // Was: JasperExportManager.exportReportToHtmlFile(print, htmlFile);
        }
    }


    // constructors ----------------------------------------------------------

    private Reporter(File outputBaseDir)
    {
        this.outputBaseDir = outputBaseDir;

        Calendar c = Calendar.getInstance();

        // Go back to midnight
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        reportNow = (Calendar) c.clone();
        midnight = new Timestamp(c.getTimeInMillis());

        c.add(Calendar.DAY_OF_YEAR, -1);
        lastday = new Timestamp(c.getTimeInMillis());

        c = (Calendar) reportNow.clone();
        c.add(Calendar.WEEK_OF_YEAR, -1);
        lastweek = new Timestamp(c.getTimeInMillis());

        c = (Calendar) reportNow.clone();
        c.add(Calendar.MONTH, -1);
        lastmonth = new Timestamp(c.getTimeInMillis());

        // Figure out the real output Dir. Note we use 'lastday' as that really
        // means midnight of the day we are reporting on, thus 'todayName'.
        c.setTimeInMillis(lastday.getTime());
        String todayName = getDateDirName(c);
        outputDir = new File(outputBaseDir, todayName);
        outputDir.mkdirs();

        try {
            File current = new File(outputBaseDir, "current");
            current.delete();
            String command = SYMLINK_CMD + " " + outputDir.getPath() + " " + current.getPath();
            Process p = Runtime.getRuntime().exec(command);

        } catch (IOException exn) {
            logger.error("Unable to create current link", exn);
        }
    }

    // main -------------------------------------------------------------------

    public static void main(String[] args)
    {
        if (args.length < 2) {
            logger.warn("usage: reporter base-dir days-to-save [mars]");
            System.exit(1);
        }

        Connection conn = null;
        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection("jdbc:postgresql://localhost/mvvm",
                                               "metavize", "foo");
            File outputBaseDir = new File(args[0]);
            int daysToKeep;
            try {
                daysToKeep = Integer.parseInt(args[1]);
                if (daysToKeep < 1)
                    daysToKeep = 1;
            } catch (NumberFormatException x) {
                logger.warn("usage: reporter base-dir days-to-save [mars]");
                System.exit(1);
                return;
            }
                
            Reporter r = new Reporter(outputBaseDir);

            r.doIt(conn, args);
            r.purgeOldReports(daysToKeep);
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
        String dailyFile = new File(outputDir, SUMMARY_FRAGMENT_DAILY).getCanonicalPath();
        processSummarizer(s, conn, dailyFile, lastday, midnight);
        String weeklyFile = new File(outputDir, SUMMARY_FRAGMENT_WEEKLY).getCanonicalPath();
        processSummarizer(s, conn, weeklyFile, lastweek, midnight);
        String monthlyFile = new File(outputDir, SUMMARY_FRAGMENT_MONTHLY).getCanonicalPath();
        processSummarizer(s, conn, monthlyFile, lastmonth, midnight);

        Thread ct = Thread.currentThread();
        ClassLoader oldCl = ct.getContextClassLoader();

        for (int i = 2; i < args.length; i++) {
            File f = new File(args[i]);

            // assume file is "tranname-transform.mar"
            String fn = f.getName();
            String tranName = fn.substring(0, fn.length() - 4);

            try {
                URLClassLoader ucl = new URLClassLoader(new URL[] { f.toURL() });
                ct.setContextClassLoader(ucl);
                logger.debug("Running TranReporter for " + tranName);
                new TranReporter(tranName, new JarFile(f), ucl).process(conn);
            } catch (Exception exn) {
                logger.warn("bad mar: " + f);
                exn.printStackTrace();
            }
        }

        /*
        dailySums.append("</table><p><br>\n");
        weeklySums.append("</table><p><br>\n");
        monthlySums.append("</table><p><br>\n");

        dailySums.append("<b>Daily Summary Graphs</b><p>");
        weeklySums.append("<b>Weekly Summary Graphs</b><p>");
        monthlySums.append("<b>Monthly Summary Graphs</b><p>");
        */

        // Graphs.
        String graphsFile = new File(outputDir, "graphs").getCanonicalPath();
        processGraphs(conn, graphsFile + "-daily", lastday, midnight);
        processGraphs(conn, graphsFile + "-weekly", lastweek, midnight);
        processGraphs(conn, graphsFile + "-monthly", lastmonth, midnight);
 
    }

    private String getDateDirName(Calendar c)
    {      
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) + 1; // Java is stupid
        int day = c.get(Calendar.DAY_OF_MONTH);
        String name = String.format("%04d-%02d-%02d", year, month, day);
        return name;
    }

    private void purgeOldReports(int daysToKeep)
    {
        // Since daysToKeep will always be at least 1, we'll always keep today's.
        Calendar firstPurged = (Calendar) reportNow.clone();
        firstPurged.add(Calendar.DAY_OF_YEAR, -1 - daysToKeep);
        Calendar c = (Calendar) firstPurged.clone();
        int missCount = 0;
        while (true) {
            String dirName = getDateDirName(c);
            File dir = new File(outputBaseDir, dirName);
            if (dir.exists())
                deleteDir(dir);
            else
                missCount++;
            if (missCount > 100)
                // Allow for 100 missed days, in case they turned off the box for a couple months.
                break;
            c.add(Calendar.DAY_OF_YEAR, -1);
        }
    }

    public static boolean deleteDir(File dir) {
        // to see if this directory is actually a symbolic link to a directory,
        // we want to get its canonical path - that is, we follow the link to
        // the file it's actually linked to
        File candir;
        try {
            candir = dir.getCanonicalFile();
        } catch (IOException e) {
            return false;
        }
  
        // a symbolic link has a different canonical path than its actual path,
        // unless it's a link to itself
        if (!candir.equals(dir.getAbsoluteFile())) {
            // this file is a symbolic link, and there's no reason for us to
            // follow it, because then we might be deleting something outside of
            // the directory we were told to delete
            return false;
        }
  
        // now we go through all of the files and subdirectories in the
        // directory and delete them one by one
        File[] files = candir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
  
                // in case this directory is actually a symbolic link, or it's
                // empty, we want to try to delete the link before we try
                // anything
                boolean deleted = file.delete();
                if (!deleted) {
                    // deleting the file failed, so maybe it's a non-empty
                    // directory
                    if (file.isDirectory()) deleteDir(file);
  
                    // otherwise, there's nothing else we can do
                }
            }
        }
  
        // now that we tried to clear the directory out, we can try to delete it
        // again
        return dir.delete();  
    }   

    // Used for both general and specific transforms.
    private void processSummarizer(ReportSummarizer s, Connection conn, String fileName,
                                   Timestamp startTime, Timestamp endTime)

        throws IOException
    {
        String result = s.getSummaryHtml(conn, startTime, endTime);
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fileName)));
        bw.write(result);
        bw.close();
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
}
