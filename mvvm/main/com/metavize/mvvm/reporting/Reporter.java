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

import com.metavize.mvvm.reporting.summary.*;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.*;
import org.apache.log4j.Logger;
import org.jfree.chart.*;
import org.xml.sax.*;


public class Reporter
{

    private static final String SYMLINK_CMD = "/bin/ln -s";
    private static final Logger logger = Logger.getLogger(Reporter.class);

    // The base, containing one directory for each day's generated reports
    private static File outputBaseDir;

    // Today
    private static File outputDir;



    private Reporter(File outputBaseDir, boolean toMidnight)
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
    }



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

            String outputBaseDirName = "/tmp";
            int daysToKeep = 90;
            boolean toMidnight = true;
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
                } else {
                    break;      // Into the mars
                }
            }
            int numMars = args.length - i;
            String[] mars = new String[numMars];
            System.arraycopy(args, i, mars, 0, numMars);
            File outputBaseDir = new File(outputBaseDirName);
            Reporter reporter = new Reporter(outputBaseDir, toMidnight);
            reporter.prepare(conn);
            reporter.generateNewReports(conn, mars);
            reporter.purgeOldReports(conn, daysToKeep);

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


    private void prepare(Connection conn)
        throws SQLException
    {
        DhcpMap.get().generateAddressMap(conn, Util.lastmonth, Util.midnight);
    }


    private void generateNewReports(Connection conn, String[] mars)
        throws IOException, JRScriptletException, SQLException, ClassNotFoundException
    {

        Thread ct = Thread.currentThread();
        ClassLoader oldCl = ct.getContextClassLoader();

        for (int i = 0; i < mars.length; i++) {
            File f = new File(mars[i]);

            // assume file is "tranname-transform.mar"
            String fn = f.getName();
            String tranName = fn.substring(0, fn.length() - 4);

            try {
                URLClassLoader ucl = new URLClassLoader(new URL[] { f.toURL() });
                ct.setContextClassLoader(ucl);
                logger.info("Running TranReporter for " + tranName);
        TranReporter tranReporter = new TranReporter(outputDir, tranName, new JarFile(f), ucl);
        tranReporter.process(conn);
            } catch (Exception exn) {
                logger.warn("bad mar: " + f, exn);
            }
        }

    }

    private void purgeOldReports(Connection conn, int daysToKeep)
    {
        // DhcpMap.get().deleteAddressMap(conn);

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

}
