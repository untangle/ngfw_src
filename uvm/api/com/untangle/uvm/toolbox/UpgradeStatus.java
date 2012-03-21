/**
 * $Id: UpgradeStatus.java,v 1.00 2012/03/16 15:20:59 dmorris Exp $
 */
package com.untangle.uvm.toolbox;

import java.io.Serializable;

@SuppressWarnings("serial")
public class UpgradeStatus implements Serializable
{
    private final boolean upgradesAvailable;
    private volatile boolean updating = false;
    private volatile boolean upgrading = false;
    private volatile boolean installing = false;
    private volatile boolean removing = false;

    public UpgradeStatus(boolean updating, boolean upgrading, boolean installing, boolean removing, boolean upgradesAvailable)
    {
        this.updating = updating;
        this.upgrading = upgrading;
        this.installing = installing;
        this.removing = removing;
        this.upgradesAvailable = upgradesAvailable;
    }

    public boolean isUpdating()
    {
        return updating;
    }

    public boolean isUpgrading()
    {
        return upgrading;
    }

    public boolean isInstalling()
    {
        return installing;
    }

    public boolean isRemoving()
    {
        return removing;
    }

    public boolean getUpgradesAvailable()
    {
        return upgradesAvailable;
    }
}