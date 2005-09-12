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

import java.lang.reflect.Constructor;    
import java.security.*;

import java.net.URLClassLoader;
import java.net.URL;


public class MLauncher {
        
    public static void main(String args[]) {

	// set the proper look and feel, and dynamic resizing
        try {
          com.incors.plaf.kunststoff.KunststoffLookAndFeel kunststoffLaf = new com.incors.plaf.kunststoff.KunststoffLookAndFeel();
          kunststoffLaf.setCurrentTheme(new com.incors.plaf.kunststoff.KunststoffTheme());
          javax.swing.UIManager.setLookAndFeel(kunststoffLaf);
          java.awt.Toolkit.getDefaultToolkit().setDynamicLayout(true);
        }
        catch (Exception e) {
            Util.handleExceptionNoRestart("Error setting LAF:", e);
        }

	// SET THE REPAINT MANAGER
	javax.swing.RepaintManager.setCurrentManager( new DebugRepaintManager() );
        
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
        MURLClassLoader mUrlClassLoader = new MURLClassLoader( Thread.currentThread().getContextClassLoader() );
        Util.setClassLoader( mUrlClassLoader );
        Thread.currentThread().setContextClassLoader(mUrlClassLoader);
    
        // apply the new class loader to future swing threads
	javax.swing.UIManager.getLookAndFeelDefaults().put("ClassLoader", mUrlClassLoader);

	// THE OLD WAY OF SETTING THE CLASSLOADER
	/*        try{ 
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
        */

        // load and start the login dialog
        new com.metavize.gui.login.MLoginJFrame(args);
    }

    
    static private class ShutdownHookThread extends Thread {
	public ShutdownHookThread(){
	    super("MVCLIENT-ShutdownHookThread");
	}
        public void run(){

	    com.metavize.mvvm.client.MvvmRemoteContextFactory.factory().logout();

	}
    }
        
}
