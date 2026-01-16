/**
 * $Id$
 */
package com.untangle.uvm.event.generic;

import com.untangle.uvm.event.EventSettings;
import com.untangle.uvm.event.SyslogServer;
import com.untangle.uvm.generic.RuleGeneric;
import org.json.JSONObject;
import org.json.JSONString;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * Generic Settings for the Report App Events.
 */
@SuppressWarnings("serial")
public class EventSettingsGeneric implements Serializable, JSONString {

    private LinkedList<EventRuleGeneric> alert_rules;
    private LinkedList<SyslogServer> syslogServers = new LinkedList<>();

    private String emailSubject = null;
    private String emailBody = null;
    private boolean emailConvert = true;


    public LinkedList<EventRuleGeneric> getAlert_rules() { return alert_rules; }
    public void setAlert_rules(LinkedList<EventRuleGeneric> alert_rules) { this.alert_rules = alert_rules; }

    public LinkedList<SyslogServer> getSyslogServers() { return this.syslogServers; }
    public void setSyslogServers( LinkedList<SyslogServer> newValue ) { this.syslogServers = newValue; } 

    public String getEmailSubject() { return emailSubject; }
    public void setEmailSubject(String emailSubject) { this.emailSubject = emailSubject; }
    public String getEmailBody() { return emailBody; }
    public void setEmailBody(String emailBody) { this.emailBody = emailBody; }
    public boolean isEmailConvert() { return emailConvert; }
    public void setEmailConvert(boolean emailConvert) { this.emailConvert = emailConvert; }
       
    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Transforms Generic (v2) Event Settings to Legacy v2 format
     * Used for Vue UI
     * @param eventSettings EventSettings
     */
    public void transformGenericToLegacySettings(EventSettings eventSettings) {
        if (eventSettings == null)
            eventSettings = new EventSettings();

        if (this.getAlert_rules() != null)
            eventSettings.setAlertRules(EventRuleGeneric.transformGenericToLegacyAlertRules(this.getAlert_rules(), eventSettings.getAlertRules()));
        
        if(this.getSyslogServers() != null)
            eventSettings.setSyslogServers(this.syslogServers);

        eventSettings.setEmailSubject(this.getEmailSubject());
        eventSettings.setEmailBody(this.getEmailBody());
        eventSettings.setEmailConvert(this.isEmailConvert());
    }
}
