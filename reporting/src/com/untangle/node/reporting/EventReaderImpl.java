/**
 * $Id: EventWriter.java,v 1.00 2011/12/18 19:09:03 dmorris Exp $
 */
package com.untangle.node.reporting;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.json.JSONObject;

/**
 * Utility the reads events from the database
 */
public class EventReaderImpl
{
    private final Logger logger = Logger.getLogger(getClass());

    private ReportingNodeImpl node;

    private HashMap<String,Class<?>> columnTypeMap = new HashMap<String,Class<?>>();
    
    public EventReaderImpl( ReportingNodeImpl node )
    {
        this.node = node;
        this.columnTypeMap.put("inet",String.class);
    }

    /**
     * WARNING
     * You must call closeConnection on the ResultSetReader ALWAYS after calling this function
     * closeConnection will call commit() on the SQL transaction
     * If you forget to call it, it will maintain an open transaction on that table
     * which will stop other queries (and vacuuming) from taking place
     * @param endDate 
     * @param startDate 
     */
    public ResultSetReader getEventsResultSet( final String query, final Long policyId, final int limit, Date startDate, Date endDate )
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
            String queryStr = query;
            if ( policyId == null || policyId == -1 ) {
                queryStr = queryStr.replace("= :policyId","is not null");
                queryStr = queryStr.replace("=:policyId","is not null");
            } else {
                queryStr = queryStr.replace(":policyId", Long.toString( policyId ) );
            }
            if (startDate != null && endDate != null){
                queryStr = queryStr.toLowerCase();
                int i = queryStr.indexOf("order by");
                DateFormat df = new SimpleDateFormat("YYYY-MM-dd HH:mm");
                if (i > 0) {
                    queryStr = queryStr.substring(0, i) + " and time_stamp <= '" + df.format(endDate) + "' and time_stamp >= '" + 
                            df.format(startDate) + "' " + queryStr.substring(i);
                } else {
                    queryStr += " and time_stamp <= " + endDate + " and time_stamp >= " + startDate;
                }
            }
            if (limit > 0)
                queryStr += " LIMIT " + limit + " ";

            logger.debug("getEventsResultSet( query: " + query + " policyId: " + policyId + " limit: " + limit + " )");
            logger.info("getEventsResultSet( queryStr: \"" + queryStr + "\")");
            
            Statement statement = dbConnection.createStatement( ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.FETCH_FORWARD );

            /* If this is an unlimited query - set a fetch size so we don't load all into memory */
            if (limit < 0 || limit > 100000)
                statement.setFetchSize(2000);
            
            if (statement == null) {
                logger.warn("Unable to create Statement");
                throw new RuntimeException("Unable to create Statement");
            }
            
            ResultSet resultSet = statement.executeQuery( queryStr );
            return new ResultSetReader( resultSet, dbConnection );
            
        } catch ( Exception e ) {
            try {dbConnection.close();} catch( Exception exc) {}
            logger.warn("Failed to query database", e );
            throw new RuntimeException( "Failed to query database.", e );
        } 
    }

    public ArrayList<JSONObject> getEvents(final String query, final Long policyId, final int limit, Date startDate, Date endDate)
    {
        ResultSetReader resultSetReader = getEventsResultSet( query, policyId, limit, startDate, endDate);
        return resultSetReader.getAllEvents();
    }
}
