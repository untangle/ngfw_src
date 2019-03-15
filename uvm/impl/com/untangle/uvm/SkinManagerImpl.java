/**
 * $Id$
 */

package com.untangle.uvm;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;

import com.untangle.uvm.servlet.UploadHandler;

/**
 * Implementation of SkinManager.
 */
public class SkinManagerImpl implements SkinManager
{
    private static final String SKINS_DIR = System.getProperty("uvm.skins.dir");;
    private static final String DEFAULT_ADMIN_SKIN = "simple-gray";

    private final Logger logger = Logger.getLogger(getClass());

    private SkinSettings settings;
    private SkinInfo skinInfo;

    /**
     * Constructor
     */
    public SkinManagerImpl()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        SkinSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-vm/" + "skin.js";

        try {
            readSettings = settingsManager.load(SkinSettings.class, settingsFileName);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:", e);
        }

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");

            SkinSettings skinSettings = new SkinSettings();
            skinSettings.setSkinName(DEFAULT_ADMIN_SKIN);

            this.setSettings(skinSettings);
        } else {
            this.settings = readSettings;

            /**
             * 13.0 conversion If the configured skin is anything other than one
             * of the supported skins Change to simple-gray
             */
            String skinName = this.settings.getSkinName();
            if (skinName == null) {
                this.settings.setSkinName("simple-gray");
                skinName = "simple-gray";
                this.setSettings(this.settings);
            } else if (!skinName.equals("simple-gray") && !skinName.equals("modern-rack")) {
                this.settings.setSkinName("simple-gray");
                this.setSettings(this.settings);
            }

            logger.debug("Loading Settings: " + this.settings.toJSONString());
        }

        /**
         * If the skin is out of date, revert to default
         */
        this.skinInfo = getSkinInfo(SKINS_DIR + File.separator + this.settings.getSkinName() + File.separator + "skinInfo.json");
        if (this.skinInfo == null || this.skinInfo.isAdminSkinOutOfDate()) {
            logger.warn("Unable to find skin \"" + this.settings.getSkinName() + "\" - reverting to default skin: " + DEFAULT_ADMIN_SKIN);
            this.settings.setSkinName(DEFAULT_ADMIN_SKIN);
            this.setSettings(this.settings);
            this.skinInfo = getSkinInfo(SKINS_DIR + File.separator + this.settings.getSkinName() + File.separator + "skinInfo.json");
        }

        this.reconfigure();
    }

    /**
     * Get the settings
     * 
     * @return The settings
     */

    public SkinSettings getSettings()
    {
        return settings;
    }

    /**
     * Set the settings
     * 
     * @param newSettings
     *        The new settings
     */
    public void setSettings(SkinSettings newSettings)
    {
        this._setSettings(newSettings);
    }

    /**
     * Get the skin info
     * 
     * @return The skin info
     */
    public SkinInfo getSkinInfo()
    {
        return skinInfo;
    }

    /**
     * Skin upload handler
     * 
     * @param item
     *        The uploaded file
     * @throws UvmException
     */
    public void uploadSkin(FileItem item) throws UvmException
    {
        try {
            BufferedOutputStream dest = null;
            FileOutputStream fos = null;
            ZipEntry entry = null;
            File defaultSkinDir = new File(SKINS_DIR + File.separator + DEFAULT_ADMIN_SKIN);
            File skinDir = new File(SKINS_DIR);
            List<File> processedSkinFolders = new ArrayList<File>();

            //validate skin
            if (!item.getName().endsWith(".zip")) {
                throw new UvmException("Invalid Skin");
            }

            // Open the ZIP file
            InputStream uploadedStream = item.getInputStream();
            ZipInputStream zis = new ZipInputStream(uploadedStream);
            while ((entry = zis.getNextEntry()) != null) {
                //validate default skin
                String tokens[] = entry.getName().split(File.separator);
                if (tokens.length >= 1) {
                    File dir = new File(SKINS_DIR + File.separator + tokens[0]);
                    if (dir.equals(defaultSkinDir)) {
                        throw new UvmException("The default skin can not be overwritten");
                    }
                }

                if (entry.isDirectory()) {
                    File dir = new File(SKINS_DIR + File.separator + entry.getName());
                    processSkinFolder(dir, processedSkinFolders);
                } else {
                    File file = new File(SKINS_DIR + File.separator + entry.getName());
                    File parentDir = file.getParentFile();
                    if (parentDir.equals(skinDir)) {
                        // invalid entry; skip it
                        continue;
                    } else {
                        processSkinFolder(parentDir, processedSkinFolders);
                    }

                    int count;
                    int bufferSize = 2048;
                    byte data[] = new byte[bufferSize];
                    // write the files to the disk
                    fos = null;
                    dest = null;
                    try {
                        fos = new FileOutputStream(SKINS_DIR + File.separator + entry.getName());
                        dest = new BufferedOutputStream(fos, bufferSize);
                        while ((count = zis.read(data, 0, bufferSize)) != -1) {
                            dest.write(data, 0, count);
                        }
                        dest.flush();
                        if (entry.getName().contains("skinInfo.json")) {
                            String skinInfoFile = SKINS_DIR + File.separator + entry.getName();
                            SkinInfo skinInfoTmp = null;
                            try {
                                skinInfoTmp = UvmContextFactory.context().settingsManager().load(SkinInfo.class, skinInfoFile);
                            } catch (SettingsManager.SettingsException e) {
                                logger.warn("Failed to load skin:", e);
                            }
                            if (skinInfoTmp == null || skinInfoTmp.isAdminSkinOutOfDate()) {
                                logger.error("Upload Skin Failed, Out of Date");
                                throw new UvmException("Upload Skin Failed, Out of Date");
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to copy skins", e);
                    } finally {
                        if (fos != null) {
                            try{
                                fos.close();
                            }catch(Exception e){
                                logger.warn(e);
                            }
                        }
                        if (dest != null) {
                            try{
                                dest.close();
                            }catch(Exception e){
                                logger.warn(e);
                            }
                        }
                    }
                }
            }
            zis.close();
            uploadedStream.close();
        } catch (IOException e) {
            logger.error(e);
            throw new UvmException("Upload Skin Failed");
        }
    }

    /**
     * Get the list of available skins
     * 
     * @return The list of skins
     */
    public List<SkinInfo> getSkinsList()
    {

        List<SkinInfo> skins = new ArrayList<SkinInfo>();
        File dir = new File(SKINS_DIR);

        File[] children = dir.listFiles();
        if (children == null) {
            logger.warn("Skin dir \"" + SKINS_DIR + "\" does not exist");
        } else {
            for (int i = 0; i < children.length; i++) {
                File file = children[i];
                if (file.isDirectory() && !file.getName().startsWith(".")) {
                    File[] skinFiles = file.listFiles(new FilenameFilter()
                    {
                        /**
                         * Accept function for directory search
                         * 
                         * @param dir
                         *        The directory
                         * @param name
                         *        The file name
                         * @return True to accept, false to reject
                         */
                        public boolean accept(File dir, String name)
                        {
                            return name.equals("skinInfo.json");
                        }
                    });
                    if (skinFiles.length < 1) {
                        logger.warn("Skin folder \"" + file.getName() + "\" does not have skin info file - skinInfo.json");
                    } else {
                        SkinInfo skinInfoTmp;
                        skinInfoTmp = getSkinInfo(skinFiles[0].getPath());
                        if (skinInfoTmp != null) {
                            skins.add(skinInfoTmp);
                        }
                    }
                }
            }
        }
        Collections.sort(skins, new Comparator<SkinInfo>()
        {
            /**
             * Compare for skink sorting
             * 
             * @param o1
             *        Object one
             * @param o2
             *        Object two
             * @return Comparison result
             */
            public int compare(SkinInfo o1, SkinInfo o2)
            {
                if (o1 != null && o2 != null && o1.getDisplayName() != null && o2.getDisplayName() != null) return o1.getDisplayName().compareTo(o2.getDisplayName());
                return 0;
            }
        });

        return skins;
    }

    /**
     * Get info for a given skin file name
     * 
     * @param skinInfoFileName
     *        The file name
     * @return The skin info
     */
    public SkinInfo getSkinInfo(String skinInfoFileName)
    {
        SkinInfo skinInfoTmp;
        try {
            skinInfoTmp = UvmContextFactory.context().settingsManager().load(SkinInfo.class, skinInfoFileName);
            return skinInfoTmp;
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load skin:", e);
        }

        return null;
    }

    /**
     * Set, save, and activate new settings
     * 
     * @param newSettings
     *        The new settings
     */
    private void _setSettings(SkinSettings newSettings)
    {
        /**
         * Save the settings
         */
        try {
            UvmContextFactory.context().settingsManager().save(System.getProperty("uvm.settings.dir") + "/" + "untangle-vm/" + "skin.js", newSettings);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.", e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {
            logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));
        } catch (Exception e) {
        }

        this.reconfigure();
    }

    /**
     * Reconfigure when new settings are applied
     */
    private void reconfigure()
    {
        /* Register a handler to upload skins */
        UvmContextImpl.context().servletFileManager().registerUploadHandler(new SkinUploadHandler());

        this.skinInfo = getSkinInfo(SKINS_DIR + File.separator + this.settings.getSkinName() + File.separator + "skinInfo.json");
    }

    /**
     * Process the skin folder
     * 
     * @param dir
     *        The directory
     * @param processedSkinFolders
     *        The list of processed folders
     * @throws IOException
     * @throws UvmException
     */
    private void processSkinFolder(File dir, List<File> processedSkinFolders) throws IOException, UvmException
    {
        if (processedSkinFolders.contains(dir)) {
            return;
        }
        if (dir.exists()) {
            // this is somewhat dangerous, so only do it if dir looks non empty
            if (dir.getAbsolutePath().length() > 3) UvmContextFactory.context().execManager().exec("rm -rf " + dir.getAbsolutePath() + "/*");
        } else {
            if (!dir.mkdirs()) {
                logger.error("Error creating skin folder: " + dir);
                throw new UvmException("Error creating skin folder");
            }
        }
        processedSkinFolders.add(dir);
    }

    /**
     * Upload handler for skin files
     */
    private class SkinUploadHandler implements UploadHandler
    {
        /**
         * Get the handler name
         * 
         * @return The handler name
         */
        @Override
        public String getName()
        {
            return "skin";
        }

        /**
         * Called to handle an uploaded file
         * 
         * @param fileItem
         *        The uploaded file
         * @param argument
         *        The uploaded argument
         * @return Upload result
         * @throws Exception
         */
        @Override
        public String handleFile(FileItem fileItem, String argument) throws Exception
        {
            uploadSkin(fileItem);
            return "Successfully updated a skin";
        }
    }
}
