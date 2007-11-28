/*
 * $HeadURL$
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

package com.untangle.uvm.user;

import com.untangle.node.util.UtLogger;

public class PhoneBookFactory
{
    private static final String PROPERTY_PHONEBOOK_IMPL = "com.untangle.uvm.phonebook";
    private static final String PREMIUM_PHONEBOOK_IMPL = "com.untangle.uvm.user.PremiumPhoneBookImpl";

    private final UtLogger logger = new UtLogger( getClass());

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
	//Disabling the premium phone book code
	if ( this.premium == null && this.remote != null ) {
	    this.premium = null;
	    this.remote = new RemotePhoneBookImpl( this.limited );
	    return;
	}

	//Uncomment this and remove above code if you ever want the premium 
	//phone book (WMIAssistant) back
//         if ( this.premium != null ) {
//             logger.debug( "Already loaded the premium offering" );
//             return;
//         }

//         String className = System.getProperty( PROPERTY_PHONEBOOK_IMPL );
//         if ( null == className ) {
//             className = PREMIUM_PHONEBOOK_IMPL;
//         }
//         try {
//             this.premium = (PremiumPhoneBook)Class.forName( className ).newInstance();
//             this.remote = new RemotePhoneBookImpl( this.premium );
//         } catch ( Exception e ) {
//             logger.info( "Could not load LocalPhoneBook: " + className, e );
//             this.premium = null;
//             this.remote = new RemotePhoneBookImpl( this.limited );
//         }
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
