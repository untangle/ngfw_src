/**
 * $Id: DashboardSettings.java,v 1.00 2015/11/10 14:34:27 dmorris Exp $
 */
package com.untangle.uvm;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONString;


/**
 * Dashboard settings.
 */
@SuppressWarnings("serial")
public class DashboardSettings implements Serializable, JSONString
{
    private Integer version;
    private Integer timeframe; //number of seconds in the past for startDate when getting data for reports and events
    private static enum Theme {
        DEFAULT,
        DARK,
        SAND
    };

    private List<DashboardWidgetSettings> widgets = new LinkedList<DashboardWidgetSettings>();
    private Theme theme = Theme.DEFAULT;

    public DashboardSettings() { }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    public List<DashboardWidgetSettings> getWidgets(){ return widgets; }
    public void setWidgets( List<DashboardWidgetSettings> newValue) { this.widgets = newValue; }

    public Integer getVersion() { return this.version; }
    public void setVersion( Integer newValue ) { this.version = newValue ; }

    public Integer getTimeframe(){ return timeframe; }
    public void setTimeframe( Integer newValue) { this.timeframe = newValue; }

    public Theme getTheme(){ return this.theme; }
    public void setTheme( Theme newValue) { this.theme = newValue; }

}
