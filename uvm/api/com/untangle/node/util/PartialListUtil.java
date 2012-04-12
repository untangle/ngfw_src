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

package com.untangle.node.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.Rule;
import com.untangle.uvm.NodeSettings;
import com.untangle.uvm.util.QueryUtil;
import com.untangle.uvm.util.TransactionWork;

public class PartialListUtil
{
    /* Useful utility for tracking rule lists. */
    public static final RuleHandler RULE_HANDLER = new RuleHandler();

    public PartialListUtil()
    {
    }

    /* Just a helper function for the most common case of listing from a node. */
    @SuppressWarnings("unchecked")
    public List getItems( String queryString, NodeContext nodeContext, NodeSettings nodeId, int start, int limit, String ... sortColumns)
    {
        return getItems( queryString, nodeContext, nodeId, null, start, limit, sortColumns );
    }

    @SuppressWarnings("unchecked")
    public List getItems( String queryString, NodeContext nodeContext, Parameter[] parameters, int start, int limit, String ... sortColumns)
    {
        return getItems(queryString, nodeContext, parameters, null, start, limit, sortColumns);
    }

    @SuppressWarnings("unchecked")
    public List getItems( String queryString, UvmContext localContext, Parameter[] parameters, int start, int limit, String ... sortColumns)
    {
        return getItems(queryString, localContext, parameters, null, start, limit, sortColumns);
    }

    /* Just a helper function for the most common case of listing from a node. */
    @SuppressWarnings("unchecked")
    public List getItems( String queryString, NodeContext nodeContext, NodeSettings nodeId, String alias, int start, int limit, String ... sortColumns)
    {
        return getItems( queryString, nodeContext, new Parameter[] { new Parameter( "nodeId", nodeId )}, alias, start, limit, sortColumns );
    }

    @SuppressWarnings("unchecked")
    public List getItems( String queryString, NodeContext nodeContext, Parameter[] parameters,
                          String alias, int start, int limit, String ... sortColumns)

    {
        TransactionWork<List> tw = getItemsTransactionWork( queryString, parameters,
                                                            alias, start, limit, sortColumns );

        nodeContext.runTransaction( tw );
        return tw.getResult();
    }
    
    @SuppressWarnings("unchecked")
    public List getItems( String queryString, UvmContext localContext, Parameter[] parameters, String alias, int start, int limit, String ... sortColumns)
    {
        TransactionWork<List> tw = getItemsTransactionWork( queryString, parameters,
                                                            alias, start, limit, sortColumns );

        localContext.runTransaction( tw );
        return tw.getResult();
    }

    /* danger, but this is how it comes in from the web ui */
    @SuppressWarnings("unchecked")
	public void updateCachedItems( Collection items, List[] modifications )
    {
        updateCachedItems(items, RULE_HANDLER, modifications );
    }

    @SuppressWarnings("unchecked")
    public <T> void updateCachedItems( Collection<T> items, Handler<T> handler, List[] modifications )
    {
        if ( modifications == null ) return;
        if ( modifications.length < 3 ) return;

        List<T> added = modifications[0];
        List<Long> deleted = modifications[1];
        List<T> modified = modifications[2];

        updateCachedItems( items, handler, added, deleted, modified );
    }

    /* not the best type safe solution, but every other case makes it hard to use. */
    @SuppressWarnings("unchecked")
	public void updateCachedItems( Collection items, List added, List<Long> deleted, List modified )
    {
        updateCachedItems(items, RULE_HANDLER, added, deleted, modified );
    }

    public <T> void updateCachedItems( Collection<T> items, Handler<T> handler, List<T> added, List<Long> deleted, List<T> modified )
    {
        for ( Iterator<T> i = items.iterator(); i.hasNext(); ) {
            T item = i.next();
            T mItem = null;
            if ( deleted != null && PartialListUtil.contains( deleted, handler.getId( item ))) {
                i.remove();
            } else if (modified != null && ( mItem = modifiedItem( handler, item, modified )) != null) {
                handler.update( item, mItem );
            }
        }

        if ( added != null ) items.addAll(added);
    }

    // private methods ------------------------------------------------
    private <T> T modifiedItem( Handler<T> handler, T item, List<T> modified )
    {
        for ( T currentItem : modified ) {
            if( handler.getId( currentItem ).equals( handler.getId( item ))) return currentItem;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private TransactionWork<List> getItemsTransactionWork( final String queryString, final Parameter[] parameters, final String alias, final int start, final int limit, final String ... sortColumns )
    {
        return new TransactionWork<List>() {
            private List result;

            public boolean doWork( Session s ) {
                Query q = s.createQuery( queryString + QueryUtil.toOrderByClause(alias, sortColumns ));

                if ( parameters != null ) {
                    for ( Parameter parameter : parameters ) {
                        q.setParameter( parameter.getKey(), parameter.getValue());
                    }
                }

                q.setFirstResult( start );
                q.setMaxResults( limit );
                result = q.list();

                return true;
            }

            public List getResult() {
                return result;
            }
        };
    }

    public static class Parameter
    {
        private final String key;
        private final Object value;

        public Parameter( String key, Object value )
        {
            this.key = key;
            this.value = value;
        }

        public String getKey()
        {
            return this.key;
        }

        public Object getValue()
        {
            return this.value;
        }
    }

    /* This is a handler for retrieving information from an item in a list. */
    public interface Handler<T>
    {
        public Long getId( T item );

        /**
         * Given another item of a specific type, update itself so it
         * contains the data from item.
         * @param current The item to copy the data to.
         * @param newItem the item to copy the data from.
         */
        public void update( T current, T newItem );
    }

    /* Utility handler for the most common case (rules) */
    public static class RuleHandler implements Handler<Rule>
    {
        public Long getId( Rule rule )
        {
            return rule.getId();
        }

        public void update( Rule current, Rule newRule )
        {
            current.update( newRule );
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean contains(List list, Long elem) {
    	for (int i = 0; i < list.size(); i++) {
    		Object currentElem = list.get(i);
    		if (currentElem instanceof Long && currentElem.equals(elem)){
    			return true;
    		} else if (currentElem instanceof Integer && 
    				((Integer)currentElem).longValue() == elem.longValue()){
    			return true;
    		}
		}
        return false;
    }
    
}
