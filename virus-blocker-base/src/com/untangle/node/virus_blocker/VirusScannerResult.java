/**
 * $Id$
 */
package com.untangle.node.virus_blocker;

import java.io.Serializable;

import com.untangle.node.smtp.TemplateValues;

/**
 * Virus scan result.  <br><br> This class also implements {@link
 * com.untangle.uvm.util.TemplateValues TemplateValues}.  There is
 * only one key which can be dereferenced -
 * <code>VirusReport:VIRUS_NAME</code>.  This will be replaced with
 * the name of the virus which was found.
 */
@SuppressWarnings("serial")
public class VirusScannerResult implements Serializable, TemplateValues
{
    private static final String VIRUS_NAME_KEY = "VirusReport:VIRUS_NAME";

    public static final VirusScannerResult CLEAN = new VirusScannerResult(true, "");
    public static final VirusScannerResult ERROR = new VirusScannerResult(true, "unknown"); // CLEAN

    private final boolean clean;
    private final String  virusName;

    public VirusScannerResult(boolean clean, String virusName )
    {
        this.clean = clean;
        this.virusName = virusName;
    }

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
     * For use in Templates (see JavaDoc at the top of this class
     * for explanation of the key which can be used).
     */
    public String getTemplateValue(String key)
    {
        key = key.trim().toLowerCase();
        if(key.equalsIgnoreCase(VIRUS_NAME_KEY)) {
            return getVirusName();
        }
        return null;
    }

    public boolean equals(Object o)
    {
        if (!(o instanceof VirusScannerResult)) {
            return false;
        } else {
            VirusScannerResult vsr = (VirusScannerResult)o;
            return clean == vsr.clean && (null == virusName ? null == vsr.virusName : virusName.equals(vsr.virusName));
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

    public int hashCode()
    {
        if ( virusName != null )
            return virusName.hashCode();
        else
            return 0;
    }
}
