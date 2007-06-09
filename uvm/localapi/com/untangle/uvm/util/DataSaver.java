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

package com.untangle.uvm.util;

import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.UvmLocalContext;
import com.untangle.uvm.node.NodeContext;


/* Isn't as flexible as it needs to be, should have a where clause, but this is just a start,
 * this class could also potentially with the DataLoader.  The Type on the generic must be
 * the specific type.  EG. class SomeImpl implements SomeInterface.  If you want to save SomeImpl
 * hibernate can't figure out that SomeInterface is actually a SomeImpl object.  Therefore, the
 * generic must be for a DataSaver<SomeImpl> */
public class DataSaver<T>
{
    /* The value retrieved from the last load */
    private T data;

    /* One of these must be null, use the non-null one */
    private final NodeContext nodeContext;
    private final UvmLocalContext localContext;
    
    public DataSaver( NodeContext node ) 
    {
        this.nodeContext = node;
        this.localContext = null;
        this.data = null;
    }

    public DataSaver( UvmLocalContext local ) 
    {
        this.nodeContext = null;
        this.localContext = local;
        this.data = null;
    }

    /** Attempt to saveData, returning the new object if it was saved, null if it wasn't */
    public T saveData( final T newData )
    {
        this.data = null;

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork( Session s )
                {
                    preSave( s );
                    s.merge( newData );
                    postSave( s );
                    DataSaver.this.data = (T)newData;
                    return true;
                }

                public Object getResult()
                {
                    return null;
                }
            };
        
        if ( this.nodeContext != null ) nodeContext.runTransaction( tw );
        else this.localContext.runTransaction( tw );
        
        return this.data;
    }
    
    /* Use this to run a function before the save */
    protected void preSave( Session s )
    {
    }

    protected void postSave( Session s )
    {
    }
}
