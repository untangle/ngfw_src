/**
 * $Id$
 */

package com.untangle.uvm;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.ExecManagerResult;

/**
 * The Manager for system-related settings
 */
public class SyncSettingsImpl implements SyncSettings
{
    public static final String SYNC_SETTINGS = "/usr/bin/sync-settings";

    private final Logger logger = Logger.getLogger(this.getClass());
    /**
     * Run sync-settings with filenames.
     * @param filenames String list of settings paths+filenames to run.
     * @return Boolean true if command succeeded. 
     */
    public Boolean run(String... filenames)
    {
        ExecManagerResult result;
        boolean success = true;
        String errorStr = null;
        String cmd = SYNC_SETTINGS + " -f " + String.join( " -f ", filenames);
        result = UvmContextFactory.context().execManager().exec( cmd );
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info("Syncing settings to O/S: ");
            for ( String line : lines )
                logger.info("sync-settings: " + line);
        } catch (Exception e) {}

        if ( result.getResult() != 0 ) {
            success = false;
            errorStr = "sync-settings failed: returned " + result.getResult();
        }
        
        if ( !success ) {
            throw new RuntimeException(errorStr);
        }
        return success;
    }

}
