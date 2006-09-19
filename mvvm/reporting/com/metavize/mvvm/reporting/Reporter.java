/*
 * Copyright (c) 2005, 2006 Metavize Inc.
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.jar.*;

import com.metavize.mvvm.reporting.summary.*;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.*;
import org.apache.log4j.Logger;
import org.jfree.chart.*;
import org.xml.sax.*;


public class Reporter
{
    private static final String SYMLINK_CMD = "/bin/ln -s";
    private final Logger logger = Logger.getLogger(getClass());

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEEE MMMM d, yyyy");

    // The base, containing one directory for each day's generated reports
    private static File outputBaseDir;

    // Today
    private static File outputDir;

    private static Settings settings;

    private Reporter() {}

    private Reporter(Connection conn, File outputBaseDir, boolean toMidnight)
    {
        this.outputBaseDir = outputBaseDir;

        Util.init(toMidnight);

        Calendar c = Calendar.getInstance();

        // Figure out the real output Dir. Note we use 'lastday' as that really
        // means midnight of the day we are reporting on, thus 'todayName'.
        c.setTimeInMillis(Util.lastday.getTime());
        String todayName = Util.getDateDirName(c);
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

        settings = new Settings(conn, c);
        String emailDetail = "export MV_EG_EMAIL_DETAIL=";
        String daily = "export MV_EG_DAILY_REPORT=";
        String weekly = "export MV_EG_WEEKLY_REPORT=";
        String monthly = "export MV_EG_MONTHLY_REPORT=";

        if (false == settings.getEmailDetail()) {
            emailDetail += "n";
        } else {
            emailDetail += "y";
        }

        if (false == settings.getDaily()) {
            daily += "n";
        } else {
            daily += "y";
        }

        if (false == settings.getWeekly()) {
            weekly += "n";
        } else {
            weekly += "y";
        }

        if (false == settings.getMonthly()) {
            monthly += "n";
        } else {
            monthly += "y";
        }
        //logger.info("email detail: " + emailDetail);
        //logger.info("daily: " + daily);
        //logger.info("weekly: " + weekly);
        //logger.info("monthly: " + monthly);

        try {
            File envFile = new File(outputDir, "settings.env");
            envFile.delete(); // discard any old env file
            envFile.createNewFile(); // create new env file
            FileWriter envFWriter = new FileWriter(envFile);
            BufferedWriter envBWriter = new BufferedWriter(envFWriter);

            envBWriter.write(emailDetail);
            envBWriter.newLine();
            envBWriter.write(daily);
            envBWriter.newLine();
            envBWriter.write(weekly);
            envBWriter.newLine();
            envBWriter.write(monthly);
            envBWriter.newLine();
            addParam(envBWriter, "MV_EG_REPORT_END", Util.midnight);
            addParam(envBWriter, "MV_EG_WEEK_START", Util.lastweek);
            addParam(envBWriter, "MV_EG_MONTH_START", Util.lastmonth);


            envBWriter.close();
            envFWriter.close();
        } catch (IOException exn) {
            logger.error("Unable to delete old env file, create new env file, or write to new env file for update-reports", exn);
        }
    }

    public static void main(String[] args)
    {
        Logger logger = Logger.getLogger(Reporter.class);

        if (args.length < 2) {
            logger.warn("usage: reporter base-dir days-to-save [mars]");
            System.exit(1);
        }

        Connection conn = null;
        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection("jdbc:postgresql://localhost/mvvm",
                                               "metavize", "foo");
            setConnectionProperties(conn);

            String outputBaseDirName = "/tmp";
            int daysToKeep = 90;
            boolean toMidnight = true;
            boolean doDHCPMap = false;
            int i;
            for (i = 0; i < args.length; i++) {
                if (args[i].equals("-o")) {
                    outputBaseDirName = args[++i];
                } else if (args[i].equals("-d")) {
                    daysToKeep = Integer.parseInt(args[++i]);
                    if (daysToKeep < 1)
                        daysToKeep = 1;
                } else if (args[i].equals("-n")) {
                    toMidnight = false;
                } else if (args[i].equals("-m")) {
                    doDHCPMap = true;
                } else {
                    break;      // Into the mars
                }
            }
            int numMars = args.length - i;
            String[] mars = new String[numMars];
            System.arraycopy(args, i, mars, 0, numMars);
            File outputBaseDir = new File(outputBaseDirName);
            if (doDHCPMap) {
                Reporter.prepare(conn, toMidnight);
            } else {
                Reporter reporter = new Reporter(conn, outputBaseDir, toMidnight);
                reporter.generateNewReports(conn, mars);
                reporter.purgeOldReports(daysToKeep);
            }

        } catch (ClassNotFoundException exn) {
            logger.error("Could not load the Postgres JDBC driver");
            System.exit(1);
        } catch (IOException exn) {
            logger.error("IOException writing reports", exn);
            System.exit(1);
        } catch (SQLException exn) {
            logger.error("Could not get JDBC connection", exn);
            System.exit(1);
        } catch (JRScriptletException exn) {
            logger.error("Unexpected Jasper exception", exn);
            System.exit(1);
        } catch (NumberFormatException x) {
            logger.warn("usage: reporter base-dir days-to-save [mars]");
            System.exit(1);
        }
    }

    private static void prepare(Connection conn, boolean toMidnight)
        throws SQLException
    {
        Util.init(toMidnight);

        DhcpMap.get().generateAddressMap(conn, Util.lastmonth, Util.midnight);
    }

    private void generateNewReports(Connection conn, String[] mars)
        throws IOException, JRScriptletException, SQLException, ClassNotFoundException
    {
        Thread ct = Thread.currentThread();
        ClassLoader oldCl = ct.getContextClassLoader();

        for (int i = 0; i < mars.length; i++) {
            File f = new File(mars[i]);

            // assume file is "tranname-transform-impl.mar"
            String fn = f.getName();
            String tranName = fn;
            if ( tranName.endsWith( ".jar" ) || tranName.endsWith( ".mar" )) {
                tranName = tranName.substring(0, tranName.length() - 4);
            }
            
            if ( tranName.endsWith( "-impl" )) {
                tranName = tranName.substring(0, tranName.length() - 5);
            }
            
            try {
                URLClassLoader ucl = new URLClassLoader(new URL[] { f.toURL() });
                ct.setContextClassLoader(ucl);
                logger.info("Running TranReporter for " + tranName);
                TranReporter tranReporter = new TranReporter(outputDir, tranName, new JarFile(f), ucl, settings);
                tranReporter.process(conn);
            } catch (Exception exn) {
                logger.warn("bad mar: " + f, exn);
            }
        }
    }

    private void purgeOldReports(int daysToKeep)
    {
        // Since daysToKeep will always be at least 1, we'll always keep today's.
        Calendar firstPurged = (Calendar) Util.reportNow.clone();
        firstPurged.add(Calendar.DAY_OF_YEAR, -1 - daysToKeep);
        Calendar c = (Calendar) firstPurged.clone();
        int missCount = 0;
        while (true) {
            String dirName = Util.getDateDirName(c);
            File dir = new File(outputBaseDir, dirName);
            if (dir.exists())
                Util.deleteDir(dir);
            else
                missCount++;
            if (missCount > 100)
                // Allow for 100 missed days, in case they turned off the box for a couple months.
                break;
            c.add(Calendar.DAY_OF_YEAR, -1);
        }
    }

    // Optimize postgres settings for this connection to do report generation.
    private static void setConnectionProperties(Connection conn)
        throws SQLException
    {
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("set sort_mem=16384");
        stmt.executeUpdate("set effective_cache_size=3000");
    }

    private static void addParam(BufferedWriter w, String name, java.util.Date value) throws IOException
    {
        w.write("export " + name + "=");
        w.write("'" + DATE_FORMAT.format(value) + "'");
        w.newLine();
    }
}
