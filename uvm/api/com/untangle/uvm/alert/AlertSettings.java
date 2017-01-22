/**
 * $Id$
 */
package com.untangle.uvm.alert;

import java.util.List;
import java.util.LinkedList;
import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.node.IPMaskedAddress;
import com.untangle.uvm.node.DayOfWeekMatcher;
import com.untangle.uvm.alert.AlertRule;

/**
 * Settings for the Reports Node.
 */
@SuppressWarnings("serial")
public class AlertSettings implements Serializable, JSONString
{
    private Integer version = 1;

    private LinkedList<AlertRule> alertRules = null;

    public AlertSettings() { }

    public Integer getVersion() { return version; }
    public void setVersion( Integer newValue ) { this.version = newValue; }

    public LinkedList<AlertRule> getAlertRules() { return this.alertRules; }
    public void setAlertRules( LinkedList<AlertRule> newValue ) { this.alertRules = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
