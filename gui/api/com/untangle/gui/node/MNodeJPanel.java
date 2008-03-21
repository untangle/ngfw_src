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

package com.untangle.gui.node;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.*;
import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;
import javax.swing.*;

import com.untangle.gui.util.*;
import com.untangle.gui.widgets.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.uvm.*;
import com.untangle.uvm.node.*;
import com.untangle.uvm.policy.*;
import com.untangle.uvm.security.*;
import com.untangle.uvm.toolbox.MackageDesc;

public class MNodeJPanel extends javax.swing.JPanel {

    // UVM MODEL
    protected NodeContext nodeContext;
    protected Node node;
    public Node getNode(){ return node; }
    protected NodeDesc nodeDesc;
    public NodeDesc getNodeDesc(){ return nodeDesc; }
    protected MackageDesc mackageDesc;
    public MackageDesc getMackageDesc(){ return mackageDesc; }
    public MackageDesc getNewMackageDesc() throws Exception{ return nodeContext.getMackageDesc(); }
    protected Tid tid;
    public Tid getTid(){ return tid; }
    protected Policy policy;
    public Policy getPolicy(){ return policy; }
    private void setPolicy(Policy policy){ this.policy = policy; }

    // GUI VIEW MODEL
    private MNodeControlsJPanel mNodeControlsJPanel;
    public MNodeControlsJPanel mNodeControlsJPanel(){ return mNodeControlsJPanel; }
    private MNodeDisplayJPanel mNodeDisplayJPanel;
    public MNodeDisplayJPanel mNodeDisplayJPanel() { return mNodeDisplayJPanel; }

    // GUI DATA MODEL
    protected MStateMachine mStateMachine;
    CycleJLabel powerOnHintJLabel;
    private static ImageIcon[] powerOnImageIcons;
    private DropdownTask controlsDropdownTask;
    private volatile boolean controlsLoaded;
    private volatile boolean showingSettings;
    private ShowControlsThread showControlsThread;

    // GUI CONSTANTS
    private static Dimension maxDimension, minDimension;
    private static final int HELPER_POWER_ON_BLINK = 200;

    // SHUTDOWNABLE
    private Map<String, Shutdownable> shutdownableMap = new LinkedHashMap(1);
    protected void addShutdownable(String name, Shutdownable shutdownable){ shutdownableMap.put(name, shutdownable); }
    protected void removeShutdownable(String shutdownableKey){ shutdownableMap.remove(shutdownableKey); }

    public static MNodeJPanel instantiate(NodeContext nodeContext, NodeDesc nodeDesc, Policy policy) throws Exception {
        String className = nodeDesc.getGuiClassName();
        if (className == null) return null;

        Class guiClass = Util.getClassLoader().mLoadClass(className);

        Constructor guiConstructor = guiClass.getConstructor( new Class[]{NodeContext.class, NodeDesc.class} );
        MNodeJPanel mNodeJPanel = (MNodeJPanel) guiConstructor.newInstance(new Object[]{nodeContext, nodeDesc});
        mNodeJPanel.setPolicy(policy);
        return mNodeJPanel;
    }

    public MNodeJPanel(NodeContext nodeContext, NodeDesc nodeDesc) { // this should not be instantiated
        setDoubleBuffered(true);
        this.nodeContext = nodeContext;
        this.nodeDesc = nodeDesc;
        node = nodeContext.node();
        mackageDesc = nodeContext.getMackageDesc();
        tid = nodeDesc.getTid();
        controlsLoaded = false;
        showControlsThread = new ShowControlsThread();

        // VISUAL HELPER
        synchronized( this ){
            if( powerOnImageIcons == null ){
                String[] powerOnImagePaths = { "com/untangle/gui/node/IconPowerOnHint30.png",
                                               "com/untangle/gui/node/IconPowerOnHint40.png",
                                               "com/untangle/gui/node/IconPowerOnHint50.png",
                                               "com/untangle/gui/node/IconPowerOnHint60.png",
                                               "com/untangle/gui/node/IconPowerOnHint70.png",
                                               "com/untangle/gui/node/IconPowerOnHint80.png",
                                               "com/untangle/gui/node/IconPowerOnHint90.png",
                                               "com/untangle/gui/node/IconPowerOnHint100.png" };
                powerOnImageIcons = Util.getImageIcons( powerOnImagePaths );
            }
        }
        powerOnHintJLabel = new CycleJLabel(powerOnImageIcons, HELPER_POWER_ON_BLINK, true, true);

        // INIT GUI
        initComponents();
        jProgressBar.setVisible(false);

        // DYNAMICALLY LOAD DISPLAY
        try{
            Class mNodeDisplayJPanelClass = Class.forName(this.getClass().getPackage().getName()  +  ".MNodeDisplayJPanel",
                                                               true, Util.getClassLoader() );
            Constructor mNodeDisplayJPanelConstructor = mNodeDisplayJPanelClass.getConstructor(new Class[]{this.getClass()});
            mNodeDisplayJPanel = (MNodeDisplayJPanel) mNodeDisplayJPanelConstructor.newInstance(new Object[]{this});
        }
        catch(Exception e){
            mNodeDisplayJPanel = new MNodeDisplayJPanel(this);
            Util.handleExceptionNoRestart("Error adding display panel", e);
        }
        this.add(mNodeDisplayJPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(104, 5, 498, 90), 0);

        // DYNAMICALLY LOAD CONFIG
        try{
            Class mNodeControlsJPanelClass = Class.forName(this.getClass().getPackage().getName()  +  ".MNodeControlsJPanel",
                                                                true, Util.getClassLoader() );
            Constructor mNodeControlsJPanelConstructor = mNodeControlsJPanelClass.getConstructor(new Class[]{this.getClass()});
            mNodeControlsJPanel = (MNodeControlsJPanel) mNodeControlsJPanelConstructor.newInstance(new Object[]{this});
        }
        catch(Exception e){
            // SHOW A LITTLE MESSAGE TELLING THEM TO RESTART
            mNodeControlsJPanel = new MNodeControlsJPanel(this){public void generateGui(){}};
            JPanel warningJPanel = new JPanel(new BorderLayout());
            JLabel warningJLabel = new JLabel("<html><center><b>Warning:</b> Settings could not be loaded properly." +
                                              "<br>Please restart the Untangle Client to load settings properly.</center></html>");
            warningJLabel.setFont(new java.awt.Font("Arial", 0, 14));
            warningJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            warningJPanel.add(warningJLabel);
            mNodeControlsJPanel.getMTabbedPane().add("Warning", warningJPanel);
            Util.handleExceptionNoRestart("Error adding control panel", e);
        }

        // DYNAMICALLY LOAD ICONS
        String name = null;
        try{
            name = nodeDesc.getName();
            name = name.substring(0, name.indexOf('-'));
            descriptionIconJLabel.setIcon(new ImageIcon(mackageDesc.getDescIcon()));
        }
        catch(Exception e){ Util.handleExceptionNoRestart("Error adding icon: " + name , e); }

        organizationIconJLabel.setIcon(null);
        /*
          try{
          name = nodeDesc.getName();
          name = name.substring(0, name.indexOf('-'));
          organizationIconJLabel.setIcon(new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/untangle/node/"
          + name
          + "/gui/IconOrg42x42.png")));
          }
          catch(Exception e){ Util.handleExceptionNoRestart("Error adding icon: " + name , e); }

        */
        // SIZES
        if(maxDimension == null)
            maxDimension = new Dimension((int)this.getPreferredSize().getWidth(), (int)(this.getPreferredSize().getHeight()
                                                                                        + mNodeControlsJPanel.getPreferredSize().getHeight()));
        if(minDimension == null)
            minDimension = new Dimension((int)this.getPreferredSize().getWidth(), (int)(this.getPreferredSize().getHeight()));
        setPreferredSize(minDimension);
        setMinimumSize(minDimension);
        setMaximumSize(minDimension);

        // ADD CONFIG PANEL
        add(mNodeControlsJPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(46, 100, 596, 380));
        setMinimumSize(minDimension);
        setMaximumSize(maxDimension);
        //mNodeControlsJPanel.setVisible(false);
        setPreferredSize(minDimension);

        // CONFIG PANEL DROPDOWN TASK /////////
        controlsDropdownTask = new DropdownTask(this, mNodeControlsJPanel, controlsJToggleButton,
                                                minDimension, maxDimension,
                                                596, 380, 46, -280, 100);

        // SHUTDOWNABLE //
        addShutdownable("ShowControlsThread", showControlsThread);

        // SETUP NAME AND MESSAGE
        descriptionTextJLabel.setText( nodeDesc.getDisplayName() );
        String extraName = mackageDesc.getExtraName();
        if( extraName != null )
            messageTextJLabel.setText( extraName );
        else
            messageTextJLabel.setText("");

        // SETUP STATE
        mStateMachine = new MStateMachine(this);
        powerJToggleButton.addActionListener(mStateMachine);
        mNodeControlsJPanel.saveJButton().addActionListener(mStateMachine);
        mNodeControlsJPanel.reloadJButton().addActionListener(mStateMachine);
        mNodeControlsJPanel.removeJButton().addActionListener(mStateMachine);
    }

    class AAJLabel extends JLabel {
        public void paint(Graphics g){
            ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            super.paint(g);
        }
    }


    public boolean getDoneRefreshing(){ return mStateMachine.getDoneRefreshing(); }

    public void highlight(){ new FadeTask(effectsJPanel,true); }

    public void setProblemView(boolean doLater){ mStateMachine.setProblemView(doLater); }
    public void setRemovingView(boolean doLater){ mStateMachine.setRemovingView(doLater); }

    public void setPowerOnHintVisible(boolean isVisible){
        if( isVisible )
            powerOnHintJLabel.start();
        else{
            powerOnHintJLabel.stop();
        }
    }

    public void doShutdown(){
        for( Map.Entry<String,Shutdownable> shutdownableEntry : shutdownableMap.entrySet()){
            shutdownableEntry.getValue().doShutdown();
        }
        SwingUtilities.invokeLater( new Runnable(){ public void run(){
            mNodeControlsJPanel.doShutdown();
            mNodeDisplayJPanel.doShutdown();
            setControlsShowing(false);
        }});
    }

    public void doRefreshState(){
        mStateMachine.doRefreshState();
    }


    public JToggleButton powerJToggleButton(){ return powerJToggleButton; }
    BlinkJLabel stateJLabel(){ return (BlinkJLabel) stateJLabel; }
    JLabel messageTextJLabel(){ return messageTextJLabel; }
    private void initComponents() {//GEN-BEGIN:initComponents
        onOffbuttonGroup = new javax.swing.ButtonGroup();
        descriptionTextJLabel = new AAJLabel();
        nbPowerOnHintJLabel = powerOnHintJLabel;
        stateJLabel = (JLabel) new com.untangle.gui.node.BlinkJLabel();
        controlsJToggleButton = new javax.swing.JToggleButton();
        helpJButton = new javax.swing.JButton();
        descriptionIconJLabel = new javax.swing.JLabel();
        organizationIconJLabel = new javax.swing.JLabel();
        jProgressBar = new javax.swing.JProgressBar();
        messageTextJLabel = new AAJLabel();
        powerJToggleButton = new javax.swing.JToggleButton();
        effectsJPanel = new javax.swing.JPanel();
        backgroundJLabel = new javax.swing.JLabel();

        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        setMaximumSize(new java.awt.Dimension(688, 500));
        setMinimumSize(new java.awt.Dimension(688, 100));
        setOpaque(false);
        setPreferredSize(new java.awt.Dimension(688, 100));
        descriptionTextJLabel.setFont(new java.awt.Font("Arial", 0, 18));
        descriptionTextJLabel.setForeground(new java.awt.Color(124, 123, 123));
        descriptionTextJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        descriptionTextJLabel.setText("SuperNode");
        descriptionTextJLabel.setFocusable(false);
        descriptionTextJLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        descriptionTextJLabel.setIconTextGap(0);
        add(descriptionTextJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(106, 16, -1, 20));

        nbPowerOnHintJLabel.setFont(new java.awt.Font("Arial", 0, 18));
        nbPowerOnHintJLabel.setForeground(new java.awt.Color(255, 0, 0));
        nbPowerOnHintJLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        nbPowerOnHintJLabel.setFocusable(false);
        nbPowerOnHintJLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        nbPowerOnHintJLabel.setIconTextGap(0);
        add(nbPowerOnHintJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 80, -1, -1));

        stateJLabel.setToolTipText("<HTML> The <B>Status Indicator</B> shows the current operating condition of a particular software product.<BR>\n<font color=\"00FF00\"><b>Green</b></font> indicates that the product is \"on\" and operating normally.<BR>\n<font color=\"FF0000\"><b>Red</b></font> indicates that the product is \"on\", but that an abnormal condition has occurred.<BR>\n<font color=\"FFFF00\"><b>Yellow</b></font> indicates that the product is saving or refreshing settings.<BR>\n<b>Clear</b> indicates that the product is \"off\", and may be turned \"on\" by the user.\n</HTML>");
        add(stateJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(606, 20, 28, 28));

        controlsJToggleButton.setFont(new java.awt.Font("Default", 0, 12));
        controlsJToggleButton.setIcon(new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/untangle/gui/node/IconControlsClosed28x28.png")));
        controlsJToggleButton.setText("Show Settings");
        controlsJToggleButton.setAlignmentX(0.5F);
        controlsJToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        controlsJToggleButton.setIconTextGap(0);
        controlsJToggleButton.setMargin(new java.awt.Insets(0, 0, 1, 3));
        controlsJToggleButton.setOpaque(false);
        controlsJToggleButton.setSelectedIcon(new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/untangle/gui/node/IconControlsOpen28x28.png")));
        controlsJToggleButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    controlsJToggleButtonActionPerformed(evt);
                }
            });

        add(controlsJToggleButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(52, 60, 120, 25));

        helpJButton.setFont(new java.awt.Font("Dialog", 0, 12));
        helpJButton.setIcon(new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/untangle/gui/node/IconHelp28x28.png")));
        helpJButton.setText("Help");
        helpJButton.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        helpJButton.setIconTextGap(0);
        helpJButton.setMargin(new java.awt.Insets(0, 0, 0, 3));
        helpJButton.setMaximumSize(new java.awt.Dimension(76, 22));
        helpJButton.setMinimumSize(new java.awt.Dimension(76, 22));
        helpJButton.setPreferredSize(new java.awt.Dimension(76, 22));
        helpJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    helpJButtonActionPerformed(evt);
                }
            });

        add(helpJButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(178, 60, 68, 25));

        descriptionIconJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        descriptionIconJLabel.setIcon(new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/untangle/gui/node/IconDesc42x42.png")));
        descriptionIconJLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        add(descriptionIconJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(52, 6, 42, 42));

        organizationIconJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        organizationIconJLabel.setIcon(new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/untangle/gui/node/IconOrg42x42.png")));
        organizationIconJLabel.setAlignmentX(0.5F);
        organizationIconJLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        organizationIconJLabel.setIconTextGap(0);
        add(organizationIconJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(52, 51, 42, 42));

        jProgressBar.setFont(new java.awt.Font("Dialog", 0, 12));
        jProgressBar.setFocusable(false);
        jProgressBar.setMaximumSize(new java.awt.Dimension(232, 20));
        jProgressBar.setMinimumSize(new java.awt.Dimension(232, 20));
        jProgressBar.setPreferredSize(new java.awt.Dimension(232, 20));
        jProgressBar.setString("");
        jProgressBar.setStringPainted(true);
        add(jProgressBar, new org.netbeans.lib.awtextra.AbsoluteConstraints(106, 37, -1, -1));

        messageTextJLabel.setFont(new java.awt.Font("Arial", 1, 12));
        messageTextJLabel.setForeground(new java.awt.Color(68, 91, 255));
        messageTextJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        messageTextJLabel.setText("SuperNode");
        messageTextJLabel.setFocusable(false);
        messageTextJLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        messageTextJLabel.setIconTextGap(0);
        add(messageTextJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(106, 37, -1, 20));

        powerJToggleButton.setFont(new java.awt.Font("Default", 0, 12));
        powerJToggleButton.setIcon(new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/untangle/gui/node/IconPowerOffState28x28.png")));
        powerJToggleButton.setToolTipText("<HTML>\nThe <B>Power Button</B> allows you to turn a product \"on\" and \"off\".<br>\n\n</HTML>");
        powerJToggleButton.setAlignmentX(0.5F);
        powerJToggleButton.setBorderPainted(false);
        powerJToggleButton.setContentAreaFilled(false);
        powerJToggleButton.setFocusPainted(false);
        powerJToggleButton.setFocusable(false);
        powerJToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        powerJToggleButton.setIconTextGap(0);
        powerJToggleButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        powerJToggleButton.setMaximumSize(new java.awt.Dimension(28, 28));
        powerJToggleButton.setMinimumSize(new java.awt.Dimension(28, 28));
        powerJToggleButton.setPreferredSize(new java.awt.Dimension(28, 28));
        powerJToggleButton.setSelectedIcon(new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/untangle/gui/node/IconPowerOnState28x28.png")));
        add(powerJToggleButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(606, 54, 28, 28));

        effectsJPanel.setBackground(new Color(255,255,255,0));
        add(effectsJPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 688, 100));

        backgroundJLabel.setIcon(new javax.swing.ImageIcon( Util.getClassLoader().getResource("com/untangle/gui/node/NodeBackground688x100.png")));
        backgroundJLabel.setDisabledIcon(new javax.swing.ImageIcon(""));
        backgroundJLabel.setOpaque(true);
        add(backgroundJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 688, 100));

    }//GEN-END:initComponents

    private void helpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpJButtonActionPerformed
        try{
            String focus = Util.getSelectedTabTitle(mNodeControlsJPanel.getMTabbedPane()).toLowerCase().replace(" ", "_");
            String source = getNodeDesc().getDisplayName().toLowerCase().replace(" ", "_");
            URL newURL = new URL( "http://www.untangle.com/docs/get.php?"
                                  + "version=" + Version.getVersion()
                                  + "&source=" + source
                                  + "&focus=" + focus);
            ((BasicService) ServiceManager.lookup("javax.jnlp.BasicService")).showDocument(newURL);
        }
        catch(Exception f){
            Util.handleExceptionNoRestart("Error showing help for " + nodeDesc.getDisplayName(), f);
        }
    }//GEN-LAST:event_helpJButtonActionPerformed

    private void controlsJToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_controlsJToggleButtonActionPerformed
        handleControlsJButton(controlsJToggleButton.isSelected());
    }//GEN-LAST:event_controlsJToggleButtonActionPerformed


    // SHOW/HIDE CONTROLS ////////////
    public void setControlsShowing(boolean showingBoolean){
        handleControlsJButton(showingBoolean);
    }
    public boolean getControlsShowing(){ return controlsJToggleButton.isSelected(); }
    public JToggleButton getControlsJToggleButton(){ return controlsJToggleButton; };

    private void handleControlsJButton(boolean showSettings){
        showingSettings = showSettings;
        controlsJToggleButton.setEnabled(false);
        synchronized(showControlsThread){
            showControlsThread.notify();
        }
    }


    private Exception generateGuiException;
    private class ShowControlsThread extends Thread implements Shutdownable {
        private volatile boolean stop = false;
        public ShowControlsThread(){
            super("MVCLIENT-ShowControlsThread: " + MNodeJPanel.this.nodeDesc.getDisplayName());
            setDaemon(true);
            start();
        }
        public synchronized void doShutdown(){
            stop = true;
            notify();
        }
        public void run(){
            try{
                while(true){
                    synchronized(this){
                        if(stop)
                            break;
                        wait();
                        if(stop)
                            break;
                        if(MNodeJPanel.this.showingSettings && !MNodeJPanel.this.controlsLoaded){
                            SwingUtilities.invokeLater( new Runnable(){ public void run(){
                                jProgressBar.setVisible(true);
                                jProgressBar.setIndeterminate(true);
                                jProgressBar.setString("Loading Settings...");
                            }});
                            try{
                                // LOAD SETTINGS //
                                mNodeControlsJPanel.refreshAll();
                                // GENERATE GUI //
                                generateGuiException = null;
                                SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
                                    try{ mNodeControlsJPanel.generateGui(); }
                                    catch(Exception f){ generateGuiException = f; }
                                }});
                                if( generateGuiException != null )
                                    throw generateGuiException;
                                // POPULATE GUI //
                                mNodeControlsJPanel.populateAll();
                            }
                            catch(Exception e){
                                try{ Util.handleExceptionWithRestart("Error showing settings", e); }
                                catch(Exception f){
                                    Util.handleExceptionNoRestart("Error showing settings", f);
                                    RefreshFailureDialog.factory( (Window) mNodeControlsJPanel.getContentJPanel().getTopLevelAncestor(),
                                                                  nodeDesc.getDisplayName());
                                }
                            }
                            MNodeJPanel.this.controlsLoaded = true;
                            SwingUtilities.invokeLater( new Runnable(){ public void run(){
                                jProgressBar.setIndeterminate(false);
                                jProgressBar.setString("Settings Loaded");
                                jProgressBar.setValue(100);
                            }});
                        }
                        SwingUtilities.invokeLater( new Runnable(){ public void run(){
                            MNodeJPanel.this.controlsDropdownTask.start(MNodeJPanel.this.showingSettings, jProgressBar);
                        }});
                    }
                }
            }
            catch(Exception e){
                Util.handleExceptionNoRestart("Error waiting", e);
            }
        }
    }

    public void focus(){
        Rectangle newBounds = this.getBounds();
        newBounds.width = this.getPreferredSize().width;
        newBounds.height = this.getPreferredSize().height;
        //Util.getMPipelineJPanel().focusMNodeJPanel(newBounds);
    }
    //////////////////////////////



    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel backgroundJLabel;
    protected javax.swing.JToggleButton controlsJToggleButton;
    protected javax.swing.JLabel descriptionIconJLabel;
    protected javax.swing.JLabel descriptionTextJLabel;
    private javax.swing.JPanel effectsJPanel;
    private javax.swing.JButton helpJButton;
    private javax.swing.JProgressBar jProgressBar;
    protected javax.swing.JLabel messageTextJLabel;
    protected javax.swing.JLabel nbPowerOnHintJLabel;
    private javax.swing.ButtonGroup onOffbuttonGroup;
    protected javax.swing.JLabel organizationIconJLabel;
    protected javax.swing.JToggleButton powerJToggleButton;
    private javax.swing.JLabel stateJLabel;
    // End of variables declaration//GEN-END:variables

}
