/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.Collection;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

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

    public SkinManagerImpl()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        SkinSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-vm/" + "skin.js";
        
        try {
            readSettings = settingsManager.load( SkinSettings.class, settingsFileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");

            SkinSettings skinSettings = new SkinSettings();
            skinSettings.setSkinName(DEFAULT_ADMIN_SKIN);

            this.setSettings(skinSettings);
        }
        else {
            this.settings = readSettings;

            /**
             * 13.0 conversion
             * If the configured skin is anything other than one of the supported skins
             * Change to simple-gray
             */
            String skinName = this.settings.getSkinName();
            if ( skinName == null ) {
                this.settings.setSkinName("simple-gray");
                skinName = "simple-gray";
                this.setSettings( this.settings );
            } else if ( !skinName.equals("simple-gray") &&
                        !skinName.equals("modern-rack") ) {
                this.settings.setSkinName("simple-gray");
                this.setSettings( this.settings );
            }

            logger.debug("Loading Settings: " + this.settings.toJSONString());
        }


        /**
         * If the skin is out of date, revert to default
         */
        this.skinInfo = getSkinInfo( SKINS_DIR + File.separator + this.settings.getSkinName() + File.separator + "skinInfo.js" );
        if ( this.skinInfo == null || this.skinInfo.isAdminSkinOutOfDate() ) {
            logger.warn("Unable to find skin \"" + this.settings.getSkinName() + "\" - reverting to default skin: " + DEFAULT_ADMIN_SKIN);
            this.settings.setSkinName( DEFAULT_ADMIN_SKIN );
            this.setSettings( this.settings );
            this.skinInfo = getSkinInfo( SKINS_DIR + File.separator + this.settings.getSkinName() + File.separator + "skinInfo.js" );
        }

        this.reconfigure();
    }

    // public methods ---------------------------------------------------------

    public SkinSettings getSettings()
    {
        return settings;
    }

    public void setSettings(SkinSettings newSettings)
    {
        this._setSettings( newSettings );
    }

    public SkinInfo getSkinInfo()
    {
        return skinInfo;
    }

    public void uploadSkin(FileItem item) throws UvmException
    {
        try {
            BufferedOutputStream dest = null;
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
                    FileOutputStream fos = new FileOutputStream(SKINS_DIR + File.separator + entry.getName());
                    dest = new BufferedOutputStream(fos, bufferSize);
                    while ((count = zis.read(data, 0, bufferSize)) != -1) {
                        dest.write(data, 0, count);
                    }
                    dest.flush();
                    dest.close();
                    if (entry.getName().contains("skinInfo.js")) {
                        String skinInfoFile = SKINS_DIR + File.separator + entry.getName();
                        SkinInfo skinInfoTmp = null;
                        try {
                            skinInfoTmp = UvmContextFactory.context().settingsManager().load( SkinInfo.class, skinInfoFile );
                        } catch (SettingsManager.SettingsException e) {
                            logger.warn("Failed to load skin:",e);
                        }
                        if ( skinInfoTmp == null || skinInfoTmp.isAdminSkinOutOfDate() ) {
                            logger.error("Upload Skin Failed, Out of Date");
                            throw new UvmException("Upload Skin Failed, Out of Date");
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

    public List<SkinInfo> getSkinsList( )
    {
        
        List<SkinInfo> skins = new ArrayList<SkinInfo>();
        File dir = new File(SKINS_DIR);
        
        File[] children = dir.listFiles();
        if (children == null) {
            logger.warn("Skin dir \""+SKINS_DIR+"\" does not exist");
        } else {
            for (int i=0; i<children.length; i++) {
                File file = children[i];
                if (file.isDirectory() && !file.getName().startsWith(".")) {
                    File[] skinFiles = file.listFiles(new FilenameFilter(){
                            public boolean accept(File dir, String name) {
                                return name.equals("skinInfo.js");
                            }
                        });
                    if (skinFiles.length < 1) {
                        logger.warn("Skin folder \""+file.getName()+"\" does not have skin info file - skinInfo.js");
                    } else {
                        SkinInfo skinInfoTmp;
                        skinInfoTmp = getSkinInfo( skinFiles[0].getPath() );
                        if (skinInfoTmp != null) {
                            skins.add(skinInfoTmp);
                        }
                    }
                }
            }
        }
        Collections.sort( skins, new Comparator<SkinInfo>() { public int compare(SkinInfo o1, SkinInfo o2) {
            if ( o1 != null && o2 != null && o1.getDisplayName() != null && o2.getDisplayName() != null )
                return o1.getDisplayName().compareTo(o2.getDisplayName());
            return 0;
        } });
        
        return skins;
    }

    public SkinInfo getSkinInfo( String skinInfoFileName  )
    {
        SkinInfo skinInfoTmp;
        try {
            skinInfoTmp = UvmContextFactory.context().settingsManager().load( SkinInfo.class, skinInfoFileName );
            return skinInfoTmp;
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load skin:",e);
        }

        return null;
    }

    // private methods --------------------------------------------------------

    private void _setSettings( SkinSettings newSettings )
    {
        /**
         * Save the settings
         */
        try {
            UvmContextFactory.context().settingsManager().save( System.getProperty("uvm.settings.dir") + "/" + "untangle-vm/" + "skin.js", newSettings );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}

        this.reconfigure();
    }
    
    private void reconfigure() 
    {
        /* Register a handler to upload skins */
        UvmContextImpl.context().servletFileManager().registerUploadHandler( new SkinUploadHandler() );

        this.skinInfo = getSkinInfo( SKINS_DIR + File.separator + this.settings.getSkinName() + File.separator + "skinInfo.js" );
    }
    
    private void processSkinFolder(File dir, List<File> processedSkinFolders)
        throws IOException, UvmException
    {
        if (processedSkinFolders.contains(dir)){
            return;
        }
        if ( dir.exists() ) {
            // this is somewhat dangerous, so only do it if dir looks non empty
            if ( dir.getAbsolutePath().length() > 3 )
                UvmContextFactory.context().execManager().exec("rm -rf " + dir.getAbsolutePath() + "/*");
        } else {
            if (!dir.mkdirs()) {
                logger.error("Error creating skin folder: " + dir );
                throw new UvmException("Error creating skin folder");
            }
        }
        processedSkinFolders.add(dir);
    }
        
    private class SkinUploadHandler implements UploadHandler
    {
        @Override
        public String getName()
        {
            return "skin";
        }
        
        @Override
        public String handleFile(FileItem fileItem, String argument) throws Exception
        {
            uploadSkin(fileItem);
            return "Successfully updated a skin";
        }
    }
}
