/*
 * $Id$
 */
package com.untangle.uvm.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.untangle.node.util.IOUtil;
import com.untangle.node.util.SimpleExec;

/**
 * Helper class to do backup/restore
 */
class BackupManager
{
    private static final String BACKUP_SCRIPT = System.getProperty("uvm.home") + "/bin/ut-backup-bundled.sh";;
    private static final String RESTORE_SCRIPT = System.getProperty("uvm.home") + "/bin/ut-restore-bundled.sh";

    private final Logger logger = Logger.getLogger(BackupManager.class);

    /**
     * Restore from a previous {@link #createBackup backup}.
     *
     *
     * @exception IOException something went wrong to prevent the
     *            restore (not the user's fault).
     *
     * @exception IllegalArgumentException if the provided bytes do not seem
     *            to have come from a valid backup (is the user's fault).
     */
    void restoreBackup(String fileName) 
        throws IOException, IllegalArgumentException
    {

        try {
            // Read bytes from file and pass to restoreBackup(byte[]) if successful.
            File file = new File(fileName);
            FileInputStream fileData  = new FileInputStream(file);
            int length = (int) file.length();
            byte[] bytes = new byte[length];
            fileData.read(bytes);
            restoreBackup(bytes);
        } catch (FileNotFoundException ex) {
            logger.error("Exception performing restore from file", ex);
        }
    }

    void restoreBackup(byte[] backupFileBytes)
        throws IOException, IllegalArgumentException
    {

        File tempFile = File.createTempFile("restore_", ".tar.gz");
        SimpleExec.SimpleExecResult result = null;

        try {
            //Copy the bytes to a temp file
            IOUtil.bytesToFile(backupFileBytes, tempFile);

            //unzip file
            result = SimpleExec.exec(RESTORE_SCRIPT,//cmd
                                     new String[] {//args
                                         "-i",
                                         tempFile.getAbsolutePath(),
                                         "-v"
                                     },
                                     null,//env
                                     null,//dir
                                     true,//stdout
                                     true,//stderr
                                     1000*60,//timeout
                                     logger,//log-into
                                     true);//use UVM threads

            // We no longer delete the file since it's a race.  jdi 7/06
            // IOUtil.delete(tempFile);

        }
        catch(IOException ex) {
            //Delete our temp file
            IOUtil.delete(tempFile);
            logger.error("Exception performing restore", ex);
            throw ex;
        }

        // We don't usually ever get here since the uvm is stopped by restore-mv script

        if(result.exitCode != 0) {
            switch(result.exitCode) {
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

    byte[] createBackup() throws IOException
    {

        //Create the temp file which will be the tar
        File tempFile = File.createTempFile("localdump", ".tar.gz.tmp");

        try {
            SimpleExec.SimpleExecResult result =
                SimpleExec.exec(BACKUP_SCRIPT,//cmd
                                new String[] {//args
                                    "-o",
                                    tempFile.getAbsolutePath(),
                                    "-v"
                                },
                                null,//env
                                null,//dir
                                true,//stdout
                                true,//stderr
                                1000*30,//timeout
                                logger,//log-into
                                true);//use UVM threads

            if(result.exitCode != 0) {
                throw new IOException("Unable to create local backup to \"" +
                                      tempFile.getAbsolutePath() + "\".  Process details " + result);
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
}
