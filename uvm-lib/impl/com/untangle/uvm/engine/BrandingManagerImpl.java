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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.SQLException;

import com.untangle.uvm.BrandingBaseSettings;
import com.untangle.uvm.BrandingSettings;
import com.untangle.uvm.LocalBrandingManager;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.util.DeletingDataSaver;
import com.untangle.uvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 * Implementation of LocalBrandingManager.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class BrandingManagerImpl implements LocalBrandingManager
{
    private static final File DEFAULT_LOGO;
    private static final File BRANDING_LOGO;
    private static final String BRANDING_LOGO_WEB_PATH;
    private static final File BRANDING_PROPS;

    private final Logger logger = Logger.getLogger(getClass());

    private BrandingSettings settings;

    BrandingManagerImpl()
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
        setBrandingProperties(settings);
        setFileLogo(settings.getLogo());
    }

    // public methods ---------------------------------------------------------

    public BrandingSettings getBrandingSettings()
    {
        return settings;
    }

    public void setBrandingSettings(BrandingSettings settings)
    {
        /* delete whatever is in the db, and just make a fresh settings object */
        BrandingSettings copy = new BrandingSettings();
        settings.copy(copy);
        saveSettings(copy);
        this.settings = copy;
        setBrandingProperties(this.settings);
        setFileLogo(this.settings.getLogo());
    }

    public BrandingBaseSettings getBaseSettings() {
    	return settings.getBaseSettings();
    }
    
    public void setBaseSettings(BrandingBaseSettings bs) {
        /* delete whatever is in the db, and just make a fresh settings object */
        BrandingSettings copy = new BrandingSettings();
        bs.copy(copy.getBaseSettings());
        if (bs.isDefaultLogo()) {
        	copy.setLogo(null);
        }
        saveSettings(copy);
        this.settings = copy;
        setBrandingProperties(this.settings);
        setFileLogo(this.settings.getLogo());
    }
    
    public void setLogo(byte[] logo) {
        /* delete whatever is in the db, and just make a fresh settings object */
        BrandingSettings copy = new BrandingSettings();
        settings.copy(copy);
        copy.setLogo(logo);
        saveSettings(copy);
        this.settings = copy;
        setBrandingProperties(this.settings);
        setFileLogo(this.settings.getLogo());
    }
    
    private void setFileLogo(byte[] logo)
    {
        FileInputStream fis = null;
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(BRANDING_LOGO);

            if (null == logo) {
                byte[] buf = new byte[1024];
                fis = new FileInputStream(DEFAULT_LOGO);
                int c;
                while (0 <= (c = fis.read(buf))) {
                    fos.write(buf, 0, c);
                }
            } else {
                fos.write(logo);
            }
        } catch (IOException exn) {
            logger.warn("could not change icon", exn);
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException exn) {
                    logger.warn("could not close", exn);
                }
            }

            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException exn) {
                    logger.warn("could not close", exn);
                }
            }
        }
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

    private void setBrandingProperties(BrandingSettings settings)
    {
        PrintWriter pr = null;
        try {
            OutputStream os = new FileOutputStream(BRANDING_PROPS);
            pr = new PrintWriter(new OutputStreamWriter(os));
            pr.print("uvm.branding.companyName=");
            pr.println(settings.getBaseSettings().getCompanyName());
            pr.print("uvm.branding.companyUrl=");
            pr.println(settings.getBaseSettings().getCompanyUrl());
            pr.print("uvm.branding.contactName=");
            pr.println(settings.getBaseSettings().getContactName());
            if (null != settings.getBaseSettings().getContactEmail()) {
                pr.print("uvm.branding.contactEmail=");
                pr.println(settings.getBaseSettings().getContactEmail());
            }
        } catch (IOException exn) {
            logger.warn("could not save branding properties", exn);
        } finally {
            if (null != pr) {
                pr.close();
            }
        }
    }

    private void saveSettings(BrandingSettings settings)
    {
        DeletingDataSaver<BrandingSettings> saver = 
            new DeletingDataSaver<BrandingSettings>(LocalUvmContextFactory.context(),"BrandingSettings");
        this.settings = saver.saveData(settings);
    }

    static {
        String wd = System.getProperty("bunnicula.web.dir");
        File id = new File(wd, "ROOT/images");
        DEFAULT_LOGO = new File(id, "Logo150x96.gif");
        BRANDING_LOGO = new File(id, "BrandingLogo.gif");
        BRANDING_LOGO_WEB_PATH = "images/BrandingLogo.gif";
        wd = System.getProperty("bunnicula.conf.dir");
        id = new File(wd);
        BRANDING_PROPS = new File(id, "branding.properties");
    }
}
