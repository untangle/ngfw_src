/**
 * $Id: SslInspectorRuleAction.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.app.ssl_inspector;

import java.io.Serializable;
import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

/**
 * Class used to represent the action associated with a rule
 * 
 * @author mahotz
 * 
 */
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

    // THIS IS FOR ECLIPSE - @formatter:off

    public ActionType getActionType() { return this.action; }
    public void setActionType(ActionType action) { this.action = action; }

    public Boolean getFlag() { return this.flag; }
    public void setFlag(Boolean flag) { this.flag = flag; }

    // THIS IS FOR ECLIPSE - @formatter:on

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
