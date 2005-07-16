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

import java.awt.*;
import java.net.*;
import java.util.*;
import javax.jnlp.*;
import javax.swing.*;

import com.metavize.gui.login.MLoginJFrame;
import com.metavize.gui.main.MMainJFrame;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.pipeline.MRackJPanel;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.mvvm.*;
import com.metavize.mvvm.client.*;
import com.metavize.mvvm.security.*;
import com.metavize.mvvm.tran.*;


/**
 *
 * @author  Ian Nieves
 */
public class Util {

    // 2.4.0 INFO /////////////////
    private static String version = "2.5.0"; /* DO NOT EDIT BY HAND */
    public static String getVersion(){ return version; }
    /////////////////////////////////

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

    public static void setMvvmContext(MvvmRemoteContext mvvmContextX){
    mvvmContext = mvvmContextX;
    toolboxManager = mvvmContext.toolboxManager();
    transformManager = mvvmContext.transformManager();
	adminManager = mvvmContext.adminManager();
	networkingManager = mvvmContext.networkingManager();
        // Somewhere else this should go? XXX jdi
        statsCache = new StatsCache();
    }

    public static MvvmRemoteContext getMvvmContext(){ return mvvmContext; }
    public static ToolboxManager getToolboxManager(){ return toolboxManager; }
    public static TransformManager getTransformManager(){ return transformManager; }
    public static AdminManager getAdminManager(){ return adminManager; }
    public static StatsCache getStatsCache(){ return statsCache; }
    public static NetworkingManager getNetworkingManager(){ return networkingManager; }
    ///////////////////////////////////

    // VALIDATION //////////////////
    public static Color INVALID_BACKGROUND_COLOR = Color.PINK;
    public static Color VALID_BACKGROUND_COLOR = new Color(224, 224, 224);
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
		Util.handleExceptionNoRestart("Error:", e);
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
    public static final int UPGRADE_THREAD_SLEEP_MILLIS = 60 * (60 * 1000); // X * (minutes * 1000)
    public static final long UPGRADE_STORE_CHECK_FRESH_MILLIS = 60l * (5l * 1000l); // X * (minutes * 1000)

    private static long lastUpgradeCheck = 0l;
    public static boolean mustCheckUpgrades(){
	if( System.currentTimeMillis() - lastUpgradeCheck > UPGRADE_STORE_CHECK_FRESH_MILLIS )
	    return true;
	else
	    return false;
    }
    public static void checkedUpgrades(){ lastUpgradeCheck = System.currentTimeMillis(); }
    ///////////////////////////////

    // DefaultTableColumnModel constants /////////
    public static final int TABLE_TOTAL_WIDTH = 471; /* in pixels (contains extra pixel) */
    public static final int LINENO_MIN_WIDTH = 38; /* # */
    public static final int STATUS_MIN_WIDTH = 55; /* status */
    public static final int TIMESTAMP_MIN_WIDTH = 55; /* time stamp */
    //////////////////////////////////////////////

    // GUI COMPONENTS AND FUNCTIONALITY //////////
    private static ClassLoader initClassLoader = null;
    private static MURLClassLoader mURLClassLoader = null;
    private static JProgressBar statusJProgressBar;
    private static boolean isDemo = false;
    private static MPipelineJPanel mPipelineJPanel;
    private static MRackJPanel mRackJPanel;
    private static MLoginJFrame mLoginJFrame;
    private static MMainJFrame mMainJFrame;

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
    ////////////////////////////////////////////


    private static boolean killThreads = false;
    private static final boolean PRINT_MESSAGES = true;

    private static Vector log;

    static{
        log = new Vector();
    }

    public static boolean getKillThreads(){
        return killThreads;
    }
    public static void exit(int i){
        killThreads = true;
        System.exit(i);
    }


    ////////////////////////////////////////////
    
    
    public static GraphicsConfiguration getGraphicsConfiguration(){
	GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice graphicsDevice = graphicsEnvironment.getDefaultScreenDevice();
        GraphicsConfiguration graphicsConfiguration = graphicsDevice.getDefaultConfiguration();
	return graphicsConfiguration;
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

    public static void handleExceptionNoRestart(String output, Exception e){
        printMessage(output);
        if(PRINT_MESSAGES)
            e.printStackTrace(System.err);
        log.add(e.getMessage());
        log.add(e.getStackTrace());
    }


    public static void handleExceptionWithRestart(String output, Exception e) throws Exception {
        // DEAL WITH COMMUNICATIONS FAILURES

	// System.err.println("catching exception: " + e.toString() );
	Throwable throwableRef = e;
	
	while( throwableRef != null){
	    if( throwableRef instanceof InvocationConnectionException ){
		mLoginJFrame.resetLogin("Server communication failure.  Re-login.");
		if(PRINT_MESSAGES)
		    e.printStackTrace(System.err);
		mLoginJFrame.reshowLogin();
		return;
	    }
	    else if( throwableRef instanceof InvocationTargetExpiredException ){
		mLoginJFrame.resetLogin("Server synchronization failure.  Re-login.");
		if(PRINT_MESSAGES)
		    e.printStackTrace(System.err);
		mLoginJFrame.reshowLogin();
		return;
	    }
	    else if( throwableRef instanceof com.metavize.mvvm.client.LoginExpiredException ){
		mLoginJFrame.resetLogin("Login expired.  Re-login.");
		if(PRINT_MESSAGES)
		    e.printStackTrace(System.err);
		mLoginJFrame.reshowLogin();
		return;
	    }
	    else if(    (throwableRef instanceof ConnectException)
		     || (throwableRef instanceof SocketException)
		     || (throwableRef instanceof SocketTimeoutException) ){
		mLoginJFrame.resetLogin("Server connection failure.  Re-login.");
		if(PRINT_MESSAGES)
		    e.printStackTrace(System.err);
		mLoginJFrame.reshowLogin();
		return;
	    }
	    throwableRef = throwableRef.getCause();
	}
        
	// System.err.println("THROWING");
	throw e;
        
    }


    public static void printMessage(String message){
        if(PRINT_MESSAGES)
            System.err.println(message);
    }

    /* choose largest of two values */
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
}
