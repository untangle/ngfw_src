/**
 * $Id: IntrusionPreventionEventMonitor.java 38792 2014-10-09 19:49:00Z dmorris $
 */
package com.untangle.app.intrusion_prevention;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;

import com.untangle.app.intrusion_prevention.IntrusionPreventionStatisticsParser;
import com.untangle.app.intrusion_prevention.IntrusionPreventionSnortUnified2Parser;

/**
 * Wach the snort/suricata unified2 event directory for activity.
 * If file size changes or new file is added, parse into event log.
 */
class IntrusionPreventionEventMonitor implements Runnable
{
    private static final String EVENT_DIRECTORY = "/var/log/suricata";
    private static final String EVENT_FILE_PREFIX = "unified2.alert.";
    public static final long SLEEP_TIME_MSEC = (long)30 * 1000;

    /* Delay a second while the thread is joining */
    private static final long THREAD_JOIN_TIME_MSEC = 1000;

    protected static final Logger logger = Logger.getLogger( IntrusionPreventionEventMonitor.class );

    private final IntrusionPreventionApp app;

    /* The thread the monitor is running on */
    private Thread thread = null;

    /* Status of the monitor */
    private volatile boolean isAlive = true;

    public IntrusionPreventionSnortUnified2Parser unified2Parser = new IntrusionPreventionSnortUnified2Parser();
    public IntrusionPreventionStatisticsParser statisticsParser = new IntrusionPreventionStatisticsParser();
    private long currentTime = System.currentTimeMillis();
    private Hashtable<File, Long> fileLastPositions = new Hashtable<>();

    /**
     * Initialize event monitor.
     *
     * @param app
     *  Intrusion Prevention application.
     */
    protected IntrusionPreventionEventMonitor( IntrusionPreventionApp app )
    {
        this.app = app;

        File f = new File( IntrusionPreventionSnortUnified2Parser.EVENT_MAP );
        if(!f.exists()){
            this.app.reconfigure(false);
        }
    }

    /**
     * Loop looking for new files and/or last file size change.
     */
    public void run()
    {
        logger.debug( "Starting" );

        if ( !isAlive ) {
            logger.error( "died before starting" );
            return;
        }

        Date now = new Date();

        while ( true ) {
            /* Check if the app is still running */
            if ( !isAlive )
                break;

            /* Update the current time */
            now.setTime( System.currentTimeMillis() );

            processEventFiles();

            /* sleep */
            try {
                Thread.sleep( SLEEP_TIME_MSEC );
            } catch ( InterruptedException e ) {
                logger.info( "ips event monitor was interrupted" );
            }

            /* Check if the app is still running */
            if ( !isAlive )
                break;
        }

        logger.debug( "Finished" );
    }

    /**
     * Start the process
     */
    public synchronized void start()
    {
        isAlive = true;

        logger.debug( "Starting Intrusion Prevention Event monitor" );

        /* If thread is not-null, there is a running thread that thinks it is alive */
        if ( thread != null ) {
            logger.debug( "Intrusion Prevention Event monitor is already running" );
            return;
        }

        thread = UvmContextFactory.context().newThread( this );
        thread.start();
    }

    /**
     * Stop the process
     */
    public synchronized void stop()
    {
        if ( thread != null ) {
            logger.debug( "Stopping Intrusion Prevention Event monitor" );

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

    /** 
     * Walk snort event files and for those that qualify, send to parser.
     */
    public void processEventFiles()
    {
        /**
         * Parse statistics immediately.
         */
        statisticsParser.parse( app );

        /*
         * Process log entries
         */
        long lastPosition;
        long startPosition = 0;
        
        File[] files = getFiles( currentTime );
        logger.debug("processEventFiles: number of files to process=" + files.length);
        for( File f: files ){
            try{
                startPosition = 
                    ( fileLastPositions.get(f.getCanonicalFile()) != null )
                    ? fileLastPositions.get(f.getCanonicalFile())
                    : 0;
                logger.debug("processEventFiles: parse file=" + f.getCanonicalFile() +", startPosition="+startPosition);
                lastPosition = unified2Parser.parse( f, startPosition, app, currentTime );
                    
                fileLastPositions.put( f.getCanonicalFile(), lastPosition );
            }catch( Exception e) {
                logger.debug("processEventFiles: Unable to parse file: " + e );
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
                    logger.debug("processEventFiles: Unable to compare filenames: " + e );
                }
            }
            if( found == false ){
                it.remove();
            }
        }
	}
    
    /**
     * Look for files at or newer than the current time
     *
     * @param currentTime
     *  Minimum time to qualify.
     * @return
     *  Array of file handlers to parse.
     */
    public File[] getFiles( final long currentTime ){
            
        File directory = new File( EVENT_DIRECTORY );
        File[] files = directory.listFiles( 
            new FilenameFilter() 
            {
                /**
                 * Accept files.
                 *
                 * @param directory
                 *  Directory to search.
                 * @param name
                 *  Filename
                 * @return
                 *  true to parse file.  Otherwise false.
                 */
                @Override
                public boolean accept( File directory, String name )
                {
                    if( name.startsWith(EVENT_FILE_PREFIX) == false ){
                        return false;
                    }
                    try{
                        File file = new File( directory.getCanonicalPath() + "/" + name );
                        if(file.isFile() && ( file.lastModified() < currentTime )){
                            file.delete();
                            return false;
                        }
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
                /**
                 * See if files are newer than the other.
                 *
                 * @param f1
                 *  File handle 1
                 * @param f2
                 *  File handle 2
                 * @return
                 *  Result of the comparision.  
                 */
                public int compare( File f1, File f2 )
                {
                    return Long.valueOf( f1.lastModified()).compareTo(f2.lastModified());
                }
            }
        );
        return files;
    }

}
