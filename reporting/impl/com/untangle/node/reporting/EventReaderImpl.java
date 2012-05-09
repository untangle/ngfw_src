/**
 * $Id: EventWriter.java,v 1.00 2011/12/18 19:09:03 dmorris Exp $
 */
package com.untangle.node.reporting;

import java.io.BufferedReader;
import java.io.FileReader;

import java.util.ArrayList;
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

    public EventReaderImpl(ReportingNode node) { }

    @SuppressWarnings("unchecked")
    public ArrayList getEvents( final String query, final Long policyId, final int limit )
    {
        logger.info("getEvents( query: " + query + " policyId: " + policyId + " limit: " + limit + " )");

        try {
            String queryStr = query;
            if ( policyId == null || policyId == -1 ) {
                queryStr = queryStr.replace("= :policyId","is not null");
                queryStr = queryStr.replace("=:policyId","is not null");
            } else {
                queryStr = queryStr.replace(":policyId", Long.toString( policyId ) );
            }

            Connection conn = null;
            Statement statement = null;
            conn = UvmContextFactory.context().getDBConnection();
            if (conn == null) {
                throw new RuntimeException( "No connection to the database." );
            }
            statement = conn.createStatement();
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
                        //String oStr = null;
                        //if (o != null)
                        //    oStr = o.toString();
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
