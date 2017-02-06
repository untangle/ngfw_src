/**
 * $Id$
 */
package com.untangle.uvm.event;

import java.util.List;
import java.util.LinkedList;
import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.node.IPMaskedAddress;
import com.untangle.uvm.node.DayOfWeekMatcher;
import com.untangle.uvm.event.EventRule;

/**
 * Settings for the Reports Node.
 */
@SuppressWarnings("serial")
public class EventSettings implements Serializable, JSONString
{
    private Integer version = 1;

    private LinkedList<EventRule> eventRules = null;

    private boolean syslogEnabled = false;
    private String syslogHost;
    private int syslogPort = 514;
    private String syslogProtocol = "UDP";

    public EventSettings() { }

    public Integer getVersion() { return version; }
    public void setVersion( Integer newValue ) { this.version = newValue; }

    public LinkedList<EventRule> getEventRules() { return this.eventRules; }
    public void setEventRules( LinkedList<EventRule> newValue ) { this.eventRules = newValue; }

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


    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
