/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */
package com.untangle.node.virus;

import java.io.Serializable;

import com.untangle.uvm.node.TemplateValues;

/**
 * Virus scan result.  <br><br> This class also implements {@link
 * com.untangle.node.util.TemplateValues TemplateValues}.  There is
 * only one key which can be dereferenced -
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
