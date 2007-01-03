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

import com.untangle.gui.login.*;
import com.untangle.mvvm.client.*;

import java.awt.*;
import javax.swing.*;
import java.security.*;
import java.net.*;


public class MLauncher {

    private static boolean isActivated;
    private static boolean isRegistered;

    public static void main(final String args[]) {

        // SET CLASS LOADER NORMAL WAYS
        final MURLClassLoader mUrlClassLoader = new MURLClassLoader( MLauncher.class.getClassLoader() );
        Thread.currentThread().setContextClassLoader(mUrlClassLoader);

        // SET CLASS LOADER AGGRESSIVELY IN CASE SWING STARTED EARLY
        try{
            EventQueue eq = Toolkit.getDefaultToolkit().getSystemEventQueue();
            eq.invokeAndWait( new Runnable(){ public void run(){
                Thread.currentThread().setContextClassLoader(mUrlClassLoader);
            }});
        }
        catch(Exception e){
            System.err.println("Error setting class loader:");
            e.printStackTrace();
        }

        // PROPERTY CHANGE LISTENER WORKAROUND FOR ALTERNATE LOOK AND FEEL BUG
        UIManager.addPropertyChangeListener(new LAFPropertyChangeListener());

        // SET LAF
        try {
            com.incors.plaf.alloy.AlloyLookAndFeel.setProperty("alloy.licenseCode", "7#Metavize_Inc.#1f75cs6#2n7ryw");
            com.incors.plaf.alloy.AlloyTheme theme =
                com.incors.plaf.alloy.themes.custom.CustomThemeFactory.createTheme(new Color(152,152,171), // PROGRESS & SCROLL
                                                                                   new Color(215,215,215), // BACKGROUND
                                                                                   Color.ORANGE, // NO IDEA
                                                                                   new Color(50,50,50), // RADIO / CHECKBOX
                                                                                   new Color(104,189,73), // MOUSEOVER
                                                                                   new Color(215,215,215)); // POPUPS
            javax.swing.LookAndFeel alloyLnF = new com.incors.plaf.alloy.AlloyLookAndFeel(theme);
            Util.setLookAndFeel(alloyLnF);
            UIManager.setLookAndFeel(alloyLnF);            
            UIManager.getLookAndFeelDefaults().put("ClassLoader", mUrlClassLoader);
        }
        catch (Exception e) {
            System.err.println("Error starting LAF:");
            e.printStackTrace();
        }


        // SET CLASSLOADER
        Util.initialize();
        Util.setClassLoader( mUrlClassLoader );
	
        // SET THE REPAINT OPTIONS
        //RepaintManager.setCurrentManager( new DebugRepaintManager() );
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
        Policy.setPolicy( new Policy(){
                public PermissionCollection getPermissions(CodeSource codeSource){
                    Permissions permissions = new Permissions();
                    permissions.add(new AllPermission());
                    return permissions;
                }
                public void refresh(){
                }
            } );

        // ARGS
        for( String arg : args ){
            if( arg.equals("local") ){
                Util.setLocal(true);
            }
            if( arg.equals("cdinstall") ){
                Util.setIsCD(true);
            }
        }

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
        if( isActivated || (!isActivated && isRegistered && InitialSetupRoutingJPanel.getNatEnabled() && !InitialSetupRoutingJPanel.getNatChanged()) )
            new MLoginJFrame(args);
        else
            Util.exit(0);

    }


    static private class ShutdownHookThread extends Thread {
        public ShutdownHookThread(){
            super("MVCLIENT-ShutdownHookThread");
            setDaemon(true);
        }
        public void run(){
            try{ MvvmRemoteContextFactory.factory().logout(); }
            catch(Exception e){ Util.handleExceptionNoRestart("Error logging out", e); }
        }
    }


    static private class WatchDogThread extends Thread {
        public WatchDogThread(){
            setDaemon(true);
            start();
        }
        public void run(){
            try{
                while(true){
                    System.out.println("LAF name: " + UIManager.getLookAndFeel().getName());
                    System.out.println("LAF ClassLoader: " + UIManager.getLookAndFeelDefaults().get("ClassLoader"));
                    EventQueue eq = Toolkit.getDefaultToolkit().getSystemEventQueue();
                    eq.invokeAndWait( new Runnable(){ public void run(){
                        System.out.println("EDT name: " + Thread.currentThread().getName());
                        System.out.println("EDT ClassLoader: " + Thread.currentThread().getContextClassLoader());
                    }});
                    System.out.println("-----------------" );
                    sleep(5000l);
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }


    static private class LAFPropertyChangeListener implements java.beans.PropertyChangeListener {
        public LAFPropertyChangeListener(){
        }
        public void propertyChange(java.beans.PropertyChangeEvent evt){
            if( evt.getPropertyName().equals("lookAndFeel") ){
                if( (Util.getLookAndFeel() != null) && !(Util.getLookAndFeel().equals(evt.getNewValue())) )
                    try{ UIManager.setLookAndFeel(Util.getLookAndFeel()); }
                    catch(Exception e){
                        System.out.println("Error resetting LAF");
                        e.printStackTrace();
                    }
                }
            }
        }
}



