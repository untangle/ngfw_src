/**
 * $Id: ReportEntry.java,v 1.00 2015/02/24 15:19:32 dmorris Exp $
 */
package com.untangle.node.reporting;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.Date;
import java.util.Arrays;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.SqlCondition;

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

    private String uniqueId = null;
    private boolean enabled = true; /* If the report entry is "enabled" (shown) */
    private Boolean readOnly = null; /* If the rule is read-only (built-in) */

    private ReportEntryType type;

    private String title; /* title of the entry/graph */
    private String category; /* category of the entry/graph */
    private String description; /* A text description */
    private int displayOrder = 9999; /* The order to display this report entry (relative to others) */
    
    private String units;
    
    private boolean preCompileResults = false; /* if the results should be pre-compiled each night */
    private String table; /* table to query data from */
    private SqlCondition[] conditions;
    
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

    public String getUniqueId() { return uniqueId; }
    public void setUniqueId( String newValue ) { this.uniqueId = newValue; }

    public boolean getEnabled() { return enabled; }
    public void setEnabled( boolean newValue ) { this.enabled = newValue; }

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

    public int getDisplayOrder() { return this.displayOrder; }
    public void setDisplayOrder( int newValue ) { this.displayOrder = newValue; }

    public String getUnits() { return this.units; }
    public void setUnits( String newValue ) { this.units = newValue; }
    
    public boolean getPreCompileResults() { return this.preCompileResults; }
    public void setPreCompileResults( boolean newValue ) { this.preCompileResults = newValue; }
    
    public String getTable() { return this.table; }
    public void setTable( String newValue ) { this.table = newValue; }

    public SqlCondition[] getConditions() { return this.conditions; }
    public void setConditions( SqlCondition[] newValue ) { this.conditions = newValue; }
    
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
    
    public PreparedStatement toSql( Connection conn, Date startDate, Date endDate )
    {
        return toSql( conn, startDate, endDate, null );
    }

    public PreparedStatement toSql( Connection conn, Date startDate, Date endDate, SqlCondition[] extraConditions )
    {
        if ( endDate == null )
            endDate = new Date(); // now
        if ( startDate == null ) {
            logger.warn("startDate not specified, using 1 day ago");
            startDate = new Date((new Date()).getTime() - (1000 * 60 * 60 * 24));
        }

        LinkedList<SqlCondition> allConditions = new LinkedList<SqlCondition>();
        if ( getConditions() != null )
            allConditions.addAll( Arrays.asList(getConditions()) );
        if ( extraConditions != null )
            allConditions.addAll( Arrays.asList(extraConditions) );
        
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

            pie_query += conditionsToString( allConditions );

            pie_query += " GROUP BY " + getPieGroupColumn() + 
                ( getOrderByColumn() == null ? "" : " ORDER BY " + getOrderByColumn() + ( getOrderDesc() ? " DESC " : "" ));
            return sqlToStatement( conn, pie_query, allConditions );

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

            time_query += conditionsToString( allConditions );
                
            time_query += " GROUP BY time_trunc ";

            String final_query = "SELECT * FROM " +
                " ( " + generate_series + " ) as t1 " +
                "LEFT JOIN " +
                " ( " + time_query + " ) as t2 " +
                " USING (time_trunc) " +
                " ORDER BY time_trunc " + ( getOrderDesc() ? " DESC " : "" );
            return sqlToStatement( conn, final_query, allConditions );
            
        case TEXT:
            /* FIXME */
            throw new RuntimeException("IMPLEMENT ME");
        }

        throw new RuntimeException("Unknown Graph type: " + this.type);
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
   
    /**
     * takes a sql string and substitutes the condition arguments into and returns a prepared statement
     */
    private PreparedStatement sqlToStatement( Connection conn, String sql, LinkedList<SqlCondition> conditions )
    {
        Connection dbConnection = null;
        ReportingNodeImpl node = (ReportingNodeImpl) UvmContextFactory.context().nodeManager().node("untangle-node-reporting");

        if ( node == null ) {
            logger.warn("node not found.");
            return null;
        }
        
        try {
            dbConnection = node.getDbConnection();
            dbConnection.setAutoCommit(false);
        } catch (Exception e) {
            logger.warn("Unable to create connection to DB",e);
        }
        if ( dbConnection == null) {
            logger.warn("Unable to connect to DB.");
            throw new RuntimeException("Unable to connect to DB.");
        }

        try {
            java.sql.PreparedStatement statement = dbConnection.prepareStatement( sql );
            statement.setFetchDirection( java.sql.ResultSet.FETCH_FORWARD );

            if ( conditions != null ) {
                int i = 0;
                for ( SqlCondition condition : conditions ) {
                    i++;
                    String columnType = node.getReportingManagerNew().getColumnType( getTable(), condition.getColumn() );
                    String value = condition.getValue();

                    if ( value == null ) {
                        logger.warn("Ignoring bad condition: Invalid value: " + value );
                        throw new RuntimeException( "Invalid value: " + value );
                    }
                    if ( columnType == null ) {
                        logger.warn("Ignoring unknown column " + condition.getColumn() + " in table " + getTable() );
                        continue;
                    }
                    
                    switch (columnType) {
                    case "int8":
                    case "bigint":
                    case "int4":
                    case "int":
                    case "integer":
                    case "int2":
                    case "smallint":
                        try {
                            statement.setLong(i, Long.valueOf( value ));
                        } catch (Exception e) {
                            throw new RuntimeException( "Invalid number: " + value );
                        }
                    break;
                    
                    case "inet":
                        statement.setObject(i, value, java.sql.Types.OTHER);
                        break;
                    
                    case "bool":
                        if ( value.toLowerCase().contains("true") || value.toLowerCase().contains("1") )
                            statement.setBoolean(i, true);
                        else
                            statement.setBoolean(i, false);
                        break;

                    case "bpchar":
                    case "character":
                    case "varchar":
                    case "text":
                        statement.setString(i, condition.getValue());
                        break;
                    default:
                        logger.warn("Unknown column type: " + columnType);
                        continue;
                    }
                }
            }

            return statement;
        } catch ( Exception e) {
            logger.warn("SQL Exception:", e);
            throw new RuntimeException("SqlException",e);
        }

    }

    private String conditionsToString( LinkedList<SqlCondition> conditions )
    {
        String str = "";

        if ( conditions == null )
            return str;

        for ( SqlCondition condition : conditions ) {
            ReportingNodeImpl node = (ReportingNodeImpl) UvmContextFactory.context().nodeManager().node("untangle-node-reporting");
            String type = node.getReportingManagerNew().getColumnType( getTable(), condition.getColumn() );

            if ( type == null ) {
                logger.warn("Ignoring unknown column " + condition.getColumn() + " in table " + getTable() );
                continue;
            }
            
            String columnType = 
            str += " and " + condition.getColumn() + " " + condition.getOperator() + " ? ";
        }

        return str;
    }

}