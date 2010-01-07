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
package com.untangle.node.cpd;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.user.ADLoginEvent;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.util.Worker;
import com.untangle.uvm.util.WorkerRunner;
import com.untangle.uvm.vnet.AbstractNode;
import com.untangle.uvm.vnet.PipeSpec;

public class CPDImpl extends AbstractNode implements CPD {
    public static final long CACHE_DELAY_MS = 10l * 60l * 1000l;

    private final Logger logger = Logger.getLogger(CPDImpl.class);

    private final PipeSpec[] pipeSpecs;

    private final PhoneBookAssistant phoneBookAssistant;
    private final WorkerRunner worker = new WorkerRunner(new CacheMonitor(), null);
    
    private final EventLogger<ADLoginEvent> loginEventLogger;
    private final EventLogger<BlockEvent> blockEventLogger;

    private CPDSettings settings;

    // constructor ------------------------------------------------------------

    public CPDImpl() {
        NodeContext nodeContext = getNodeContext();
        this.loginEventLogger = EventLoggerFactory.factory().getEventLogger(nodeContext);
        this.blockEventLogger = EventLoggerFactory.factory().getEventLogger(nodeContext);
        
        this.settings = new CPDSettings();
        this.pipeSpecs = new PipeSpec[0];
        this.phoneBookAssistant = new PhoneBookAssistant();
    }

    public void initializeSettings() {
        CPDSettings settings = new CPDSettings(this.getTid());
        logger.info("Initializing Settings...");

        setCPDSettings(settings);
    }

    // TestNode methods --------------------------------------------------

    @Override
    public void setCPDSettings(final CPDSettings settings) {
        if ( settings == this.settings ) {
            throw new IllegalArgumentException("Unable to update original settings, set this.settings to null first.");
        }
        
        TransactionWork<Void> tw = new TransactionWork<Void>()
        {
            public boolean doWork(Session s)
            {
                s.saveOrUpdate(settings);
                CPDImpl.this.settings = settings;
                return true;
            }

            public Void getResult() {
                return null;
            }
        };
        getNodeContext().runTransaction(tw);
        
        reconfigure();
    }

    public CPDSettings getCPDSettings() {
        return this.settings;
    }
    
    @Override
    public CPDBaseSettings getBaseSettings()
    {
        CPDBaseSettings baseSettings = this.settings.getBaseSettings();
        if ( baseSettings == null ) {
            baseSettings = new CPDBaseSettings();
        }
        
        return baseSettings;        
    }
    
    @Override
    public void setBaseSettings(final CPDBaseSettings baseSettings)
    {
        TransactionWork<Void> tw = new TransactionWork<Void>()
        {
            public boolean doWork(Session s)
            {
                CPDImpl.this.settings.setBaseSettings( baseSettings );
                CPDImpl.this.settings = (CPDSettings)s.merge(settings);
                return true;
            }

            public Void getResult() {
                return null;
            }
        };
        getNodeContext().runTransaction(tw);
        
        reconfigure();
    }

    @Override
    public List<CaptureRule> getCaptureRules()
    {
        List<CaptureRule> captureRules = this.settings.getCaptureRules();
        if ( captureRules == null ) {
            captureRules = new LinkedList<CaptureRule>();
        }
        
        return captureRules;
    }
    
    @Override
    public void setCaptureRules( final List<CaptureRule> captureRules )
    {
        TransactionWork<Void> tw = new TransactionWork<Void>()
        {
            public boolean doWork(Session s)
            {
                CPDImpl.this.settings.setCaptureRules( captureRules );
                CPDImpl.this.settings = (CPDSettings)s.merge(settings);
                return true;
            }

            public Void getResult() {
                return null;
            }
        };
        getNodeContext().runTransaction(tw);
        
        reconfigure();
    }
    
    @Override
    public List<PassedClient> getPassedClients()
    {
        return this.settings.getPassedClients();
    }
    
    @Override
    public void setPassedClients( final List<PassedClient> newValue )
    {
        TransactionWork<Void> tw = new TransactionWork<Void>()
        {
            public boolean doWork(Session s)
            {
                CPDImpl.this.settings.setPassedClients( newValue );
                CPDImpl.this.settings = (CPDSettings)s.merge(settings);
                return true;
            }

            public Void getResult() {
                return null;
            }
        };
        getNodeContext().runTransaction(tw);
        
        reconfigure();
    }
    
    @Override
    public List<PassedServer> getPassedServers()
    {
        return this.settings.getPassedServers();
    }

    @Override
    public void setPassedServers( final List<PassedServer> newValue )
    {
        TransactionWork<Void> tw = new TransactionWork<Void>()
        {
            public boolean doWork(Session s)
            {
                CPDImpl.this.settings.setPassedServers( newValue );
                CPDImpl.this.settings = (CPDSettings)s.merge(settings);
                return true;
            }

            public Void getResult() {
                return null;
            }
        };
        getNodeContext().runTransaction(tw);
        
        reconfigure();
    }
    
    @Override
    public Map<String, String> getUserMap() {
        return this.phoneBookAssistant.getUserMap();
    }

    @Override
    public String registerUser(String addressString, String username,
            Date expirationDate) throws UnknownHostException {
        InetAddress address = InetAddress.getByName(addressString);

        return this.phoneBookAssistant.addOrUpdate(address, username,
                expirationDate);
    }

    @Override
    public String removeUser(String addressString) throws UnknownHostException {
        InetAddress address = InetAddress.getByName(addressString);
        return this.phoneBookAssistant.removeEntry(address);
    }
    
    @Override
    public EventManager<ADLoginEvent> getLoginEventManager()
    {
        return this.loginEventLogger;
    }
    
    @Override
    public EventManager<BlockEvent> getBlockEventManager()
    {
        return this.blockEventLogger;
    }


    // AbstractNode methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs() {
        return pipeSpecs;
    }

    // lifecycle --------------------------------------------------------------

    protected void postInit(final String[] args) {
        TransactionWork<Object> tw = new TransactionWork<Object>() {
            public boolean doWork(Session s) {
                Query q = s
                        .createQuery("from CPDSettings cs where cs.tid = :tid");
                q.setParameter("tid", getTid());

                CPDImpl.this.settings = (CPDSettings) q.uniqueResult();
                return true;
            }

            public Object getResult() {
                return null;
            }
        };
        getNodeContext().runTransaction(tw);
    }

    // private methods -------------------------------------------------------
    
    private void reconfigure() {
        // TODO Auto-generated method stub
        
    }


    private class CacheMonitor implements Worker
    {
        @Override
        public void start() {
            // nothing special
        }

        @Override
        public void stop() {
            // Nothing special
        }

        @Override
        public void work() throws InterruptedException {
            Thread.sleep( CACHE_DELAY_MS );
            
            CPDImpl.this.phoneBookAssistant.clearExpiredData();
        }
        
    }
}
