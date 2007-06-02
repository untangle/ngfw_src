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

import java.util.ArrayList;
import java.util.List;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.ServiceUnavailableException;

import org.apache.log4j.Logger;

import com.untangle.mvvm.addrbook.AddressBook;
import com.untangle.mvvm.addrbook.AddressBookConfiguration;
import com.untangle.mvvm.addrbook.AddressBookSettings;
import com.untangle.mvvm.addrbook.NoSuchEmailException;
import com.untangle.mvvm.addrbook.RepositorySettings;
import com.untangle.mvvm.addrbook.RepositoryType;
import com.untangle.mvvm.addrbook.UserEntry;

/**
 * Concrete implementation of the AddressBook.  Note that this class
 * should only be used by classes in "engine" (the parent package).
 *
 */
public class DefaultAddressBookImpl implements AddressBook {

    private static final ABStatus status = new ABStatus();

    private final Logger m_logger =
        Logger.getLogger(getClass());
    private AddressBookSettings m_settings;

    DefaultAddressBookImpl() {
    }

    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public AddressBookSettings getAddressBookSettings() {
        m_logger.info("getting invalid settings");
        AddressBookSettings m_settings = new AddressBookSettings();
        m_settings.setAddressBookConfiguration(AddressBookConfiguration.NOT_CONFIGURED);
        m_settings.setADRepositorySettings(new RepositorySettings());
        return m_settings;
    }

    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public void setAddressBookSettings(final AddressBookSettings newSettings) {
        m_logger.info("ignoring save settings");
    }


    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public boolean authenticate(String uid, String pwd)
        throws ServiceUnavailableException {
        m_logger.info("ignoring authenticate.");
        return false;
    }

    private static class ABStatus implements AddressBook.Status {
        public boolean isLocalWorking() { return false; }
        public boolean isADWorking() { return false; }
        public String localDetail() { return "unconfigured"; }
        public String adDetail() { return "unconfigured"; }
    }
        
    public Status getStatus() {
        m_logger.info("ignoring get status.");
        return this.status;
    }


    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public boolean authenticateByEmail(String email, String pwd)
        throws ServiceUnavailableException, NoSuchEmailException {
        
        m_logger.info("ignoring authenticate by email.");
        return false;
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public RepositoryType containsEmail(String address, RepositoryType searchIn)
        throws ServiceUnavailableException {
        m_logger.info("ignoring contains email.");
        return RepositoryType.NONE;
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public RepositoryType containsEmail(String address)
        throws ServiceUnavailableException {
        m_logger.info("ignoring contains email.");
        return RepositoryType.NONE;
    }

    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public RepositoryType containsUid(String uid)
        throws ServiceUnavailableException {
        m_logger.info("ignoring contains uid.");
        return RepositoryType.NONE;
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public RepositoryType containsUid(String uid, RepositoryType searchIn)
        throws ServiceUnavailableException {
        m_logger.info("ignoring contains uid.");
        return RepositoryType.NONE;
    }


    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public List<UserEntry> getLocalUserEntries()
        throws ServiceUnavailableException {
        m_logger.info("ignoring contains getLocalUserEntries.");
        return new ArrayList<UserEntry>();
    }


    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public void setLocalUserEntries(List<UserEntry> userEntries)
        throws ServiceUnavailableException, NameNotFoundException, NameAlreadyBoundException {
        m_logger.info("ignoring contains setLocalUserEntries.");
    }


    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public List<UserEntry> getUserEntries()
        throws ServiceUnavailableException {
        m_logger.info("ignoring getUserEntries");
        return new ArrayList<UserEntry>();
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public List<UserEntry> getUserEntries(RepositoryType searchIn)
        throws ServiceUnavailableException {
        m_logger.info("ignoring getUserEntries");
        return new ArrayList<UserEntry>();
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public UserEntry getEntry(String uid)
        throws ServiceUnavailableException {
        m_logger.info("ignoring getEntry");
        return null;
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public UserEntry getEntry(String uid, RepositoryType searchIn)
        throws ServiceUnavailableException {
        m_logger.info("ignoring getEntry");
        return null;
    }

    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public UserEntry getEntryByEmail(String email)
        throws ServiceUnavailableException {
        m_logger.info("ignoring getEntryByEmail");
        return null;
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public UserEntry getEntryByEmail(String email, RepositoryType searchIn)
        throws ServiceUnavailableException {
        m_logger.info("ignoring getEntryByEmail");
        return null;
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public void createLocalEntry(UserEntry newEntry, String password)
        throws NameAlreadyBoundException, ServiceUnavailableException {
        m_logger.info("ignoring createLocalEntry");
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public boolean deleteLocalEntry(String entryUid)
        throws ServiceUnavailableException {
        m_logger.info("ignoring deleteLocalEntry");
        return false;
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public void updateLocalEntry(UserEntry changedEntry)
        throws ServiceUnavailableException, NameNotFoundException {
        m_logger.info("ignoring updateLocalEntry");
    }



    //====================================================
    // See doc on com.untangle.mvvm.addrbook.AddressBook
    //====================================================
    public void updateLocalPassword(String uid, String newPassword)
        throws ServiceUnavailableException, NameNotFoundException {
        m_logger.info("ignoring updateLocalPassword");
    }
}


