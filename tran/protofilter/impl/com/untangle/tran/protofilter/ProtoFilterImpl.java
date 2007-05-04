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
package com.untangle.tran.protofilter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import com.untangle.mvvm.logging.EventLogger;
import com.untangle.mvvm.logging.EventLoggerFactory;
import com.untangle.mvvm.logging.EventManager;
import com.untangle.mvvm.logging.SimpleEventFilter;
import com.untangle.mvvm.tapi.AbstractTransform;
import com.untangle.mvvm.tapi.Affinity;
import com.untangle.mvvm.tapi.Fitting;
import com.untangle.mvvm.tapi.PipeSpec;
import com.untangle.mvvm.tapi.SoloPipeSpec;
import com.untangle.mvvm.tran.TransformContext;
import com.untangle.mvvm.tran.TransformException;
import com.untangle.mvvm.tran.TransformStartException;
import com.untangle.mvvm.util.TransactionWork;
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

    private ProtoFilterSettings cachedSettings = null;

    // constructors -----------------------------------------------------------

    public ProtoFilterImpl()
    {
        TransformContext tctx = getTransformContext();
        eventLogger = EventLoggerFactory.factory().getEventLogger(tctx);

        SimpleEventFilter ef = new ProtoFilterAllFilter();
        eventLogger.addSimpleEventFilter(ef);
        ef = new ProtoFilterBlockedFilter();
        eventLogger.addSimpleEventFilter(ef);
    }

    // ProtoFilter methods ----------------------------------------------------

    public ProtoFilterSettings getProtoFilterSettings()
    {
        if( this.cachedSettings == null )
            logger.error("Settings not yet initialized. State: " + getTransformContext().getRunState() );
        return this.cachedSettings;
    }

    public void setProtoFilterSettings(final ProtoFilterSettings settings)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.merge(settings);
                    ProtoFilterImpl.this.cachedSettings = settings;
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

    /*
     * First time initialization
     */
    public void initializeSettings()
    {
        ProtoFilterSettings settings = new ProtoFilterSettings(this.getTid());
        logger.info("INIT: Importing patterns...");
        TreeMap factoryPatterns = LoadPatterns.getPatterns(); /* Global List of Patterns */
        // Turn on the Instant Messenger ones so it does something by default:
        ArrayList pats = new ArrayList(factoryPatterns.values());
        for (Object pat : pats) {
            ProtoFilterPattern pfp = (ProtoFilterPattern)pat;
            if (pfp.getCategory().equalsIgnoreCase("Instant Messenger"))
                pfp.setLog(true);
        }
        settings.setPatterns(pats);
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
                    ProtoFilterImpl.this.cachedSettings = (ProtoFilterSettings)q.uniqueResult();

                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);

        updateToCurrent();
    }

    protected void preStart() throws TransformStartException
    {
        try {
            reconfigure();
        } catch (Exception e) {
            throw new TransformStartException(e);
        }
    }

    private void reconfigure() throws TransformException
    {
        ArrayList enabledPatternsList = new ArrayList();

        logger.info("Reconfigure()");

        if (cachedSettings == null) {
            throw new TransformException("Failed to get ProtoFilter settings: " + cachedSettings);
        }

        List curPatterns = cachedSettings.getPatterns();
        if (curPatterns == null)
            logger.error("NULL pattern list. Continuing anyway...");
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
        handler.byteLimit(cachedSettings.getByteLimit());
        handler.chunkLimit(cachedSettings.getChunkLimit());
        handler.unknownString(cachedSettings.getUnknownString());
        handler.stripZeros(cachedSettings.isStripZeros());
    }


    private   void updateToCurrent()
    {
        if (cachedSettings == null) {
            logger.error("NULL ProtoFilter Settings");
            return;
        }

        boolean    madeChange = false;
        TreeMap    factoryPatterns = LoadPatterns.getPatterns(); /* Global List of Patterns */
        List       curPatterns = cachedSettings.getPatterns(); /* Current list of Patterns */

        /*
         * Look for updates
         */
        for (Iterator i = curPatterns.iterator() ; i.hasNext() ; ) {
            ProtoFilterPattern curPat = (ProtoFilterPattern) i.next();
            int mvid = curPat.getMetavizeId();
            ProtoFilterPattern newPat = (ProtoFilterPattern) factoryPatterns.get(mvid);

            // logger.info("INFO: Found existing pattern " + mvid + " Pattern (" + curPat.getProtocol() + ")");
            if (mvid == ProtoFilterPattern.NEEDS_CONVERSION_METAVIZE_ID) {
                // Special one-time handling for conversion from 3.1.3 to 3.2
                // Find it by name
                madeChange = true;
                String curName = curPat.getProtocol();
                boolean found = false;
                for (Iterator j = factoryPatterns.values().iterator() ; j.hasNext() ; ) {
                    newPat = (ProtoFilterPattern) j.next();
                    if (newPat.getProtocol().equals(curName)) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    logger.info("CONVERT: Updating MVID for Pattern (" + curName + ")");
                    mvid = newPat.getMetavizeId();
                    curPat.setMetavizeId(mvid);
                } else {
                    // A name mismatch, a user pattern, or the pattern has been deleted.
                    newPat = null;
                }
                // In either case, fall through to below
            }
            if (newPat != null) {
                /*
                 * Pattern is present in current config
                 * Update it if needed
                 */
                if (!newPat.getProtocol().equals(curPat.getProtocol())) {
                    logger.info("UPDATE: Updating Protocol for Pattern (" + mvid + ")");
                    madeChange = true;
                    curPat.setProtocol(newPat.getProtocol());
                }
                if (!newPat.getCategory().equals(curPat.getCategory())) {
                    logger.info("UPDATE: Updating Category for Pattern (" + mvid + ")");
                    madeChange = true;
                    curPat.setCategory(newPat.getCategory());
                }
                if (!newPat.getDescription().equals(curPat.getDescription())) {
                    logger.info("UPDATE: Updating Description for Pattern (" + mvid + ")");
                    madeChange = true;
                    curPat.setDescription(newPat.getDescription());
                }
                if (!newPat.getDefinition().equals(curPat.getDefinition())) {
                    logger.info("UPDATE: Updating Definition  for Pattern (" + mvid + ")");
                    madeChange = true;
                    curPat.setDefinition(newPat.getDefinition());
                }
                if (!newPat.getQuality().equals(curPat.getQuality())) {
                    logger.info("UPDATE: Updating Quality  for Pattern (" + mvid + ")");
                    madeChange = true;
                    curPat.setQuality(newPat.getQuality());
                }

                // Remove it, its been accounted for
                factoryPatterns.remove(mvid);
            } else if (mvid != ProtoFilterPattern.USER_CREATED_METAVIZE_ID) {
                // MV Pattern has been deleted.
                i.remove();
                madeChange = true;
                logger.info("UPDATE: Removing old Pattern " + mvid + " (" + curPat.getProtocol() + ")");
            }
        }

        /*
         * At this point, curPatterns is correct except for the newly added factory patterns.
         * Go ahead and add them now.  To put them in the right place, we do a linear
         * insertion, which isn't bad since the list is never very long.
         */
        if (factoryPatterns.size() > 0) {
            madeChange = true;
            LinkedList allPatterns = new LinkedList(curPatterns);
            for (Iterator i = factoryPatterns.values().iterator() ; i.hasNext() ; ) {
                ProtoFilterPattern factoryPat = (ProtoFilterPattern) i.next();
                logger.info("UPDATE: Adding New Pattern (" + factoryPat.getProtocol() + ")");
                boolean added = false;
                int index = 0;
                for (Iterator j = allPatterns.iterator() ; j.hasNext() ; index++) {
                    ProtoFilterPattern curPat = (ProtoFilterPattern) j.next();
                    if (factoryPat.getMetavizeId() < curPat.getMetavizeId()) {
                        allPatterns.add(index, factoryPat);
                        added = true;
                        break;
                    }
                }
                if (!added)
                    allPatterns.add(factoryPat);
            }
            curPatterns = new ArrayList(allPatterns);
        }

        if (madeChange) {
            logger.info("UPDATE: Saving new patterns list, size " + curPatterns.size());
            cachedSettings.setPatterns(new ArrayList(curPatterns));
            setProtoFilterSettings(cachedSettings);
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
