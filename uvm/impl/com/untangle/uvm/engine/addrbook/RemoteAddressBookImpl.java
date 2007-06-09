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

package com.untangle.mvvm.engine.addrbook;

import java.util.List;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.ServiceUnavailableException;

import com.untangle.mvvm.addrbook.AddressBook;
import com.untangle.mvvm.addrbook.AddressBookSettings;
import com.untangle.mvvm.addrbook.NoSuchEmailException;
import com.untangle.mvvm.addrbook.RepositoryType;
import com.untangle.mvvm.addrbook.UserEntry;

/**
 * Concrete implementation of the AddressBook.  Note that this class
 * should only be used by classes in "engine" (the parent package).
 *
 */
public class RemoteAddressBookImpl implements AddressBook {

    private final AddressBook addressBook;

    RemoteAddressBookImpl(AddressBook addressBook) {
        this.addressBook = addressBook;
    }

    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public AddressBookSettings getAddressBookSettings() {
        return this.addressBook.getAddressBookSettings();
    }

    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public void setAddressBookSettings(final AddressBookSettings newSettings) {
        this.addressBook.setAddressBookSettings(newSettings);
    }


    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public boolean authenticate(String uid, String pwd)
        throws ServiceUnavailableException {
        return this.addressBook.authenticate(uid,pwd);
    }
        
    public Status getStatus() {
        return this.addressBook.getStatus();
    }


    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public boolean authenticateByEmail(String email, String pwd)
        throws ServiceUnavailableException, NoSuchEmailException {
        return this.addressBook.authenticateByEmail(email,pwd);
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public RepositoryType containsEmail(String address, RepositoryType searchIn)
        throws ServiceUnavailableException {
        return this.addressBook.containsEmail(address,searchIn);
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public RepositoryType containsEmail(String address)
        throws ServiceUnavailableException {
        return this.addressBook.containsEmail(address);
    }

    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public RepositoryType containsUid(String uid)
        throws ServiceUnavailableException {
        return this.addressBook.containsUid(uid);
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public RepositoryType containsUid(String uid, RepositoryType searchIn)
        throws ServiceUnavailableException {
        return this.addressBook.containsUid(uid,searchIn);
    }


    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public List<UserEntry> getLocalUserEntries()
        throws ServiceUnavailableException {
        return this.addressBook.getLocalUserEntries();
    }


    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public void setLocalUserEntries(List<UserEntry> userEntries)
        throws ServiceUnavailableException, NameNotFoundException, NameAlreadyBoundException {
        this.addressBook.setLocalUserEntries(userEntries);
    }


    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public List<UserEntry> getUserEntries()
        throws ServiceUnavailableException {
        return this.addressBook.getUserEntries();
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public List<UserEntry> getUserEntries(RepositoryType searchIn)
        throws ServiceUnavailableException {
        return this.addressBook.getUserEntries(searchIn);
    }


    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public UserEntry getEntry(String uid)
        throws ServiceUnavailableException {
        return this.addressBook.getEntry(uid);
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public UserEntry getEntry(String uid, RepositoryType searchIn)
        throws ServiceUnavailableException {
        return this.addressBook.getEntry(uid,searchIn);
    }

    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public UserEntry getEntryByEmail(String email)
        throws ServiceUnavailableException {
        return this.addressBook.getEntryByEmail(email);
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public UserEntry getEntryByEmail(String email, RepositoryType searchIn)
        throws ServiceUnavailableException {
        return this.addressBook.getEntryByEmail(email,searchIn);
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public void createLocalEntry(UserEntry newEntry, String password)
        throws NameAlreadyBoundException, ServiceUnavailableException {
        this.addressBook.createLocalEntry(newEntry,password);
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public boolean deleteLocalEntry(String entryUid)
        throws ServiceUnavailableException {
        return this.addressBook.deleteLocalEntry(entryUid);
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public void updateLocalEntry(UserEntry changedEntry)
        throws ServiceUnavailableException, NameNotFoundException {
        this.addressBook.updateLocalEntry(changedEntry);
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public void updateLocalPassword(String uid, String newPassword)
        throws ServiceUnavailableException, NameNotFoundException {
        this.addressBook.updateLocalPassword(uid,newPassword);
    }
}


