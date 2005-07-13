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


public class Reporter
{

    private static final String SYMLINK_CMD = "/bin/ln -s";
    private static final Logger logger = Logger.getLogger(Reporter.class);

    // The base, containing one directory for each day's generated reports
    private static File outputBaseDir;

    // Today
    private static File outputDir;



    private Reporter(File outputBaseDir)
    {
        this.outputBaseDir = outputBaseDir;

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
                
            Reporter reporter = new Reporter(outputBaseDir);
            reporter.generateNewReports(conn, args);
            reporter.purgeOldReports(daysToKeep);

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


    private void generateNewReports(Connection conn, String[] args)
        throws IOException, JRScriptletException, SQLException, ClassNotFoundException
    {
	
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
		TranReporter tranReporter = new TranReporter(outputDir, tranName, new JarFile(f), ucl);
		tranReporter.process(conn);
            } catch (Exception exn) {
                logger.warn("bad mar: " + f);
                exn.printStackTrace();
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

}
