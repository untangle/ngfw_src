/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.airgap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Collections;

import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tran.TransformStats;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

import com.metavize.mvvm.tran.TransformStopException;
import com.metavize.mvvm.tran.TransformStartException;

import com.metavize.mvvm.ArgonManager;
import com.metavize.mvvm.MvvmContextFactory;

import com.metavize.mvvm.shield.ShieldNodeRule;

import com.metavize.mvvm.tran.TransformState;

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

    private AirgapSettings settings;

    // We keep a stats around so we don't have to create one each time.
    private FakeTransformStats fakeStats;

    public AirgapTransformImpl() {}

    public void setAirgapSettings(AirgapSettings settings)
    {
        Session s = getTransformContext().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.saveOrUpdate(settings);
            this.settings = settings;

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn(exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn(exn); // XXX TransExn
            }
        }

        if ( getRunState() == TransformState.RUNNING ) {
            ArgonManager argonManager = MvvmContextFactory.context().argonManager();
            
            try {
                argonManager.setShieldNodeRules( this.settings.getShieldNodeRuleList());
            } catch ( Exception e ) {
                logger.error( "Error setting shield node rules", e );
            }
        }
    }

    public AirgapSettings getAirgapSettings()
    {
        return settings;
    }

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return new PipeSpec[0];
    }

    protected void initializeSettings()
    {
        AirgapSettings settings = new AirgapSettings(this.getTid());
        logger.info("Initializing Settings...");
        setAirgapSettings( settings);
    }

    protected void postInit(String[] args)
    {
        Session s = getTransformContext().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery
                ("from AirgapSettings ts where ts.tid = :tid");
            q.setParameter("tid", getTid());
            this.settings = (AirgapSettings)q.uniqueResult();

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn(exn);
        } finally {
            try {
                if (null != s) {
                    s.close();
                }
            } catch (HibernateException exn) {
                logger.warn(exn);
            }
        }
    }

    public TransformStats getStats() throws IllegalStateException
    {
        fakeStats.update();
        return fakeStats;
    }

    public List<ShieldRejectionLogEntry> getLogs( int limit )
    {
        List<ShieldRejectionLogEntry> l = new ArrayList<ShieldRejectionLogEntry>(limit);

        Session s = getTransformContext().openSession();
        try {
            Connection c = s.connection();
            PreparedStatement ps = c.prepareStatement( SHIELD_REJECTION_EVENT_QUERY );
            ps.setInt( 1, limit );
            long l0 = System.currentTimeMillis();
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Date createDate   = new Date( rs.getTimestamp( CREATE_DATE_IDX ).getTime());
                String clientAddr = rs.getString( CLIENT_ADDR_IDX );

                /* XXX A Hack to convert the index to a number */
                String clientIntf = ( rs.getByte( CLIENT_INTF_IDX ) == 0 ) ? "External" : "Internal";

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
        } catch (SQLException exn) {
            logger.warn( "could not get events", exn );
        } catch (HibernateException exn) {
            logger.warn( "could not get events", exn );
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Hibernate session", exn);
            }
        }

        return l;
    }

    protected void preStart()
    {
        if (this.settings == null) {
            String[] args = {""};
            postInit(args);
        }
        
        fakeStats = new FakeTransformStats();
    }

    protected void postStart() throws TransformStartException
    {
        ArgonManager argonManager = MvvmContextFactory.context().argonManager();
                
        try {
            argonManager.setShieldNodeRules( this.settings.getShieldNodeRuleList());
        } catch ( Exception e ) {
            throw new TransformStartException( e );
        }
    }

    protected void postStop() throws TransformStopException
    {
        ArgonManager argonManager = MvvmContextFactory.context().argonManager();
        
        List<ShieldNodeRule> emptyList = Collections.emptyList();
        
        try {
            /* Deconfigure all of the nodes */
            argonManager.setShieldNodeRules( emptyList );
        } catch ( Exception e ) {
            throw new TransformStopException( e );
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
