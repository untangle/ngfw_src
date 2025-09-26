/**
 * $Id: UPnPSettings.java 37267 2016-07-25 23:42:19Z cblaise $
 */
package com.untangle.uvm.network.generic;

import com.untangle.uvm.generic.RuleGeneric;
import com.untangle.uvm.network.UpnpSettings;
import org.json.JSONObject;
import org.json.JSONString;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * UPnP settings generic.
 */
@SuppressWarnings("serial")
public class UpnpSettingsGeneric implements Serializable, JSONString {
    private boolean upnpEnabled = false;
    private boolean secureMode = true;
    private int listenPort = 5000;

    private LinkedList<RuleGeneric> upnp_rules = new LinkedList<>();

    public boolean isUpnpEnabled() { return upnpEnabled; }
    public void setUpnpEnabled(boolean upnpEnabled) { this.upnpEnabled = upnpEnabled; }
    public boolean isSecureMode() { return secureMode; }
    public void setSecureMode(boolean secureMode) { this.secureMode = secureMode; }
    public int getListenPort() { return listenPort; }
    public void setListenPort(int listenPort) { this.listenPort = listenPort; }

    public LinkedList<RuleGeneric> getUpnp_rules() { return upnp_rules; }
    public void setUpnp_rules(LinkedList<RuleGeneric> upnp_rules) { this.upnp_rules = upnp_rules; }

    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    public UpnpSettings transformGenericToUnpnSettings(UpnpSettings upnpSettings) {
        if (upnpSettings == null)
            upnpSettings = new UpnpSettings();

        upnpSettings.setUpnpEnabled(this.isUpnpEnabled());
        upnpSettings.setSecureMode(this.isSecureMode());
        upnpSettings.setListenPort(this.getListenPort());

        if (this.getUpnp_rules() != null)
            upnpSettings.setUpnpRules(RuleGeneric.transformGenericToUpnpRules(this.getUpnp_rules(), upnpSettings.getUpnpRules()));

        return upnpSettings;
    }
}
