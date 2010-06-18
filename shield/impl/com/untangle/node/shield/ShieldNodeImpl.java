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
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.untangle.node.util.PartialListUtil;
import com.untangle.uvm.IntfEnum;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.NodeStartException;
import com.untangle.uvm.node.NodeState;
import com.untangle.uvm.node.NodeStopException;
import com.untangle.uvm.shield.ShieldRejectionEvent;
import com.untangle.uvm.shield.ShieldStatisticEvent;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.AbstractNode;
import com.untangle.uvm.vnet.PipeSpec;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

public class ShieldNodeImpl extends AbstractNode  implements ShieldNode

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

    private final PipeSpec pipeSpec[] = new PipeSpec[0];

    private ShieldSettings settings;

    private final PartialListUtil listUtil = new PartialListUtil();

    private final ShieldManager shieldManager;

    public ShieldNodeImpl()
    {
        NodeContext tctx = getNodeContext();
        EventLogger<ShieldStatisticEvent> sse = EventLoggerFactory.factory().getEventLogger(tctx);
        EventLogger<ShieldRejectionEvent> sre = EventLoggerFactory.factory().getEventLogger(tctx);


        this.shieldManager = new ShieldManager( sse, sre );
    }

    public void setShieldSettings(final ShieldSettings settings)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    ShieldNodeImpl.this.settings = (ShieldSettings)s.merge(settings);
                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);

        if ( getRunState() == NodeState.RUNNING ) {
            try {
                this.shieldManager.start();
                this.shieldManager.blessUsers( this.settings );
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
    }

    protected void postStart() throws NodeStartException
    {
        validateSettings();
        try {
            this.shieldManager.start();
            this.shieldManager.blessUsers( this.settings );
        } catch ( Exception e ) {
            logger.error( "Error setting shield node rules", e );
        }
    }

    protected void postStop() throws NodeStopException
    {
        try {
            this.shieldManager.stop();
        } catch ( Exception e ) {
            logger.error( "Error setting shield node rules", e );
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

    public ShieldBaseSettings getBaseSettings() {
        return settings.getBaseSettings();
    }

    public void setBaseSettings(final ShieldBaseSettings baseSettings) {
        TransactionWork tw = new TransactionWork() {
            public boolean doWork(Session s) {
                settings.setBaseSettings(baseSettings);
                s.merge(settings);
                return true;
            }

            public Object getResult() {
                return null;
            }
        };
        getNodeContext().runTransaction(tw);
    }

    public List<ShieldNodeRule> getShieldNodeRules(int start, int limit,
            String... sortColumns) {
        return listUtil.getItems(
                "select ts.shieldNodeRules from ShieldSettings ts where ts.tid = :tid ",
                getNodeContext(), getTid(), start, limit, sortColumns);
    }

    public void updateShieldNodeRules(List<ShieldNodeRule> added,
            List<Long> deleted, List<ShieldNodeRule> modified) {

        updateRules(getShieldSettings().getShieldNodeRules(), added, deleted,
                modified);
    }

    /*
     * For this node, updateAll means update only the rules
     * @see com.untangle.node.shield.ShieldNode#updateAll(java.util.List[])
     */
    @SuppressWarnings("unchecked")
	public void updateAll(List[] shieldNodeRulesChanges) 
    {
        if (shieldNodeRulesChanges != null && shieldNodeRulesChanges.length >= 3) {
            updateShieldNodeRules(shieldNodeRulesChanges[0], shieldNodeRulesChanges[1], shieldNodeRulesChanges[2]);
        }

        if ( getRunState() == NodeState.RUNNING ) {
            try {
                this.shieldManager.start();
                this.shieldManager.blessUsers( this.settings );
            } catch ( Exception e ) {
                logger.error( "Error setting shield node rules", e );
            }
        }
    }

    private void updateRules(final Set<ShieldNodeRule> rules,
            final List<ShieldNodeRule> added, final List<Long> deleted,
            final List<ShieldNodeRule> modified) {

        TransactionWork tw = new TransactionWork() {
            public boolean doWork(Session s) {
                listUtil.updateCachedItems( rules, added, deleted, modified );

                settings = (ShieldSettings)s.merge(settings);

                return true;
            }

            public Object getResult() {
                return null;
            }
        };
        getNodeContext().runTransaction(tw);
    }

}
