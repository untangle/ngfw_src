/**
 * $Id$
 */
package com.untangle.app.wan_balancer.generic;

import java.io.Serializable;
import java.util.LinkedList;

import com.untangle.app.wan_balancer.RouteRule;
import com.untangle.app.wan_balancer.WanBalancerSettings;
import com.untangle.uvm.generic.RuleGeneric;
import org.json.JSONObject;
import org.json.JSONString;

/**
 * Generic (V2) settings for the WAN Balancer app, consumed by the Vue UI.
 * Keeps V1 field names where structurally identical; transforms only the
 * routeRules list into the shared RuleGeneric shape.
 */
@SuppressWarnings("serial")
public class WanBalancerSettingsGeneric implements Serializable, JSONString {

    private int version = 1;
    private int[] weights = null;

    // The ONLY transformed list - type changes from List<RouteRule> to LinkedList<RuleGeneric>
    // snake_case follows the V2 convention for transformed rule lists
    private LinkedList<RuleGeneric> route_rules = new LinkedList<>();

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public int[] getWeights() { return weights; }
    public void setWeights(int[] weights) { this.weights = weights; }

    public LinkedList<RuleGeneric> getRoute_rules() { return route_rules; }
    public void setRoute_rules(LinkedList<RuleGeneric> route_rules) { this.route_rules = route_rules; }


    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Transforms this V2 settings object into V1 by mutating the passed-in
     * V1 settings object. Preserves any V1-only fields not exposed in V2.
     *
     * @param v1 current V1 settings (mutated in place)
     * @return the same v1 reference, populated from this V2 object
     */
    public WanBalancerSettings transformGenericToWanBalancerSettings(WanBalancerSettings v1) {
        if (v1 == null) v1 = new WanBalancerSettings();

        v1.setVersion(this.version);
        v1.setWeights(this.weights);

        if (this.route_rules != null)
            v1.setRouteRules(RouteRule.transformGenericToRouteRules(this.route_rules, v1.getRouteRules()));

        return v1;
    }
}
