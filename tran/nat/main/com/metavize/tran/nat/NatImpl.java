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
package com.metavize.tran.nat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.NetworkingConfiguration;
import com.metavize.mvvm.NetworkingManager;
import com.metavize.mvvm.IntfEnum;

import com.metavize.mvvm.argon.SessionMatcher;
import com.metavize.mvvm.argon.SessionMatcherFactory;
import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.MPipe;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.mvvm.tran.Direction;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.TransformException;
import com.metavize.mvvm.tran.TransformStartException;
import com.metavize.mvvm.tran.TransformState;
import com.metavize.mvvm.tran.TransformStopException;
import com.metavize.mvvm.tran.firewall.IPMatcher;
import com.metavize.mvvm.tran.firewall.IntfMatcher;
import com.metavize.mvvm.tran.firewall.PortMatcher;
import com.metavize.mvvm.tran.firewall.ProtocolMatcher;
import com.metavize.tran.token.TokenAdaptor;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class NatImpl extends AbstractTransform implements Nat
{
    private static final String REDIRECT_EVENT_QUERY
        = "SELECT create_date, proto, c_client_addr, c_client_port, s_client_addr,"
        + " c_server_addr, c_server_port, s_server_addr, s_server_port,"
        + " client_intf, server_intf, rule_index, is_dmz "
        + " FROM pl_endp JOIN tr_nat_redirect_evt USING ( session_id )"
        + " ORDER BY create_date DESC LIMIT ?";

    private static final int CREATE_DATE_IDX   =  1;
    private static final int PROTO_IDX         =  2;
    private static final int O_CLIENT_ADDR_IDX =  3;
    private static final int O_CLIENT_PORT_IDX =  4;
    private static final int R_CLIENT_ADDR_IDX =  5;
    private static final int O_SERVER_ADDR_IDX =  6;
    private static final int O_SERVER_PORT_IDX =  7;
    private static final int R_SERVER_ADDR_IDX =  8;
    private static final int R_SERVER_PORT_IDX =  9;
    private static final int CLIENT_INTF_IDX   = 10;
    private static final int SERVER_INTF_IDX   = 11;
    private static final int RULE_INDEX_IDX    = 12;
    private static final int IS_DMZ_IDX        = 13;

    private final NatEventHandler handler;
    private final NatSessionManager sessionManager;
    final NatStatisticManager statisticManager;

    private final SoloPipeSpec natPipeSpec;
    private final SoloPipeSpec natFtpPipeSpec;

    private final PipeSpec[] pipeSpecs;
    
    private final DhcpManager dhcpManager;

    private final Logger logger = Logger.getLogger( NatImpl.class );

    private NatSettings settings = null;

    public NatImpl()
    {
        handler = new NatEventHandler(this);
        sessionManager = new NatSessionManager(this);
        dhcpManager = new DhcpManager( this );
        statisticManager = new NatStatisticManager();

        /* Have to figure out pipeline ordering, this should always next
         * to towards the outside */
        natPipeSpec = new SoloPipeSpec
            ("nat", this, handler, Fitting.OCTET_STREAM, Affinity.OUTSIDE,
             SoloPipeSpec.MAX_STRENGTH - 1);

        /* This subscription has to evaluate after NAT */
        natFtpPipeSpec = new SoloPipeSpec
            ("nat-ftp", this, new TokenAdaptor(this, new NatFtpFactory(this)),
             Fitting.FTP_TOKENS, Affinity.SERVER, 0);

        pipeSpecs = new SoloPipeSpec[] { natPipeSpec, natFtpPipeSpec };
    }

    public NatSettings getNatSettings()
    {
        if (settings.getDhcpEnabled()) {
            /* Insert all of the leases from DHCP */
            dhcpManager.loadLeases(settings);
        } else {
            /* remove any leftover leases from DHCP */
            dhcpManager.fleeceLeases(settings);
        }

        return this.settings;
    }

    public void setNatSettings(NatSettings settings) throws Exception
    {
        /* Remove all of the non-static addresses before saving */
        dhcpManager.fleeceLeases( settings );

        /* Validate the settings */
        try {
            settings.validate();
        }
        catch (Exception e) {
            logger.error("Invalid NAT settings", e);
            throw e;
        }

        Session s = getTransformContext().openSession();

        try {
            Transaction tx = s.beginTransaction();

            s.saveOrUpdate(settings);
            this.settings = settings;

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("Could not get NatSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("Could not close hibernate session", exn);
            }
        }

        try {
            reconfigure();

            if (getRunState() == TransformState.RUNNING) {
                /* NAT configuration needs information from the
                 * networking settings. */
                NetworkingConfiguration netConfig = MvvmContextFactory.context().networkingManager().get();

                /* Have to configure DHCP before the handler */
                dhcpManager.configure(settings, netConfig);
                this.handler.configure(settings, netConfig);
                dhcpManager.startDnsMasq();
            }
        } catch (TransformException exn) {
            logger.error( "Could not save Nat settings", exn );
        }
    }

    public List<NatRedirectLogEntry> getLogs( int limit )
    {
        List<NatRedirectLogEntry> l = new ArrayList<NatRedirectLogEntry>(limit);

        NetworkingManager networkingManager = MvvmContextFactory.context().networkingManager();
        IntfEnum intfEnum = networkingManager.getIntfEnum();

        Session s = getTransformContext().openSession();
        try {
            Connection c = s.connection();
            PreparedStatement ps = c.prepareStatement( REDIRECT_EVENT_QUERY );
            ps.setInt( 1, limit );
            long l0 = System.currentTimeMillis();
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Date createDate           = new Date( rs.getTimestamp( CREATE_DATE_IDX ).getTime());
                String clientAddr         = rs.getString( O_CLIENT_ADDR_IDX );
                boolean isNatd            = !clientAddr.equalsIgnoreCase( rs.getString( R_CLIENT_ADDR_IDX ));
                int    clientPort         = rs.getInt( O_CLIENT_PORT_IDX );
                String originalServerAddr = rs.getString( O_SERVER_ADDR_IDX );
                int    originalServerPort = rs.getInt( O_SERVER_PORT_IDX);
                String redirectServerAddr = rs.getString( R_SERVER_ADDR_IDX );
                int    redirectServerPort = rs.getInt( R_SERVER_PORT_IDX );
                Protocol proto            = Protocol.getInstance( rs.getInt( PROTO_IDX ));
                /* Just in case, it is null */
                String protocol = ( proto == null ) ? "UNK" : proto.toString();

                /* XXX Dirty ICMP hack */
                if ( clientPort == 0 ) protocol = "Ping";

                /* Set the direction */
                String clientIntf           = intfEnum.getIntfName( rs.getByte( CLIENT_INTF_IDX ));
                String serverIntf           = intfEnum.getIntfName( rs.getByte( SERVER_INTF_IDX ));
                
                /* Determine the reason
                 * The rule index is presently ignored, because the rule may have already
                 * been modified, which could be confusing ot the user
                 */
                boolean isDmz             = rs.getBoolean( IS_DMZ_IDX );
                int ruleIndex             = rs.getInt( RULE_INDEX_IDX );

                NatRedirectLogEntry redirectLogEntry = new NatRedirectLogEntry
                    ( createDate, protocol, clientAddr, clientPort, isNatd,
                      originalServerAddr, originalServerPort, redirectServerAddr, redirectServerPort,
                      clientIntf, serverIntf, isDmz, ruleIndex );

                l.add(redirectLogEntry );
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

    // package protected methods ----------------------------------------------

    NatEventHandler getHandler()
    {
        return handler;
    }

    MPipe getNatMPipe()
    {
        return natPipeSpec.getMPipe();
    }

    MPipe getNatFtpPipeSpec()
    {
        return natFtpPipeSpec.getMPipe();
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

        NatSettings settings = getDefaultSettings();

        /* Disable everything */

        /* deconfigure the event handle and the dhcp manager */
        dhcpManager.deconfigure();

        try {
            setNatSettings(settings);
            handler.deconfigure();
        } catch( Exception e ) {
            logger.error( "Unable to set Nat Settings", e );
        }

        /* Stop the statistics manager */
        statisticManager.stop();
    }

    protected void postInit(String[] args)
    {
        Session s = getTransformContext().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery("from NatSettings hbs where hbs.tid = :tid");
            q.setParameter("tid", getTid());
            this.settings = (NatSettings)q.uniqueResult();

            updateToCurrent(this.settings);

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("Could not get NatSettings", exn);
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

        /* NAT configuration needs information from the networking settings. */
        NetworkingConfiguration netConfig = MvvmContextFactory.context()
            .networkingManager().get();


        dhcpManager.configure( settings, netConfig );
        try {
            handler.configure( settings, netConfig );
        } catch ( TransformException e ) {
            throw new TransformStartException(e);
        }
        dhcpManager.startDnsMasq();

        statisticManager.start();
    }

    protected void postStart()
    {
        /* Kill all active sessions */
        shutdownMatchingSessions();
    }

    protected void postStop() throws TransformStopException
    {
        /* Kill all active sessions */
        shutdownMatchingSessions();

        dhcpManager.deconfigure();

        /* deconfigure the event handle */
        try {
            handler.deconfigure();
        } catch ( TransformException e ) {
            throw new TransformStopException( e );
        }

        statisticManager.stop();
    }

    public void reconfigure() throws TransformException
    {
        NatSettings settings = getNatSettings();

        logger.info("Reconfigure()");

        /* ????, what goes here. Configure the handler */

        /* Settings will always be null right now */
        if (settings == null) throw new TransformException("Failed to get Nat settings: " + settings);
    }

    private void updateToCurrent(NatSettings settings)
    {
        if (settings == null) {
            logger.error("NULL Nat Settings");
        } else {
            logger.info("Update Settings Complete");
        }
    }

    /* Kill all sessions when starting or stopping this transform */
    protected SessionMatcher sessionMatcher()
    {
        return SessionMatcherFactory.getAllInstance();
    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        return getNatSettings();
    }

    public void setSettings(Object settings) throws Exception
    {
        setNatSettings((NatSettings)settings);
    }

    private NatSettings getDefaultSettings()
    {
        logger.info( "Using default settings" );

        NatSettings settings = new NatSettings( this.getTid());

        List<RedirectRule> redirectList = new LinkedList<RedirectRule>();

        try {
            settings.setNatEnabled( true );
            settings.setNatInternalAddress( IPaddr.parse( "192.168.1.1" ));
            settings.setNatInternalSubnet( IPaddr.parse( "255.255.255.0" ));

            settings.setDmzLoggingEnabled( false );

            /* DMZ Settings */
            settings.setDmzEnabled( false );
            /* A sample DMZ */
            settings.setDmzAddress( IPaddr.parse( "192.168.1.2" ));

            RedirectRule tmp = new RedirectRule( false, ProtocolMatcher.MATCHER_ALL,
                                                 IntfMatcher.getOutside(), IntfMatcher.getAll(),
                                                 IPMatcher.MATCHER_ALL, IPMatcher.MATCHER_LOCAL,
                                                 PortMatcher.MATCHER_ALL, new PortMatcher( 8080 ),
                                                 true, IPaddr.parse( "192.168.1.16" ), 80 );
            tmp.setDescription( "Redirect incoming traffic to EdgeGuard port 8080 to port 80 on 192.168.1.16" );
            tmp.setLog( true );

            redirectList.add( tmp );

            tmp = new RedirectRule( false, ProtocolMatcher.MATCHER_ALL,
                                    IntfMatcher.getOutside(), IntfMatcher.getAll(),
                                    IPMatcher.MATCHER_ALL, IPMatcher.MATCHER_ALL,
                                    PortMatcher.MATCHER_ALL, new PortMatcher( 6000, 10000 ),
                                    true, (IPaddr)null, 6000 );
            tmp.setDescription( "Redirect incoming traffic from ports 6000-10000 to port 6000" );
            redirectList.add( tmp );

            tmp = new RedirectRule( false, ProtocolMatcher.MATCHER_ALL,
                                    IntfMatcher.getInside(), IntfMatcher.getAll(),
                                    IPMatcher.MATCHER_ALL, IPMatcher.parse( "1.2.3.4" ),
                                    PortMatcher.MATCHER_ALL, PortMatcher.MATCHER_ALL,
                                    true, IPaddr.parse( "4.3.2.1" ), 0 );
            tmp.setDescription( "Redirect outgoing traffic going to 1.2.3.4 to 4.2.3.1, (port is unchanged)" );
            tmp.setLog( true );

            redirectList.add( tmp );


            for ( Iterator<RedirectRule> iter = redirectList.iterator() ; iter.hasNext() ; ) {
                iter.next().setCategory( "[Sample]" );
            }

            /* Enable DNS and DHCP */
            settings.setDnsEnabled( true );
            settings.setDhcpEnabled( true );

            settings.setDhcpStartAndEndAddress( IPaddr.parse( "192.168.1.100" ),
                                                IPaddr.parse( "192.168.1.200" ));


        } catch ( Exception e ) {
            logger.error( "This should never happen", e );
        }

        settings.setRedirectList( redirectList );

        return settings;
    }

    private NatSettings getTestSettings()
    {
        logger.info( "Using the test settings" );

        NatSettings settings = new NatSettings( this.getTid());

        /* Need this to lookup the local IP address */
        NetworkingConfiguration netConfig = MvvmContextFactory.context().networkingManager().get();

        try {
            /* Nat settings */
            settings.setNatEnabled( true );
            settings.setNatInternalAddress( IPaddr.parse( "192.168.1.1" ));
            settings.setNatInternalSubnet( IPaddr.parse( "255.255.255.0" ));

            /* DMZ Settings */
            settings.setDmzEnabled( true );
            settings.setDmzAddress( IPaddr.parse( "192.168.1.4" ));

            /* Redirect settings */
            IPaddr redirectHost        = IPaddr.parse( "192.168.1.6" );
            IPMatcher localHostMatcher = new IPMatcher( netConfig.host());

            List redirectList = new LinkedList();

            /* This rule is enabled, redirect port 7000 to the redirect host port 7 */
            redirectList.add( new RedirectRule( true, ProtocolMatcher.MATCHER_ALL,
                                                IntfMatcher.getOutside(), IntfMatcher.getAll(),
                                                IPMatcher.MATCHER_ALL, localHostMatcher,
                                                PortMatcher.MATCHER_ALL, new PortMatcher( 7000 ),
                                                true, redirectHost, 7 ));

            /* This rule is disabled, to verify the on off switch works */
            redirectList.add( new RedirectRule( false, ProtocolMatcher.MATCHER_ALL,
                                                IntfMatcher.getInside(), IntfMatcher.getInside(),
                                                IPMatcher.MATCHER_ALL, localHostMatcher,
                                                PortMatcher.MATCHER_ALL, new PortMatcher( 5901 ),
                                                true, redirectHost, 5900 ));

            settings.setRedirectList( redirectList );

            /* Enable DNS and DHCP */
            settings.setDnsEnabled( true );
            settings.setDhcpEnabled( true );

            /* Quick sanity test, remove in a second */
            IPaddr tmp  = IPaddr.parse( "192.168.1.1" );
            IPaddr full = IPaddr.parse( "0.0.0.0" );

            if ( !tmp.isInNetwork( tmp, full ))
                throw new IllegalStateException( "isInNetwork is totally busted" );

            /* Set in reverse to test if it works */
            settings.setDhcpStartAndEndAddress( IPaddr.parse( "192.168.1.155" ),
                                                IPaddr.parse( "192.168.1.150" ));

        } catch (Exception e ) {
            logger.error( "This should never happen", e );
        }

        return settings;
    }

    NatSessionManager getSessionManager()
    {
        return sessionManager;
    }
}
