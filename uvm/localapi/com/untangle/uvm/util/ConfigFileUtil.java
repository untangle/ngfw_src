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
package com.untangle.uvm.util;

import java.io.BufferedWriter;
import java.io.FileWriter;

import com.untangle.uvm.UvmContextFactory;
import org.apache.log4j.Logger;

public class ConfigFileUtil {

    private static final Logger logger = Logger.getLogger(ConfigFileUtil.class);

    private static final String CHMOD_CMD          = "/bin/chmod";
    private static final String CHMOD_PROTECT_CMD          = "/bin/chmod go-rwx ";

    public static void writeFile( StringBuilder sb, String fileName )
    {
        BufferedWriter out = null;

        /* Open up the interfaces file */
        try {
            String data = sb.toString();

            out = new BufferedWriter(new FileWriter( fileName ));
            out.write( data, 0, data.length());
        } catch ( Exception ex ) {
            /* XXX May need to catch this exception, restore defaults
             * then try again */
            logger.error( "Error writing file " + fileName + ":", ex );
        }

        try {
            if ( out != null )
                out.close();
        } catch ( Exception ex ) {
        }
    }

    // Used to make file unreadable by other than owner (root).
    public static void protectFile(String fileName)
    {
        int code;

        try {
            logger.debug( "Protecting " + fileName );

            String command = CHMOD_PROTECT_CMD + fileName;
            Process p = UvmContextFactory.context().exec(command);
            code = p.waitFor();
        } catch ( Exception e ) {
            logger.error( "Unable to protect " + fileName, e );
            return;
        }

        if ( code != 0 ) {
            logger.error( "Error protecting " + fileName + ": " + code );
        }
    }
}
