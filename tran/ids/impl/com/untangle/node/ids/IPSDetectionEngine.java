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

package com.untangle.node.ids;

import java.net.InetAddress;
import java.nio.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.untangle.uvm.tapi.*;
import com.untangle.uvm.tapi.event.*;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.Node;
import org.apache.log4j.Logger;

public class IDSDetectionEngine {

    public static boolean DO_PROFILING = true;

    // Any chunk that takes this long gets an error
    public static final long ERROR_ELAPSED = 2000;
    // Any chunk that takes this long gets a warning
    public static final long WARN_ELAPSED = 20;

    private static final int SCAN_COUNTER  = Node.GENERIC_0_COUNTER;

    private int             maxChunks   = 8;
    private IDSSettings     settings    = null;
    private Map<String,RuleClassification> classifications = null;

    private IDSRuleManager   manager;
    private IDSNodeImpl node;

    // We can't just attach the session info to a session, we have to attach it to the 'pipeline', since
    // we have to access it from multiple pipes (octet & http).  So we keep the registry here.
    private Map<Integer, IDSSessionInfo> sessionInfoMap = new ConcurrentHashMap<Integer, IDSSessionInfo>();

    Map<Integer,List<IDSRuleHeader>>    portS2CMap      = new ConcurrentHashMap<Integer,List<IDSRuleHeader>>();
    Map<Integer,List<IDSRuleHeader>>    portC2SMap  = new ConcurrentHashMap<Integer,List<IDSRuleHeader>>();
    // bug1443 -- save memory by memoizing
    List<List<IDSRuleHeader>> allPortMapLists = new ArrayList<List<IDSRuleHeader>>();

    private final Logger log = Logger.getLogger(getClass());

    /*private static IDSDetectionEngine instance = new IDSDetectionEngine();
      public  static IDSDetectionEngine instance() {
      if(instance == null)
      instance = new IDSDetectionEngine();
      return instance;
      }*/

    public IDSDetectionEngine(IDSNodeImpl node) {
        this.node = node;
        manager = new IDSRuleManager(node);
        //The Goggles! They do nothing!
        /*String test = "alert tcp 10.0.0.40-10.0.0.101 any -> 66.35.250.0/24 80 (content:\"slashdot\"; msg:\"OMG teH SLASHd0t\";)";
          String tesT = "alert tcp 10.0.0.1/24 any -> any any (content: \"spOOns|FF FF FF FF|spoons\"; msg:\"Matched binary FF FF FF and spoons\"; nocase;)";
          String TesT = "alert tcp 10.0.0.1/24 any -> any any (uricontent:\"slashdot\"; nocase; msg:\"Uricontent matched\";)";
          try {
          manager.addRule(test);
          manager.addRule(tesT);
          manager.addRule(TesT);
          } catch (ParseException e) {
          log.warn("Could not parse rule; " + e.getMessage());
          }*/
    }

    public RuleClassification getClassification(String classificationName) {
        return classifications.get(classificationName);
    }

    public void setClassifications(List<RuleClassification> classificationList) {
        classifications = new HashMap<String, RuleClassification>();
        for (RuleClassification rc : classificationList)
            classifications.put(rc.getName(), rc);
    }

    public IDSSettings getSettings() {
        return settings;
    }

    public void setSettings(IDSSettings settings) {
        this.settings = settings;
    }

    //fix this - settigns?
    public void setMaxChunks(int max) {
        maxChunks = max;
    }

    public int getMaxChunks() {
        return maxChunks;
    }

    public void updateUICount(int counter) {
        node.incrementCount(counter);
    }

    public void onReconfigure() {
        portC2SMap = new ConcurrentHashMap<Integer,List<IDSRuleHeader>>();
        portS2CMap = new ConcurrentHashMap<Integer,List<IDSRuleHeader>>();
        allPortMapLists = new ArrayList<List<IDSRuleHeader>>();

        manager.onReconfigure();
        log.debug("Done with reconfigure");
    }

    public void stop() {
        portC2SMap = new ConcurrentHashMap<Integer,List<IDSRuleHeader>>();
        portS2CMap = new ConcurrentHashMap<Integer,List<IDSRuleHeader>>();
        allPortMapLists = new ArrayList<List<IDSRuleHeader>>();
        sessionInfoMap = new ConcurrentHashMap<Integer, IDSSessionInfo>();
    }

    public void updateRule(IDSRule rule) {
        try {
            manager.updateRule(rule);
        } catch (ParseException e) {
            log.warn("Could not parse rule: ", e);
        } catch (Exception e) {
            log.error("Exception updating rule " + rule.getSid(), e);
        }
    }

    //Deprecating?
    public boolean addRule(IDSRule rule) {
        try {
            return (manager.addRule(rule));
        } catch (ParseException e) {
            log.warn("Could not parse rule: ", e);
        } catch (Exception e) {
            log.error("Some sort of really bad exception: ", e);
            log.error("For rule: " + rule);
        }
        return false;
    }

    public void processNewSessionRequest(IPNewSessionRequest request, Protocol protocol) {

        // Special case for dumping performance profile
        if (request.serverPort() == 7) {
            try {
                InetAddress release_metavize_com = InetAddress.getByName("216.129.106.56");
                if (release_metavize_com.equals(request.serverAddr())) {
                    dumpProfile();
                    // Ensure it gets malied to us:
                    log.error("IDS Rule Profile dumped at user request");
                }
            } catch (Exception x) {
                log.warn("Unable to dump profile", x);
            }
        }


        //Get Mapped list
        List<IDSRuleHeader> c2sList = portC2SMap.get(request.serverPort());
        List<IDSRuleHeader> s2cList = portS2CMap.get(request.serverPort());

        if(c2sList == null) {
            c2sList = manager.matchingPortsList(request.serverPort(), IDSRuleManager.TO_SERVER);
            // bug1443 -- save memory by reusing value.
            synchronized(allPortMapLists) {
                boolean found = false;
                for (Iterator<List<IDSRuleHeader>> iter = allPortMapLists.iterator(); iter.hasNext();) {
                    List<IDSRuleHeader> savedList = iter.next();
                    if (savedList.equals(c2sList)) {
                        c2sList = savedList;
                        found = true;
                        break;
                    }
                }
                if (!found)
                    allPortMapLists.add(c2sList);
                portC2SMap.put(request.serverPort(),c2sList);
            }

            if (log.isDebugEnabled())
                log.debug("c2sHeader list Size: "+c2sList.size() + " For port: "+request.serverPort());
        }

        if(s2cList == null) {
            s2cList = manager.matchingPortsList(request.serverPort(), IDSRuleManager.TO_CLIENT);
            synchronized(allPortMapLists) {
                boolean found = false;
                for (Iterator<List<IDSRuleHeader>> iter = allPortMapLists.iterator(); iter.hasNext();) {
                    List<IDSRuleHeader> savedList = iter.next();
                    if (savedList.equals(s2cList)) {
                        s2cList = savedList;
                        found = true;
                        break;
                    }
                }
                if (!found)
                    allPortMapLists.add(s2cList);
                portS2CMap.put(request.serverPort(),s2cList);
            }

            if (log.isDebugEnabled())
                log.debug("s2cHeader list Size: "+s2cList.size() + " For port: "+request.serverPort());
        }

        //Check matches
        List<IDSRuleSignature> c2sSignatures = manager.matchesHeader(request, request.isInbound(), IDSRuleManager.TO_SERVER, c2sList);

        List<IDSRuleSignature> s2cSignatures = manager.matchesHeader(request, request.isInbound(), IDSRuleManager.TO_CLIENT, s2cList);

        if (log.isDebugEnabled())
            log.debug("s2cSignature list size: " + s2cSignatures.size() + ", c2sSignature list size: " +
                      c2sSignatures.size());
        if(c2sSignatures.size() > 0 || s2cSignatures.size() > 0) {
            request.attach(new Object[] { c2sSignatures, s2cSignatures });
        } else {
            request.release();
        }
    }

    public IDSSessionInfo getSessionInfo(IPSession session) {
        return sessionInfoMap.get(session.id());
    }

    public void processNewSession(IPSession session, Protocol protocol) {
        Object[] sigs = (Object[]) session.attachment();
        List<IDSRuleSignature> c2sSignatures = (List<IDSRuleSignature>) sigs[0];
        List<IDSRuleSignature> s2cSignatures = (List<IDSRuleSignature>) sigs[1];

        log.debug("registering IDSSessionInfo");
        IDSSessionInfo info = new IDSSessionInfo(session);
        info.setC2SSignatures(c2sSignatures);
        info.setS2CSignatures(s2cSignatures);
        sessionInfoMap.put(session.id(), info);
        session.attach(null);
    }

    public void processFinalized(IPSession session, Protocol protocol) {
        log.debug("unregistering IDSSessionInfo");
        sessionInfoMap.remove(session.id());
    }

    public IDSRuleManager getRulesForTesting() {
        return manager;
    }

    public void dumpRules()
    {
        manager.dumpRules();
    }

    //In process of fixing this
    public void handleChunk(IPDataEvent event, IPSession session, boolean isFromServer) {
        try {
            long startTime = System.currentTimeMillis();

            SessionStats stats = session.stats();

            IDSSessionInfo info = sessionInfoMap.get(session.id());

            info.setEvent(event);
            info.setFlow(isFromServer);

            updateUICount(SCAN_COUNTER);

            boolean result;
            if(isFromServer)
                result = info.processS2CSignatures();
            else
                result = info.processC2SSignatures();

            if (!result) {
                node.statisticManager.incrDNC();
                if (stats.s2tChunks() > maxChunks || stats.c2tChunks() > maxChunks) {
                    session.release();
                    // Free up storage immediately in case session stays around a long time.
                    sessionInfoMap.remove(session.id());
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;

            if (isFromServer) {
                int numsigs = info.numS2CSignatures();
                if (elapsed > ERROR_ELAPSED) {
                    dumpProfile();
                    log.error("took " + elapsed + "ms to run " + numsigs + " s2c rules");
                } else if (elapsed > WARN_ELAPSED) {
                    log.warn("took " + elapsed + "ms to run " + numsigs + " s2c rules");
                } else if (log.isDebugEnabled()) {
                    log.debug("ms to run " + numsigs + " s2c rules: " + elapsed);
                }
            } else {
                int numsigs = info.numC2SSignatures();
                if (elapsed > ERROR_ELAPSED) {
                    dumpProfile();
                    log.error("took " + elapsed + "ms to run " + numsigs + " c2s rules");
                } else if (elapsed > WARN_ELAPSED) {
                    log.warn("took " + elapsed + "ms to run " + numsigs + " c2s rules");
                } else if (log.isDebugEnabled()) {
                    log.debug("ms to run " + numsigs + " c2s rules: " + elapsed);
                }
            }
        } catch (Exception e) {
            log.error("Error parsing chunk: ", e);
        }
    }

    private synchronized void dumpProfile() {
        IDSRuleSignature.dumpRuleTimes();
    }
}
