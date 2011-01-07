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

package com.untangle.node.ips;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.localapi.LocalIntfManager;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.PipelineEndpoints;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.IPSession;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.SessionStats;
import com.untangle.uvm.vnet.event.IPDataEvent;

public class IpsDetectionEngine
{
    public static boolean DO_PROFILING = true;

    // Any chunk that takes this long gets an error
    public static final long ERROR_ELAPSED = 50000;
    // Any chunk that takes this long gets a warning
    public static final long WARN_ELAPSED = 10000;

    private int maxChunks = 8;
    private IpsSettings settings = null;
    private Map<String,RuleClassification> classifications = null;

    private IpsRuleManager manager;
    private IpsNodeImpl node;

    private static final LocalIntfManager intfManager = LocalUvmContextFactory.context().localIntfManager();

    // We can't just attach the session info to a session, we have to
    // attach it to the 'pipeline', since we have to access it from
    // multiple pipes (octet & http).  So we keep the registry here.
    private Map<Integer, IpsSessionInfo> sessionInfoMap = new ConcurrentHashMap<Integer, IpsSessionInfo>();

    Map<Integer,List<IpsRuleHeader>> portS2CMap = new ConcurrentHashMap<Integer,List<IpsRuleHeader>>();
    Map<Integer,List<IpsRuleHeader>> portC2SMap = new ConcurrentHashMap<Integer,List<IpsRuleHeader>>();
    // bug1443 -- save memory by memoizing
    List<List<IpsRuleHeader>> allPortMapLists = new ArrayList<List<IpsRuleHeader>>();

    private final Logger log = Logger.getLogger(getClass());

    public IpsDetectionEngine(IpsNodeImpl node)
    {
        this.node = node;
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

    public IpsSettings getSettings()
    {
        return settings;
    }

    public void setSettings(IpsSettings settings)
    {
        this.settings = settings;
    }

    //fix this - settigns?
    public void setMaxChunks(int max)
    {
        maxChunks = max;
    }

    public int getMaxChunks()
    {
        return maxChunks;
    }

    public void onReconfigure()
    {
        portC2SMap = new ConcurrentHashMap<Integer,List<IpsRuleHeader>>();
        portS2CMap = new ConcurrentHashMap<Integer,List<IpsRuleHeader>>();
        allPortMapLists = new ArrayList<List<IpsRuleHeader>>();

        log.debug("Done with reconfigure");
    }

    public void stop()
    {
        portC2SMap = new ConcurrentHashMap<Integer,List<IpsRuleHeader>>();
        portS2CMap = new ConcurrentHashMap<Integer,List<IpsRuleHeader>>();
        allPortMapLists = new ArrayList<List<IpsRuleHeader>>();
        sessionInfoMap = new ConcurrentHashMap<Integer, IpsSessionInfo>();
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
            log.warn("Could not parse rule: ", e);
        } catch (Exception e) {
            log.error("Some sort of really bad exception: ", e);
            log.error("For rule: " + rule);
        }
        return false;
    }

    public void processNewSessionRequest(IPNewSessionRequest request,
                                         Protocol protocol)
    {
        //Get Mapped list
        List<IpsRuleHeader> c2sList = portC2SMap.get(request.serverPort());
        List<IpsRuleHeader> s2cList = portS2CMap.get(request.serverPort());

        if(c2sList == null) {
            c2sList = manager.matchingPortsList(request.serverPort(), IpsRuleManager.TO_SERVER);
            // bug1443 -- save memory by reusing value.
            synchronized(allPortMapLists) {
                boolean found = false;
                for (Iterator<List<IpsRuleHeader>> iter = allPortMapLists.iterator(); iter.hasNext();) {
                    List<IpsRuleHeader> savedList = iter.next();
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
            s2cList = manager.matchingPortsList(request.serverPort(), IpsRuleManager.TO_CLIENT);
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
                portS2CMap.put(request.serverPort(),s2cList);
            }

            if (log.isDebugEnabled())
                log.debug("s2cHeader list Size: "+s2cList.size() + " For port: "+request.serverPort());
        }

        //Check matches
        PipelineEndpoints pe = request.pipelineEndpoints();
        boolean incoming = intfManager.getInterfaceComparator().isMoreTrusted(pe.getServerIntf(), pe.getClientIntf());
        Set<IpsRuleSignature> c2sSignatures = manager.matchesHeader(request, incoming, IpsRuleManager.TO_SERVER, c2sList);
        Set<IpsRuleSignature> s2cSignatures = manager.matchesHeader(request, incoming, IpsRuleManager.TO_CLIENT, s2cList);

        if (log.isDebugEnabled())
            log.debug("s2cSignature list size: " + s2cSignatures.size() + ", c2sSignature list size: " +
                      c2sSignatures.size());
        if (c2sSignatures.size() > 0 || s2cSignatures.size() > 0) {
            request.attach(new Object[] { c2sSignatures, s2cSignatures });
        } else {
            request.release();
        }
    }

    public IpsSessionInfo getSessionInfo(IPSession session) 
    {
        return sessionInfoMap.get(session.id());
    }

    @SuppressWarnings("unchecked") //attachment
    public void processNewSession(IPSession session, Protocol protocol) 
    {
        Object[] sigs = (Object[]) session.attachment();
        Set<IpsRuleSignature> c2sSignatures = (Set<IpsRuleSignature>) sigs[0];
        Set<IpsRuleSignature> s2cSignatures = (Set<IpsRuleSignature>) sigs[1];

        log.debug("registering IpsSessionInfo");
        IpsSessionInfo info = new IpsSessionInfo(node, session, c2sSignatures,
                                                 s2cSignatures);
        sessionInfoMap.put(session.id(), info);
        session.attach(null);
    }

    public void processFinalized(IPSession session, Protocol protocol) 
    {
        log.debug("unregistering IpsSessionInfo");
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
    public void handleChunk(IPDataEvent event, IPSession session,
                            boolean isFromServer)
    {
        try {
            long startTime = System.currentTimeMillis();

            SessionStats stats = session.stats();

            IpsSessionInfo info = sessionInfoMap.get(session.id());

            info.setEvent(event);
            info.setFlow(isFromServer);

            node.incrementScanCount();

            boolean result;
            if(isFromServer)
                result = info.processS2CSignatures();
            else
                result = info.processC2SSignatures();

            if (!result) {
                node.statisticManager.incrDNC();
                if (stats.s2tChunks() > maxChunks || stats.c2tChunks() > maxChunks) {
                    session.release();
                    // Free up storage immediately in case session
                    // stays around a long time.
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
        IpsRuleSignature.dumpRuleTimes();
    }
}
