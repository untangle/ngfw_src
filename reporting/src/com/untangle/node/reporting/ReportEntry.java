/**
 * $Id: ReportEntry.java,v 1.00 2015/02/24 15:19:32 dmorris Exp $
 */
package com.untangle.node.reporting;

import java.util.LinkedList;
import java.util.Date;
import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * The settings for an individual report entry (graph)
 */
@SuppressWarnings("serial")
public class ReportEntry implements Serializable, JSONString
{
    public static enum ReportEntryType {
        TEXT, /* A text entry */
            PIE_GRAPH, /* A top X pie chart graph */
            TIME_GRAPH /* A graph with time (minutes) on the x-axis */
            };

    private ReportEntryType type;

    private String title; /* title of the entry/graph */
    private String category; /* category of the entry/graph */
    private String description; /* A text description */

    private boolean preCompileResults = false; /* if the results should be pre-compiled each night */
    private String table; /* table to query data from */

    private String pieGroupColumn; /* the column to group by in top X charts (usually user, host, etc) */
    private String pieSumColumn; /* the column to sum in the top X charts */
    
    private String orderByColumn = null; /* The column to order by */
    private Boolean orderDesc = null; /* The direction to order, True is DESC, False is regular, null is neither */

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    public ReportEntryType getType() { return this.type; }
    public void setType( ReportEntryType newValue ) { this.type = newValue; }

    public String getCategory() { return this.category; }
    public void setCategory( String newValue ) { this.category = newValue; }

    public String getTitle() { return this.title; }
    public void setTitle( String newValue ) { this.title = newValue; }

    public String getDescription() { return this.description; }
    public void setDescription( String newValue ) { this.description = newValue; }

    public boolean getPreCompileResults() { return this.preCompileResults; }
    public void setPreCompileResults( boolean newValue ) { this.preCompileResults = newValue; }
    
    public String getTable() { return this.table; }
    public void setTable( String newValue ) { this.table = newValue; }

    public String getPieGroupColumn() { return this.pieGroupColumn; }
    public void setPieGroupColumn( String newValue ) { this.pieGroupColumn = newValue; }

    public String getPieSumColumn() { return this.pieSumColumn; }
    public void setPieSumColumn( String newValue ) { this.pieSumColumn = newValue; }

    public String getOrderByColumn() { return this.orderByColumn; }
    public void setOrderByColumn( String newValue ) { this.orderByColumn = newValue; }

    public Boolean getOrderDesc() { return this.orderDesc; }
    public void setOrderDesc( Boolean newValue ) { this.orderDesc = newValue; }

    public String toSql( Date startDate, Date endDate )
    {
        switch ( this.type ) {

        case PIE_GRAPH:
            return "select " +
                getPieGroupColumn() + ", " + getPieSumColumn() + " as value " +
                " from " +
                "reports." + getTable() +
                " group by " + getPieGroupColumn() + 
                ( getOrderByColumn() == null ? "" : " order by " + getOrderByColumn() + ( getOrderDesc() ? " DESC " : "" ));
        case TIME_GRAPH:
            return "FIXME";
        case TEXT:
            return "FIXME";
        }

        return "FIXME";
    }
}