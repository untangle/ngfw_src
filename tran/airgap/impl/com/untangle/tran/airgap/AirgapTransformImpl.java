/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.tran.airgap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.untangle.mvvm.localapi.LocalShieldManager;
import com.untangle.mvvm.IntfEnum;
import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.shield.ShieldNodeSettings;
import com.untangle.mvvm.tapi.AbstractTransform;
import com.untangle.mvvm.tapi.PipeSpec;
import com.untangle.mvvm.tran.TransformStartException;
import com.untangle.mvvm.tran.TransformState;
import com.untangle.mvvm.tran.TransformStats;
import com.untangle.mvvm.tran.TransformStopException;
import com.untangle.mvvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

public class AirgapTransformImpl extends AbstractTransform
    implements AirgapTransform
{
    private static final String SHIELD_REJECTION_EVENT_QUERY
        = "SELECT time_stamp, client_addr, client_intf, reputation, limited, dropped, rejected"
        + " FROM shield_rejection_evt "
        + " ORDER BY time_stamp DESC LIMIT ?";

    private static final int CREATE_DATE_IDX =  1;
    private static final int CLIENT_ADDR_IDX =  2;
    private static final int CLIENT_INTF_IDX =  3;
    private static final int REPUTATION_IDX  =  4;
    private static final int LIMITED_IDX     =  5;
    private static final int DROPPED_IDX     =  6;
    private static final int REJECTED_IDX    =  7;

    private final Logger logger = Logger.getLogger(AirgapTransformImpl.class);

    private final List<ShieldNodeSettings> emptyList = Collections.emptyList();

    private final PipeSpec pipeSpec[] = new PipeSpec[0];

    private AirgapSettings settings;

    // We keep a stats around so we don't have to create one each time.
    private TransformStats fakeStats;

    public AirgapTransformImpl() {}

    public void setAirgapSettings(final AirgapSettings settings)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.merge(settings);
                    AirgapTransformImpl.this.settings = settings;
                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);

        if ( getRunState() == TransformState.RUNNING ) {
            LocalShieldManager lsm = MvvmContextFactory.context().localShieldManager();

            try {
                lsm.setShieldNodeSettings( this.settings.getShieldNodeRuleList());
            } catch ( Exception e ) {
                logger.error( "Error setting shield node rules", e );
            }
        }
    }

    public AirgapSettings getAirgapSettings()
    {
        validateSettings();
	if( settings == null )
	    logger.error("Settings not yet initialized. State: " + getTransformContext().getRunState() );
        return settings;
    }

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpec;
    }

    public void initializeSettings()
    {
        AirgapSettings settings = new AirgapSettings(this.getTid());
        logger.info("Initializing Settings...");
        setAirgapSettings( settings);
    }

    protected void postInit(String[] args)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery
                        ("from AirgapSettings ts where ts.tid = :tid");
                    q.setParameter("tid", getTid());
                    AirgapTransformImpl.this.settings = (AirgapSettings)q.uniqueResult();
                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);
    }

    public TransformStats getStats() throws IllegalStateException
    {
        FakeTransformStats.update(fakeStats);
        return fakeStats;
    }

    public List<ShieldRejectionLogEntry> getLogs( final int limit )
    {
        final List<ShieldRejectionLogEntry> l = new ArrayList<ShieldRejectionLogEntry>(limit);

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s) throws SQLException
                {
                    IntfEnum intfEnum = MvvmContextFactory.context().localIntfManager().getIntfEnum();

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
        getTransformContext().runTransaction(tw);

        return l;
    }

    protected void preStart()
    {
        validateSettings();

        fakeStats = new TransformStats();
    }

    protected void postStart() throws TransformStartException
    {
        validateSettings();
        LocalShieldManager lsm = MvvmContextFactory.context().localShieldManager();

        try {
            lsm.setShieldNodeSettings( this.settings.getShieldNodeRuleList());
        } catch ( Exception e ) {
            throw new TransformStartException( e );
        }
    }

    protected void postStop() throws TransformStopException
    {
        LocalShieldManager lsm = MvvmContextFactory.context().localShieldManager();

        try {
            /* Deconfigure all of the nodes */
            lsm.setShieldNodeSettings( this.emptyList );
        } catch ( Exception e ) {
            throw new TransformStopException( e );
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
        return getAirgapSettings();
    }

    public void setSettings(Object settings)
    {
        setAirgapSettings((AirgapSettings)settings);
    }
}
