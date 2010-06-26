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

import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.node.NodeContext;


/* Isn't as flexible as it needs to be, should have a where clause, but this is just a start */
public class DataLoader<T>
{
    /* The value retrieved from the last load */
    private T data;
    private final String type;

    /* One of these must be null, use the non-null one */
    private final NodeContext nodeContext;
    private final LocalUvmContext localContext;
    
    public DataLoader( String type, NodeContext node ) 
    {
        this.type = type;
        this.nodeContext = node;
        this.localContext = null;
        this.data = null;
    }

    public DataLoader( String type, LocalUvmContext local ) 
    {
        this.type = type;
        this.nodeContext = null;
        this.localContext = local;
        this.data = null;
    }

    @SuppressWarnings("unchecked") //Query
    public T loadData()
    {
        /* Set to null, so if the transaction fails the previous result (if this object is reused)
         * will not be returned.  Could also throw an exception? */
        this.data = null;

        TransactionWork<Object> tw = new TransactionWork<Object>()
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
