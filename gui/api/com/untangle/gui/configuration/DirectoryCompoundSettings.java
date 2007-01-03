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

import com.untangle.gui.util.Util;
import com.untangle.gui.transform.CompoundSettings;
import com.untangle.mvvm.addrbook.*;

import com.untangle.mvvm.user.WMISettings;


import java.util.List;


public class DirectoryCompoundSettings implements CompoundSettings {

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
    }

    public void validate() throws Exception {

    }

}
