/**
 * $Id: ClamDaemonController.java,v 1.00 2013/10/15 12:44:50 dmorris Exp $
 */
package com.untangle.node.clam;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;

public class ClamDaemonController
{
    private final String DAEMON_START = "/etc/init.d/clamav-daemon start";
    private final String FRESHCLAM_START = "/etc/init.d/clamav-freshclam start";
    private final String DAEMON_STOP = "/etc/init.d/clamav-daemon stop";
    private final String FRESHCLAM_STOP = "/etc/init.d/clamav-freshclam stop";

    private static ClamDaemonController instance = null;

    private static final Logger logger = Logger.getLogger( ClamDaemonController.class );
    
    private int usageCount = 0;
    
    private ClamDaemonController() {}

    public static synchronized ClamDaemonController getInstance()
    {
        if (instance == null)
            instance = new ClamDaemonController();
        return (instance);
    }

    public synchronized void incrementUsageCount()
    {
        if ( this.usageCount == 0 ) {
            String output;
            output = UvmContextFactory.context().execManager().execOutput(DAEMON_START);
            try {
                String lines[] = output.split("\\r?\\n");
                logger.info(DAEMON_START + ": ");
                for ( String line : lines )
                    logger.info(DAEMON_START + ": " + line);
            } catch (Exception e) {}

            output = UvmContextFactory.context().execManager().execOutput(FRESHCLAM_START);
            try {
                String lines[] = output.split("\\r?\\n");
                logger.info(DAEMON_START + ": ");
                for ( String line : lines )
                    logger.info(DAEMON_START + ": " + line);
            } catch (Exception e) {}
        }
            
        this.usageCount++;
    }

    public synchronized void decrementUsageCount()
    {
        this.usageCount--;

        if ( this.usageCount < 0 ) {
            logger.warn("Invalid usageCount: " + usageCount);
        }

        if ( this.usageCount < 1 ) {
            String output;
            output = UvmContextFactory.context().execManager().execOutput(DAEMON_STOP);
            try {
                String lines[] = output.split("\\r?\\n");
                for ( String line : lines )
                    logger.info(DAEMON_STOP + ": " + line);
            } catch (Exception e) {}

            output = UvmContextFactory.context().execManager().execOutput(FRESHCLAM_STOP);
            try {
                String lines[] = output.split("\\r?\\n");
                for ( String line : lines )
                    logger.info(DAEMON_STOP + ": " + line);
            } catch (Exception e) {}
        }
    }
    
}
