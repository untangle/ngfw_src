/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: ProtoFilterImpl.java,v 1.12 2005/02/11 22:47:01 jdi Exp $
 */
package com.metavize.tran.protofilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tapi.SoloTransform;
import com.metavize.mvvm.tapi.Subscription;
import com.metavize.mvvm.tapi.TransformContextFactory;
import com.metavize.mvvm.tran.*;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;

public class FirewallImpl extends SoloTransform implements Firewall
{
    private static final Logger logger = Logger.getLogger(ProtoFilterImpl.class);
    private final PipeSpec pipeSpec;
    private ProtoFilterSettings settings = null;
    private EventHandler handler = null;

    public FirewallImpl()
    {
        Set subscriptions = new HashSet();
        subscriptions.add(new Subscription(Protocol.TCP));
        subscriptions.add(new Subscription(Protocol.UDP));
        
        /* Have to figure out pipeline ordering, this should always next to towards the outside */
        this.pipeSpec = new PipeSpec( "firewall", Fitting.OCTET_STREAM, subscriptions, Affinity.BEGIN );
    }

    public FirewallSettings getFirewallSettings()
    {
        return this.settings;
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
            logger.warn("could not get HttpBlockerSettings", exn);
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

    public PipeSpec getPipeSpec()
    {
        return this.pipeSpec;
    }


    protected void initializeSettings()
    {
        FirewallSettings settings = new FirewallSettings(this.getTid());
        logger.info("Initializing Settings...");

        updateToCurrent(settings);

        setFirewallSettings(settings);
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
            logger.warn("Could not get HttpBlockerSettings", exn);
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

        getMPipe().setSessionEventListener(this.handler);
    }

    public    void reconfigure() throws TransformException
    {
        FirewallSettings settings = getFirewallSettings();
        ArrayList enabledPatternsList = new ArrayList();

        logger.info("Reconfigure()");

        if (settings == null) {
            throw new TransformException("Failed to get Firewall settings: " + settings);
        }

        List curPatterns = settings.getPatterns();
        if (curPatterns == null)
            logger.warn("NULL pattern list. Continuing anyway...");
        else {
            for (Iterator i=curPatterns.iterator() ; i.hasNext() ; ) {
                FirewallPattern pat = (FirewallPattern)i.next();

                if ( pat.getLog() || pat.getAlert() || pat.isBlocked() ) {
                    logger.info("Matching on pattern \"" + pat.getProtocol() + "\"");
                    enabledPatternsList.add(pat);
                }
            }
        }

        if (this.handler == null) this.handler = new EventHandler();
        handler.patternList(enabledPatternsList);
        handler.byteLimit(settings.getByteLimit());
        handler.chunkLimit(settings.getChunkLimit());
        handler.unknownString(settings.getUnknownString());
        handler.stripZeros(settings.isStripZeros());
    }


    private   void updateToCurrent(FirewallSettings settings)
    {
        if (settings == null) {
            logger.error("NULL Protofilter Settings");
            return;
        }

        HashMap    allPatterns = LoadPatterns.getPatterns(); /* Global List of Patterns */
        List       curPatterns = settings.getPatterns(); /* Current list of Patterns */

        if (curPatterns == null) {
            /**
             * First time initialization
             */
            logger.info("UPDATE: Importing patterns...");
            settings.setPatterns(new ArrayList(allPatterns.values()));
            curPatterns = settings.getPatterns();
        }
        else {
            /**
             * Look for updates
             */
            for (Iterator i=curPatterns.iterator() ; i.hasNext() ; ) {
                FirewallPattern pat = (FirewallPattern) i.next();
                String name = pat.getProtocol();
                String def  = pat.getDefinition();

                if (allPatterns.containsKey(name)) {
                    /**
                     * Key is present in current config
                     * Update definition and description if needed
                     */
                    FirewallPattern newpat = (FirewallPattern)allPatterns.get(name);
                    if (newpat == null) {
                        logger.error("Missing pattern");
                        continue;
                    }

                    if (!newpat.getDescription().equals(pat.getDescription())) {
                        logger.info("UPDATE: Updating Description for Pattern (" + name + ")");
                        pat.setDescription(newpat.getDescription());
                    }
                    if (!newpat.getDefinition().equals(pat.getDefinition())) {
                        logger.info("UPDATE: Updating Definition  for Pattern (" + name + ")");
                        pat.setDefinition(newpat.getDefinition());
                    }

                    /**
                     * Remove it, its been accounted for
                     */
                    allPatterns.remove(name);
                }
            }

            /**
             * Add all the necessary new patterns
             * Whatever is left in allPatterns at this point, is not in the curPatterns
             */
            for (Iterator i=allPatterns.values().iterator() ; i.hasNext() ; ) {
                FirewallPattern pat = (FirewallPattern) i.next();
                logger.info("UPDATE: Adding New Pattern (" + pat.getProtocol() + ")");
                curPatterns.add(pat);
            }
        }

        logger.info("UPDATE: Complete");
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
