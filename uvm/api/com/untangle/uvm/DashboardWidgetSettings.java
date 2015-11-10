/**
 * $Id: DashboardWidget.java,v 1.00 2015/11/10 14:34:27 dmorris Exp $
 */
package com.untangle.uvm;

import java.io.Serializable;
import java.util.List;
import java.util.LinkedList;

import org.json.JSONObject;
import org.json.JSONString;


/**
 * Dashboard widget settings.
 */
@SuppressWarnings("serial")
public class DashboardWidgetSettings implements Serializable, JSONString
{
    private String type = null;
    private int refreshIntervalSec = 30;
    /* TODO add other parameters here */
    
    public DashboardWidgetSettings() { }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    public String getType(){ return type; }
    public void setType( String newValue) { this.type = newValue; }

    public int getRefreshIntervalSec(){ return refreshIntervalSec; }
    public void setRefreshIntervalSec( int newValue) { this.refreshIntervalSec = newValue; }

}
