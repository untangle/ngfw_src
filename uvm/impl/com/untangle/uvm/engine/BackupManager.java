/*
 * $HeadURL:$
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

import java.io.File;
import java.io.IOException;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.node.util.IOUtil;
import com.untangle.node.util.SimpleExec;
import org.apache.log4j.Logger;


/**
 * Helper class to do backup/restore
 */
class BackupManager {

    private static final String OLD_BACKUP_SCRIPT;
    private static final String BACKUP_SCRIPT;
    private static final String RESTORE_SCRIPT;
    private static final String LOCAL_ARG = "local";
    private static final String USB_ARG = "usb";





    private final Logger m_logger =
        Logger.getLogger(BackupManager.class);

    static {
        OLD_BACKUP_SCRIPT = System.getProperty("bunnicula.home")
            + "/../../bin/uvmdb-backup";
        BACKUP_SCRIPT = System.getProperty("bunnicula.home")
            + "/../../bin/mv-backup-bundled.sh";
        RESTORE_SCRIPT = System.getProperty("bunnicula.home")
            + "/../../bin/mv-restore-bundled.sh";
    }




    void localBackup() throws IOException {
        backup(true);
    }

    void usbBackup() throws IOException {
        backup(false);
    }

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
    void restoreBackup(byte[] backupFileBytes)
        throws IOException, IllegalArgumentException {


        File tempFile = File.createTempFile("restore_", ".tar.gz");
        SimpleExec.SimpleExecResult result = null;

        try {
            //Copy the bytes to a temp file
            IOUtil.bytesToFile(backupFileBytes, tempFile);

            //nohup sh @PREFIX@/usr/share/metavize/uvm_restart.sh 1 2 > @PREFIX@/var/log/uvm/restart.log 2>&1 &

            File restartLog = new File(
                                       System.getProperty("bunnicula.log.dir") +
                                       File.separator +
                                       "restart.log");

            //unzip file
            result = SimpleExec.exec(RESTORE_SCRIPT,//cmd
                                     new String[] {//args
                                         "-i",
                                         tempFile.getAbsolutePath(),
                                         "-v"
                                     },
                                     /*
                                       RESTORE_SCRIPT,
                                       new String[] {//args
                                       "sh",
                                       "-i",
                                       tempFile.getAbsolutePath(),
                                       "-v",
                                       "2>&1",
                                       "&"
                                       },*/
                                     null,//env
                                     null,//dir
                                     true,//stdout
                                     true,//stderr
                                     1000*60,//timeout
                                     m_logger,//log-into
                                     true);//use UVM threads

            // We no longer delete the file since it's a race.  jdi 7/06
            // IOUtil.delete(tempFile);

        }
        catch(IOException ex) {
            //Delete our temp file
            IOUtil.delete(tempFile);
            m_logger.error("Exception performing restore", ex);
            throw ex;
        }

        // We don't usually ever get here since the uvm is stopped by restore-mv script

        if(result.exitCode != 0) {
            switch(result.exitCode) {
            case 1:
            case 2:
            case 3:
                throw new IllegalArgumentException("File does not seem to be valid untangle backup");
            case 4:
                throw new IOException("Error in processing restore itself (yet file seems valid)");
            default:
                throw new IOException("Unknown error in local processing");
            }
        }
    }

    byte[] createBackup() throws IOException {

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
                                m_logger,//log-into
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
            m_logger.error("Exception creating backup for transfer to client", ex);
            throw new IOException("Unable to create backup file");//Generic, in case it ever gets shown in the UI
        }
    }



    // private methods --------------------------------------------------------

    private void backup(boolean local) throws IOException {

        Process p = UvmContextFactory.context().exec(new String[]
            { OLD_BACKUP_SCRIPT, local ? LOCAL_ARG : USB_ARG });
        for (byte[] buf = new byte[1024]; 0 <= p.getInputStream().read(buf); );

        while (true) {
            try {
                int exitValue = p.waitFor();
                if (0 != exitValue) {
                    throw new IOException("dump not successful");
                } else {
                    return;
                }
            } catch (InterruptedException exn) { }
        }
    }

}
