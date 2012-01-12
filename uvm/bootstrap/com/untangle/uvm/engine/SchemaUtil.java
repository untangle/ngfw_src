/**
 * $Id$
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
 */
public class SchemaUtil
{
    private static final int PORT = 2345;

    private final Logger logger = Logger.getLogger(getClass());

    private final Set<String> converts = new HashSet<String>();

    private Process proc = null;
    private Socket sock = null;
    private PrintWriter out = null;
    private BufferedReader in = null;

    protected SchemaUtil()
    { 
        initDaemonAndSocket();
    }

    public synchronized void close()
    {
        try { in.close(); } catch (Exception ex) { }
        try { out.close(); } catch (Exception ex) { }
        try { sock.close(); } catch (Exception ex) { }
        try { proc.destroy(); } catch (Exception ex) { }
        in = null;
        out = null;
        sock = null;
        proc = null;
    }

    /**
     * Initialize component schema.
     *
     * XXX we need timeout and barf behavior
     *
     * @param type the schema to initialize (typically "events" or "settings")
     * @param component name of the component to initialize.
     */
    public synchronized void initSchema(String type, String component)
    {
        if (in == null | out == null || sock == null || proc == null) {
            initDaemonAndSocket();
        }

        String key = type + "," + component;
        String output = "";

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
            output = in.readLine();
        } catch (IOException exn) { // retry...
            logger.warn("Exception during ut-update-schema, retrying...", exn);
            
            initDaemonAndSocket();

            try {
                out.println(type + " " + component);
                output = in.readLine();
            } catch (IOException ex) {
                logger.warn("error in update-schema", ex);
            }
        } finally {
            logger.info("Got input from ut-update-schema: " + output);
            synchronized (converts) {
                logger.info("Removing key " + key);
                converts.remove(key);
                converts.notifyAll();
            }
        }
    }

    private void initDaemonAndSocket()
    {
        close();

        String bd = System.getProperty("uvm.home") + "/bin/";
        String us = bd + "ut-update-schema";
        ProcessBuilder pb = new ProcessBuilder(us);

        try {
            logger.info("About to start daemon " + us);
            proc = pb.start();
        } catch (IOException e) {
            logger.error("Couldn't start ut-update-schema", e);
        }

        try { Thread.sleep(1000); } catch (Exception e) { }

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
    
}
