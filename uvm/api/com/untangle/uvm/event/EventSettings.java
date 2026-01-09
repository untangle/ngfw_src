/**
 * $Id$
 */
package com.untangle.uvm.event;

import java.util.LinkedList;
import java.io.Serializable;

import com.untangle.uvm.event.generic.EventSettingsGeneric;
import org.json.JSONObject;
import org.json.JSONString;

/**
 * Settings for the Reports App.
 */
@SuppressWarnings("serial")
public class EventSettings implements Serializable, JSONString
{
    private Integer version = 4;

    private LinkedList<AlertRule> alertRules = null;
    private LinkedList<SyslogRule> syslogRules = null;
    private LinkedList<TriggerRule> triggerRules = null;
    private LinkedList<SyslogServer> syslogServers = null;

    private boolean syslogEnabled = false;
    private String syslogHost;
    private int syslogPort = 514;
    private String syslogProtocol = "UDP";

    private String emailSubject = null;
    private String emailBody = null;
    private boolean emailConvert = true;

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

    public LinkedList<SyslogServer> getSyslogServers() { return this.syslogServers; }
    public void setSyslogServers( LinkedList<SyslogServer> newValue ) { this.syslogServers = newValue; }


    /**
     * Email template
     */
    public String getEmailSubject() { return emailSubject; }
    public void setEmailSubject( String emailSubject ) { this.emailSubject = emailSubject; }

    public String getEmailBody() { return emailBody; }
    public void setEmailBody( String emailBody ) { this.emailBody = emailBody; }

    public boolean getEmailConvert() { return emailConvert; }
    public void setEmailConvert( boolean emailConvert ) { this.emailConvert = emailConvert; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Transforms legacy Event Settings to Generic v2 format
     * Used for Vue UI
     * @return EventSettingsGeneric
     */
    public EventSettingsGeneric transformLegacyToGenericSettings() {
        EventSettingsGeneric eventSettingsGen = new EventSettingsGeneric();
        if (this.getAlertRules() != null)
            eventSettingsGen.setAlert_rules(AlertRule.transformAlertRulesToGeneric(this.getAlertRules()));
        eventSettingsGen.setEmailBody(emailBody);
        eventSettingsGen.setEmailSubject(emailSubject);
        eventSettingsGen.setEmailConvert(emailConvert);
        return eventSettingsGen;
    }
}
