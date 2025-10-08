/**
 * $Id$
 */

package com.untangle.uvm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Calendar;
import java.text.SimpleDateFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.util.IOUtil;
import com.untangle.uvm.servlet.UploadHandler;
import com.untangle.uvm.servlet.DownloadHandler;

import static com.untangle.uvm.util.Constants.COMMA;
import static com.untangle.uvm.util.Constants.DOT;
import static com.untangle.uvm.util.Constants.SPACE;
import static com.untangle.uvm.util.Constants.UNDERSCORE;

/**
 * Helper class to do backup/restore
 */
public class BackupManagerImpl implements BackupManager
{
    private static final String DATE_FORMAT_NOW = "yyyy_MM_dd_HH_mm";

    private static final String BACKUP_SCRIPT = System.getProperty("uvm.home") + "/bin/ut-backup.sh";
    private static final String RESTORE_SCRIPT = System.getProperty("uvm.home") + "/bin/ut-restore.sh";

    private final Logger logger = LogManager.getLogger(BackupManagerImpl.class);

    private I18nUtil i18nUtil;

    /**
     * Constructor
     */
    protected BackupManagerImpl()
    {
        UvmContextFactory.context().servletFileManager().registerUploadHandler(new RestoreUploadHandler());
        UvmContextFactory.context().servletFileManager().registerDownloadHandler(new BackupDownloadHandler());

        Map<String, String> i18nMap = UvmContextFactory.context().languageManager().getTranslations("untangle");
        this.i18nUtil = new I18nUtil(i18nMap);
    }

    /**
     * Create a backup file
     * 
     * @return The File where the backup was stored
     */
    public File createBackup()
    {
        File tempFile = null;

        try {
            //Create the temp file which will be the tar
            tempFile = new File("/tmp/" + createBackupFileName() + ".tar.gz");

            Integer result = UvmContextFactory.context().execManager().execResult(BACKUP_SCRIPT + " -o " + tempFile.getAbsolutePath() + " -v");

            if (result != 0) {
                throw new IOException("Unable to create local backup to \"" + tempFile.getAbsolutePath() + "\".  Process details " + result);
            }

            return tempFile;
        } catch (IOException ex) {
            //Don't forget to delete the temp file
            if (tempFile != null) IOUtil.delete(tempFile);
            logger.error("Exception creating backup for transfer to client", ex);
            throw new RuntimeException("Unable to create backup file");
        }
    }

    /**
     * Restore a backup from a File object
     *
     * @param restoreFile
     *        The File containing the backup to restore
     * @param maintainRegex
     *        Regular expression
     * @return Null for success or an error String
     */
    public String restoreBackup(File restoreFile, String maintainRegex)
    {
        FileInputStream fileInputStream = null;
        byte[] fileData = new byte[(int)restoreFile.length()];

        // read the raw backup data into a byte array
        try {
            fileInputStream = new FileInputStream(restoreFile);
            fileInputStream.read(fileData);
        } catch (Exception exn) {
            return exn.getMessage();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    logger.error("Failed to close input stream", e);
                }
            }
        }

        try {
            restoreBackup(fileData, maintainRegex);
        } catch (Exception exn) {
            return exn.getMessage();
        }

        return null;
    }

    /**
     * Restore a backup file from raw backup data
     * 
     * @param backupFileBytes
     *        The raw backup data
     * @param maintainRegex
     *        Regular expression
     * @return The restore result
     * @throws IOException
     * @throws IllegalArgumentException
     */
    private ExecManagerResult restoreBackup(byte[] backupFileBytes, String maintainRegex) throws IOException, IllegalArgumentException
    {
        File restoreFile = new File(System.getProperty("uvm.conf.dir") + "/restore.tar.gz");
        ExecManagerResult checkResult = null;

        logger.info("restoreBackup( {} \"{}\"", restoreFile, maintainRegex);

        try {
            //Copy the bytes to a temp file
            IOUtil.bytesToFile(backupFileBytes, restoreFile);
        } catch (IOException ex) {
            //Delete our temp file
            IOUtil.delete(restoreFile);
            logger.error("Exception performing restore", ex);
            throw ex;
        }

        logger.info("Restore Backup: {}", restoreFile);

        // just check the backup file
        logger.info("Restore Backup: check file {}", restoreFile);
        checkResult = UvmContextFactory.context().execManager().exec(RESTORE_SCRIPT + " -i " + restoreFile.getAbsolutePath() + " -v -c");

        // if the backup file is not legitimate then just return the results
        if (checkResult.getResult() != 0) {
            return checkResult;
        }

        // run same command with nohup and without -c check-only flag
        logger.info("Restore Backup: launching restore {}", restoreFile);
        UvmContextFactory.context().execManager().execSafe(System.getProperty("uvm.bin.dir") + "/ut-backup-restore-helper.sh restore " + restoreFile.getAbsolutePath() + "  \"" + maintainRegex +"\"");

        logger.info("Restore Backup: returning");
        return new ExecManagerResult(0, i18nUtil.tr("The restore procedure is running. This may take several minutes. The server may be unavailable during this time. Once the process is complete you will be able to log in again."));
    }

    /**
     * Creates a backup file name using the current date and time
     * 
     * @return The backup file name
     */
    private static String createBackupFileName()
    {
        String version = UvmContextFactory.context().version().replace(DOT, UNDERSCORE);
        // NGFW-14925 replace space with empty string
        String hostName = UvmContextFactory.context().networkManager().getNetworkSettings().getHostName().replace(DOT, UNDERSCORE).replace(SPACE, StringUtils.EMPTY);
        String domainName = UvmContextFactory.context().networkManager().getNetworkSettings().getDomainName().replace(DOT, UNDERSCORE).replace(SPACE, StringUtils.EMPTY);
        String dateStr = (new SimpleDateFormat(DATE_FORMAT_NOW)).format((Calendar.getInstance()).getTime());
        return hostName + UNDERSCORE + domainName + "-" + "configuration_backup" + "_v" + version + "-" + dateStr + ".backup";
    }

    /**
     * Handler for backup files uploaded via the Admin interface
     */
    private class RestoreUploadHandler implements UploadHandler
    {

        private static final String EXCEPTIONS = "exceptions";
        private static final String NETWORK_REGEX = ".*/network.*";

        /**
         * Get the name of our handler
         * 
         * @return The name of our handler
         */
        @Override
        public String getName()
        {
            return "restore";
        }

        /**
         * Handle a file uploaded via the Admin interface
         * 
         * @param fileItem
         *        The file
         * @param argument
         *        Upload argument
         * @return The upload result
         * @throws Exception
         */
        @Override
        public ExecManagerResult handleFile(FileItem fileItem, String argument) throws Exception
        {
            return restoreBackup(fileItem.get(), argument);
        }

        /**
         * Handle a file bytes uploaded via the Admin interface
         * Transforms the arguments map and calls restoreBackup
         *
         * @param backupFileBytes
         *        The file
         * @param arguments
         *        Upload argument
         * @return The upload result
         * @throws Exception
         */
        @Override
        public ExecManagerResult handleV2File(byte[] backupFileBytes, Map<String, String> arguments) throws Exception
        {
            return restoreBackup(backupFileBytes, getRegExFromExceptions(arguments));
        }

        /**
         * Get regex from arguments map
         * @param arguments
         * @return
         */
        private String getRegExFromExceptions(Map<String, String> arguments) {
            if (arguments == null || !arguments.containsKey(EXCEPTIONS))
                return StringUtils.EMPTY;
            return getRegExFromExceptions(arguments.get(EXCEPTIONS));
        }

        /**
         * Generates a regular expression string based on the provided exception list.
         * <p>
         * This method splits the given comma-separated {@code exceptions} string and checks
         * whether it contains the keyword {@code "network"} (case-insensitive). If the keyword
         * is found, it returns a predefined {@code NETWORK_REGEX}. Otherwise, it returns an
         * empty string.
         * </p>
         *
         * @param exceptions a comma-separated list of exception keywords; may be {@code null} or empty
         * @return the {@code NETWORK_REGEX} string if "network" is present in the list,
         * otherwise an empty string
         */
        private String getRegExFromExceptions(String exceptions) {
            if (StringUtils.isNotEmpty(exceptions)) {
                boolean found = Arrays.stream(exceptions.split(COMMA))
                        .map(String::trim)
                        .anyMatch("network"::equalsIgnoreCase);
                return found ? NETWORK_REGEX : StringUtils.EMPTY;
            }
            return StringUtils.EMPTY;
        }

    }

    /**
     * Handler for downloading a backup file via the Admin interface
     */
    private class BackupDownloadHandler implements DownloadHandler
    {
        /**
         * Get the name of our handler
         * 
         * @return The name of our handler
         */
        @Override
        public String getName()
        {
            return "backup";
        }

        /**
         * Handles a backup download request from the Admin interface
         * 
         * @param req
         *        The web request
         * @param resp
         *        The web response
         */
        @Override
        public void serveDownload(HttpServletRequest req, HttpServletResponse resp)
        {
            File backupFile = createBackup();

            // Set the headers.
            resp.setContentType("application/x-download");
            resp.setHeader("Content-Disposition", "attachment; filename=" + createBackupFileName());

            // Send to client
            FileInputStream fis = null;
            try {
                byte[] buffer = new byte[1024];
                int read;
                fis = new FileInputStream(backupFile);
                OutputStream out = resp.getOutputStream();

                while ((read = fis.read(buffer)) > 0) {
                    out.write(buffer, 0, read);
                }

                out.flush();
                out.close();
            } catch (Exception e) {
                logger.warn("Failed to write backup data", e);
            } finally {
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch (IOException ex) {
                    logger.error("Unable to close file", ex);
                }
            }

            backupFile.delete();
        }
    }
}
