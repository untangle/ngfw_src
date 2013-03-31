/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.node.util.IOUtil;
import com.untangle.uvm.servlet.UploadHandler;
import com.untangle.uvm.servlet.UploadManager;

/**
 * Helper class to do backup/restore
 */
public class BackupManager
{
    private static final String BACKUP_SCRIPT = System.getProperty("uvm.home") + "/bin/ut-backup.sh";;
    private static final String RESTORE_SCRIPT = System.getProperty("uvm.home") + "/bin/ut-restore.sh";

    private static final Logger logger = Logger.getLogger(BackupManager.class);

    protected BackupManager()
    {
        UvmContextFactory.context().uploadManager().registerHandler(new RestoreUploadHandler());
    }
    
    protected static byte[] createBackup() throws IOException
    {
        //Create the temp file which will be the tar
        File tempFile = File.createTempFile("localdump", ".tar.gz.tmp");

        try {
            Integer result = UvmContextFactory.context().execManager().execResult(BACKUP_SCRIPT + " -o " + tempFile.getAbsolutePath() +" -v");

            if(result != 0) {
                throw new IOException("Unable to create local backup to \"" + tempFile.getAbsolutePath() + "\".  Process details " + result);
            }

            //Read contents into a byte[]
            byte[] ret = IOUtil.fileToBytes(tempFile);

            //Delete our temp files
            IOUtil.delete(tempFile);
            return ret;
        }
        catch(IOException ex) {
            //Don't forget to delete the temp file
            IOUtil.delete(tempFile);
            logger.error("Exception creating backup for transfer to client", ex);
            throw new IOException("Unable to create backup file - can't transfer to client.");//Generic, in case it ever gets shown in the UI
        }
    }

    private static ExecManagerResult restoreBackup(byte[] backupFileBytes) throws IOException, IllegalArgumentException
    {

        File tempFile = File.createTempFile("restore_", ".tar.gz");
        ExecManagerResult checkResult = null;
        ExecManagerResult result = null;

        try {
            //Copy the bytes to a temp file
            IOUtil.bytesToFile(backupFileBytes, tempFile);
        }
        catch(IOException ex) {
            //Delete our temp file
            IOUtil.delete(tempFile);
            logger.error("Exception performing restore", ex);
            throw ex;
        }

        logger.info("Restore Backup: " + tempFile);
        
        // just check the backup file
        logger.info("Restore Backup: check file " + tempFile);
        checkResult = UvmContextFactory.context().execManager().exec(RESTORE_SCRIPT + " -i " + tempFile.getAbsolutePath() + " -v -c");

        // if the backup file is not legitimate then just return the results
        if (checkResult.getResult() != 0) {
            return checkResult;
        }

        // get the list of required files
        logger.info("Restore Backup: check packages " + tempFile);
        result = UvmContextFactory.context().execManager().exec(RESTORE_SCRIPT + " -i " + tempFile.getAbsolutePath() + " -f");

        // if the backup file is not legitimate then just return the results
        if (result.getResult() != 0) {
            return result;
        }

        // install all the needed packages
        String[] packages = result.getOutput().split("[\\r\\n]+");
        boolean installingPackages = false;
        String msg = "Files required for the restore are missing. Please retry again after the download is complete.<br/>";
        if (packages != null) {
            for ( String pkg : packages ) {
                if (! UvmContextFactory.context().toolboxManager().isInstalled( pkg )) {
                    logger.info("Restore Backup: need package: " + pkg);
                    installingPackages = true;
                    msg = msg + pkg + "<br/>";
                    UvmContextFactory.context().toolboxManager().requestInstall( pkg );
                }
            }
        }
        if (installingPackages) {
            return new ExecManagerResult( 0, msg);
        }
            
        // run same command with nohup and without -c check-only flag
        logger.info("Restore Backup: launching restore " + tempFile);
        UvmContextFactory.context().execManager().exec("nohup " + RESTORE_SCRIPT + " -i " + tempFile.getAbsolutePath() + " -v >/var/log/uvm/restore.log 2>&1 &");

        logger.info("Restore Backup: returning");
        return new ExecManagerResult( 0, "The restore procedure is running. This may take several minutes. The server may be unavailable during this time. Once the process is complete you will be able to log in again.");
    }

    private class RestoreUploadHandler implements UploadHandler
    {
        @Override
        public String getName()
        {
            return "restore";
        }

        @Override
        public ExecManagerResult handleFile(FileItem fileItem) throws Exception
        {
            return BackupManager.restoreBackup( fileItem.get() );
        }
        
    }
}
