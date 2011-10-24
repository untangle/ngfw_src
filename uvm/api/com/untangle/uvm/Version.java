/*
 * $Id$
 */
package com.untangle.uvm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Provides access to the UVM version.
 *
 * @author <a href="mailto:dmorris@untangle.com">Dirk Morris</a>
 * @version 1.0
 */
public class Version
{
    /**
     * Get the public version number.
     *
     * @return the version string.
     */
    public static String getVersion()
    {
        return getVersion("PUBVERSION");
    }

    /**
     * Get the public version number.
     *
     * @return the version string.
     */
    public static String getFullVersion()
    {
        return getVersion("VERSION");
    }

    private static String getVersion(String type)
    {
        String line = "unknown version";

        try {
            InputStream is = Version.class.getClassLoader().getResourceAsStream(type);
            if (is != null) {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader bis = new BufferedReader(isr);
                line = bis.readLine();
            }
        } catch (IOException exn) {
            System.out.println("Exception reading: " + type + " " + exn);
        }

        return line;
    }
}
