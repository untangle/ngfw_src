package com.untangle.node.ips;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.NodeSessionStats;
import com.untangle.uvm.vnet.event.IPDataEvent;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.util.LoadAvg;

public class IpsDetectionEngine
{
    private final Logger logger = Logger.getLogger(getClass());

    public static boolean DO_PROFILING = true;

    // Any chunk that takes this long gets an error
    public static final long ERROR_ELAPSED = 50000;
    // Any chunk that takes this long gets a warning
    public static final long WARN_ELAPSED = 10000;

    // We can't just attach the session info to a session, we have to
    // attach it to the 'pipeline', since we have to access it from
    // multiple pipes (octet & http).  So we keep the registry here.
    private static Map<Long, IpsSessionInfo> sessionInfoMap = null;

    private int maxChunks = 4;
    private Map<String,RuleClassification> classifications = null;

    private IpsRuleManager manager;
    private IpsNodeImpl node;

    Map<Integer,List<IpsRuleHeader>> portS2CMap = new ConcurrentHashMap<Integer,List<IpsRuleHeader>>();
    Map<Integer,List<IpsRuleHeader>> portC2SMap = new ConcurrentHashMap<Integer,List<IpsRuleHeader>>();
    // bug1443 -- save memory by memoizing
    List<List<IpsRuleHeader>> allPortMapLists = new ArrayList<List<IpsRuleHeader>>();

    public IpsDetectionEngine(IpsNodeImpl node)
    {
        this.node = node;
        synchronized ( IpsDetectionEngine.class ) {
            if ( this.sessionInfoMap == null ) {
                this.sessionInfoMap = new ConcurrentHashMap<Long, IpsSessionInfo>();
            }
        }
        manager = new IpsRuleManager(node);
    }

    public RuleClassification getClassification(String classificationName)
    {
        return classifications.get(classificationName);
    }

    public void setClassifications(List<RuleClassification> classificationList)
    {
        classifications = new HashMap<String, RuleClassification>();
        for (RuleClassification rc : classificationList)
            classifications.put(rc.getName(), rc);
    }

    public void incrementDetectCount()
    {
        node.incrementDetectCount();
    }

    public void incrementBlockCount()
    {
        node.incrementBlockCount();
    }

    public void onReconfigure()
    {
        portC2SMap = new ConcurrentHashMap<Integer,List<IpsRuleHeader>>();
        portS2CMap = new ConcurrentHashMap<Integer,List<IpsRuleHeader>>();
        allPortMapLists = new ArrayList<List<IpsRuleHeader>>();

        logger.debug("Done with reconfigure");
    }

    public void stop()
    {
        portC2SMap = new ConcurrentHashMap<Integer,List<IpsRuleHeader>>();
        portS2CMap = new ConcurrentHashMap<Integer,List<IpsRuleHeader>>();
        allPortMapLists = new ArrayList<List<IpsRuleHeader>>();
        sessionInfoMap = new ConcurrentHashMap<Long, IpsSessionInfo>();
    }

    public void clearRules()
    {
        manager.clearRules();
    }

    public boolean addRule(IpsRule rule)
    {
        try {
            return (manager.addRule(rule));
        } catch (ParseException e) {
            logger.warn("Could not parse rule: ", e);
        } catch (Exception e) {
            logger.error("Some sort of really bad exception: ", e);
            logger.error("For rule: " + rule);
        }
        return false;
    }

    public void processNewSessionRequest(IPNewSessionRequest request, Protocol protocol)
    {
        node.incrementScanCount();

        //Get Mapped list
        List<IpsRuleHeader> c2sList = portC2SMap.get(request.getServerPort());
        List<IpsRuleHeader> s2cList = portS2CMap.get(request.getServerPort());

        if ( LoadAvg.get().getOneMin() >= node.getSettings().getLoadBypassLimit() ) {
            logger.debug("Releasing session (load bypass limit exceeded): " + request);
            request.release();
            return;
        }
        if ( sessionInfoMap.size() > node.getSettings().getSessionBypassLimit() ) {
            logger.debug("Releasing session (session bypass limit exceeded): " + request);
            request.release();
            return;
        }
        
        if(c2sList == null) {
            c2sList = manager.matchingPortsList( request.getServerPort(), true );
            // bug1443 -- save memory by reusing value.
            synchronized(allPortMapLists) {
                boolean found = false;
                for ( Iterator<List<IpsRuleHeader>> iter = allPortMapLists.iterator(); iter.hasNext(); ) {
                    List<IpsRuleHeader> savedList = iter.next();
                    if (savedList.equals(c2sList)) {
                        c2sList = savedList;
                        found = true;
                        break;
                    }
                }
                if (!found)
                    allPortMapLists.add(c2sList);
                portC2SMap.put(request.getServerPort(),c2sList);
            }

            if (logger.isDebugEnabled())
                logger.debug("c2sHeader list Size: "+c2sList.size() + " For port: "+request.getServerPort());
        }

        if(s2cList == null) {
            s2cList = manager.matchingPortsList( request.getServerPort(), false );
            synchronized(allPortMapLists) {
                boolean found = false;
                for (Iterator<List<IpsRuleHeader>> iter = allPortMapLists.iterator(); iter.hasNext();) {
                    List<IpsRuleHeader> savedList = iter.next();
                    if (savedList.equals(s2cList)) {
                        s2cList = savedList;
                        found = true;
                        break;
                    }
                }
                if (!found)
                    allPortMapLists.add(s2cList);
                portS2CMap.put(request.getServerPort(),s2cList);
            }

            if (logger.isDebugEnabled())
                logger.debug("s2cHeader list Size: "+s2cList.size() + " For port: "+request.getServerPort());
        }

        //Check matches
        SessionEvent pe = request.sessionEvent();
        
        boolean incoming = true;
        Integer clientIntf = pe.getClientIntf();
        if (clientIntf != null) { 
            if (clientIntf == 250) { /* OpenVPN */
                incoming = true;
            } else {
                InterfaceSettings sourceIntf = UvmContextFactory.context().networkManager().findInterfaceId( clientIntf );
                if (sourceIntf == null) {
                    logger.warn("Unable to find source interface: " + clientIntf);
                } else {
                    incoming = sourceIntf.getIsWan();
                }
            }
        }
        
        Set<IpsRuleSignature> c2sSignatures = manager.matchesHeader(request, incoming, true, c2sList);
        Set<IpsRuleSignature> s2cSignatures = manager.matchesHeader(request, incoming, false, s2cList);

        if (logger.isDebugEnabled())
            logger.debug("s2cSignature list size: " + s2cSignatures.size() + ", c2sSignature list size: " + c2sSignatures.size());

        if (c2sSignatures.size() > 0 || s2cSignatures.size() > 0) {
            request.attach(new Object[] { c2sSignatures, s2cSignatures });

        } else {
            logger.debug("Releasing session (no rules to evaluate): " + request);
            request.release();
        }
    }

    public IpsSessionInfo getSessionInfo(NodeSession session) 
    {
        return sessionInfoMap.get(session.id());
    }

    @SuppressWarnings("unchecked") //attachment
    public void processNewSession(NodeSession session, Protocol protocol) 
    {
        Object[] sigs = (Object[]) session.attachment();

        if ( sigs == null ) {
            session.release();
            return;
        }

        Set<IpsRuleSignature> c2sSignatures = (Set<IpsRuleSignature>) sigs[0];
        Set<IpsRuleSignature> s2cSignatures = (Set<IpsRuleSignature>) sigs[1];

        logger.debug("registering IpsSessionInfo");
        IpsSessionInfo info = new IpsSessionInfo(node, session, c2sSignatures, s2cSignatures);
        sessionInfoMap.put(session.id(), info);
        session.attach(null);
    }

    public void processFinalized(NodeSession session, Protocol protocol) 
    {
        logger.debug("unregistering IpsSessionInfo");
        sessionInfoMap.remove(session.id());
    }

    public IpsRuleManager getRulesForTesting() 
    {
        return manager;
    }

    public void dumpRules()
    {
        manager.dumpRules();
    }

    //In process of fixing this
    public void handleChunk(IPDataEvent event, NodeSession session, boolean isFromServer)
    {
        try {
            long startTime = System.currentTimeMillis();

            NodeSessionStats stats = session.stats();
    
            IpsSessionInfo info = sessionInfoMap.get(session.id());
            if ( info == null ) {
                logger.warn("Missing IpsSessionInfo: " + session);
                session.release();
                return;
            }
            
            info.setEvent(event);
            info.setFlow(isFromServer);

            boolean result;
            if(isFromServer)
                result = info.processS2CSignatures();
            else
                result = info.processC2SSignatures();

            if (!result) {
                int maxChunks = node.getSettings().getMaxChunks();
                if (stats.s2tChunks() > maxChunks || stats.c2tChunks() > maxChunks) {
                    sessionInfoMap.remove(session.id());
                    session.release();
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;

            if (isFromServer) {
                int numsigs = info.numS2CSignatures();
                if (elapsed > ERROR_ELAPSED) {
                    dumpProfile();
                    logger.error("took " + elapsed + "ms to run " + numsigs + " s2c rules");
                } else if (elapsed > WARN_ELAPSED) {
                    logger.warn("took " + elapsed + "ms to run " + numsigs + " s2c rules");
                } else if (logger.isDebugEnabled()) {
                    logger.debug("ms to run " + numsigs + " s2c rules: " + elapsed);
                }
            } else {
                int numsigs = info.numC2SSignatures();
                if (elapsed > ERROR_ELAPSED) {
                    dumpProfile();
                    logger.error("took " + elapsed + "ms to run " + numsigs + " c2s rules");
                } else if (elapsed > WARN_ELAPSED) {
                    logger.warn("took " + elapsed + "ms to run " + numsigs + " c2s rules");
                } else if (logger.isDebugEnabled()) {
                    logger.debug("ms to run " + numsigs + " c2s rules: " + elapsed);
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing chunk: ", e);
        }
    }

    private synchronized void dumpProfile() {
        IpsRuleSignature.dumpRuleTimes();
    }
}
