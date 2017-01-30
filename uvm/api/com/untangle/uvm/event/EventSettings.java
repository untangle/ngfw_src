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

    public EventSettings() { }

    public Integer getVersion() { return version; }
    public void setVersion( Integer newValue ) { this.version = newValue; }

    public LinkedList<EventRule> getEventRules() { return this.eventRules; }
    public void setEventRules( LinkedList<EventRule> newValue ) { this.eventRules = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
