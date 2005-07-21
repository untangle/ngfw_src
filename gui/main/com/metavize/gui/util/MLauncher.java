/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.util;

import com.metavize.gui.login.MLoginJFrame;

import javax.swing.UIManager;
import java.awt.Toolkit;
import java.lang.reflect.Constructor;    
import java.security.*;

import java.net.URLClassLoader;
import java.net.URL;
/**
 *
 * @author inieves
 */
public class MLauncher {
        
    public static void main(String args[]) {
        
        // start a shutdown hook
        Runtime.getRuntime().addShutdownHook( new ShutdownHookThread() );
        
        // give all these class loaders full permissions
        Policy.setPolicy(
                new Policy(){
                    public PermissionCollection getPermissions(CodeSource codeSource){
                        Permissions permissions = new Permissions();
                        permissions.add(new AllPermission());
                        return permissions;
                    }
                    public void refresh(){
                    }
                }
        );

        // set up the new class loader
        MURLClassLoader mURLClassLoader = new MURLClassLoader( Thread.currentThread().getContextClassLoader() );
        Util.setClassLoader( mURLClassLoader );
        Thread.currentThread().setContextClassLoader(mURLClassLoader);
    
        // apply the new class loader to future swing threads
        try{ 
            java.awt.EventQueue eq = java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue();
            eq.invokeAndWait(new Runnable() {
                public void run() {
                    Thread.currentThread().setContextClassLoader(Util.getClassLoader());
                }
            });
        }
        catch(Exception e){
            System.err.println(e);
        }

	// set the proper look and feel, and dynamic resizing
        try {
          com.incors.plaf.kunststoff.KunststoffLookAndFeel kunststoffLnF = new com.incors.plaf.kunststoff.KunststoffLookAndFeel();
          kunststoffLnF.setCurrentTheme(new com.incors.plaf.kunststoff.KunststoffTheme());
          UIManager.setLookAndFeel(kunststoffLnF);
          Toolkit.getDefaultToolkit().setDynamicLayout(true);
        }
        catch (Exception e) {
            Util.handleExceptionNoRestart("Error:", e);
        }

        
        // load and start the login dialog
        new MLoginJFrame(args);
    }

    
    static private class ShutdownHookThread extends Thread {
	public ShutdownHookThread(){
	    super("MVCLIENT-ShutdownHookThread");
	}
        public void run(){
            
        }
    }
        
}
