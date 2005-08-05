/*
 * Copyright (c) 2004,2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.util;

public class AlarmTimer
{
    private Timer timer = null;
    private Thread timerThread = null;
    
    private class Timer implements Runnable
    {
        private Thread sleeperThread;
        private int sleepTimeMilli;
            
        public Timer(Thread thr, int milli)
        {
            this.sleeperThread = thr;
            this.sleepTimeMilli = milli;
        }
        
        public void run()
        {
            try {
                Thread.sleep( this.sleepTimeMilli );
            }
            catch (java.lang.InterruptedException e) {
                /* do nothing */
            }
            System.err.println("Interrupting!!!!!");
            sleeperThread.interrupt();
            return;
        }
    }

    public AlarmTimer (Thread thr, int milli)
    {
        this.timer = new Timer( thr, milli );
    }


    public void startTimer() 
    {
        if (this.timerThread != null)
            throw new IllegalStateException("Timer already started");
            
        this.timerThread = new Thread( this.timer );
        this.timerThread.start();
    }

    public void resetTimer()
    {
        this.killTimer();
        this.startTimer();
    }

    public void killTimer() 
    {
        if (this.timerThread == null)
            throw new IllegalStateException("Timer not started");
        /**
         * interrupt the timer thread, which just exits
         * when interrupted
         */
        this.timerThread.interrupt(); 
    }
}
