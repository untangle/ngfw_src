/**
 * $Id: UPnPSettings.java 37267 2016-07-25 23:42:19Z cblaise $
 */
package com.untangle.uvm.network;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * QoS settings.
 */
@SuppressWarnings("serial")
public class UpnpSettings implements Serializable, JSONString
{
    private boolean upnpEnabled = false;
    private boolean secureMode = true;
    private int listenPort = 5000;
    
    private List<UpnpRule> upnpRules = new LinkedList<>();

    public boolean getUpnpEnabled() { return this.upnpEnabled; }
    public void setUpnpEnabled( boolean newValue ) { this.upnpEnabled = newValue; }

    public boolean getSecureMode() { return this.secureMode; }
    public void setSecureMode( boolean newValue ) { this.secureMode = newValue; }

    public int getListenPort() { return this.listenPort; }
    public void setListenPort( int newValue ) { this.listenPort = newValue; }

    public List<UpnpRule> getUpnpRules() { return this.upnpRules; }
    public void setUpnpRules( List<UpnpRule> newValue ) { this.upnpRules = newValue; }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}