/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.gui.util;

import java.util.*;

import com.untangle.uvm.security.Tid;
import com.untangle.uvm.vnet.IPSessionDesc;
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
