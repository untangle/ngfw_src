/**
 * $Id$
 */
package com.untangle.node.virus;

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
