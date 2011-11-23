/*
 * $Id$
 */
package com.untangle.uvm.engine;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.logging.EventRepository;
import com.untangle.uvm.logging.ListEventFilter;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.util.TransactionWork;

/**
 * Implements a <code>EventCache</code> cache using a <Code>ListEventFilter</code> for filtering and doGetEvents.
 */
class SimpleEvent<E extends LogEvent> implements EventRepository<E>
{
    private final Logger logger = Logger.getLogger(getClass());

    private EventLoggerImpl<E> eventLogger;
    private final ListEventFilter<E> eventFilter;

    private final LinkedList<E> list = new LinkedList<E>();

    // constructors ----------------------------------------------------------

    SimpleEvent(ListEventFilter<E> eventFilter)
    {
        this.eventFilter = eventFilter;
    }

    public void setEventLogger(EventLoggerImpl<E> eventLogger)
    {
        this.eventLogger = eventLogger;
    }

    // EventRepository methods -----------------------------------------------

    public List<E> getEvents()
    {
        LoggingManagerImpl lm = UvmContextImpl.getInstance().loggingManager();

        doGetEvents();

        return new ArrayList<E>(list);
    }

    public List<E> getEvents(int limit)
    {
        List<E> events = getEvents();
        int maxLen = Math.min(limit, EventRepository.MAX_SIZE);
        if(events.size() > maxLen) {
            events = events.subList(0, maxLen);
        }
        return new ArrayList<E>(events);
        
    }
    
    public RepositoryDesc getRepositoryDesc()
    {
        return eventFilter.getRepositoryDesc();
    }

    // private methods -------------------------------------------------------

    private void doGetEvents()
    {
        synchronized (list) {
            final NodeContext tctx = eventLogger.getNodeContext();

            logger.warn("doGetEvents for " + tctx);

            TransactionWork<Void> tw = new TransactionWork<Void>()
                {
                    public boolean doWork(Session s) throws SQLException
                    {
                        Map<String,Object> params;
                        if (null != tctx) {
                            Policy policy = tctx.getNodeId().getPolicy();
                            if (policy != null) {
                                params = new HashMap<String,Object>();
                                params.put("policy", (Object)policy);
                                params.put("policyId", (Object)policy.getId());
                            } else {
                                params = Collections.emptyMap();
                            }
                        } else {
                            params = Collections.emptyMap();
                        }

                        eventFilter.doGetEvents(s, list, MAX_SIZE, params);

                        return true;
                    }
                };

            if (null == tctx) {
                UvmContextFactory.context().runTransaction(tw);
            } else {
                tctx.runTransaction(tw);
            }

            Collections.sort(list);
            String last = null;
            for (Iterator<E> i = list.iterator(); i.hasNext(); ) {
                E e = i.next();
                String id = e.getId();
                if (null == id) {
                    id = Integer.toString(System.identityHashCode(e));
                }

                if (null == last ? last == id : last.equals(id)) {
                    // XXX we usually use linked lists, otherwise
                    // this is bad, probably better to make a new list
                    i.remove();
                } else {
                    last = id;
                }
            }
        }
    }
}
