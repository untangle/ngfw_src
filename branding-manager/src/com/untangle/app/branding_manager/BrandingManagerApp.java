/**
 * $Id$
 */
package com.untangle.app.branding_manager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.commons.fileupload.FileItem;

import com.untangle.uvm.CertificateManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.servlet.UploadHandler;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.util.IOUtil;

/**
 * Branding manager
 */
public class BrandingManagerApp extends AppBase implements com.untangle.uvm.BrandingManager
{
    private static final File DEFAULT_LOGO = new File("/var/www/images/DefaultLogo.png");
    private static final File BRANDING_LOGO = new File("/var/www/images/BrandingLogo.png");

    private static final String DEFAULT_UNTANGLE_COMPANY_NAME = "Untangle";
    private static final String DEFAULT_UNTANGLE_URL = "http://untangle.com/";

    public enum FILE_PARSE_TYPE {
        QUOTED,
        NON_QUOTED
    }

    private static final String ROOT_CA_INSTALLER_DIRECTORY_NAME = System.getProperty("uvm.lib.dir") + "/branding-manager/root_certificate_installer";
    private static final HashMap<FILE_PARSE_TYPE, String> ROOT_CA_INSTALLER_PARSE_FILE_NAMES;

    public enum REGEX_TYPE {
        COMPANY_NAME,
        COMPANY_URL
    }

    static {
        ROOT_CA_INSTALLER_PARSE_FILE_NAMES = new HashMap<>();
        ROOT_CA_INSTALLER_PARSE_FILE_NAMES.put(FILE_PARSE_TYPE.QUOTED, ROOT_CA_INSTALLER_DIRECTORY_NAME + "/installer.nsi");
        ROOT_CA_INSTALLER_PARSE_FILE_NAMES.put(FILE_PARSE_TYPE.NON_QUOTED, ROOT_CA_INSTALLER_DIRECTORY_NAME + "/SoftwareLicense.txt");
    }

    private final Logger logger = Logger.getLogger(getClass());
    private final PipelineConnector[] connectors = new PipelineConnector[] {};

    private static final String EOL = "\n";

    private BrandingManagerSettings settings = null;

    /**
     * Setup branding manager application
     *
     * @param appSettings       Branding manager Application settings.
     * @param appProperties     Branding manager Application properties
     */
    public BrandingManagerApp( com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties )
    {
        super( appSettings, appProperties );
    }

    /**
     * Read branding manager settings into local settings object
     */
    private void readAppSettings()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/branding-manager/settings_" + appID + ".js";
        BrandingManagerSettings readSettings = null;

        logger.info("Loading settings from " + settingsFile);

        try {
            readSettings =  settingsManager.load( BrandingManagerSettings.class, settingsFile);
        } catch (Exception exn) {
            logger.error("Could not read app settings", exn);
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
            logger.error("Could not apply app settings", exn);
        }
    }

    /**
     * Pre branding manager start.
     * Set the file logo.
     *
     * @param isPermanentTransition
     *  If true, the app is permenant
     */
    @Override
    protected void preStart( boolean isPermanentTransition )
    {
        /**
         * Overwrite the logo just in case it has changed on disk
         */
        setFileLogo(settings.binary_getLogo());
    }

    /**
     * Post branding manager iniitalization
     */
    @Override
    protected void postInit()
    {
        readAppSettings();
        setFileLogo(settings.binary_getLogo());
        UvmContextFactory.context().servletFileManager().registerUploadHandler( new LogoUploadHandler(this) );
    }

    /**
     * Get the pineliene connector(???)
     *
     * @return PipelineConector
     */
    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    /**
     * Return branding manager settings
     *
     * @return
     *  BrandingManagerSettings object.
     */
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

    /**
     * Write branding manager settings
     *
     * @param newSettings
     *  New branding manager settings.
     */
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
        String appID = this.getAppSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/branding-manager/settings_" + appID +".js";

        try {
            settingsManager.save( settingsFile, newSettings );
        } catch (Exception exn) {
            logger.error("Could not save branding settings", exn);
            return;
        }
        
        this.settings = newSettings;

        setFileLogo(settings.binary_getLogo());
        createRootCaInstaller();
    }

    /**
     * Initializae default branding manager settings into the settings object.
     */
    @Override
    public void initializeSettings()
    {
        BrandingManagerSettings newSettings = new BrandingManagerSettings();
        logger.info("Initializing Settings...");

        /**
         * If OEM, initialize settings differently
         */
        String oemName = UvmContextFactory.context().oemManager().getOemName();
        if (oemName == null || oemName.equals("Untangle")) {
            newSettings.setCompanyName(DEFAULT_UNTANGLE_COMPANY_NAME);
            newSettings.setCompanyUrl(DEFAULT_UNTANGLE_URL);
        } else {
            newSettings.setCompanyName("MyCompany");
            newSettings.setCompanyUrl("http://mycompany.com/");
        }

        newSettings.setContactName("your network administrator");
        newSettings.setContactEmail(null);

        setSettings(newSettings);
    }

    /**
     * Read the contact HTML setting.
     *
     * @return
     *  String of contact HTML.
     */
    @Override
    public String getContactHtml()
    {
        return settings.grabContactHtml();
    }

    /**
     * Read the contact emailsetting.
     *
     * @return
     *  String of contact email address.
     */
    @Override
    public String getContactEmail()
    {
        return settings.getContactEmail();
    }

    /**
     * Read the contact name setting.
     *
     * @return
     *  String of contact name.
     */
    @Override
    public String getContactName()
    {
        return settings.getContactName();
    }

    /**
     * Read the company URL.
     *
     * @return
     *  String of company URL.
     */
    @Override
    public String getCompanyUrl()
    {
        return settings.getCompanyUrl();
    }

    /**
     * Read the company name.
     *
     * @return
     *  String of company name.
     */
    @Override
    public String getCompanyName()
    {
        return settings.getCompanyName();
    }

    /**
     * Write the company logo.
     *
     * @param logo
     *  Byte array of the logo image.
     */
    public void setLogo(byte[] logo)
    {
        if ( ! isLicenseValid() ) {
            return;
        }

        BrandingManagerSettings newSettings = this.getSettings();
        newSettings.binary_setLogo(logo);
        newSettings.setDefaultLogo(false);
        this.setSettings(newSettings);
    }

    /**
     * Write the company logo to the apopriate file.
     *
     * @param logo
     *  Byte array of the logo image.
     */
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

    /**
     * Using the non-branded version from uvm as a template base, modify
     * images and text to reflect branding.
     */
    private void createRootCaInstaller()
    {
        /*
         * Use the non-branded version as a template base.  Copy over.
         */
        UvmContextFactory.context().execManager().exec("rm -rf " + ROOT_CA_INSTALLER_DIRECTORY_NAME + "; cp -fa " + CertificateManager.ROOT_CA_INSTALLER_DIRECTORY_NAME + " " + ROOT_CA_INSTALLER_DIRECTORY_NAME);

        /*
         * Convert images to .bmp format
         */
        UvmContextFactory.context().execManager().exec("anytopnm " + BRANDING_LOGO + " | ppmtobmp > "+ROOT_CA_INSTALLER_DIRECTORY_NAME+"/images/modern-header.bmp");
        UvmContextFactory.context().execManager().exec("anytopnm " + BRANDING_LOGO + " | pnmrotate 90 | ppmtobmp > "+ROOT_CA_INSTALLER_DIRECTORY_NAME+"/images/modern-wizard.bmp");

        /*
         * Parse files replacing Untangle defaults
         */
        String companyName = settings.getCompanyName();
        String companyUrl = settings.getCompanyUrl();
        for(Map.Entry<FILE_PARSE_TYPE, String> filenameSet : ROOT_CA_INSTALLER_PARSE_FILE_NAMES.entrySet()) {
            String filename = filenameSet.getValue();
            File file = new File(filename);
            String name = file.getName();
            HashMap<REGEX_TYPE, Pattern> regexes = new HashMap<>();
            String quotedString = "";
            int flags = 0;
            if(filenameSet.getKey() == FILE_PARSE_TYPE.QUOTED){
                quotedString = "\"";
            }else{
                flags = Pattern.CASE_INSENSITIVE;                
            }

            /*
             * Build up regexes to find the first occurance of our current name.
             */
            regexes.put(REGEX_TYPE.COMPANY_NAME, Pattern.compile("(" + quotedString + ".*?)" + DEFAULT_UNTANGLE_COMPANY_NAME + "(.*" + quotedString + ")", flags));
            regexes.put(REGEX_TYPE.COMPANY_URL, Pattern.compile("(" + quotedString + ".*?)" + "http://.*.untangle.com(.*" + quotedString + ")", flags));

            StringBuilder parsed = new StringBuilder();
            Matcher match = null;
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(filename));
                for (String line = reader.readLine(); null != line; line = reader.readLine()) {
                    /*
                     * When parsing the nsi file we only want to replace strings within quotes and
                     * for other files, everything.
                     */
                    for(Map.Entry<REGEX_TYPE, Pattern> regex : regexes.entrySet()) {
                        match = regex.getValue().matcher(line);
                        int startPos = 0;
                        while(match.find(startPos)){
                            switch(regex.getKey()){
                                case COMPANY_NAME:
                                    startPos = match.start() + match.group(1).length() + companyName.length();
                                    line = match.replaceAll("$1" + companyName + "$2");
                                    break;
                                case COMPANY_URL:
                                    startPos = match.start() + match.group(1).length() + companyUrl.length();
                                    line = match.replaceAll("$1" + companyUrl + "$2");
                                    break;

                                default:
                                    /* Shouldn'e be here...but if we are, make sure we exit the loop. */
                                    startPos = line.length();
                            }
                            if(startPos >= line.length()){
                                break;
                            }
                           match = regex.getValue().matcher(line);
                        }
                    }
                    parsed.append(line).append(EOL);
                }
            } catch (Exception x) {
                logger.warn("Unable to open installer configuration file: " + filename );
                return;
            } finally {
                if (reader != null){
                    try {
                        reader.close();
                    } catch (Exception x) {
                        logger.warn("Unable to close installer configuration file: " + filename );
                    }
                }
            }

            FileOutputStream fos = null;
            File tmp = null;
            try {
                tmp = File.createTempFile( file.getName(), ".tmp");
                fos = new FileOutputStream(tmp);
                fos.write(parsed.toString().getBytes());
                fos.flush();
                fos.close();
                IOUtil.copyFile(tmp, new File(filename));
                tmp.delete();
            }catch(Exception ex) {
                IOUtil.close(fos);
                tmp.delete();
                logger.error("Unable to create installer file:" + filename + ":", ex);
            }
        }

        /*
         * Regenerate
         */
        UvmContextFactory.context().execManager().exec(CertificateManager.ROOT_CA_INSTALLER_SCRIPT);
    }

    /**
     * Branding manager logo upload handler.
     */
    private class LogoUploadHandler implements UploadHandler
    {
        private BrandingManagerApp app;

        /**
         * Initialize handler.
         *
         * @param app
         *  Application associated with this handler.
         */
        LogoUploadHandler( BrandingManagerApp app )
        {
            this.app = app;
        }

        /**
         * Read name of handler.
         *
         * @return
         *     String of the handler.
         */
        @Override
        public String getName()
        {
            return "logo";
        }

        /**
         * If file ends with supported filetype, save.
         * 
         * @param fileItem
         *  Uploaded file item.
         * @param argument
         *  Unused
         * @return
         *  On unsupported filetype, a string indicating invalid type.
         * @throws
         *  Generic exception indciating an unsupportrf filetype.
         */
        @Override
        public String handleFile(FileItem fileItem, String argument) throws Exception
        {
            if (fileItem.getName().toLowerCase().endsWith(".gif") ||
                fileItem.getName().toLowerCase().endsWith(".png") ||
                fileItem.getName().toLowerCase().endsWith(".jpg") ||
                fileItem.getName().toLowerCase().endsWith(".jpeg") ) {
                byte[] logo=fileItem.get();

                /* Use the context in order to properly handler premium vs normal. */
                app.setLogo(logo);
            } else {
                throw new Exception("Branding logo must be GIF, PNG, or JPG");
            }
            return "Uploaded new branding logo";
        }
    }
}
