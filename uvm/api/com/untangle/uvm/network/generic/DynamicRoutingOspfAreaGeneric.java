/**
 * $Id$
 */
package com.untangle.uvm.network.generic;

import java.util.LinkedList;
import java.io.Serializable;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.network.DynamicRouteOspfArea;
import com.untangle.uvm.network.DynamicRoutingSettings;

/**
 * Dynamic Route Ospf Area Generic
 */
@SuppressWarnings("serial")
public class DynamicRoutingOspfAreaGeneric implements JSONString, Serializable {

    private Integer ruleId = null;
    private String description = null;
    private String area = "";
    private Integer type = 0;
    private Integer authentication = 0;
    private LinkedList<InetAddress> virtualLinks = new LinkedList<>();

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId(Integer ruleId) { this.ruleId = ruleId; }

    public String getDescription() { return description; }
    public void setDescription( String description ) { this.description = description; }

    public String getArea() { return area; }
    public void setArea( String area ) { this.area = area;}

    public Integer getType() { return type; }
    public void setType( Integer type ) { this.type = type;}

    public Integer getAuthentication() { return authentication; }
    public void setAuthentication( Integer authentication ) { this.authentication = authentication;}

    public LinkedList<InetAddress> getVirtualLinks() { return virtualLinks; }
    public void setVirtualLinks( LinkedList<InetAddress> virtualLinks ) { this.virtualLinks = virtualLinks;}

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Transforms a {@link DynamicRoutingOspfAreaGeneric} object into its v1 DynamicRouteOspfArea representation.
     * @param legacyDynamicRouteOspfArea DynamicRouteOspfArea
     * @return DynamicRouteOspfArea
     */
    public static DynamicRouteOspfArea transformGenericToDynamicRoutingOspfAreas(DynamicRouteOspfArea legacyDynamicRouteOspfArea) {
        if (legacyDynamicRouteOspfArea == null) {
            return null;
        }
        DynamicRouteOspfArea dynamicRouteOspfArea = new DynamicRouteOspfArea();

        dynamicRouteOspfArea.setArea(legacyDynamicRouteOspfArea.getArea());
        dynamicRouteOspfArea.setAuthentication(legacyDynamicRouteOspfArea.getAuthentication());
        dynamicRouteOspfArea.setDescription(legacyDynamicRouteOspfArea.getDescription());
        dynamicRouteOspfArea.setRuleId(legacyDynamicRouteOspfArea.getRuleId());
        dynamicRouteOspfArea.setType(legacyDynamicRouteOspfArea.getType());
        dynamicRouteOspfArea.setVirtualLinks(
            legacyDynamicRouteOspfArea.getVirtualLinks() != null 
                ? new LinkedList<>(legacyDynamicRouteOspfArea.getVirtualLinks()) 
                : new LinkedList<>());
        return dynamicRouteOspfArea;
    }
}
