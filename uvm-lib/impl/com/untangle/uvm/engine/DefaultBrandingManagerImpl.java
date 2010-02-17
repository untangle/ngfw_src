/*
 * $HeadURL$
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
import java.sql.SQLException;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.BrandingBaseSettings;
import com.untangle.uvm.BrandingSettings;
import com.untangle.uvm.LocalBrandingManager;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.servlet.UploadHandler;
import com.untangle.uvm.util.TransactionWork;

/**
 * Implementation of LocalBrandingManager.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class DefaultBrandingManagerImpl implements LocalBrandingManager
{
    private static final File BRANDING_LOGO;
    private static final String BRANDING_LOGO_WEB_PATH;

    private final Logger logger = Logger.getLogger(getClass());
    private BrandingSettings settings;

    DefaultBrandingManagerImpl()
    {
        TransactionWork<BrandingSettings> tw = new TransactionWork<BrandingSettings>()
            {
                private BrandingSettings bs;

                public boolean doWork(Session s) throws SQLException
                {
                    Query q = s.createQuery("from BrandingSettings bs");
                    bs = (BrandingSettings)q.uniqueResult();
                    if (null == bs) {
                        bs = new BrandingSettings();
                        s.save(bs);
                    }

                    return true;
                }

                public BrandingSettings getResult() { return bs; }
            };
        LocalUvmContextFactory.context().runTransaction(tw);

        this.settings = tw.getResult();
        
        LocalUvmContextFactory.context().uploadManager().registerHandler(new LogoUploadHandler());
    }

    // public methods ---------------------------------------------------------

    public BrandingSettings getBrandingSettings()
    {
        return settings;
    }

    public void setBrandingSettings(BrandingSettings settings)
    {
        logger.debug( "Default branding manager doesn't save settings." );
    }

    public BrandingBaseSettings getBaseSettings() {
        return settings.getBaseSettings();
    }

    public void setBaseSettings(BrandingBaseSettings bs) {
        logger.debug( "Default branding manager doesn't save settings." );
    }

    public void setLogo(byte[] logo) {
        logger.debug( "Default branding manager doesn't save settings." );
    }

    public File getLogoFile()
    {
        return BRANDING_LOGO;
    }

    public String getLogoWebPath()
    {
        return BRANDING_LOGO_WEB_PATH;
    }

    // private methods --------------------------------------------------------

    static {
        File id = new File("/var/www/images");
        BRANDING_LOGO = new File(id, "BrandingLogo.gif");
        BRANDING_LOGO_WEB_PATH = "images/BrandingLogo.gif";
    }
    
    private class LogoUploadHandler implements UploadHandler
    {
        @Override
        public String getName()
        {
            return "logo";
        }
        
        @Override
        public String handleFile(FileItem fileItem) throws Exception
        {
            if (fileItem.getName().toLowerCase().endsWith(".gif")
                    || fileItem.getName().toLowerCase().endsWith(".png")
                    || fileItem.getName().toLowerCase().endsWith(".jpg")
                    || fileItem.getName().toLowerCase().endsWith(".jpeg") ) {
                      byte[] logo=fileItem.get();
  
                      /* Use the context in order to properly handler premium vs normal. */
                      LocalUvmContextFactory.context().brandingManager().setLogo(logo);
                  } else {
                      throw new Exception("Branding logo must be GIF, PNG, or JPG");
                  }
            return "Uploaded new branding logo";
        }
    }

}
