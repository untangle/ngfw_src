/**
 * $Id: ReportEntry.java,v 1.00 2015/02/24 15:19:32 dmorris Exp $
 */
package com.untangle.app.reports;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.logging.LogEvent;

/**
 * The settings for an individual report entry (graph)
 */
@SuppressWarnings("serial")
public class ReportEntry implements Serializable, JSONString
{
    private static final Logger logger = Logger.getLogger(ReportEntry.class);

    public static enum ReportEntryType {
        TEXT, /* A text entry */
        PIE_GRAPH, /* A top X pie chart graph */
        TIME_GRAPH, /* A graph with time (minutes) on the x-axis */
        TIME_GRAPH_DYNAMIC, /* A graph with time (minutes) on the x-axis with dynamic columns */
        EVENT_LIST /* A list of events (event log) */
    };

    public static enum TimeDataInterval {
        SECOND,
        TENMINUTE,
        MINUTE,
        HOUR,
        DAY,
        WEEK,
        MONTH,
        AUTO
    };

    public static enum TimeStyle {
        BAR,
        BAR_OVERLAPPED,
        BAR_STACKED,
        BAR_3D, /* DEPRECATED */
        BAR_3D_OVERLAPPED, /* DEPRECATED */
        BAR_3D_STACKED, /* DEPRECATED */
        LINE,
        AREA,
        AREA_STACKED
    };

    public static enum PieStyle {
        PIE,
        PIE_3D,
        DONUT,
        DONUT_3D,
        COLUMN,
        COLUMN_3D
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
    private PieStyle pieStyle;

    private String textString; /* The string representation of the text */
    private String[] textColumns; /* The data to graph by time */

    private TimeDataInterval timeDataInterval; /* The time interval to be used in time-based graphs */
    private TimeStyle timeStyle; /* The time chart type (line/bar/etc) */
    private String[] timeDataColumns; /* The column/data to graph by time */

    private String timeDataDynamicValue; /* The value to graph (the column in "table") */
    private String timeDataDynamicColumn;  /* The columns to create the dynamic column based on (the distinct values) */
    private Integer timeDataDynamicLimit;  /* The columns to create the dynamic column based on (the distinct values) */
    private String timeDataDynamicAggregationFunction; /* The way to aggregate multiple values per date_trun (max, avg, etc) */
    private Boolean timeDataDynamicAllowNull;  /* Allow Null/None as one of the distinct valuestimeD */

    private String orderByColumn = null; /* The column to order by */
    private Boolean orderDesc = null; /* The direction to order, True is DESC, False is regular, null is neither */

    private String[] defaultColumns; /* The default columns for an event list report entry */

    /* http://api.highcharts.com/highstock/plotOptions.area.dataGrouping */
    private String approximation; /* The data-approximation technique: average, open, high, low, close, sum */

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

    public PieStyle getPieStyle() { return this.pieStyle; }
    public void setPieStyle( PieStyle newValue ) { this.pieStyle = newValue; }

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

    public TimeStyle getTimeStyle()
    {
        if (this.timeStyle == null) return null;
        // remove obsolete styles
        switch (this.timeStyle) {
        case BAR_3D: return TimeStyle.BAR;
        case BAR_3D_OVERLAPPED: return TimeStyle.BAR_OVERLAPPED;
        case BAR_3D_STACKED: return TimeStyle.BAR_STACKED;
        default: break;
        }
        return this.timeStyle;
    }
    public void setTimeStyle( TimeStyle newValue ) { this.timeStyle = newValue; }

    public String[] getTimeDataColumns() { return this.timeDataColumns; }
    public void setTimeDataColumns( String[] newValue ) { this.timeDataColumns = newValue; }

    public String getTimeDataDynamicValue() { return this.timeDataDynamicValue; }
    public void setTimeDataDynamicValue( String newValue ) { this.timeDataDynamicValue = newValue; }

    public String getTimeDataDynamicColumn() { return this.timeDataDynamicColumn; }
    public void setTimeDataDynamicColumn( String newValue ) { this.timeDataDynamicColumn = newValue; }

    public Integer getTimeDataDynamicLimit() { return this.timeDataDynamicLimit; }
    public void setTimeDataDynamicLimit( Integer newValue ) { this.timeDataDynamicLimit = newValue; }

    public Boolean getTimeDataDynamicAllowNull() { return this.timeDataDynamicAllowNull; }
    public void setTimeDataDynamicAllowNull( Boolean newValue ) { this.timeDataDynamicAllowNull = newValue; }

    public String getTimeDataDynamicAggregationFunction() { return this.timeDataDynamicAggregationFunction; }
    public void setTimeDataDynamicAggregationFunction( String newValue ) { this.timeDataDynamicAggregationFunction = newValue; }

    public String[] getColors() { return this.colors; }
    public void setColors( String[] newValue ) { this.colors = newValue; }

    public String[] getDefaultColumns() { return this.defaultColumns; }
    public void setDefaultColumns( String[] newValue ) { this.defaultColumns = newValue; }

    public String getApproximation() { return this.approximation; }
    public void setApproximation( String newValue ) { this.approximation = newValue; }

    public PreparedStatement toSql( Connection conn, Date startDate, Date endDate )
    {
        return toSql( conn, startDate, endDate, null, null );
    }

    public PreparedStatement toSql( Connection conn, Date startDate, Date endDate, SqlCondition[] extraConditions )
    {
        return toSql( conn, startDate, endDate, extraConditions, null );
    }

    public PreparedStatement toSql( Connection conn, Date startDate, Date endDate, SqlCondition[] extraConditions, Integer limit )
    {
        if ( endDate == null ) {
            endDate = new Date(System.currentTimeMillis() + 60*1000); // now + 1-minute
            logger.info("endDate not specified, using now plus 1 min: " + dateFormat(endDate));
        }
        if ( startDate == null ) {
            startDate = new Date((new Date()).getTime() - (1000 * 60 * 60 * 24));
            logger.info("startDate not specified, using 1 day ago: " + dateFormat(startDate));
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

        case EVENT_LIST:
            return toSqlEventList( conn, startDate, endDate, allConditions, limit );
        }

        throw new RuntimeException("Unknown Graph type: " + this.type);
    }

    private TimeDataInterval calculateTimeDataInterval( Date startDate, Date endDate )
    {
        if ( this.timeDataInterval != TimeDataInterval.AUTO )
            return this.timeDataInterval;

        // with high charts the data combination is handled in the frontend
        // as such we can return as granular of data as possible and the UI
        // will handle combining the data into a manageable chunks
        return TimeDataInterval.MINUTE;

        // /* otherwise its auto, calculate a good interval based on the data */
        // long timeDiffSec = ((endDate.getTime() - startDate.getTime())/1000);

        // if ( timeDiffSec > ( 60 * 60 * 24 * 2 ) ) /* more than 2 days, use days */
        //     return TimeDataInterval.DAY;
        // else if ( timeDiffSec >= ( 60 * 60 * 10 ) ) /* >= 10 hours */
        //     return TimeDataInterval.HOUR;
        // else
        //     return TimeDataInterval.MINUTE;
    }

    private PreparedStatement toSqlEventList( Connection conn, Date startDate, Date endDate, LinkedList<SqlCondition> allConditions, Integer limit )
    {
        String query = "";
        String dateCondition = " time_stamp >= " + dateFormat(startDate) + " " + " and " + " time_stamp <= " + dateFormat(endDate) + " ";
        query +=  "SELECT * FROM " + LogEvent.schemaPrefix() + this.table + " WHERE " + dateCondition;
        query += conditionsToString( allConditions );
        query += " ORDER BY time_stamp DESC";
        if ( limit != null && limit > 0 )
        query += " LIMIT " + limit;

        return sqlToStatement( conn, query, allConditions );
    }

    private PreparedStatement toSqlPieGraph( Connection conn, Date startDate, Date endDate, LinkedList<SqlCondition> allConditions )
    {
        String dateCondition = " time_stamp >= " + dateFormat(startDate) + " " + " and " + " time_stamp <= " + dateFormat(endDate) + " ";
        String pieQuery = "SELECT " +
            getPieGroupColumn() + ", " + getPieSumColumn() + " as value " +
            " FROM " +
            LogEvent.schemaPrefix() + getTable() +
            " WHERE " + dateCondition;

        pieQuery += conditionsToString( allConditions );

        pieQuery += " GROUP BY " + getPieGroupColumn() +
            ( getOrderByColumn() == null ? "" : " ORDER BY " + getOrderByColumn() + ( getOrderDesc() ? " DESC " : "" ));
        return sqlToStatement( conn, pieQuery, allConditions );
    }

    private PreparedStatement toSqlText( Connection conn, Date startDate, Date endDate, LinkedList<SqlCondition> allConditions )
    {
        String textQuery = "SELECT ";
        String dateCondition = " time_stamp >= " + dateFormat(startDate) + " " + " and " + " time_stamp <= " + dateFormat(endDate) + " ";

        boolean first = true;
        for ( String s : getTextColumns() ) {
            if ( !first )
                textQuery += ", ";
            else
                first = false;
            textQuery += s;
        }

        textQuery += " FROM " +
            LogEvent.schemaPrefix() + getTable() +
            " WHERE " + dateCondition;

        textQuery += conditionsToString( allConditions );

        return sqlToStatement( conn, textQuery, allConditions );
    }

    private PreparedStatement toSqlTimeGraph( Connection conn, Date startDate, Date endDate, LinkedList<SqlCondition> allConditions )
    {
        String dataInterval = calculateTimeDataInterval( startDate, endDate ).toString().toLowerCase();
        String dateCondition = " time_stamp >= " + dateFormat(startDate) + " " + " and " + " time_stamp <= " + dateFormat(endDate) + " ";
        String generateSeriesQuery = generateSeriesQuery( startDate, endDate, dataInterval );

        String timeQuery = "SELECT " +
            " date_trunc( '" + dataInterval + "', time_stamp ) as time_trunc ";

        for ( String s : getTimeDataColumns() )
            timeQuery += ", " + s;

        timeQuery += " FROM " +
            LogEvent.schemaPrefix() + getTable() +
            " WHERE " + dateCondition;

        timeQuery += conditionsToString( allConditions );

        timeQuery += " GROUP BY time_trunc ";

        String finalQuery = "SELECT * FROM " +
            " ( " + generateSeriesQuery + " ) as t1 " +
            "LEFT JOIN " +
            " ( " + timeQuery + " ) as t2 " +
            " USING (time_trunc) " +
            " ORDER BY time_trunc " + ( getOrderDesc() ? " DESC " : "" );

        return sqlToStatement( conn, finalQuery, allConditions );
    }

    private PreparedStatement toSqlTimeGraphDynamic( Connection conn, Date startDate, Date endDate, LinkedList<SqlCondition> allConditions )
    {
        String dataInterval = calculateTimeDataInterval( startDate, endDate ).toString().toLowerCase();
        String dateCondition = " time_stamp >= " + dateFormat(startDate) + " " + " and " + " time_stamp <= " + dateFormat(endDate) + " ";

        if ( endDate.getTime() > System.currentTimeMillis() ) {
            /**
             * when endDate = null, we assume now+1minute.
             * if the endDate is effectively "now" or later, then chop off the last minute from the generateSeriesQuery
             * we do this because otherwise it adds an extra minute onto the range. usually you want that, but in this case it ends up adding a datapoint to the series
             * which when joined with the actual data just results in a null point at the end which looks poor in the graph
             */
            endDate = new Date( endDate.getTime() - (60*1000) );
        }
        String generateSeriesQuery = generateSeriesQuery( startDate, endDate, dataInterval );

        /**
         * distinctQuery
         * This querys the distinct values that will be used to detemine the columns in the final result
         */
        String distinctQuery = "SELECT DISTINCT(" + getTimeDataDynamicColumn() + ") as value, " + getTimeDataDynamicAggregationFunction() + "(" + getTimeDataDynamicValue() + ")" +
            " FROM " + LogEvent.schemaPrefix() + getTable() +
            " WHERE " + dateCondition +
            conditionsToString( allConditions ) +
            ( getTimeDataDynamicAllowNull() == null || getTimeDataDynamicAllowNull() == Boolean.FALSE ? (" AND " + getTimeDataDynamicColumn() + " IS NOT NULL") : "" ) +
            " GROUP BY " + getTimeDataDynamicColumn() +
            " ORDER BY 2 DESC " +
            ( getTimeDataDynamicLimit() != null ? " LIMIT " + getTimeDataDynamicLimit() : "" );

        /**
         * Fetch the distinct values (with conditions and all)
         */
        String[] distinctValues = getDistinctValues( conn, getTable(), distinctQuery, allConditions );
        if ( distinctValues == null ) {
            throw new RuntimeException("Unable to find unique values: " + distinctQuery);
        }
        /**
         * If there are no distinct values, nothing to show
         * Just return
         */
        // we actually just want to return a normal query with the normal timerange in this case
        // it just won't have any data series because there are no "top" data series to show
        // NGFW-11438
        // if ( distinctValues.length == 0 ) {
        //     return sqlToStatement( conn, "select null", null);
        //     return sqlToStatement( conn, "select 1 where 1 = 2", null);
        // }

        String timeQuery;
        timeQuery = "SELECT " +
            " date_trunc( '" + dataInterval + "', time_stamp ) as time_trunc ";

        for ( String distinctValue : distinctValues ) {
            if ( distinctValue == null ) {
                timeQuery += ", " +
                    getTimeDataDynamicAggregationFunction() + "(CASE WHEN " + getTimeDataDynamicColumn() + " IS NULL THEN " + getTimeDataDynamicValue() + " END) " +
                    "AS \"None\"";
            } else if ( distinctValue.trim().equals("") ) {
                timeQuery += ", " +
                    getTimeDataDynamicAggregationFunction() + "(CASE WHEN " + getTimeDataDynamicColumn() + " = '" + distinctValue.replaceAll("'","") + "' THEN " + getTimeDataDynamicValue() + " END) " +
                    "AS \" \"";
            } else {
                timeQuery += ", " +
                    getTimeDataDynamicAggregationFunction() + "(CASE WHEN " + getTimeDataDynamicColumn() + " = '" + distinctValue.replaceAll("'","") + "' THEN " + getTimeDataDynamicValue() + " END) " +
                    "AS \"" + distinctValue.replaceAll("\"","") + "\"";
            }
        }

        timeQuery += " FROM " +
            LogEvent.schemaPrefix() + getTable() +
            " WHERE " + dateCondition;

        timeQuery += conditionsToString( allConditions );

        timeQuery += " GROUP BY time_trunc ";

        String finalQuery = "SELECT * FROM " +
            " ( " + generateSeriesQuery + " ) as t1 " +
            "LEFT JOIN " +
            " ( " + timeQuery + " ) as t2 " +
            " USING (time_trunc) " +
            " ORDER BY time_trunc " + ( getOrderDesc() ? " DESC " : "" );
        return sqlToStatement( conn, finalQuery, allConditions );
    }

    /**
     * takes a sql string and substitutes the condition arguments into and returns a prepared statement
     *
     * @param conn
     *  Database connection.
     * @param sql
     *  String of SQL to add.
     * @param conditions
     *  List of SqlConditions ot process.
     * @return
     *  PreparedStatement with updated SQL.
     */
    private PreparedStatement sqlToStatement( Connection conn, String sql, LinkedList<SqlCondition> conditions )
    {
        EventReaderImpl.checkConnection( conn );

        /**
         * If we are using the SQLite driver
         * Transfrom the SQL syntax to a SQLite compatible version
         */
        if ( ReportsApp.dbDriver.equals("sqlite") ) {
            /**
             * Postgres uses x::y to cast. SQLite uses CAST(x as y)
             * x::y -> CAST(x as y)
             * (a b)::y -> CAST((a b) as y)
             */
            sql = sql.replaceAll("([a-z_]+)::([a-z]+)","CAST($1 AS $2)");
            sql = sql.replaceAll("(\\([a-z_ ]+\\))::([a-z]+)","CAST($1 AS $2)");

            /**
             * Postgres uses date_trunc. SQLite just stores dates as long and doesn't support date_trunc - use division
             * date_trunc( 'minute', x ) -> x/60000*60000
             * date_trunc( 'hour', x ) -> x/3600000*3600000
             */
            sql = sql.replaceAll("date_trunc\\(\\s*'second'\\s*,\\s*([a-z_]+)\\s*\\)","$1/1000*1000");
            sql = sql.replaceAll("date_trunc\\(\\s*'minute'\\s*,\\s*([a-z_]+)\\s*\\)","$1/60000*60000");
            sql = sql.replaceAll("date_trunc\\(\\s*'hour'\\s*,\\s*([a-z_]+)\\s*\\)","$1/3600000*3600000");
            sql = sql.replaceAll("date_trunc\\(\\s*'day'\\s*,\\s*([a-z_]+)\\s*\\)","$1/86400000*86400000");
            sql = sql.replaceAll("date_trunc\\(\\s*'week'\\s*,\\s*([a-z_]+)\\s*\\)","$1/604800000*604800000");
            sql = sql.replaceAll("date_trunc\\(\\s*'month'\\s*,\\s*([a-z_]+)\\s*\\)","$1/18144000000*18144000000");

            /**
             * Postgres uses "is true" and "is false." SQLite uses "= 1" and "= 0"
             * is true -> = 1
             * IS FALSE -> 0
             */
            sql = sql.replaceAll("\\s+[iI][sS]\\s+[tT][rR][uU][eE]\\s+"," = 1 ");
            sql = sql.replaceAll("\\s+[iI][sS]\\s+[fF][aA][lL][sS][eE]\\s+"," = 0 ");
        }

        try {
            logger.debug("SQL: " + sql);
            java.sql.PreparedStatement statement = conn.prepareStatement( sql );
            SqlCondition.setPreparedStatementValues( statement, conditions, getTable() );

            return statement;
        } catch ( Exception e) {
            logger.warn("SQL Exception. Query: " + sql, e);
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

    private String[] getDistinctValues( Connection conn, String table, String querySql, List<SqlCondition> conditions )
    {
        EventReaderImpl.checkConnection( conn );

        String[] results = null;

        java.sql.ResultSet resultSet = null;
        try {
            java.sql.PreparedStatement statement = conn.prepareStatement( querySql );
            SqlCondition.setPreparedStatementValues( statement, conditions, table );

            logger.info("Getting distinct values: " + statement);
            resultSet = statement.executeQuery();

            LinkedList<String> values = new LinkedList<String>();
            while (resultSet.next()) {
                values.add( resultSet.getString(1) );
            }

            results = new String[values.size()];
            results = values.toArray(results);
        } catch (Exception e) {
            logger.warn("Exception:",e);
        } finally {
            if (resultSet != null){
                try{
                    resultSet.close();
                }catch( Exception e ){
                    logger.warn("Exception:",e);
                }
            }
        }

        return results;
    }

    private static final DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private String dateFormat( Date date )
    {
        if ( ReportsApp.dbDriver.equals("sqlite") )
            return Long.toString(date.getTime());
        else
            return "'" + dateFormatter.format(date) + "'";
    }

    private String generateSeriesQuery( Date startDate, Date endDate, String dataInterval )
    {
        if ( ReportsApp.dbDriver.equals("sqlite") ) {
            int divisor = 60000;
            switch(dataInterval) {
            case "minute": divisor = 60000; break;
            case "hour": divisor = 60*60000; break;
            case "day": divisor = 24*60*60000; break;
            default: throw new RuntimeException("unsupported interval: " + dataInterval);
            }

            // FIXME only covers maximum 90000 datapoints currently (86400 is enough for a day)
            String generateSeriesQuery = "SELECT DISTINCT(((" + dateFormat(startDate) + "/" +divisor+")+e+d*10+c*100+b*1000+a*10000)*" + divisor + ") AS time_trunc FROM" +
                "(select 0 as a union select 1 union select 2 union select 3 union select 4 union select 5 union select 6 union select 7 union select 8), " +
                "(select 0 as b union select 1 union select 2 union select 3 union select 4 union select 5 union select 6 union select 7 union select 8 union select 9), " +
                "(select 0 as c union select 1 union select 2 union select 3 union select 4 union select 5 union select 6 union select 7 union select 8 union select 9), " +
                "(select 0 as d union select 1 union select 2 union select 3 union select 4 union select 5 union select 6 union select 7 union select 8 union select 9), " +
                "(select 0 as e union select 1 union select 2 union select 3 union select 4 union select 5 union select 6 union select 7 union select 8 union select 9) " +
                "WHERE time_trunc < " + dateFormat(endDate);
            return generateSeriesQuery;
        } else {
            String generateSeriesQuery = " SELECT generate_series( " +
                " date_trunc( '" + dataInterval + "', " + dateFormat(startDate) + "::timestamp), " +
                " " + dateFormat(endDate)   + "::timestamp , " +
                " '1 " + dataInterval + "' ) as time_trunc ";
            return generateSeriesQuery;
        }

    }
}
