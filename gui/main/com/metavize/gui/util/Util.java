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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.net.*;
import java.text.*;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.jnlp.*;
import javax.swing.*;
import java.lang.reflect.Constructor;

import com.metavize.gui.login.*;
import com.metavize.gui.main.MMainJFrame;
import com.metavize.gui.main.PolicyStateMachine;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.pipeline.MRackJPanel;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.transform.CompoundSettings;
import com.metavize.mvvm.*;
import com.metavize.mvvm.client.*;
import com.metavize.mvvm.logging.*;
import com.metavize.mvvm.policy.*;
import com.metavize.mvvm.security.*;
import com.metavize.mvvm.toolbox.ToolboxManager;
import com.metavize.mvvm.tran.*;

public class Util {

    private Util(){}

    private static boolean isInitialized;
    public static void initialize(){
	if(isInitialized)
	    return;
	else
	    isInitialized = true;
        daemonThreadVector = new Vector<Killable>();
        logDateFormat = new SimpleDateFormat("EEE, MMM d HH:mm:ss");
        log = new Vector();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        iconOnState = new ImageIcon( classLoader.getResource("com/metavize/gui/transform/IconOnState28x28.png") );
        iconOffState = new ImageIcon( classLoader.getResource("com/metavize/gui/transform/IconOffState28x28.png") );
        iconStoppedState = new ImageIcon( classLoader.getResource("com/metavize/gui/transform/IconStoppedState28x28.png") );
        iconPausedState = new ImageIcon( classLoader.getResource("com/metavize/gui/transform/IconAttentionState28x28.png") );
        buttonReloading = new ImageIcon( classLoader.getResource("com/metavize/gui/images/Button_Reloading_106x17.png") );
        buttonReloadSettings = new ImageIcon( classLoader.getResource("com/metavize/gui/images/Button_Reload_Settings_106x17.png") );
        buttonSaving = new ImageIcon( classLoader.getResource("com/metavize/gui/images/Button_Saving_106x17.png") );
        buttonSaveSettings = new ImageIcon( classLoader.getResource("com/metavize/gui/images/Button_Save_Settings_106x17.png") );
        buttonRefreshLog = new ImageIcon( classLoader.getResource("com/metavize/gui/images/Button_Refresh_Log_106x17.png") );
        buttonRefreshing = new ImageIcon( classLoader.getResource("com/metavize/gui/images/Button_Refreshing_106x17.png") );
        buttonStartAutoRefresh = new ImageIcon( classLoader.getResource("com/metavize/gui/images/Button_Start_Auto_Refresh_106x17.png") );
        buttonStopAutoRefresh = new ImageIcon( classLoader.getResource("com/metavize/gui/images/Button_Stop_Auto_Refresh_106x17.png") );
        buttonExpandSettings = new ImageIcon( classLoader.getResource("com/metavize/gui/images/Button_Expand_Settings_106x17.png") );
        buttonCollapseSettings = new ImageIcon( classLoader.getResource("com/metavize/gui/images/Button_Collapse_Settings_106x17.png") );

        buttonCancelPowerOn = new ImageIcon( classLoader.getResource("com/metavize/gui/images/Button_Cancel_Power_On_130x17.png") );
        buttonContinuePowerOn = new ImageIcon( classLoader.getResource("com/metavize/gui/images/Button_Continue_Power_On_130x17.png") );
        buttonCancelPowerOff = new ImageIcon( classLoader.getResource("com/metavize/gui/images/Button_Cancel_Power_Off_130x17.png") );
        buttonContinuePowerOff = new ImageIcon( classLoader.getResource("com/metavize/gui/images/Button_Continue_Power_Off_130x17.png") );
        buttonCancelRemove = new ImageIcon( classLoader.getResource("com/metavize/gui/images/Button_Cancel_Remove_106x17.png") );
        buttonContinueRemoving = new ImageIcon( classLoader.getResource("com/metavize/gui/images/Button_Continue_Removing_130x17.png") );
        buttonCancelSave = new ImageIcon( classLoader.getResource("com/metavize/gui/images/Button_Cancel_Save_106x17.png") );
        buttonContinueSaving = new ImageIcon( classLoader.getResource("com/metavize/gui/images/Button_Continue_Saving_106x17.png") );
        buttonCancel = new ImageIcon( classLoader.getResource("com/metavize/gui/images/Button_Cancel_106x17.png") );
        buttonCancelling = new ImageIcon( classLoader.getResource("com/metavize/gui/images/Button_Cancelling_106x17.png") );
        buttonProcure = new ImageIcon( classLoader.getResource("com/metavize/gui/images/Button_Procure_106x17.png") );
        buttonProcuring = new ImageIcon( classLoader.getResource("com/metavize/gui/images/Button_Procuring_106x17.png") );
        buttonBackupToHardDisk = new ImageIcon( classLoader.getResource("com/metavize/gui/images/Button_Backup_To_Hard_Disk_130x17.png") );
        buttonBackupToUsbKey = new ImageIcon( classLoader.getResource("com/metavize/gui/images/Button_Backup_To_Usb_Key_130x17.png") );

	INVALID_BACKGROUND_COLOR = Color.PINK;
	VALID_BACKGROUND_COLOR = new Color(224, 224, 224);
    }


    // LOGIN //////////////////////
    public static final int LOGIN_RETRY_COUNT = 6;
    public static final long LOGIN_RETRY_SLEEP = 3000l;
    ///////////////////////////////


    // SERVER PROXIES ///////////////
    private static MvvmRemoteContext mvvmContext;
    private static ToolboxManager toolboxManager;
    private static TransformManager transformManager;
    private static AdminManager adminManager;
    private static StatsCache statsCache;
    private static NetworkingManager networkingManager;
    private static NetworkManager networkManager;
    private static PolicyManager policyManager;
    private static LoggingManager loggingManager;
    private static AppServerManager appServerManager;

    public static void setMvvmContext(MvvmRemoteContext mvvmContextX){
        mvvmContext = mvvmContextX;
        if( mvvmContext != null ){
            toolboxManager = mvvmContext.toolboxManager();
            transformManager = mvvmContext.transformManager();
            adminManager = mvvmContext.adminManager();
            networkingManager = mvvmContext.networkingManager();
            networkManager = mvvmContext.networkManager();
            policyManager = mvvmContext.policyManager();
            loggingManager = mvvmContext.loggingManager();
            appServerManager = mvvmContext.appServerManager();
        }
        else{
            toolboxManager = null;
            transformManager = null;
            adminManager = null;
            networkingManager = null;
            networkManager = null;
            policyManager = null;
            loggingManager = null;
            appServerManager = null;
        }
    }

    public static MvvmRemoteContext getMvvmContext(){ return mvvmContext; }
    public static ToolboxManager getToolboxManager(){ return toolboxManager; }
    public static TransformManager getTransformManager(){ return transformManager; }
    public static AdminManager getAdminManager(){ return adminManager; }
    public static StatsCache getStatsCache(){
        if( statsCache == null )
            statsCache = new StatsCache();
        return statsCache;
    }
    public static NetworkingManager getNetworkingManager(){ return networkingManager; }
    public static NetworkManager getNetworkManager(){ return networkManager; }
    public static PolicyManager getPolicyManager(){ return policyManager; }
    public static LoggingManager getLoggingManager(){ return loggingManager; }
    public static AppServerManager getAppServerManager(){ return appServerManager; }
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
    private static boolean killThreads = false;
    private static Vector<Killable> daemonThreadVector;

    public static boolean getKillThreads(){
        return killThreads;
    }
    public static void exit(int i){
        killThreads = true;
        System.exit(i);
    }
    public static void addKillableThread(Killable killable){
        daemonThreadVector.add(killable);
    }
    public static void killDaemonThreads(){
        for( Killable killable : daemonThreadVector ){
            killable.kill();
        }
        daemonThreadVector.clear();
    }
    ////////////////////////////////////////////


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
        //System.err.println("Screen height: " + graphicsConfiguration.getBounds().height);
        //System.err.println("Screen width: " + graphicsConfiguration.getBounds().width);
        //System.err.println("Top insets: " + screenInsets.top);
        //System.err.println("Bottom insets: " + screenInsets.bottom);
        //System.err.println("Right insets: " + screenInsets.right);
        //System.err.println("Left insets: " + screenInsets.left);
        //  System.err.println("Determined screen height to be: " + screenHeight);
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
          System.err.println("----------------------");
          System.err.println("| Initial size: " + currentSize);
          System.err.println("| Min size: " + minSize);
          System.err.println("| Max size: " + maxSize);
          System.err.println("----------------------");
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
            //Rectangle rectangle = resizableComponent.getBounds();
            //resizableComponent.setBounds( rectangle.x, rectangle.y, currentSize.width, currentSize.height);
            //final Dimension newSize = currentSize;
            //SwingUtilities.invokeLater( new Runnable() { public void run(){
            resizableComponent.setSize( newWidth, newHeight );
            //System.err.println(" SCREEN CHANGE ---> New size: " + newSize);
            //}});
        }
        //else{
        //System.err.println(" NO SCREEN CHANGE");
        //}

    }
    //////////////////////////////////////////////////////


    // EXCEPTION HANDLING AND MESSAGE PRINTING ////////////
    private static final boolean PRINT_MESSAGES = true;
    private static Vector log;

    public static void handleExceptionNoRestart(String output, Exception e){
        printMessage(output);
        if(PRINT_MESSAGES)
            e.printStackTrace(System.err);
        log.add(e.getMessage());
        log.add(e.getStackTrace());
    }

    public static void handleExceptionWithRestart(String output, Exception e) throws Exception {
        e.printStackTrace();
        Throwable throwableRef = e;

        while( throwableRef != null){
            if( throwableRef instanceof InvocationConnectionException ){
                killDaemonThreads();
                mLoginJFrame.resetLogin("Server communication failure.  Re-login.");
                mLoginJFrame.reshowLogin();
                return;
            }
            else if( throwableRef instanceof InvocationTargetExpiredException ){
                killDaemonThreads();
                mLoginJFrame.resetLogin("Server synchronization failure.  Re-login.");
                mLoginJFrame.reshowLogin();
                return;
            }
            else if( throwableRef instanceof com.metavize.mvvm.client.LoginExpiredException ){
                killDaemonThreads();
                mLoginJFrame.resetLogin("Login expired.  Re-login.");
                mLoginJFrame.reshowLogin();
                return;
            }
            else if(    (throwableRef instanceof ConnectException)
                        || (throwableRef instanceof SocketException)
                        || (throwableRef instanceof SocketTimeoutException) ){
                killDaemonThreads();
                mLoginJFrame.resetLogin("Server connection failure.  Re-login.");
                mLoginJFrame.reshowLogin();
                return;
            }
            else if( throwableRef instanceof LoginStolenException ){
                String loginName = ((LoginStolenException)throwableRef).getThief().getMvvmPrincipal().getName();
                String loginAddress = ((LoginStolenException)throwableRef).getThief().getClientAddr().getHostAddress();
                new LoginStolenJDialog(loginName, loginAddress);
                killDaemonThreads();
                mLoginJFrame.resetLogin("Login ended by: " + loginName + " at " + loginAddress);
                mLoginJFrame.reshowLogin();
                return;
            }
            throwableRef = throwableRef.getCause();
        }

        throw e;
    }


    public static void printMessage(String message){
        if(PRINT_MESSAGES)
            System.err.println(message);
    }
    /////////////////////////////////////////////////



    // GENERAL UTIL ////////////////////////////////
    public static int chooseMax(int iValue, int iMinValue)
    {
        if (iValue >= iMinValue)
            {
                return iValue;
            }
        else
            {
                return iMinValue;
            }
    }

    public static boolean isArrayEmpty(Object[] inArray){
        if( inArray == null )
            return true;
        else if( inArray.length <= 0 )
            return true;
        else
            return false;
    }

    public static boolean isListEmpty(List inList)
    {
        return null == inList || 0 == inList.size();
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

    // TRANSFORM LOADING //////////////////////
    public static Transform getTransform(String transformName) throws Exception {
	Transform transform = null;
	List<Tid> transformInstances = Util.getTransformManager().transformInstances(transformName);
	if(transformInstances.size()>0){
	    TransformContext transformContext = Util.getTransformManager().transformContext(transformInstances.get(0));	    
	    transform = transformContext.transform();
	}
	return transform;
    }

    public static Object getRemoteObject(String className, String transformName) throws Exception {
	Transform transform = getTransform(transformName);
	if( transform != null){
	    TransformDesc transformDesc = transform.getTransformDesc();
	    Class transformClass = Util.getClassLoader().loadClass(className, transformDesc);
	    Constructor transformConstructor = transformClass.getConstructor(new Class[]{});
	    return transformConstructor.newInstance();
	}
	else
	    return null;
    }

    public static CompoundSettings getCompoundSettings(String className, String transformName) throws Exception {
	return (CompoundSettings) getRemoteObject(className, transformName);
    }
    
    public static Component getSettingsComponent(String className, String transformName) throws Exception {
	return (Component) getRemoteObject(className, transformName);
    }
    //////////////////////////////////////////
}
