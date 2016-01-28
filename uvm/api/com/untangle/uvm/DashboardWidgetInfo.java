/**
 * $Id: DashboardWidgetInfo.java,v 1.00 2015/01/28 14:34:27 vdumitrescu Exp $
 */
package com.untangle.uvm;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.node.reports.EventEntry;
import com.untangle.node.reports.ReportEntry;


/**
 * Dashboard widget aggregated information.
 */
@SuppressWarnings("serial")
public class DashboardWidgetInfo implements Serializable, JSONString
{
    private DashboardWidgetSettings widget;
    private ReportEntry reportEntry;
    private EventEntry eventEntry;
    
    public DashboardWidgetInfo() { }
    public DashboardWidgetInfo(DashboardWidgetSettings widget) {
        this.widget = widget;
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    public DashboardWidgetSettings getWidget() { return widget; }
    public void setWidget(DashboardWidgetSettings widget) { this.widget = widget; }

    public ReportEntry getReportEntry() { return reportEntry; }
    public void setReportEntry(ReportEntry reportEntry) { this.reportEntry = reportEntry; }

    public EventEntry getEventEntry() { return eventEntry; }
    public void setEventEntry(EventEntry eventEntry) { this.eventEntry = eventEntry; }

}
