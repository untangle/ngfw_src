/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.util;

import java.util.Iterator;

import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.LocalUvmContext;
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

    public DeletingDataSaver( LocalUvmContext local, String className )
    {
        super( local );
        this.className = className;
    }
    
    /* Overwrite with a method that fixes everything */
    protected final void preSave( Session s )
    {
        Query q = s.createQuery( "from " + className );
        for ( Iterator<T> iter = q.iterate() ; iter.hasNext() ; ) {
            T settings = iter.next();
            s.delete( settings );
        }
    }
}


