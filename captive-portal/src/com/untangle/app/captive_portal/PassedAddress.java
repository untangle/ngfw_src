/**
 * $Id$
 */

package com.untangle.app.captive_portal;

import com.untangle.uvm.app.IPMatcher;

@SuppressWarnings("serial")
public class PassedAddress
{
    private boolean live = true;
    private boolean log = false;
    private IPMatcher address = IPMatcher.getNilMatcher();
    private String description = null;

    public IPMatcher getAddress()
    {
        return this.address;
    }

    public void setAddress(IPMatcher newValue)
    {
        this.address = newValue;
    }

    public boolean getLive()
    {
        return live;
    }

    public void setLive(boolean live)
    {
        this.live = live;
    }

    public boolean getLog()
    {
        return log;
    }

    public void setLog(boolean log)
    {
        this.log = log;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }
}
