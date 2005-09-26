/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.firewall;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.NetworkingConfiguration;
import com.metavize.mvvm.argon.SessionMatcher;
import com.metavize.mvvm.argon.SessionMatcherFactory;
import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.mvvm.tran.Direction;
import com.metavize.mvvm.tran.TransformException;
import com.metavize.mvvm.tran.TransformStartException;
import com.metavize.mvvm.tran.firewall.IPMatcher;
import com.metavize.mvvm.tran.firewall.IntfMatcher;
import com.metavize.mvvm.tran.firewall.PortMatcher;
import com.metavize.mvvm.tran.firewall.ProtocolMatcher;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class FirewallImpl extends AbstractTransform implements Firewall
{
    private static final String EVENT_QUERY_BASE
        = "SELECT create_date, was_blocked, "
        + "c_client_addr, c_client_port, s_server_addr, s_server_port, "
        + "policy_inbound AS incoming, rule_index "
        + "FROM pl_endp endp "
        + "JOIN tr_firewall_evt evt ON endp.session_id = evt.session_id "
        + "WHERE endp.policy_id = ? ";

    private static final String EVENT_QUERY
        = EVENT_QUERY_BASE
        + "ORDER BY create_date DESC LIMIT ?";

    private static final String EVENT_BLOCKED_QUERY
        = EVENT_QUERY_BASE
        + "AND was_blocked "
        + "ORDER BY create_date DESC LIMIT ?";

    private final EventHandler handler;
    private final SoloPipeSpec pipeSpec;
    private final SoloPipeSpec[] pipeSpecs;

    private final Logger logger = Logger.getLogger(FirewallImpl.class);

    private FirewallSettings settings = null;

    public FirewallImpl()
    {
        this.handler = new EventHandler( this );

        /* Have to figure out pipeline ordering, this should always
         * next to towards the outside */
        this.pipeSpec = new SoloPipeSpec
            ("firewall", this, handler, Fitting.OCTET_STREAM, Affinity.OUTSIDE,
             SoloPipeSpec.MAX_STRENGTH - 2);
        this.pipeSpecs = new SoloPipeSpec[] { pipeSpec };
    }

    // Firewall methods -------------------------------------------------------

    public FirewallSettings getFirewallSettings()
    {
        return settings;
    }

    public void setFirewallSettings(FirewallSettings settings)
    {
        Session s = getTransformContext().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.saveOrUpdate(settings);
            this.settings = settings;

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not get FirewallSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close hibernate sessino", exn);
            }
        }

        try {
            reconfigure();
        }
        catch (TransformException exn) {
            logger.error("Could not save Firewall settings", exn);
        }
    }

    // Backwards compat
    public List<FirewallLog> getEventLogs(int limit)
    {
        return getEventLogs(limit, false);
    }

    public List<FirewallLog> getEventLogs(int limit, boolean blockedOnly)
    {
        List<FirewallLog> l = new ArrayList<FirewallLog>(limit);

        Session s = getTransformContext().openSession();
        try {
            Connection c = s.connection();
            PreparedStatement ps;
            if (blockedOnly)
                ps = c.prepareStatement(EVENT_BLOCKED_QUERY);
            else
                ps = c.prepareStatement(EVENT_QUERY);
            ps.setLong(1, getPolicy().getId());
            ps.setInt(2, limit);
            long l0 = System.currentTimeMillis();
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                long ts = rs.getTimestamp("create_date").getTime();
                Date createDate = new Date(ts);
                boolean trafficBlocker = rs.getBoolean("was_blocked");
                String clientAddr = rs.getString("c_client_addr");
                int clientPort = rs.getInt("c_client_port");
                String serverAddr = rs.getString("s_server_addr");
                int serverPort = rs.getInt("s_server_port");
                boolean incoming = rs.getBoolean("incoming");
                int ruleIndex = rs.getInt("rule_index");

                Direction d = incoming ? Direction.INCOMING : Direction.OUTGOING;

                FirewallLog rl = new FirewallLog
                    (createDate, trafficBlocker, clientAddr, clientPort,
                     serverAddr, serverPort, d, ruleIndex);

                l.add(rl);
            }
            long l1 = System.currentTimeMillis();
            logger.debug("getEventLogs() in: " + (l1 - l0));
        } catch (SQLException exn) {
            logger.warn("could not get events", exn);
        } catch (HibernateException exn) {
            logger.warn("could not get events", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Hibernate session", exn);
            }
        }

        return l;
    }

    // AbstractTransform methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    protected void initializeSettings()
    {
        logger.info("Initializing Settings...");

        FirewallSettings settings = getDefaultSettings();

        setFirewallSettings(settings);

        FirewallStatisticManager.getInstance().stop();
    }

    protected void postInit(String[] args)
    {
        Session s = getTransformContext().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery("from FirewallSettings hbs where hbs.tid = :tid");
            q.setParameter("tid", getTid());
            this.settings = (FirewallSettings)q.uniqueResult();

            updateToCurrent(this.settings);

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("Could not get FirewallSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Hibernate session", exn);
            }
        }
    }

    protected void preStart() throws TransformStartException
    {
        try {
            reconfigure();
        } catch (Exception e) {
            throw new TransformStartException(e);
        }

        FirewallStatisticManager.getInstance().start();
    }

    protected void postStart()
    {
        /* Kill all active sessions */
        shutdownMatchingSessions();
    }

    protected void postStop()
    {
        /* Kill all active sessions */
        shutdownMatchingSessions();

        FirewallStatisticManager.getInstance().stop();
    }

    public    void reconfigure() throws TransformException
    {
        FirewallSettings settings = getFirewallSettings();
        ArrayList enabledPatternsList = new ArrayList();

        logger.info("Reconfigure()");

        if (settings == null) {
            throw new TransformException("Failed to get Firewall settings: " + settings);
        }

        handler.configure( settings );
    }

    private   void updateToCurrent(FirewallSettings settings)
    {
        if (settings == null) {
            logger.error( "NULL Firewall Settings" );
            return;
        }

        logger.info( "Update Settings Complete" );
    }

    FirewallSettings getDefaultSettings()
    {
        logger.info( "Loading the default settings" );
        FirewallSettings settings = new FirewallSettings( this.getTid());

        /* Need this to lookup the local IP address */
        NetworkingConfiguration netConfig = MvvmContextFactory.context().networkingManager().get();

        try {
            /* Redirect settings */
            settings.setQuickExit( true );
            settings.setRejectSilently( true );
            settings.setDefaultAccept( true );

            List<FirewallRule> firewallList = new LinkedList<FirewallRule>();

            IPMatcher localHostMatcher = new IPMatcher( netConfig.host());



            FirewallRule tmp = new FirewallRule( false, ProtocolMatcher.MATCHER_ALL,
                                                 IntfMatcher.getOutside(), IntfMatcher.getAll(),
                                                 IPMatcher.MATCHER_ALL, IPMatcher.MATCHER_ALL,
                                                 PortMatcher.MATCHER_ALL, new PortMatcher( 21 ),
                                                 true );
            tmp.setLog( true );
            tmp.setDescription( "Block and log all incoming traffic destined to port 21 (FTP)" );
            firewallList.add( tmp );

            /* Block all traffic TCP traffic from the network 1.2.3.4/255.255.255.0 */
            tmp = new FirewallRule( false, ProtocolMatcher.MATCHER_TCP,
                                    IntfMatcher.getAll(), IntfMatcher.getAll(),
                                    IPMatcher.parse( "1.2.3.0/255.255.255.0" ), IPMatcher.MATCHER_ALL,
                                    PortMatcher.MATCHER_ALL, PortMatcher.MATCHER_ALL,
                                    true );
            tmp.setDescription( "Block all TCP traffic from 1.2.3.0 netmask 255.255.255.0" );
            firewallList.add( tmp );

            tmp = new FirewallRule( false, ProtocolMatcher.MATCHER_ALL,
                                    IntfMatcher.getAll(), IntfMatcher.getAll(),
                                    IPMatcher.MATCHER_ALL, IPMatcher.parse( "1.2.3.1-1.2.3.10" ),
                                    new PortMatcher( 1000, 5000 ), PortMatcher.MATCHER_ALL,
                                    false );
            tmp.setLog( true );
            tmp.setDescription( "Accept and log all traffic to the range 1.2.3.1 - 1.2.3.10 from ports 1000-5000" );
            firewallList.add( tmp );

            tmp = new FirewallRule( false, ProtocolMatcher.MATCHER_PING,
                                    IntfMatcher.getAll(), IntfMatcher.getAll(),
                                    IPMatcher.MATCHER_ALL, IPMatcher.parse( "1.2.3.1" ),
                                    PortMatcher.MATCHER_PING, PortMatcher.MATCHER_PING,
                                    false );
            tmp.setDescription( "Accept PINGs to 1.2.3.1.  Note: the source and destination ports are ignored." );
            firewallList.add( tmp );


            for ( Iterator<FirewallRule> iter = firewallList.iterator() ; iter.hasNext() ; ) {
                iter.next().setCategory( "[Sample]" );
            }

            settings.setFirewallRuleList( firewallList );

        } catch (Exception e ) {
            logger.error( "This should never happen", e );
        }

        return settings;
    }

    /* Kill all sessions when starting or stopping this transform */
    protected SessionMatcher sessionMatcher()
    {
        return SessionMatcherFactory.getAllInstance();
    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        return getFirewallSettings();
    }

    public void setSettings(Object settings)
    {
        setFirewallSettings((FirewallSettings)settings);
    }
}
