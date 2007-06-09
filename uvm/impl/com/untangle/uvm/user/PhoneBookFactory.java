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

package com.untangle.uvm.user;

import com.untangle.node.util.MVLogger;

public class PhoneBookFactory
{
    private static final String PROPERTY_PHONEBOOK_IMPL = "com.untangle.uvm.phonebook";
    private static final String PREMIUM_PHONEBOOK_IMPL = "com.untangle.uvm.user.PremiumPhoneBookImpl";

    private final MVLogger logger = new MVLogger( getClass());

    /** The stripped down default limited phone book */
    private final DefaultPhoneBookImpl limited = new DefaultPhoneBookImpl();

    /** The current phonebook */
    private PremiumPhoneBook premium = null;
    
    /* The current remote phonebook */
    private RemotePhoneBook remote = new RemotePhoneBookImpl( this.limited );

    private PhoneBookFactory()
    {
    }

    public LocalPhoneBook getLocal()
    {
        return ( this.premium == null ) ? this.limited : this.premium;
    }

    public RemotePhoneBook getRemote()
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

        String className = System.getProperty( PROPERTY_PHONEBOOK_IMPL );
        if ( null == className ) {
            className = PREMIUM_PHONEBOOK_IMPL;
        }
        try {
            this.premium = (PremiumPhoneBook)Class.forName( className ).newInstance();
            this.remote = new RemotePhoneBookImpl( this.premium );
        } catch ( Exception e ) {
            logger.info( "Could not load LocalPhoneBook: " + className, e );
            this.premium = null;
            this.remote = new RemotePhoneBookImpl( this.limited );
        }
    }

    public void init()
    {
        if ( this.premium != null ) this.premium.init();
    }

    public void destroy()
    {
        if ( this.premium != null ) this.premium.destroy();
    }

    public static PhoneBookFactory makeInstance()
    {
        PhoneBookFactory factory = new PhoneBookFactory();
        factory.refresh();
        return factory;
    }

    /**
     * Inner interface used to indicate the additional methods that the 
     * premium offering must implement.
     */
    static interface PremiumPhoneBook extends LocalPhoneBook
    {
        public void init();
        
        public void destroy();
    }
} 
