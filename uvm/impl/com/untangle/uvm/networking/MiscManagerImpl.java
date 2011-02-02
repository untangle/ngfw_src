/* $HeadURL$ */
package com.untangle.uvm.networking;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Query;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.node.script.ScriptWriter;
import com.untangle.uvm.util.TransactionWork;

class MiscManagerImpl implements LocalMiscManager
{
    private final Logger logger = Logger.getLogger(getClass());

    MiscSettings miscSettings = null;

    MiscManagerImpl()
    {
    }

    /* Use this to retrieve just the remote settings */
    public MiscSettings getSettings()
    {
        return this.miscSettings;
    }

    /* Use this to mess with the remote settings without modifying the network settings */
    @SuppressWarnings("unchecked")
    public synchronized void setSettings( final MiscSettings settings )
    {
        TransactionWork<Void> tw = new TransactionWork<Void>()
            {
                public boolean doWork(Session s)
                {
                    miscSettings = (MiscSettings)s.merge(settings);
                    return true;
                }
            };
        LocalUvmContextFactory.context().runTransaction(tw);
    }

    /* ---------------------- PACKAGE ---------------------- */
    /* Initialize the settings, load at startup */
    synchronized void init()
    {
        TransactionWork<Object> tw = new TransactionWork<Object>()
            {
                public boolean doWork(Session s)
                {
                    /* XXX What happens when there are multiple results */
                    Query q = s.createQuery( "from " + "MiscSettings");
                    miscSettings = (MiscSettings)q.uniqueResult();
                    return true;
                }
            };

        LocalUvmContextFactory.context().runTransaction(tw);

        if ( this.miscSettings == null ) {
            logger.info( "There are no misc settings in the database, must initialize from files." );
            setSettings( new MiscSettings() );
        }
    }
}
