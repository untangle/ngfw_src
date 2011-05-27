package com.untangle.uvm.logging;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.util.TransactionWork;

/**
 * This event manager doesn't use a cache.  It fetches events directly from the database every time.
 * This is useful for cases where there log events 
 * @author rbscott
 *
 */
public class UncachedEventManager<E extends LogEvent> implements EventManager<E> {
    private final List<EventRepository<E>> repositories = new LinkedList<EventRepository<E>>();
    
    @Override
    public List<EventRepository<E>> getRepositories() {
        return new ArrayList<EventRepository<E>>( this.repositories );
    }

    @Override
    public EventRepository<E> getRepository(String filterName) {
        for ( EventRepository<E> repository : this.repositories ) {
            if ( repository.getRepositoryDesc().getName().equals( filterName )) {
                return repository;
            }
        }
        
        return null;
    }

    @Override
    public List<RepositoryDesc> getRepositoryDescs() {
        List<RepositoryDesc> repositoryDesc = new ArrayList<RepositoryDesc>( this.repositories.size());
        
        for (  EventRepository<E> repository : this.repositories ) {
            repositoryDesc.add(repository.getRepositoryDesc());
        }
        
        return repositoryDesc;
    }

    @Override
    public void log(E e) {
        /* This should never be used. */
        throw new IllegalStateException("Do not use an uncached event manager for logging.");        
    }
    
    public void makeRepository(UncachedEventFilter<E> uncachedEventFilter )
    {
        EventRepository<E> repository = new UncachedRepository<E>(uncachedEventFilter);
        this.repositories.add(repository);
    }

    private static class UncachedRepository<T extends LogEvent> implements EventRepository<T>
    {
        private UncachedEventFilter<T> uncachedEventFilter;

        UncachedRepository(UncachedEventFilter<T> uncachedEventFilter)
        {
            this.uncachedEventFilter = uncachedEventFilter;
        }

        // ListEventFilter methods ------------------------------------------------
        @Override
        public RepositoryDesc getRepositoryDesc()
        {
            return this.uncachedEventFilter.getRepositoryDesc();
        }
        
        @Override
        public List<T> getEvents()
        {
            return getEvents(MAX_SIZE);
        }
        
        @Override
        public List<T> getEvents(int limit)
        {
            final NodeContext tctx = this.uncachedEventFilter.getNodeContext();
            final String query = this.uncachedEventFilter.getQuery();
            final List<T> events = new LinkedList<T>();

            TransactionWork<Void> tw = new TransactionWork<Void>()
                {
                    public boolean doWork(Session s) throws SQLException
                    {
                        Map<String,Object> params;
                        if (null != tctx) {
                            Policy policy = tctx.getNodeId().getPolicy();
                            params = Collections.singletonMap("policy", (Object)policy);
                        } else {
                            params = Collections.emptyMap();
                        }
                        
                        runQuery( query, s, events, MAX_SIZE, params);
                        return true;
                    }
                };

            if (null == tctx) {
                LocalUvmContextFactory.context().runTransaction(tw);
            } else {
                tctx.runTransaction(tw);
            }
            
            return events;
        }

        // private methods --------------------------------------------------------
        

        @SuppressWarnings("unchecked") //Query
        private void runQuery(String query, Session s, List<T> events, int limit,
                Map<String, Object> params)
        {
            Query q = s.createQuery(query);
            for (String param : q.getNamedParameters()) {
                Object o = params.get(param);
                if (null != o) {
                    q.setParameter(param, o);
                }
            }

            q.setMaxResults(limit);

            int c = 0;
            for (Iterator<T> i = q.iterate(); i.hasNext() && ++c < limit; ) {
                T sb = i.next();
                Hibernate.initialize(sb);
                events.add(sb);
            }
        }
    }
}
