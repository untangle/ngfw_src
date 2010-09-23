/*
 * $HeadURL: svn://chef/branch/prod/web-ui/work/src/uvm-lib/impl/com/untangle/uvm/engine/RemoteGridSettingsManagerImpl.java $
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

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import com.untangle.uvm.RemoteImportGridSettingsManager;
import com.untangle.uvm.UvmException;
import com.untangle.uvm.servlet.UploadHandler;

/**
 * Implementation of RemoteGridSettingsManager.
 *
 * @author <a href="mailto:vdumitrescu@untangle.com">Vlad Dumitrescu</a>
 * @version 1.0
 */
class RemoteImportGridSettingsManagerImpl implements RemoteImportGridSettingsManager
{

    private final Logger logger = Logger.getLogger(getClass());

    private JSONArray gridSettings;


    RemoteImportGridSettingsManagerImpl(UvmContextImpl uvmContext)
    {
        /* Register a handler to upload grid settings */
        uvmContext.uploadManager().registerHandler(new ImportSettingsUploadHandler());
    }

    public JSONArray getGridSettings() {
    	JSONArray gridSettingsClone=gridSettings;
    	gridSettings=null;
		return gridSettingsClone;
	}

	public String uploadGridSettings(FileItem item) throws UvmException {
		gridSettings=null;
		String fileString=item.getString();
    	try {
    		if(fileString != null && fileString.trim().length()>0) {
    			gridSettings = new JSONArray(fileString.trim());
    		} else {
    			throw new UvmException("Import Grid Settings Failed. Settings file is empty.");
    		}
		} catch (JSONException e) {
			logger.debug("Upload grid settings failed", e);
            throw new UvmException("Import Grid Settings Failed. Settings must be formatted as a JSON Array.");
		}
		return null;
    }

    
    private class ImportSettingsUploadHandler implements UploadHandler
    {
        @Override
        public String getName()
        {
            return "import_settings";
        }
        
        @Override
        public String handleFile(FileItem fileItem) throws Exception
        {
        	uploadGridSettings(fileItem);
        	return "Grid Settings Uploaded successfully";
        }
    }
}
