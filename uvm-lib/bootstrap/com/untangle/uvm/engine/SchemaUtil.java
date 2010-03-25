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

import java.io.IOException;
import java.io.InputStream;
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
    private final Logger logger = Logger.getLogger(getClass());

    private final Set<String> converts = new HashSet<String>();

    // constructors -----------------------------------------------------------

    /**
     * Package protected.
     */
    SchemaUtil() { }

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
            String bd = System.getProperty("uvm.home") + "/bin/";
            String us = bd + "update-schema";
            ProcessBuilder pb = new ProcessBuilder(us, type, component);
            Process p = pb.start();
            InputStream is = p.getInputStream();
            // XXX we log in the script, maybe move up to here
            for (byte[] b = new byte[1024]; 0 <= is.read(b); );

            boolean tryAgain;
            do {
                tryAgain = false;
                try {
                    p.waitFor();
                } catch (InterruptedException exn) {
                    // can happen from the EventLogger
                    logger.debug("waiting for update-schema");
                    tryAgain = true;
                }
            } while (tryAgain);

        } catch (IOException exn) {
            logger.warn("error in update-schema", exn);
        } finally {
            synchronized (converts) {
                converts.remove(key);
                converts.notifyAll();
            }
        }
    }
}
