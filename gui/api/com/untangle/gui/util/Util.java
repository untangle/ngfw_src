/*
 * $HeadURL:$
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.lang.reflect.Constructor;
import java.net.*;
import java.text.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.jnlp.*;
import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;

import com.untangle.gui.login.*;
import com.untangle.gui.main.MMainJFrame;
import com.untangle.gui.main.PolicyStateMachine;
import com.untangle.gui.node.CompoundSettings;
import com.untangle.gui.node.SettingsChangedListener;
import com.untangle.gui.pipeline.MPipelineJPanel;
import com.untangle.gui.pipeline.MRackJPanel;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.*;
import com.untangle.uvm.addrbook.*;
import com.untangle.uvm.client.*;
import com.untangle.uvm.license.LicenseStatus;
import com.untangle.uvm.license.RemoteLicenseManager;
import com.untangle.uvm.logging.*;
import com.untangle.uvm.networking.ping.PingManager;
import com.untangle.uvm.node.*;
import com.untangle.uvm.policy.*;
import com.untangle.uvm.security.*;
import com.untangle.uvm.toolbox.ToolboxManager;
import com.untangle.uvm.user.RemotePhoneBook;
import org.apache.log4j.Logger;


public class Util {
    private static final String PROPERTY_IS_DEVEL = "com.untangle.isDevel";
    public static final String EXCEPTION_PORT_RANGE = "The port must be an integer number between 1 and 65535.";

    private static final Logger logger = Logger.getLogger(Util.class);

    private Util(){}

    public static void initialize(){
        shutdownableMap = new HashMap<String,Shutdownable>();
        statsCache = new StatsCache();
        logDateFormat = new SimpleDateFormat("EEE, MMM d HH:mm:ss");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        iconOnState = new ImageIcon( classLoader.getResource("com/untangle/gui/node/IconOnState28x28.png") );
        iconOffState = new ImageIcon( classLoader.getResource("com/untangle/gui/node/IconOffState28x28.png") );
        iconStoppedState = new ImageIcon( classLoader.getResource("com/untangle/gui/node/IconStoppedState28x28.png") );
        iconPausedState = new ImageIcon( classLoader.getResource("com/untangle/gui/node/IconAttentionState28x28.png") );
        buttonReloading = new ImageIcon( classLoader.getResource("com/untangle/gui/images/Button_Reloading_106x17.png") );
        buttonReloadSettings = new ImageIcon( classLoader.getResource("com/untangle/gui/images/Button_Reload_Settings_106x17.png") );
        buttonSaving = new ImageIcon( classLoader.getResource("com/untangle/gui/images/Button_Saving_106x17.png") );
        buttonSaveSettings = new ImageIcon( classLoader.getResource("com/untangle/gui/images/Button_Save_Settings_106x17.png") );
        buttonRefreshLog = new ImageIcon( classLoader.getResource("com/untangle/gui/images/Button_Refresh_Log_106x17.png") );
        buttonRefreshing = new ImageIcon( classLoader.getResource("com/untangle/gui/images/Button_Refreshing_106x17.png") );
        buttonStartAutoRefresh = new ImageIcon( classLoader.getResource("com/untangle/gui/images/Button_Start_Auto_Refresh_106x17.png") );
        buttonStopAutoRefresh = new ImageIcon( classLoader.getResource("com/untangle/gui/images/Button_Stop_Auto_Refresh_106x17.png") );
        buttonExpandSettings = new ImageIcon( classLoader.getResource("com/untangle/gui/images/Button_Expand_Settings_106x17.png") );
        buttonCollapseSettings = new ImageIcon( classLoader.getResource("com/untangle/gui/images/Button_Collapse_Settings_106x17.png") );

        buttonCancelPowerOn = new ImageIcon( classLoader.getResource("com/untangle/gui/images/Button_Cancel_Power_On_130x17.png") );
        buttonContinuePowerOn = new ImageIcon( classLoader.getResource("com/untangle/gui/images/Button_Continue_Power_On_130x17.png") );
        buttonCancelPowerOff = new ImageIcon( classLoader.getResource("com/untangle/gui/images/Button_Cancel_Power_Off_130x17.png") );
        buttonContinuePowerOff = new ImageIcon( classLoader.getResource("com/untangle/gui/images/Button_Continue_Power_Off_130x17.png") );
        buttonCancelRemove = new ImageIcon( classLoader.getResource("com/untangle/gui/images/Button_Cancel_Remove_106x17.png") );
        buttonContinueRemoving = new ImageIcon( classLoader.getResource("com/untangle/gui/images/Button_Continue_Removing_130x17.png") );
        buttonCancelSave = new ImageIcon( classLoader.getResource("com/untangle/gui/images/Button_Cancel_Save_106x17.png") );
        buttonContinueSaving = new ImageIcon( classLoader.getResource("com/untangle/gui/images/Button_Continue_Saving_106x17.png") );
        buttonCancel = new ImageIcon( classLoader.getResource("com/untangle/gui/images/Button_Cancel_106x17.png") );
        buttonCancelling = new ImageIcon( classLoader.getResource("com/untangle/gui/images/Button_Cancelling_106x17.png") );
        buttonProcure = new ImageIcon( classLoader.getResource("com/untangle/gui/images/Button_Procure_106x17.png") );
        buttonProcuring = new ImageIcon( classLoader.getResource("com/untangle/gui/images/Button_Procuring_106x17.png") );
        buttonBackupToHardDisk = new ImageIcon( classLoader.getResource("com/untangle/gui/images/Button_Backup_To_Hard_Disk_130x17.png") );
        buttonBackupToUsbKey = new ImageIcon( classLoader.getResource("com/untangle/gui/images/Button_Backup_To_Usb_Key_130x17.png") );

        INVALID_BACKGROUND_COLOR = Color.PINK;
        VALID_BACKGROUND_COLOR = new Color(224, 224, 224);
    }

    // LOGOUT /////////////////////
    private static volatile boolean shutdownInitiated = false;
    public static boolean getShutdownInitiated(){ return shutdownInitiated; }
    public static void setShutdownInitiated(boolean x){ shutdownInitiated = x; }

    // LOOK AND FEEL //////////////
    private static LookAndFeel lookAndFeel;
    public static LookAndFeel getLookAndFeel(){ return lookAndFeel; }
    public static void setLookAndFeel(LookAndFeel x){ lookAndFeel = x; }

    // LOGIN //////////////////////
    public static final int LOGIN_RETRY_COUNT = 6;
    public static final long LOGIN_RETRY_SLEEP = 3000l;
    ///////////////////////////////

    // NETWORKING ////////////////
    public static final int RECONFIGURE_NETWORK_TIMEOUT_MILLIS = 60*1000;
    public static final int DISCONNECT_NETWORK_TIMEOUT_MILLIS = 15*1000;
    //////////////////////////////

    // SERVER PROXIES ///////////////
    private static UvmRemoteContext uvmContext;
    private static ToolboxManager toolboxManager;
    private static NodeManager nodeManager;
    private static AdminManager adminManager;
    private static StatsCache statsCache;
    private static NetworkManager networkManager;
    // DO NOT CACHE THIS private static PolicyManager policyManager;
    private static LoggingManager loggingManager;
    private static RemoteAppServerManager appServerManager;
    // DO NOT CACHE THIS private static AddressBook addressBook;
    // DO NOT CACHE THIS private static RemotePhoneBook phoneBook;
    private static RemoteIntfManager remoteIntfManager;
    private static PingManager pingManager;
    private static BrandingManager brandingManager;

    public static void setUvmContext(UvmRemoteContext uvmContextX){
        uvmContext = uvmContextX;
        if( uvmContext != null ){
            toolboxManager = uvmContext.toolboxManager();
            nodeManager = uvmContext.nodeManager();
            adminManager = uvmContext.adminManager();
            networkManager = uvmContext.networkManager();
            // DO NOT CACHE THIS policyManager = uvmContext.policyManager();
            loggingManager = uvmContext.loggingManager();
            appServerManager = uvmContext.appServerManager();
            // DO NOT CACHE THIS addressBook = uvmContext.appAddressBook();
            // DO NOT CACHE THIS phoneBook = uvmContext.phoneBook();
            remoteIntfManager = uvmContext.intfManager();
            pingManager = uvmContext.pingManager();
            brandingManager = uvmContext.brandingManager();
        }
        else{
            toolboxManager = null;
            nodeManager = null;
            adminManager = null;
            networkManager = null;
            // DO NOT CACHE THIS policyManager = null;
            loggingManager = null;
            appServerManager = null;
            // DO NOT CACHE THIS addressBook = null;
            // DO NOT CACHE THIS phoneBook = null;
            remoteIntfManager = null;
            pingManager = null;
            brandingManager = null;
        }
    }
    
    public static UvmRemoteContext getUvmContext(){ return uvmContext; }
    public static ToolboxManager getToolboxManager(){ return toolboxManager; }
    public static NodeManager getNodeManager(){ return nodeManager; }
    public static AdminManager getAdminManager(){ return adminManager; }
    public static StatsCache getStatsCache(){ return statsCache; }
    public static RemoteIntfManager getIntfManager(){ return remoteIntfManager; }
    public static NetworkManager getNetworkManager(){ return networkManager; }
    public static PolicyManager getPolicyManager(){ return uvmContext.policyManager(); }
    public static LoggingManager getLoggingManager(){ return loggingManager; }
    public static RemoteAppServerManager getAppServerManager(){ return appServerManager; }
    public static AddressBook getAddressBook(){ return uvmContext.appAddressBook(); }
    public static RemotePhoneBook getPhoneBook(){ return uvmContext.phoneBook(); }
    public static PingManager getPingManager(){ return pingManager; }
    public static BrandingManager getBrandingManager(){ return brandingManager; }
    public static RemoteLicenseManager getLicenseManager(){ return uvmContext.licenseManager(); }
    ///////////////////////////////////


    // BUTTON DECALS /////////////////

    public static ImageIcon[] getImageIcons(String[] imagePaths){
        ImageIcon[] imageIcons = new ImageIcon[imagePaths.length];
        for( int i=0; i<imagePaths.length; i++){
            imageIcons[i] = new javax.swing.ImageIcon( Util.getClassLoader().getResource(imagePaths[i]) );
        }
        return imageIcons;
    }

    private static ImageIcon iconOnState;
    private static ImageIcon iconOffState;
    private static ImageIcon iconStoppedState;
    private static ImageIcon iconPausedState;
    private static ImageIcon buttonReloading;
    private static ImageIcon buttonReloadSettings;
    private static ImageIcon buttonSaving;
    private static ImageIcon buttonSaveSettings;
    private static ImageIcon buttonRefreshLog;
    private static ImageIcon buttonRefreshing;
    private static ImageIcon buttonStartAutoRefresh;
    private static ImageIcon buttonStopAutoRefresh;
    private static ImageIcon buttonExpandSettings;
    private static ImageIcon buttonCollapseSettings;
    private static ImageIcon buttonCancelPowerOn;
    private static ImageIcon buttonContinuePowerOn;
    private static ImageIcon buttonCancelPowerOff;
    private static ImageIcon buttonContinuePowerOff;
    private static ImageIcon buttonCancelRemove;
    private static ImageIcon buttonContinueRemoving;
    private static ImageIcon buttonCancelSave;
    private static ImageIcon buttonContinueSaving;
    private static ImageIcon buttonCancel;
    private static ImageIcon buttonCancelling;
    private static ImageIcon buttonProcure;
    private static ImageIcon buttonProcuring;
    private static ImageIcon buttonBackupToHardDisk;
    private static ImageIcon buttonBackupToUsbKey;

    public static ImageIcon getIconOnState(){ return iconOnState; }
    public static ImageIcon getIconOffState(){ return iconOffState; }
    public static ImageIcon getIconStoppedState(){ return iconStoppedState; }
    public static ImageIcon getIconPausedState(){ return iconPausedState; }
    public static ImageIcon getButtonReloading(){ return buttonReloading; }
    public static ImageIcon getButtonReloadSettings(){ return buttonReloadSettings; }
    public static ImageIcon getButtonSaving(){ return buttonSaving; }
    public static ImageIcon getButtonSaveSettings(){ return buttonSaveSettings; }
    public static ImageIcon getButtonRefreshLog(){ return buttonRefreshLog; }
    public static ImageIcon getButtonRefreshing(){ return buttonRefreshing; }
    public static ImageIcon getButtonStartAutoRefresh(){ return buttonStartAutoRefresh; }
    public static ImageIcon getButtonStopAutoRefresh(){ return buttonStopAutoRefresh; }
    public static ImageIcon getButtonExpandSettings(){ return buttonExpandSettings; }
    public static ImageIcon getButtonCollapseSettings(){ return buttonCollapseSettings; }
    public static ImageIcon getButtonCancelPowerOn(){ return buttonCancelPowerOn; }
    public static ImageIcon getButtonContinuePowerOn(){ return buttonContinuePowerOn; }
    public static ImageIcon getButtonCancelPowerOff(){ return buttonCancelPowerOff; }
    public static ImageIcon getButtonContinuePowerOff(){ return buttonContinuePowerOff; }
    public static ImageIcon getButtonCancelRemove(){ return buttonCancelRemove; }
    public static ImageIcon getButtonContinueRemoving(){ return buttonContinueRemoving; }
    public static ImageIcon getButtonCancelSave(){ return buttonCancelSave; }
    public static ImageIcon getButtonContinueSaving(){ return buttonContinueSaving; }
    public static ImageIcon getButtonCancel(){ return buttonCancel; }
    public static ImageIcon getButtonCancelling(){ return buttonCancelling; }
    public static ImageIcon getButtonProcure(){ return buttonProcure; }
    public static ImageIcon getButtonProcuring(){ return buttonProcuring; }
    public static ImageIcon getButtonBackupToHardDisk(){ return buttonBackupToHardDisk; }
    public static ImageIcon getButtonBackupToUsbKey(){ return buttonBackupToUsbKey; }
    //////////////////////////////////


    // VALIDATION //////////////////
    public static Color INVALID_BACKGROUND_COLOR;
    public static Color VALID_BACKGROUND_COLOR;
    ///////////////////////////////


    // LOCAL //////////////////////
    private static boolean isLocal = false;
    public static void setLocal(boolean isLocalX){ isLocal = isLocalX; }
    public static boolean isLocal(){ return isLocal; }
    //////////////////////////////


    // CD //////////////////////
    private static boolean IS_CD = false;
    public static boolean getIsCD(){ return IS_CD; }
    public static void setIsCD(boolean isCD){ IS_CD = isCD; }
    //////////////////////////////


    // PREMIUM //////////////////////
    public static boolean getIsPremium(String identifier){
        return !getLicenseManager().getLicenseStatus(identifier).isExpired();
    }
    //////////////////////////////


    // CODEBASE /////////////////
    private static URL serverCodeBase;

    public static URL getServerCodeBase(){
        if(serverCodeBase != null)
            return serverCodeBase;
        else{
            try{
                BasicService basicService = (BasicService) ServiceManager.lookup("javax.jnlp.BasicService");
                serverCodeBase = basicService.getCodeBase();
            }
            catch(Exception e){
                Util.handleExceptionNoRestart("Error (setting code base to http://127.0.0.1/webstart):", e);
                serverCodeBase = new URL("http://127.0.0.1/webstart");
            }
            finally{
                return serverCodeBase;
            }
        }
    }

    public static boolean isSecureViaHttps(){
        try{
            String protocol = getServerCodeBase().getProtocol();
            if( protocol.equals("https") )
                return true;
            else
                return false;
        }
        catch(Exception e){
            return false;
        }
    }
    /////////////////////////////////


    // UPGRADE /////////////////////
    public static final int UPGRADE_THREAD_SLEEP_MILLIS = 60 * (60 * 1000); // X * (minutes * Y)
    public static final long UPGRADE_STORE_CHECK_FRESH_MILLIS = 60l * (60l * 1000l); // X * (minutes * Y)
    public static final int UPGRADE_UNAVAILABLE = -1;
    public static final int UPGRADE_CHECKING = -2;
    private static long lastUpgradeCheck = 0l;
    private static int upgradeCount = UPGRADE_CHECKING;

    public static synchronized void setUpgradeCount(int upgradeCountX){
        upgradeCount = upgradeCountX;
        lastUpgradeCheck = System.currentTimeMillis();
    }
    public static synchronized int getUpgradeCount(){ return upgradeCount; }
    public static synchronized boolean mustCheckUpgrades(){
        if( (System.currentTimeMillis() - lastUpgradeCheck > UPGRADE_STORE_CHECK_FRESH_MILLIS)
            || (upgradeCount != 0) )
            return true;
        else
            return false;
    }
    ///////////////////////////////


    // DefaultTableColumnModel constants /////////
    public static final int TABLE_TOTAL_WIDTH = 470; /* in pixels (contains extra pixel) */
    public static final int TABLE_TOTAL_WIDTH_LARGE = 501; /* in pixels (contains extra pixel) */
    public static final int LINENO_MIN_WIDTH = 38; /* # */
    public static final int LINENO_EDIT_MIN_WIDTH = 55; /* # */
    public static final int STATUS_MIN_WIDTH = 55; /* status */
    public static final int TIMESTAMP_MIN_WIDTH = 150; /* time stamp */
    //////////////////////////////////////////////


    // GUI COMPONENTS AND FUNCTIONALITY //////////
    private static ClassLoader initClassLoader;
    private static MURLClassLoader mURLClassLoader;
    private static JProgressBar statusJProgressBar;
    private static boolean isDemo;
    private static MPipelineJPanel mPipelineJPanel;
    private static MRackJPanel mRackJPanel;
    private static MLoginJFrame mLoginJFrame;
    private static MMainJFrame mMainJFrame;
    private static PolicyStateMachine policyStateMachine;

    public static ClassLoader getInitClassLoader(){ return initClassLoader; }
    public static void setInitClassLoader(ClassLoader initClassLoaderX){ initClassLoader = initClassLoaderX;}
    public static MURLClassLoader getClassLoader(){ return mURLClassLoader; }
    public static void setClassLoader(MURLClassLoader mURLClassLoaderX){ mURLClassLoader = mURLClassLoaderX;}
    public static JProgressBar getStatusJProgressBar(){ return statusJProgressBar; }
    public static void setStatusJProgressBar(JProgressBar statusJProgressBarX){ statusJProgressBar = statusJProgressBarX; }
    public static boolean getIsDemo(){ return isDemo; }
    public static void setIsDemo(boolean isDemoX){ isDemo = isDemoX; }
    public static MPipelineJPanel getMPipelineJPanel(){ return mPipelineJPanel; }
    public static void setMPipelineJPanel(MPipelineJPanel mPipelineJPanelX){ mPipelineJPanel = mPipelineJPanelX; }
    public static MRackJPanel getMRackJPanel(){ return mRackJPanel; }
    public static void setMRackJPanel(MRackJPanel mRackJPanelX){ mRackJPanel = mRackJPanelX; }
    public static void setMLoginJFrame(MLoginJFrame mLoginJFrameX){ mLoginJFrame = mLoginJFrameX; }
    public static JFrame getMLoginJFrame(){ return mLoginJFrame; }
    public static void setMMainJFrame(MMainJFrame mMainJFrameX){ mMainJFrame = mMainJFrameX; }
    public static MMainJFrame getMMainJFrame(){ return mMainJFrame; }
    public static void setPolicyStateMachine(PolicyStateMachine xPolicyStateMachine){ policyStateMachine = xPolicyStateMachine; }
    public static PolicyStateMachine getPolicyStateMachine(){ return policyStateMachine; }
    ////////////////////////////////////////////

    // EXITING AND SHUTDOWN ///////////////////
    private static Map<String,Shutdownable> shutdownableMap;
    public static void exit(int i){
        System.exit(i);
    }
    public static void addShutdownable(String name, Shutdownable shutdownable){
        shutdownableMap.put(name, shutdownable);
    }
    private static void doShutdown(){
        Util.printMessage("Shutdown initiated by: " + Thread.currentThread().getName() );
        for( Map.Entry<String,Shutdownable> shutdownableEntry : shutdownableMap.entrySet() ){
            logger.info("Shutting down: " + shutdownableEntry.getKey());
            shutdownableEntry.getValue().doShutdown();
        }
        shutdownableMap.clear();
    }
    ////////////////////////////////////////////
    public static boolean isDevel() {
        return Boolean.getBoolean(PROPERTY_IS_DEVEL);
    }


    // WINDOW PLACEMENT AND FORMATTING /////////
    public static GraphicsConfiguration getGraphicsConfiguration(){
        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice graphicsDevice = graphicsEnvironment.getDefaultScreenDevice();
        GraphicsConfiguration graphicsConfiguration = graphicsDevice.getDefaultConfiguration();
        return graphicsConfiguration;
    }

    public static Rectangle generateCenteredBounds(Window window, int childWidth, int childHeight){
        if( window != null )
            return generateCenteredBounds(window.getBounds(), childWidth, childHeight);
        else
            return generateCenteredBounds((Rectangle)null, childWidth, childHeight);

    }

    public static Rectangle generateCenteredBounds(Rectangle parentBounds, int childWidth, int childHeight){
        Rectangle childBounds;
        Rectangle defaultScreenBounds;

        GraphicsConfiguration graphicsConfiguration = getGraphicsConfiguration();
        defaultScreenBounds = graphicsConfiguration.getBounds();

        if(parentBounds == null){
            parentBounds = defaultScreenBounds;
        }

        int xCenter = parentBounds.x + parentBounds.width/2;
        int yCenter = parentBounds.y + parentBounds.height/2;
        childBounds = new Rectangle( (xCenter-(childWidth/2)),
                                     (yCenter-(childHeight/2)),
                                     childWidth,
                                     childHeight );

        if(childBounds.x < 0)
            childBounds.x = 0;
        if(childBounds.y < 0)
            childBounds.y = 0;

        return childBounds;
    }

    public static int determineMinHeight(int attemptedMinHeight){
        GraphicsConfiguration graphicsConfiguration = getGraphicsConfiguration();
        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets( graphicsConfiguration );
        int screenHeight = graphicsConfiguration.getBounds().height - screenInsets.top - screenInsets.bottom;
        //logger.debug("Screen height: " + graphicsConfiguration.getBounds().height);
        //logger.debug("Screen width: " + graphicsConfiguration.getBounds().width);
        //logger.debug("Top insets: " + screenInsets.top);
        //logger.debug("Bottom insets: " + screenInsets.bottom);
        //logger.debug("Right insets: " + screenInsets.right);
        //logger.debug("Left insets: " + screenInsets.left);
        //  logger.debug("Determined screen height to be: " + screenHeight);
        if( screenHeight < attemptedMinHeight)
            return screenHeight;
        else
            return attemptedMinHeight;
    }

    public static void resizeCheck(final Component resizableComponent, Dimension minSize, Dimension maxSize){

        final int currentWidth = resizableComponent.getWidth();
        final int currentHeight = resizableComponent.getHeight();
        int newWidth = currentWidth;
        int newHeight = currentHeight;
        /*
          logger.debug("----------------------");
          logger.debug("| Initial size: " + currentSize);
          logger.debug("| Min size: " + minSize);
          logger.debug("| Max size: " + maxSize);
          logger.debug("----------------------");
        */
        boolean resetSize = false;
        if(currentWidth < minSize.width){
            newWidth = minSize.width;
            resetSize = true;
        }
        else if(currentWidth > maxSize.width){
            newWidth = maxSize.width;
            resetSize = true;
        }
        if(currentHeight < minSize.height){
            newHeight = minSize.height;
            resetSize = true;
        }
        else if(currentHeight > maxSize.height){
            newHeight = maxSize.height;
            resetSize = true;
        }
        if(resetSize){
            resizableComponent.setSize( newWidth, newHeight );
        }
    }
    //////////////////////////////////////////////////////


    public static void addSettingChangeListener(final SettingsChangedListener s,
                                                final Object source,
                                                final JComponent c){
        if(s==null)
            return;
        if(c instanceof JComboBox){
            JComboBox cb = (JComboBox) c;
            for( ActionListener a : cb.getActionListeners() ){
                if(a instanceof SettingChangeListener )
                    cb.removeActionListener(a);
            }
            cb.addActionListener(new SettingChangeListener(s, source, cb.getSelectedItem()));
        }
        else if(c instanceof JTextComponent){
            JTextComponent tc = (JTextComponent) c;
            for( CaretListener cl : tc.getCaretListeners() ){
                if(cl instanceof SettingChangeListener )
                    tc.removeCaretListener(cl);
            }
            tc.addCaretListener(new SettingChangeListener(s, source, tc.getText()));
        }
        else if(c instanceof AbstractButton){
            AbstractButton ab = (AbstractButton) c;
            for( ActionListener a : ab.getActionListeners() ){
                if(a instanceof SettingChangeListener )
                    ab.removeActionListener(a);
            }
            ab.addActionListener(new SettingChangeListener(s, source, ab.isSelected()));
        }
        else if(c instanceof JSpinner){
            JSpinner js = (JSpinner) c;
            for( ChangeListener cl : js.getChangeListeners() ){
                if(cl instanceof SettingChangeListener )
                    js.removeChangeListener(cl);
            }
            js.addChangeListener(new SettingChangeListener(s, source, js.getValue()));
        }
    }

    private static class SettingChangeListener implements ActionListener, CaretListener, ChangeListener {
        private Object source;
        private SettingsChangedListener s;
        private Object setting;
        public SettingChangeListener(SettingsChangedListener s, Object source, Object setting){
            this.s = s;
            this.source = source;
            this.setting = setting;
        }
        public void stateChanged(ChangeEvent e){
            Object source = e.getSource();
            if(source instanceof JSpinner){
                JSpinner js = (JSpinner) source;
                if( !js.getValue().equals(setting) )
                    s.settingsChanged(source);
            }
        }
        public void caretUpdate(CaretEvent e){
            Object source = e.getSource();
            if(source instanceof JTextComponent){
                JTextComponent tc = (JTextComponent) source;
                String lastSetting = (String) setting;
                if( !tc.getText().trim().equals(lastSetting.trim())  )
                    s.settingsChanged(source);
            }
        }
        public void actionPerformed(ActionEvent e){
            Object source = e.getSource();
            if(source instanceof JComboBox){
                if( !((JComboBox)source).getSelectedItem().equals(setting) )
                    s.settingsChanged(source);
            }
            else if(source instanceof AbstractButton){
                if( ! (((AbstractButton)source).isSelected() == (Boolean)setting) )
                    s.settingsChanged(source);
            }
        }
    }

    // FOCUS //
    public static void addPanelFocus(final JComponent source, final JComponent target){
        source.addFocusListener( new FocusListener(){
                public void focusGained(FocusEvent e){
                    target.requestFocus();
                }
                public void focusLost(FocusEvent e){}
            });
    }

    public static void addFocusHighlight(final JComponent c){
        if(c instanceof JTextComponent)
            c.addFocusListener(new FocusHighlightListener());
        else if(c instanceof JSpinner){
            JComponent editor = ((JSpinner)c).getEditor();
            ((JSpinner.DefaultEditor)editor).getTextField().addFocusListener(new FocusHighlightListener());
        }
        if(c instanceof JTextArea){
            ((JTextArea)c).setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
            ((JTextArea)c).setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
        }
        else if(c instanceof JEditorPane){
            ((JEditorPane)c).setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
            ((JEditorPane)c).setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
        }
    }

    private static class FocusHighlightListener extends FocusAdapter {
        public void focusGained(FocusEvent e){
            if(e.getSource() instanceof JTextComponent){
                final JTextComponent c = (JTextComponent) e.getSource();
                SwingUtilities.invokeLater( new Runnable(){ public void run(){
                    c.selectAll();
                }});
            }
        }
        public void focusLost(FocusEvent e){
            if(e.getSource() instanceof JTextComponent){
                final JTextComponent c = (JTextComponent) e.getSource();
                SwingUtilities.invokeLater( new Runnable(){ public void run(){
                    c.select(0,0);
                }});
            }
        }
    }

    public static String getSelectedTabTitle(JTabbedPane jTabbedPane){

        String focus;
        int focusIndex = jTabbedPane.getSelectedIndex();
        if(focusIndex < 0){
            return "null";
        }
        else{
            Component focusComponent = jTabbedPane.getComponentAt(focusIndex);
            if(focusComponent instanceof JTabbedPane)
                return jTabbedPane.getTitleAt(focusIndex) + "+" + getSelectedTabTitle((JTabbedPane)focusComponent);
            else if( (focusComponent instanceof Container)
                     && (((Container)focusComponent).getComponentCount()>0)
                     && (((Container)focusComponent).getComponent(0) instanceof JTabbedPane) ){
                return jTabbedPane.getTitleAt(focusIndex) + "+"
                    + getSelectedTabTitle((JTabbedPane)((Container)focusComponent).getComponent(0));
            }
            else
                return jTabbedPane.getTitleAt(focusIndex);
        }

    }
    //////////////////////////////////////////////////////

    // EXCEPTION HANDLING AND MESSAGE PRINTING ////////////
    public synchronized static void handleExceptionNoRestart(String output, Exception e){
        logger.debug(output,e);
    }

    public synchronized static void handleExceptionWithRestart(String output, Exception e) throws Exception {
        Throwable throwableRef = e;

        while( throwableRef != null){
            if( throwableRef instanceof InvocationConnectionException ){
                logger.info(output,e);
                if( !Util.getShutdownInitiated() ){
                    Util.setShutdownInitiated(true);
                    doShutdown();
                    mLoginJFrame.resetLogin("Server communication failure.  Re-login.");
                    mLoginJFrame.reshowLogin();
                    UvmRemoteContextFactory.factory().logout();
                }
                return;
            }
            else if( throwableRef instanceof InvocationTargetExpiredException ){
                logger.info(output,e);
                if( !Util.getShutdownInitiated() ){
                    Util.setShutdownInitiated(true);
                    doShutdown();
                    mLoginJFrame.resetLogin("Server synchronization failure.  Re-login.");
                    mLoginJFrame.reshowLogin();
                    UvmRemoteContextFactory.factory().logout();
                }
                return;
            }
            else if( throwableRef instanceof com.untangle.uvm.client.LoginExpiredException ){
                logger.info(output,e);
                if( !Util.getShutdownInitiated() ){
                    Util.setShutdownInitiated(true);
                    doShutdown();
                    mLoginJFrame.resetLogin("Login expired.  Re-login.");
                    mLoginJFrame.reshowLogin();
                    UvmRemoteContextFactory.factory().logout();
                }
                return;
            }
            else if(    (throwableRef instanceof ConnectException)
                        || (throwableRef instanceof SocketException)
                        || (throwableRef instanceof SocketTimeoutException) ){
                logger.info(output,e);
                if( !Util.getShutdownInitiated() ){
                    Util.setShutdownInitiated(true);
                    doShutdown();
                    mLoginJFrame.resetLogin("Server connection failure.  Re-login.");
                    mLoginJFrame.reshowLogin();
                    UvmRemoteContextFactory.factory().logout();
                }
                return;
            }
            else if( throwableRef instanceof LoginStolenException ){
                if( !Util.getShutdownInitiated() ){
                    String loginName = ((LoginStolenException)throwableRef).getThief().getUvmPrincipal().getName();
                    String loginAddress = ((LoginStolenException)throwableRef).getThief().getClientAddr().getHostAddress();
                    new LoginStolenJDialog(loginName, loginAddress);
                    Util.setShutdownInitiated(true);
                    doShutdown();
                    mLoginJFrame.resetLogin("Login ended by: " + loginName + " at " + loginAddress);
                    mLoginJFrame.reshowLogin();
                    UvmRemoteContextFactory.factory().logout();
                }
                return;
            }
            throwableRef = throwableRef.getCause();
        }
        throw e;
    }


    public static void printMessage(String message){
        logger.info(message);
    }
    /////////////////////////////////////////////////



    // GENERAL UTIL ////////////////////////////////
    public static void setPortView(JSpinner jSpinner, int defaultValue){
        jSpinner.setModel(new SpinnerNumberModel(defaultValue,0,65535,1));
        ((JSpinner.NumberEditor)jSpinner.getEditor()).getFormat().setGroupingUsed(false);
    }
    public static void setAAClientProperty(Component parentComponent, boolean isAAEnabled){
        if( parentComponent instanceof JComponent ){
            try{ ((JComponent)parentComponent).putClientProperty(com.sun.java.swing.SwingUtilities2.AA_TEXT_PROPERTY_KEY, new Boolean(isAAEnabled)); }
            catch(Throwable t){}
        }

        if( parentComponent instanceof Container ){
            for( Component component : ((Container)parentComponent).getComponents() ){
                setAAClientProperty(component, isAAEnabled);
            }
        }
    }
    public static int chooseMax(int iValue, int iMinValue){
        if(iValue >= iMinValue){ return iValue; }
        else { return iMinValue; }
    }

    public static boolean isArrayEmpty(Object[] inArray){
        if( inArray == null )
            return true;
        else if( inArray.length <= 0 )
            return true;
        else
            return false;
    }

    public static boolean isListEmpty(List inList){
        return null == inList || 0 == inList.size();
    }

    public static boolean isEqual(Object a, Object b){
        if( (a==null) && (b==null) )
            return true;
        else if( (a!=null) && (b==null) )
            return false;
        else if( (a==null) && (b!=null) )
            return false;
        else
            return a.equals(b) && b.equals(a);
    }

    ///////////////////////////////////////////



    // STRING FORMATTING //////////////////////
    private static DateFormat logDateFormat;

    public static DateFormat getLogDateFormat(){ return logDateFormat; }

    public static String padZero(long number){
        if( number >= 100 )  // uses all 3 digits
            return Long.toString(number);
        else if( number >= 10 ) // uses only 2 digits
            return "0" + Long.toString(number);
        else // uses only 1 digit
            return "00" + Long.toString(number);
    }

    public static String wrapString(String originalString, int lineLength){
        StringTokenizer stringTokenizer = new StringTokenizer(originalString);
        StringBuffer stringBuffer = new StringBuffer();
        String tempString;
        int currentLineLength = 0;
        while( stringTokenizer.hasMoreTokens() ){
            tempString = stringTokenizer.nextToken();

            if( currentLineLength + tempString.length() >= lineLength ){
                stringBuffer.append("<br>" + tempString + " ");
                currentLineLength = tempString.length() + 1;
            }
            else{
                stringBuffer.append(tempString + " ");
                currentLineLength += (tempString.length() + 1);
            }
        }
        return stringBuffer.toString();
    }
    ///////////////////////////////////////////

    // NODE LOADING //////////////////////
    public static Node getNode(String nodeName) throws Exception {
        Node node = null;
        List<Tid> nodeInstances = Util.getNodeManager().nodeInstances(nodeName);
        if(nodeInstances.size()>0){
            NodeContext nodeContext = Util.getNodeManager().nodeContext(nodeInstances.get(0));
            node = nodeContext.node();
        }
        return node;
    }

    public static Object getRemoteObject(String className, String nodeName) throws Exception {
        Node node = getNode(nodeName);
        if( node != null){
            NodeDesc nodeDesc = node.getNodeDesc();
            Class nodeClass = Util.getClassLoader().mLoadClass(className);
            Constructor nodeConstructor = nodeClass.getConstructor(new Class[]{});
            return nodeConstructor.newInstance();
        }
        else
            return null;
    }

    public static CompoundSettings getCompoundSettings(String className, String nodeName) throws Exception {
        return (CompoundSettings) getRemoteObject(className, nodeName);
    }

    public static Component getSettingsComponent(String className, String nodeName) throws Exception {
        return (Component) getRemoteObject(className, nodeName);
    }
    //////////////////////////////////////////
}
