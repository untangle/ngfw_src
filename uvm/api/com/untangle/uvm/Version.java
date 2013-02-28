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
 */
public class Version
{
    /**
     * Get the public version number.
     * "A" (10)
     */
    public static String getMajorVersion()
    {
        return getResource("MAJORVERSION");
    }

    /**
     * Get the public version number.
     * "A.B" (10.0)
     */
    public static String getVersion()
    {
        return getResource("PUBVERSION");
    }

    /**
     * Get the public version number.
     * "A.B.C" (10.0.1)
     */
    public static String getFullVersion()
    {
        return getResource("VERSION");
    }

    /**
     * Get the public version as a name IE: "focus"
     * "focus"
     */
    public static String getVersionName()
    {
        return getResource("RELEASE_CODENAME");
    }
    
    private static String getResource(String type)
    {
        String line = null;

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
