/**
 * $Id$
 */

package com.untangle.app.virus_blocker;

import java.io.Serializable;

import com.untangle.app.smtp.TemplateValues;

/**
 * Virus scan result. This class also implements TemplateValues. There is only
 * one key which can be dereferenced VirusReport:VIRUS_NAME. This will be
 * replaced with the name of the virus which was found.
 */
@SuppressWarnings("serial")
public class VirusScannerResult implements Serializable, TemplateValues
{
    private static final String VIRUS_NAME_KEY = "VirusReport:VIRUS_NAME";

    public static final VirusScannerResult CLEAN = new VirusScannerResult(true, "");
    public static final VirusScannerResult ERROR = new VirusScannerResult(true, "unknown"); // CLEAN

    private final boolean clean;
    private final String virusName;

    /**
     * Constructor
     * 
     * @param clean
     *        Clean flag
     * @param virusName
     *        Virus name
     */
    public VirusScannerResult(boolean clean, String virusName)
    {
        this.clean = clean;
        this.virusName = virusName;
    }

    /**
     * Gets the clean flag
     * 
     * @return The clean flag
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
     * 
     * For use in Templates (see JavaDoc at the top of this class for
     * explanation of the key which can be used).
     * 
     * @param key
     *        The key
     * @return The template value
     */
    public String getTemplateValue(String key)
    {
        key = key.trim().toLowerCase();
        if (key.equalsIgnoreCase(VIRUS_NAME_KEY)) {
            return getVirusName();
        }
        return null;
    }

    /**
     * Check for equality
     * 
     * @param o
     *        The object for comparison
     * 
     * @return True if equal, otherwise false
     */
    public boolean equals(Object o)
    {
        if (!(o instanceof VirusScannerResult)) {
            return false;
        } else {
            VirusScannerResult vsr = (VirusScannerResult) o;
            return clean == vsr.clean && (null == virusName ? null == vsr.virusName : virusName.equals(vsr.virusName));
        }
    }

    /**
     * Convert result to string
     * 
     * @return The result in string format
     */
    public String toString()
    {
        if (clean) {
            return "Clean";
        } else {
            return new String("Infected(" + virusName + ")");
        }
    }

    /**
     * Get a hash of the virus name
     * 
     * @return The hash of a virus name
     */
    public int hashCode()
    {
        if (virusName != null) return virusName.hashCode();
        else return 0;
    }
}
