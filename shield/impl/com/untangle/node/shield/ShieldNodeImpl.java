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
package com.untangle.node.shield;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.untangle.uvm.IntfEnum;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.localapi.LocalShieldManager;
import com.untangle.uvm.shield.ShieldNodeSettings;
import com.untangle.uvm.vnet.AbstractNode;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.node.NodeStartException;
import com.untangle.uvm.node.NodeState;
import com.untangle.uvm.node.NodeStats;
import com.untangle.uvm.node.NodeStopException;
import com.untangle.uvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

public class ShieldNodeImpl extends AbstractNode
    implements ShieldNode
{
    private static final String SHIELD_REJECTION_EVENT_QUERY
        = "SELECT time_stamp, client_addr, client_intf, reputation, limited, dropped, rejected"
        + " FROM n_shield_rejection_evt "
        + " ORDER BY time_stamp DESC LIMIT ?";

    private static final int CREATE_DATE_IDX =  1;
    private static final int CLIENT_ADDR_IDX =  2;
    private static final int CLIENT_INTF_IDX =  3;
    private static final int REPUTATION_IDX  =  4;
    private static final int LIMITED_IDX     =  5;
    private static final int DROPPED_IDX     =  6;
    private static final int REJECTED_IDX    =  7;

    private final Logger logger = Logger.getLogger(ShieldNodeImpl.class);

    private final List<ShieldNodeSettings> emptyList = Collections.emptyList();

    private final PipeSpec pipeSpec[] = new PipeSpec[0];

    private ShieldSettings settings;

    // We keep a stats around so we don't have to create one each time.
    private NodeStats fakeStats;

    public ShieldNodeImpl() {}

    public void setShieldSettings(final ShieldSettings settings)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.merge(settings);
                    ShieldNodeImpl.this.settings = settings;
                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);

        if ( getRunState() == NodeState.RUNNING ) {
            LocalShieldManager lsm = LocalUvmContextFactory.context().localShieldManager();

            try {
                lsm.setShieldNodeSettings( this.settings.getShieldNodeRuleList());
            } catch ( Exception e ) {
                logger.error( "Error setting shield node rules", e );
            }
        }
    }

    public ShieldSettings getShieldSettings()
    {
        validateSettings();
        if( settings == null )
            logger.error("Settings not yet initialized. State: " + getNodeContext().getRunState() );
        return settings;
    }

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpec;
    }

    public void initializeSettings()
    {
        ShieldSettings settings = new ShieldSettings(this.getTid());
        logger.info("Initializing Settings...");
        setShieldSettings( settings);
    }

    protected void postInit(String[] args)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery
                        ("from ShieldSettings ts where ts.tid = :tid");
                    q.setParameter("tid", getTid());
                    ShieldNodeImpl.this.settings = (ShieldSettings)q.uniqueResult();
                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);
    }

    public NodeStats getStats() throws IllegalStateException
    {
        FakeNodeStats.update(fakeStats);
        return fakeStats;
    }

    public List<ShieldRejectionLogEntry> getLogs( final int limit )
    {
        final List<ShieldRejectionLogEntry> l = new ArrayList<ShieldRejectionLogEntry>(limit);

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s) throws SQLException
                {
                    IntfEnum intfEnum = LocalUvmContextFactory.context().localIntfManager().getIntfEnum();

                    Connection c = s.connection();
                    PreparedStatement ps = c.prepareStatement( SHIELD_REJECTION_EVENT_QUERY );
                    ps.setInt( 1, limit );
                    long l0 = System.currentTimeMillis();
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        Date createDate   = new Date( rs.getTimestamp( CREATE_DATE_IDX ).getTime());
                        String clientAddr = rs.getString( CLIENT_ADDR_IDX );

                        String clientIntf = ( intfEnum.getIntfName( rs.getByte( CLIENT_INTF_IDX )));
                        if ( clientIntf == null ) clientIntf = "unknown";

                        double reputation = rs.getDouble( REPUTATION_IDX );
                        int limited       = rs.getInt( LIMITED_IDX );
                        int dropped       = rs.getInt( DROPPED_IDX );
                        int rejected      = rs.getInt( REJECTED_IDX );

                        ShieldRejectionLogEntry entry = new ShieldRejectionLogEntry
                            ( createDate, clientAddr, clientIntf, reputation, limited, dropped, rejected );

                        l.add(entry);
                    }
                    long l1 = System.currentTimeMillis();
                    logger.debug( "getAccessLogs() in: " + ( l1 - l0 ));
                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);

        return l;
    }

    protected void preStart()
    {
        validateSettings();

        fakeStats = new NodeStats();
    }

    protected void postStart() throws NodeStartException
    {
        validateSettings();
        LocalShieldManager lsm = LocalUvmContextFactory.context().localShieldManager();

        try {
            lsm.setShieldNodeSettings( this.settings.getShieldNodeRuleList());
        } catch ( Exception e ) {
            throw new NodeStartException( e );
        }
    }

    protected void postStop() throws NodeStopException
    {
        LocalShieldManager lsm = LocalUvmContextFactory.context().localShieldManager();

        try {
            /* Deconfigure all of the nodes */
            lsm.setShieldNodeSettings( this.emptyList );
        } catch ( Exception e ) {
            throw new NodeStopException( e );
        }
    }

    /* This just checks if the settings are null, tries to load them from the database
     * if they are still null it will then initialize a new set of settings */
    private void validateSettings()
    {
        if (this.settings == null) {
            String args[] = { "" };
            postInit( args );
            /* If the settings are still null, initialize them */
            if ( this.settings == null ) initializeSettings();
        }
    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        return getShieldSettings();
    }

    public void setSettings(Object settings)
    {
        setShieldSettings((ShieldSettings)settings);
    }
}
