/**
 * $Id$
 */

package com.untangle.uvm;

import java.util.Map;

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
     * @param arguments Variable length list of arguments to run of the following types:
     *  String path+filename of settings file (-f)
     *  Otherwise, an expected Map.Entry to specifiy variables (-v K=V)
     * @return Boolean true if command succeeded. 
     */
    public Boolean run(Object... arguments)
    {
        ExecManagerResult result;
        boolean success = true;
        String errorStr = null;

        String cmd = SYNC_SETTINGS;
        try{
            for(Object argument : arguments){
                if(argument.getClass() == String.class){
                    cmd += " -f " + (String) argument;
                }else{
                    cmd += " -v " + ((Map.Entry) argument).getKey() + "=" + ((Map.Entry) argument).getValue();
                }
            }
        }catch(Exception e){
            logger.warn("Unable to build cmd: ", e);
        }
        logger.info(cmd);
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
