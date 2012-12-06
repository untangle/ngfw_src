/**
 * $Id: PassedAddress.java,v 1.00 2011/12/14 01:02:03 mahotz Exp $
 */

package com.untangle.node.capture;

import java.net.InetAddress;

@SuppressWarnings("serial")
public class PassedAddress
{
    private boolean live = true;
    private boolean log = false;
    private InetAddress address = null;
    private String description = null;

    public InetAddress getAddress() { return this.address; }
    public void setAddress( InetAddress newValue ) { this.address = newValue; }

    public boolean getLive() { return live; }
    public void setLive(boolean live) { this.live = live; }

    public boolean getLog() { return log; }
    public void setLog(boolean log) { this.log = log; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
