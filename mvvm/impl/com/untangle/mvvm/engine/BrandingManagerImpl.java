/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: ConnectivityTester.java 8515 2007-01-03 00:13:24Z amread $
 */

package com.untangle.mvvm.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.SQLException;

import com.untangle.mvvm.BrandingManager;
import com.untangle.mvvm.BrandingSettings;
import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

class BrandingManagerImpl implements BrandingManager
{
    private static final File DEFAULT_LOGO;
    private static final File BRANDING_LOGO;
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
        MvvmContextFactory.context().runTransaction(tw);

        this.settings = tw.getResult();
        setBrandingProperties(settings);
        setLogo(settings.getLogo());
    }

    // public methods ---------------------------------------------------------

    public BrandingSettings getBrandingSettings()
    {
        return settings;
    }

    public void setBrandingSettings(BrandingSettings settings)
    {
        this.settings = settings;
        setBrandingProperties(settings);
        setLogo(settings.getLogo());
    }

    // private methods --------------------------------------------------------

    private void setBrandingProperties(BrandingSettings settings)
    {
        PrintWriter pr = null;
        try {
            OutputStream os = new FileOutputStream(BRANDING_PROPS);
            pr = new PrintWriter(new OutputStreamWriter(os));
            pr.print("mvvm.branding.companyName=");
            pr.println(settings.getCompanyName());
            pr.print("mvvm.branding.companyUrl=");
            pr.println(settings.getCompanyUrl());
            pr.print("mvvm.branding.contactName=");
            pr.println(settings.getContactName());
            if (null != settings.getContactEmail()) {
                pr.print("mvvm.branding.contactEmail=");
                pr.println(settings.getContactEmail());
            }
        } catch (IOException exn) {
            logger.warn("could not save branding properties", exn);
        } finally {
            if (null != pr) {
                pr.close();
            }
        }
    }

    private void setLogo(byte[] logo)
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

    static {
        String wd = System.getProperty("bunnicula.web.dir");
        File id = new File(wd, "ROOT/images");
        DEFAULT_LOGO = new File(id, "Logo32x32.gif");
        BRANDING_LOGO = new File(id, "BrandingLogo.gif");
        wd = System.getProperty("bunnicula.conf.dir");
        id = new File(wd);
        BRANDING_PROPS = new File(id, "branding.properties");
    }
}
