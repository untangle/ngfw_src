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

package com.untangle.gui.util;

import java.util.*;

import com.untangle.uvm.security.Tid;
import com.untangle.uvm.tapi.IPSessionDesc;
import com.untangle.uvm.node.*;

public class StatsCache implements Shutdownable
{
    protected static long NORMAL_SLEEP_MILLIS = 1000l;
    protected static long PROBLEM_SLEEP_MILLIS = 10000l;

    private final StatsCacheUpdateThread statsCacheUpdateThread;

    private final HashMap<Tid, FakeNode> fakies;

    public StatsCache(){
        fakies = new HashMap<Tid, FakeNode>();
        statsCacheUpdateThread = new StatsCacheUpdateThread();
        Util.addShutdownable("StatsCacheUpdateThread", statsCacheUpdateThread);
    }

    public void start(){
        statsCacheUpdateThread.start();
    }

    public void doShutdown(){
        statsCacheUpdateThread.doShutdown();
    }

    public Node getFakeNode(Tid tid)
    {
        return fakies.get(tid);
    }

    private class StatsCacheUpdateThread extends Thread implements Shutdownable {

        private volatile boolean stop = false;

        public StatsCacheUpdateThread(){
            setName("MVCLIENT-StatsCacheUpdateThread");
            setDaemon(true);
        }

        public void doShutdown(){
            if(!stop){
                stop = true;
                interrupt();
            }
        }


        public void run() {

            Map<Tid, NodeStats> allStats;
            boolean mustClear = false;

            while (!stop) {
                try {
                    // GET ALL NODE STATS, AND KILL IF NECESSSARY
                    allStats = Util.getNodeManager().allNodeStats();
                    if (fakies.size() != allStats.size()) {
                        fakies.clear();
                    }

                    // PUT STATS IN RESPECTIVE SPOTS
                    for(Tid tid : allStats.keySet()){
                        if( fakies.containsKey(tid) ) {
                            fakies.get(tid).setStats(allStats.get(tid));
                        } else {
                            fakies.put(tid, new FakeNode(allStats.get(tid)));
                        }
                    }

                    // PAUSE A REASONABLE AMOUNT OF TIME
                    sleep(NORMAL_SLEEP_MILLIS);
                }
                catch(InterruptedException e){ continue; }
                catch(Exception e){
                    if( !isInterrupted() ){
                        try { Util.handleExceptionWithRestart("Error getting graph data", e); }
                        catch(Exception f){
                            Util.handleExceptionNoRestart("Error getting graph data", f);
                            try{ sleep(PROBLEM_SLEEP_MILLIS); }
                            catch(InterruptedException g){ continue; }
                        }
                    }
                }
            }
            Util.printMessage("StatsCacheUpdateThread Stopped");
        }
    }

    public class FakeNode implements Node {
        private NodeStats stats;

        FakeNode(NodeStats stats) {
            setStats(stats);
        }

        public Tid getTid() { throw new Error("Unsupported"); }
        public NodeState getRunState() { throw new Error("Unsupported"); }
        public boolean neverStarted() { return false; }
        public void start() { throw new Error("Unsupported"); }
        public void disable() { throw new Error("Unsupported"); }
        public void stop() { throw new Error("Unsupported"); }
        public void reconfigure() { throw new Error("Unsupported"); }
        public NodeContext getNodeContext() { throw new Error("Unsupported"); }
        public NodeDesc getNodeDesc() { throw new Error("Unsupported"); }
        public IPSessionDesc[] liveSessionDescs() { throw new Error("Unsupported"); }
        public void dumpSessions() { throw new Error("Unsupported"); }
        public Object getSettings() { throw new Error("Unsupported"); }
        public void setSettings(Object settings) { throw new Error("Unsupported"); }

        public NodeStats getStats() {
            return stats;
        }
        public void setStats(NodeStats stats){
            this.stats = stats;
        }
    }
}
