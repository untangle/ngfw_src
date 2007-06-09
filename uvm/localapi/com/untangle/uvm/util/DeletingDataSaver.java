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

import java.util.Iterator;

import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.UvmLocalContext;
import com.untangle.uvm.node.NodeContext;

/* This is a DataSaver designed to delete any related objects before saving the data */
public class DeletingDataSaver<T> extends DataSaver<T>
{
    private final String className;

    public DeletingDataSaver( NodeContext nodeContext, String className )
    {
        super( nodeContext );
        this.className = className;
    }

    public DeletingDataSaver( UvmLocalContext local, String className )
    {
        super( local );
        this.className = className;
    }
    
    /* Overwrite with a method that fixes everything */
    protected final void preSave( Session s )
    {
        Query q = s.createQuery( "from " + className );
        for ( Iterator iter = q.iterate() ; iter.hasNext() ; ) {
            T settings = (T)iter.next();
            s.delete( settings );
        }
    }
}


