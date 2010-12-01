/*
 * $HeadURL: svn://chef/branch/prod/web-ui/work/src/uvm-lib/impl/com/untangle/uvm/engine/RemoteSkinManagerImpl.java $
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.thoughtworks.xstream.XStream;
import com.untangle.uvm.RemoteSkinManager;
import com.untangle.uvm.SkinInfo;
import com.untangle.uvm.SkinSettings;
import com.untangle.uvm.UvmException;
import com.untangle.uvm.license.License;
import com.untangle.uvm.servlet.UploadHandler;
import com.untangle.uvm.util.DeletingDataSaver;
import com.untangle.uvm.util.JsonClient;
import com.untangle.uvm.util.TransactionWork;

/**
 * Implementation of RemoteSkinManager.
 *
 * @author <a href="mailto:cmatei@untangle.com">Catalin Matei</a>
 * @version 1.0
 */
class RemoteSkinManagerImpl implements RemoteSkinManager
{
    private static final String SKINS_DIR;
    private static final String DEFAULT_SKIN = "default";
    private static final String DEFAULT_ADMIN_SKIN = DEFAULT_SKIN;
    private static final String DEFAULT_USER_SKIN = DEFAULT_SKIN;
    private static final int BUFFER = 2048; 

    private final Logger logger = Logger.getLogger(getClass());

    private final UvmContextImpl uvmContext;
    private SkinSettings settings;

    RemoteSkinManagerImpl(UvmContextImpl uvmContext)
    {
    	this.uvmContext = uvmContext;    	
    	
        TransactionWork<Object> tw = new TransactionWork<Object>()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from SkinSettings");
                    settings = (SkinSettings)q.uniqueResult();

                    if (null == settings) {
                	settings = new SkinSettings();
                	settings.setAdministrationClientSkin(DEFAULT_ADMIN_SKIN);
                	settings.setUserPagesSkin(DEFAULT_USER_SKIN);
                        s.save(settings);
                    }

		    // check version of skins and revert to default if it is not compatible
		    String userSkin = settings.getUserPagesSkin();
		    File userSkinXML = new File( SKINS_DIR + File.separator + userSkin + File.separator + "skin.xml" );
		    SkinInfo userSkinInfo = getSkinInfo( userSkinXML, false, true );
		    if ( userSkinInfo == null || userSkinInfo.isUserFacingSkinOutOfDate() ) {
			settings.setUserPagesSkin( DEFAULT_USER_SKIN );
                        settings.setOutOfDate( true );
		    }
		    
		    String adminSkin = settings.getAdministrationClientSkin();
		    File adminSkinXML = new File( SKINS_DIR + File.separator + adminSkin + File.separator + "skin.xml" );
		    SkinInfo adminSkinInfo = getSkinInfo( adminSkinXML, false, true );
		    if ( adminSkinInfo == null || adminSkinInfo.isAdminSkinOutOfDate() ) {
			settings.setAdministrationClientSkin( DEFAULT_ADMIN_SKIN );
                        settings.setOutOfDate( true );
		    }
        
                    return true;
                }
            };
        uvmContext.runTransaction(tw);

        /* Register a handler to upload skins */
        uvmContext.uploadManager().registerHandler(new SkinUploadHandler());
    }

    // public methods ---------------------------------------------------------

    public SkinSettings getSkinSettings()
    {
        return settings;
    }

    public void setSkinSettings(SkinSettings settings)
    {
        /* delete whatever is in the db, and just make a fresh settings object */
        SkinSettings copy = new SkinSettings();
        settings.copy(copy);

        boolean isValid = uvmContext.licenseManager().getLicense( License.BRANDING ).getValid();

        if ( isValid ) {
            String userSkin = this.settings.getUserPagesSkin();
            if ( userSkin == null || userSkin.length() == 0 ) userSkin = DEFAULT_USER_SKIN;
            settings.setUserPagesSkin( userSkin );
        }
        
        saveSettings(copy);
        this.settings = copy;

        try {
            /* This is asynchronous */
            JsonClient.getInstance().updateAlpacaSettings();
        } catch ( Exception e ) {
            logger.warn( "Unable to update alpaca settings." );
        }
    }
	
    public void uploadSkin(FileItem item) throws UvmException {
        try {
            BufferedOutputStream dest = null;
            ZipEntry entry = null;
            File defaultSkinDir = new File(SKINS_DIR + File.separator + DEFAULT_SKIN);
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
                    byte data[] = new byte[BUFFER];
                    // write the files to the disk
                    FileOutputStream fos = new FileOutputStream(SKINS_DIR + File.separator + entry.getName());
                    dest = new BufferedOutputStream(fos, BUFFER);
                    while ((count = zis.read(data, 0, BUFFER)) != -1) {
                        dest.write(data, 0, count);
                    }
                    dest.flush();
                    dest.close();
                    if (entry.getName().contains("skin.xml")) {
		       File skinXML = new File( SKINS_DIR + File.separator + entry.getName() );
		       SkinInfo skinInfo = getSkinInfo( skinXML, false, true );
		       if ( skinInfo == null || skinInfo.isUserFacingSkinOutOfDate() || skinInfo.isAdminSkinOutOfDate() ) {
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

    public List<SkinInfo> getSkinsList(boolean fetchAdminSkins, boolean fetchUserFacingSkins) {
    	
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
                                return name.equals("skin.xml");
                            }
                	});
                    if (skinFiles.length < 1) {
                    	logger.warn("Skin folder \""+file.getName()+"\" does not have skin info file - skin.xml");
                    } else {
                    	SkinInfo skinInfo;
			skinInfo = getSkinInfo(skinFiles[0], fetchAdminSkins, fetchUserFacingSkins);
			if (skinInfo != null) {
			    skins.add(skinInfo);
			}
                    }
                }
            }
        }    	
        return skins;    	
    }

    public SkinInfo getSkinInfo(File skinXML, boolean fetchAdminSkins, boolean fetchUserFacingSkins) {
	XStream xstream = new XStream();
	xstream.alias("skin", SkinInfo.class);
	SkinInfo skinInfo;
	try {
	    skinInfo = (SkinInfo)xstream.fromXML(new FileInputStream(skinXML));
	    if((fetchAdminSkins && skinInfo.isAdminSkin()  && !skinInfo.isAdminSkinOutOfDate()) ||
	       (fetchUserFacingSkins && skinInfo.isUserFacingSkin() && !skinInfo.isUserFacingSkinOutOfDate())) {
		return skinInfo;
	    } 
	} catch (FileNotFoundException e) {
	    logger.error("Error reading skin info from skin foder \"" + skinXML.getName() + "\"");
	}
	return null;
    }
    
    // private methods --------------------------------------------------------
    private void saveSettings(SkinSettings settings) {
        DeletingDataSaver<SkinSettings> saver = 
            new DeletingDataSaver<SkinSettings>(uvmContext,"SkinSettings");
        this.settings = saver.saveData(settings);
    }
    
    private void processSkinFolder(File dir, List<File> processedSkinFolders)
        throws IOException, UvmException {
        if (processedSkinFolders.contains(dir)){
            return;
        }
        if (dir.exists()) {
            FileUtils.cleanDirectory(dir);
        } else {
            if (!dir.mkdirs()) {
                logger.error("Error creating skin folder: " + dir );
                throw new UvmException("Error creating skin folder");
            }
        }
        processedSkinFolders.add(dir);
    }
        
    static {
        SKINS_DIR = System.getProperty("uvm.skins.dir");
    }
    
    private class SkinUploadHandler implements UploadHandler
    {
        @Override
        public String getName()
        {
            return "skin";
        }
        
        @Override
        public String handleFile(FileItem fileItem) throws Exception
        {
            uploadSkin(fileItem);
            return "Successfully updated a skin";
        }
    }
}
