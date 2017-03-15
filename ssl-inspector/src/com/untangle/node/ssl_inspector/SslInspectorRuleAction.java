/*
 * $Id: SslInspectorRuleAction.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.node.ssl_inspector;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.vnet.AppSession;

@SuppressWarnings("serial")
public class SslInspectorRuleAction implements JSONString, Serializable
{
    private final Logger logger = Logger.getLogger(getClass());

    public enum ActionType
    {
        INSPECT, IGNORE, BLOCK
    }

    private ActionType action = null;
    private Boolean flag = Boolean.FALSE;

    public SslInspectorRuleAction()
    {
    }

    public SslInspectorRuleAction(ActionType action, Boolean flag)
    {
        setActionType(action);
        setFlag(flag);
    }

    /**
     * Get the action type for this action
     */
    public ActionType getActionType()
    {
        return this.action;
    }

    /**
     * Set the action type for this action
     */
    public void setActionType(ActionType action)
    {
        this.action = action;
    }

    /**
     * Should this rule be flagged
     */
    public Boolean getFlag()
    {
        return this.flag;
    }

    /**
     * Set the flag action for this rule
     */
    public void setFlag(Boolean flag)
    {
        this.flag = flag;
    }

    /**
     * This prints this action in JSON format This is used for JSON
     * serialization
     */
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
