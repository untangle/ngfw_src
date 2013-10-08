/**
 * $Id: OpenVpnMonitor.java 35345 2013-07-19 04:32:15Z dmorris $
 */
package com.untangle.node.openvpn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.File;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.sql.Timestamp;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.util.I18nUtil;


class OpenVpnMonitor implements Runnable
{
    /* Poll every 5 seconds */
    private static final long   SLEEP_TIME_MSEC = 10 * 1000;

    /* Log every 5 minutes ? */
    private static final long   LOG_TIME_MSEC = 5 * 60 * 1000;

    /* Delay a second while the thread is joining */
    private static final long   THREAD_JOIN_TIME_MSEC = 1000;

    /* Interrupt if there is no traffic for 2 seconds */
    private static final int READ_TIMEOUT = 2000;

    private static final String KILL_CMD   = "kill";
    private static final String STATUS_CMD = "status 2";
    private static final String KILL_UNDEF = KILL_CMD + " UNDEF";
    private static final String END_MARKER = "end";

    private static final int TYPE_INDEX    = 0;
    private static final int NAME_INDEX    = 1;
    private static final int ADDRESS_INDEX = 2;
    private static final int RX_INDEX      = 4;
    private static final int TX_INDEX      = 5;
    private static final int START_INDEX   = 7;
    private static final int TOTAL_INDEX   = 8;

    private final Logger logger = Logger.getLogger( this.getClass());

    private Map<Key,Stats> statusMap    = new HashMap<Key,Stats>();
    private Map<String,Stats> activeMap = new HashMap<String,Stats>();

    /* This is a list that contains the contents of the command "status 2" from openvpn */
    private final List<String> clientStatus = new LinkedList<String>();

    private final OpenVpnNodeImpl node;

    /* The thread the monitor is running on */
    private Thread thread = null;

    /* Status of the monitor */
    private volatile boolean isAlive = true;

    /* Whether or not openvpn is started */
    private volatile boolean isEnabled = false;

    protected OpenVpnMonitor( OpenVpnNodeImpl node )
    {
        this.node = node;
    }

    public void run()
    {
        logger.debug( "Starting" );

        /* Flush both of these maps */
        statusMap = new HashMap<Key,Stats>();
        activeMap = new HashMap<String,Stats>();

        Date nextUpdate = new Date(( new Date()).getTime() + LOG_TIME_MSEC );

        if ( !isAlive ) {
            logger.error( "died before starting" );
            return;
        }

        Date now = new Date();

        while ( true ) {
            try {
                Thread.sleep( SLEEP_TIME_MSEC );
            } catch ( InterruptedException e ) {
                logger.info( "openvpn monitor was interrupted" );
            }

            /* Check if the node is still running */
            if ( !isAlive )
                break;

            /* Only log when enabled */
            if ( !isEnabled ) {
                flushLogEvents();
                continue;
            }

            /* Update the current time */
            now.setTime( System.currentTimeMillis() );

            // Grab lock, such that a concurrent read of the "activeMap"
            // doesn't happen during an update
            synchronized(this) {
                try {
                    /* Cleanup UNDEF sessions every time you are going to update the stats */
                    boolean killUndef = now.after( nextUpdate );
                    updateServerStatus( killUndef );
                } catch (java.net.ConnectException e) {
                    logger.debug( "Unable to connect to OpenVPN - trying again in " + SLEEP_TIME_MSEC + " ms.");
                } catch ( Exception e ) {
                    logger.info( "Error updating status", e );
                }

                if ( now.after( nextUpdate )) {
                    nextUpdate.setTime( now.getTime() + LOG_TIME_MSEC );
                }
            }

            /**
             * Check that all necessary clients are running
             */
            checkRemoteServerProcesses();
            checkServerProcess();
                
            /* Check if the node is still running */
            if ( !isAlive )
                break;
        }

        /* Flush out all of the log events that are remaining */
        flushLogEvents();

        logger.debug( "Finished" );
    }

    /**
     * Method returns a list of open clients as OpenVpnStatusEvents w/o
     * an end date.
     */
    public synchronized List<OpenVpnStatusEvent> getOpenConnectionsAsEvents()
    {
        Date now = new Date();
        List<OpenVpnStatusEvent> ret = new ArrayList<OpenVpnStatusEvent>();
        for(Stats s : activeMap.values()) {
            if(s.isActive) {
                OpenVpnStatusEvent copy = s.getCurrentStatusEventCopy(now);
                copy.setEnd(null);
                ret.add(copy);
            }
        }

        return ret;
    }


    public synchronized void start()
    {
        isAlive = true;
        isEnabled = false;

        logger.debug( "Starting OpenVpn monitor" );

        /* If thread is not-null, there is a running thread that thinks it is alive */
        if ( thread != null ) {
            logger.debug( "OpenVpn monitor is already running" );
            return;
        }

        thread = UvmContextFactory.context().newThread( this );
        thread.start();
    }

    public synchronized void enable()
    {
        isEnabled = true;
    }

    public synchronized void disable()
    {
        isEnabled = false;
    }

    public synchronized void stop()
    {
        if ( thread != null ) {
            logger.debug( "Stopping OpenVpn monitor" );

            isAlive = false;
            try {
                thread.interrupt();
                thread.join( THREAD_JOIN_TIME_MSEC );
            } catch ( SecurityException e ) {
                logger.error( "security exception, impossible", e );
            } catch ( InterruptedException e ) {
                logger.error( "interrupted while stopping", e );
            }
            thread = null;
        }
    }

    private void updateServerStatus( boolean killUndef )
        throws UnknownHostException, SocketException, IOException
    {
        Socket socket = null;
        BufferedReader in = null;
        BufferedWriter out = null;

        try {
            /* Connect to the management port */
            socket = new Socket((String)null, OpenVpnManager.MANAGEMENT_PORT );

            socket.setSoTimeout( READ_TIMEOUT );

            in = new BufferedReader( new InputStreamReader( socket.getInputStream()));
            out = new BufferedWriter( new OutputStreamWriter( socket.getOutputStream()));

            /* Read out the hello message */
            in.readLine();

            /* First kill all of the undefined connections if necessary */
            if ( killUndef ) {
                logger.debug( "Killing all undefined clients" );
                writeCommandAndFlush( out, in, KILL_UNDEF );
            }

            /* Now get the status */
            writeCommand( out, STATUS_CMD );

            /* Set all of the stats to not updated */
            for ( Stats stats : statusMap.values()) stats.updated = false;

            /* Preload, so it is is safe to send commands while processeing */
            this.clientStatus.clear();

            while( true ) {
                String line = in.readLine().trim();
                if ( line.equalsIgnoreCase( END_MARKER )) break;
                this.clientStatus.add( line );
            }

            for ( String line : this.clientStatus ) processLine( line );

            /* Log disconnects and connects */
            logEvents();

            /* Check for any dead connections */
            killDeadConnections( out, in );
        } finally {
            if ( out != null )    out.close();
            if ( in != null )     in.close();
            if ( socket != null ) socket.close();
        }
    }

    /* Flush out any remainging log events and indicate that
     * all of the active sessions have completed */
    private void flushLogEvents()
    {
        Timestamp now = new Timestamp((new Date()).getTime());

        for ( Stats stats : activeMap.values() ) {
            stats.fillEvent( now );
            node.logEvent( stats.sessionEvent );
        }

        activeMap.clear();
    }

    private void logEvents()
    {
        Timestamp now = new Timestamp((new Date()).getTime());

        for ( Stats stats : statusMap.values()) {
            stats.fillEvent( now );
            if ( logger.isDebugEnabled()) logger.debug( "Logging stats for " + stats.key );
            node.logEvent( stats.sessionEvent );
        }
    }

    private void killDeadConnections( BufferedWriter out, BufferedReader in ) throws IOException
    {
        for ( Iterator<Stats> iter = statusMap.values().iterator() ; iter.hasNext() ; ) {
            Stats stats = iter.next();

            if ( stats.isActive && stats.updated ) continue;

            /* Remove any nodes that are not active */
            iter.remove();

            /* Remove the active entries from the active map */
            if ( stats.isActive ) activeMap.remove( stats.key.name );

            /* If this client was in the current list of clients, then kill it */
            if ( stats.updated ) {
                String command = KILL_CMD + " " + stats.key.address.getHostAddress() + ":" + stats.key.port;
                writeCommandAndFlush( out, in, command );
            }
        }

        for ( Iterator<Stats> iter = activeMap.values().iterator() ; iter.hasNext() ; ) {
            Stats stats = iter.next();

            if ( stats.isActive ) continue;

            logger.warn( "Inactive node in the active map[" + stats.key + "]" );

            /* Remove any nodes that are not active */
            iter.remove();

            statusMap.remove( stats.key );
        }
    }

    private void processLine( String line )
    {
        String valueArray[] = line.split( "," );
        if ( !valueArray[TYPE_INDEX].equals( "CLIENT_LIST" )) return;

        if ( valueArray.length != TOTAL_INDEX ) {
            logger.info( "Strange client description, ignoring: " + line );
            return;
        }

        String name = valueArray[NAME_INDEX];

        /* Ignore undef entries */
        if ( name.equalsIgnoreCase( "undef" )) return;

        String addressAndPort[] = valueArray[ADDRESS_INDEX].split( ":" );
        if ( addressAndPort.length != 2 ) {
            logger.info( "Strange address description, ignoring: " + line );
            return;
        }


        InetAddress address = null;
        int port = 0;
        long bytesRx = 0;
        long bytesTx = 0;
        Date start = null;

        try {
            address = InetAddress.getByName( addressAndPort[0] );
            port    = Integer.parseInt( addressAndPort[1] );
            bytesRx = Long.parseLong( valueArray[RX_INDEX] );
            bytesTx = Long.parseLong( valueArray[TX_INDEX] );
            start   = new Date( Long.parseLong( valueArray[START_INDEX] ) * 1000 );
        } catch ( Exception e ) {
            logger.warn( "Unable to parse line: " + line, e );
            return;
        }

        Key key = new Key( name, address, port, start );
        Stats stats = statusMap.get( key );

        if ( stats == null ) {
            node.incrementConnectCount();

            stats  = activeMap.get( name );
            if ( stats == null ) {
                if ( logger.isDebugEnabled()) logger.debug( "New vpn client session: inserting key " + key );
                stats = new Stats( key, bytesRx, bytesTx );
                stats.isActive = true;
                stats.updated = true;
                activeMap.put( name, stats );
                statusMap.put( key, stats );
            } else {
                /* This is a new session */
                if ( stats.key.start.after( start )) {
                    if ( logger.isDebugEnabled()) {
                        logger.debug( "newer vpn client [" + stats.key + "] session: not using key " + key );
                    }
                    /* Create a disable stats */
                    stats = new Stats( key, bytesRx, bytesTx );
                    stats.isActive = false;
                    stats.updated = true;
                    statusMap.put( key, stats );
                } else {
                    if ( logger.isDebugEnabled()) {
                        logger.debug( "older vpn client [" + stats.key + "] session: inserting key " + key );
                    }
                    /* This is an older session */

                    /* Disable the current stats */
                    stats.isActive = false;
                    stats.updated  = true;

                    /* Create new stats and replace them in the map */
                    stats = new Stats( key, bytesRx, bytesTx );
                    stats.isActive = true;
                    stats.updated = true;

                    /* Replace the status map */
                    statusMap.put( key, stats );
                    activeMap.put( name, stats );
                }
            }
        } else {
            if ( logger.isDebugEnabled()) logger.debug( "current vpn client [" + stats.key + "] updating." );
            stats.update( bytesRx, bytesTx );
        }
    }

    private void writeCommand( BufferedWriter out, String command ) throws IOException
    {
        out.write( command + "\n" );
        out.flush();
    }

    private void writeCommandAndFlush( BufferedWriter out, BufferedReader in, String command )
        throws IOException
    {
        writeCommand( out, command );

        /* Read out the response, ignore it */
        in.readLine();
    }

    /**
     * Checks that all enabled remote servers have a running OpenVpn process
     * If one is missing it restarts it
     */
    private void checkRemoteServerProcesses()
    {
        for ( OpenVpnRemoteServer server : node.getSettings().getRemoteServers() ) {
            if ( ! server.getEnabled() )
                continue;

            try {
                File pidFile = new File("/var/run/openvpn." + server.getName() + ".pid");
                if (! pidFile.exists() )
                    continue;

                BufferedReader reader = new BufferedReader(new FileReader(pidFile));
                String currentLine;
                String contents = "";
                while((currentLine = reader.readLine()) != null) {
                    contents += currentLine;
                }

                int pid;
                try {
                    pid = Integer.parseInt(contents);
                } catch ( Exception e ) {
                    logger.warn("Unable to parse pid file: " + contents);
                    continue;
                }

                File procFile = new File("/proc/" + pid);
                if ( ! procFile.exists() ) {
                    logger.warn("OpenVpn process for " + server.getName() + " (" + pid + ") missing. Restarting...");
                    UvmContextFactory.context().execManager().exec( "/etc/init.d/openvpn restart " + server.getName() );
                }

            } catch ( Exception e ) {
                logger.warn("Failed to check openvpn pid file.", e);
            }
        }
    }

    /**
     * Checks that all enabled remote servers have a running OpenVpn process
     * If one is missing it restarts it
     */
    private void checkServerProcess()
    {
        if ( ! this.node.getSettings().getServerEnabled() )
            return;

        try {
            File pidFile = new File("/var/run/openvpn.server.pid");
            if (! pidFile.exists() )
                return;

            BufferedReader reader = new BufferedReader(new FileReader(pidFile));
            String currentLine;
            String contents = "";
            while((currentLine = reader.readLine()) != null) {
                contents += currentLine;
            }

            int pid;
            try {
                pid = Integer.parseInt(contents);
            } catch ( Exception e ) {
                logger.warn("Unable to parse pid file: " + contents);
                return;
            }

            File procFile = new File("/proc/" + pid);
            if ( ! procFile.exists() ) {
                logger.warn("OpenVpn server process (" + pid + ") missing. Restarting...");
                UvmContextFactory.context().execManager().exec( "/etc/init.d/openvpn restart server" );
            }
        } catch ( Exception e ) {
            logger.warn("Failed to check openvpn pid file.", e);
        }
    }

}

class Key
{
    final String name;
    final InetAddress address;
    final int port;
    final Date start;
    final int hashCode;

    protected Key( String name, InetAddress address, int port, Date start )
    {
        this.name     = name;
        this.address  = address;
        this.port     = port;
        this.start    = start;
        this.hashCode = calculateHashCode();
    }

    public String toString()
    {
        return this.name + "@" + address.getHostAddress() + ":" + port;
    }

    public boolean equals( Object o )
    {
        if ( !(o instanceof Key )) return false;

        Key k = (Key)o;

        if (( k.port == this.port ) && k.start.equals( this.start ) &&
            k.address.equals( this.address ) && k.name.equals( this.name )) {
            return true;
        }

        return false;
    }

    public int hashCode()
    {
        return hashCode;
    }

    private int calculateHashCode()
    {
        int result = 17;
        result = ( 37 * result ) + this.port;
        result = ( 37 * result ) + this.name.hashCode();
        result = ( 37 * result ) + this.start.hashCode();
        result = ( 37 * result ) + this.address.hashCode();

        return result;
    }

}

class Stats
{
    final Key key;

    final OpenVpnStatusEvent sessionEvent;

    /* Total bytes received since the last event */
    long bytesRxDelta;

    /* Total bytes received */
    long bytesRxTotal;

    /* Total bytes transferred since the last event  */
    long bytesTxDelta;

    /* Total bytes transferred */
    long bytesTxTotal;

    Date lastUpdate;

    boolean updated;

    boolean isActive;

    protected Stats( Key key, long bytesRx, long bytesTx )
    {
        this.key          = key;
        this.bytesRxTotal = bytesRx;
        this.bytesRxDelta  = bytesRx;
        this.bytesTxTotal = bytesTx;
        this.bytesTxDelta  = bytesTx;
        this.lastUpdate   = new Date();
        this.isActive     = true;
        this.sessionEvent = new OpenVpnStatusEvent( new Timestamp(key.start.getTime()), this.key.address, this.key.port, this.key.name );
    }

    void fillEvent( Timestamp now )
    {
        this.fillEvent( this.sessionEvent, now );
    }

    void fillEvent( OpenVpnStatusEvent event, Timestamp now )
    {
        event.setEnd( now );
        event.setBytesTxTotal( this.bytesTxTotal );
        event.setBytesRxTotal( this.bytesRxTotal );
        event.setBytesTxDelta( this.bytesTxDelta );
        event.setBytesRxDelta( this.bytesRxDelta );
    }
    
    OpenVpnStatusEvent getCurrentStatusEventCopy(Date now)
    {
        return new OpenVpnStatusEvent( this.sessionEvent );
    }

    void update( long newBytesRxTotal, long newBytesTxTotal )
    {
        long prevBytesRxTotal = this.bytesRxTotal;
        long prevBytesTxTotal = this.bytesTxTotal;
        this.bytesRxTotal = newBytesRxTotal;
        this.bytesTxTotal = newBytesTxTotal;
        this.bytesRxDelta = this.bytesRxTotal - prevBytesRxTotal;
        this.bytesTxDelta = this.bytesTxTotal - prevBytesTxTotal;
        this.updated = true;
    }
}
