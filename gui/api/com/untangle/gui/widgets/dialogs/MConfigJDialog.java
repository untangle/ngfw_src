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

package com.untangle.gui.widgets.dialogs;

import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.text.*;
import java.util.*;
import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.*;
import com.untangle.gui.widgets.coloredTable.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.*;
import com.untangle.uvm.node.*;

public abstract class MConfigJDialog extends javax.swing.JDialog implements java.awt.event.WindowListener, SettingsChangedListener {

    // SAVING/REFRESHING ///////////
    protected boolean settingsSaved;
    public boolean getSettingsSaved(){ return settingsSaved; }
    protected boolean settingsChanged = false;
    public boolean getSettingsChanged(){ return settingsChanged; }
    private Map<String, Refreshable> refreshableMap = new LinkedHashMap(5);
    protected void addRefreshable(String name, Refreshable refreshable){ refreshableMap.put(name, refreshable); }
    private Map<String, Savable> savableMap = new LinkedHashMap(5);
    protected void addSavable(String name, Savable savable){ savableMap.put(name, savable); }
    private Map<String, Shutdownable> shutdownableMap = new LinkedHashMap(1);
    protected void addShutdownable(String name, Shutdownable shutdownable){ shutdownableMap.put(name, shutdownable); }
    protected CompoundSettings compoundSettings;
    public CompoundSettings getCompoundSettings(){ return compoundSettings; }
    protected InfiniteProgressJComponent infiniteProgressJComponent = new InfiniteProgressJComponent();
    public InfiniteProgressJComponent getInfiniteProgressJComponent(){ return infiniteProgressJComponent; }
    private static final long MIN_PROGRESS_MILLIS = 1000;
    private static final int HELPER_SAVE_SETTINGS_BLINK = 200;
    private static ImageIcon[] saveSettingsImageIcons;
    CycleJLabel saveSettingsHintJLabel;
    ///////////////////////////////

    public MConfigJDialog(Dialog parentDialog){
        super(parentDialog, true);
        init(parentDialog);
    }

    public MConfigJDialog(Frame parentFrame) {
        super(parentFrame, true);
        init(parentFrame);
    }

    private void init(Window parentWindow){

        synchronized( this ){
            if( saveSettingsImageIcons == null ){
                String[] saveSettingsImagePaths = { "com/untangle/gui/node/IconSaveSettingsHint30.png",
                                                    "com/untangle/gui/node/IconSaveSettingsHint40.png",
                                                    "com/untangle/gui/node/IconSaveSettingsHint50.png",
                                                    "com/untangle/gui/node/IconSaveSettingsHint60.png",
                                                    "com/untangle/gui/node/IconSaveSettingsHint70.png",
                                                    "com/untangle/gui/node/IconSaveSettingsHint80.png",
                                                    "com/untangle/gui/node/IconSaveSettingsHint90.png",
                                                    "com/untangle/gui/node/IconSaveSettingsHint100.png" };
                saveSettingsImageIcons = Util.getImageIcons(saveSettingsImagePaths);
            }
        }
        saveSettingsHintJLabel = new CycleJLabel(saveSettingsImageIcons, HELPER_SAVE_SETTINGS_BLINK, true, true);

        getRootPane().setDoubleBuffered(true);
        RepaintManager.currentManager(this).setDoubleBufferingEnabled(true);
        initComponents();
        helpJButton.setVisible(false);
        setBounds( Util.generateCenteredBounds( parentWindow.getBounds(), getMinSize().width, getMinSize().height) );
        addWindowListener(this);
        setGlassPane(infiniteProgressJComponent);
        addComponentListener( new ComponentAdapter() { public void componentResized(ComponentEvent evt) {
            dialogResized();
        }});
        setSaveSettingsHintVisible(false);
    }

    private static JComponent initialFocusComponent;
    public static void setInitialFocusComponent(JComponent c){
        initialFocusComponent = c;
    }
    public static JComponent getInitialFocusComponent(){ return initialFocusComponent; }

    public void setVisible(boolean isVisible){
        if(isVisible){
            new RefreshAllThread(true);
        }
        super.setVisible(isVisible);
        if(!isVisible){
            dispose();
        }
    }

    private String helpSource;
    public void setHelpSource(String helpSource){
        this.helpSource = helpSource;
        helpJButton.setVisible(true);
    }


    public void settingsChanged(Object source){
        settingsChanged = true;
        saveJButton.setEnabled(true);
        setSaveSettingsHintVisible(true);
    }

    protected abstract void generateGui();

    public void setSaveSettingsHintVisible(boolean isVisible){
        if( isVisible ){
            if( !saveSettingsHintJLabel.isRunning() )
                saveSettingsHintJLabel.start();
        }
        else
            saveSettingsHintJLabel.stop();
    }

    // TABS AND TABBED PANES //////////////
    public JTabbedPane getMTabbedPane(){ return contentJTabbedPane; }
    public void addTab(String name, Icon icon, Component component){ contentJTabbedPane.addTab(name, icon, component); }
    public JTabbedPane addTabbedPane(String name, Icon icon){
        JTabbedPane newJTabbedPane = new JTabbedPane();
        newJTabbedPane.setBorder(new EmptyBorder(7, 13, 13, 13));
        newJTabbedPane.setFont(new java.awt.Font("Arial", 0, 11));
        JPanel backJPanel = new JPanel();
        backJPanel.setLayout(new BorderLayout());
        backJPanel.add(newJTabbedPane);
        addTab(name, icon, backJPanel);
        return newJTabbedPane;
    }
    public JScrollPane addScrollableTab(JTabbedPane parentJTabbedPane, String name, Icon icon,
                                        Component childComponent, boolean scrollH, boolean scrollV){
        JScrollPane newJScrollPane = new JScrollPane(childComponent);
        newJScrollPane.getHorizontalScrollBar().setFocusable(false);
        newJScrollPane.getVerticalScrollBar().setFocusable(false);
        newJScrollPane.setHorizontalScrollBarPolicy( scrollH ? JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS : JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
        newJScrollPane.setVerticalScrollBarPolicy( scrollV ? JScrollPane.VERTICAL_SCROLLBAR_ALWAYS : JScrollPane.VERTICAL_SCROLLBAR_NEVER );
        if( parentJTabbedPane != null )
            parentJTabbedPane.addTab(name, icon, newJScrollPane);
        else
            addTab(name, icon, newJScrollPane);
        newJScrollPane.getVerticalScrollBar().setUnitIncrement(5);
        return newJScrollPane;
    }
    //////////////////////////////////////

    protected boolean shouldSave(){ return true; }

    private Exception saveException;
    protected void saveAll() throws Exception {
        // GATHER ALL SETTINGS AND VALIDATE INDIVIDUALLY
        for( Map.Entry<String, Savable> savableMapEntry : savableMap.entrySet() ){
            String componentName = savableMapEntry.getKey();
            final Savable savableComponent = savableMapEntry.getValue();
            saveException = null;
            SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
                try{ savableComponent.doSave(compoundSettings, false); }
                catch(Exception e){
                    saveException = e;
                }
            }});
            if(saveException != null){
                ValidateFailureDialog.factory( (Window) MConfigJDialog.this,
                                               getTitle(), componentName, saveException.getMessage() );
                throw new ValidationException();
            }
        }
        // VALIDATE SIMULTANEOUSLY
        try{
            if( compoundSettings != null )
                compoundSettings.validate();
        }
        catch(Exception e){
            ValidateFailureDialog.factory( (Window) MConfigJDialog.this,
                                           getTitle(), "multiple settings panels", e.getMessage() );
            throw new ValidationException();
        }
        // SEND SETTINGS TO SERVER
        if( compoundSettings != null )
            compoundSettings.save();

        // RECORD THE FACT THAT SETTINGS WERE SAVED FOR WHEN THE DIALOG RETURNS
        settingsSaved = true;
        SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
            saveJButton.setEnabled(false);
        }});
        setSaveSettingsHintVisible(false);
        settingsChanged = false;
    }


    protected void refreshAll() throws Exception {
        // GET SETTINGS FROM SERVER
        if( compoundSettings != null )
            compoundSettings.refresh();
    }

    protected void populateAll() throws Exception {
        // UPDATE PANELS WITH NEW SETTINGS
        for( Map.Entry<String, Refreshable> refreshableMapEntry : refreshableMap.entrySet() ){
            final String componentName = refreshableMapEntry.getKey();
            final Refreshable refreshableComponent = refreshableMapEntry.getValue();
            SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
                try{ refreshableComponent.doRefresh(compoundSettings); }
                catch(Exception e){
                    Util.handleExceptionNoRestart("Error distributing settings", e);
                    RefreshFailureDialog.factory( (Window) MConfigJDialog.this,
                                                  componentName );
                }
            }});
        }
        SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
            saveJButton.setEnabled(false);
        }});
        setSaveSettingsHintVisible(false);
        settingsChanged = false;
    }
    ////////////////////////////////////////////

    // SIZING ///////////////////////////////
    protected Dimension getMinSize(){
        return new Dimension(640, 480);
    }
    protected Dimension getMaxSize(){
        return new Dimension(1600, 1200);
    }
    private void dialogResized(){
        Util.resizeCheck(this, getMinSize(), getMaxSize());
    }
    ////////////////////////////////////////

    // ACTION BUTTONS /////////////////////
    public void refreshGui(){
        new RefreshAllThread(false);
    }
    public void removeActionButtons(){
        saveJButton.setVisible(false);
    }
    ///////////////////////////////////////


    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        buttonGroup1 = new javax.swing.ButtonGroup();
        contentJTabbedPane = new javax.swing.JTabbedPane();
        nbSaveSettingsHintJLabel = saveSettingsHintJLabel;
        helpJButton = new javax.swing.JButton();
        closeJButton = new javax.swing.JButton();
        saveJButton = new javax.swing.JButton();
        backgroundJLabel = new com.untangle.gui.widgets.MTiledIconLabel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("");
        setModal(true);
        contentJTabbedPane.setFont(new java.awt.Font("Default", 0, 12));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 15, 15);
        getContentPane().add(contentJTabbedPane, gridBagConstraints);

        nbSaveSettingsHintJLabel.setFont(new java.awt.Font("Arial", 0, 18));
        nbSaveSettingsHintJLabel.setForeground(new java.awt.Color(255, 0, 0));
        nbSaveSettingsHintJLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        nbSaveSettingsHintJLabel.setFocusable(false);
        nbSaveSettingsHintJLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        nbSaveSettingsHintJLabel.setIconTextGap(0);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 38, 60);
        getContentPane().add(nbSaveSettingsHintJLabel, gridBagConstraints);

        helpJButton.setFont(new java.awt.Font("Dialog", 0, 12));
        helpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconHelp_18x16.png")));
        helpJButton.setText("Help");
        helpJButton.setIconTextGap(6);
        helpJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        helpJButton.setOpaque(false);
        helpJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    helpJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 15, 0);
        getContentPane().add(helpJButton, gridBagConstraints);

        closeJButton.setFont(new java.awt.Font("Default", 0, 12));
        closeJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconClose_16x16.png")));
        closeJButton.setText("Cancel");
        closeJButton.setIconTextGap(6);
        closeJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        closeJButton.setOpaque(false);
        closeJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    closeJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 15);
        getContentPane().add(closeJButton, gridBagConstraints);

        saveJButton.setFont(new java.awt.Font("Arial", 0, 12));
        saveJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconSave_23x16.png")));
        saveJButton.setText("Save");
        saveJButton.setIconTextGap(6);
        saveJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        saveJButton.setOpaque(false);
        saveJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    saveJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 15);
        getContentPane().add(saveJButton, gridBagConstraints);

        backgroundJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/DarkGreyBackground1600x100.png")));
        backgroundJLabel.setFocusable(false);
        backgroundJLabel.setOpaque(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(backgroundJLabel, gridBagConstraints);

    }//GEN-END:initComponents

    private void helpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpJButtonActionPerformed
        try{
            String focus = Util.getSelectedTabTitle(contentJTabbedPane).toLowerCase().replace(" ", "_");
            URL newURL = new URL( "http://www.untangle.com/docs/get.php?"
                                  + "version=" + Version.getVersion()
                                  + "&source=" + helpSource
                                  + "&focus=" + focus);
            ((BasicService) ServiceManager.lookup("javax.jnlp.BasicService")).showDocument(newURL);
        }
        catch(Exception f){
            Util.handleExceptionNoRestart("Error showing help for " + helpSource, f);
        }
    }//GEN-LAST:event_helpJButtonActionPerformed

    private void saveJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveJButtonActionPerformed
        if( Util.getIsDemo() )
            return;
        if( !shouldSave() )
            return;
        new SaveAllThread(true);
    }//GEN-LAST:event_saveJButtonActionPerformed
    public boolean saveSettings(){
        SaveAllThread thread = new SaveAllThread(false);
        while(thread.isAlive()){
            try{Thread.sleep(500l);} catch(Exception e){ e.printStackTrace(); }
        }
        return thread.getSuccessfulSave();
    }
    private void closeJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeJButtonActionPerformed
        setVisible(false);
    }//GEN-LAST:event_closeJButtonActionPerformed

    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
        setVisible(false);
    }

    public void windowClosed(java.awt.event.WindowEvent windowEvent) {}
    public void windowActivated(java.awt.event.WindowEvent windowEvent) {}
    public void windowDeactivated(java.awt.event.WindowEvent windowEvent) {}
    public void windowDeiconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowIconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowOpened(java.awt.event.WindowEvent windowEvent) {}



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel backgroundJLabel;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton closeJButton;
    protected javax.swing.JTabbedPane contentJTabbedPane;
    private javax.swing.JButton helpJButton;
    protected javax.swing.JLabel nbSaveSettingsHintJLabel;
    protected javax.swing.JButton saveJButton;
    // End of variables declaration//GEN-END:variables

    private class SaveAllThread extends Thread{
        private boolean doClose;
        private volatile boolean successfulSave;
        public SaveAllThread(boolean doClose){
            super("MVCLIENT-ConfigSaveAllThread");
            setDaemon(true);
            this.doClose = doClose;
            if(doClose)
                infiniteProgressJComponent.start("Saving...");
            else
                infiniteProgressJComponent.startLater("Saving...");
            start();
        }
        public boolean getSuccessfulSave(){ return successfulSave; }
        public void run(){
            long startTime = System.currentTimeMillis();
            boolean noException = true;
            try{
                MConfigJDialog.this.saveAll();
                successfulSave = true;
                if(!doClose){
                    MConfigJDialog.this.refreshAll();
                    MConfigJDialog.this.populateAll();
                }
            }
            catch(Exception e){
                try{ Util.handleExceptionWithRestart("Error sending saved settings", e); }
                catch(ValidationException f){ noException = false; }
                catch(Exception g){
                    noException = false;
                    Util.handleExceptionNoRestart("Error sending saved settings", g);
                    SaveFailureDialog.factory( (Window) MConfigJDialog.this,
                                               MConfigJDialog.this.getTitle() );
                }
            }
            infiniteProgressJComponent.stopLater(MIN_PROGRESS_MILLIS);
            if(doClose && noException){
                setVisible(false);
            }
        }
    }

    private Exception generateGuiException;
    private class RefreshAllThread extends Thread{
        private boolean doGenerateGui;
        public RefreshAllThread(boolean doGenerateGui){
            super("MVCLIENT-ConfigRefreshAllThread");
            setDaemon(true);
            this.doGenerateGui = doGenerateGui;
            // START INFINITE PROGRESS
            infiniteProgressJComponent.start("Refreshing...");
            start();
        }
        public void run(){
            long startTime = System.currentTimeMillis();
            // INIT COMPOUND SETTINGS
            try{
                MConfigJDialog.this.refreshAll();
                if(doGenerateGui){
                    generateGuiException = null;
                    SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
                        MConfigJDialog.this.generateGui();
                    }});
                    if( generateGuiException != null )
                        throw generateGuiException;
                }
                MConfigJDialog.this.populateAll();
            }
            catch(Exception e){
                try{ Util.handleExceptionWithRestart("Error refreshing settings", e); }
                catch(Exception f){
                    Util.handleExceptionNoRestart("Error refreshing settings", f);
                    RefreshFailureDialog.factory( (Window) MConfigJDialog.this,
                                                  MConfigJDialog.this.getTitle() );
                }
            }
            // END INFINITE PROGRESS
            infiniteProgressJComponent.stopLater(MIN_PROGRESS_MILLIS);
            // FOCUS
            SwingUtilities.invokeLater( new Runnable(){ public void run(){
                JComponent target = MConfigJDialog.getInitialFocusComponent();
                if(target != null){
                    target.requestFocus();
                    if(target instanceof JTextComponent)
                        ((JTextComponent)target).selectAll();
                }
            }});

        }
    }
}







