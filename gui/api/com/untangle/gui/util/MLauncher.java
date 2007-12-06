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

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.security.*;
import java.util.Properties;
import javax.swing.*;

import com.untangle.gui.login.*;
import com.untangle.uvm.client.*;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class MLauncher {
    private static final String LOG4J_DEFAULT_PROPERTIES = "com/untangle/gui/log4j.properties";
    private static final String LOG4J_DEVEL_PROPERTIES   = "com/untangle/gui/log4j-devel.properties";

    private static boolean isActivated;
    private static boolean isRegistered;

    private static final Logger logger = Logger.getLogger(MLauncher.class);

    public static void main(final String args[]) {
        String tzStr = System.getProperty("user.timezone");
        if (null != tzStr) {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(tzStr));
        }

        // SET CLASS LOADER NORMAL WAYS
        final MURLClassLoader mUrlClassLoader = new MURLClassLoader( MLauncher.class.getClassLoader() );
        Thread.currentThread().setContextClassLoader(mUrlClassLoader);

        // CONFIGURE LOGGING
        configureLogging();

        // SET CLASS LOADER AGGRESSIVELY IN CASE SWING STARTED EARLY
        try{
            EventQueue eq = Toolkit.getDefaultToolkit().getSystemEventQueue();
            eq.invokeAndWait( new Runnable(){ public void run(){
                Thread.currentThread().setContextClassLoader(mUrlClassLoader);
            }});
        }
        catch(Exception e){
            logger.info("Error setting class loader:", e);
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
            logger.info("Error starting LAF:",e);
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
            logger.info("Error setting dynamic layout:", e);
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
            if( arg.equals("untangle-appliance") ){
                Util.setUntangleAppliance(true);
            }
            if( arg.equals("inside-vm") ){
                Util.setInsideVM(true);
            }
        }

        // HANDLE FIRST TIME LOGINS
        try{
            URL url = Util.getServerCodeBase();
            isActivated = RemoteUvmContextFactory.factory().isActivated( url.getHost(), url.getPort(), 0, Util.isSecureViaHttps() );
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

    static void configureLogging()
    {
        Properties props = new Properties();

        /* defaults in case it can't parse the log4j.properties */
        props.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
        props.setProperty("log4j.appender.A1.layout","org.apache.log4j.PatternLayout");
        props.setProperty("log4j.appender.A1.layout.ConversionPattern",
                          "%d{HH:mm:ss,SSS} (%t) %-5p [%c] - %m%n");
        props.setProperty("log4j.rootLogger=WARN","A1");

        try {
            InputStream is = MLauncher.class.getClassLoader().
                getResourceAsStream(LOG4J_DEFAULT_PROPERTIES);

            if (is != null) {
                props.load(is);
            } else {
                System.err.println("Unable to load default log4j properties");
            }

        } catch ( IOException e ) {
            System.err.println("Unable to load default logging properties.");
            System.err.println("Using defaults." );
        }

        try {
            if ( Util.isDevel()) {
                System.out.println("enabling debug.");

                InputStream is = MLauncher.class.getClassLoader().
                    getResourceAsStream(LOG4J_DEVEL_PROPERTIES);
                if (is != null) {
                    props.load(is);
                } else {
                    System.err.println("Unable to load development log4j properties.");
                }
            }
        } catch ( IOException e ) {
            System.err.println("Unable to load development log4j properties.");
        }

        PropertyConfigurator.configure(props);
    }

    static private class ShutdownHookThread extends Thread {
        public ShutdownHookThread(){
            super("MVCLIENT-ShutdownHookThread");
            setDaemon(true);
        }
        public void run(){
            try{ RemoteUvmContextFactory.factory().logout(); }
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
                    logger.debug("LAF name: " + UIManager.getLookAndFeel().getName());
                    logger.debug("LAF ClassLoader: " + UIManager.getLookAndFeelDefaults().get("ClassLoader"));
                    EventQueue eq = Toolkit.getDefaultToolkit().getSystemEventQueue();
                    eq.invokeAndWait( new Runnable(){ public void run(){
                        logger.debug("EDT name: " + Thread.currentThread().getName());
                        logger.debug("EDT ClassLoader: " + Thread.currentThread().getContextClassLoader());
                    }});
                    logger.debug("-----------------" );
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
                        logger.info("Error resetting LAF",e);
                    }
            }
        }
    }
}



