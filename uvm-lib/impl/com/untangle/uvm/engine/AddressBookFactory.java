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

package com.untangle.uvm.engine;

import java.lang.reflect.Constructor;

import com.untangle.node.util.UtLogger;
import com.untangle.uvm.addrbook.RemoteAddressBook;

class AddressBookFactory
{
    private static final String PROPERTY_ADDRESSBOOK_IMPL = "com.untangle.uvm.addrbook";
    private static final String PREMIUM_ADDRESSBOOK_IMPL = "com.untangle.uvm.engine.PremiumAddressBookImpl";

    private final UtLogger logger = new UtLogger(getClass());

    /** The stripped down default limited address book */
    private final DefaultAddressBookImpl limited = new DefaultAddressBookImpl();

    /** The premium address book */
    private PremiumAddressBook premium = null;

    /** remote address book */
    private RemoteAddressBook remote = new RemoteAddressBookAdaptor(limited);

    private AddressBookFactory() { }

    public RemoteAddressBook getAddressBook()
    {
        return ( this.premium == null ) ? this.limited : this.premium;
    }

    public RemoteAddressBook getRemoteAddressBook()
    {
        return this.remote;
    }

    /* Retest for the premium class */
    @SuppressWarnings("unchecked") //Class.forName
    public void refresh()
    {
        if ( this.premium != null ) {
            logger.debug( "Already loaded the premium offering" );
            return;
        }

        String className = System.getProperty(PROPERTY_ADDRESSBOOK_IMPL);
        if (null == className) {
            className = PREMIUM_ADDRESSBOOK_IMPL;
        }
        try {
            Constructor<PremiumAddressBook> constructor =
                (Constructor<PremiumAddressBook>)Class.forName( className ).
                getDeclaredConstructor(DefaultAddressBookImpl.class);

            this.premium = constructor.newInstance( this.limited );
            this.premium.init();
            //this.remote = new RemoteAddressBookAdaptor(this.premium);
            this.remote = this.premium;
        } catch ( Exception e ) {
            logger.debug( "Could not load premium AddressBook: " + className);
            this.premium = null;
            //this.remote = new RemoteAddressBookAdaptor(this.limited);
            this.remote = this.limited;
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
    static interface PremiumAddressBook extends RemoteAddressBook
    {
        public void init();
    }
}
