/*
 * $Id: OemManagerImpl.java,v 1.00 2011/08/11 13:35:05 dmorris Exp $
 */
package com.untangle.uvm.engine;

import org.apache.log4j.Logger;

import java.util.Enumeration;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import com.untangle.uvm.RemoteOemManager;

/**
 * OemManagerImpl opens up the OEM properties file and overwrites any current properties with the OEM settings
 */
class OemManagerImpl implements RemoteOemManager
{
    private static final String OEM_PROPERTIES_FILE = "/etc/untangle/oem/oem.properties";

    private static final String OEM_PROPERTY_NAME = "uvm.oem.name";
    private static final String OEM_PROPERTY_URL = "uvm.oem.url";

    private final Logger logger = Logger.getLogger(getClass());

    private final Properties props;
    
    public OemManagerImpl()
    {
        /**
         * FIXME Need to check for a valid OEM license here
         */
        boolean validOemLicense = true; 
        
        this.props = new Properties();

        if (validOemLicense) {
            try {
                logger.info("Loading OEM properties file: " + OEM_PROPERTIES_FILE);
                File propsFile = new File(OEM_PROPERTIES_FILE);
                FileInputStream is = new FileInputStream(propsFile);
                props.load(is);
                is.close();
            } catch (FileNotFoundException e) {
                logger.info("No OEM properties file found.");
            }
            catch (IOException e) {
                logger.error("Exception reading OEM properties file",e); 
            }
        }

        /**
         * Set the defaults if they have not been set
         */
        if (props.getProperty(OEM_PROPERTY_NAME) == null) {
            logger.info("Loading OEM Name default");
            props.setProperty(OEM_PROPERTY_NAME,"Untangle");
        }
        if (props.getProperty(OEM_PROPERTY_URL) == null) {
            logger.info("Loading OEM URL  default");
            props.setProperty(OEM_PROPERTY_URL,"http://untangle.com/");
        }

        Enumeration<Object> e = props.keys();
        while(e.hasMoreElements()) {
            String key = (String)e.nextElement();
            logger.info("Setting OEM property " + key +  " = " + props.getProperty(key));
            System.setProperty(key,props.getProperty(key));
        }
    }

    @Override
    public String getOemName()
    {
        return props.getProperty(OEM_PROPERTY_NAME);
    }

    @Override
    public String getOemUrl()
    {
        return props.getProperty(OEM_PROPERTY_URL);
    }
    
}
