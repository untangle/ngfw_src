/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.untangle.node.protofilter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.node.util.PartialListUtil;
import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.NodeException;
import com.untangle.uvm.node.NodeStartException;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.AbstractNode;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.SoloPipeSpec;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.message.BlingBlinger;
import com.untangle.uvm.message.Counters;
import com.untangle.uvm.message.LocalMessageManager;


public class ProtoFilterImpl extends AbstractNode implements ProtoFilter
{
    private final EventHandler handler = new EventHandler( this );

    private final SoloPipeSpec pipeSpec = new SoloPipeSpec
        ("protofilter", this, handler, Fitting.OCTET_STREAM,
         Affinity.CLIENT, 0);
    private final PipeSpec[] pipeSpecs = new PipeSpec[] { pipeSpec };

    private final EventLogger<ProtoFilterLogEvent> eventLogger;

    private final Logger logger = Logger.getLogger(ProtoFilterImpl.class);

    private ProtoFilterSettings cachedSettings = null;

    private final PartialListUtil listUtil = new PartialListUtil();
    private final ProtoFilterPatternHandler patternHandler = new ProtoFilterPatternHandler();

    private final BlingBlinger scanBlinger;
    private final BlingBlinger detectBlinger;
    private final BlingBlinger blockBlinger;
    //private final BlingBlinger passedBlinger;

    // constructors -----------------------------------------------------------

    public ProtoFilterImpl()
    {
        NodeContext tctx = getNodeContext();
        eventLogger = EventLoggerFactory.factory().getEventLogger(tctx);

        SimpleEventFilter<ProtoFilterLogEvent> ef = new ProtoFilterAllFilter();
        eventLogger.addSimpleEventFilter(ef);
        ef = new ProtoFilterBlockedFilter();
        eventLogger.addSimpleEventFilter(ef);

        LocalMessageManager lmm = LocalUvmContextFactory.context().localMessageManager();
        Counters c = lmm.getCounters(getTid());
        scanBlinger = c.addActivity("scan", I18nUtil.marktr("Sessions scanned"), null, I18nUtil.marktr("SCAN"));
        detectBlinger = c.addActivity("detect", I18nUtil.marktr("Sessions logged"), null, I18nUtil.marktr("LOG"));
        blockBlinger = c.addActivity("block", I18nUtil.marktr("Sessions blocked"), null, I18nUtil.marktr("BLOCK"));
        lmm.setActiveMetricsIfNotSet(getTid(), scanBlinger, detectBlinger, blockBlinger);
    }

    // ProtoFilter methods ----------------------------------------------------

    public ProtoFilterSettings getProtoFilterSettings()
    {
        if( this.cachedSettings == null )
            logger.error("Settings not yet initialized. State: " + getNodeContext().getRunState() );
        return this.cachedSettings;
    }

    public void setProtoFilterSettings(final ProtoFilterSettings settings)
    {
        TransactionWork<Object> tw = new TransactionWork<Object>()
            {
                public boolean doWork(Session s)
                {
                	ProtoFilterImpl.this.cachedSettings = (ProtoFilterSettings)s.merge(settings);
                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);

        try {
            reconfigure();
        }
        catch (NodeException exn) {
            logger.error("Could not save ProtoFilter settings", exn);
        }
    }
    
    public ProtoFilterBaseSettings getBaseSettings() {
        return cachedSettings.getBaseSettings();
    }

    public void setBaseSettings(final ProtoFilterBaseSettings baseSettings) {
        TransactionWork<Object> tw = new TransactionWork<Object>() {
                public boolean doWork(Session s) {
                    cachedSettings.setBaseSettings(baseSettings);
                    s.merge(cachedSettings);
                    return true;
                }
                
                public Object getResult() {
				return null;
                }
            };
        getNodeContext().runTransaction(tw);
    }
    
    @SuppressWarnings("unchecked") //getItems
    public List<ProtoFilterPattern> getPatterns(final int start,
                                                final int limit, final String... sortColumns) {
        return listUtil.getItems( "select hbs.patterns from ProtoFilterSettings hbs where hbs.tid = :tid ",
                                  getNodeContext(), getTid(), start, limit, sortColumns );
    }
    
    public void updatePatterns(List<ProtoFilterPattern> added,
                               List<Long> deleted, List<ProtoFilterPattern> modified) {
        
        updatePatterns(getProtoFilterSettings().getPatterns(), added, deleted,
                       modified);
    }

	/*
	 * For this node, updateAll means update only the patterns and then reconfigure the node
	 * @see com.untangle.node.protofilter.ProtoFilter#updateAll(java.util.List[])
	 */
    @SuppressWarnings("unchecked")
	public void updateAll(List[] patternsChanges) {
    	if (patternsChanges != null && patternsChanges.length >= 3) {
            updatePatterns(patternsChanges[0], patternsChanges[1], patternsChanges[2]);
    	}
        
        try {
            reconfigure();
        }
        catch (NodeException exn) {
            logger.error("Could not update ProtoFilter changes", exn);
        }
    }
    
    public EventManager<ProtoFilterLogEvent> getEventManager()
    {
        return eventLogger;
    }

    // AbstractNode methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    // Node methods ------------------------------------------------------

    /*
     * First time initialization
     */
    public void initializeSettings()
    {
        ProtoFilterSettings settings = new ProtoFilterSettings(this.getTid());
        logger.info("INIT: Importing patterns...");
        TreeMap<Integer,ProtoFilterPattern> factoryPatterns = LoadPatterns.getPatterns(); /* Global List of Patterns */
        // Turn on the Instant Messenger ones so it does something by default:
        Set<ProtoFilterPattern> pats = new HashSet<ProtoFilterPattern>(factoryPatterns.values());
        for (ProtoFilterPattern pfp : pats) {
            if (pfp.getCategory().equalsIgnoreCase("Instant Messenger"))
                pfp.setLog(true);
        }
        settings.setPatterns(pats);
        setProtoFilterSettings(settings);
    }

    protected void postInit(String[] args)
    {
        TransactionWork<Object> tw = new TransactionWork<Object>()
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
        getNodeContext().runTransaction(tw);

        updateToCurrent();
    }

    protected void preStart() throws NodeStartException
    {
        try {
            reconfigure();
        } catch (Exception e) {
            throw new NodeStartException(e);
        }
    }

    public void reconfigure() throws NodeException
    {
        Set<ProtoFilterPattern> enabledPatternsSet = new HashSet<ProtoFilterPattern>();

        logger.info("Reconfigure()");

        if (cachedSettings == null) {
            throw new NodeException("Failed to get ProtoFilter settings: " + cachedSettings);
        }

        Set<ProtoFilterPattern> curPatterns = cachedSettings.getPatterns();
        if (curPatterns == null)
            logger.error("NULL pattern list. Continuing anyway...");
        else {
            for (Iterator<ProtoFilterPattern> i=curPatterns.iterator() ; i.hasNext() ; ) {
                ProtoFilterPattern pat = i.next();

                if ( pat.getLog() || pat.getAlert() || pat.isBlocked() ) {
                    logger.info("Matching on pattern \"" + pat.getProtocol() + "\"");
                    enabledPatternsSet.add(pat);
                }
            }
        }

        handler.patternSet(enabledPatternsSet);
        handler.byteLimit(cachedSettings.getByteLimit());
        handler.chunkLimit(cachedSettings.getChunkLimit());
        handler.stripZeros(cachedSettings.isStripZeros());
    }


    private   void updateToCurrent()
    {
        if (cachedSettings == null) {
            logger.error("NULL ProtoFilter Settings");
            return;
        }

        boolean    madeChange = false;
        TreeMap<Integer,ProtoFilterPattern> factoryPatterns = LoadPatterns.getPatterns(); /* Global List of Patterns */
        Set<ProtoFilterPattern> curPatterns = cachedSettings.getPatterns(); /* Current list of Patterns */

        /*
         * Look for updates
         */
        for (Iterator<ProtoFilterPattern> i = curPatterns.iterator() ; i.hasNext() ; ) {
            ProtoFilterPattern curPat = i.next();
            int mvid = curPat.getMetavizeId();
            ProtoFilterPattern newPat = factoryPatterns.get(mvid);

            // logger.info("INFO: Found existing pattern " + mvid + " Pattern (" + curPat.getProtocol() + ")");
            if (mvid == ProtoFilterPattern.NEEDS_CONVERSION_METAVIZE_ID) {
                // Special one-time handling for conversion from 3.1.3 to 3.2
                // Find it by name
                madeChange = true;
                String curName = curPat.getProtocol();
                boolean found = false;
                for (Iterator<ProtoFilterPattern> j = factoryPatterns.values().iterator() ; j.hasNext() ; ) {
                    newPat = j.next();
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
            LinkedList<ProtoFilterPattern> allPatterns = new LinkedList<ProtoFilterPattern>(curPatterns);
            for (Iterator<ProtoFilterPattern> i = factoryPatterns.values().iterator() ; i.hasNext() ; ) {
                ProtoFilterPattern factoryPat = i.next();
                logger.info("UPDATE: Adding New Pattern (" + factoryPat.getProtocol() + ")");
                boolean added = false;
                int index = 0;
                for (Iterator<ProtoFilterPattern> j = allPatterns.iterator() ; j.hasNext() ; index++) {
                    ProtoFilterPattern curPat = j.next();
                    if (factoryPat.getMetavizeId() < curPat.getMetavizeId()) {
                        allPatterns.add(index, factoryPat);
                        added = true;
                        break;
                    }
                }
                if (!added)
                    allPatterns.add(factoryPat);
            }
            curPatterns = new HashSet<ProtoFilterPattern>(allPatterns);
        }

        if (madeChange) {
            logger.info("UPDATE: Saving new patterns list, size " + curPatterns.size());
            cachedSettings.setPatterns(curPatterns);
            setProtoFilterSettings(cachedSettings);
        }

        logger.info("UPDATE: Complete");
    }

    void log(ProtoFilterLogEvent se)
    {
        eventLogger.log(se);
    }

    private void updatePatterns(final Set<ProtoFilterPattern> patterns, final List<ProtoFilterPattern> added,
                             final List<Long> deleted, final List<ProtoFilterPattern> modified)
    {
        TransactionWork<Object> tw = new TransactionWork<Object>()
            {
                public boolean doWork(Session s)
                {
                    listUtil.updateCachedItems( patterns, patternHandler, added, deleted, modified );

                    cachedSettings = (ProtoFilterSettings)s.merge(cachedSettings);

                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);
    }
    
    private static class ProtoFilterPatternHandler implements PartialListUtil.Handler<ProtoFilterPattern>
    {
        public Long getId( ProtoFilterPattern rule )
        {
            return rule.getId();
        }

        public void update( ProtoFilterPattern current, ProtoFilterPattern newRule )
        {
            current.updateRule( newRule );
        }
    }

    void incrementScanCount()
    {
        scanBlinger.increment();
    }

    void incrementBlockCount()
    {
        blockBlinger.increment();
    }

    void incrementDetectCount()
    {
        detectBlinger.increment();
    }
}
