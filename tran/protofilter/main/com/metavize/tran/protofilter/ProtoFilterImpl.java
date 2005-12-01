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
package com.metavize.tran.protofilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.metavize.mvvm.logging.SimpleEventFilter;
import com.metavize.mvvm.logging.EventLogger;
import com.metavize.mvvm.logging.EventManager;
import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.tran.TransformException;
import com.metavize.mvvm.tran.TransformStartException;
import com.metavize.mvvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

public class ProtoFilterImpl extends AbstractTransform implements ProtoFilter
{
    private final EventHandler handler = new EventHandler( this );

    private final SoloPipeSpec pipeSpec = new SoloPipeSpec
        ("protofilter", this, handler, Fitting.OCTET_STREAM,
         Affinity.CLIENT, 0);
    private final PipeSpec[] pipeSpecs = new PipeSpec[] { pipeSpec };

    private final EventLogger<ProtoFilterLogEvent> eventLogger;

    private final Logger logger = Logger.getLogger(ProtoFilterImpl.class);

    private ProtoFilterSettings settings = null;

    // constructors -----------------------------------------------------------

    public ProtoFilterImpl()
    {
        TransformContext tctx = getTransformContext();
        eventLogger = new EventLogger<ProtoFilterLogEvent>(tctx);

        SimpleEventFilter ef = new ProtoFilterAllFilter();
        eventLogger.addSimpleEventFilter(ef);
        ef = new ProtoFilterBlockedFilter();
        eventLogger.addSimpleEventFilter(ef);
    }

    // ProtoFilter methods ----------------------------------------------------

    public ProtoFilterSettings getProtoFilterSettings()
    {
        return this.settings;
    }

    public void setProtoFilterSettings(final ProtoFilterSettings settings)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.saveOrUpdate(settings);
                    ProtoFilterImpl.this.settings = settings;
                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);

        try {
            reconfigure();
        }
        catch (TransformException exn) {
            logger.error("Could not save ProtoFilter settings", exn);
        }
    }

    public EventManager<ProtoFilterLogEvent> getEventManager()
    {
        return eventLogger;
    }

    // AbstractTransform methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    // Transform methods ------------------------------------------------------

    protected void initializeSettings()
    {
        ProtoFilterSettings settings = new ProtoFilterSettings(this.getTid());
        logger.info("Initializing Settings...");

        updateToCurrent(settings);

        setProtoFilterSettings(settings);
    }

    protected void postInit(String[] args)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from ProtoFilterSettings hbs where hbs.tid = :tid");
                    q.setParameter("tid", getTid());
                    ProtoFilterImpl.this.settings = (ProtoFilterSettings)q.uniqueResult();

                    updateToCurrent(ProtoFilterImpl.this.settings);

                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);
    }

    protected void preStart() throws TransformStartException
    {
        eventLogger.start();

        try {
            reconfigure();
        } catch (Exception e) {
            throw new TransformStartException(e);
        }
    }

    protected void postStop()
    {
        eventLogger.stop();
    }

    public    void reconfigure() throws TransformException
    {
        ProtoFilterSettings settings = getProtoFilterSettings();
        ArrayList enabledPatternsList = new ArrayList();

        logger.info("Reconfigure()");

        if (settings == null) {
            throw new TransformException("Failed to get ProtoFilter settings: " + settings);
        }

        List curPatterns = settings.getPatterns();
        if (curPatterns == null)
            logger.warn("NULL pattern list. Continuing anyway...");
        else {
            for (Iterator i=curPatterns.iterator() ; i.hasNext() ; ) {
                ProtoFilterPattern pat = (ProtoFilterPattern)i.next();

                if ( pat.getLog() || pat.getAlert() || pat.isBlocked() ) {
                    logger.info("Matching on pattern \"" + pat.getProtocol() + "\"");
                    enabledPatternsList.add(pat);
                }
            }
        }

        handler.patternList(enabledPatternsList);
        handler.byteLimit(settings.getByteLimit());
        handler.chunkLimit(settings.getChunkLimit());
        handler.unknownString(settings.getUnknownString());
        handler.stripZeros(settings.isStripZeros());
    }


    private   void updateToCurrent(ProtoFilterSettings settings)
    {
        if (settings == null) {
            logger.error("NULL ProtoFilter Settings");
            return;
        }

        HashMap    allPatterns = LoadPatterns.getPatterns(); /* Global List of Patterns */
        List       curPatterns = settings.getPatterns(); /* Current list of Patterns */

        if (curPatterns == null) {
            /*
             * First time initialization
             */
            logger.info("UPDATE: Importing patterns...");
            settings.setPatterns(new ArrayList(allPatterns.values()));
            curPatterns = settings.getPatterns();
        }
        else {
            /*
             * Look for updates
             */
            for (Iterator i=curPatterns.iterator() ; i.hasNext() ; ) {
                ProtoFilterPattern pat = (ProtoFilterPattern) i.next();
                String name = pat.getProtocol();
                String def  = pat.getDefinition();

                if (allPatterns.containsKey(name)) {
                    /*
                     * Key is present in current config
                     * Update definition and description if needed
                     */
                    ProtoFilterPattern newpat = (ProtoFilterPattern)allPatterns.get(name);
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

                    /*
                     * Remove it, its been accounted for
                     */
                    allPatterns.remove(name);
                }
            }

            /*
             * Add all the necessary new patterns.  Whatever is left
             * in allPatterns at this point, is not in the curPatterns
             */
            for (Iterator i=allPatterns.values().iterator() ; i.hasNext() ; ) {
                ProtoFilterPattern pat = (ProtoFilterPattern) i.next();
                logger.info("UPDATE: Adding New Pattern (" + pat.getProtocol() + ")");
                curPatterns.add(pat);
            }
        }

        logger.info("UPDATE: Complete");
    }

    void log(ProtoFilterLogEvent se)
    {
        eventLogger.log(se);
    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        return getProtoFilterSettings();
    }

    public void setSettings(Object settings)
    {
        setProtoFilterSettings((ProtoFilterSettings)settings);
    }
}
