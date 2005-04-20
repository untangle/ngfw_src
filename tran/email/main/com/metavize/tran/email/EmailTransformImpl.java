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
package com.metavize.tran.email;

import java.io.FileOutputStream;
import java.io.PrintStream;

import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.Interface;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.mvvm.tapi.SoloTransform;
import com.metavize.mvvm.tapi.Subscription;
import com.metavize.mvvm.tapi.TransformContextFactory;
import com.metavize.mvvm.tran.IPMaddr;
import com.metavize.mvvm.tran.PortRange;
import com.metavize.mvvm.tran.TransformStartException;
import net.sf.hibernate.Hibernate;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;

public class EmailTransformImpl extends SoloTransform implements EmailTransform
{
    private static final Logger zLog = Logger.getLogger(EmailTransformImpl.class);

    public static final String SPAM_IN_CFG_FILE  = "spam.inbound.cf";
    public static final String SPAM_OUT_CFG_FILE = "spam.outbound.cf";

    private EventHandler handler;

    private final PipeSpec pipeSpec;

    private volatile EmailSettings settings;

    // constructors -----------------------------------------------------------

    public EmailTransformImpl()
    {
        Subscription smtpSub = new Subscription(Protocol.TCP, Interface.ANY, Interface.ANY,
                                                IPMaddr.anyAddr, PortRange.ANY,
                                                IPMaddr.anyAddr, new PortRange(25));
        Subscription pop3Sub = new Subscription(Protocol.TCP, Interface.ANY, Interface.ANY,
                                                IPMaddr.anyAddr, PortRange.ANY,
                                                IPMaddr.anyAddr, new PortRange(110));
        Subscription imap4Sub = new Subscription(Protocol.TCP, Interface.ANY, Interface.ANY,
                                                 IPMaddr.anyAddr, PortRange.ANY,
                                                 IPMaddr.anyAddr, new PortRange(143));
        pipeSpec = new SoloPipeSpec("email", smtpSub, Fitting.OCTET_STREAM, Affinity.CLIENT, 0);
        pipeSpec.addSubscription(pop3Sub);
        pipeSpec.addSubscription(imap4Sub);
    }

    // EmailTransform methods ----------------------------------------------------

    public void reconfigure()
    {
        if (handler != null) {
            zLog.info("reconfiguring email transform");

            /* re-build XMailScanner cache */
            XMailScannerCache zXMSCache = XMailScannerCache.build(this);
            if (null == zXMSCache)
            {
                zLog.error("Unable to reconfigure email transform with new settings; email transform will continue to use original settings");
                return;
            }

            /* re-new handler with new cache */
            handler.renew(zXMSCache);

            /* Update the spamassassin settings */
            writeSpamAssassinConfiguration();
        }

        return;
    }

    public EmailSettings getEmailSettings()
    {
        return settings;
    }

    public void setEmailSettings(EmailSettings settings)
    {
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.saveOrUpdateCopy(settings);
            this.settings = settings;

            tx.commit();
        } catch (HibernateException exn) {
            zLog.warn("could not get EmailSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                zLog.warn("could not close hibernate session", exn);
            }
        }

        reconfigure();
        return;
    }

    // SoloTransform methods --------------------------------------------------

    protected void initializeSettings()
    {
        SSCTLDefinition spamInboundCtl = new SSCTLDefinition();
        spamInboundCtl.setNotes("Default configuration for inbound spam control");
        // <alerts generateCriticalAlerts="false" generateSummaryAlerts="false"/>

        SSCTLDefinition spamOutboundCtl = new SSCTLDefinition();
        spamOutboundCtl.setScan(false);
        spamOutboundCtl.setNotes("Default configuration for outbound spam control");
        // <alerts generateCriticalAlerts="false" generateSummaryAlerts="false"/>

        VSCTLDefinition virusInboundCtl = new VSCTLDefinition();
        virusInboundCtl.setActionOnDetect(Action.BLOCK);
        virusInboundCtl.setNotes("Default configuration for inbound virus control");
        // <alerts generateCriticalAlerts="false" generateSummaryAlerts="false"/>

        VSCTLDefinition virusOutboundCtl = new VSCTLDefinition();
        virusOutboundCtl.setScan(false);
        virusOutboundCtl.setActionOnDetect(Action.PASS);
        virusOutboundCtl.setNotes("Default configuration for outbound virus control");
        // <alerts generateCriticalAlerts="false" generateSummaryAlerts="false"/>

        EmailSettings settings = new EmailSettings(getTid(), spamInboundCtl, spamOutboundCtl,
                                                   virusInboundCtl, virusOutboundCtl);
        setEmailSettings(settings);
        return;
    }

    protected PipeSpec getPipeSpec()
    {
        return pipeSpec;
    }

    protected void postInit(String[] args)
    {
        if (args.length > 0) {
            zLog.info("Arguments (" + args.length + ") : ");
            for (int i=0;i<args.length;i++) {
                System.out.print(" " + args[i] + " ");
            }
        }

        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery
                ("from EmailSettings es where es.tid = :tid");
            q.setParameter("tid", getTid());
            settings = (EmailSettings)q.uniqueResult();

            Hibernate.initialize(settings.getFilters());

            tx.commit();
        } catch (HibernateException exn) {
            zLog.warn("Could not get EmailSettings", exn);
            // XXXXX
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                zLog.warn("could not close Hibernate session", exn);
            }
        }
        zLog.debug("postinit");
        return;
    }

    protected void preStart() throws TransformStartException
    {
        zLog.info("starting and configuring email transform");

        /* build XMailScanner cache */
        XMailScannerCache zXMSCache = XMailScannerCache.build(this);
        if (null == zXMSCache)
        {
            throw new TransformStartException("Unable to configure email transform with settings; email transform cannot start");
        }

        handler = new EventHandler(zXMSCache);

        /* Write out the spamassassin settings */
        writeSpamAssassinConfiguration();

        getMPipe().setSessionEventListener(handler);
        return;
    }

    private void writeSpamAssassinConfiguration()
    {
        writeSpamAssassinFile( getSpamInboundConfigFile(),  settings.getSpamInboundCtl());
        writeSpamAssassinFile( getSpamOutboundConfigFile(), settings.getSpamOutboundCtl());
        return;
    }

    private void writeSpamAssassinFile( String fileName, SSCTLDefinition value )
    {
        try {
            FileOutputStream out = new FileOutputStream( fileName );

            // Connect print stream to the output stream
            PrintStream p = new PrintStream( out );

            p.println( "## AUTO GENERATED BY Metavize, DO NOT MANUALLY EDIT" );
            p.println( "required_score " + value.getScanStrengthValue());
        // Added 3/25/05 by jdi: turn off trusted network code:
            p.println( "score ALL_TRUSTED 0" );
        // Added 3/25/05 by jdi: optimization (no NFS)
            p.println( "lock_method flock" );
        // Added 3/25/05 by jdi: change the subject too
            p.println( "rewrite_header Subject ***SPAM(_SCORE( )_)***" );

            p.close();
        } catch (Exception e) {
            zLog.error( "Error writing spam assassin configuration file: " + fileName, e );
        }

        return;
    }

    public static String getSpamInboundConfigFile()
    {
        return getSpamConfigFile( SPAM_IN_CFG_FILE );
    }

    public static String getSpamOutboundConfigFile()
    {
        return getSpamConfigFile( SPAM_OUT_CFG_FILE );
    }

    private static String getSpamConfigFile( String fileName )
    {
        String baseDir = System.getProperty("bunnicula.conf.dir");

        /* Default to the root configuration */
        if ( baseDir == null )
            baseDir = "/usr/share/metavize/conf";

        return baseDir + "/" + fileName;
    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        return getEmailSettings();
    }

    public void setSettings(Object settings)
    {
        zLog.debug("SETTING EMAIL SETTINGS: ");
        setEmailSettings((EmailSettings)settings);
        return;
    }
}
