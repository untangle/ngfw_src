/**
 * $Id$
 */
package com.untangle.uvm.event;

import java.util.LinkedList;
import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.event.AlertRule;
import com.untangle.uvm.event.SyslogRule;

/**
 * Settings for the Reports App.
 */
@SuppressWarnings("serial")
public class EventSettings implements Serializable, JSONString
{
    private Integer version = 1;

    private LinkedList<AlertRule> alertRules = null;
    private LinkedList<SyslogRule> syslogRules = null;
    private LinkedList<TriggerRule> triggerRules = null;

    private boolean syslogEnabled = false;
    private String syslogHost;
    private int syslogPort = 514;
    private String syslogProtocol = "UDP";

    public EventSettings() { }

    public Integer getVersion() { return version; }
    public void setVersion( Integer newValue ) { this.version = newValue; }

    public LinkedList<AlertRule> getAlertRules() { return this.alertRules; }
    public void setAlertRules( LinkedList<AlertRule> newValue ) { this.alertRules = newValue; }

    /*
     Remote syslog
     */
    public boolean getSyslogEnabled() { return syslogEnabled; }
    public void setSyslogEnabled( boolean syslogEnabled ) { this.syslogEnabled = syslogEnabled; }

    public String getSyslogHost() { return syslogHost; }
    public void setSyslogHost( String syslogHost ) { this.syslogHost = syslogHost; }

    public int getSyslogPort() { return syslogPort; }
    public void setSyslogPort( int syslogPort ) { this.syslogPort = syslogPort; }

    public String getSyslogProtocol() { return syslogProtocol; }
    public void setSyslogProtocol( String syslogProtocol ) { this.syslogProtocol = syslogProtocol; }

    public LinkedList<SyslogRule> getSyslogRules() { return this.syslogRules; }
    public void setSyslogRules( LinkedList<SyslogRule> newValue ) { this.syslogRules = newValue; }

    public LinkedList<TriggerRule> getTriggerRules() { return this.triggerRules; }
    public void setTriggerRules( LinkedList<TriggerRule> newValue ) { this.triggerRules = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
