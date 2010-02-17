/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.untangle.uvm.LocalUvmContextFactory;

/**
 * Class that monitors the total memory size and the prints an error message if grows
 * to quickly.
 */
class HeapMonitor
{
    private static final Logger logger = Logger.getLogger( HeapMonitor.class );

    /* 3 Megs a second */
    private static final double DEFAULT_TRIGGER_RATE  = 3 * 1024;    /* 3 megs / sec */
    private static final int    DEFAULT_TRIGGER_MIN   = 100 * 1024;  /* Has to be above 100 megs */
    private static final int    DEFAULT_TRIGGER_POLL  = 500;         /* 1/2 second poll. */
    private static final int    DEFAULT_TRIGGER_LEVEL = 310 * 1024;  /* Do a thread dump above 310 megs */
    private static final int    DEFAULT_TRIGGER_DELAY = 3000;        /* Wait 3 seconds between events */
    
    private static final String DEFAULT_FILENAME      = null;        /* null means to log to stderr */

    /* Keys to the properties that control these items */
    private static final String KEY_PREFIX           = "com.untangle.uvm.memmonitor.";
    static final         String KEY_ENABLE_MONITOR   = KEY_PREFIX + "enabled";
    private static final String KEY_TRIGGER_RATE     = KEY_PREFIX + "rate";
    private static final String KEY_TRIGGER_MIN      = KEY_PREFIX + "min";
    private static final String KEY_TRIGGER_LEVEL    = KEY_PREFIX + "level";
    private static final String KEY_TRIGGER_POLL     = KEY_PREFIX + "poll";
    private static final String KEY_TRIGGER_INTERVAL = KEY_PREFIX + "interval";
    private static final String KEY_FILENAME         = KEY_PREFIX + "file";

    private long lastMemory;      /* The amount of memory used at the last check */
    private long lastRead;        /* The time at which memory was last updated. */
    private long nextTrigger;     /* The next time the trigger can occur */
    
    /* Configurable value that determines which rate(KB/s) to trigger a thread dump */
    private double triggerRate;
    
    /* Minimum memory usage(Kilobytes) before a trigger occurs. */
    private int triggerThreshold;
    
    /* Level at which a stack(Kilobytes) trace is triggered regardless of the rate */
    private int triggerLevel;
    
    /* Delay between each poll in millis */
    private int interval;

    /* Minimum number of milliseconds in between triggering */
    private int triggerInterval;  

    /* Name of the file to write, if null, this logs to stderr */
    private final String filename;

    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    
    /* List of listener for when there is an destabilizing memory event */
    private final Set<Listener> listeners = new HashSet<Listener>();

    private final Listener defaultListener = new DumpThreadListener();

    private volatile Thread thread;

    HeapMonitor( double rate, int threshold, int level, int interval, int triggerInterval )
    {
        this( rate, threshold, level, interval, triggerInterval, null );
    }

    HeapMonitor()
    {
        /* Set to defaults */
        this.triggerRate      = parseDouble( KEY_TRIGGER_RATE,     DEFAULT_TRIGGER_RATE );
        this.triggerThreshold = parseInt(    KEY_TRIGGER_MIN,      DEFAULT_TRIGGER_MIN ) * 1024;
        this.triggerLevel     = parseInt(    KEY_TRIGGER_LEVEL,    DEFAULT_TRIGGER_LEVEL ) * 1024;
        this.triggerInterval  = parseInt(    KEY_TRIGGER_POLL,     DEFAULT_TRIGGER_POLL );
        this.interval         = parseInt(    KEY_TRIGGER_INTERVAL, DEFAULT_TRIGGER_DELAY );
        this.filename         = parseString( KEY_FILENAME,         DEFAULT_FILENAME );

        this.listeners.add( defaultListener );        
    }

    HeapMonitor( double rate, int threshold, int level, int interval, int triggerInterval, String filename )
    {
        this.triggerRate      = rate;
        this.triggerThreshold = threshold * 1024;
        this.triggerLevel     = level * 1024;
        this.triggerInterval  = triggerInterval;
        this.interval         = interval;
        this.filename         = filename;
        this.listeners.add( defaultListener );
    }

    synchronized void registerListener( Listener hml )
    {
        this.listeners.add( hml );
    }

    synchronized void unregisterListener( Listener hml )
    {
        this.listeners.remove( hml );
    }

    long getMemoryUsageBytes()
    {
        return memoryMXBean.getHeapMemoryUsage().getUsed();
    }

    long getMemoryUsageKiloBytes()
    {
        return getMemoryUsageBytes() >> 10;
    }

    long getMemoryUsageMegaBytes()
    {
        return getMemoryUsageBytes() >> 20;
    }
    
    void enableDefaultListener()
    {
        this.listeners.add( defaultListener );
    }

    void disableDefaultListener()
    {
        this.listeners.remove( defaultListener );
    }

    synchronized void start()
    {
        if ( this.thread != null ) {
            logger.warn( "The heap<" + this + "> monitor is already started" );
            return;
        }

        logger.debug( "Starting the heap monitor" );
        
        this.thread = LocalUvmContextFactory.context().newThread(new Task());
    }
    
    synchronized void stop()
    {
        Thread t = this.thread;

        logger.debug( "Stopping the heap monitor" );
        
        if ( null != t ) {
            this.thread = null;
            try { 
                t.interrupt();
            } catch ( SecurityException e ) {
                logger.warn( "Unable to stop the heap monitor", e );
            }
        }
    }

    private void event( double rate, long usage )
    {
        for ( Listener hml : this.listeners ) hml.event( this, rate, usage );
    }

    /** XXX These should go into a util class somewhere */
    /* Retrieve a String property with a default value */
    private String parseString( String key, String defaultValue )
    {
        String value = System.getProperty( key );
        return (( null != value ) && ( value.length() > 0 )) ? value : defaultValue;
    }
    
    /* Retrieve a integer property with a default value */
    private int parseInt( String key, int defaultValue )
    {
        String value = System.getProperty( key );
        
        if (( null == value ) || ( value.length() == 0 )) return defaultValue;
        
        try {
            return Integer.parseInt( value );
        } catch ( NumberFormatException e ) {
            logger.warn( "Error parsing '"+ key + "', '" + value + "' using default value: " + defaultValue );
        }

        return defaultValue;
    }

    private double parseDouble( String key, double defaultValue )
    {
        String value = System.getProperty( key );
        
        if (( null == value ) || ( value.length() == 0 )) return defaultValue;
        
        try {
            return Double.parseDouble( value );
        } catch ( NumberFormatException e ) {
            logger.warn( "Error parsing '"+ key + "', '" + value + "' using default value: " + defaultValue );
        }

        return defaultValue;
    }
    
    private class Task implements Runnable
    {
        Task()
        {
        }

        public void run()
        {
            Runtime runtime = Runtime.getRuntime();
        
            lastMemory = getMemoryUsageBytes();
            lastRead = System.currentTimeMillis();
            nextTrigger = 0;
                    
            while ( thread == Thread.currentThread()) {
                /* Sleep a little while */
                try {
                    Thread.sleep( interval );
                } catch ( InterruptedException e ) {
                    logger.warn( "Interrupted." );
                    continue;
                }
                
                long now = System.currentTimeMillis();
                
                /* Get the new memory */
                long newMemory = getMemoryUsageBytes();
                    
                /* Calculate the rate of the growth */
                double rate = ( newMemory - lastMemory ) / ( now - lastRead ) * ( 1000.0 / 1024.0 );
                
                lastMemory = newMemory;
                lastRead = now;

                /* Just a check in case the nextTrigger gets out of sync due to a change in the clock */
                if ( nextTrigger > ( now + ( 10 * triggerInterval ))) {
                    nextTrigger = now + triggerInterval;
                    continue;
                }
                
                /* Not ready to log just yet, regardless of the state */
                if ( nextTrigger > now ) continue;

                /* Verify that memory has already passed the minimum threshhold */
                if ( newMemory < triggerThreshold ) continue;
                
                if (( newMemory > triggerLevel ) || ( rate > triggerRate )) {
                    event( rate, newMemory );
                    /* Update the trigger to fire a little later */
                    nextTrigger = System.currentTimeMillis() + triggerInterval;
                }
               
                    
                /* Update the last values */
                lastRead = now;
            }
        }
    }
    
    public static interface Listener
    {
        public void event( HeapMonitor hm, double rate, long usage );
    }

    private class DumpThreadListener implements Listener
    {
        BufferedWriter out = null;

        public synchronized void event( HeapMonitor hm, double rate, long usage )
        {
            try {
                Map<Thread,StackTraceElement[]> threadMap = Thread.getAllStackTraces();
                
                Date timestamp = new Date();
                
                openFile();
                
                println( "[" + timestamp + 
                         "] Detected a destabilizing memory event, dumping all stack traces[" + 
                         rate + "KB/s, " + ( usage >> 10 ) + "KB]." );
                for ( Map.Entry<Thread,StackTraceElement[]> entry : threadMap.entrySet()) {
                    dumpThread( entry.getKey(), entry.getValue());
                }
                
                println( "[" + timestamp + "] end of stack trace" );
            } catch ( IOException e ) {
                logger.error( "Error writing output: " + filename );
            } finally {
                closeFile();
            }
        }
        
        private void dumpThread( Thread thread, StackTraceElement[] stack ) throws IOException
        {
            println( "Thread<" + thread.getName() + "> in state <" + thread.getState() + ">" );
            
            for ( StackTraceElement element : stack ) {
                println( "   " + element.toString());
            }
        }
        
        private void openFile() throws IOException
        {
            /* Using standard error */
            if ( filename == null || ( filename.length() == 0 )) return;

            /* Just in case the previous iteration left the file open */
            closeFile();

            /* This is an appender */
            out = new BufferedWriter(new FileWriter( filename, true ));
        }

        private void println( String line ) throws IOException
        {
            if ( filename == null || ( filename.length() == 0 )) {
                System.err.println( line );
            } else {
                out.write( line + "\n" );
            }
        }

        private void closeFile()
        {
            try {
                if ( out != null ) out.close();
            } catch ( Exception ex ) {
                logger.error( "Unable to close file", ex );
            }
            out = null;
        }
    }
}