/**
 * $Id: ReportEntry.java,v 1.00 2015/02/24 15:19:32 dmorris Exp $
 */
package com.untangle.node.reporting;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;

/**
 * The settings for an individual report entry (graph)
 */
@SuppressWarnings("serial")
public class ReportEntry implements Serializable, JSONString
{
    private static final Logger logger = Logger.getLogger(ReportEntry.class);
    private static final DateFormat dateFormatter = new SimpleDateFormat("YYYY-MM-dd HH:mm");

    public static enum ReportEntryType {
        TEXT, /* A text entry */
            PIE_GRAPH, /* A top X pie chart graph */
            TIME_GRAPH /* A graph with time (minutes) on the x-axis */
            };

    public static enum TimeDataInterval {
        SECOND,
            MINUTE,
            HOUR,
            DAY,
            WEEK,
            MONTH,
            AUTO
            };

    private boolean enabled = true; /* If the report entry is "enabled" (shown) */
    private Boolean readOnly = null; /* If the rule is read-only (built-in) */

    private ReportEntryType type;

    private String title; /* title of the entry/graph */
    private String category; /* category of the entry/graph */
    private String description; /* A text description */

    private boolean preCompileResults = false; /* if the results should be pre-compiled each night */
    private String table; /* table to query data from */
    private ReportEntryCondition[] conditions;
    
    private String pieGroupColumn; /* the column to group by in top X charts (usually user, host, etc) */
    private String pieSumColumn; /* the column to sum in the top X charts */

    private TimeDataInterval timeDataInterval; /* The time interval to be used in time-based graphs */
    private String[] timeDataColumns; /* The data to graph by time */
    
    private String orderByColumn = null; /* The column to order by */
    private Boolean orderDesc = null; /* The direction to order, True is DESC, False is regular, null is neither */

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    public boolean getEnabled() { return enabled; }
    public void setEnabled( boolean enabled ) { this.enabled = enabled; }

    public Boolean getReadOnly() { return this.readOnly; }
    public void setReadOnly( Boolean newValue ) { this.readOnly = newValue; }
    
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

    public ReportEntryCondition[] getConditions() { return this.conditions; }
    public void setConditions( ReportEntryCondition[] newValue ) { this.conditions = newValue; }
    
    public String getPieGroupColumn() { return this.pieGroupColumn; }
    public void setPieGroupColumn( String newValue ) { this.pieGroupColumn = newValue; }

    public String getPieSumColumn() { return this.pieSumColumn; }
    public void setPieSumColumn( String newValue ) { this.pieSumColumn = newValue; }

    public String getOrderByColumn() { return this.orderByColumn; }
    public void setOrderByColumn( String newValue ) { this.orderByColumn = newValue; }

    public Boolean getOrderDesc() { return this.orderDesc; }
    public void setOrderDesc( Boolean newValue ) { this.orderDesc = newValue; }

    public TimeDataInterval getTimeDataInterval() { return this.timeDataInterval; }
    public void setTimeDataInterval( TimeDataInterval newValue ) { this.timeDataInterval = newValue; }

    public String[] getTimeDataColumns() { return this.timeDataColumns; }
    public void setTimeDataColumns( String[] newValue ) { this.timeDataColumns = newValue; }
    
    public String toSql( Date startDate, Date endDate )
    {
        return toSql( startDate, endDate, null );
    }

    public String toSql( Date startDate, Date endDate, ReportEntryCondition[] extraConditions )
    {
        if ( endDate == null )
            endDate = new Date(); // now
        if ( startDate == null ) {
            logger.warn("startDate not specified, using 1 day ago");
            startDate = new Date((new Date()).getTime() - (1000 * 60 * 60 * 24));
        }

        String dateCondition =
            " time_stamp > '" + dateFormatter.format(startDate) + "' " + " and " +
            " time_stamp < '" + dateFormatter.format(endDate) + "' ";
        
        switch ( this.type ) {

        case PIE_GRAPH:

            String pie_query = "SELECT " +
                getPieGroupColumn() + ", " + getPieSumColumn() + " as value " +
                " FROM " +
                " reports." + getTable() +
                " WHERE " + dateCondition;

            if ( getConditions() != null ) {
                for ( ReportEntryCondition condition : getConditions() ) {
                    pie_query += " and " + condition.getColumn() + " " + condition.getOperator() + " " + condition.getValue() + "";
                }
            }
            if ( extraConditions != null ) {
                for ( ReportEntryCondition condition : extraConditions ) {
                    pie_query += " and " + condition.getColumn() + " " + condition.getOperator() + " " + condition.getValue() + "";
                }
            }

            pie_query += " GROUP BY " + getPieGroupColumn() + 
                ( getOrderByColumn() == null ? "" : " ORDER BY " + getOrderByColumn() + ( getOrderDesc() ? " DESC " : "" ));
            return pie_query;

        case TIME_GRAPH:
            String dataInterval = calculateTimeDataInterval( startDate, endDate ).toString().toLowerCase();
            
            String generate_series = " SELECT generate_series( " +
                " date_trunc( '" + dataInterval + "', '" + dateFormatter.format(startDate) + "'::timestamp), " + 
                " '" + dateFormatter.format(endDate)   + "'::timestamp , " +
                " '1 " + dataInterval + "' ) as time_trunc ";
            
            String time_query = "SELECT " +
                " date_trunc( '" + dataInterval + "', time_stamp ) as time_trunc ";

            for ( String s : getTimeDataColumns() )
                time_query += ", " + s;

            time_query += " FROM " +
                " reports." + getTable() +
                " WHERE " + dateCondition;

            if ( getConditions() != null ) {
                for ( ReportEntryCondition condition : getConditions() ) {
                    time_query += " and " + condition.getColumn() + " " + condition.getOperator() + " " + condition.getValue() + "";
                }
            }
            if ( extraConditions != null ) {
                for ( ReportEntryCondition condition : extraConditions ) {
                    time_query += " and " + condition.getColumn() + " " + condition.getOperator() + " " + condition.getValue() + "";
                }
            }
                
            time_query += " GROUP BY time_trunc ";

            String final_query = "SELECT * FROM " +
                " ( " + generate_series + " ) as t1 " +
                "LEFT JOIN " +
                " ( " + time_query + " ) as t2 " +
                " USING (time_trunc) " +
                " ORDER BY time_trunc " + ( getOrderDesc() ? " DESC " : "" );
            return final_query;
            
        case TEXT:
            return "FIXME";
        }

        return "FIXME";
    }
    
    private TimeDataInterval calculateTimeDataInterval( Date startDate, Date endDate )
    {
        if ( this.timeDataInterval != TimeDataInterval.AUTO )
            return this.timeDataInterval;

        /* otherwise its auto, calculate a good interval based on the data */
        long timeDiffSec = ((endDate.getTime() - startDate.getTime())/1000);

        if ( timeDiffSec > ( 60 * 60 * 24 * 2 ) ) /* more than 2 days, use days */
            return TimeDataInterval.DAY;
        if ( timeDiffSec > ( 60 * 60 * 2 ) ) /* more than 2 hours */
            return TimeDataInterval.HOUR;
        else
            return TimeDataInterval.MINUTE;
    }
   
    static {
        try {
            ReportEntry entry = new ReportEntry();
            ReportEntryCondition condition = new ReportEntryCondition();
            Date oneDayAgo = new Date((new Date()).getTime() - (1000L * 60L * 60L * 24L));
            Date oneMonthAgo = new Date((new Date()).getTime() - (1000L * 60L * 60L * 24L * 30L));

            condition.setColumn("c_client_addr");
            condition.setOperator("=");
            condition.setValue("'10.21.68.172'");
            
            entry = new ReportEntry();
            entry.setEnabled( true );
            entry.setReadOnly( true );
            entry.setCategory("Web Filter");
            entry.setTitle("Top Web Browsing Hosts (by requests)");
            entry.setDescription("The number of web requests by each host.");
            entry.setTable("http_events");
            entry.setType(ReportEntry.ReportEntryType.PIE_GRAPH);
            entry.setPieSumColumn("count(*)");
            entry.setPieGroupColumn("hostname");
            entry.setOrderByColumn("value");
            entry.setOrderDesc(Boolean.TRUE);

            logger.warn("SQL: " + entry.toSql( oneDayAgo, null, null ));
            UvmContextFactory.context().settingsManager().save( System.getProperty("uvm.lib.dir") + "/" + "untangle-node-reporting/" + "top-web-browsing-hosts-by-requests.js", entry, false );

            entry = new ReportEntry();
            entry.setEnabled( true );
            entry.setReadOnly( true );
            entry.setCategory("Web Filter");
            entry.setTitle("Web Usage");
            entry.setDescription("The number of web requests by each host.");
            entry.setTable("http_events");
            entry.setType(ReportEntry.ReportEntryType.TIME_GRAPH);
            entry.setTimeDataInterval(ReportEntry.TimeDataInterval.AUTO);
            entry.setTimeDataColumns(new String[]{"count(*) as scanned", "sum(sitefilter_flagged::int) as flagged", "sum(sitefilter_blocked::int) as blocked"});
            //entry.setTimeDataColumns(new String[]{"coalesce(count(*),0) as scanned", "coalesce(sum(sitefilter_flagged::int),0) as flagged", "coalesce(sum(sitefilter_blocked::int),0) as blocked"});
            entry.setOrderDesc(Boolean.FALSE);

            logger.warn("SQL: " + entry.toSql( oneMonthAgo, null, null ));
            UvmContextFactory.context().settingsManager().save( System.getProperty("uvm.lib.dir") + "/" + "untangle-node-reporting/" + "web-usage.js", entry, false );
        
            entry = new ReportEntry();
            entry.setEnabled( true );
            entry.setReadOnly( false );
            entry.setCategory("Web Filter");
            entry.setTitle("Web Usage [10.21.68.172]");
            entry.setDescription("The number of web requests by each host.");
            entry.setTable("http_events");
            entry.setConditions(new ReportEntryCondition[] { condition });
            entry.setType(ReportEntry.ReportEntryType.TIME_GRAPH);
            entry.setTimeDataInterval(ReportEntry.TimeDataInterval.AUTO);
            entry.setTimeDataColumns(new String[]{"count(*) as scanned", "sum(sitefilter_flagged::int) as flagged", "sum(sitefilter_blocked::int) as blocked"});
            //entry.setTimeDataColumns(new String[]{"coalesce(count(*),0) as scanned", "coalesce(sum(sitefilter_flagged::int),0) as flagged", "coalesce(sum(sitefilter_blocked::int),0) as blocked"});
            entry.setOrderDesc(Boolean.FALSE);

            logger.warn("SQL: " + entry.toSql( oneMonthAgo, null, null ));
            // UvmContextFactory.context().settingsManager().save( System.getProperty("uvm.lib.dir") + "/" + "untangle-node-reporting/" + "webUsage.js", entry, false );
             
        } catch (Exception e) {
            logger.warn("Exception.",e);
        }
        
    }
}