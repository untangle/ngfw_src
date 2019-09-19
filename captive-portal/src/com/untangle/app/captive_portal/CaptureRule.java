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
import com.untangle.uvm.vnet.SessionAttachments;

/**
 * This in the implementation of a Capture Rule
 * 
 * A rule is basically a collection of CaptureRuleConditions (matchers) and what
 * to do if the matchers match (capture, log, etc)
 * 
 * @author mahotz
 * 
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

// THIS IS FOR ECLIPSE - @formatter:off
    
    public List<CaptureRuleCondition> getConditions() { return this.matchers; }
    public void setConditions(List<CaptureRuleCondition> matchers) { this.matchers = matchers; }

    // NOTE: Use getRuleId and setRuleId instead. Kept for backwards compatability only.
    public Integer getId() { return this.ruleId; }
    public void setId(Integer ruleId) { this.ruleId = ruleId; }

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId(Integer ruleId) { this.ruleId = ruleId; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    
    public Boolean getCapture() { return capture; }
    public void setCapture(Boolean capture) { this.capture = capture; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

// THIS IS FOR ECLIPSE - @formatter:on

    /**
     * @return A String of this rule in JSON format
     */
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Checks to see if a connection matches this rule
     * 
     * @param protocol
     *        The network protocol
     * @param srcIntf
     *        The source interface
     * @param dstIntf
     *        The destination interface
     * @param srcAddress
     *        The source address
     * @param dstAddress
     *        The destination address
     * @param srcPort
     *        The source port
     * @param dstPort
     *        The destination port
     * @param attachments
     *        attachments
     * @return True if this rule matches, otherwise false
     */
    public boolean isMatch(short protocol, int srcIntf, int dstIntf, InetAddress srcAddress, InetAddress dstAddress, int srcPort, int dstPort, SessionAttachments attachments)
    {
        if (!getEnabled()) return false;

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
            if (!matcher.matches(protocol, srcIntf, dstIntf, srcAddress, dstAddress, srcPort, dstPort, attachments)) return false;
        }

        /**
         * Otherwise all match - so the rule matches
         */
        return true;
    }

    /**
     * Checks to see if a session matches this rule.
     * 
     * @param sess
     *        The session object
     * @return True if this rule matches, otherwise false
     */
    public boolean isMatch(AppSession sess)
    {
        if (!getEnabled()) return false;

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
            if (!matcher.matches(sess)) return false;
        }

        /**
         * Otherwise all match - so the rule matches
         */
        return true;
    }
}
