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

import com.metavize.gui.login.*;
import com.metavize.mvvm.client.*;

import java.awt.*;
import javax.swing.*;
import java.security.*;
import java.net.*;


public class MLauncher {

    private static boolean isActivated;
    private static boolean isRegistered;

    public static void main(final String args[]) {

        // SET LAF
        try {
	    /*
	    com.incors.plaf.alloy.AlloyLookAndFeel.setProperty("alloy.licenseCode", "2006/02/11#imnieves@gmail.com#ay1m62#13knm1");
	    javax.swing.LookAndFeel alloyLnF = new com.incors.plaf.alloy.AlloyLookAndFeel();
	    javax.swing.UIManager.setLookAndFeel(alloyLnF);
	    */
            com.incors.plaf.kunststoff.KunststoffLookAndFeel kunststoffLaf = new com.incors.plaf.kunststoff.KunststoffLookAndFeel();
            kunststoffLaf.setCurrentTheme(new com.incors.plaf.kunststoff.KunststoffTheme());
            UIManager.setLookAndFeel(kunststoffLaf);
        }
        catch (Exception e) {
	    System.err.println("Error starting LAF:");
	    e.printStackTrace();
        }

        // SET CLASS LOADER
        MURLClassLoader mUrlClassLoader = new MURLClassLoader( Thread.currentThread().getContextClassLoader() );
        UIManager.getLookAndFeelDefaults().put("ClassLoader", mUrlClassLoader);
        Thread.currentThread().setContextClassLoader(mUrlClassLoader);

	// INITIALIZE UTIL
	Util.initialize();
        Util.setClassLoader( mUrlClassLoader );
	
        // SET THE REPAINT OPTIONS
        RepaintManager.setCurrentManager( new DebugRepaintManager() );
	try{
	    Toolkit.getDefaultToolkit().setDynamicLayout(true);
	}
	catch(Exception e){
	    System.err.println("Error setting dynamic layout:");
	    e.printStackTrace();
	}

        // SHUTDOWN HOOK
        Runtime.getRuntime().addShutdownHook( new ShutdownHookThread() );

        // FULL PERMISSIONS POLICY
        Policy.setPolicy( 
			 new Policy(){
			     public PermissionCollection getPermissions(CodeSource codeSource){
				 Permissions permissions = new Permissions();
				 permissions.add(new AllPermission());
				 return permissions;
			     }
			     public void refresh(){}
			 }
			 );

        // HANDLE FIRST TIME LOGINS
        try{
            URL url = Util.getServerCodeBase();
            isActivated = MvvmRemoteContextFactory.factory().isActivated( url.getHost(), url.getPort(), 0, Util.isSecureViaHttps() );
        }
        catch(Exception e){
            Util.handleExceptionNoRestart("unable to connect to server for activation check", e);
            isActivated = true;
        }
        if( !isActivated ){
            try{
                SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
                    InitialSetupWizard initialSetupWizard = new InitialSetupWizard();
                    initialSetupWizard.setVisible(true);
		    MLauncher.isRegistered = initialSetupWizard.isRegistered();
		}});
	    }
	    catch(Exception e){ Util.handleExceptionNoRestart("unable to show setup wizard", e); }
        }

        // LOGIN
	if( isActivated || (!isActivated && isRegistered) )
	    new MLoginJFrame(args);
	else
	    Util.exit(0);

    }


    static private class ShutdownHookThread extends Thread {
        public ShutdownHookThread(){
            super("MVCLIENT-ShutdownHookThread");
        }
        public void run(){
            MvvmRemoteContextFactory.factory().logout();
        }
    }
}
