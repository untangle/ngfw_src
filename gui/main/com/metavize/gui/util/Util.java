/*
 * util.java
 *
 * Created on March 28, 2004, 5:52 PM
 */

package com.metavize.gui.util;

import java.awt.*;
import java.util.Vector;
import javax.swing.*;
import java.util.StringTokenizer;

import java.net.*;

import com.metavize.gui.login.MLoginJFrame;
import com.metavize.gui.main.MMainJFrame;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.widgets.editTable.*;

import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;
import com.metavize.mvvm.client.*;


/**
 *
 * @author  Ian Nieves
 */
public class Util {
    
    // SERVER PROXIES ///////////////
    private static MvvmContext mvvmContext;
    private static ToolboxManager toolboxManager;
    private static TransformManager transformManager;

    public static void setMvvmContext(MvvmContext mvvmContextX){
	mvvmContext = mvvmContextX;
	toolboxManager = mvvmContext.toolboxManager();
	transformManager = mvvmContext.transformManager();
    }
    public static MvvmContext getMvvmContext(){ return mvvmContext; }
    public static ToolboxManager getToolboxManager() { return toolboxManager; }
    public static TransformManager getTransformManager() { return transformManager; }
    



    private static String undetectedMessage  = "<html>To access this functionality, you must install an AntiVirus Scanner Softwawre Appliance.</html>";
    private static EmailDetectionJPanel emailDetectionSophosJPanel;
    private static EmailDetectionJPanel emailDetectionFprotJPanel;
    private static EmailDetectionJPanel emailDetectionHauriJPanel;
    private static MEditTableJPanel virusMEditTableJPanel;

    /* DefaultTableColumnModel constants */
    public static final int TABLE_TOTAL_WIDTH = 471; /* in pixels (contains extra pixel) */
    public static final int LINENO_MIN_WIDTH = 30; /* # */
    public static final int STATUS_MIN_WIDTH = 55; /* status */

    private static ClassLoader initClassLoader = null;
    private static MURLClassLoader mURLClassLoader = null;
    private static boolean killThreads = false;
    private static final boolean PRINT_MESSAGES = true;
    private static Boolean pipelineSync;
    private static boolean isDemo = false;
    private static JProgressBar statusJProgressBar;
    private static MPipelineJPanel mPipelineJPanel;
    
    private static MMainJFrame mMainJFrame;
    private static MLoginJFrame mLoginJFrame;
    private static String[] args;

    private static JFrame jFrame;

    private static Vector log;

    static{
        pipelineSync = new Boolean(false);
        log = new Vector();
    }

    public static boolean getKillThreads(){
        return killThreads;
    }
    public static void exit(int i){
        killThreads = true;
        System.exit(i);
    }


    public static void setVirusMEditTableJPanel(MEditTableJPanel virusMEditTableJPanelX){ /* System.err.println("Settings email: " + virusMEditTableJPanelX); */ virusMEditTableJPanel = virusMEditTableJPanelX; }
    public static void setEmailDetectionSophosJPanel(EmailDetectionJPanel emailDetectionJPanel){ /* System.err.println("Setting sophos: " + emailDetectionJPanel); */ emailDetectionSophosJPanel = emailDetectionJPanel; }
    public static void setEmailDetectionFprotJPanel(EmailDetectionJPanel emailDetectionJPanel){ /* System.err.println("Setting fprot: " + emailDetectionJPanel); */ emailDetectionFprotJPanel = emailDetectionJPanel; }
    public static void setEmailDetectionHauriJPanel(EmailDetectionJPanel emailDetectionJPanel){ /* System.err.println("Setting fprot: " + emailDetectionJPanel); */ emailDetectionHauriJPanel = emailDetectionJPanel; }
    public static void updateDependencies(){
	// System.err.println("UPDATING");
	if( virusMEditTableJPanel != null){
	    // System.err.println("email transform detected");
	    if(emailDetectionSophosJPanel != null)
		emailDetectionSophosJPanel.setDetected(true);
	    if(emailDetectionFprotJPanel != null)
		emailDetectionFprotJPanel.setDetected(true);
            if(emailDetectionHauriJPanel != null)
		emailDetectionHauriJPanel.setDetected(true);
	}
	else{
	    // System.err.println("no email transform detected");
	    if(emailDetectionSophosJPanel != null)
		emailDetectionSophosJPanel.setDetected(false);
	    if(emailDetectionFprotJPanel != null)
		emailDetectionFprotJPanel.setDetected(false);
            if(emailDetectionHauriJPanel != null)
		emailDetectionHauriJPanel.setDetected(false);
	}

	/*
	if(emailDetectionSophosJPanel != null)
	    System.err.println("sophos detected");
	else
	    System.err.println("no sophos detected");
	if(emailDetectionFprotJPanel != null)
	    System.err.println("fprot detected");
	else
	    System.err.println("no fprot detected");
	*/

	if(virusMEditTableJPanel != null){
	    int index = ((JTabbedPane)virusMEditTableJPanel.getParent()).indexOfComponent(virusMEditTableJPanel);
	    if( emailDetectionSophosJPanel!=null ){
		((JTabbedPane)virusMEditTableJPanel.getParent()).setTitleAt(index, "AntiVirus (by Sophos)");
		virusMEditTableJPanel.setMessage( null );
	    }
            else if( emailDetectionFprotJPanel!=null ){
		((JTabbedPane)virusMEditTableJPanel.getParent()).setTitleAt(index, "AntiVirus (by F-Prot)");
		virusMEditTableJPanel.setMessage( null );
	    }
            else if( emailDetectionHauriJPanel!=null ){
		((JTabbedPane)virusMEditTableJPanel.getParent()).setTitleAt(index, "AntiVirus (by Hauri)");
		virusMEditTableJPanel.setMessage( null );
	    }
	    else{
		((JTabbedPane)virusMEditTableJPanel.getParent()).setTitleAt(index, "AntiVirus (uninstalled)");
		virusMEditTableJPanel.setMessage( undetectedMessage );
	    }
	}	
    }


    public static Object getPipelineSync(){ return pipelineSync; }

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

    public static void setMLoginJFrame(MLoginJFrame mLoginJFrameX){ mLoginJFrame = mLoginJFrameX; }
    public static JFrame getMLoginJFrame(){ return mLoginJFrame; }

    public static void setMMainJFrame(MMainJFrame mMainJFrameX){ mMainJFrame = mMainJFrameX; }
    public static MMainJFrame getMMainJFrame(){ return mMainJFrame; }


    public static void setArgs( String argsX[] ){
        args = argsX;
    }


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

        //System.err.println("");
        //System.err.println("parentBound: " + parentBounds);
        //System.err.println("childBound: " + childBounds);


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
	//boolean lastBR = false;
	while( stringTokenizer.hasMoreTokens() ){
	    tempString = stringTokenizer.nextToken();
	    //lastBR = false;
	
	    if( currentLineLength + tempString.length() >= lineLength ){
		stringBuffer.append("<br>" + tempString + " ");
		currentLineLength = tempString.length() + 1;
		//lastBR = true;
	    }
	    else{
		stringBuffer.append(tempString + " ");
		currentLineLength += (tempString.length() + 1);
	    }
	}
	//if( !lastBR ){
	//stringBuffer.append("<br>");
	//}
	return stringBuffer.toString();
    }

    public static int determineMinHeight(int attemptedMinHeight){
        GraphicsConfiguration graphicsConfiguration = getGraphicsConfiguration();
	Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets( graphicsConfiguration );
	int screenHeight = graphicsConfiguration.getBounds().height - screenInsets.top - screenInsets.bottom;
	if( screenHeight < attemptedMinHeight)
	    return screenHeight;
	else
	    return attemptedMinHeight;
    }
    
    public static void resizeCheck(Component resizableComponent, Dimension minSize, Dimension maxSize){

        Dimension currentSize = new Dimension( resizableComponent.getSize() );

        boolean resetSize = false;
        if(currentSize.width < minSize.width){
            currentSize.width = minSize.width;
            resetSize = true;
        }
        else if(currentSize.width > maxSize.width){
            currentSize.width = maxSize.width;
            resetSize = true;
        }
        if(currentSize.height < minSize.height){
            currentSize.height = minSize.height;
            resetSize = true;
        }
        else if(currentSize.height > maxSize.height){
            currentSize.height = maxSize.height;
            resetSize = true;
        }
        if(resetSize){
            resizableComponent.setSize(currentSize);
        }

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
	    else if( throwableRef instanceof LoginExpiredException ){
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
