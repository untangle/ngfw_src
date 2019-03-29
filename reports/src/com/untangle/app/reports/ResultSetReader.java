/**
 * $Id$
 */
package com.untangle.app.reports;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.untangle.uvm.UvmContextFactory;

/**
 * A ResultSetReader is an object that holds both the ResultSet from the database and the Connecition created to retrieve it.
 * This is necessary because the way postgres jdbc cursors operates.
 *
 * For large queries we don't want all the results at once, so we use a cursor.
 * Postgres JDBC cursors require autoCommit to false, and so this means a read-lock is held while the ResultSet and Connection are open
 * So you must call commit on the Connection after done reading the results. Forgetting to do so is very bad because it prevents
 * writing and vacuuming of the locked table. So there is a built in safety mechanism to close the connection if it is unused for a while
 */
public class ResultSetReader implements Runnable
{
    private static final Logger logger = Logger.getLogger(ResultSetReader.class);

    /**
     * MAX_RESULTS defines the maximum # of results that can be serialized in memory at one time
     * with getAllEvents()
     * If more than this number of results is required it should be read my chunk
     * with getNextChunk()
     */
    private static final int MAX_RESULTS = 500000;

    private ResultSet resultSet;
    private Connection connection;
    private Statement statement;
    private final Thread thread;
    
    /**
     * Initialize ResultSetReader
     *
     * @param resultSet
     *  ResultSet object
     * @param connection
     *  Connection object
     * @param statement
     *  Statement object
     */
    public ResultSetReader( ResultSet resultSet, Connection connection, Statement statement )
    {
        this.resultSet = resultSet;
        this.connection = connection;
        this.statement = statement;

        try {
            if ( this.resultSet != null && ! this.resultSet.isClosed() ) {
                this.resultSet.setFetchDirection( ResultSet.FETCH_FORWARD );
            }
        }
        catch (Exception e) {
            logger.warn( "Exception", e );
        }

        this.thread = UvmContextFactory.context().newThread( this, "ResultSetReader" );
        thread.start();
    }

    /**
     * Return the result set.
     *
     * @return
     *  ResultSet object.
     */
    public ResultSet getResultSet()
    {
        return this.resultSet;
    }

    /**
     * Return the connection
     *
     * @return
     *  Connection object.
     */
    public Connection getConnection()
    {
        return this.connection;
    }

    /**
     * Test if the result set is closed.
     *
     * @return
     *  true if closed, false otherwise.
     */
    public boolean isClosed()
    {
        try {
            return this.resultSet.isClosed();
        } catch (Exception e) {
            logger.warn( "Exception", e );
            return true;
        }
    }
    
    /**
     * Get the next set of results.
     *
     * @param chunkSize
     *  Maximum number of results to return.
     * @return
     *  ArrayList of result objects.
     */
    public ArrayList<Object> getNextChunk( int chunkSize )
    {
        ArrayList<Object> newList = new ArrayList<>( chunkSize );

        try {
            if ( resultSet.isClosed()  ){
                return newList;
            }

            ResultSetMetaData metadata = this.resultSet.getMetaData();
            int numColumns = metadata.getColumnCount();

            for ( int i = 0 ; i < chunkSize && this.resultSet.next() ; i++ ) {
                try {
                    JSONObject row = new JSONObject();
                    for ( int columnIter = 1 ; columnIter < numColumns+1 ; columnIter++ ) {
                        Object o = resultSet.getObject( columnIter );

                        // if its a special Postgres type - change it to string
                        if (o instanceof org.postgresql.util.PGobject) {
                            o = o.toString();
                        }

                        row.put( metadata.getColumnName(columnIter), o );
                    }
                    
                    newList.add(row);
                } catch (Exception e) {
                    logger.warn("Failed to process row - skipping.",e);
                }
            }

            if ( ! this.resultSet.next() ) {
                this.closeConnection();
            }
        } catch ( java.sql.SQLException e ) {
            logger.warn("Exception retrieving events",e);
        }

        return newList;
    }

    /**
     * Get all results in JSON format.
     *
     * @return
     *  ArrayList of all results in JSON format.
     */
    public ArrayList<JSONObject> getAllEvents()
    {
        ArrayList<JSONObject> newList = new ArrayList<>();

        try {
            if (resultSet == null || connection == null)
                return newList;
            if ( resultSet.isClosed() || !resultSet.isBeforeFirst() /*isEmpty*/ )
                return newList;

            ResultSetMetaData metadata = resultSet.getMetaData();
            int numColumns = metadata.getColumnCount();

            int count = 0;
            while (resultSet.next() && count < MAX_RESULTS) {
                try {
                    count++;
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
            if ( count >= MAX_RESULTS ) {
                logger.warn("Results truncated due to MAX_RESULTS");
            }

            return newList;
        } catch (SQLException e) {
            logger.warn("Failed to query database", e );
            throw new RuntimeException( "Failed to query database.", e );
        } finally {
            closeConnection();
        }
    }
    
    /**
     * Close the connection object.
     */
    public synchronized void closeConnection()
    {
        if ( this.statement != null ) {
            try { this.statement.close(); } catch (Exception e) {
                logger.warn("Close Exception",e);
            }
            this.statement = null;
        }
        if ( this.connection != null ) {
            try { this.connection.commit(); } catch (Exception e) {}
            try { this.connection.close(); } catch (Exception e) {}
            this.connection = null;
        }
        if ( this.thread != null )
            this.thread.interrupt();
        
        return;
    }

    /**
     * The thread checks occasionally to make sure this reader is being used.
     * If not, it commits and closes the connection. This is because leaving
     * queries uncommited is dangerous as it holds a lock on the data.
     * This is a safety mechanism that should not be relied on, so it prints a warning
     */
    public void run()
    {
        if (this.resultSet == null) {
            logger.warn("Invalid resultSet: null");
            closeConnection();
            return;
        }
        
        try {
            if ( resultSet.isClosed() ) {
                closeConnection();
                return;
            }
            int lastPosition;
            
            // JDBC can throw null pointer exceptions in this case
            // no idea why. Just go ahead and close the connection.
            try {
                lastPosition = this.resultSet.getRow();
            } catch ( java.lang.NullPointerException e ) {
                closeConnection();
                return;
            }
        
            while ( true ) {
                try {
                    Thread.sleep( 30*1000 ); /* sleep 30 seconds */
                } catch (Exception e) {}

                if ( this.connection == null || this.connection.isClosed() )
                    return;
                if ( this.resultSet == null ) {
                    logger.warn("Invalid resultSet: null");
                    closeConnection();
                    return;
                }

                if ( this.resultSet.getRow() == 0 ) {
                    logger.warn("Unclosed ResultSetReader! ( left open ) - closing!");
                    closeConnection();
                    return;
                }
                if ( lastPosition == this.resultSet.getRow() ) {
                    logger.warn("Unclosed ResultSetReader! ( stuck at row " + lastPosition + " ) - closing!");
                    closeConnection();
                    return;
                }
                lastPosition = this.resultSet.getRow();
            }
        }
        catch ( org.postgresql.util.PSQLException e ) {
            if ( e.getMessage() != null && e.getMessage().contains("is closed") ) {
                //result set is closed, in this case, we're done
            } else {
                // some other exception
                logger.warn("Exception in ResultSetReader", e);
            }
            closeConnection();
            return;
        } 
        catch ( Exception e ) {
            logger.warn("Exception in ResultSetReader", e);
            closeConnection();
            return;
        } 
    }
    
}

