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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.json.JSONException;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.NodeException;
import com.untangle.uvm.node.NodeStartException;
import com.untangle.uvm.node.NodeState;
import com.untangle.uvm.node.NodeStopException;
import com.untangle.uvm.user.ADLoginEvent;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.AbstractNode;
import com.untangle.uvm.vnet.PipeSpec;

public class CPDImpl extends AbstractNode implements CPD {
    private final Logger logger = Logger.getLogger(CPDImpl.class);

    private final PipeSpec[] pipeSpecs;

    private final PhoneBookAssistant phoneBookAssistant;
    
    private final EventLogger<ADLoginEvent> loginEventLogger;
    private final EventLogger<BlockEvent> blockEventLogger;
    
    private final CPDManager manager = new CPDManager(this);

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

        try {
            setCPDSettings(settings);
        } catch (NodeException e) {
            logger.error( "Unable to initialize the settings", e );
            throw new IllegalStateException("Error initializing cpd", e);
        }
    }

    // TestNode methods --------------------------------------------------

    @Override
    public void setCPDSettings(final CPDSettings settings) throws NodeException {
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
    public void setBaseSettings(final CPDBaseSettings baseSettings) throws NodeException
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
    public void setCaptureRules( final List<CaptureRule> captureRules ) throws NodeException
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
    public void setPassedClients( final List<PassedClient> newValue ) throws NodeException
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
    public void setPassedServers( final List<PassedServer> newValue ) throws NodeException
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
    
    public void setAll( final CPDBaseSettings baseSettings, 
            final List<CaptureRule> captureRules,
            final List<PassedClient> passedClients, 
            final List<PassedServer> passedServers ) throws NodeException
    {
        TransactionWork<Void> tw = new TransactionWork<Void>()
        {
            public boolean doWork(Session s)
            {
                if ( baseSettings != null ) {
                    CPDImpl.this.settings.setBaseSettings( baseSettings );
                }
                
                if ( captureRules != null ) {
                    CPDImpl.this.settings.setCaptureRules(captureRules);
                }
                
                if ( passedClients != null ) {
                    CPDImpl.this.settings.setPassedClients( passedClients );
                }

                if ( passedServers != null ) {
                    CPDImpl.this.settings.setPassedServers( passedServers );
                }
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

    @Override
    protected void preStart() throws NodeStartException
    {
        reconfigure(true);
           
        super.preStart();
     }
    
    @Override
    protected void preStop() throws NodeStopException
    {
        LocalUvmContextFactory.context().localPhoneBook().unregisterAssistant(this.phoneBookAssistant);
    }
    
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
    private void reconfigure() throws NodeException
    {
        reconfigure(false);
    }
    
    private void reconfigure(boolean force) throws NodeStartException {
        if ( force || this.getRunState() == NodeState.RUNNING) {
            try {
                this.manager.setConfig(this.settings);
            } catch (JSONException e) {
                throw new NodeStartException( "Unable to convert the JSON while setting the configuration.", e);
            } catch (IOException e) {
                throw new NodeStartException( "Unable to write settings.", e);
            }
            
            LocalUvmContextFactory.context().localPhoneBook().registerAssistant(this.phoneBookAssistant);
            
            this.manager.start();
        } else {
            this.manager.stop();
        }
    }

    PhoneBookAssistant getPhoneBookAssistant() {
        return this.phoneBookAssistant;
    }
}
