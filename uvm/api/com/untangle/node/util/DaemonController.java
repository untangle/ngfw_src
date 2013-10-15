/**
 * $Id: DaemonController.java,v 1.00 2013/10/15 12:44:50 dmorris Exp $
 */
package com.untangle.node.util;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;

/**
 * This is a utility class for starting/stopping daemons and keeping reference counts
 * so they are automatically started when 1 or more apps require them and automatically
 * stopped when no one requires them.
 */
public class DaemonController
{
    private static DaemonController instance = null;

    private static final Logger logger = Logger.getLogger( DaemonController.class );

    /**
     * Stores a map from the daemon name to the current requirement count
     * If the daemon name is not in the map that is equivalent to a requirement count of 0
     */
    private ConcurrentHashMap<String,Integer> usageCountMap = new ConcurrentHashMap<String,Integer>();
    
    private DaemonController() {}

    public static synchronized DaemonController getInstance()
    {
        if (instance == null)
            instance = new DaemonController();
        return (instance);
    }

    public synchronized void incrementUsageCount( String daemonName )
    {
        int newUsageCount = getUsageCount( daemonName ) + 1;
        setUsageCount( daemonName, newUsageCount );

        if ( newUsageCount == 1 ) {
            String cmd = "/etc/init.d/" + daemonName + " start";
            String output = UvmContextFactory.context().execManager().execOutput( cmd );
            try {
                String lines[] = output.split("\\r?\\n");
                logger.info(cmd + ": ");
                for ( String line : lines )
                    logger.info(cmd + ": " + line);
            } catch (Exception e) {}
        }
    }

    public synchronized void decrementUsageCount( String daemonName )
    {
        int newUsageCount = getUsageCount( daemonName ) - 1;
        setUsageCount( daemonName, newUsageCount );

        if ( newUsageCount < 0 ) {
            logger.warn("Invalid daemon usageCount for " + daemonName + ": " + newUsageCount);
        }

        if ( newUsageCount < 1 ) {
            String cmd = "/etc/init.d/" + daemonName + " stop";
            String output = UvmContextFactory.context().execManager().execOutput( cmd );
            try {
                String lines[] = output.split("\\r?\\n");
                for ( String line : lines )
                    logger.info(cmd + ": " + line);
            } catch (Exception e) {}
        }

    }

    public synchronized int getUsageCount( String daemonName )
    {
        Integer usageCount = usageCountMap.get( daemonName );
        if ( usageCount == null ) {
            usageCount = 0;
            usageCountMap.put( daemonName, usageCount );
        }
        return usageCount;
    }

    public synchronized void setUsageCount( String daemonName, int usageCount )
    {
        usageCountMap.put( daemonName, usageCount );
    }
}
