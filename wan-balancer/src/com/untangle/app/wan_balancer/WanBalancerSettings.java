/**
 * $Id$
 */
package com.untangle.app.wan_balancer;

import java.io.Serializable;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * wan balancer setting object
 */
@SuppressWarnings("serial")
public class WanBalancerSettings implements Serializable, JSONString
{
    private int version = 1;

    private int[] weights = null;
    private List<RouteRule> routeRules; 

    public WanBalancerSettings() { }

    public List<RouteRule> getRouteRules() { return this.routeRules; }
    public void setRouteRules( List<RouteRule> routeRules ) { this.routeRules = routeRules; }

    public void setWeights(int[] weights) { this.weights = weights; }
    public int[] getWeights() { return this.weights; }

    public int getVersion() { return this.version; }
    public void setVersion(int version) { this.version = version; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
