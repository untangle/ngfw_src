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


/* Isn't as flexible as it needs to be, should have a where clause, but this is just a start */
public class DataLoader<T>
{
    /* The value retrieved from the last load */
    private T data;
    private final String type;

    /* One of these must be null, use the non-null one */
    private final NodeContext nodeContext;
    private final UvmLocalContext localContext;
    
    public DataLoader( String type, NodeContext node ) 
    {
        this.type = type;
        this.nodeContext = node;
        this.localContext = null;
        this.data = null;
    }

    public DataLoader( String type, UvmLocalContext local ) 
    {
        this.type = type;
        this.nodeContext = null;
        this.localContext = local;
        this.data = null;
    }

    public T loadData()
    {
        /* Set to null, so if the transaction fails the previous result (if this object is reused)
         * will not be returned.  Could also throw an exception? */
        this.data = null;

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    /* ???? What happens when there are multiple results */
                    Query q = s.createQuery( "from " + type );
                    DataLoader.this.data = (T)q.uniqueResult();
                    
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
}
