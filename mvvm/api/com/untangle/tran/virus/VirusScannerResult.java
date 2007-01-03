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
package com.untangle.tran.virus;

import java.io.Serializable;

import com.untangle.mvvm.tran.TemplateValues;

/**
 * Virus scan result.  <br><br> This class also implements {@link
 * com.untangle.tran.util.TemplateValues TemplateValues}.  There is
 * only one key which can be derefferenced -
 * <code>VirusReport:VIRUS_NAME</code>.  This will be replaced with
 * the name of the virus which was found.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class VirusScannerResult
    implements Serializable, TemplateValues {

    private static final long serialVersionUID = -9165160954531529727L;

    private static final String VIRUS_NAME_KEY = "VirusReport:VIRUS_NAME";

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

    /**
    * For use in Templates (see JavaDoc at the top of this class
    * for explanation of the key which can be used).
    */
    public String getTemplateValue(String key) {
      key = key.trim().toLowerCase();
      if(key.equalsIgnoreCase(VIRUS_NAME_KEY)) {
        return getVirusName();
      }
      return null;
    }

    // Object methods ---------------------------------------------------------

    public boolean equals(Object o)
    {
        if (!(o instanceof VirusScannerResult)) {
            return false;
        } else {
            VirusScannerResult vsr = (VirusScannerResult)o;
            return clean == vsr.clean
                && (null == virusName
                    ? null == virusName : virusName.equals(vsr.virusName))
                && virusCleaned == vsr.virusCleaned;
        }
    }

    public String toString()
    {
        if (clean) {
            return "Clean";
        } else {
            return new String("Infected(" + virusName + ")");
        }
    }
}
