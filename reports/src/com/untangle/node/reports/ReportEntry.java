/**
 * $Id: ReportEntry.java,v 1.00 2015/02/24 15:19:32 dmorris Exp $
 */
package com.untangle.node.reports;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONString;

/**
 * The settings for an individual report entry (graph)
 */
@SuppressWarnings("serial")
public class ReportEntry implements Serializable, JSONString
{
    private static final Logger logger = Logger.getLogger(ReportEntry.class);
    private static final DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public static enum ReportEntryType {
        TEXT, /* A text entry */
        PIE_GRAPH, /* A top X pie chart graph */
        TIME_GRAPH, /* A graph with time (minutes) on the x-axis */
        TIME_GRAPH_DYNAMIC /* A graph with time (minutes) on the x-axis with dynamic columns */
    };

    public static enum TimeDataInterval {
        SECOND,
        MINUTE,
        TENMINUTE,
        HOUR,
        DAY,
        WEEK,
        MONTH,
        AUTO
    };

    public static enum TimeStyle {
        BAR,
        BAR_OVERLAPPED,
        BAR_3D,
        BAR_3D_OVERLAPPED,
        LINE,
        AREA
    };
    
    private String uniqueId = null;
    private boolean enabled = true; /* If the report entry is "enabled" (shown) */
    private Boolean readOnly = null; /* If the rule is read-only (built-in) */
    
    private ReportEntryType type;

    private String title; /* title of the entry/graph */
    private String category; /* category of the entry/graph */
    private String description; /* A text description */
    private int displayOrder = 9999; /* The order to display this report entry (relative to others) */
    private String seriesRenderer; /* The renderer that can be used to display the column/series name */
    
    private String units;
    private String[] colors; /* The colors of the columns/lines/pie slices */
    
    private String table; /* table to query data from */
    private SqlCondition[] conditions;
    
    private String pieGroupColumn; /* the column to group by in top X charts (usually user, host, etc) */
    private String pieSumColumn; /* the column to sum in the top X charts */
    private Integer pieNumSlices; /* the default number of pie slices shown (the excess will be groupde into "others") */

    private String textString; /* The string representation of the text */
    private String[] textColumns; /* The data to graph by time */
    
    private TimeDataInterval timeDataInterval; /* The time interval to be used in time-based graphs */
    private TimeStyle timeStyle; /* The time chart type (line/bar/etc) */
    private String[] timeDataColumns; /* The column/data to graph by time */

    private String timeDataDynamicValue; /* The value to graph (the column in "table") */
    private String timeDataDynamicColumn;  /* The columns to create the dynamic column based on (the distinct values) */
    private Integer timeDataDynamicLimit;  /* The columns to create the dynamic column based on (the distinct values) */
    private String timeDataDynamicAggregationFunction; /* The way to aggregate multiple values per date_trun (max, avg, etc) */
    
    private String orderByColumn = null; /* The column to order by */
    private Boolean orderDesc = null; /* The direction to order, True is DESC, False is regular, null is neither */

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    public ReportEntryType getType() { return this.type; }
    public void setType( ReportEntryType newValue )
    {
        this.type = newValue;

        // initialize sane defaults
        switch ( this.type ) {
        case PIE_GRAPH:
            if ( pieNumSlices == null )
                setPieNumSlices( 10 );
            break;
        case TIME_GRAPH:
            if ( timeStyle == null )
                setTimeStyle( TimeStyle.BAR );
            break;
        }
    }

    public String getUniqueId() { return uniqueId; }
    public void setUniqueId( String newValue ) { this.uniqueId = newValue; }

    public boolean getEnabled() { return enabled; }
    public void setEnabled( boolean newValue ) { this.enabled = newValue; }

    public Boolean getReadOnly() { return this.readOnly; }
    public void setReadOnly( Boolean newValue ) { this.readOnly = newValue; }
    
    public String getCategory() { return this.category; }
    public void setCategory( String newValue ) { this.category = newValue; }

    public String getTitle() { return this.title; }
    public void setTitle( String newValue ) { this.title = newValue; }

    public String getDescription() { return this.description; }
    public void setDescription( String newValue ) { this.description = newValue; }

    public int getDisplayOrder() { return this.displayOrder; }
    public void setDisplayOrder( int newValue ) { this.displayOrder = newValue; }

    public String getSeriesRenderer() { return this.seriesRenderer; }
    public void setSeriesRenderer( String newValue ) { this.seriesRenderer = newValue; }
    
    public String getUnits() { return this.units; }
    public void setUnits( String newValue ) { this.units = newValue; }
    
    public String getTable() { return this.table; }
    public void setTable( String newValue ) { this.table = newValue; }

    public SqlCondition[] getConditions() { return this.conditions; }
    public void setConditions( SqlCondition[] newValue ) { this.conditions = newValue; }
    
    public String getPieGroupColumn() { return this.pieGroupColumn; }
    public void setPieGroupColumn( String newValue ) { this.pieGroupColumn = newValue; }

    public String getPieSumColumn() { return this.pieSumColumn; }
    public void setPieSumColumn( String newValue ) { this.pieSumColumn = newValue; }

    public Integer getPieNumSlices() { return this.pieNumSlices; }
    public void setPieNumSlices( Integer newValue ) { this.pieNumSlices = newValue; }
    
    public String getTextString() { return this.textString; }
    public void setTextString( String newValue ) { this.textString = newValue; }

    public String[] getTextColumns() { return this.textColumns; }
    public void setTextColumns( String[] newValue ) { this.textColumns = newValue; }
    
    public String getOrderByColumn() { return this.orderByColumn; }
    public void setOrderByColumn( String newValue ) { this.orderByColumn = newValue; }

    public Boolean getOrderDesc() { return this.orderDesc; }
    public void setOrderDesc( Boolean newValue ) { this.orderDesc = newValue; }

    public TimeDataInterval getTimeDataInterval() { return this.timeDataInterval; }
    public void setTimeDataInterval( TimeDataInterval newValue ) { this.timeDataInterval = newValue; }

    public TimeStyle getTimeStyle() { return this.timeStyle; }
    public void setTimeStyle( TimeStyle newValue ) { this.timeStyle = newValue; }
    
    public String[] getTimeDataColumns() { return this.timeDataColumns; }
    public void setTimeDataColumns( String[] newValue ) { this.timeDataColumns = newValue; }

    public String getTimeDataDynamicValue() { return this.timeDataDynamicValue; }
    public void setTimeDataDynamicValue( String newValue ) { this.timeDataDynamicValue = newValue; }

    public String getTimeDataDynamicColumn() { return this.timeDataDynamicColumn; }
    public void setTimeDataDynamicColumn( String newValue ) { this.timeDataDynamicColumn = newValue; }

    public Integer getTimeDataDynamicLimit() { return this.timeDataDynamicLimit; }
    public void setTimeDataDynamicLimit( Integer newValue ) { this.timeDataDynamicLimit = newValue; }
    
    public String getTimeDataDynamicAggregationFunction() { return this.timeDataDynamicAggregationFunction; }
    public void setTimeDataDynamicAggregationFunction( String newValue ) { this.timeDataDynamicAggregationFunction = newValue; }
    
    public String[] getColors() { return this.colors; }
    public void setColors( String[] newValue ) { this.colors = newValue; }
    
    public PreparedStatement toSql( Connection conn, Date startDate, Date endDate )
    {
        return toSql( conn, startDate, endDate, null );
    }

    public PreparedStatement toSql( Connection conn, Date startDate, Date endDate, SqlCondition[] extraConditions )
    {
        if ( endDate == null ) {
            endDate = new Date(System.currentTimeMillis() + 60*1000); // now + 1-minute
            logger.info("endDate not specified, using now plus 1 min: " + dateFormatter.format(endDate));
        }
        if ( startDate == null ) {
            startDate = new Date((new Date()).getTime() - (1000 * 60 * 60 * 24));
            logger.info("startDate not specified, using 1 day ago: " + dateFormatter.format(startDate));
        }

        LinkedList<SqlCondition> allConditions = new LinkedList<SqlCondition>();
        if ( getConditions() != null )
            allConditions.addAll( Arrays.asList(getConditions()) );
        if ( extraConditions != null )
            allConditions.addAll( Arrays.asList(extraConditions) );
        
        switch ( this.type ) {

        case PIE_GRAPH:
            return toSqlPieGraph( conn, startDate, endDate, allConditions );

        case TIME_GRAPH:
            return toSqlTimeGraph( conn, startDate, endDate, allConditions );

        case TIME_GRAPH_DYNAMIC:
            return toSqlTimeGraphDynamic( conn, startDate, endDate, allConditions );
            
        case TEXT:
            return toSqlText( conn, startDate, endDate, allConditions );
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

    private PreparedStatement toSqlPieGraph( Connection conn, Date startDate, Date endDate, LinkedList<SqlCondition> allConditions )
    {
        String dateCondition = " time_stamp >= '" + dateFormatter.format(startDate) + "' " + " and " + " time_stamp <= '" + dateFormatter.format(endDate) + "' ";
        String pieQuery = "SELECT " +
            getPieGroupColumn() + ", " + getPieSumColumn() + " as value " +
            " FROM " +
            " reports." + getTable() +
            " WHERE " + dateCondition;

        pieQuery += conditionsToString( allConditions );

        pieQuery += " GROUP BY " + getPieGroupColumn() + 
            ( getOrderByColumn() == null ? "" : " ORDER BY " + getOrderByColumn() + ( getOrderDesc() ? " DESC " : "" ));
        return sqlToStatement( conn, pieQuery, allConditions );
    }

    private PreparedStatement toSqlText( Connection conn, Date startDate, Date endDate, LinkedList<SqlCondition> allConditions )
    {
        String textQuery = "SELECT ";
        String dateCondition = " time_stamp >= '" + dateFormatter.format(startDate) + "' " + " and " + " time_stamp <= '" + dateFormatter.format(endDate) + "' ";

        boolean first = true;
        for ( String s : getTextColumns() ) {
            if ( !first )
                textQuery += ", ";
            else
                first = false;
            textQuery += s;
        }

        textQuery += " FROM " +
            " reports." + getTable() +
            " WHERE " + dateCondition;
            
        textQuery += conditionsToString( allConditions );

        return sqlToStatement( conn, textQuery, allConditions );
    }

    private PreparedStatement toSqlTimeGraph( Connection conn, Date startDate, Date endDate, LinkedList<SqlCondition> allConditions )
    {
        String dataInterval = calculateTimeDataInterval( startDate, endDate ).toString().toLowerCase();
        String dateCondition = " time_stamp >= '" + dateFormatter.format(startDate) + "' " + " and " + " time_stamp <= '" + dateFormatter.format(endDate) + "' ";
            
        String generate_series;
        if ( dataInterval.equals("tenminute") )
            generate_series = " SELECT generate_series( " +
                " date_trunc( 'hour', '" + dateFormatter.format(startDate) + "'::timestamp ) + INTERVAL '10 min' * ROUND(date_part('minute', '" + dateFormatter.format(startDate) + "'::timestamp)/10.0), " + 
                " '" + dateFormatter.format(endDate)   + "'::timestamp , " +
                " '10 minute' ) as time_trunc ";
        else
            generate_series = " SELECT generate_series( " +
                " date_trunc( '" + dataInterval + "', '" + dateFormatter.format(startDate) + "'::timestamp), " + 
                " '" + dateFormatter.format(endDate)   + "'::timestamp , " +
                " '1 " + dataInterval + "' ) as time_trunc ";
            
        String timeQuery;
        if ( dataInterval.equals("tenminute") )
            timeQuery = "SELECT " +
                " date_trunc( 'hour', time_stamp ) + INTERVAL '10 min' * ROUND(date_part('minute', time_stamp)/10.0) as time_trunc ";
        else
            timeQuery = "SELECT " +
                " date_trunc( '" + dataInterval + "', time_stamp ) as time_trunc ";

        for ( String s : getTimeDataColumns() )
            timeQuery += ", " + s;

        timeQuery += " FROM " +
            " reports." + getTable() +
            " WHERE " + dateCondition;

        timeQuery += conditionsToString( allConditions );
                
        timeQuery += " GROUP BY time_trunc ";

        String final_query = "SELECT * FROM " +
            " ( " + generate_series + " ) as t1 " +
            "LEFT JOIN " +
            " ( " + timeQuery + " ) as t2 " +
            " USING (time_trunc) " +
            " ORDER BY time_trunc " + ( getOrderDesc() ? " DESC " : "" );
        return sqlToStatement( conn, final_query, allConditions );
    }

    private PreparedStatement toSqlTimeGraphDynamic( Connection conn, Date startDate, Date endDate, LinkedList<SqlCondition> allConditions )
    {
        String dataInterval = calculateTimeDataInterval( startDate, endDate ).toString().toLowerCase();
        String dateCondition = " time_stamp >= '" + dateFormatter.format(startDate) + "' " + " and " + " time_stamp <= '" + dateFormatter.format(endDate) + "' ";
            
        if ( allConditions.size() > 0 ) {
            throw new RuntimeException("This graph type does not currently support extra conditions");
        }

        String generate_series;
        if ( dataInterval.equals("tenminute") )
            generate_series = " SELECT generate_series( " +
                " date_trunc( 'hour', '" + dateFormatter.format(startDate) + "'::timestamp ) + INTERVAL '10 min' * ROUND(date_part('minute', '" + dateFormatter.format(startDate) + "'::timestamp)/10.0), " + 
                " '" + dateFormatter.format(endDate)   + "'::timestamp , " +
                " '10 minute' ) as time_trunc ";
        else
            generate_series = " SELECT generate_series( " +
                " date_trunc( '" + dataInterval + "', '" + dateFormatter.format(startDate) + "'::timestamp), " + 
                " '" + dateFormatter.format(endDate)   + "'::timestamp , " +
                " '1 " + dataInterval + "' ) as time_trunc ";
            

        String distinctQuery = "SELECT DISTINCT(" + getTimeDataDynamicColumn() + ") as value, " + getTimeDataDynamicAggregationFunction() + "(" + getTimeDataDynamicValue() + ")" +
            " FROM " + "reports." + getTable() +
            " WHERE " + dateCondition +
            " GROUP BY " + getTimeDataDynamicColumn() + 
            " ORDER BY 2 DESC " + 
            ( getTimeDataDynamicLimit() != null ? " LIMIT " + getTimeDataDynamicLimit() : "" );
        distinctQuery = "SELECT value FROM (" + distinctQuery + ") as data";
        
        String[] distinctValues = getDistinctValues( conn, distinctQuery );
        if ( distinctValues == null ) {
            throw new RuntimeException("Unable to find unique values: " + distinctQuery);
        }
        // if there are no distinct values, nothing to show
        if ( distinctValues.length == 0 ) {
            return sqlToStatement( conn, "select null", null);
        }
        
        String timeQuery;
        if ( dataInterval.equals("tenminute") )
            timeQuery = "SELECT " +
                " date_trunc( 'hour', time_stamp ) + INTERVAL '10 min' * ROUND(date_part('minute', time_stamp)/10.0) as time_trunc ";
        else
            timeQuery = "SELECT " +
                " date_trunc( '" + dataInterval + "', time_stamp ) as time_trunc ";

        timeQuery += ", " + getTimeDataDynamicColumn() + ", " + getTimeDataDynamicAggregationFunction() + "(" + getTimeDataDynamicValue() + ")";

        timeQuery += " FROM " +
            " reports." + getTable() +
            " WHERE " + dateCondition;

        timeQuery += conditionsToString( allConditions );
                
        timeQuery += " GROUP BY time_trunc, " + getTimeDataDynamicColumn() + " ";

        String final_time_query = "SELECT * FROM " +
            " ( " + generate_series + " ) as t1 " +
            "LEFT JOIN " +
            " ( " + timeQuery + " ) as t2 " +
            " USING (time_trunc) " +
            " ORDER BY time_trunc " + ( getOrderDesc() ? " DESC " : "" );

        String final_crosstab_query = "SELECT * FROM crosstab( " +
            "$$" + final_time_query + "$$" + " , " +
            "$$" + distinctQuery + "$$" + ") " + " as " +
            "( \"time_trunc\" timestamp ";
        for ( String s : distinctValues ) {
            final_crosstab_query = final_crosstab_query + ", \"" + s + "\" numeric ";
        }
        final_crosstab_query = final_crosstab_query + ")";

        return sqlToStatement( conn, final_crosstab_query, allConditions );
    }
    
    /**
     * takes a sql string and substitutes the condition arguments into and returns a prepared statement
     */
    private PreparedStatement sqlToStatement( Connection conn, String sql, LinkedList<SqlCondition> conditions )
    {
        try {
            java.sql.PreparedStatement statement = conn.prepareStatement( sql );
            statement.setFetchDirection( java.sql.ResultSet.FETCH_FORWARD );

            SqlCondition.setPreparedStatementValues( statement, conditions, getTable() );

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

        for ( Iterator<SqlCondition> itr = conditions.iterator() ; itr.hasNext() ; ) {
            SqlCondition condition = itr.next();
            String type = ReportsManagerImpl.getInstance().getColumnType( getTable(), condition.getColumn() );

            if ( type == null ) {
                logger.warn("Ignoring unknown column " + condition.getColumn() + " in table " + getTable() );
                itr.remove();
                continue;
            }
            
            str += " and " + condition.toSqlString();
        }

        return str;
    }

    private String[] getDistinctValues( Connection conn, String querySql )
    {
        try {
            java.sql.PreparedStatement statement = conn.prepareStatement( querySql );
            java.sql.ResultSet resultSet = statement.executeQuery();

            LinkedList<String> values = new LinkedList<String>();
            int i = 0;
            while (resultSet.next()) {
                values.add( resultSet.getString(1) );
                i++;
            }

            String[] array = new String[values.size()];
            array = values.toArray(array);
        
            return array;
        } catch (Exception e) {
            logger.warn("Exception:",e);
            return null;
        }
    }

}
