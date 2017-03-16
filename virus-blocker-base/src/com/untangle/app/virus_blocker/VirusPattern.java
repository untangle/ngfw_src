/**
 * $Id$
 */
package com.untangle.app.virus_blocker;

import java.util.regex.Pattern;

public class VirusPattern {

    protected Pattern pattern = null;
    protected boolean scan = false;

    public VirusPattern (Pattern pat, boolean scan)
    {
        this.pattern = pat;
        this.scan = scan;
    }
}
