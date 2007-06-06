/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.gui.configuration;

import javax.swing.JPanel;
import java.util.List;
import java.lang.reflect.Constructor;

import org.apache.log4j.Logger;

import com.untangle.gui.transform.CompoundSettings;
import com.untangle.gui.util.Util;
import com.untangle.mvvm.addrbook.*;
import com.untangle.mvvm.user.WMISettings;


public class DirectoryCompoundSettings implements CompoundSettings {
    
    
    private static final String DIRECTORY_JAR_NAME   = "charon";
    private final Logger logger = Logger.getLogger(getClass());
    
    // ADDRESS BOOK SETTINGS //
    private AddressBookSettings addressBookSettings;

    public AddressBookSettings getAddressBookSettings(){ return addressBookSettings; }

    public void setAddressBookSettings(AddressBookSettings inAddressBookSettings){ addressBookSettings = inAddressBookSettings; }

    // ADDRESS BOOK CONFIGURATION //
    private AddressBookConfiguration addressBookConfiguration;
    public AddressBookConfiguration getAddressBookConfiguration(){ return addressBookConfiguration; }
    public void setAddressBookConfiguration(AddressBookConfiguration inAddressBookConfiguration){ addressBookConfiguration = inAddressBookConfiguration; }

    // LOCAL USER ENTRIES //
    List<UserEntry> localUserList;
    public List<UserEntry> getLocalUserList(){ return localUserList; }
    public void setLocalUserList(List<UserEntry> inUserEntryList){ localUserList = inUserEntryList; }

    // WMI Settings
    private WMISettings wmiSettings;
    public WMISettings getWMISettings(){ return wmiSettings; }
    public void setWMISettings(WMISettings inWmiSettings){ wmiSettings = inWmiSettings; }

    // JPanels
    private JPanel localJPanel;
    public JPanel getLocalJPanel(){ return localJPanel; }
    private JPanel adJPanel;
    public JPanel getRemoteADJPanel(){ return adJPanel; }
    
    
    public void save() throws Exception {
        addressBookSettings.setAddressBookConfiguration(addressBookConfiguration);
        Util.getAddressBook().setAddressBookSettings(addressBookSettings);
        Util.getAddressBook().setLocalUserEntries(localUserList);
        Util.getPhoneBook().setWMISettings(wmiSettings);
    }

    public void refresh() throws Exception {
        addressBookSettings = Util.getAddressBook().getAddressBookSettings();
        addressBookConfiguration = addressBookSettings.getAddressBookConfiguration();
        localUserList = Util.getAddressBook().getLocalUserEntries();
        wmiSettings = Util.getPhoneBook().getWMISettings();

        if (localJPanel==null) {
           try {
                // LOCAL DIRECTORY ////////            
                Class localJPanelClass = Util.getClassLoader().loadClass( "com.untangle.gui.configuration.DirectoryLocalJPanel", DIRECTORY_JAR_NAME );
                Constructor localJPanelConstructor = localJPanelClass.getConstructor( new Class[]{} );
                localJPanel = (JPanel) localJPanelConstructor.newInstance(new Object[]{});            
           }
           catch (Exception e) {
                logger.warn("Unable to load: Local Directory", e);
                throw e;
           }
        }

        if (adJPanel==null) {
           try {
                // REMOTE ACTIVE DIRECTORY ////////        
                Class adJPanelClass = Util.getClassLoader().loadClass( "com.untangle.gui.configuration.DirectoryRemoteADJPanel", DIRECTORY_JAR_NAME );
                Constructor adJPanelConstructor = adJPanelClass.getConstructor( new Class[]{} );
                adJPanel = (JPanel) adJPanelConstructor.newInstance(new Object[]{});            
           }
           catch (Exception e) {
                logger.warn("Unable to load: Remote Directory", e);
                throw e;
           }
        }
 
    }

    public void validate() throws Exception {

    }

}
