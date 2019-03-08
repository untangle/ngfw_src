/**
 * $Id: DashboardWidget.java,v 1.00 2015/11/10 14:34:27 dmorris Exp $
 */
package com.untangle.uvm;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;


/**
 * Dashboard widget settings.
 */
@SuppressWarnings("serial")
public class DashboardWidgetSettings implements Serializable, JSONString
{
    public static enum Size {
        SMALL,
        MEDIUM,
        LARGE,
        XLARGE
    };

    private boolean enabled = true;
    private String type = null;
    private Integer refreshIntervalSec; //0= never auto refresh
    private String entryId = null;
    private String[] displayColumns;
    private Size size = Size.MEDIUM;

    public DashboardWidgetSettings() { }
    public DashboardWidgetSettings(String type) {
        this.type = type;
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getType(){ return type; }
    public void setType( String newValue) { this.type = newValue; }

    public Integer getRefreshIntervalSec(){ return refreshIntervalSec; }
    public void setRefreshIntervalSec( Integer newValue) { this.refreshIntervalSec = newValue; }

    public String getEntryId() { return entryId; }
    public void setEntryId(String entryId) { this.entryId = entryId; }

    public String[] getDisplayColumns() { return displayColumns; }
    public void setDisplayColumns(String[] columns) { this.displayColumns = columns; }

    public Size getSize(){ return size; }
    public void setSize( Size newValue) { this.size = newValue; }

    /**
     * DEPRECATED
     * The timeframe setting is now in the dashboard settings
     */
    public Integer getTimeframe(){ return null; }
    public void setTimeframe( Integer newValue) { return; }

}
