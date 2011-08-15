/*
 * $Id$
 */
package com.untangle.node.protofilter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.node.NodeContext;
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
import com.untangle.uvm.message.MessageManager;
import com.untangle.uvm.SettingsManager;

public class ProtoFilterImpl extends AbstractNode implements ProtoFilter
{
    private final EventHandler handler = new EventHandler( this );

    private final SoloPipeSpec pipeSpec = new SoloPipeSpec("protofilter", this, handler, Fitting.OCTET_STREAM, Affinity.CLIENT, 0);
    private final PipeSpec[] pipeSpecs = new PipeSpec[] { pipeSpec };

    private final EventLogger<ProtoFilterLogEvent> eventLogger;

    private final Logger logger = Logger.getLogger(ProtoFilterImpl.class);

    private ProtoFilterSettings cachedSettings = null;
    
    private final BlingBlinger scanBlinger;
    private final BlingBlinger detectBlinger;
    private final BlingBlinger blockBlinger;

    // constructors -----------------------------------------------------------

    public ProtoFilterImpl()
    {
        eventLogger = EventLoggerFactory.factory().getEventLogger(getNodeContext());

        SimpleEventFilter<ProtoFilterLogEvent> ef = new ProtoFilterAllFilter();
        eventLogger.addSimpleEventFilter(ef);
        ef = new ProtoFilterBlockedFilter();
        eventLogger.addSimpleEventFilter(ef);

        MessageManager lmm = LocalUvmContextFactory.context().messageManager();
        Counters c = lmm.getCounters(getNodeId());
        scanBlinger = c.addActivity("scan", I18nUtil.marktr("Sessions scanned"), null, I18nUtil.marktr("SCAN"));
        detectBlinger = c.addActivity("detect", I18nUtil.marktr("Sessions logged"), null, I18nUtil.marktr("LOG"));
        blockBlinger = c.addActivity("block", I18nUtil.marktr("Sessions blocked"), null, I18nUtil.marktr("BLOCK"));
        lmm.setActiveMetricsIfNotSet(getNodeId(), scanBlinger, detectBlinger, blockBlinger);
    }

    // ProtoFilter methods ----------------------------------------------------

    public ProtoFilterSettings getNodeSettings()
    {
        if( this.cachedSettings == null )
            logger.error("Settings not yet initialized. State: " + getNodeContext().getRunState() );
        return this.cachedSettings;
    }

    public void setNodeSettings(final ProtoFilterSettings settings)
    {
        this.cachedSettings = settings;
        
        SettingsManager setman = LocalUvmContextFactory.context().settingsManager();

        try {
            setman.save( ProtoFilterSettings.class, System.getProperty("uvm.settings.dir") + "/untangle-node-protofilter/settings_" + getNodeId(), cachedSettings);
            reconfigure();
        }

        catch (Exception exn) {
            logger.error("Could not save ProtoFilter settings", exn);
        }
    }

    public int getPatternsTotal()
    {
        return(cachedSettings.getPatterns().size());
    }

    public int getPatternsLogged()
    {
        HashSet<ProtoFilterPattern> set = cachedSettings.getPatterns();
        Iterator it = set.iterator();
        int count = 0;
        
        while (it.hasNext()) {
            ProtoFilterPattern item = (ProtoFilterPattern)it.next();
            if (item.getLog()) count++;
        }
        
        return(count);
    }

    public int getPatternsBlocked()
    {
        HashSet<ProtoFilterPattern> set = cachedSettings.getPatterns();
        Iterator it = set.iterator();
        int count = 0;
        
        while (it.hasNext()) {
            ProtoFilterPattern item = (ProtoFilterPattern)it.next();
            if (item.isBlocked()) count++;
        }
        
        return(count);
    }

    @SuppressWarnings("unchecked") //getItems
    public LinkedList<ProtoFilterPattern> getPatterns()
    {
        return(new LinkedList(cachedSettings.getPatterns()));
    }

    public void setPatterns(LinkedList<ProtoFilterPattern> patterns)
    {
        cachedSettings.setPatterns(new HashSet<ProtoFilterPattern>(patterns));
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
    @Override
    public void initializeSettings()
    {
        ProtoFilterSettings settings = new ProtoFilterSettings();
        logger.info("INIT: Importing patterns...");
        TreeMap<Integer,ProtoFilterPattern> factoryPatterns = LoadPatterns.getPatterns(); /* Global List of Patterns */
        // Turn on the Instant Messenger ones so it does something by default:
        HashSet<ProtoFilterPattern> pats = new HashSet<ProtoFilterPattern>(factoryPatterns.values());
        for (ProtoFilterPattern pfp : pats) {
            if (pfp.getCategory().equalsIgnoreCase("Instant Messenger"))
                pfp.setLog(true);
        }
        settings.setPatterns(new HashSet<ProtoFilterPattern>(pats));
        setNodeSettings(settings);
    }

    protected void postInit(String[] args)
    {
        updateToCurrent();
    }

    protected void preStart()
    {
        SettingsManager setman = LocalUvmContextFactory.context().settingsManager();
        
        try {
            cachedSettings =  setman.load( ProtoFilterSettings.class, System.getProperty("uvm.settings.dir") + "/untangle-node-protofilter/settings_" + getNodeId());
            if (cachedSettings == null) initializeSettings();
            reconfigure();
        }

        catch (Exception exn) {
            logger.error("Could not read node settings", exn);
        }
    }

    public void reconfigure() throws Exception
    {
        HashSet<ProtoFilterPattern> enabledPatternsSet = new HashSet<ProtoFilterPattern>();

        logger.info("Reconfigure()");

        if (cachedSettings == null) {
            throw new Exception("Failed to get ProtoFilter settings: " + cachedSettings);
        }

        HashSet<ProtoFilterPattern> curPatterns = cachedSettings.getPatterns();
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

    private void updateToCurrent()
    {
        if (cachedSettings == null) {
            logger.error("NULL ProtoFilter Settings");
            return;
        }

        boolean    madeChange = false;
        TreeMap<Integer,ProtoFilterPattern> factoryPatterns = LoadPatterns.getPatterns(); /* Global List of Patterns */
        HashSet<ProtoFilterPattern> curPatterns = cachedSettings.getPatterns(); /* Current list of Patterns */

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
            cachedSettings.setPatterns(new HashSet<ProtoFilterPattern>(curPatterns));
            setNodeSettings(cachedSettings);
        }

        logger.info("UPDATE: Complete");
    }

    void log(ProtoFilterLogEvent se)
    {
        eventLogger.log(se);
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
