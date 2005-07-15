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
import com.metavize.mvvm.tapi.TransformContextFactory;
import com.metavize.mvvm.tran.Direction;
import com.metavize.mvvm.tran.TransformException;
import com.metavize.mvvm.tran.TransformStartException;
import com.metavize.mvvm.tran.firewall.IPMatcher;
import com.metavize.mvvm.tran.firewall.IntfMatcher;
import com.metavize.mvvm.tran.firewall.PortMatcher;
import com.metavize.mvvm.tran.firewall.ProtocolMatcher;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;

public class FirewallImpl extends AbstractTransform implements Firewall
{
    private static final String EVENT_QUERY
        = "SELECT create_date, is_traffic_blocker, "
        + "c_client_addr, c_client_port, s_server_addr, s_server_port, "
        + "client_intf, server_intf "
        + "FROM pl_endp endp "
        + "JOIN tr_firewall_evt evt ON endp.session_id = evt.session_id "
        + "JOIN firewall_rule rule ON evt.rule_id = rule.rule_id "
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
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.saveOrUpdateCopy(settings);
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

    public List<FirewallLog> getEventLogs(int limit)
    {
        List<FirewallLog> l = new LinkedList<FirewallLog>();

        Session s = TransformContextFactory.context().openSession();
        try {
            Connection c = s.connection();
            PreparedStatement ps = c.prepareStatement(EVENT_QUERY);
            ps.setInt(1, limit);
            long l0 = System.currentTimeMillis();
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                long ts = rs.getTimestamp("create_date").getTime();
                Date createDate = new Date(ts);
                boolean trafficBlocker = rs.getBoolean("is_traffic_blocker");
                String clientAddr = rs.getString("c_client_addr");
                int clientPort = rs.getInt("c_client_port");
                String serverAddr = rs.getString("s_server_addr");
                int serverPort = rs.getInt("s_server_port");
                byte clientIntf = rs.getByte("client_intf");
                byte serverIntf = rs.getByte("server_intf");

                Direction d = Direction.getDirection(clientIntf, serverIntf);

                FirewallLog rl = new FirewallLog
                    (createDate, trafficBlocker, clientAddr, clientPort,
                     serverAddr, serverPort, d);

                l.add(0, rl);
            }
            long l1 = System.currentTimeMillis();
            logger.debug("getActiveXLogs() in: " + (l1 - l0));
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
        Session s = TransformContextFactory.context().openSession();
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
                                                 IntfMatcher.MATCHER_OUT, IntfMatcher.MATCHER_ALL,
                                                 IPMatcher.MATCHER_ALL, IPMatcher.MATCHER_ALL,
                                                 PortMatcher.MATCHER_ALL, new PortMatcher( 21 ),
                                                 true );
            tmp.setDescription( "Block all incoming traffic destined to port 21 (FTP)" );
            firewallList.add( tmp );

            /* Block all traffic TCP traffic from the network 1.2.3.4/255.255.255.0 */
            tmp = new FirewallRule( false, ProtocolMatcher.MATCHER_TCP,
                                    IntfMatcher.MATCHER_ALL, IntfMatcher.MATCHER_ALL,
                                    IPMatcher.parse( "1.2.3.0/255.255.255.0" ), IPMatcher.MATCHER_ALL,
                                    PortMatcher.MATCHER_ALL, PortMatcher.MATCHER_ALL,
                                    true );
            tmp.setDescription( "Block all TCP traffic from 1.2.3.0 netmask 255.255.255.0" );
            firewallList.add( tmp );

            tmp = new FirewallRule( false, ProtocolMatcher.MATCHER_ALL,
                                    IntfMatcher.MATCHER_ALL, IntfMatcher.MATCHER_ALL,
                                    IPMatcher.MATCHER_ALL, IPMatcher.parse( "1.2.3.1-1.2.3.10" ),
                                    new PortMatcher( 1000, 5000 ), PortMatcher.MATCHER_ALL,
                                    false );
            tmp.setDescription( "Accept all traffic to the range 1.2.3.1 - 1.2.3.10 from ports 1000-5000" );
            firewallList.add( tmp );

            tmp = new FirewallRule( false, ProtocolMatcher.MATCHER_PING,
                                    IntfMatcher.MATCHER_ALL, IntfMatcher.MATCHER_ALL,
                                    IPMatcher.MATCHER_ALL, IPMatcher.parse( "1.2.3.1" ),
                                    PortMatcher.MATCHER_PING, PortMatcher.MATCHER_PING,
                                    true );
            tmp.setDescription( "Block PINGs to 1.2.3.1.  Note: the source and destination ports are ignored." );
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
