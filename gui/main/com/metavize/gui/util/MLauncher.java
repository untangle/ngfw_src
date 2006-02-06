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

        // set the proper look and feel, and dynamic resizing
        try {
	    /*
	    com.incors.plaf.alloy.AlloyLookAndFeel.setProperty("alloy.licenseCode", "2006/02/11#imnieves@gmail.com#ay1m62#13knm1");
	    javax.swing.LookAndFeel alloyLnF = new com.incors.plaf.alloy.AlloyLookAndFeel();
	    javax.swing.UIManager.setLookAndFeel(alloyLnF);
	    */
            com.incors.plaf.kunststoff.KunststoffLookAndFeel kunststoffLaf = new com.incors.plaf.kunststoff.KunststoffLookAndFeel();
            kunststoffLaf.setCurrentTheme(new com.incors.plaf.kunststoff.KunststoffTheme());
            UIManager.setLookAndFeel(kunststoffLaf);
            Toolkit.getDefaultToolkit().setDynamicLayout(true);
        }
        catch (Exception e) {
            Util.handleExceptionNoRestart("Error setting LAF:", e);
        }

	// INITIALIZE UTIL
	Util.initialize();
	
        // SET THE REPAINT MANAGER
        RepaintManager.setCurrentManager( new DebugRepaintManager() );

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
			     public void refresh(){}
			 }
			 );

        // set up the new class loader
        MURLClassLoader mUrlClassLoader = new MURLClassLoader( Thread.currentThread().getContextClassLoader() );
        Util.setClassLoader( mUrlClassLoader );
        Thread.currentThread().setContextClassLoader(mUrlClassLoader);

        // apply the new class loader to future swing threads
        UIManager.getLookAndFeelDefaults().put("ClassLoader", mUrlClassLoader);

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
