/**
 * $Id$
 */
package com.untangle.uvm.network.generic;

import com.untangle.uvm.app.IPMaskedAddress;
import com.untangle.uvm.network.StaticRoute;
import org.json.JSONObject;
import org.json.JSONString;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generic represesntation of @{StaticRoute}
 */
@SuppressWarnings("serial")
public class StaticRouteGeneric implements JSONString, Serializable {

    private Integer ruleId;
    private String description;
    private String network;
    private String nextHop;

    public StaticRouteGeneric() {}

    public StaticRouteGeneric(Integer ruleId, String description, String network, String nextHop) {
        this.ruleId = ruleId;
        this.description = description;
        this.network = network;
        this.nextHop = nextHop;
    }

    public Integer getRuleId() { return ruleId; }
    public void setRuleId(Integer ruleId) { this.ruleId = ruleId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getNetwork() { return network; }
    public void setNetwork(String network) { this.network = network; }
    public String getNextHop() { return nextHop; }
    public void setNextHop(String nextHop) { this.nextHop = nextHop; }

    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Transforms Static Routes Generic List to Static Routes List for vue UI set API
     * @param staticRouteGenList LinkedList<StaticRouteGeneric>
     * @param legacyStaticRoutes List<StaticRoute>
     * @return List<StaticRoute>
     */
    public static List<StaticRoute> transformGenericToStaticRoutes(LinkedList<StaticRouteGeneric> staticRouteGenList, List<StaticRoute> legacyStaticRoutes) {
        if (legacyStaticRoutes == null)
            legacyStaticRoutes = new LinkedList<>();

        // CLEANUP: Remove deleted routes first
        Set<Integer> incomingIds = staticRouteGenList.stream()
                .map(StaticRouteGeneric::getRuleId)
                .collect(Collectors.toSet());
        legacyStaticRoutes.removeIf(route -> !incomingIds.contains(route.getRuleId()));

        // Build a map for quick lookup by ruleId
        Map<Integer, StaticRoute> routesMap = legacyStaticRoutes.stream()
                .collect(Collectors.toMap(StaticRoute::getRuleId, Function.identity()));

        List<StaticRoute> staticRoutes = new LinkedList<>();
        for (StaticRouteGeneric staticRouteGeneric : staticRouteGenList) {
            StaticRoute staticRoute = routesMap.get(staticRouteGeneric.getRuleId());
            staticRoute = StaticRouteGeneric.transformStaticRoute(staticRouteGeneric, staticRoute);
            staticRoutes.add(staticRoute);
        }
        return staticRoutes;
    }

    /**
     * Transforms Static Routes Generic entity to Static Routes for vue UI set API
     * @param staticRouteGeneric StaticRouteGeneric
     * @param staticRoute StaticRoute
     * @return StaticRoute
     */
    private static StaticRoute transformStaticRoute(StaticRouteGeneric staticRouteGeneric, StaticRoute staticRoute) {
        if (staticRoute == null)
            staticRoute = new StaticRoute();

        staticRoute.setRuleId(staticRouteGeneric.getRuleId());
        staticRoute.setDescription(staticRouteGeneric.getDescription());
        if (staticRouteGeneric.getNetwork() != null) {
            IPMaskedAddress ipMaskedAddress = new IPMaskedAddress(staticRouteGeneric.getNetwork());
            staticRoute.setNetwork(ipMaskedAddress.getAddress());
            staticRoute.setPrefix(ipMaskedAddress.getPrefixLength());
        }
        staticRoute.setNextHop(staticRouteGeneric.getNextHop());
        return staticRoute;
    }
}
