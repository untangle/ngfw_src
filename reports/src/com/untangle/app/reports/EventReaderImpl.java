/**
 * $Id$
 */
package com.untangle.app.reports;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.json.JSONObject;

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

    private static final DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    
    private ReportsApp app;

    private HashMap<String,Class<?>> columnTypeMap = new HashMap<>();

    private Semaphore querySemaphore = new Semaphore(MAX_SIMULTANEOUS_QUERIES);
    
    /**
     * Initialize event reader.
     *
     * @param app
     *  Reports application.
     */
    public EventReaderImpl( ReportsApp app )
    {
        this.app = app;
        this.columnTypeMap.put("inet",String.class);
    }

    /**
     * Return the event result set.
     *
     * @param dbConnection
     *  Database connection.
     * @param statement
     *  SQL statement to query.
     * @param limit
     *  Maximum number of results to include.
     * @return
     *  ResultSetReader
     */
    public ResultSetReader getEventsResultSet( final Connection dbConnection, final PreparedStatement statement, final int limit )
    {
        EventReaderImpl.checkConnection( dbConnection );
        
        try {
            logger.debug("getEventsResultSet( statement: " + statement + " )");

            
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
            
                ResultSet resultSet;
                boolean execResult = statement.execute();

                resultSet = statement.getResultSet();
                
                // if the statement is a list of statements, then we may have to check additional results
                // for the actual results, because the first statements may return no results
                while ( resultSet == null ) {
                    // if no more results
                    boolean nextResult = statement.getMoreResults();
                    int updateCount = statement.getUpdateCount();
                    // according to javadocs this is the test to see that there are no more results
                    if ( !nextResult && updateCount < 0 )
                        break;
                    resultSet = statement.getResultSet();
                }

                return new ResultSetReader( resultSet, dbConnection, statement );
            } catch (InterruptedException e) {
                logger.warn("Interrupted",e);
                throw e;
            } finally {
                this.querySemaphore.release();
            }

        } catch ( Exception e ) {
            logger.warn("Failed to query database. query: " + statement, e );
            throw new RuntimeException( "Failed to query database.", e );
        } 
    }
    
    /**
     * Return the event result set.
     *
     * @param dbConnection
     *  Database connection.
     * @param statement
     *  SQL statement to query.
     * @param table
     *  Table to query
     * @param conditions
     *  Additional SQL conditions.
     * @param limit
     *  Maximum number of results to include.
     * @return
     *  ResultSetReader
     */
    public ResultSetReader getEventsResultSet( final Connection dbConnection, final PreparedStatement statement, final String table, final SqlCondition[] conditions, final int limit )
    {
        if ( dbConnection == null) {
            logger.warn("Unable to connect to DB.");
            throw new RuntimeException("Unable to connect to DB.");
        }

        try {
            dbConnection.setAutoCommit(false);
        } catch (Exception e) {
            logger.warn("Unable to set auto-commit.",e);
        }
        
        SqlCondition.setPreparedStatementValues( statement, conditions, table );

        logger.debug("getEventsResultSet( " + statement + " )");
        
        try {
            return getEventsResultSet( dbConnection, statement, limit );
            
        } catch ( Exception e ) {
            try {dbConnection.close();} catch( Exception exc) {}
            logger.warn("Failed to query database. query: " + statement, e );
            throw new RuntimeException( "Failed to query database.", e );
        } 
    }

    /**
     * Return the event result set.
     *
     * @param sql
     *  SQL statement to query.
     * @param table
     *  Table to query
     * @param conditions
     *  Additional SQL conditions.
     * @param limit
     *  Maximum number of results to include.
     * @return
     *  ResultSetReader
     */
    public ResultSetReader getEventsResultSet( final String sql, final String table, final SqlCondition[] conditions, final int limit )
    {
        Connection dbConnection = null;

        try {
            dbConnection = this.app.getDbConnection();
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
            
            return getEventsResultSet( dbConnection, statement, table, conditions, limit );
        } catch ( Exception e ) {
            try {dbConnection.close();} catch( Exception exc) {}
            logger.warn("Failed to query database. query: " + sql, e );
            throw new RuntimeException( "Failed to query database.", e );
        } 
    }
    
    /**
     * Return the event result set.
     *
     * @param query
     *  SQL statement to query.
     * @param table
     *  Table to query
     * @param conditions
     *  Additional SQL conditions.
     * @param limit
     *  Maximum number of results to include.
     * @param startDate
     *  Start date for query.
     * @param endDate
     *  Send date for query.
     * @return
     *  ResultSetReader
     */
    public ResultSetReader getEventsResultSet( final String query, final String table, final SqlCondition[] conditions, final int limit, final Date startDate, final Date endDate )
    {
        String queryStr = query;

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

            queryStr += queryPart2;
        }
        if (limit > 0) {
            String tmpStr = queryStr.toLowerCase();
            int limitIndex   = tmpStr.indexOf("limit");
            if ( limitIndex < 0 )
                queryStr += " LIMIT " + limit + " ";
        }

        logger.debug("getEventsResultSet( query: " + query + " limit: " + limit + " )");

        return getEventsResultSet( queryStr, table, conditions, limit );
    }

    /**
     * Get events as an array of JSON objects.
     *
     * @param query
     *  SQL statement to query.
     * @param table
     *  Table to query
     * @param conditions
     *  Additional SQL conditions.
     * @param limit
     *  Maximum number of results to include.
     * @param startDate
     *  Start date for query.
     * @param endDate
     *  Send date for query.
     * @return
     *  ArraList of JSON objects containing results.
     */
    public ArrayList<JSONObject> getEvents( final String query, final String table, final SqlCondition[] conditions, final int limit, final Date startDate, final Date endDate )
    {
        ResultSetReader resultSetReader = getEventsResultSet( query, table, conditions, limit, startDate, endDate);
        return resultSetReader.getAllEvents();
    }

    /**
     * Get events as an array of JSON objects.
     *
     * @param dbConnection
     *  Database connection.
     * @param statement
     *  SQL statement to query
     * @param table
     *  Table to query
     * @param limit
     *  Maximum number of results to include.
     * @return
     *  ArraList of JSON objects containing results.
     */
    public ArrayList<JSONObject> getEvents( final Connection dbConnection, final PreparedStatement statement, final String table, final int limit )
    {
        ResultSetReader resultSetReader = getEventsResultSet( dbConnection, statement, table, null, limit );
        return resultSetReader.getAllEvents();
    }

    /**
     * Check state of database connection.
     *
     * @param dbConnection
     *  Database connection to verify.
     */
    public static void checkConnection( final Connection dbConnection )
    {
        if ( dbConnection == null ) {
            throw new RuntimeException("Database connection failed.");
        }

        java.sql.PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            statement = dbConnection.prepareStatement( "SELECT 1" );
            statement.execute();
            resultSet = statement.getResultSet();
        } catch (Exception e) {
            throw new RuntimeException("Database connection failed.");
        } finally {
            if ( resultSet != null ) try {resultSet.close();} catch (Exception e) {}
            if ( statement != null ) try {statement.close();} catch (Exception e) {}
        }
    }
    
}
