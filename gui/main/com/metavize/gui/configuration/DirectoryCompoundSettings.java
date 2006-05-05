/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.configuration;

import com.metavize.gui.util.Util;
import com.metavize.gui.transform.CompoundSettings;
import com.metavize.mvvm.addrbook.*;

import java.util.List;


public class DirectoryCompoundSettings implements CompoundSettings {

    // ADDRESS BOOK SETTINGS //
    private AddressBookSettings addressBookSettings;
    public AddressBookSettings getAddressBookSettings(){ return addressBookSettings; }
    public void setAddressBookSettings(AddressBookSettings inAddressBookSettings){ addressBookSettings = inAddressBookSettings; }

    // LOCAL USER ENTRIES //
    List<UserEntry> localUserList;
    public List<UserEntry> getLocalUserList(){ return localUserList; }
    public void setLocalUserList(List<UserEntry> inUserEntryList){ localUserList = inUserEntryList; }

    public void save() throws Exception {
	Util.getAddressBook().setAddressBookSettings(addressBookSettings);
	Util.getAddressBook().setLocalUserEntries(localUserList);
    }

    public void refresh() throws Exception {
	addressBookSettings = Util.getAddressBook().getAddressBookSettings();
	localUserList = Util.getAddressBook().getLocalUserEntries();
    }

    public void validate() throws Exception {

    }

}
