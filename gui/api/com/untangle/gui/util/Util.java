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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.JTextArea;
import javax.swing.LookAndFeel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.apache.log4j.Logger;

public class Util {
    private static final String PROPERTY_IS_DEVEL = "com.untangle.isDevel";
    private static final String BASENAME_COMMUNITY_PREFIX = "i18n.community";
    private static final String BASENAME_OFFICIAL_PREFIX = "i18n.official";

    private static final Logger logger = Logger.getLogger(Util.class);

    private static final List<Localizable> localizable = new ArrayList<Localizable>();

    private Util(){}

    public static void initialize(){
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

    // BRANDING ///////////////////
    public static final String getCompanyName(){ return "Untangle"; }

    // LOOK AND FEEL //////////////
    private static LookAndFeel lookAndFeel;
    public static LookAndFeel getLookAndFeel(){ return lookAndFeel; }
    public static void setLookAndFeel(LookAndFeel x){ lookAndFeel = x; }

    // NETWORKING ////////////////
    public static final int RECONFIGURE_NETWORK_TIMEOUT_MILLIS = 60*1000;
    public static final int DISCONNECT_NETWORK_TIMEOUT_MILLIS = 15*1000;

    // BUTTON DECALS /////////////////

    public static ImageIcon[] getImageIcons(String[] imagePaths){
        ImageIcon[] imageIcons = new ImageIcon[imagePaths.length];
        for( int i=0; i<imagePaths.length; i++){
            imageIcons[i] = new javax.swing.ImageIcon( Util.class.getClassLoader().getResource(imagePaths[i]) );
        }
        return imageIcons;
    }

    private static Map<String, String> i18nMap = new HashMap<String, String>();

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


    // UNTANGLE_APPLIANCE //////////////////////
    private static boolean isUntangleAppliance = false;
    public static void setUntangleAppliance(boolean isX) { isUntangleAppliance = isX; }
    public static boolean isUntangleAppliance(){ return isUntangleAppliance; }
    //////////////////////////////

    // INSIDE_VM //////////////////////
    private static boolean isInsideVM = false;
    public static void setInsideVM(boolean isX) { isInsideVM = isX; }
    public static boolean isInsideVM(){ return isInsideVM; }
    //////////////////////////////

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
        if( lastUpgradeCheck == 0l ){
            return true;
        }
        else if( System.currentTimeMillis() - lastUpgradeCheck > UPGRADE_STORE_CHECK_FRESH_MILLIS){
            return true;
        }
        else if (upgradeCount != 0){
            return true;
        }
        else{
            return false;
        }
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
    private static JProgressBar statusJProgressBar;
    private static boolean isDemo;

    public static ClassLoader getInitClassLoader(){ return initClassLoader; }
    public static void setInitClassLoader(ClassLoader initClassLoaderX){ initClassLoader = initClassLoaderX;}
    public static JProgressBar getStatusJProgressBar(){ return statusJProgressBar; }
    public static void setStatusJProgressBar(JProgressBar statusJProgressBarX){ statusJProgressBar = statusJProgressBarX; }
    public static boolean getIsDemo(){ return isDemo; }
    public static void setIsDemo(boolean isDemoX){ isDemo = isDemoX; }
    ////////////////////////////////////////////

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

    //////////////////////////////////////////////////////

    // EXCEPTION HANDLING AND MESSAGE PRINTING ////////////
    public synchronized static void handleExceptionNoRestart(String output, Exception e){
        logger.debug(output,e);
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

    public static void addLocalizable(Localizable l)
    {
        localizable.add(l);
    }

    public static void setLocale(Locale l)
    {
        i18nMap = getTranslations(l);
        for (Localizable lable : localizable) {
            lable.reloadStrings();
        }
    }

    public static String tr(String value)
    {
        String tr = i18nMap.get(value);
        if (tr == null) {
            tr = value;
        }
        return tr;
    }

    public static String tr(String value, Object o1)
    {
        return tr(value, new Object[]{ o1 });
    }

    private static Map<String, String> getTranslations(Locale locale)
    {
        String module = "untangle-install-wizard";
        String ungPrefixedModule = "ung_" + module;
        Map<String, String> map = new HashMap<String, String>();

        try {
            Class clazz = loadResourceBundleClass(BASENAME_COMMUNITY_PREFIX + "." + ungPrefixedModule + "_" + locale.getLanguage());
            if (clazz == null) {
                // fall back to official translations
                clazz = loadResourceBundleClass(BASENAME_OFFICIAL_PREFIX + "." + ungPrefixedModule + "_" + locale.getLanguage());
            }
            if (clazz != null) {
                ResourceBundle resourceBundle = (ResourceBundle)clazz.newInstance();
                if (resourceBundle != null) {
                    for (Enumeration<String> enumeration = resourceBundle.getKeys(); enumeration.hasMoreElements();) {
                        String key = enumeration.nextElement();
                        map.put(key, resourceBundle.getString(key));
                    }
                }
            }
        } catch (Exception e) {
        }

        return map;
    }

    private static Class loadResourceBundleClass(String name) {
        Class clazz = null;
        try {
            clazz =Thread.currentThread().getContextClassLoader().loadClass(name);
        } catch (ClassNotFoundException e1) {
        }
        return clazz;
    }
}
