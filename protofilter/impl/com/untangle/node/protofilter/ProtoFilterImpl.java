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

import com.untangle.uvm.node.EventLogQuery;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.SoloPipeSpec;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;

public class ProtoFilterImpl extends NodeBase implements ProtoFilter
{
    private static final String SETTINGS_CONVERSION_SCRIPT = System.getProperty( "uvm.bin.dir" ) + "/protofilter-convert-settings.py";

    private static final String STAT_SCAN = "scan";
    private static final String STAT_DETECT = "detect";
    private static final String STAT_BLOCK = "block";

    private final EventHandler handler = new EventHandler( this );

    private final SoloPipeSpec pipeSpec = new SoloPipeSpec("protofilter", this, handler, Fitting.OCTET_STREAM, Affinity.CLIENT, 0);
    private final PipeSpec[] pipeSpecs = new PipeSpec[] { pipeSpec };

    private final Logger logger = Logger.getLogger(ProtoFilterImpl.class);

    private ProtoFilterSettings nodeSettings = null;

    private EventLogQuery allEventQuery;
    private EventLogQuery blockedEventQuery;
    
    // constructors -----------------------------------------------------------

    public ProtoFilterImpl( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );

        this.allEventQuery = new EventLogQuery(I18nUtil.marktr("All Events"),
                                               "FROM SessionLogEventFromReports evt " +
                                               "WHERE evt.policyId = :policyId " +
                                               "AND pfProtocol IS NOT NULL " +
                                               "ORDER BY evt.timeStamp DESC");

        this.blockedEventQuery = new EventLogQuery(I18nUtil.marktr("Blocked Events"),
                                                   "FROM SessionLogEventFromReports evt " +
                                                   "WHERE evt.policyId = :policyId " +
                                                   "AND pfBlocked IS TRUE " +
                                                   "ORDER BY evt.timeStamp DESC");

        this.addMetric(new NodeMetric(STAT_SCAN, I18nUtil.marktr("Chunks scanned")));
        this.addMetric(new NodeMetric(STAT_DETECT, I18nUtil.marktr("Sessions logged")));
        this.addMetric(new NodeMetric(STAT_BLOCK, I18nUtil.marktr("Sessions blocked")));
    }

    // ProtoFilter methods ----------------------------------------------------

    public ProtoFilterSettings getSettings()
    {
        if( this.nodeSettings == null )
            logger.error("Settings not yet initialized. State: " + this.getRunState() );
        return this.nodeSettings;
    }

    public void setSettings(final ProtoFilterSettings settings)
    {
        this.nodeSettings = settings;
        
        SettingsManager setman = UvmContextFactory.context().settingsManager();

        String nodeID = this.getNodeSettings().getId().toString();

        String settingsBase = System.getProperty("uvm.settings.dir") + "/untangle-node-protofilter/settings_" + nodeID;

        try {
            setman.save( ProtoFilterSettings.class, settingsBase, nodeSettings);
            reconfigure();
        }

        catch (Exception exn) {
            logger.error("Could not save ProtoFilter settings", exn);
        }
    }

    public int getPatternsTotal()
    {
        return(nodeSettings.getPatterns().size());
    }

    public int getPatternsLogged()
    {
        LinkedList<ProtoFilterPattern>list = nodeSettings.getPatterns();
        int count = 0;
        
            for(int x = 0;x < list.size();x++)
            {
            ProtoFilterPattern curr = list.get(x);
            if (curr.getLog()) count++;
            }
        
        return(count);
    }

    public int getPatternsBlocked()
    {
        LinkedList<ProtoFilterPattern>list = nodeSettings.getPatterns();
        int count = 0;
        
            for(int x = 0;x < list.size();x++)
            {
            ProtoFilterPattern curr = list.get(x);
            if (curr.isBlocked()) count++;
            }
        
        return(count);
    }

    public EventLogQuery[] getEventQueries()
    {
        return new EventLogQuery[] { this.allEventQuery, this.blockedEventQuery };
    }        

    // NodeBase methods ----------------------------------------------

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
        settings.setPatterns(new LinkedList<ProtoFilterPattern>(pats));
        setSettings(settings);
    }

    protected void postInit()
    {
        SettingsManager setman = UvmContextFactory.context().settingsManager();

        String nodeID = this.getNodeSettings().getId().toString();

        String settingsBase = System.getProperty("uvm.settings.dir") + "/untangle-node-protofilter/settings_" + nodeID;
        String settingsFile = settingsBase + ".js";
        ProtoFilterSettings readSettings = null;
        
        logger.info("Loading settings from " + settingsFile);
        
        try {
            readSettings =  setman.load( ProtoFilterSettings.class, settingsBase);
        }

        catch (Exception exn) {
            logger.error("Could not read node settings", exn);
        }

        // if no settings found try getting them from the database
        if (readSettings == null) {
            logger.warn("No json settings found... attempting to import from database");
            
            try {
                String convertCmd = SETTINGS_CONVERSION_SCRIPT + " " + nodeID.toString() + " " + settingsFile;;
                logger.warn("Running: " + convertCmd);
                UvmContextFactory.context().execManager().exec( convertCmd );
            }

            catch (Exception exn) {
                logger.error("Conversion script failed", exn);
            }

            try {
                readSettings =  setman.load( ProtoFilterSettings.class, settingsBase);
            }

            catch (Exception exn) {
                logger.error("Could not read node settings", exn);
            }
            
            if (readSettings != null) logger.warn("Database settings successfully imported");
        }

        try
        {
            if (readSettings == null) {
                logger.warn("No database or json settings found... initializing with defaults");
                initializeSettings();
            }
            
            else {
                nodeSettings = readSettings;
                reconfigure();
            }
        }
        
        catch (Exception exn) {
            logger.error("Could not apply node settings", exn);
        }
    }

    public void reconfigure() throws Exception
    {
        HashSet<ProtoFilterPattern> enabledPatternsSet = new HashSet<ProtoFilterPattern>();

        logger.info("Reconfigure()");

        if (nodeSettings == null) {
            throw new Exception("Failed to get ProtoFilter settings: " + nodeSettings);
        }

        LinkedList<ProtoFilterPattern> curPatterns = nodeSettings.getPatterns();
        if (curPatterns == null)
            logger.error("NULL pattern list. Continuing anyway...");
        else {
            for(int x = 0;x < curPatterns.size();x++) {
                ProtoFilterPattern pat = curPatterns.get(x);

                if ( pat.getLog() || pat.getAlert() || pat.isBlocked() ) {
                    logger.info("Matching on pattern \"" + pat.getProtocol() + "\"");
                    enabledPatternsSet.add(pat);
                }
            }
        }

        handler.patternSet(enabledPatternsSet);
        handler.byteLimit(nodeSettings.getByteLimit());
        handler.chunkLimit(nodeSettings.getChunkLimit());
        handler.stripZeros(nodeSettings.isStripZeros());
    }

    void incrementScanCount()
    {
        this.incrementMetric(STAT_SCAN);
    }

    void incrementBlockCount()
    {
        this.incrementMetric(STAT_BLOCK);
    }

    void incrementDetectCount()
    {
        this.incrementMetric(STAT_DETECT);
    }
}
