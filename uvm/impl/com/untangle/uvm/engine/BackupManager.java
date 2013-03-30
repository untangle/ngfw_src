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
    
    /**
     * Restore system state from a backup file
     */
    protected static ExecManagerResult restoreBackup( String fileName )
    {

        try {
            // Read bytes from file and pass to restoreBackup(byte[]) if successful.
            File file = new File(fileName);
            FileInputStream fileData  = new FileInputStream(file);
            int length = (int) file.length();
            byte[] bytes = new byte[length];
            fileData.read(bytes);
            restoreBackup(bytes);
        } catch ( Exception ex ) {
            logger.error("Exception performing restore from file", ex);
        }
        return null; //FIXME
    }

    protected static void restoreBackup(byte[] backupFileBytes) throws IOException, IllegalArgumentException
    {

        File tempFile = File.createTempFile("restore_", ".tar.gz");
        Integer result = null;

        try {
            //Copy the bytes to a temp file
            IOUtil.bytesToFile(backupFileBytes, tempFile);

            //restore the file
            result = UvmContextFactory.context().execManager().execResult(RESTORE_SCRIPT + " -i " + tempFile.getAbsolutePath() + " -v ");
        }
        catch(IOException ex) {
            //Delete our temp file
            IOUtil.delete(tempFile);
            logger.error("Exception performing restore", ex);
            throw ex;
        }

        // We don't usually ever get here since the uvm is stopped by restore script
        if(result != 0) {
            switch(result) {
            case 1:
            case 2:
            case 3:
                throw new IllegalArgumentException("File does not seem to be valid backup");
            case 4:
                throw new IOException("Error in processing restore itself (yet file seems valid)");
            case 5:
                throw new IOException("File is from an older version and cannot be used");
            default:
                throw new IOException("Unknown error in local processing");
            }
        }
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

    private class RestoreUploadHandler implements UploadHandler
    {
        @Override
        public String getName()
        {
            return "restore";
        }

        @Override
        public String handleFile(FileItem fileItem) throws Exception
        {
            byte[] backupFileBytes=fileItem.get();
            BackupManager.restoreBackup(backupFileBytes);
            return "restored backup file.";
        }
        
    }
}
