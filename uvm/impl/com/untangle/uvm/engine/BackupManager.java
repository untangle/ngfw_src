/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Calendar;
import java.text.SimpleDateFormat;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.node.util.IOUtil;
import com.untangle.uvm.servlet.UploadHandler;
import com.untangle.uvm.servlet.DownloadHandler;

/**
 * Helper class to do backup/restore
 */
public class BackupManager
{
    private static final String BACKUP_SCRIPT = System.getProperty("uvm.home") + "/bin/ut-backup.sh";;
    private static final String RESTORE_SCRIPT = System.getProperty("uvm.home") + "/bin/ut-restore.sh";

    private final Logger logger = Logger.getLogger(BackupManager.class);

    private I18nUtil i18nUtil;

    protected BackupManager()
    {
        UvmContextFactory.context().servletFileManager().registerUploadHandler( new RestoreUploadHandler() );
        UvmContextFactory.context().servletFileManager().registerDownloadHandler( new BackupDownloadHandler() );

        Map<String,String> i18nMap = UvmContextFactory.context().languageManager().getTranslations("untangle-libuvm");
        this.i18nUtil = new I18nUtil(i18nMap);
    }
    
    private File createBackup() 
    {
        File tempFile = null;
        
        try {
            //Create the temp file which will be the tar
            tempFile = File.createTempFile("localdump", ".tar.gz.tmp");

            Integer result = UvmContextFactory.context().execManager().execResult(BACKUP_SCRIPT + " -o " + tempFile.getAbsolutePath() +" -v");

            if(result != 0) {
                throw new IOException("Unable to create local backup to \"" + tempFile.getAbsolutePath() + "\".  Process details " + result);
            }

            return tempFile;
        }
        catch(IOException ex) {
            //Don't forget to delete the temp file
            if ( tempFile != null ) IOUtil.delete(tempFile);
            logger.error("Exception creating backup for transfer to client", ex);
            throw new RuntimeException("Unable to create backup file");
        }
    }

    private ExecManagerResult restoreBackup( byte[] backupFileBytes, String maintainRegex ) throws IOException, IllegalArgumentException
    {
        File restoreFile = new File(System.getProperty("uvm.conf.dir") + "/restore.tar.gz");
        ExecManagerResult checkResult = null;
        ExecManagerResult result = null;

        logger.info("restoreBackup( " + restoreFile + " , \"" + maintainRegex + "\" );");
        
        try {
            //Copy the bytes to a temp file
            IOUtil.bytesToFile(backupFileBytes, restoreFile);
        }
        catch(IOException ex) {
            //Delete our temp file
            IOUtil.delete(restoreFile);
            logger.error("Exception performing restore", ex);
            throw ex;
        }

        logger.info("Restore Backup: " + restoreFile);
        
        // just check the backup file
        logger.info("Restore Backup: check file " + restoreFile);
        checkResult = UvmContextFactory.context().execManager().exec(RESTORE_SCRIPT + " -i " + restoreFile.getAbsolutePath() + " -v -c");

        // if the backup file is not legitimate then just return the results
        if (checkResult.getResult() != 0) {
            return checkResult;
        }

        try {
            if ( UvmContextFactory.context().aptManager().getUpgradeStatus(true).getUpgradesAvailable() ) {
                return new ExecManagerResult( 0, i18nUtil.tr("Upgrades are available. Please upgrade before restoring."));
            }
        } catch (Exception e) {
            logger.warn("Unable to check upgrade status",e);
        }
        
        // get the list of required files
        logger.info("Restore Backup: check packages " + restoreFile);
        result = UvmContextFactory.context().execManager().exec(RESTORE_SCRIPT + " -i " + restoreFile.getAbsolutePath() + " -f");

        // if the backup file is not legitimate then just return the results
        if (result.getResult() != 0) {
            return result;
        }

        // install all the needed packages
        String[] packages = result.getOutput().split("[\\r\\n]+");
        if ( packages != null ) {
            String pkgsStr = "";
            for ( String pkg : packages ) {
                // if the needed package is installed, skip it
                if ( UvmContextFactory.context().aptManager().isInstalled( pkg ) )
                    continue;
                // also ignore missing packages in development environment
                if ( UvmContextFactory.context().isDevel() )
                    continue;

                if (! "".equals(pkgsStr))
                    pkgsStr += ",";

                pkgsStr += pkg;
            }

            // if there are missing packages
            if (! "".equals(pkgsStr))
                return new ExecManagerResult( -1, "NEED_TO_INSTALL:" + pkgsStr);
        }
            
        // run same command with nohup and without -c check-only flag
        logger.info("Restore Backup: launching restore " + restoreFile);
        UvmContextFactory.context().execManager().exec("nohup " + RESTORE_SCRIPT + " -i " + restoreFile.getAbsolutePath() + " -v -m \"" + maintainRegex + "\" >/var/log/uvm/restore.log 2>&1 &");

        logger.info("Restore Backup: returning");
        return new ExecManagerResult( 0, i18nUtil.tr("The restore procedure is running. This may take several minutes. The server may be unavailable during this time. Once the process is complete you will be able to log in again."));
    }

    private class RestoreUploadHandler implements UploadHandler
    {
        @Override
        public String getName()
        {
            return "restore";
        }

        @Override
        public ExecManagerResult handleFile(FileItem fileItem, String argument) throws Exception
        {
            return restoreBackup( fileItem.get(), argument );
        }
        
    }

    private class BackupDownloadHandler implements DownloadHandler
    {
        private static final String DATE_FORMAT_NOW = "yyyy-MM-dd_HH-mm-ss";
        private static final String ATTR_BACKUP_DATA = "backupData";

        @Override
        public String getName()
        {
            return "backup";
        }
        
        @Override
        public void serveDownload( HttpServletRequest req, HttpServletResponse resp )
        {
            String oemName = UvmContextFactory.context().oemManager().getOemName();
            String version = UvmContextFactory.context().version().replace(".","_");
            String hostName = UvmContextFactory.context().networkManager().getNetworkSettings().getHostName().replace(".","_");
            String dateStr = (new SimpleDateFormat(DATE_FORMAT_NOW)).format((Calendar.getInstance()).getTime());
            String filename = oemName + "-" + version + "-" + "backup" + "-" + hostName + "-" + dateStr + ".backup";

            File backupFile = createBackup();
            
            // Set the headers.
            resp.setContentType("application/x-download");
            resp.setHeader("Content-Disposition", "attachment; filename=" + filename);

            // Send to client
            try {
                byte[] buffer = new byte[1024];
                int read;
                FileInputStream fis = new FileInputStream(backupFile);
                OutputStream out = resp.getOutputStream();
                
                while ( ( read = fis.read( buffer ) ) > 0 ) {
                    out.write( buffer, 0, read);
                }

                fis.close();
                out.flush();
                out.close();
            } catch (Exception e) {
                logger.warn("Failed to write backup data",e);
            }

            backupFile.delete();
        }
    }

}
