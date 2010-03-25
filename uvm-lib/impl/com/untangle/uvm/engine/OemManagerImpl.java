/*
 * $HeadURL: svn://chef/work/src/uvm-lib/impl/com/untangle/uvm/engine/UvmContextImpl.java $
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

import org.apache.log4j.Logger;

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
    private static final String OEM_PROPERTIES_FILE = System.getProperty("uvm.conf.dir") + "/oem.properties";

    private static final String OEM_PROPERTY_NAME = "uvm.oem.name";

    private final Logger logger = Logger.getLogger(UvmContextImpl.class);

    private final Properties props;
    
    public OemManagerImpl()
    {
        /**
         * FIXME XXX
         * Need to check for a valid OEM license here
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
            logger.info("Loading OEM name default");
            props.setProperty(OEM_PROPERTY_NAME,"Untangle");
        }
    }

    @Override
    public String getOemName()
    {
        return props.getProperty(OEM_PROPERTY_NAME);
    }

}
