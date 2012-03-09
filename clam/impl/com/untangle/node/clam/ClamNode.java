/**
 * $Id$
 */
package com.untangle.node.clam;

import com.untangle.node.virus.VirusNodeImpl;

public class ClamNode extends VirusNodeImpl
{
    protected int getStrength()
    {
        return 15;
    }

    public String getName()
    {
        return "clam";
    }
    
    public ClamNode()
    {
        super(new ClamScanner());
    }
}
