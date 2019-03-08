/**
 * $Id$
 */

package com.untangle.app.virus_blocker;

import java.util.regex.Pattern;

/**
 * Virus pattern
 */
public class VirusPattern
{

    protected Pattern pattern = null;
    protected boolean scan = false;

    /**
     * Constructor
     * 
     * @param pat
     *        The pattern
     * @param scan
     *        The scan flag
     */
    public VirusPattern(Pattern pat, boolean scan)
    {
        this.pattern = pat;
        this.scan = scan;
    }
}
