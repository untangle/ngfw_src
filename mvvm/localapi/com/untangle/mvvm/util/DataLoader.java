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

package com.untangle.mvvm.util;

import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.mvvm.MvvmLocalContext;
import com.untangle.mvvm.tran.TransformContext;


/* Isn't as flexible as it needs to be, should have a where clause, but this is just a start */
public class DataLoader<T>
{
    /* The value retrieved from the last load */
    private T data;
    private final String type;

    /* One of these must be null, use the non-null one */
    private final TransformContext transformContext;
    private final MvvmLocalContext localContext;
    
    public DataLoader( String type, TransformContext transform ) 
    {
        this.type = type;
        this.transformContext = transform;
        this.localContext = null;
        this.data = null;
    }

    public DataLoader( String type, MvvmLocalContext local ) 
    {
        this.type = type;
        this.transformContext = null;
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
        
        if ( this.transformContext != null ) transformContext.runTransaction( tw );
        else this.localContext.runTransaction( tw );
        
        return this.data;
    }
}
