/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.engine;

import java.io.IOException;
import com.metavize.tran.util.SimpleExec;
import com.metavize.tran.util.IOUtil;
import org.apache.log4j.Logger;
import java.io.File;


/**
 * Helper class to do backup/restore
 */
class BackupManager {

  private static final String BACKUP_SCRIPT;
  private static final String LOCAL_ARG;
  private static final String USB_ARG;

  private final Logger m_logger =
    Logger.getLogger(MvvmContextImpl.class);  

  static {
    BACKUP_SCRIPT = System.getProperty("bunnicula.home")
        + "/../../bin/mvvmdb-backup";
    LOCAL_ARG = "local";
    USB_ARG = "usb";
  }




  void localBackup() throws IOException {
    backup(true);
  }

  void usbBackup() throws IOException {
    backup(false);
  }

  void restoreBackup(byte[] backupFileBytes)
    throws IOException {


    File tempFile = File.createTempFile("restore_", ".tar.gz");
/*    
    try {
      //Copy the bytes to a temp file
      IOUtil.bytesToFile(backupFileBytes, tempFile);
  
      //unzip file
      SimpleExec.SimpleExecResult result =
        SimpleExec.exec("gunzip",//cmd
          new String[] {//args
            tempFile.getAbsolutePath()
          },
          null,//env
          tempFile.getParentFile(),//dir
          true,//stdout
          true,//stderr
          1000*3,//timeout
          m_logger,//log-into
          true);//use MVVM threads

      if(result.exitCode != 0) {
        throw new BadFileException("Unable to unzip file.  Process details " + result);
      }


      
  
      //create a temp dir
  
      //untar the file into temp dir
  
      //delete temp file
  
      //validate the contents of temp dir
  
      //Make log entry now, as executing this script is suicide for
      //this process.
      
      //execute restore script
    }
    catch(IOException ex) {
      
    }
*/          
  }

  byte[] createBackup() throws IOException {

    //Create the temp dir for TARing-to
    File tempDir = IOUtil.mktempDir();
    m_logger.debug("Going to use " + tempDir.getAbsolutePath() + " to hold backup dump");

    //Create the temp file which will be the tar
    File tempFile = File.createTempFile("localdump", ".tar.tmp");

    try {
      SimpleExec.SimpleExecResult result =
        SimpleExec.exec(BACKUP_SCRIPT,//cmd
          new String[] {//args
            LOCAL_ARG,
            tempDir.getAbsolutePath()
          },
          null,//env
          null,//dir
          true,//stdout
          true,//stderr
          1000*30,//timeout
          m_logger,//log-into
          true);//use MVVM threads

      if(result.exitCode != 0) {
        throw new IOException("Unable to create local backup to \"" +
          tempDir + "\".  Process details " + result);
      }

      //Now, we have the three files in a single directory.  Time to tar them up
      result = SimpleExec.exec("tar",
          new String[] {
            "-cf",
            tempFile.getAbsolutePath(),
            "."
          },
          null,
          tempDir,
          true,
          true,
          1000*10,
          m_logger,
          true);

      if(result.exitCode != 0) {
        throw new IOException("Unable to tar backup in \"" +
          tempDir + "\".  Process details " + result);
      }

      //GZIP the stuff
      result = SimpleExec.exec("gzip",
          new String[] {
            "-c",
            tempFile.getAbsolutePath()
          },
          null,
          null,
          true,
          true,
          1000*10,
          m_logger,
          true);

      if(result.exitCode != 0) {
        throw new IOException("Unable to gzip backup \"" +
          tempFile + "\".  Process details " + result);
      }

      //Delete our temp files
      IOUtil.rmDir(tempDir);
      IOUtil.delete(tempFile);
      return result.stdOut;
    }
    catch(IOException ex) {
      //Don't forget to delete the temp dir
      IOUtil.rmDir(tempDir);
      IOUtil.delete(tempFile);
      m_logger.error("Exception creating backup for transfer to client", ex);
      throw new IOException("Unable to create backup file");//Generic, in case it ever gets shown in the UI
    }
  }



  // private methods --------------------------------------------------------

  private void backup(boolean local) throws IOException {
  
    Process p = Runtime.getRuntime().exec(new String[]
        { BACKUP_SCRIPT, local ? LOCAL_ARG : USB_ARG });
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
