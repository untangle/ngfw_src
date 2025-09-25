/**
 * $Id$
 */
package com.untangle.uvm.network.generic;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.network.DynamicRouteBgpNeighbor;
import com.untangle.uvm.network.DynamicRouteNetwork;
import com.untangle.uvm.network.DynamicRouteOspfArea;
import com.untangle.uvm.network.DynamicRouteOspfInterface;
import com.untangle.uvm.network.DynamicRoutingSettings;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * Dynamic Routing settings generic.
 */
@SuppressWarnings("serial")
public class DynamicRoutingSettingsGeneric implements Serializable, JSONString {

    private Boolean enabled = false;
    private Boolean bgpEnabled = false;
    private Boolean ospfEnabled = false;
    private Boolean ospfUseDefaultMetricEnabled = false;
    private Boolean ospfRedistConnectedEnabled = false;
    private Boolean ospfRedistStaticEnabled = false;
    private Boolean ospfRedistBgpEnabled = false;

    private Integer ospfDefaultMetric = 0;
    private Integer ospfAbrType = 0;
    private Integer ospfAutoCost = 0;
    private Integer ospfDefaultInformationOriginateType = 0;
    private Integer ospfDefaultInformationOriginateMetric = 0;
    private Integer ospfDefaultInformationOriginateExternalType = 1;
    private Integer ospfRedistConnectedMetric = 0;
    private Integer ospfRedistConnectedExternalType = 1;
    private Integer ospfRedistStaticMetric = 0;
    private Integer ospfRedistStaticExternalType = 1;
    private Integer ospfRedistBgpMetric = 0;
    private Integer ospfRedistBgpExternalType = 1;

    private String bgpRouterId = "";
    private String bgpRouterAs = "";
    private String ospfRouterId = "";

    private LinkedList<DynamicRouteBgpNeighbor> bgpNeighbors = new LinkedList<>();
    private LinkedList<DynamicRouteNetwork> bgpNetworks = new LinkedList<>();
    private LinkedList<DynamicRouteNetwork> ospfNetworks = new LinkedList<>();
    private LinkedList<DynamicRouteOspfArea> ospfAreas = new LinkedList<>();
    private LinkedList<DynamicRouteOspfInterface> ospfInterfaces = new LinkedList<>();

    public Boolean getEnabled() { return this.enabled; }
    public void setEnabled( Boolean newValue ) { this.enabled = newValue; }

    public Boolean getBgpEnabled() { return this.bgpEnabled; }
    public void setBgpEnabled( Boolean newValue ) { this.bgpEnabled = newValue; }

    public String getBgpRouterId() { return this.bgpRouterId; }
    public void setBgpRouterId( String newValue ) { this.bgpRouterId = newValue; }

    public String getBgpRouterAs() { return this.bgpRouterAs; }
    public void setBgpRouterAs( String newValue ) { this.bgpRouterAs = newValue; }

    public LinkedList<DynamicRouteBgpNeighbor> getBgpNeighbors() { return this.bgpNeighbors; }
    public void setBgpNeighbors( LinkedList<DynamicRouteBgpNeighbor> newValue ) { this.bgpNeighbors = newValue; }

    public LinkedList<DynamicRouteNetwork> getBgpNetworks() { return this.bgpNetworks; }
    public void setBgpNetworks( LinkedList<DynamicRouteNetwork> newValue ) { this.bgpNetworks = newValue; }

    public Boolean getOspfEnabled() { return this.ospfEnabled; }
    public void setOspfEnabled( Boolean newValue ) { this.ospfEnabled = newValue; }

    public String getOspfRouterId() { return this.ospfRouterId; }
    public void setOspfRouterId( String newValue ) { this.ospfRouterId = newValue; }

    public Boolean getOspfUseDefaultMetricEnabled() { return this.ospfUseDefaultMetricEnabled; }
    public void setOspfUseDefaultMetricEnabled( Boolean newValue ) { this.ospfUseDefaultMetricEnabled = newValue; }

    public Integer getOspfDefaultMetric() { return this.ospfDefaultMetric; }
    public void setOspfDefaultMetric( Integer newValue ) { this.ospfDefaultMetric = newValue; }

    public Integer getOspfAbrType() { return this.ospfAbrType; }
    public void setOspfAbrType( Integer newValue ) { this.ospfAbrType = newValue; }

    public Integer getOspfAutoCost() { return this.ospfAutoCost; }
    public void setOspfAutoCost( Integer newValue ) { this.ospfAutoCost = newValue; }

    public Integer getOspfDefaultInformationOriginateType() { return this.ospfDefaultInformationOriginateType; }
    public void setOspfDefaultInformationOriginateType( Integer newValue ) { this.ospfDefaultInformationOriginateType = newValue; }

    public Integer getOspfDefaultInformationOriginateMetric() { return this.ospfDefaultInformationOriginateMetric; }
    public void setOspfDefaultInformationOriginateMetric( Integer newValue ) { this.ospfDefaultInformationOriginateMetric = newValue; }

    public Integer getOspfDefaultInformationOriginateExternalType() { return this.ospfDefaultInformationOriginateExternalType; }
    public void setOspfDefaultInformationOriginateExternalType( Integer newValue ) { this.ospfDefaultInformationOriginateExternalType = newValue; }

    public Boolean getOspfRedistConnectedEnabled() { return this.ospfRedistConnectedEnabled; }
    public void setOspfRedistConnectedEnabled( Boolean newValue ) { this.ospfRedistConnectedEnabled = newValue; }

    public Integer getOspfRedistConnectedMetric() { return this.ospfRedistConnectedMetric; }
    public void setOspfRedistConnectedMetric( Integer newValue ) { this.ospfRedistConnectedMetric = newValue; }

    public Integer getOspfRedistConnectedExternalType() { return this.ospfRedistConnectedExternalType; }
    public void setOspfRedistConnectedExternalType( Integer newValue ) { this.ospfRedistConnectedExternalType = newValue; }

    public Boolean getOspfRedistStaticEnabled() { return this.ospfRedistStaticEnabled; }
    public void setOspfRedistStaticEnabled( Boolean newValue ) { this.ospfRedistStaticEnabled = newValue; }

    public Integer getOspfRedistStaticMetric() { return this.ospfRedistStaticMetric; }
    public void setOspfRedistStaticMetric( Integer newValue ) { this.ospfRedistStaticMetric = newValue; }

    public Integer getOspfRedistStaticExternalType() { return this.ospfRedistStaticExternalType; }
    public void setOspfRedistStaticExternalType( Integer newValue ) { this.ospfRedistStaticExternalType = newValue; }

    public Boolean getOspfRedistBgpEnabled() { return this.ospfRedistBgpEnabled; }
    public void setOspfRedistBgpEnabled( Boolean newValue ) { this.ospfRedistBgpEnabled = newValue; }

    public Integer getOspfRedistBgpMetric() { return this.ospfRedistBgpMetric; }
    public void setOspfRedistBgpMetric( Integer newValue ) { this.ospfRedistBgpMetric = newValue; }

    public Integer getOspfRedistBgpExternalType() { return this.ospfRedistBgpExternalType; }
    public void setOspfRedistBgpExternalType( Integer newValue ) { this.ospfRedistBgpExternalType = newValue; }

    public LinkedList<DynamicRouteNetwork> getOspfNetworks() { return this.ospfNetworks; }
    public void setOspfNetworks( LinkedList<DynamicRouteNetwork> newValue ) { this.ospfNetworks = newValue; }

    public LinkedList<DynamicRouteOspfArea> getOspfAreas() { return this.ospfAreas; }
    public void setOspfAreas( LinkedList<DynamicRouteOspfArea> newValue ) { this.ospfAreas = newValue; }

    public LinkedList<DynamicRouteOspfInterface> getOspfInterfaces() { return this.ospfInterfaces; }
    public void setOspfInterfaces( LinkedList<DynamicRouteOspfInterface> newValue ) { this.ospfInterfaces = newValue; }


    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Transforms a {@link DynamicRoutingSettingsGeneric} object into its v1 DynamicRoutingSettings representation.
     * @param legacyDynamicRoutingSettings DynamicRoutingSettings
     * @return legacyDynamicRoutingSettings
     */
    public DynamicRoutingSettings transformGenericToDynamicRoutingSettings(DynamicRoutingSettings legacyDynamicRoutingSettings) {
        if (legacyDynamicRoutingSettings == null)
            legacyDynamicRoutingSettings = new DynamicRoutingSettings();

        legacyDynamicRoutingSettings.setBgpNeighbors(this.getBgpNeighbors() != null ? this.getBgpNeighbors() : new LinkedList<>());
        legacyDynamicRoutingSettings.setBgpNetworks(this.getBgpNetworks() != null ? this.getBgpNetworks() : new LinkedList<>());
        legacyDynamicRoutingSettings.setOspfAreas(this.getOspfAreas() != null ? this.getOspfAreas() : new LinkedList<>());
        legacyDynamicRoutingSettings.setOspfInterfaces(this.getOspfInterfaces() != null ? this.getOspfInterfaces() : new LinkedList<>());
        legacyDynamicRoutingSettings.setOspfNetworks(this.getOspfNetworks() != null ? this.getOspfNetworks() : new LinkedList<>());
        legacyDynamicRoutingSettings.setBgpRouterAs(this.getBgpRouterAs());
        legacyDynamicRoutingSettings.setBgpRouterId(this.getBgpRouterId());
        legacyDynamicRoutingSettings.setBgpEnabled(this.getBgpEnabled());
        legacyDynamicRoutingSettings.setEnabled(this.getEnabled());
        legacyDynamicRoutingSettings.setOspfAbrType(this.getOspfAbrType());
        legacyDynamicRoutingSettings.setOspfAutoCost(this.getOspfAutoCost());
        legacyDynamicRoutingSettings.setOspfDefaultInformationOriginateExternalType(this.getOspfDefaultInformationOriginateExternalType());
        legacyDynamicRoutingSettings.setOspfDefaultInformationOriginateMetric(this.getOspfDefaultInformationOriginateMetric());
        legacyDynamicRoutingSettings.setOspfDefaultInformationOriginateType(this.getOspfDefaultInformationOriginateType());
        legacyDynamicRoutingSettings.setOspfDefaultMetric(this.getOspfDefaultMetric());
        legacyDynamicRoutingSettings.setOspfEnabled(this.getOspfEnabled());
        legacyDynamicRoutingSettings.setOspfRedistBgpEnabled(this.getOspfRedistBgpEnabled());
        legacyDynamicRoutingSettings.setOspfRedistBgpExternalType(this.getOspfRedistBgpExternalType());
        legacyDynamicRoutingSettings.setOspfRedistBgpMetric(this.getOspfRedistBgpMetric());
        legacyDynamicRoutingSettings.setOspfRedistConnectedEnabled(this.getOspfRedistConnectedEnabled());
        legacyDynamicRoutingSettings.setOspfRedistConnectedExternalType(this.getOspfRedistConnectedExternalType());
        legacyDynamicRoutingSettings.setOspfRedistConnectedMetric(this.getOspfRedistConnectedMetric());
        legacyDynamicRoutingSettings.setOspfRedistStaticEnabled(this.getOspfRedistStaticEnabled());
        legacyDynamicRoutingSettings.setOspfRedistStaticExternalType(this.getOspfRedistStaticExternalType());
        legacyDynamicRoutingSettings.setOspfRedistStaticMetric(this.getOspfRedistStaticMetric());
        legacyDynamicRoutingSettings.setOspfRouterId(this.getOspfRouterId());
        legacyDynamicRoutingSettings.setOspfUseDefaultMetricEnabled(this.getOspfUseDefaultMetricEnabled());
        return legacyDynamicRoutingSettings;
    }
}
