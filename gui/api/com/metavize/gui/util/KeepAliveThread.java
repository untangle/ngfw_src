/*
 * Copyright (c) 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.util;

import com.metavize.mvvm.client.MvvmRemoteContext;

public class KeepAliveThread extends Thread implements Shutdownable {

    private static final long PING_DELAY = 60000l; // 1 minute
    private MvvmRemoteContext mvvmRemoteContext;
    private volatile boolean stop = false;

    public KeepAliveThread(MvvmRemoteContext mvvmRemoteContext){
        setName("MV-CLIENT: KeepAliveThread");
        this.mvvmRemoteContext = mvvmRemoteContext;        
        start();
    }
    public void run(){
        while(!stop){
            try{
                mvvmRemoteContext.version();
                sleep(PING_DELAY);
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }
    public void doShutdown(){
        stop = true;
    }
}
