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

package com.untangle.uvm.engine.addrbook;

import com.untangle.uvm.addrbook.AddressBook;

import com.untangle.node.util.MVLogger;

public class AddressBookFactory
{
    private static final String PROPERTY_ADDRESSBOOK_IMPL = "com.untangle.uvm.addrbook";
    private static final String PREMIUM_ADDRESSBOOK_IMPL = "com.untangle.uvm.engine.addrbook.PremiumAddressBookImpl";

    private final MVLogger logger = new MVLogger( getClass());

    /** The stripped down default limited address book */
    private final DefaultAddressBookImpl limited = new DefaultAddressBookImpl();

    /** The premium address book */
    private AddressBook premium = null;

    /** remote address book */
    private AddressBook remote = new RemoteAddressBookImpl(limited);
    
    private AddressBookFactory()
    {
    }

    public AddressBook getAddressBook()
    {
        return ( this.premium == null ) ? this.limited : this.premium;
    }

    public AddressBook getRemoteAddressBook()
    {
        return this.remote;
    }

    /* Retest for the premium class */
    public void refresh()
    {
        if ( this.premium != null ) {
            logger.debug( "Already loaded the premium offering" );
            return;
        }

        String className = System.getProperty( PROPERTY_ADDRESSBOOK_IMPL );
        if ( null == className ) {
            className = PREMIUM_ADDRESSBOOK_IMPL;
        }
        try {
            this.premium = (PremiumAddressBook)Class.forName( className ).newInstance();
            this.remote = new RemoteAddressBookImpl( this.premium );
        } catch ( Exception e ) {
            logger.info( "Could not load premium AddressBook: " + className, e );
            this.premium = null;
            this.remote = new RemoteAddressBookImpl( this.limited );
        }
    }

    public void init()
    {
        /* Premium needs an initialization function */
    }

    public void destroy()
    {
        /* Premium needs an destroy function */
    }

    public static AddressBookFactory makeInstance()
    {
        AddressBookFactory factory = new AddressBookFactory();
        factory.refresh();
        return factory;
    }

    /**
     * Inner interface used to indicate the additional methods that the 
     * premium offering must implement.
     */
    static interface PremiumAddressBook extends AddressBook
    {
    }
} 
