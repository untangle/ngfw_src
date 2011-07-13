/*
 * $Id$
 */
package com.untangle.uvm.engine;

import java.lang.reflect.Constructor;

import com.untangle.node.util.UtLogger;
import com.untangle.uvm.addrbook.RemoteAddressBook;

class AddressBookFactory
{
    private static final String PREMIUM_ADDRESSBOOK_IMPL = "com.untangle.uvm.engine.PremiumAddressBookImpl";

    private final UtLogger logger = new UtLogger(getClass());

    /** The stripped down default limited address book */
    private final DefaultAddressBookImpl limited = new DefaultAddressBookImpl();

    /** The premium address book */
    private PremiumAddressBook premium = null;

    /** remote address book */
    //private RemoteAddressBook remote = new RemoteAddressBookAdaptor(limited);
    private RemoteAddressBook remote = limited;

    private AddressBookFactory() { }

    public RemoteAddressBook getAddressBook()
    {
        if (this.premium == null) {
            refresh();
        }
        
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

        try {
            Constructor<PremiumAddressBook> constructor = (Constructor<PremiumAddressBook>)Class.forName( PREMIUM_ADDRESSBOOK_IMPL ).getDeclaredConstructor(DefaultAddressBookImpl.class);

            this.premium = constructor.newInstance( this.limited );
            this.premium.init();
            //this.remote = new RemoteAddressBookAdaptor(this.premium);
            this.remote = this.premium;
        } catch ( Exception e ) {
            logger.info( "Could not load premium AddressBook: " + PREMIUM_ADDRESSBOOK_IMPL, e);
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
