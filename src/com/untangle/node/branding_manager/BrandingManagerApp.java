/**
 * $Id$
 */
package com.untangle.node.branding_manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.apache.commons.fileupload.FileItem;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.node.License;
import com.untangle.uvm.servlet.UploadHandler;
import com.untangle.uvm.SettingsManager;

public class BrandingManagerApp extends NodeBase implements com.untangle.uvm.BrandingManager
{
    private static final File DEFAULT_LOGO = new File("/var/www/images/Logo150x96.png");;
    private static final File BRANDING_LOGO = new File("/var/www/images/BrandingLogo.png");
    private static final String BRANDING_LOGO_WEB_PATH = "images/BrandingLogo.png";

    private final Logger logger = Logger.getLogger(getClass());
    private final PipelineConnector[] connectors = new PipelineConnector[] {};

    private BrandingManagerSettings settings = null;

    public BrandingManagerApp( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );
    }

    private void readNodeSettings()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/untangle-node-branding-manager/settings_" + nodeID + ".js";
        BrandingManagerSettings readSettings = null;

        logger.info("Loading settings from " + settingsFile);

        try {
            readSettings =  settingsManager.load( BrandingManagerSettings.class, settingsFile);
        } catch (Exception exn) {
            logger.error("Could not read node settings", exn);
        }

        try {
            if (readSettings == null) {
                logger.warn("No settings found... initializing with defaults");
                initializeSettings();
            }
            else {
                this.settings = readSettings;
                setFileLogo(settings.binary_getLogo());                
            }
        }
        catch (Exception exn) {
            logger.error("Could not apply node settings", exn);
        }
    }

    private void writeNodeSettings(BrandingManagerSettings argSettings)
    {
    }

    @Override
    protected void preStart()
    {
        /**
         * Overwrite the logo just in case it has changed on disk
         */
        setFileLogo(settings.binary_getLogo());
    }

    @Override
    protected void postInit()
    {
        readNodeSettings();
        setFileLogo(settings.binary_getLogo());
        UvmContextFactory.context().servletFileManager().registerUploadHandler( new LogoUploadHandler(this) );
    }

    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    public BrandingManagerSettings getSettings()
    {
        /**
         * Don't send the logo - that is handled by the upload manager
         */
        BrandingManagerSettings copy = new BrandingManagerSettings(this.settings);
        boolean defaultLogo = copy.getDefaultLogo();
        copy.binary_setLogo(null);
        copy.setDefaultLogo(defaultLogo);

        return copy;
    }

    public void setSettings(final BrandingManagerSettings newSettings)
    {
        /**
         * getSettings always returns null logo
         * since setSettings will be called using a modified version of
         * what getSettings returns we should restore current logo
         * before saving if getDefaultLogo is still false and logo is null
         */
        if (!newSettings.getDefaultLogo() && newSettings.binary_getLogo() == null)
            newSettings.binary_setLogo(this.settings.binary_getLogo());

        logger.debug("setSettings(): " + newSettings.getCompanyName());

        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/untangle-node-branding-manager/settings_" + nodeID +".js";

        try {
            settingsManager.save( settingsFile, newSettings );
        } catch (Exception exn) {
            logger.error("Could not save branding settings", exn);
            return;
        }
        
        this.settings = newSettings;

        setFileLogo(settings.binary_getLogo());
    }

    @Override
    public void initializeSettings()
    {
        BrandingManagerSettings settings = new BrandingManagerSettings();
        logger.info("Initializing Settings...");

        /**
         * If OEM, initialize settings differently
         */
        String oemName = UvmContextFactory.context().oemManager().getOemName();
        if (oemName == null || oemName == "Untangle") {
            settings.setCompanyName("Untangle");
            settings.setCompanyUrl("http://untangle.com/");
        } else {
            settings.setCompanyName("MyCompany");
            settings.setCompanyUrl("http://mycompany.com/");
        }

        settings.setContactName("your network administrator");
        settings.setContactEmail(null);

        setSettings(settings);
    }

    @Override
    public String getContactHtml()
    {
        return settings.grabContactHtml();
    }

    @Override
    public String getContactEmail()
    {
        return settings.getContactEmail();
    }

    @Override
    public String getContactName()
    {
        return settings.getContactName();
    }

    @Override
    public String getCompanyUrl()
    {
        return settings.getCompanyUrl();
    }

    @Override
    public String getCompanyName()
    {
        return settings.getCompanyName();
    }

    public void setLogo(byte[] logo)
    {
        if ( ! isLicenseValid() ) {
            return;
        }

        BrandingManagerSettings settings = this.getSettings();
        settings.binary_setLogo(logo);
        settings.setDefaultLogo(false);
        this.setSettings(settings);
    }

    private void setFileLogo(byte[] logo)
    {
        /* if license is invalid - revert to default logo */
        if ( ! isLicenseValid() )
            logo = null;

        logger.debug("Writing logo file: " + BRANDING_LOGO);

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

    private boolean isLicenseValid()
    {
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.BRANDING_MANAGER))
            return true;
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.BRANDING_MANAGER_OLDNAME))
            return true;
        return false;
    }

    private class LogoUploadHandler implements UploadHandler
    {
        private BrandingManagerApp node;

        LogoUploadHandler( BrandingManagerApp node )
        {
            this.node = node;
        }

        @Override
        public String getName()
        {
            return "logo";
        }

        @Override
        public String handleFile(FileItem fileItem, String argument) throws Exception
        {
            if (fileItem.getName().toLowerCase().endsWith(".gif") ||
                fileItem.getName().toLowerCase().endsWith(".png") ||
                fileItem.getName().toLowerCase().endsWith(".jpg") ||
                fileItem.getName().toLowerCase().endsWith(".jpeg") ) {
                byte[] logo=fileItem.get();

                /* Use the context in order to properly handler premium vs normal. */
                node.setLogo(logo);
            } else {
                throw new Exception("Branding logo must be GIF, PNG, or JPG");
            }
            return "Uploaded new branding logo";
        }
    }
}
