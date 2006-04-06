/*
 * Copyright (c) 2004, 2005, 2006 Metavize Inc.
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

public class StatsCache implements Shutdownable
{
    protected static long NORMAL_SLEEP_MILLIS = 1000l;
    protected static long PROBLEM_SLEEP_MILLIS = 10000l;

    private final UpdateThread updateThread;

    private final HashMap<Tid, FakeTransform> fakies;

    public StatsCache(){
        fakies = new HashMap<Tid, FakeTransform>();
        updateThread = new UpdateThread();
	Util.addShutdownable("StatsCache", this);
    }

    public void start(){
        updateThread.start();
    }

    public Transform getFakeTransform(Tid tid)
    {
        return fakies.get(tid);
    }

    public void doShutdown(){
        updateThread.doShutdown();
    }

    private class UpdateThread extends Thread implements Shutdownable {

	private volatile boolean stop = false;

	public UpdateThread(){
	    setName("MVCLIENT-StatsCache");
	    setDaemon(true);
	}

        public void doShutdown(){
	    if( !stop ){ // because upgrade can call doShutdown, and cause this call twice
		stop = true;
		this.interrupt();
	    }
        }
       

        public void run() {

            Map<Tid, TransformStats> allStats;
            boolean mustClear = false;

            while (!stop) {
                try {
                    // GET ALL TRANSFORM STATS, AND KILL IF NECESSSARY
                    allStats = Util.getTransformManager().allTransformStats();
                    if (fakies.size() != allStats.size()) {
                        fakies.clear();
                    }

		    // PUT SPOTS IN RESPECTIVE SPOTS
		    for(Tid tid : allStats.keySet()){
			if( fakies.containsKey(tid) ) {
			    fakies.get(tid).setStats(allStats.get(tid));
			} else {
			    fakies.put(tid, new FakeTransform(allStats.get(tid)));
			}
		    }

                    // PAUSE A REASONABLE AMOUNT OF TIME
                    sleep(NORMAL_SLEEP_MILLIS);
                }
		catch (InterruptedException exn) {
                    continue; // reevaluate loop condition
                }
		catch (Exception exn) {
                    try { Util.handleExceptionWithRestart("Error getting graph data", exn); }
		    catch(Exception f){
			Util.handleExceptionNoRestart("Error getting graph data", f);
			try{ sleep(PROBLEM_SLEEP_MILLIS); }
			catch(InterruptedException g){ continue; }
		    }
		}
            }
        }
    }
    
    public class FakeTransform implements Transform {
        private TransformStats stats;

        FakeTransform(TransformStats stats) {
            setStats(stats);
        }

        public Tid getTid() { throw new Error("Unsupported"); }
        public TransformState getRunState() { throw new Error("Unsupported"); }
        public boolean neverStarted() { return false; }
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
        public void setStats(TransformStats stats){
            this.stats = stats;
        }
    }
}
