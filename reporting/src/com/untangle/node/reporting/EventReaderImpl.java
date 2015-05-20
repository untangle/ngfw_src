/**
 * $Id$
 */
package com.untangle.node.reporting;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.PreparedStatement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.untangle.uvm.node.SqlCondition;

/**
 * Utility the reads events from the database
 *
 * WARNING
 * You must call closeConnection on the ResultSetReader ALWAYS after calling these functions
 * closeConnection will call commit() on the SQL transaction
 * If you forget to call it, it will maintain an open transaction on that table
 * which will stop other queries (and vacuuming) from taking place
 *
 */
public class EventReaderImpl
{
    private int MAX_SIMULTANEOUS_QUERIES = 2;

    private final Logger logger = Logger.getLogger(getClass());

    private static final DateFormat dateFormatter = new SimpleDateFormat("YYYY-MM-dd HH:mm");
    
    private ReportingNodeImpl node;

    private HashMap<String,Class<?>> columnTypeMap = new HashMap<String,Class<?>>();

    private Semaphore querySemaphore = new Semaphore(MAX_SIMULTANEOUS_QUERIES);
    
    public EventReaderImpl( ReportingNodeImpl node )
    {
        this.node = node;
        this.columnTypeMap.put("inet",String.class);
    }

    public ResultSetReader getEventsResultSet( final Connection dbConnection, final PreparedStatement statement, final int limit )
    {
        if ( dbConnection == null) {
            logger.warn("Invalid parameter.");
            throw new RuntimeException("Invalid parameter.");
        }
        
        try {
            logger.debug("getEventsResultSet( statement: " + statement + " )");

            statement.setFetchDirection( ResultSet.FETCH_FORWARD );
            
            /* If this is an unlimited query - set a fetch size so we don't load all into memory */
            if (limit < 0 || limit > 100000)
                statement.setFetchSize(2000);
            
            if (statement == null) {
                logger.warn("Unable to create Statement");
                throw new RuntimeException("Unable to create Statement");
            }


            try {
                if ( ! this.querySemaphore.tryAcquire(60L, java.util.concurrent.TimeUnit.SECONDS) ) {
                    logger.error("Unable to acquire query lock", new Exception());
                    throw new RuntimeException("Unable to acquire query lock");
                }
            
                ResultSet resultSet = statement.executeQuery();
                return new ResultSetReader( resultSet, dbConnection );
            } catch (InterruptedException e) {
                logger.warn("Interrupted",e);
                throw e;
            } finally {
                this.querySemaphore.release();
            }

        } catch ( Exception e ) {
            logger.warn("Failed to query database. query: " + statement, e );
            throw new RuntimeException( "Failed to query database. query: " + statement, e );
        } 
    }
    

    public ResultSetReader getEventsResultSet( final PreparedStatement statement, final int limit )
    {
        Connection dbConnection = null;

        try {
            dbConnection = this.node.getDbConnection();
            dbConnection.setAutoCommit(false);
        } catch (Exception e) {
            logger.warn("Unable to create connection to DB",e);
        }

        if ( dbConnection == null) {
            logger.warn("Unable to connect to DB.");
            throw new RuntimeException("Unable to connect to DB.");
        }
        
        try {
            return getEventsResultSet( dbConnection, statement, limit );
            
        } catch ( Exception e ) {
            try {dbConnection.close();} catch( Exception exc) {}
            logger.warn("Failed to query database. query: " + statement, e );
            throw new RuntimeException( "Failed to query database. query: " + statement, e );
        } 
    }
    
    public ResultSetReader getEventsResultSet( final PreparedStatement statement, final SqlCondition[] extraConditions, final int limit )
    {
        return getEventsResultSet( statement, null, limit );
    }

    public ResultSetReader getEventsResultSet( final String sql, final int limit )
    {
        Connection dbConnection = null;

        try {
            dbConnection = this.node.getDbConnection();
        } catch (Exception e) {
            logger.warn("Unable to create connection to DB",e);
        }

        if ( dbConnection == null) {
            logger.warn("Unable to connect to DB.");
            throw new RuntimeException("Unable to connect to DB.");
        }
        
        try {
            logger.debug("getEventsResultSet( sql: " + sql + " )");
            
            java.sql.PreparedStatement statement = dbConnection.prepareStatement( sql );
            statement.setFetchDirection( ResultSet.FETCH_FORWARD );
            
            return getEventsResultSet( statement, limit );
        } catch ( Exception e ) {
            try {dbConnection.close();} catch( Exception exc) {}
            logger.warn("Failed to query database. query: " + sql, e );
            throw new RuntimeException( "Failed to query database. query: " + sql, e );
        } 
    }
    
    public ResultSetReader getEventsResultSet( final String query, final Long policyId, final SqlCondition[] extraConditions, final int limit, final Date startDate, final Date endDate )
    {
        String queryStr = query;
        if ( policyId == null || policyId == -1 ) {
            queryStr = queryStr.replace("= :policyId","is not null");
            queryStr = queryStr.replace("=:policyId","is not null");
        } else {
            queryStr = queryStr.replace(":policyId", Long.toString( policyId ) );
        }
        if (startDate != null || endDate != null) {

            String tmpStr = queryStr.toLowerCase();
            int whereIndex   = tmpStr.indexOf("where");
            int groupByIndex = tmpStr.indexOf("group by");
            int orderByIndex = tmpStr.indexOf("order by");
            int insertIndex;
            if ( groupByIndex > 0 )
                insertIndex = groupByIndex; // insert the where clause before "group by"
            else if ( orderByIndex > 0 )
                insertIndex = orderByIndex; // insert the where clause before "order by"
            else
                insertIndex = queryStr.length() - 1; // insert the where clause at the end

            String queryPart1 = queryStr.substring(0, insertIndex);
            String queryPart2 = queryStr.substring(insertIndex);
            queryStr = queryPart1;
            if ( whereIndex < 0 )
                queryStr += " where true ";
            if ( endDate != null )
                queryStr += " and time_stamp <= '" + dateFormatter.format(endDate)   + "' ";
            if ( startDate != null )
                queryStr += " and time_stamp >= '" + dateFormatter.format(startDate) + "' ";
            if (extraConditions != null) {
                for ( SqlCondition condition : extraConditions ) {
                    queryStr += " and " + condition.getColumn() + " " + condition.getOperator() + " ? ";
                }
            }
            queryStr += queryPart2;
        }
        if (limit > 0)
            queryStr += " LIMIT " + limit + " ";

        logger.debug("getEventsResultSet( query: " + query + " policyId: " + policyId + " limit: " + limit + " )");

        return getEventsResultSet( queryStr, limit );
    }

    public ArrayList<JSONObject> getEvents(final String query, final Long policyId, final SqlCondition[] extraConditions, final int limit, final Date startDate, final Date endDate)
    {
        ResultSetReader resultSetReader = getEventsResultSet( query, policyId, extraConditions, limit, startDate, endDate);
        return resultSetReader.getAllEvents();
    }

    public ArrayList<JSONObject> getEvents( final String sql, final int limit )
    {
        ResultSetReader resultSetReader = getEventsResultSet( sql, -1 );
        return resultSetReader.getAllEvents();
    }

    public ArrayList<JSONObject> getEvents( final PreparedStatement statement, final int limit )
    {
        ResultSetReader resultSetReader = getEventsResultSet( statement, -1 );
        return resultSetReader.getAllEvents();
    }
    
}
