/**
 * $Id: Application.java,v 1.00 2012/04/16 12:25:02 dmorris Exp $
 */
package com.untangle.uvm.toolbox;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Application implements Comparable<Application>, Serializable
{
    private final PackageDesc libItem;
    private final PackageDesc trialLibItem;
    private final PackageDesc node;

    public Application(PackageDesc libItem, PackageDesc trialLibItem, PackageDesc node)
    {
        this.libItem = libItem;
        this.trialLibItem = trialLibItem;
        this.node = node;
    }

    public PackageDesc getLibItem()
    {
        return libItem;
    }

    public PackageDesc getTrialLibItem()
    {
        return trialLibItem;
    }

    public PackageDesc getNode()
    {
        return node;
    }

    public int getViewPosition()
    {
        if (libItem != null &&
            libItem.getViewPosition() != PackageDesc.UNKNOWN_POSITION) {
            return libItem.getViewPosition();
        } else if (trialLibItem != null
                   && trialLibItem.getViewPosition() != PackageDesc.UNKNOWN_POSITION) {
            return trialLibItem.getViewPosition();
        } else if (node != null
                   && node.getViewPosition() != PackageDesc.UNKNOWN_POSITION) {
            return node.getViewPosition();
        } else {
            return -1;
        }
    }

    // Comparable methods ------------------------------------------------------

    public int compareTo(Application a)
    {
        return new Integer(getViewPosition()).compareTo(a.getViewPosition());
    }

    // Object methods ----------------------------------------------------------

    @Override
    public String toString()
    {
        return "(Application libItem: " + libItem + " trialLibItem: " + trialLibItem + " node: " + node + ")";
    }
}