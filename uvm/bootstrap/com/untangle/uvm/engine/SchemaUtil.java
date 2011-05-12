/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * Internal utilities for dealing with the database schema.
 *
 * Currently, we run the update-schema script which runs SQL scripts
 * to initialize or update database schemas. In the future we may use
 * Ruby scripts to drive the schema upgrade process, but it will
 * retains backwards compatibility with the SQL scripts.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class SchemaUtil
{
    private static final int PORT = 2345;

    private final Logger logger = Logger.getLogger(getClass());

    private final Set<String> converts = new HashSet<String>();

    private Process proc;
    private Socket sock;
    private PrintWriter out;
    private BufferedReader in;

    // constructors -----------------------------------------------------------

    /**
     * Package protected.
     */
    SchemaUtil() { 
        initDaemonAndSocket();
    }

    protected void finalize() {
        try {
            in.close();
            out.close();
            sock.close();
            proc.destroy();
        } catch (Exception ex) { //fine
        }
    }

    // private methods --------------------------------------------------------

    private void initDaemonAndSocket() {
        finalize();

        String bd = System.getProperty("uvm.home") + "/bin/";
        String us = bd + "ut-update-schema";
        ProcessBuilder pb = new ProcessBuilder(us);
        try {
            logger.info("About to start daemon " + us);
            Process proc = pb.start();
        } catch (IOException e) {
            logger.error("Couldn't start ut-update-schema", e);
        }

        try {
            Thread.sleep(1000);
        } catch (Exception javaYouReReallyPissingMeOff) {
        }

        try {
            sock = new Socket("localhost", PORT);
            out = new PrintWriter(sock.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        } catch (UnknownHostException ex) {
            // localhost, ffs
        } catch (IOException ex) {
            logger.error("No I/O for ut-update-schema...", ex);
        }
    }

    // public methods ---------------------------------------------------------

    /**
     * Initialize component schema.
     *
     * XXX we need timeout and barf behavior
     *
     * @param type the schema to initialize, either "events" or
     * "schema".
     * @param component name of the component to initialize.
     */
    public void initSchema(String type, String component)
    {
        String key = type + "," + component;

        logger.info("initializing schema: " + key);

        synchronized (converts) {
            while (converts.contains(key)) {
                try {
                    converts.wait();
                } catch (InterruptedException exn) {
                    // doesn't happen, XXX need a destroy method?
                }
            }
        }

        try {
            out.println(type + " " + component);
            in.readLine();
        } catch (IOException exn) { // retry...
            initDaemonAndSocket();

            try {
                out.println(type + " " + component);
                in.readLine();
            } catch (IOException ex) {
                logger.warn("error in update-schema", ex);
            }
        } finally {
            synchronized (converts) {
                logger.info("Removing key " + key);
                converts.remove(key);
                converts.notifyAll();
            }
        }
    }
}
