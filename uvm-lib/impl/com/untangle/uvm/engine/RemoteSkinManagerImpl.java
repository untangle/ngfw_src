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
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.RemoteSkinManager;
import com.untangle.uvm.SkinSettings;
import com.untangle.uvm.UvmException;
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
    private static final String DEFAULT_ADMIN_SKIN = "default";
    private static final String DEFAULT_USER_SKIN = "default";
	private static final int BUFFER = 2048; 

    private final Logger logger = Logger.getLogger(getClass());

    private final UvmContextImpl uvmContext;
    private SkinSettings settings;

    RemoteSkinManagerImpl(UvmContextImpl uvmContext) {
    	this.uvmContext = uvmContext;    	
    	
        TransactionWork tw = new TransactionWork()
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
                
                return true;
            }
        };
        uvmContext.runTransaction(tw);
    }

    // public methods ---------------------------------------------------------

	public SkinSettings getSkinSettings() {
		return settings;
	}

	public void setSkinSettings(final SkinSettings ss) {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.saveOrUpdate(ss);
                    return true;
                }
            };
        uvmContext.runTransaction(tw);

        this.settings = ss;
		
	}
	
    public void uploadSkin(FileItem item) throws UvmException {
	    try {
	        BufferedOutputStream dest = null;
			ZipEntry entry = null;
			
	        // Open the ZIP file
		    InputStream uploadedStream = item.getInputStream();
		    ZipInputStream zis = new ZipInputStream(uploadedStream);
			while ((entry = zis.getNextEntry()) != null) {
				if (entry.isDirectory()) {
					File dir = new File(SKINS_DIR + File.separator + entry.getName());
					boolean success = dir.mkdir();
					if (!success) {
						throw new UvmException("A skin with the same name already exists");
					}
				} else {
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
				}
			}
			zis.close();		    	
		    uploadedStream.close();
	    } catch (IOException e) {
	    	logger.error(e);
			throw new UvmException("Upload Skin Failed");
	    }
    }
	
    public List<String> getSkinsList() {
    	List<String> skins = new ArrayList<String>();
    	File dir = new File(SKINS_DIR);
    	
        File[] children = dir.listFiles();
        if (children == null) {
            // Either dir does not exist or is not a directory
        } else {
            for (int i=0; i<children.length; i++) {
                File file = children[i];
                if (file.isDirectory() && !file.getName().startsWith(".")) {
                	skins.add(file.getName());
                }
            }
        }    	
        return skins;    	
    }
    
    // private methods --------------------------------------------------------
    static {
        SKINS_DIR = System.getProperty("bunnicula.skins.dir");
    }
}
