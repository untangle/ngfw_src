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

import java.util.Iterator;

import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.mvvm.MvvmLocalContext;
import com.untangle.mvvm.tran.TransformContext;

/* This is a DataSaver designed to delete any related objects before saving the data */
public class DeletingDataSaver<T> extends DataSaver<T>
{
    private final String className;

    public DeletingDataSaver( TransformContext transformContext, String className )
    {
        super( transformContext );
        this.className = className;
    }

    public DeletingDataSaver( MvvmLocalContext local, String className )
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


