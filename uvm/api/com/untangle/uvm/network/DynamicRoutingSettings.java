/**
 * $Id: NetflowSettings.java 37267 2016-07-25 23:42:19Z cblaise $
 */
package com.untangle.uvm.network;

import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.List;
import java.util.LinkedList;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * Dynamic Routing settings
 */
@SuppressWarnings("serial")
public class DynamicRoutingSettings implements Serializable, JSONString
{
    private final Logger logger = Logger.getLogger(this.getClass());

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

    private List<DynamicRouteBgpNeighbor> bgpNeighbors = new LinkedList<>();
    private List<DynamicRouteNetwork> bgpNetworks = new LinkedList<>();
    private List<DynamicRouteNetwork> ospfNetworks = new LinkedList<>();
    private List<DynamicRouteOspfArea> ospfAreas = new LinkedList<>();
    private List<DynamicRouteOspfInterface> ospfInterfaces = new LinkedList<>();

    public Boolean getEnabled() { return this.enabled; }
    public void setEnabled( Boolean newValue ) { this.enabled = newValue; }

    public Boolean getBgpEnabled() { return this.bgpEnabled; }
    public void setBgpEnabled( Boolean newValue ) { this.bgpEnabled = newValue; }

    public String getBgpRouterId() { return this.bgpRouterId; }
    public void setBgpRouterId( String newValue ) { this.bgpRouterId = newValue; }

    public String getBgpRouterAs() { return this.bgpRouterAs; }
    public void setBgpRouterAs( String newValue ) { this.bgpRouterAs = newValue; }

    public List<DynamicRouteBgpNeighbor> getBgpNeighbors() { return this.bgpNeighbors; }
    public void setBgpNeighbors( List<DynamicRouteBgpNeighbor> newValue ) { this.bgpNeighbors = newValue; }

    public List<DynamicRouteNetwork> getBgpNetworks() { return this.bgpNetworks; }
    public void setBgpNetworks( List<DynamicRouteNetwork> newValue ) { this.bgpNetworks = newValue; }


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

    public List<DynamicRouteNetwork> getOspfNetworks() { return this.ospfNetworks; }
    public void setOspfNetworks( List<DynamicRouteNetwork> newValue ) { this.ospfNetworks = newValue; }

    public List<DynamicRouteOspfArea> getOspfAreas() { return this.ospfAreas; }
    public void setOspfAreas( List<DynamicRouteOspfArea> newValue ) { this.ospfAreas = newValue; }

    public List<DynamicRouteOspfInterface> getOspfInterfaces() { return this.ospfInterfaces; }
    public void setOspfInterfaces( List<DynamicRouteOspfInterface> newValue ) { this.ospfInterfaces = newValue; }


    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
