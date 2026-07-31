/**
 * $Id$
 */
package com.untangle.uvm.event.generic;

import org.json.JSONObject;
import org.json.JSONString;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * This in the Generic Rule Action Class for Event Rules
 * Used for vue model transformations
 */
@SuppressWarnings("serial")
public class EventRuleActionGeneric implements JSONString, Serializable {
    /**
     * EMAIL, EMAIL_OFF - Required for Alert Rules
     */
    public enum Type { SYSLOG, EMAIL, EMAIL_OFF, TAG_HOST, TAG_DEVICE, TAG_USER, UNTAG_HOST, UNTAG_DEVICE, UNTAG_USER, IPS_DEFAULT, IPS_LOG, IPS_BLOCKLOG, IPS_BLOCK, IPS_DISABLE, IPS_WHITELIST }

    private EventRuleActionGeneric.Type type;

    public EventRuleActionGeneric.Type getType() { return type; }
    public void setType(EventRuleActionGeneric.Type type) { this.type = type; }

    // Required for Alert Rules
    private Boolean emailLimitFrequency = false;
    private Integer emailLimitFrequencyMinutes = 0;

    public Integer getEmailLimitFrequencyMinutes() { return emailLimitFrequencyMinutes; }
    public void setEmailLimitFrequencyMinutes(Integer emailLimitFrequencyMinutes) { this.emailLimitFrequencyMinutes = emailLimitFrequencyMinutes; }
    public Boolean getEmailLimitFrequency() { return emailLimitFrequency; }
    public void setEmailLimitFrequency(Boolean emailLimitFrequency) { this.emailLimitFrequency = emailLimitFrequency; }

    // Required for Trigger Rules
    private String tagTarget;       /* names the JSON entity for the target of the tag */
    private String tagName;
    private Long tagLifetimeSec;

    // Required for Intrusion Prevention Rules
    private String sourceNetworks;
    private String destinationNetworks;

    public String getSourceNetworks() { return sourceNetworks; }
    public void setSourceNetworks(String sourceNetworks) { this.sourceNetworks = sourceNetworks; }
    public String getDestinationNetworks() { return destinationNetworks; }
    public void setDestinationNetworks(String destinationNetworks) { this.destinationNetworks = destinationNetworks; }

    public String getTagTarget() { return tagTarget; }
    public void setTagTarget(String tagTarget) { this.tagTarget = tagTarget; }
    public String getTagName() { return tagName; }
    public void setTagName(String tagName) { this.tagName = tagName; }
    public Long getTagLifetimeSec() { return tagLifetimeSec; }
    public void setTagLifetimeSec(Long tagLifetimeSec) { this.tagLifetimeSec = tagLifetimeSec; }

    // Required for Syslog Rules
    private Boolean syslog = false;
    private LinkedList<Integer> syslogServers = null;

    public Boolean getSyslog() { return syslog; }
    public void setSyslog(Boolean syslog) { this.syslog = syslog; }

    public LinkedList<Integer> getSyslogServers() { return syslogServers; }
    public void setSyslogServers(LinkedList<Integer> syslogServers) {
        this.syslogServers = syslogServers;
    }

    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
