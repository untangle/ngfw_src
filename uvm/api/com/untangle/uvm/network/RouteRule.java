/**
 * $Id: RouteRule.java 33317 2012-10-17 19:12:21Z dmorris $
 */
package com.untangle.uvm.network;

import java.util.List;
import java.io.Serializable;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

/**
 * This in the implementation of a Route Rule
 *
 * A rule is basically a collection of RouteRuleMatchers (matchers)
 * and what to do if the matchers match (block, log, etc)
 */
@SuppressWarnings("serial")
public class RouteRule implements JSONString, Serializable
{
    private final Logger logger = Logger.getLogger(getClass());

    private List<RouteRuleMatcher> matchers;

    private Integer ruleId;
    private Boolean enabled;
    private Boolean route;
    private String description;
    
    public RouteRule() { }

    public RouteRule(boolean enabled, List<RouteRuleMatcher> matchers, boolean route, String description)
    {
        this.setMatchers(matchers);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setRoute(route);
        this.setDescription(description);
    }
    
    public List<RouteRuleMatcher> getMatchers() { return this.matchers; }
    public void setMatchers( List<RouteRuleMatcher> matchers ) { this.matchers = matchers; }

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId(Integer ruleId) { this.ruleId = ruleId; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled( Boolean enabled ) { this.enabled = enabled; }

    public String getDescription() { return description; }
    public void setDescription( String description ) { this.description = description; }

    public Boolean getRoute() { return route; }
    public void setRoute( Boolean route ) { this.route = route; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}

