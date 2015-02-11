/**
 * $Id: IdpsEventMonitor.java 38792 2014-10-09 19:49:00Z dmorris $
 */
package com.untangle.node.idps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.sql.Timestamp;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.HostTable;
import com.untangle.uvm.HostTableEntry;
import com.untangle.uvm.util.I18nUtil;

import com.untangle.node.idps.IdpsSnortStatisticsParser;
import com.untangle.node.idps.IdpsSnortUnified2Parser;

class IdpsEventMonitor implements Runnable
{
    private static final long   SLEEP_TIME_MSEC = 30 * 1000;

    /* Delay a second while the thread is joining */
    private static final long   THREAD_JOIN_TIME_MSEC = 1000;

    protected static final Logger logger = Logger.getLogger( IdpsEventMonitor.class );

    private final IdpsNodeImpl node;

    /* The thread the monitor is running on */
    private Thread thread = null;

    /* Status of the monitor */
    private volatile boolean isAlive = true;

    /* Whether or not openvpn is started */
    private volatile boolean isEnabled = false;

    protected IdpsEventMonitor( IdpsNodeImpl node )
    {
        this.node = node;
    }

    public void run()
    {
        logger.debug( "Starting" );

        if ( !isAlive ) {
            logger.error( "died before starting" );
            return;
        }

        Date now = new Date();

        while ( true ) {
            try {
                Thread.sleep( SLEEP_TIME_MSEC );
            } catch ( InterruptedException e ) {
                logger.info( "idps event monitor was interrupted" );
            }

            /* Check if the node is still running */
            if ( !isAlive )
                break;

            /* Only log when enabled */
            if ( !isEnabled ) {
                continue;
            }

            /* Update the current time */
            now.setTime( System.currentTimeMillis() );

            processSnortLogFiles();
                
            /* Check if the node is still running */
            if ( !isAlive )
                break;
        }

        logger.debug( "Finished" );
    }

    public synchronized void start()
    {
        isAlive = true;
        isEnabled = false;

        logger.debug( "Starting IDPS Event monitor" );

        /* If thread is not-null, there is a running thread that thinks it is alive */
        if ( thread != null ) {
            logger.debug( "IDPS Event monitor is already running" );
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
            logger.debug( "Stopping IDPS Event monitor" );

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

    public IdpsSnortUnified2Parser unified2Parser = new IdpsSnortUnified2Parser();
    public IdpsSnortStatisticsParser statisticsParser = new IdpsSnortStatisticsParser();
    private long currentTime = System.currentTimeMillis();    
    private Hashtable<File, Long> fileLastPositions = new Hashtable<File, Long>();
    public void processSnortLogFiles()
    {
        /*
         * Process log entries
         */
        long lastPosition;
        long startPosition = 0;
        
        File[] files = getFiles( currentTime );
        logger.debug("processSnortLogFiles: number of files to process=" + files.length);
        for( File f: files ){
            try{
                startPosition = 
                    ( fileLastPositions.get(f.getCanonicalFile()) != null )
                    ? fileLastPositions.get(f.getCanonicalFile())
                    : 0;
                logger.debug("processSnortLogFiles: parse file=" + f.getCanonicalFile() +", startPosition="+startPosition);
                lastPosition = unified2Parser.parse( f, startPosition, node );
                    
                fileLastPositions.put( f.getCanonicalFile(), lastPosition );
            }catch( Exception e) {
                logger.debug("processSnortLogFiles: Unable to parse file: " + e );
            }
            currentTime = f.lastModified();
        }
            
        boolean found;
        Iterator<?> it = fileLastPositions.entrySet().iterator();
        while( it.hasNext() ){
            Map.Entry<?,?> pair = (Map.Entry<?,?>)it.next();
            File flpf = (File) pair.getKey();

            found = false;
            for( File f: files ){
                try{
                    if( f.getCanonicalFile().equals( flpf.getCanonicalFile() ) ){
                        found = true;
                    }
                }catch( Exception e ){
                    logger.debug("processSnortLogFiles: Unable to compare filenames: " + e );
                }
            }
            if( found == false ){
                it.remove();
            }
        }
        
        statisticsParser.parse( node );
	}
    
    public File[] getFiles( final long currentTime ){
            
        File directory = new File( "/var/log/snort" );
        File[] files = directory.listFiles( 
            new FilenameFilter() 
            {
                @Override
                public boolean accept( File directory, String name )
                {
                    if( name.startsWith("snort.log.") == false ){
                        return false;
                    }
                    try{
                        File file = new File( directory.getCanonicalPath() + "/" + name );
                        return file.isFile() && ( file.lastModified() >= currentTime );
                    }catch(Exception e){
                        return false;
                    }
                }
            } 
        );
        Arrays.sort( files, 
            new Comparator<File>()
            {
                public int compare( File f1, File f2 )
                {
                    return Long.valueOf( f1.lastModified()).compareTo(f2.lastModified());
                }
            }
        );
        return files;
    }

}
