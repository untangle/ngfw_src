/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: VirusTransformImpl.java,v 1.20 2005/03/12 01:54:30 amread Exp $
 */
package com.metavize.tran.virus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.argon.SessionMatcher;
import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.IPSessionDesc;
import com.metavize.mvvm.tapi.Interface;
import com.metavize.mvvm.tapi.MPipe;
import com.metavize.mvvm.tapi.MPipeManager;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.PipelineFoundry;
import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tapi.Subscription;
import com.metavize.mvvm.tapi.TransformContextFactory;
import com.metavize.mvvm.tapi.event.SessionEventListener;
import com.metavize.mvvm.tran.MimeType;
import com.metavize.mvvm.tran.MimeTypeRule;
import com.metavize.mvvm.tran.StringRule;
import com.metavize.tran.token.TokenAdaptor;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;

public class VirusTransformImpl extends AbstractTransform
    implements VirusTransform
{
    private static final PipelineFoundry FOUNDRY = MvvmContextFactory.context()
        .pipelineFoundry();

    private static final int FTP_COMMAND = 0;
    private static final int FTP_DATA = 1;
    private static final int HTTP = 2;

    private final VirusScanner scanner;

    private final PipeSpec[] pipeSpecs = new PipeSpec[]
        { new PipeSpec("virus-ftp", Fitting.FTP_STREAM, Affinity.END),
          new PipeSpec("virus-ftp-data", Fitting.OCTET_STREAM, Affinity.END),
          new PipeSpec("virus-http", Fitting.HTTP_TOKENS, Affinity.END) };

    private final MPipe[] mPipes = new MPipe[3];
    private final SessionEventListener[] listeners = new SessionEventListener[]
        { new FtpCommandHandler(this), new FtpDataHandler(this),
          new TokenAdaptor(new VirusHttpFactory(this)) };
    private final Logger logger = Logger.getLogger(VirusTransformImpl.class);

    private VirusSettings settings;

    private SessionMatcher VIRUS_SESSION_MATCHER = new SessionMatcher() {
            /* Kill all sessions on ports 20, 21 and 80 */
            public boolean isMatch( com.metavize.mvvm.argon.IPSessionDesc session )
            {
                /* Don't kill any UDP Sessions */
                if ( session.protocol() == com.metavize.mvvm.argon.IPSessionDesc.IPPROTO_UDP )
                    return false;

                int clientPort = session.clientPort();
                int serverPort = session.serverPort();

                /* FTP responds on port 20, server is on 21, HTTP server is on 80 */
                if ( clientPort == 20 || serverPort == 21 || serverPort == 80 || serverPort == 20 )
                    return true;

                /* EMAIL SMTP/POP3/IMAP */
                if ( serverPort == 25 || serverPort == 143 || serverPort == 109 )
                    return true;

                return false;
            }
        };

    // constructors -----------------------------------------------------------

    public VirusTransformImpl(VirusScanner scanner)
    {
        this.scanner = scanner;
    }

    // VirusTransform methods -------------------------------------------------

    public void setVirusSettings(VirusSettings settings)
    {
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.saveOrUpdateCopy(settings);
            this.settings = settings;

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not get VirusSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close hibernate session", exn);
            }
        }

        virusReconfigure();
    }

    public VirusSettings getVirusSettings()
    {
        return settings;
    }

    // Transform methods ------------------------------------------------------

    public void dumpSessions()
    {
        for (int i = 0; i < mPipes.length; i++) {
            MPipe pipe = mPipes[i];
            if (pipe != null)
                pipe.dumpSessions();
        }
    }

    public IPSessionDesc[] liveSessionDescs()
    {
        IPSessionDesc[] s1 = null;
        if (mPipes[0] != null)
            s1 = mPipes[0].liveSessionDescs();
        else
            s1 = new IPSessionDesc[0];

        IPSessionDesc[] s2 = null;
        if (mPipes[1] != null)
            s2 = mPipes[1].liveSessionDescs();
        else
            s2 = new IPSessionDesc[0];

        IPSessionDesc[] s3 = null;
        if (mPipes[2] != null)
            s3 = mPipes[2].liveSessionDescs();
        else
            s3 = new IPSessionDesc[0];

        IPSessionDesc[] retDescs = new IPSessionDesc[s1.length + s2.length + s3.length];
        System.arraycopy(s1, 0, retDescs, 0, s1.length);
        System.arraycopy(s2, 0, retDescs, s1.length, s2.length);
        System.arraycopy(s3, 0, retDescs, s1.length + s2.length, s3.length);
        return retDescs;
    }

    private void virusReconfigure()
    {
        // FTP
        Set subscriptions = new HashSet();
        if (settings.getFtpInbound().getScan()) {
            // XXX i get -2 on my home machine
            Subscription subscription = new Subscription
                (Protocol.TCP, Interface.INSIDE, Interface.OUTSIDE);
            subscriptions.add(subscription);
        }

        if (settings.getFtpOutbound().getScan()) {
            // XXX i get -2 on my home machine
            Subscription subscription = new Subscription
                (Protocol.TCP, Interface.OUTSIDE, Interface.INSIDE);
            subscriptions.add(subscription);
        }

        pipeSpecs[FTP_COMMAND].setSubscriptions(subscriptions);

        // HTTP
        subscriptions = new HashSet();
        if (settings.getHttpInbound().getScan()) {
            // XXX i get -2 on my home machine
            Subscription subscription = new Subscription
                (Protocol.TCP, Interface.INSIDE, Interface.OUTSIDE );
            subscriptions.add(subscription);
        }

        if (settings.getHttpOutbound().getScan()) {
            // XXX i get -2 on my home machine
            Subscription subscription = new Subscription
                (Protocol.TCP, Interface.OUTSIDE, Interface.INSIDE );
            subscriptions.add(subscription);
        }

        pipeSpecs[HTTP].setSubscriptions(subscriptions);
    }

    // AbstractTransform methods ----------------------------------------------

    protected void connectMPipe()
    {
        for (int i = 0; i < pipeSpecs.length; i++) {
            mPipes[i] = MPipeManager.manager().plumbLocal(this, pipeSpecs[i]);
            mPipes[i].setSessionEventListener(listeners[i]);
            FOUNDRY.registerMPipe(mPipes[i]);
            logger.debug( "Connecting mPipe[" + i + "] as " + mPipes[i] );
        }
    }

    protected void disconnectMPipe()
    {
        for (int i = 0; i < mPipes.length; i++) {
            logger.debug( "Disconnecting mPipe[" + i + "] as " + mPipes[i] );
            if ( mPipes[i] != null ) {
                FOUNDRY.deregisterMPipe(mPipes[i]);
            } else {
                logger.warn("Disconnecting null mPipe[" + i + "]");
            }
            mPipes[i] = null;
        }
    }

    protected void initializeSettings()
    {
        VirusSettings vs = new VirusSettings(getTid());
        vs.setHttpInbound(new VirusConfig(true, true, "Scan incoming files"));
        vs.setHttpOutbound(new VirusConfig(true, true, "Scan outgoing files" ));
        vs.setFtpInbound(new VirusConfig(true, true, "Scan incoming files" ));
        vs.setFtpOutbound(new VirusConfig(true, true, "Scan outgoing files" ));

        /**
         * FIXME, need list with booleans
         * default should be:
         * application/x-javascript.*    false
         * application/x-shockwave-flash false
         * application/.*" true
         * images/.* false
         * text/.*   false
         * video/.*  false
         * audio/.*  false
         */
        List s = new ArrayList();
        s.add(new MimeTypeRule(new MimeType("application/x-javascript*"), "JavaScript", "executable", false));
        s.add(new MimeTypeRule(new MimeType("application/x-shockwave-flash*"), "Shockwave Flash", "executable", false));
        s.add(new MimeTypeRule(new MimeType("application/*"), "applications", "misc", true));

        s.add(new MimeTypeRule(new MimeType("image/*"), "images", "image", false));
        s.add(new MimeTypeRule(new MimeType("video/*"), "video", "video", false));
        s.add(new MimeTypeRule(new MimeType("text/*"), "text", "text", false));
        s.add(new MimeTypeRule(new MimeType("audio/*"), "audio", "audio", false));

        vs.setHttpMimeTypes(s);

        s = new ArrayList();
        /* XXX Need a description here */
        s.add(new StringRule("exe"));
        vs.setExtensions(s);

        setVirusSettings(vs);
    }

    protected void preInit(String args[])
    {
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery
                ("from VirusSettings vs where vs.tid = :tid");
            q.setParameter("tid", getTid());
            settings = (VirusSettings)q.uniqueResult();

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("Could not get VirusSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Hibernate session", exn);
            }
        }
    }

    protected void postStart()
    {
        virusReconfigure();

        shutdownMatchingSessions();
    }

    // package protected methods ----------------------------------------------

    VirusScanner getScanner()
    {
        return scanner;
    }

    FtpCommandHandler getFtpCommandHandler()
    {
        return (FtpCommandHandler)listeners[FTP_COMMAND];
    }

    MPipe getFtpDataMPipe()
    {
        return mPipes[FTP_DATA];
    }

    int getTricklePercent()
    {
        return settings.getTricklePercent();
    }

    List getExtensions()
    {
        return settings.getExtensions();
    }

    List getHttpMimeTypes()
    {
        return settings.getHttpMimeTypes();
    }

    boolean getFtpDisableResume()
    {
        return settings.getFtpDisableResume();
    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        return getVirusSettings();
    }

    public void setSettings(Object settings)
    {
        setVirusSettings((VirusSettings)settings);
    }

    public void reconfigure()
    {
        shutdownMatchingSessions();
    }

    protected SessionMatcher sessionMatcher()
    {
        return VIRUS_SESSION_MATCHER;
    }
}
