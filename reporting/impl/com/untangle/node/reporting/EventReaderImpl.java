/**
 * $Id: EventWriter.java,v 1.00 2011/12/18 19:09:03 dmorris Exp $
 */
package com.untangle.node.reporting;

import java.io.BufferedReader;
import java.io.FileReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONException;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.logging.LogEvent;

/**
 * Utility the reads events from the database
 */
public class EventReaderImpl
{
    private final Logger logger = Logger.getLogger(getClass());

    private ReportingNodeImpl node;

    private Connection dbConnection = null;

    private HashMap<String,Class<?>> columnTypeMap = new HashMap<String,Class<?>>();
    
    public EventReaderImpl( ReportingNodeImpl node )
    {
        this.node = node;
        this.dbConnection = null;
        this.columnTypeMap.put("inet",String.class);
    }

    @SuppressWarnings("unchecked")
    public ResultSet getEventsResultSet( final String query, final Long policyId, final int limit )
    {
        if ( dbConnection == null ) {
            try {
                dbConnection = this.node.getDbConnection();
                dbConnection.setAutoCommit(false);
            } catch (Exception e) {
                logger.warn("Unable to create connection to DB",e);
            }
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
            if (limit > 0)
                queryStr += " LIMIT " + limit + " ";

            logger.debug("getEventsResultSet( query: " + query + " policyId: " + policyId + " limit: " + limit + " )");
            logger.info("getEventsResultSet( queryStr: \"" + queryStr + "\")");
            
            Statement statement = dbConnection.createStatement();

            /* If this is an unlimited query - set a fetch size so we don't load all into memory */
            if (limit < 0 || limit > 100000)
                statement.setFetchSize(1000);
            
            if (statement == null) {
                logger.warn("Unable to create Statement");
                throw new RuntimeException("Unable to create Statement");
            }
            
            ResultSet resultSet = statement.executeQuery( queryStr );
            return resultSet;
            
        } catch (SQLException e) {
            closeDbConnection();
            logger.warn("Failed to query database", e );
            throw new RuntimeException( "Failed to query database.", e );
        } finally {
            if (dbConnection != null)
                try {dbConnection.commit();} catch(Exception exn) {}
        }
    }
        
    @SuppressWarnings("unchecked")
    public ArrayList getEvents( final String query, final Long policyId, final int limit )
    {
        try {
            ResultSet resultSet = getEventsResultSet( query, policyId, limit );
            if (resultSet == null)
                return null;
        
            ResultSetMetaData metadata = resultSet.getMetaData();
            int numColumns = metadata.getColumnCount();
                
            ArrayList newList = new ArrayList();

            while (resultSet.next()) {
                try {
                    JSONObject row = new JSONObject();
                    for ( int i = 1 ; i < numColumns+1 ; i++ ) {
                        Object o = resultSet.getObject( i );

                        // if its a special Postgres type - change it to string
                        if (o instanceof org.postgresql.util.PGobject) {
                            o = o.toString();
                        }
                        //logger.info( "getEvents( " + queryStr + " ) column[ " + metadata.getColumnName(i) + " ] = " + o);

                        row.put( metadata.getColumnName(i), o );
                    }
                    newList.add(row);
                } catch (Exception e) {
                    logger.warn("Failed to process row - skipping.",e);
                }
            }
            return newList;
        } catch (SQLException e) {
            closeDbConnection();
            logger.warn("Failed to query database", e );
            throw new RuntimeException( "Failed to query database.", e );
        }
    }

    private void closeDbConnection()
    {
        try {
            if (dbConnection != null) {
                try{dbConnection.commit();} catch(Exception e) {}
                dbConnection.close();
            }
        } catch(Exception exn) {
            logger.warn("Failed to close connection",exn);
        }
        dbConnection = null;
    }
}
