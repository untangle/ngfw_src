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
import com.untangle.uvm.logging.SyslogManager;

/**
 * Utility the reads events from the database
 */
public class EventReaderImpl
{
    private final Logger logger = Logger.getLogger(getClass());

    private Connection dbConnection;

    private HashMap<String,Class<?>> columnTypeMap = new HashMap<String,Class<?>>();
    
    public EventReaderImpl( )
    {
        this.dbConnection = null;
        this.columnTypeMap.put("inet",String.class);
    }

    @SuppressWarnings("unchecked")
    public ArrayList getEvents( final String query, final Long policyId, final int limit )
    {
        logger.info("getEvents( query: " + query + " policyId: " + policyId + " limit: " + limit + " )");

        if ( dbConnection == null ) {
            try {
                dbConnection = UvmContextFactory.context().getDBConnection();
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

            Statement statement = dbConnection.createStatement();
            if (statement == null) {
                logger.warn("Unable to create Statement");
                throw new RuntimeException("Unable to create Statement");
            }
            
            ResultSet resultSet = statement.executeQuery( queryStr );
            ResultSetMetaData metadata = resultSet.getMetaData();
            int numColumns = metadata.getColumnCount();
            logger.info( "getEvents( " + queryStr + " ) columnCount: " + numColumns);
                
            ArrayList newList = new ArrayList();
            while (resultSet.next()) {
                JSONObject row = new JSONObject();
                for ( int i = 1 ; i < numColumns+1 ; i++ ) {
                    try {
                        Object o = resultSet.getObject( i );

                        // if its a special Postgres type - change it to string
                        if (o instanceof org.postgresql.util.PGobject) {
                            o = o.toString();
                        }
                        //logger.info( "getEvents( " + queryStr + " ) column[ " + metadata.getColumnName(i) + " ] = " + o);

                        row.put( metadata.getColumnName(i), o );
                    } catch (JSONException e) {
                        logger.warn("Failed to query database, bad column.", e );
                        throw new RuntimeException( "Failed to query database, bad column.", e );
                    }
                }
                newList.add(row);
            }
            return newList;
                
        } catch (SQLException e) {
            logger.warn("Failed to query database", e );
            throw new RuntimeException( "Failed to query database.", e );
        }
    }
}
