/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.virus;

import java.io.Serializable;

/**
 * Virus scan result.
 *
 * @author <a href="mailto:amread@nyx.net">Aaron Read</a>
 * @version 1.0
 */
public class VirusScannerResult implements Serializable
{
    private static final long serialVersionUID = -9165160954531529727L;

    public static final VirusScannerResult CLEAN
        = new VirusScannerResult(true, "" , false);
    public static final VirusScannerResult INFECTED
        = new VirusScannerResult(false, "unknown", false);
    public static final VirusScannerResult CLEANED
        = new VirusScannerResult(true, "unknown", true);
    public static final VirusScannerResult ERROR
        = new VirusScannerResult(true, "unknown", false); // CLEAN

    private final boolean clean;
    private final String  virusName;
    private final boolean virusCleaned;

    // constructors -----------------------------------------------------------

    public VirusScannerResult(boolean clean, String virusName,
                              boolean virusCleaned)
    {
        this.clean = clean;
        this.virusName = virusName;
        this.virusCleaned = virusCleaned;
    }

    // accessors --------------------------------------------------------------

    /**
     * Not infected.
     *
     * @return true if not infected.
     */
    public boolean isClean()
    {
        return clean;
    }

    /**
     * Name of virus.
     *
     * @return the virus name.
     */
    public String getVirusName()
    {
        return virusName;
    }

    /**
     * True if the virus was cleaned.
     *
     * @return true if the virus was cleaned.
     */
    public boolean isVirusCleaned()
    {
        return virusCleaned;
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        if (clean)
            return "Clean";
        else
            return new String("Infected(" + virusName + ")");
    }
}
