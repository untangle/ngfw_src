/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.http;

import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.AbstractNode;
import com.untangle.uvm.vnet.CasingPipeSpec;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;

/**
 * An HTTP casing node.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class HttpNodeImpl extends AbstractNode
    implements HttpNode
{
    private final CasingPipeSpec pipeSpec = new CasingPipeSpec("http", this, new HttpCasingFactory(this), Fitting.HTTP_STREAM, Fitting.HTTP_TOKENS);
    private final PipeSpec[] pipeSpecs = new PipeSpec[] { pipeSpec };

    private final EventLogger<LogEvent> eventLogger;

    private HttpSettings settings;

    // constructors -----------------------------------------------------------

    public HttpNodeImpl()
    {
        this.eventLogger = EventLoggerFactory.factory().getEventLogger(getNodeContext());
    }

    // HttpNode methods --------------------------------------------------

    public HttpSettings getHttpSettings()
    {
        return settings;
    }

    public void setHttpSettings(final HttpSettings settings)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.merge(settings);
                    HttpNodeImpl.this.settings = settings;
                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);

        reconfigure();
    }

    private void reconfigure()
    {
        if (null != settings) {
            pipeSpec.setEnabled(settings.isEnabled());
            pipeSpec.setReleaseParseExceptions(!settings.isNonHttpBlocked());
        }
    }

    // Node methods ------------------------------------------------------

    protected void postInit(String[] args)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from HttpSettings hbs");
                    settings = (HttpSettings)q.uniqueResult();

                    if (null == settings) {
                        settings = new HttpSettings();
                        s.save(settings);
                    }

                    reconfigure();
                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);
    }

    // AbstractNode methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    // package protected methods ----------------------------------------------

    void log(LogEvent le)
    {
        eventLogger.log(le);
    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        return getHttpSettings();
    }

    public void setSettings(Object settings)
    {
        setHttpSettings((HttpSettings)settings);
    }
}
