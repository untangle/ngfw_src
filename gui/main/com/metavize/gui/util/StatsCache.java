/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: StatsCache.java 194 2005-04-06 19:13:55Z inieves $
 */

package com.metavize.gui.util;

import java.util.*;

import com.metavize.mvvm.security.Tid;
import com.metavize.mvvm.tapi.IPSessionDesc;
import com.metavize.mvvm.tran.*;

public class StatsCache
{
    protected static long SLEEP_MILLIS = 1000l;

    protected UpdateThread updateThread;

    protected HashMap<Tid, FakeTransform> fakies;
    private volatile boolean killUpdate = false;

    public StatsCache()
    {
        fakies = new HashMap<Tid, FakeTransform>();
        updateThread = new UpdateThread();
        updateThread.start();
    }

    public Transform getFakeTransform(Tid tid)
    {
        return fakies.get(tid);
    }

    protected void killUpdate(){
        killUpdate = true;
    }

    protected class UpdateThread extends Thread implements Killable {
	// KILLABLE //////////
	private volatile boolean killed;
	public void setKilled(boolean killed){ this.killed = killed; }
	///////////////////////

        protected UpdateThread() {
	    super("MVCLIENT-StatsCache.UpdateThread");
            this.setDaemon(true);
	    Util.addKillableThread(this);
        }

        public void run() {
            while(true) {
                try {
                    // KILL UPDATE IF NECESSARY
                    if(killUpdate)
                        return;
                    if(killed)
                        return;

                    // get all transform stats
                    Map<Tid, TransformStats> allStats = Util.getTransformManager().allTransformStats();

                    fakies.clear();
                    for (Iterator<Tid> iter = allStats.keySet().iterator(); iter.hasNext();) {
                        Tid tid = iter.next();
                        TransformStats stats = allStats.get(tid);
                        FakeTransform fakie = new FakeTransform(stats);
                        fakies.put(tid, fakie);
                    }

                    // PAUSE A NORMAL AMOUNT OF TIME
                    Thread.sleep(SLEEP_MILLIS);
                }
		catch (Exception e) {
		    try{
			Util.handleExceptionWithRestart("Error getting graph data", e);
		    }
		    catch(Exception f){
			Util.handleExceptionNoRestart("Error getting graph data", f);
			// Server is probably down.
			// This is ugly: XXXXXXXXXXXXXXX
			try { Thread.currentThread().sleep(10000); } catch(Exception g) {}
		    }
		}
            }
        }
    }
    
    public class FakeTransform implements Transform {
        private TransformStats stats;

        FakeTransform(TransformStats stats) {
            this.stats = stats;
        }

        public Tid getTid() { throw new Error("Unsupported"); }
        public TransformState getRunState() { throw new Error("Unsupported"); }
        public void start() { throw new Error("Unsupported"); }
        public void disable() { throw new Error("Unsupported"); }
        public void stop() { throw new Error("Unsupported"); }
        public void reconfigure() { throw new Error("Unsupported"); }
        public TransformContext getTransformContext() { throw new Error("Unsupported"); }
        public TransformDesc getTransformDesc() { throw new Error("Unsupported"); }
        public IPSessionDesc[] liveSessionDescs() { throw new Error("Unsupported"); }
        public void dumpSessions() { throw new Error("Unsupported"); }
        public Object getSettings() { throw new Error("Unsupported"); }
        public void setSettings(Object settings) { throw new Error("Unsupported"); }

        public TransformStats getStats() {
            return stats;
        }
    }
}
