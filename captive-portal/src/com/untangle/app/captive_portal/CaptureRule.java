/**
 * $Id$
 */

package com.untangle.app.captive_portal;

import java.util.List;
import java.io.Serializable;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.vnet.AppSession;

/**
 * This in the implementation of a Capture Rule
 * 
 * A rule is basically a collection of CaptureRuleConditions (matchers) and what
 * to do if the matchers match (capture, log, etc)
 */

@SuppressWarnings("serial")
public class CaptureRule implements JSONString, Serializable
{
    private final Logger logger = Logger.getLogger(getClass());

    private List<CaptureRuleCondition> matchers;

    private Integer ruleId;
    private Boolean enabled;
    private Boolean capture;
    private String description;

    public CaptureRule()
    {
    }

    public CaptureRule(boolean enabled, List<CaptureRuleCondition> matchers, boolean capture, String description)
    {
        this.setConditions(matchers);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setCapture(Boolean.valueOf(capture));
        this.setDescription(description);
    }

    public List<CaptureRuleCondition> getConditions()
    {
        return this.matchers;
    }

    public void setConditions(List<CaptureRuleCondition> matchers)
    {
        this.matchers = matchers;
    }

    /**
     * Use RuleId instead Kept for backwards compatability
     */
    public Integer getId()
    {
        return this.ruleId;
    }

    /**
     * Use RuleId instead Kept for backwards compatability
     */
    public void setId(Integer ruleId)
    {
        this.ruleId = ruleId;
    }

    public Integer getRuleId()
    {
        return this.ruleId;
    }

    public void setRuleId(Integer ruleId)
    {
        this.ruleId = ruleId;
    }

    public Boolean getEnabled()
    {
        return enabled;
    }

    public void setEnabled(Boolean enabled)
    {
        this.enabled = enabled;
    }

    public Boolean getCapture()
    {
        return capture;
    }

    public void setCapture(Boolean capture)
    {
        this.capture = capture;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    public boolean isMatch(short protocol, int srcIntf, int dstIntf, InetAddress srcAddress, InetAddress dstAddress, int srcPort, int dstPort)
    {
        if (!getEnabled())
            return false;

        /**
         * If no matchers return true
         */
        if (this.matchers == null) {
            logger.warn("Null matchers - assuming true");
            return true;
        }

        /**
         * IF any matcher doesn't match - return false
         */
        for (CaptureRuleCondition matcher : matchers) {
            if (!matcher.matches(protocol, srcIntf, dstIntf, srcAddress, dstAddress, srcPort, dstPort))
                return false;
        }

        /**
         * Otherwise all match - so the rule matches
         */
        return true;
    }

    public boolean isMatch( AppSession sess )
    {
        if (!getEnabled())
            return false;

        /**
         * If no matchers return true
         */
        if (this.matchers == null) {
            logger.warn("Null matchers - assuming true");
            return true;
        }

        /**
         * IF any matcher doesn't match - return false
         */
        for (CaptureRuleCondition matcher : matchers) {
            if ( ! matcher.matches( sess ) )
                return false;
        }

        /**
         * Otherwise all match - so the rule matches
         */
        return true;
    }
}
