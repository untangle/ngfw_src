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

package com.untangle.gui.main;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.*;
import java.lang.reflect.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.*;
import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;
import javax.swing.*;
import javax.swing.border.*;

import org.apache.log4j.Logger;

import com.untangle.gui.configuration.*;
import com.untangle.gui.node.*;
import com.untangle.gui.pipeline.*;
import com.untangle.gui.store.*;
import com.untangle.gui.upgrade.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.separator.Separator;
import com.untangle.uvm.*;
import com.untangle.uvm.client.*;
import com.untangle.uvm.node.*;
import com.untangle.uvm.policy.*;
import com.untangle.uvm.security.*;
import com.untangle.uvm.toolbox.*;

public class PolicyStateMachine implements ActionListener, Shutdownable {

    private final Logger logger = Logger.getLogger(getClass());

    // FOR REMOVING TRIALS FROM STORE WHEN ITEM IS PURCHASED
    // UVM DATA MODELS (USED ONLY DURING INIT) //////
    private List<Tid>                       utilTidList;
    private Map<String,Object>              utilNameMap;
    private Map<Policy,List<Tid>>           policyTidMap;
    private Map<Policy,Map<String,Object>>  policyNameMap;
    private List<Tid>                       nonPolicyTidList;
    private Map<String,Object>              nonPolicyNameMap;
    // GUI DATA MODELS /////////
    private Map<ButtonKey,MNodeJButton>             storeMap;
    private Map<ButtonKey,MNodeJButton>             utilToolboxMap;
    private Map<ButtonKey,MNodeJPanel>              utilRackMap;
    private Map<Policy,Map<ButtonKey,MNodeJButton>> policyToolboxMap;
    private Map<Policy,Map<ButtonKey,MNodeJPanel>>  policyRackMap;
    private Map<ButtonKey,MNodeJButton>             coreToolboxMap;
    private Map<ButtonKey,MNodeJPanel>              coreRackMap;
    // GUI VIEW MODELS //////////
    private JScrollPane        toolboxJScrollPane;
    private JScrollPane        rackJScrollPane;
    private JPanel             rackViewJPanel;
    private JPanel             storeJPanel;
    private JPanel             utilToolboxSocketJPanel;
    private JPanel             utilToolboxJPanel;
    private JPanel             utilRackJPanel;
    private JPanel             policyToolboxSocketJPanel;
    private Map<Policy,JPanel> policyToolboxJPanelMap;
    private Map<Policy,JPanel> policyRackJPanelMap;
    private JPanel             coreToolboxSocketJPanel;
    private JPanel             coreToolboxJPanel;
    private JPanel             coreRackJPanel;
    // MISC REFERENCES ////////
    private JButton             storeWizardJButton;
    private JTabbedPane         actionJTabbedPane;
    private JComboBox           viewSelector;
    private Policy              selectedPolicy;
    private JPanel              selectedPolicyRackJPanel;
    private int                 lastToolboxScrollPosition = -1;
    private Map<Policy,Integer> lastRackScrollPosition;
    private volatile static int applianceLoadProgress;
    private MessageClientThread messageClientThread;
    // THREAD QUEUES & THREADS /////////
    BlockingQueue<PurchaseWrapper>   purchaseBlockingQueue;
    StoreModelThread                 storeModelThread;
    MoveFromStoreToToolboxThread     moveFromStoreToToolboxThread;
    // CONSTANTS /////////////
    private GridBagConstraints buttonGridBagConstraints;
    private GridBagConstraints storeProgressGridBagConstraints;
    private GridBagConstraints storeSettingsGridBagConstraints;
    private GridBagConstraints storeSpacerGridBagConstraints;
    private GridBagConstraints applianceGridBagConstraints;
    private GridBagConstraints utilGridBagConstraints;
    private GridBagConstraints utilSeparatorGridBagConstraints;
    private GridBagConstraints policyGridBagConstraints;
    private GridBagConstraints policySeparatorGridBagConstraints;
    private GridBagConstraints coreGridBagConstraints;
    private GridBagConstraints coreSeparatorGridBagConstraints;
    private GridBagConstraints storeWizardGridBagConstraints;
    private Separator utilSeparator;
    private Separator policySeparator;
    private Separator coreSeparator;
    private static final String POLICY_MANAGER_SEPARATOR = "____________";
    private static final String POLICY_MANAGER_OPTION = "Show Policy Manager";
    private static final int CONCURRENT_LOAD_MAX = 2;
    private static Semaphore loadSemaphore;
    // STORE DELAYS //////////////////
    private static final long STORE_UPDATE_CHECK_SLEEP = 2l*60l*60l*1000l;
    // DOWNLOAD DELAYS ///////////////
    private static final int DOWNLOAD_CHECK_SLEEP_MILLIS = 500;
    private static final int DOWNLOAD_FINAL_PAUSE_MILLIS = 1000;
    // INSTALL DELAYS ////////////////
    private static final int INSTALL_CHECK_SLEEP_MILLIS = 500;
    private static final int INSTALL_FINAL_PAUSE_MILLIS = 1000;
    private static final int INSTALL_CHECK_TIMEOUT_MILLIS = 3*60*1000; // (3 minutes)
    private static final int DEPLOY_FINAL_PAUSE_MILLIS = 1000;

    public PolicyStateMachine(JTabbedPane actionJTabbedPane, JPanel rackViewJPanel, JScrollPane toolboxJScrollPane,
                              JPanel utilToolboxSocketJPanel, JPanel policyToolboxSocketJPanel, JPanel coreToolboxSocketJPanel,
                              JPanel storeJPanel, JScrollPane rackJScrollPane) {
        // UVM DATA MODELS
        utilTidList = new Vector<Tid>();
        utilNameMap = new HashMap<String,Object>();
        policyTidMap = new LinkedHashMap<Policy,List<Tid>>(); // Linked so view selector order is consistent (initially)
        policyNameMap = new HashMap<Policy,Map<String,Object>>();
        nonPolicyTidList = new Vector<Tid>();
        nonPolicyNameMap = new HashMap<String,Object>();
        // GUI DATA MODELS
        storeMap = new TreeMap<ButtonKey,MNodeJButton>();
        utilToolboxMap = new TreeMap<ButtonKey,MNodeJButton>();
        utilRackMap = new TreeMap<ButtonKey,MNodeJPanel>();
        policyToolboxMap = new HashMap<Policy,Map<ButtonKey,MNodeJButton>>();
        policyRackMap = new HashMap<Policy,Map<ButtonKey,MNodeJPanel>>();
        coreToolboxMap = new TreeMap<ButtonKey,MNodeJButton>();
        coreRackMap = new TreeMap<ButtonKey,MNodeJPanel>();
        // GUI VIEW MODELS
        this.rackViewJPanel = rackViewJPanel;
        this.toolboxJScrollPane = toolboxJScrollPane;
        this.utilToolboxSocketJPanel = utilToolboxSocketJPanel;
        this.policyToolboxSocketJPanel = policyToolboxSocketJPanel;
        this.coreToolboxSocketJPanel = coreToolboxSocketJPanel;
        this.storeJPanel = storeJPanel;
        this.rackJScrollPane = rackJScrollPane;
        utilToolboxJPanel = new JPanel();
        utilRackJPanel = new JPanel();
        policyToolboxJPanelMap = new HashMap<Policy,JPanel>();
        policyRackJPanelMap = new HashMap<Policy,JPanel>();
        coreToolboxJPanel = new JPanel();

        storeWizardJButton = new JButton();
        storeWizardJButton.setFont(new java.awt.Font("Dialog", 0, 12));
        storeWizardJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconWizard_32x32.png")));
        storeWizardJButton.setText("Which applications should I use?");
        storeWizardJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        storeWizardJButton.setOpaque(false);
        storeWizardJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    storeWizardJButtonActionPerformed(evt);
                }
            });
        storeWizardGridBagConstraints = new GridBagConstraints(0, 10, 1, 1, 0d, 0d,
                                                               GridBagConstraints.CENTER, GridBagConstraints.NONE,
                                                               new Insets(0,0,100,0), 0, 0);

        coreRackJPanel = new JPanel();
        //rackViewJPanel.add(storeWizardJButton, storeWizardGridBagConstraints);

        utilToolboxJPanel.setOpaque(false);
        utilRackJPanel.setOpaque(false);
        coreToolboxJPanel.setOpaque(false);
        coreRackJPanel.setOpaque(false);
        utilToolboxJPanel.setLayout(new GridBagLayout());
        utilRackJPanel.setLayout(new GridBagLayout());
        coreToolboxJPanel.setLayout(new GridBagLayout());
        coreRackJPanel.setLayout(new GridBagLayout());
        // SEPARATORS
        utilSeparator = new Separator(false);
        utilSeparator.setForegroundText("Services & Utilities");
        policySeparator = new Separator(true);
        policySeparator.setForegroundText(" ");
        coreSeparator = new Separator(false);
        coreSeparator.setForegroundText("Services");
        // MISC REFERENCES
        this.actionJTabbedPane = actionJTabbedPane;
        this.viewSelector = policySeparator.getJComboBox();
        viewSelector.addActionListener(this);
        lastRackScrollPosition = new HashMap<Policy,Integer>();
        // THREAD QUEUES & THREADS /////////
        purchaseBlockingQueue = new ArrayBlockingQueue<PurchaseWrapper>(100);
        moveFromStoreToToolboxThread = new MoveFromStoreToToolboxThread();
        actionJTabbedPane.setSelectedIndex(0);
        storeModelThread = new StoreModelThread();
        messageClientThread = new MessageClientThread(Util.getUvmContext(),new StoreMessageVisitor());
        // CONSTANTS
        buttonGridBagConstraints = new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1d, 0d,
                                                          GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                                                          new Insets(0,1,3,3), 0, 0);
        storeProgressGridBagConstraints = new GridBagConstraints(0, 0, 1, 1, .5d, 0d,
                                                                 GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                                                                 new Insets(0,4,0,4), 0, 0);
        storeSettingsGridBagConstraints = new GridBagConstraints(0, 1, 1, 1, .5d, 0d,
                                                                 GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                                                                 new Insets(15,0,0,0), 0, 0);
        storeSpacerGridBagConstraints = new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0d, 1d,
                                                               GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                               new Insets(0,0,0,0), 0, 0);
        applianceGridBagConstraints = new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0d, 0d,
                                                             GridBagConstraints.CENTER, GridBagConstraints.NONE,
                                                             new Insets(1,0,0,0), 0, 0);


        utilSeparatorGridBagConstraints = new GridBagConstraints(0, 5, 1, 1, 0d, 0d,
                                                                 GridBagConstraints.NORTH, GridBagConstraints.NONE,
                                                                 new Insets(1,0,101,12), 0, 0);
        utilGridBagConstraints = new GridBagConstraints(0, 5, 1, 1, 0d, 0d,
                                                        GridBagConstraints.SOUTH, GridBagConstraints.NONE,
                                                        new Insets(51,0,0,12), 0, 0);
        policySeparatorGridBagConstraints = new GridBagConstraints(0, 3, 1, 1, 0d, 0d,
                                                                   GridBagConstraints.NORTH, GridBagConstraints.NONE,
                                                                   new Insets(1,0,101,12), 0, 0);
        policyGridBagConstraints = new GridBagConstraints(0, 3, 1, 1, 0d, 0d,
                                                          GridBagConstraints.SOUTH, GridBagConstraints.NONE,
                                                          new Insets(51,0,0,12), 0, 0);
        coreSeparatorGridBagConstraints = new GridBagConstraints(0, 4, 1, 1, 0d, 0d,
                                                                 GridBagConstraints.NORTH, GridBagConstraints.NONE,
                                                                 new Insets(1,0,101,12), 0, 0);
        coreGridBagConstraints = new GridBagConstraints(0, 4, 1, 1, 0d, 0d,
                                                        GridBagConstraints.SOUTH, GridBagConstraints.NONE,
                                                        new Insets(51,0,0,12), 0, 0);


        loadSemaphore = new Semaphore(CONCURRENT_LOAD_MAX);
        try{
            // LET THE FUN BEGIN
            initUvmModel();
            // the order of the following three is based on their output to the progress bar, thats all
            initRackModel(Util.getStatusJProgressBar());
            initToolboxModel(Util.getStatusJProgressBar());
            initViewSelector();
            // CACHING OF CASING CLASSES SO THE PROTOCOL SETTINGS DIALOG LOADS FASTER
            loadAllCasings(false);
        }
        catch(Exception e){
            try{ Util.handleExceptionWithRestart("Error instantiating policy model", e); }
            catch(Exception f){ Util.handleExceptionNoRestart("Error instantiating policy model", f); }
        }

        Util.setPolicyStateMachine(this);
        Util.addShutdownable("MessageClientThread", messageClientThread);
        Util.addShutdownable("StoreModelThread", storeModelThread);
        Util.addShutdownable("MoveFromStoreToToolboxThread", moveFromStoreToToolboxThread);
    }

    public void doShutdown(){
        for ( MNodeJPanel mNodeJPanel : getAllNodePanels()) {
            mNodeJPanel.doShutdown();
        }
        Util.printMessage("PolicyStateMachine Stopped");
    }

    // HANDLERS ///////////////////////////////////////////
    ///////////////////////////////////////////////////////
    private void storeWizardJButtonActionPerformed(java.awt.event.ActionEvent evt) {
        try{
            if( Util.getIsDemo() )
                return;
            if( Util.mustCheckUpgrades() ){
                new StoreCheckJDialog( Util.getMMainJFrame() );
            }
            if( Util.getUpgradeCount() != 0 )
                return;
            String authNonce = Util.getRemoteAdminManager().generateAuthNonce();
            URL newURL = new URL( Util.getServerCodeBase(), "../library/index.php?option=com_wizard&Itemid=92&" + authNonce);
            ((BasicService) ServiceManager.lookup("javax.jnlp.BasicService")).showDocument(newURL);
        }
        catch(Exception f){
            Util.handleExceptionNoRestart("Error showing store wizard.", f);
        }
    }

    public void actionPerformed(ActionEvent actionEvent){
        if( actionEvent.getSource().equals(viewSelector) ){
            handleViewSelector();
        }
    }
    private void handleViewSelector(){
        Policy newPolicy;
        if( viewSelector.getSelectedItem() instanceof String ){
            if( viewSelector.getSelectedItem().equals(POLICY_MANAGER_OPTION) ){
                PolicyJDialog policyJDialog = new PolicyJDialog(Util.getMMainJFrame());
                policyJDialog.setVisible(true);
                viewSelector.setSelectedItem(selectedPolicy);
                PolicyUpdateProgressJDialog pupJDialog = new PolicyUpdateProgressJDialog(Util.getMMainJFrame());
                pupJDialog.setVisible(true);
                List<Policy> policies = pupJDialog.getPolicies();
                if(policies==null)
                    return;
                updatePolicyRacks(policies);
            }
            else
                viewSelector.setSelectedItem(selectedPolicy);
            return;
        }
        else{
            newPolicy = (Policy) viewSelector.getSelectedItem();
            if( newPolicy.equals(selectedPolicy) )
                return;
        }
        // TOOLBOX VIEW AND SCROLL POSITION
        JPanel newPolicyToolboxJPanel = policyToolboxJPanelMap.get(newPolicy);
        int currentToolboxScrollPosition = toolboxJScrollPane.getVerticalScrollBar().getValue();
        policyToolboxSocketJPanel.removeAll();
        policyToolboxSocketJPanel.add( newPolicyToolboxJPanel );
        newPolicyToolboxJPanel.revalidate();
        if( utilToolboxSocketJPanel.getComponentCount() == 0 ){
            utilToolboxSocketJPanel.add( utilToolboxJPanel );
            utilToolboxJPanel.revalidate();
        }
        if( coreToolboxSocketJPanel.getComponentCount() == 0 ){
            coreToolboxSocketJPanel.add( coreToolboxJPanel );
            coreToolboxJPanel.revalidate();
        }
        toolboxJScrollPane.repaint();
        if( lastToolboxScrollPosition >= 0 )
            toolboxJScrollPane.getVerticalScrollBar().setValue( currentToolboxScrollPosition );
        lastToolboxScrollPosition = currentToolboxScrollPosition;
        // RACK VIEW AND SCROLL POSITION
        lastRackScrollPosition.put(selectedPolicy, rackJScrollPane.getVerticalScrollBar().getValue());
        JPanel newPolicyRackJPanel = policyRackJPanelMap.get(newPolicy);
        // DETERMINE IF THERE ARE ANY SECURITY APPLIANCES
        boolean allEmpty = true;
        for( Policy policy : policyRackMap.keySet() ){
            if(policyRackMap.get(policy).size()>0){
                allEmpty = false;
                break;
            }
        }
        if( selectedPolicyRackJPanel != null ){ // not the first rack viewed
            rackViewJPanel.remove( selectedPolicyRackJPanel );
            if( !allEmpty )
                rackViewJPanel.add( newPolicyRackJPanel, policyGridBagConstraints );
        }
        else{ // the first rack viewed
            // ADD CORE AND SEPARATOR
            if( !utilRackMap.isEmpty() ){
                rackViewJPanel.add( utilRackJPanel, utilGridBagConstraints );
                rackViewJPanel.add( utilSeparator, utilSeparatorGridBagConstraints );
            }
            // ADD SECURITY AND SEPARATOR
            if( !allEmpty ){
                rackViewJPanel.add( policySeparator, policySeparatorGridBagConstraints );
                rackViewJPanel.add( newPolicyRackJPanel, policyGridBagConstraints );
            }
            // ADD CORE AND SEPARATOR
            if( !coreRackMap.isEmpty() ){
                rackViewJPanel.add( coreRackJPanel, coreGridBagConstraints );
                rackViewJPanel.add( coreSeparator, coreSeparatorGridBagConstraints );
            }
            coreRackJPanel.revalidate();
        }
        //rackSeparator.setForegroundText( newPolicy.getName() );
        newPolicyRackJPanel.revalidate();
        rackViewJPanel.revalidate();
        rackViewJPanel.repaint();
        rackJScrollPane.getVerticalScrollBar().setValue( lastRackScrollPosition.get(newPolicy) );
        selectedPolicyRackJPanel = newPolicyRackJPanel;
        selectedPolicy = newPolicy;
    }
    ///////////////////////////////////////////////////////
    // HANDLERS ///////////////////////////////////////////


    // POLICY UPDATING ////////////////////////////////////
    ///////////////////////////////////////////////////////
    private void updatePolicyRacks(List<Policy> policies) {
        // BUILD A GUI MODEL AND UVM MODEL
        Map<Policy,Object> currentPolicyRacks = new HashMap<Policy,Object>();
        Map<Policy,Object> newPolicyRacks = new LinkedHashMap<Policy,Object>();
        for(int i=0; i<((DefaultComboBoxModel)viewSelector.getModel()).getSize()-2; i++) // -2 for the last 2 policy manager options
            currentPolicyRacks.put( (Policy) ((DefaultComboBoxModel)viewSelector.getModel()).getElementAt(i), null );
        for( Policy policy : policies )
            newPolicyRacks.put( policy, null );
        // FIND THE DIFFERENCES
        Vector<Policy> addedPolicyVector = new Vector<Policy>();
        Vector<Policy> removedPolicyVector = new Vector<Policy>();
        for( Policy newPolicy : newPolicyRacks.keySet() )
            if( !currentPolicyRacks.containsKey(newPolicy) )
                addedPolicyVector.add( newPolicy );
        for( Policy currentPolicy : currentPolicyRacks.keySet() )
            if( !newPolicyRacks.containsKey(currentPolicy) )
                removedPolicyVector.add( currentPolicy );
        // UPDATE VIEW SELECTOR
        Policy activePolicy = (Policy) viewSelector.getSelectedItem();
        DefaultComboBoxModel newModel = new DefaultComboBoxModel();
        for( Policy newPolicy : newPolicyRacks.keySet() ){
            newModel.addElement(newPolicy);
            if( activePolicy.equals(newPolicy) ){
                newModel.setSelectedItem(newPolicy);
                selectedPolicy = newPolicy;
            }
        }
        newModel.addElement(POLICY_MANAGER_SEPARATOR);
        newModel.addElement(POLICY_MANAGER_OPTION);
        if( newModel.getSelectedItem() == null ){
            newModel.setSelectedItem( newModel.getElementAt(0) );
            selectedPolicy = (Policy) newModel.getElementAt(0);
        }
        viewSelector.setModel(newModel);
        // ADD THE NEW AND REMOVE THE OLD
        addedPolicyRacks(addedPolicyVector);
        removedPolicyRacks(removedPolicyVector);
        // UPDATE VIEW
        handleViewSelector();
    }
    private void addedPolicyRacks(final List<Policy> policies){
        Policy firstPolicy = policyToolboxMap.keySet().iterator().next();
        for( Policy policy : policies ){
            // ADD TO GUI DATA MODEL
            policyToolboxMap.put(policy, new TreeMap<ButtonKey,MNodeJButton>());
            policyRackMap.put(policy,new TreeMap<ButtonKey,MNodeJPanel>());
            // ADD TO GUI VIEW MODEL
            JPanel toolboxJPanel = new JPanel();
            toolboxJPanel.setLayout(new GridBagLayout());
            toolboxJPanel.setOpaque(false);
            policyToolboxJPanelMap.put(policy, toolboxJPanel);
            JPanel rackJPanel = new JPanel();
            rackJPanel.setLayout(new GridBagLayout());
            rackJPanel.setOpaque(false);
            policyRackJPanelMap.put(policy, rackJPanel);
            // ADD TO SCROLL POSITION
            lastRackScrollPosition.put(policy,0);
            // POPULATE THE TOOLBOX
            for( Map.Entry<ButtonKey,MNodeJButton> firstPolicyEntry : policyToolboxMap.get(firstPolicy).entrySet() )
                addToToolbox(policy,firstPolicyEntry.getValue().getMackageDesc(),false,false);
            revalidateToolboxes();
        }
    }
    private void removedPolicyRacks(final List<Policy> policies){
        for( Policy policy : policies ){
            // SHUTDOWN ALL APPLIANCES
            /* We dont need to do this anymore since non-empty policies cannot be deleted */
            //for( MNodeJPanel mNodeJPanel : policyRackMap.get(policy).values() ){
            //mNodeJPanel.doShutdown();
            //}
            // REMOVE FROM GUI DATA MODEL
            policyRackMap.get(policy).clear();
            policyRackMap.remove(policy);
            // REMOVE FROM GUI VIEW MODEL
            policyRackJPanelMap.get(policy).removeAll();
            policyRackJPanelMap.remove(policy);
            // REMOVE FROM SCROLL POSITION
            lastRackScrollPosition.remove(policy);
        }
    }
    /////////////////////////////////////////
    // POLICY UPDATING //////////////////////

    // TOOLBOX / STORE OPERATIONS //////////
    ////////////////////////////////////////
    private class MoveFromToolboxToRackThread extends Thread {
        private Policy policy;
        private MNodeJButton mNodeJButton;
        public MoveFromToolboxToRackThread(final Policy policy, final MNodeJButton mNodeJButton){
            setDaemon(true);
            this.policy = policy;
            this.mNodeJButton = mNodeJButton;
            setContextClassLoader( Util.getClassLoader() );
            setName("MVCLIENT-MoveFromToolboxToRackThread: " + mNodeJButton.getDisplayName() );
            mNodeJButton.setDeployingView();
            focusInToolbox(mNodeJButton, false);
            start();
        }
        public void run(){
            try{
                // INSTANTIATE IN UVM
                Tid tid = Util.getNodeManager().instantiate(mNodeJButton.getName(),policy);
                // CREATE APPLIANCE
                NodeContext nodeContext = Util.getNodeManager().nodeContext( tid );
                NodeDesc nodeDesc = nodeContext.getNodeDesc();
                MNodeJPanel mNodeJPanel = MNodeJPanel.instantiate(nodeContext, nodeDesc, policy);
                // DEPLOY APPLIANCE TO CURRENT POLICY RACK (OR CORE RACK)
                addToRack(policy, mNodeJPanel,true);
                // FOCUS AND HIGHLIGHT IN CURRENT RACK
                focusInRack(mNodeJPanel);
                // AUTO ON
                if( nodeDesc.getName().endsWith("router") || nodeDesc.getName().endsWith("openvpn") ){
                    mNodeJPanel.setPowerOnHintVisible(true);
                    MOneButtonJDialog.factory( Util.getMMainJFrame(), "", nodeDesc.getDisplayName()
                                               + " can not be automatically turned on."
                                               + "<br>Please configure its settings first.",
                                               nodeDesc.getDisplayName() + " Warning", "");
                }
                else{
                    while(!mNodeJPanel.getDoneRefreshing())
                        sleep(100L);
                    final MNodeJPanel target = mNodeJPanel;
                    SwingUtilities.invokeLater( new Runnable(){ public void run(){
                        target.powerJToggleButton().doClick();
                    }});
                }
            }
            catch(Exception e){
                e.printStackTrace();
                try{ Util.handleExceptionWithRestart("Error moving from toolbox to rack", e); }
                catch(Exception f){
                    Util.handleExceptionNoRestart("Error moving from toolbox to rack", f);
                    mNodeJButton.setFailedDeployView();
                    MOneButtonJDialog.factory( Util.getMMainJFrame(), "",
                                               "A problem occurred while installing to the rack:<br>"
                                               + mNodeJButton.getDisplayName()
                                               + "<br>Please contact Untangle Support.",
                                               mNodeJButton.getDisplayName() + " Warning", "");
                    return;
                }
            }
            mNodeJButton.setDeployedView();
            // UPDATE PROTOCOL SETTINGS CACHE
            loadAllCasings(false);
        }
    }
    public void moveFromRackToToolbox(final Policy policy, final MNodeJPanel mNodeJPanel){
        new MoveFromRackToToolboxThread(policy,mNodeJPanel);
    }
    private class MoveFromRackToToolboxThread extends Thread{
        private Policy policy;
        private MNodeJPanel mNodeJPanel;
        private MNodeJButton mNodeJButton;
        private ButtonKey buttonKey;
        public MoveFromRackToToolboxThread(final Policy policy, final MNodeJPanel mNodeJPanel){
            setDaemon(true);
            this.policy = policy;
            this.mNodeJPanel = mNodeJPanel;
            this.buttonKey = new ButtonKey(mNodeJPanel);
            if(mNodeJPanel.getMackageDesc().isService() || mNodeJPanel.getMackageDesc().isUtil())
                this.mNodeJButton = utilToolboxMap.get(buttonKey);
            else if(mNodeJPanel.getMackageDesc().isSecurity())
                this.mNodeJButton = policyToolboxMap.get(policy).get(buttonKey);
            else if(mNodeJPanel.getMackageDesc().isCore() )
                this.mNodeJButton = coreToolboxMap.get(buttonKey);
            else
                this.mNodeJButton = null;
            setContextClassLoader( Util.getClassLoader() );
            setName("MVCLIENT-MoveFromRackToToolboxThread: " + mNodeJPanel.getMackageDesc().getDisplayName() );
            mNodeJPanel.setRemovingView(false);
            start();
        }
        public void run(){
            try{
                // DESTROY IN UVM
                Util.getNodeManager().destroy(mNodeJPanel.getTid());
                // REMOVE APPLIANCE FROM THE CURRENT POLICY RACK
                mNodeJPanel.doShutdown();
                removeFromRack(policy, mNodeJPanel);
            }
            catch(Exception e){
                try{ Util.handleExceptionWithRestart("Error moving from rack to toolbox", e); }
                catch(Exception f){
                    Util.handleExceptionNoRestart("Error moving from rack to toolbox", f);
                    mNodeJPanel.setProblemView(true);
                    mNodeJButton.setFailedRemoveFromRackView();
                    MOneButtonJDialog.factory(Util.getMMainJFrame(), "",
                                              "A problem occurred while removing from the rack:<br>"
                                              + mNodeJPanel.getMackageDesc().getDisplayName()
                                              + "<br>Please contact Untangle Support.",
                                              mNodeJPanel.getMackageDesc().getDisplayName() + " Warning", "");
                    return;
                }
            }
            // VIEW: DEPLOYABLE
            MNodeJButton targetMNodeJButton;
            if( mNodeJPanel.getMackageDesc().isUtil() || mNodeJPanel.getMackageDesc().isService() )
                targetMNodeJButton = utilToolboxMap.get(buttonKey);
            else if( mNodeJPanel.getMackageDesc().isSecurity() )
                targetMNodeJButton = policyToolboxMap.get(policy).get(buttonKey);
            else if( mNodeJButton.getMackageDesc().isCore() )
                targetMNodeJButton = coreToolboxMap.get(buttonKey);
            else
                targetMNodeJButton = null;
            targetMNodeJButton.setDeployableView();
            focusInToolbox(targetMNodeJButton, true);
        }
    }

    private class MoveFromToolboxToStoreThread extends Thread{
        private MNodeJButton mNodeJButton;
        private Vector<MNodeJButton> buttonVector;
        public MoveFromToolboxToStoreThread(final MNodeJButton mNodeJButton){
            setDaemon(true);
            this.mNodeJButton = mNodeJButton;
            ButtonKey buttonKey = new ButtonKey(mNodeJButton);
            buttonVector = new Vector<MNodeJButton>();
            setContextClassLoader( Util.getClassLoader() );
            setName("MVCLIENT-MoveFromToolboxToStoreThread: " + mNodeJButton.getDisplayName() );
            // DECIDE IF WE CAN REMOVE
            if( mNodeJButton.getMackageDesc().isUtil() || mNodeJButton.getMackageDesc().isService() ){
                buttonVector.add(mNodeJButton);
            }
            else if(mNodeJButton.getMackageDesc().isSecurity()){
                for( Map.Entry<Policy,Map<ButtonKey,MNodeJButton>> policyToolboxMapEntry : policyToolboxMap.entrySet() ){
                    Map<ButtonKey,MNodeJButton> toolboxMap = policyToolboxMapEntry.getValue();
                    if( toolboxMap.containsKey(buttonKey) && toolboxMap.get(buttonKey).isEnabled() ){
                        buttonVector.add( toolboxMap.get(buttonKey) );
                    }
                    else{
                        MOneButtonJDialog.factory(Util.getMMainJFrame(), "",
                                                  mNodeJButton.getDisplayName()
                                                  + " cannot be removed from the toolbox because it is being"
                                                  + " used by the following policy rack:<br><b>"
                                                  + policyToolboxMapEntry.getKey().getName()
                                                  + "</b><br><br>You must remove the product from all policy racks first.",
                                                  mNodeJButton.getDisplayName() + " Warning", "");
                        return;
                    }
                }
            }
            else if(mNodeJButton.getMackageDesc().isCore()){
                buttonVector.add(mNodeJButton);
            }
            for( MNodeJButton button : buttonVector )
                button.setRemovingFromToolboxView();
            start();
        }
        public void run(){
            try{
                // UNINSTALL IN UVM
                Util.getRemoteToolboxManager().uninstall(mNodeJButton.getName());
                // REMOVE FROM TOOLBOX
                removeFromToolbox(mNodeJButton.getMackageDesc());
                // UPDATE STORE MODEL
                updateStoreModel();
            }
            catch(Exception e){
                try{ Util.handleExceptionWithRestart("Error moving from toolbox to store", e); }
                catch(Exception f){
                    Util.handleExceptionNoRestart("Error moving from toolbox to store", f);
                    mNodeJButton.setFailedRemoveFromToolboxView();
                    MOneButtonJDialog.factory(Util.getMMainJFrame(), "",
                                              "A problem occurred while removing from the toolbox:<br>"
                                              + mNodeJButton.getDisplayName()
                                              + "<br>Please contact Untangle Support.",
                                              mNodeJButton.getDisplayName() + " Warning", "");
                    return;
                }
            }
        }
    }


    private Object storeLock = new Object();

    private class StoreMessageVisitor implements ToolboxMessageVisitor {
        public void visitMackageInstallRequest(final MackageInstallRequest req) {
            synchronized(storeLock){
                MackageDesc mackageDesc = req.getMackageDesc();
                String purchasedMackageName = mackageDesc.getName();

                if ( MackageDesc.Type.TRIAL == mackageDesc.getType()) {
                    purchasedMackageName = mackageDesc.getFullVersion();
                }
                
                if ( purchasedMackageName == null ) {
                    /* set so this will never match in the later code */
                    logger.warn( "Trial Package, " + mackageDesc.getName() + " has no fullVersion." );
                    purchasedMackageName = "1233432453";
                }
                         
                 // FIND THE BUTTON THAT WOULD HAVE BEEN CLICKED
                MNodeJButton mNodeJButton = null;
                for( MNodeJButton storeButton : storeMap.values() ){
                    String storeButtonName = storeButton.getName();
                    if ( purchasedMackageName.equals( storeButtonName )) {
                        mNodeJButton = storeButton;
                        break;
                    }
                }
                try{
                    if(mNodeJButton == null){
                        MOneButtonJDialog.factory(Util.getMMainJFrame(), "",
                                                  "A problem occurred while purchasing:<br>"
                                                  + req.getMackageDesc().getName()
                                                  + "<br>Please try again.",
                                                  req.getMackageDesc().getName() + " Warning", "");
                        return;
                    }
                    Policy purchasePolicy;
                    if(mNodeJButton.getMackageDesc().isCore())
                        purchasePolicy = null;
                    else
                        purchasePolicy = selectedPolicy;
                    final MNodeJButton target = mNodeJButton;
                    SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
                        target.setProcuringView();
                    }});
                    purchaseBlockingQueue.put(new PurchaseWrapper(mNodeJButton, purchasePolicy, mackageDesc));
                }
                catch (Exception e) {
                    e.printStackTrace();
                    Util.handleExceptionNoRestart("Error purchasing", e);
                    mNodeJButton.setFailedProcureView();
                }
            }
        }

        public void visitUpdateExtraName(MackageUpdateExtraName req)
        {
            String name = req.getMackageName();
            String extraName = req.getExtraName();
            logger.debug("new extra name: '" + extraName + "' for mackage: '" + name + "'" );

            final List<MNodeJPanel> panels = new LinkedList<MNodeJPanel>();
            for (MNodeJPanel mNodeJPanel : getAllNodePanels()) {
                MackageDesc mackageDesc = mNodeJPanel.getMackageDesc();
                if (null == mackageDesc) continue;
                if (name.equals(mackageDesc.getName())) panels.add(mNodeJPanel);
            }

            /* Refresh the state in the swing thread */
            Thread thread = new Thread( new Runnable() {
                public void run() {
                    logger.debug("Refreshing the state for " + panels.size() + " panels");
                    for  (MNodeJPanel mNodeJPanel : panels) {
                        mNodeJPanel.doRefreshState();
                    }
                }
            } );

            thread.start();
        }
    }

    private class PurchaseWrapper {
        private final MNodeJButton mNodeJButton;
        private final Policy selectedPolicy;
        private final MackageDesc mackageDesc;
        public PurchaseWrapper(MNodeJButton mNodeJButton, Policy selectedPolicy, MackageDesc mackageDesc){
            this.mNodeJButton = mNodeJButton;
            this.selectedPolicy = selectedPolicy;
            this.mackageDesc = mackageDesc;
        }
        public MNodeJButton getMNodeJButton(){ return mNodeJButton; }
        public Policy getPolicy(){ return selectedPolicy; }
        public MackageDesc getMackageDesc() { return mackageDesc; }
    }
    /*
      public void moveFromStoreToToolbox(final MNodeJButton mNodeJButton){
      mNodeJButton.setProcuringView();
      try{
      purchaseBlockingQueue.put(mNodeJButton);
      }
      catch(Exception e){
      Util.handleExceptionNoRestart("Interrupted while waiting to purchase", e);
      mNodeJButton.setFailedProcureView();
      }
      }
    */
    private class MoveFromStoreToToolboxThread extends Thread implements Shutdownable {
        private volatile boolean stop = false;
        public MoveFromStoreToToolboxThread(){
            setDaemon(true);
            setContextClassLoader( Util.getClassLoader() );
            setName("MVCLIENT-MoveFromStoreToToolboxThread");
            start();
        }
        public void doShutdown(){
            if(!stop){
                stop = true;
                interrupt();
            }
        }
        public void run(){
            while(!stop){
                MNodeJButton purchasedMNodeJButton;
                try{
                    purchase(purchaseBlockingQueue.take());
                }
                catch(InterruptedException e){ continue; }
            }
            Util.printMessage("MoveFromStoreToToolboxThread Stopped");
        }
        private void purchase(PurchaseWrapper wrapper) throws InterruptedException {
            MNodeJButton mNodeJButton = wrapper.getMNodeJButton();
            final Policy targetPolicy = wrapper.getPolicy();
            final MackageDesc requestedMackageDesc = wrapper.getMackageDesc();
            final String installName = requestedMackageDesc.getName();
            try{
                //// MAKE SURE NOT PREVIOUSLY INSTALLED AS PART OF A BUNDLE
                MackageDesc[] originalUninstalledMackages = Util.getRemoteToolboxManager().uninstalled();
                boolean installed = true;
                for( MackageDesc mackageDesc : originalUninstalledMackages ){
                    if(installName.equals(mackageDesc.getName())){
                        installed = false;
                        break;
                    }
                }
                if( installed )
                    return;
                //// GET THE CORRECT BUTTON TO MESS WIT CAUSE STORE REFRESH MAY HAVE SHANKED US
                for( final MNodeJButton storeButton : storeMap.values() ){
                    if( storeButton.getName().equals(mNodeJButton.getName()) ){
                        mNodeJButton = storeButton;
                        SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
                            storeButton.setEnabled(false);
                            storeButton.setProgress("Preparing", 101);
                        }});
                        break;
                    }
                }

                //// SHOW PENDING PURCHASES IF ANY
                for( PurchaseWrapper purchaseWrapper : purchaseBlockingQueue ){
                    for( final MNodeJButton storeButton : storeMap.values() ){
                        if( purchaseWrapper.getMNodeJButton().getName().equals(storeButton.getName()) ){
                            SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
                                storeButton.setProcuringView();
                            }});
                            break;
                        }
                    }
                }

                //// DOWNLOAD FROM SERVER
                MackageDesc[] originalInstalledMackages = Util.getRemoteToolboxManager().installed(); // for use later

                long key = Util.getRemoteToolboxManager().install(installName);
                com.untangle.gui.util.Visitor visitor = new com.untangle.gui.util.Visitor(mNodeJButton);
                while (true) {
                    java.util.List<InstallProgress> lip = Util.getRemoteToolboxManager().getProgress(key);
                    for (InstallProgress ip : lip) {
                        ip.accept(visitor);
                        if( visitor.isDone() )
                            break;
                    }
                    if( visitor.isDone() )
                        break;
                    if (0 == lip.size()) {
                        Thread.currentThread().sleep(DOWNLOAD_CHECK_SLEEP_MILLIS);
                    }
                }
                if( !visitor.isSuccessful() )
                    throw new Exception();
                Thread.currentThread().sleep(DOWNLOAD_FINAL_PAUSE_MILLIS);
                // INSTALL INTO TOOLBOX
                long installStartTime = System.currentTimeMillis();
                final MNodeJButton finalJButton = mNodeJButton;
                SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
                    finalJButton.setDeployingView(); //Progress("Installing...", 101);
                }});
                boolean mackageInstalled = false;
                MackageDesc[] currentInstalledMackages = null;
                while( !mackageInstalled &&
                       ((System.currentTimeMillis() - installStartTime) < INSTALL_CHECK_TIMEOUT_MILLIS) ){
                    currentInstalledMackages = Util.getRemoteToolboxManager().installed();
                    for( MackageDesc mackageDesc : currentInstalledMackages ){
                        if(mackageDesc.getName().equals(installName)){
                            mackageInstalled = true;
                            break;
                        }
                    }
                    if( !mackageInstalled )
                        Thread.currentThread().sleep(INSTALL_CHECK_SLEEP_MILLIS);
                }
                if( !mackageInstalled )
                    throw new Exception();
                // UPDATE PROTOCOL SETTINGS CACHE
                loadAllCasings(false);
                // BRING MAIN WINDOW TO FRONT
                SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
                    Util.getMMainJFrame().setVisible(true);
                    Util.getMMainJFrame().toFront();
                }});
                // GENERATE LIST OF NEW MACKAGES, COUNTING EXTRA NAME
                List<MackageDesc> newMackageDescs = computeNewMackageDescs(originalInstalledMackages,
                                                                           currentInstalledMackages,
                                                                           true);
                // ADD TO TOOLBOX
                Policy currentPolicy = (Policy) viewSelector.getSelectedItem();
                for( MackageDesc newMackageDesc : newMackageDescs ){
                    if( isMackageNode(newMackageDesc)){
                        MNodeJButton newMNodeJButton = null;
                        if( newMackageDesc.isUtil() || newMackageDesc.isService()){
                            newMNodeJButton = addToToolbox(null,newMackageDesc,false,false);
                        }
                        else if(newMackageDesc.isSecurity()){
                            for( Policy policy : policyToolboxMap.keySet() ){
                                MNodeJButton tempMNodeJButton = addToToolbox(policy,newMackageDesc,false,false);
                                if( policy.equals(currentPolicy) ){
                                    newMNodeJButton = tempMNodeJButton;
                                }
                            }
                        }
                        else if( newMackageDesc.isCore() ){
                            newMNodeJButton = addToToolbox(null,newMackageDesc,false,false);
                        }
                        newMNodeJButton.setDeployedView();
                        revalidateToolboxes();
                    }
                }
                // GENERATE LIST OF NEW MACKAGES, NOT COUNTING EXTRA NAME
                newMackageDescs = computeNewMackageDescs(originalInstalledMackages,
                                                         currentInstalledMackages,
                                                         false);
                //// AUTO-INSTALL INTO RACK
                for( MackageDesc newMackageDesc : newMackageDescs ){
                    if( !isMackageNode(newMackageDesc)) {
                        continue;
                    }
                    try{
                        Policy newPolicy = null;
                        if( !newMackageDesc.isCore() )
                            newPolicy = targetPolicy;
                        Tid tid = Util.getNodeManager().instantiate(newMackageDesc.getName(),newPolicy);
                        NodeContext nodeContext = Util.getNodeManager().nodeContext( tid );
                        NodeDesc nodeDesc = nodeContext.getNodeDesc();
                        MNodeJPanel mNodeJPanel = MNodeJPanel.instantiate(nodeContext, nodeDesc, newPolicy);
                        Thread.currentThread().sleep(DEPLOY_FINAL_PAUSE_MILLIS);
                        addToRack(newPolicy, mNodeJPanel, true);
                        focusInRack(mNodeJPanel);
                        // AUTO ON
                        if( newMackageDesc.getName().endsWith("router") || newMackageDesc.getName().endsWith("openvpn") ){
                            mNodeJPanel.setPowerOnHintVisible(true);
                            MOneButtonJDialog.factory( Util.getMMainJFrame(), "", newMackageDesc.getDisplayName()
                                                       + " can not be automatically turned on."
                                                       + "<br>Please configure its settings first.",
                                                       newMackageDesc.getDisplayName() + " Warning", "");
                        }
                        else{
                            while(!mNodeJPanel.getDoneRefreshing())
                                sleep(100L);
                            final MNodeJPanel target = mNodeJPanel;
                            SwingUtilities.invokeLater( new Runnable(){ public void run(){
                                target.powerJToggleButton().doClick();
                            }});
                        }
                    }
                    catch(Exception e){
                        e.printStackTrace();
                        try{ Util.handleExceptionWithRestart("Error during auto install/on procedure", e); }
                        catch(Exception f){
                            Util.handleExceptionNoRestart("Error during auto install/on procedure", f);
                            mNodeJButton.setFailedDeployView();
                            MOneButtonJDialog.factory( Util.getMMainJFrame(), "",
                                                       "A problem occurred while installing to the rack:<br>"
                                                       + mNodeJButton.getDisplayName()
                                                       + "<br>Please contact Untangle Support.",
                                                       mNodeJButton.getDisplayName() + " Warning", "");
                        }
                    }
                }
                // SHOW DEPLOYED PROGRESS
                final MNodeJButton finalJButton2 = mNodeJButton;
                SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
                    finalJButton2.setProgress("Installed!", 100);
                    finalJButton2.setEnabled(false);
                }});
                Thread.currentThread().sleep(INSTALL_FINAL_PAUSE_MILLIS);
                // REFRESH STATE OF ALL EXISTING APPLIANCES
                for ( MNodeJPanel mNodeJPanel : getAllNodePanels()) {
                    mNodeJPanel.doRefreshState();
                }
                // SHOW IF PREMIUM SIGNATURE SERVICE WAS ENABLED
                updatePremiumStatus();
            }
            catch(InterruptedException e){ throw e; }
            catch(Exception e){
                e.printStackTrace();
                if( !isInterrupted() ){
                    try{
                        Util.handleExceptionWithRestart("Error purchasing: " +  mNodeJButton.getName(),  e);
                    }
                    catch(Exception f){
                        Util.handleExceptionNoRestart("Error purchasing:", f);
                        mNodeJButton.setFailedProcureView();
                        final MNodeJButton finalJButton3 = mNodeJButton;
                        SwingUtilities.invokeLater( new Runnable(){ public void run(){
                            MOneButtonJDialog.factory(Util.getMMainJFrame(), "",
                                                      "A problem occurred while purchasing:<br>"
                                                      + finalJButton3.getDisplayName()
                                                      + "<br>Please try again or contact Untangle Support for assistance.",
                                                      finalJButton3.getDisplayName() + " Warning", "");
                        }});
                    }
                }
            }
            finally{
                // REMOVE FROM STORE / UPDATE STORE MODEL
                updateStoreModelBlocking();
            }
        }
    }
    private List<MackageDesc> computeNewMackageDescs(MackageDesc[] originalInstalledMackages,
                                                     MackageDesc[] currentInstalledMackages,
                                                     boolean countExtraName){
        Vector<MackageDesc> newlyInstalledMackages = new Vector<MackageDesc>();
        Hashtable<String,String> originalInstalledMackagesHashtable = new Hashtable<String,String>();
        if(countExtraName){
            for( MackageDesc mackageDesc : originalInstalledMackages )
                originalInstalledMackagesHashtable.put(mackageDesc.getName()+mackageDesc.getExtraName(),
                                                       mackageDesc.getName()+mackageDesc.getExtraName());
            for( MackageDesc mackageDesc : currentInstalledMackages )
                if( !originalInstalledMackagesHashtable.containsKey(mackageDesc.getName()+mackageDesc.getExtraName()) )
                    newlyInstalledMackages.add(mackageDesc);
        }
        else{
            for( MackageDesc mackageDesc : originalInstalledMackages )
                originalInstalledMackagesHashtable.put(mackageDesc.getName(), mackageDesc.getName());
            for( MackageDesc mackageDesc : currentInstalledMackages )
                if( !originalInstalledMackagesHashtable.containsKey(mackageDesc.getName()) )
                    newlyInstalledMackages.add(mackageDesc);
        }
        return newlyInstalledMackages;
    }
    ///////////////////////////////////////////////////////
    // TOOLBOX / STORE OPERATIONS /////////////////////////


    // INIT API ////////////////////////////////////////////
    ////////////////////////////////////////////////////////
    private void initUvmModel() throws Exception {
        // SECURITY
        for( Policy policy : Util.getPolicyManager().getPolicies() )
            policyTidMap.put( policy, Util.getNodeManager().nodeInstancesVisible(policy) );
        // NON-SECURITY (CORE & UTIL & SERVICES)
        nonPolicyTidList = Util.getNodeManager().nodeInstancesVisible((Policy)null);
        // NAME MAPS FOR QUICK LOOKUP
        for( Policy policy : policyTidMap.keySet() ){
            Map<String,Object> nameMap = new HashMap<String,Object>();
            policyNameMap.put(policy,nameMap);
            for( Tid tid : policyTidMap.get(policy) ){
                nameMap.put(tid.getNodeName(),null);
            }
        }
        for( Tid tid : nonPolicyTidList )
            nonPolicyNameMap.put(tid.getNodeName(),null);
    }

    public void updateStoreModel(){
        synchronized(storeLock){
            storeModelThread.updateStoreModel();
        }
    }
    public void updateStoreModelBlocking(){
        synchronized(storeLock){
            storeModelThread.updateStoreModelBlocking();
        }
    }
    private class StoreModelThread extends Thread implements Shutdownable {
        private JProgressBar storeProgressBar;
        private volatile boolean doUpdate = false;
        private volatile boolean firstRun = true;
        private volatile boolean stop = false;
        public StoreModelThread(){
            setDaemon(true);
            setName("MVCLIENT-StoreModelThread");
            storeProgressBar = new JProgressBar();
            storeProgressBar.setStringPainted(true);
            storeProgressBar.setOpaque(true);
            //storeProgressBar.setForeground(new java.awt.Color(68, 91, 255));
            //storeProgressBar.setFont(new java.awt.Font("Dialog", 0, 12));
            storeProgressBar.setPreferredSize(new java.awt.Dimension(130, 20));
            storeProgressBar.setMaximumSize(new java.awt.Dimension(130, 20));
            storeProgressBar.setMinimumSize(new java.awt.Dimension(130, 20));
            start();
        }
        public synchronized void updateStoreModel(){
            doUpdate = true;
            notify();
        }
        public synchronized void updateStoreModelBlocking(){
            initStoreModel();
        }
        public synchronized void doShutdown(){
            if(!stop){
                stop = true;
                notify();
                interrupt();
            }
        }
        public void run(){
            // MAIN STORE EVENT LOOP
            while(!stop){
                try{
                    initStoreModel();
                    synchronized(this){
                        if(stop)
                            break;
                        else if( doUpdate ){
                            doUpdate = false;
                            wait(STORE_UPDATE_CHECK_SLEEP);
                        }
                        else{
                            if(stop)
                                break;
                            wait(STORE_UPDATE_CHECK_SLEEP);
                            if(stop)
                                break;
                        }
                    }
                }
                catch(InterruptedException e){ continue; }
            }
        }
        private void initStoreModel(){
            // REFRESH STATE OF ALL EXISTING APPLIANCES
            for ( MNodeJPanel mNodeJPanel : getAllNodePanels()) {
                mNodeJPanel.doRefreshState();
            }

            // SHOW THE USER WHATS GOING ON
            SwingUtilities.invokeLater( new Runnable(){ public void run(){
                // CLEAR OUT THE STORE
                storeMap.clear();
                storeJPanel.removeAll();
                // CREATE PROGRESS BAR AND ADD IT
                storeProgressBar.setIndeterminate(true);
                storeProgressBar.setString("Connecting...");
                storeJPanel.add(storeProgressBar, storeProgressGridBagConstraints);
                storeJPanel.revalidate();
                storeJPanel.repaint();
            }});


            // CHECK FOR STORE CONNECTIVITY AND AVAILABLE ITEMS
            boolean connectedToStore = false;
            MackageDesc[] storeItemsAvailable = null;
            try{
                Util.getRemoteToolboxManager().update();
                storeItemsAvailable = Util.getRemoteToolboxManager().uninstalled();
                if( storeItemsAvailable == null ) {
                    logger.debug("items: null");
                } else {
                    logger.debug("items: " + storeItemsAvailable.length);
                }

                connectedToStore = true;
            }
            catch(Exception e){
                Util.handleExceptionNoRestart("Error: unable to connect to store",e);
            }
            // SHOW RESULTS
            if( !connectedToStore ){
                // NO CONNECTION
                SwingUtilities.invokeLater( new Runnable(){ public void run(){
                    storeProgressBar.setValue(1);
                    storeProgressBar.setIndeterminate(false);
                    storeProgressBar.setString("No Connection");
                    if(firstRun){
                        actionJTabbedPane.setSelectedIndex(0);
                        firstRun = false;
                    }
                }});
            }
            else{
                if( storeItemsAvailable.length == 0 ){
                    // CONNECTION, BUT NO ITEMS AVAILABLE
                    SwingUtilities.invokeLater( new Runnable(){ public void run(){
                        storeProgressBar.setValue(1);
                        storeProgressBar.setIndeterminate(false);
                        storeProgressBar.setString("No New Items");
                        JButton storeSettingsJButton = new JButton();
                        storeSettingsJButton.setFocusPainted(false);
                        storeSettingsJButton.setFont(new java.awt.Font("Arial", 0, 12));
                        storeSettingsJButton.setText("<html>Show<br>My Account</html>");
                        storeSettingsJButton.setMargin(new Insets(3,3,3,2));
                        storeSettingsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/main/IconBilling36x36.png")));
                        storeSettingsJButton.addActionListener(new StoreSettingsActionListener());
                        storeJPanel.add(storeSettingsJButton, storeSettingsGridBagConstraints);
                        storeJPanel.revalidate();
                        storeJPanel.repaint();
                        if(firstRun){
                            actionJTabbedPane.setSelectedIndex(1);
                            firstRun = false;
                        }
                    }});
                }
                else{
                    // CONNECTION, ITEMS AVAILABLE
                    SwingUtilities.invokeLater( new Runnable(){ public void run(){
                        storeJPanel.remove(storeProgressBar);
                        JPanel storeSpacerJPanel = new JPanel();
                        storeSpacerJPanel.setOpaque(false);
                        storeJPanel.add(storeSpacerJPanel, storeSpacerGridBagConstraints, 0);
                        storeJPanel.revalidate();
                        storeJPanel.repaint();
                        if(firstRun){
                            actionJTabbedPane.setSelectedIndex(0);
                            firstRun = false;
                        }
                    }});



                    // REMOVE TRIAL IF THE ACTUAL THING WAS PURCHASED (IS NO LONGER IN THE STORE)
                    /*
                      Map<String,String> storeItemMap = new HashMap<String,String>();
                      for( MackageDesc mackageDesc : storeItemsAvailable ){
                      String name = mackageDesc.getName();
                      if( name.endsWith(STOREITEM_EXTENSION) && !name.endsWith(TRIAL_EXTENSION) ){
                      name = name.substring(0, name.indexOf('-'));
                      storeItemMap.put(name, name);
                      }
                      }
                      for( MackageDesc mackageDesc : storeItemsAvailable ){
                      String name = mackageDesc.getName();
                      if( name.endsWith(TRIAL_EXTENSION) ){
                      name = name.substring(0, name.indexOf('-'));
                      if( storeItemMap.containsKey(name) ){
                      addToStore(mackageDesc,false);
                      }
                      else
                      continue;
                      }
                      else
                      addToStore(mackageDesc,false);
                      }
                    */


                    /*
                    // COMPUTE REMOVED BUTTONS
                    Vector<MNodeJButton> removedVector = new Vector<MNodeJButton>();
                    for( MNodeJButton storeButton : storeMap.values() ){
                    String storeButtonName = storeButton.getName().substring(0, storeButtonName.indexOf('-'));
                    boolean found = false;
                    for( MackageDesc mackageDesc : storeItemsAvailable ){
                    String mackageDescName = mackageDesc.getName();
                    if( mackageDescName.endsWith(STOREITEM_EXTENSION) && !mackageDescName.endsWith(TRIAL_EXTENSION) ){
                    mackageDescName = mackageDescName.substring(0, mackageDescName.indexOf('-'));
                    if( storeButtonName.equals(mackageDescName) ){
                    found = true;
                    break;
                    }
                    }
                    }
                    if(!found)
                    removedVector.add(storeButton);
                    }

                    // COMPUTE ADDABLE MACKAGES
                    Vector<MackageDesc> addableVector = new Vector<MackageDesc>();
                    for( MackageDesc mackageDesc : storeItemsAvailable ){
                    String mackageDescName = mackageDesc.getName();
                    boolean found = false;
                    if( mackageDescName.endsWith(STOREITEM_EXTENSION) && !mackageDescName.endsWith(TRIAL_EXTENSION) ){
                    mackageDescName = mackageDescName.substring(0, mackageDescName.indexOf('-'));
                    for( MNodeJButton storeButton : storeMap.values() ){
                    String storeButtonName = storeButton.getName().substring(0, storeButtonName.indexOf('-'));
                    if( storeButtonName.equals(mackageDescName) ){
                    found = true;
                    break;
                    }
                    }
                    }
                    if(!found)
                    addableVector.add(mackageDesc);
                    }
                    */


                    // ADD TO STORE IF NOT A TRIAL
                    for( MackageDesc mackageDesc : storeItemsAvailable ){
                        MackageDesc.Type type = mackageDesc.getType();
                        String name = mackageDesc.getName();
                        //System.out.println("testing: " + name);
                        if( type == MackageDesc.Type.LIB_ITEM ) {
                            addToStore(mackageDesc,false);
                            //System.out.println("added");
                        }
                        else{
                            //System.out.println("failed");
                        }
                    }

                    revalidateStore();
                }
            }
        }
    }

    private class StoreSettingsActionListener implements ActionListener{
        public void actionPerformed(ActionEvent e){
            try{
                String authNonce = Util.getRemoteAdminManager().generateAuthNonce();
                URL newURL = new URL( Util.getServerCodeBase(), "../library/index.php?option=com_content&task=view&id=31&Itemid=63&"+ authNonce);
                ((BasicService) ServiceManager.lookup("javax.jnlp.BasicService")).showDocument(newURL);
            }
            catch(Exception f){
                Util.handleExceptionNoRestart("error launching browser for Library settings", f);
                MOneButtonJDialog.factory(Util.getMMainJFrame(), "",
                                          "A problem occurred while trying to access Library."
                                          + "<br>Please contact Untangle Support.",
                                          "Untangle Library Warning", "");
            }
        }
    }

    private void initToolboxModel(final JProgressBar progressBar){
        // BUILD THE MODEL
        Map<String,MackageDesc> installedMackageMap = new HashMap<String,MackageDesc>();
        for( MackageDesc mackageDesc : Util.getRemoteToolboxManager().installedVisible() ){
            installedMackageMap.put(mackageDesc.getName(),mackageDesc);
        }
        // SHOW IF PREMIUM SIGNATURE SERVICE WAS ENABLED
        updatePremiumStatus();
        SwingUtilities.invokeLater( new Runnable(){ public void run(){
            progressBar.setValue(64);
            progressBar.setString("Populating My Apps...");
        }});
        int progress = 0;
        final float overallFinal = (float) (installedMackageMap.size() * (policyTidMap.size()+2)); // +1 for cores, +1 for util&serv
        // NODES
        for( MackageDesc mackageDesc : installedMackageMap.values() ){
            if( isMackageNode(mackageDesc) && mackageDesc.isCore()){
                boolean isDeployed = nonPolicyNameMap.containsKey(mackageDesc.getName());
                addToToolbox(null,mackageDesc,isDeployed,false);
            }
            progress++;
            final float progressFinal = (float) progress;
            SwingUtilities.invokeLater( new Runnable(){ public void run(){
                progressBar.setValue(64 + (int) (32f*progressFinal/overallFinal) );
            }});
        }
        // SECURITY
        for( Policy policy : policyTidMap.keySet() ){
            JPanel toolboxJPanel = new JPanel();
            toolboxJPanel.setLayout(new GridBagLayout());
            toolboxJPanel.setOpaque(false);
            policyToolboxJPanelMap.put(policy, toolboxJPanel);
            policyToolboxMap.put(policy, new TreeMap<ButtonKey,MNodeJButton>());
            for( MackageDesc mackageDesc : installedMackageMap.values() ){
                if( isMackageNode( mackageDesc ) && mackageDesc.isSecurity() ){
                    boolean isDeployed = policyNameMap.get(policy).containsKey(mackageDesc.getName());
                    addToToolbox(policy,mackageDesc,isDeployed,false);
                }
                progress++;
            }
            final float progressFinal = (float) progress;
            SwingUtilities.invokeLater( new Runnable(){ public void run(){
                progressBar.setValue(64 + (int) (32f*progressFinal/overallFinal) );
            }});
        }
        revalidateToolboxes();
        SwingUtilities.invokeLater( new Runnable(){ public void run(){
            progressBar.setValue(96);
        }});
    }
    private void initRackModel(final JProgressBar progressBar){
        int addCount = 0;
        SwingUtilities.invokeLater( new Runnable(){ public void run(){
            progressBar.setValue(16);
            if( policyTidMap.keySet().size() == 1 )
                progressBar.setString("Populating Rack...");
            else
                progressBar.setString("Populating Racks...");
        }});
        // GENERATE OVERALL AND CURRENT COUNT
        applianceLoadProgress = 0;
        int overall = 0;
        for( Policy policy : policyTidMap.keySet() )
            overall += policyTidMap.get(policy).size();
        overall += nonPolicyTidList.size();
        // SECURITY
        for( Policy policy : policyTidMap.keySet() ){
            lastRackScrollPosition.put(policy,0);
            JPanel rackJPanel = new JPanel();
            rackJPanel.setLayout(new GridBagLayout());
            rackJPanel.setOpaque(false);
            policyRackJPanelMap.put(policy,rackJPanel);
            policyRackMap.put(policy,new TreeMap<ButtonKey,MNodeJPanel>());
            for( Tid tid : policyTidMap.get(policy) ){
                addCount++;
                new LoadApplianceThread(policy,tid,overall,progressBar);
            }
        }
        // NON-SECURITY
        for( Tid tid : nonPolicyTidList ){
            addCount++;
            new LoadApplianceThread(null,tid,overall,progressBar);
        }
        try{
            while( applianceLoadProgress < overall ){
                Thread.currentThread().sleep(100);
            }
        }
        catch(Exception e){ Util.handleExceptionNoRestart("Error sleeping while product loading",e); }


        int count = 0;
        boolean nonNatFound = false;
        for( Policy policy : policyRackMap.keySet() ){
            count += policyRackMap.get(policy).size();
        }
        if(count>0)
            nonNatFound = true;
        count += utilRackMap.size();
        if(count>0)
            nonNatFound = true;
        count += coreRackMap.size();
        for( MNodeJPanel target : coreRackMap.values() )
            if(!target.getMackageDesc().getName().equals("untangle-node-router"))
                nonNatFound = true;
        if( (count <= 1) && (!nonNatFound) )
            rackViewJPanel.add(storeWizardJButton, storeWizardGridBagConstraints);
        //Util.getMPipelineJPanel().setStoreWizardButtonVisible( addCount<=1 );
        SwingUtilities.invokeLater( new Runnable(){ public void run(){
            progressBar.setValue(64);
        }});
    }
    private class LoadApplianceThread extends Thread{
        private Policy policy;
        private Tid tid;
        private int overallProgress;
        private JProgressBar progressBar;
        public LoadApplianceThread(Policy policy, Tid tid, int overallProgress, JProgressBar progressBar){
            setDaemon(true);
            this.policy = policy;
            this.tid = tid;
            this.overallProgress = overallProgress;
            this.progressBar = progressBar;
            setName("MVCLIENT-LoadApplianceThread: " + tid.getId());
            setContextClassLoader( Util.getClassLoader() );
            start();
        }
        public void run(){
            try{
                loadSemaphore.acquire();
                // GET THE NODE CONTEXT AND MACKAGE DESC
                NodeContext nodeContext = Util.getNodeManager().nodeContext(tid);
                NodeDesc nodeDesc = nodeContext.getNodeDesc();
                // CONSTRUCT AND ADD THE APPLIANCE
                MNodeJPanel mNodeJPanel = MNodeJPanel.instantiate(nodeContext,nodeDesc,policy);
                addToRack(policy,mNodeJPanel,false);
            }
            catch(Exception e){
                try{ Util.handleExceptionWithRestart("Error instantiating product: " + tid, e); }
                catch(Exception f){ Util.handleExceptionNoRestart("Error instantiating product: " + tid, f); }
            }
            finally{
                loadSemaphore.release();
            }

            final float overallFinal = (float) overallProgress;
            SwingUtilities.invokeLater( new Runnable(){ public void run(){
                PolicyStateMachine.this.applianceLoadProgress++;
                float progressFinal = (float) PolicyStateMachine.this.applianceLoadProgress;
                progressBar.setValue(16 + (int) (48f*progressFinal/overallFinal) );
            }});
        }
    }
    private void initViewSelector(){
        DefaultComboBoxModel newModel = new DefaultComboBoxModel();
        for( Policy policy : policyTidMap.keySet() )
            newModel.addElement(policy);
        newModel.addElement(POLICY_MANAGER_SEPARATOR);
        newModel.addElement(POLICY_MANAGER_OPTION);
        newModel.setSelectedItem( newModel.getElementAt(0) );
        viewSelector.setModel(newModel);
        viewSelector.setRenderer( new PolicyRenderer(viewSelector.getRenderer()) );
        handleViewSelector();
    }
    ////////////////////////////////////////
    // INIT API ////////////////////////////


    // REMOVE API /////////////////////////
    ///////////////////////////////////////
    private void removeFromStore(final MackageDesc mackageDesc){
        final ButtonKey buttonKey = new ButtonKey(mackageDesc);
        SwingUtilities.invokeLater( new Runnable() { public void run() {
            MNodeJButton mNodeJButton = storeMap.get(buttonKey);
            for( ActionListener actionListener : mNodeJButton.getActionListeners() )
                mNodeJButton.removeActionListener(actionListener);
            storeMap.remove(buttonKey);
            storeJPanel.remove(mNodeJButton);
            storeJPanel.revalidate();
        }});
    }
    private void removeFromToolbox(final MackageDesc mackageDesc){
        final ButtonKey buttonKey = new ButtonKey(mackageDesc);
        SwingUtilities.invokeLater( new Runnable() { public void run() {
            if( mackageDesc.isUtil() || mackageDesc.isService() ){
                int position = ((TreeMap)utilToolboxMap).headMap(buttonKey).size();
                MNodeJButton mNodeJButton = utilToolboxMap.get(buttonKey);
                for( ActionListener actionListener : mNodeJButton.getActionListeners() )
                    mNodeJButton.removeActionListener(actionListener);
                utilToolboxMap.remove(buttonKey);
                utilToolboxJPanel.remove(position);
                utilToolboxJPanel.revalidate();
            }
            else if(mackageDesc.isSecurity() ){
                for( Policy policy : policyToolboxMap.keySet() ){
                    Map<ButtonKey,MNodeJButton> toolboxMap = policyToolboxMap.get(policy);
                    int position = ((TreeMap)toolboxMap).headMap(buttonKey).size();
                    MNodeJButton mNodeJButton = toolboxMap.get(buttonKey);
                    for( ActionListener actionListener : mNodeJButton.getActionListeners() )
                        mNodeJButton.removeActionListener(actionListener);
                    toolboxMap.remove(buttonKey);
                    JPanel toolboxJPanel = policyToolboxJPanelMap.get(policy);
                    toolboxJPanel.remove(position);
                    toolboxJPanel.revalidate();
                }
            }
            else if( mackageDesc.isCore() ){
                int position = ((TreeMap)coreToolboxMap).headMap(buttonKey).size();
                MNodeJButton mNodeJButton = coreToolboxMap.get(buttonKey);
                for( ActionListener actionListener : mNodeJButton.getActionListeners() )
                    mNodeJButton.removeActionListener(actionListener);
                coreToolboxMap.remove(buttonKey);
                coreToolboxJPanel.remove(position);
                coreToolboxJPanel.revalidate();
            }
        }});
    }
    private void removeFromRack(final Policy policy, final MNodeJPanel mNodeJPanel){
        final ButtonKey buttonKey = new ButtonKey(mNodeJPanel);
        SwingUtilities.invokeLater( new Runnable(){ public void run(){
            if( mNodeJPanel.getMackageDesc().isUtil() || mNodeJPanel.getMackageDesc().isService() ){
                // REMOVE FROM RACK MODEL
                utilRackMap.remove(buttonKey);
                // REMOVE FROM RACK VIEW
                utilRackJPanel.remove(mNodeJPanel);
                utilRackJPanel.revalidate();
                // DEAL WITH SPACER AND JPANEL
                if( utilRackMap.isEmpty() ){
                    rackViewJPanel.remove( utilSeparator );
                    rackViewJPanel.remove( utilRackJPanel );
                    rackViewJPanel.revalidate();
                    rackViewJPanel.repaint();
                }
            }
            else if(mNodeJPanel.getMackageDesc().isSecurity()){
                JPanel rackJPanel = policyRackJPanelMap.get(policy);
                // REMOVE FROM RACK MODEL
                policyRackMap.get(policy).remove(buttonKey);
                // SEE IF ALL POLICIES ARE EMPTY
                boolean allPolicyEmpty = true;
                for( Policy policy : policyRackMap.keySet() ){
                    if(policyRackMap.get(policy).size()>0){
                        allPolicyEmpty = false;
                        break;
                    }
                }
                // REMOVE FROM RACK VIEW
                rackJPanel.remove(mNodeJPanel);
                rackJPanel.revalidate();
                // REMOVE SEPARATOR IF ALL EMPTY (AND SET TO THE INITIAL VALUE)
                if( allPolicyEmpty ){
                    viewSelector.setSelectedIndex(0);
                    rackViewJPanel.remove( policySeparator );
                    rackViewJPanel.remove( policyRackJPanelMap.get(policy) );
                    rackViewJPanel.revalidate();
                    rackViewJPanel.repaint();
                }
            }
            else if( mNodeJPanel.getMackageDesc().isCore() ){
                // REMOVE FROM RACK MODEL
                coreRackMap.remove(buttonKey);
                // REMOVE FROM RACK VIEW
                coreRackJPanel.remove(mNodeJPanel);
                coreRackJPanel.revalidate();
                // DEAL WITH SPACER AND JPANEL
                if( coreRackMap.isEmpty() ){
                    rackViewJPanel.remove( coreSeparator );
                    rackViewJPanel.remove( coreRackJPanel );
                    rackViewJPanel.revalidate();
                    rackViewJPanel.repaint();
                }
            }

            int count = 0;
            boolean nonNatFound = false;
            for( Policy policy : policyRackMap.keySet() ){
                count += policyRackMap.get(policy).size();
            }
            if(count>0)
                nonNatFound = true;
            count += utilRackMap.size();
            if(count>0)
                nonNatFound = true;
            count += coreRackMap.size();
            for( MNodeJPanel target : coreRackMap.values() )
                if(!target.getMackageDesc().getName().equals("untangle-node-router"))
                    nonNatFound = true;
            if( (count <= 1) && (!nonNatFound) && (storeWizardJButton.getParent()==null) )
                rackViewJPanel.add(storeWizardJButton, storeWizardGridBagConstraints);
            //  Util.getMPipelineJPanel().setStoreWizardButtonVisible(true);
        }});
    }
    ///////////////////////////////////////
    // REMOVE API /////////////////////////


    // ADD API ////////////////////////////
    ///////////////////////////////////////
    private void revalidateStore(){
        SwingUtilities.invokeLater( new Runnable(){ public void run(){
            storeJPanel.revalidate();
        }});
    }
    private void addToStore(final MackageDesc mackageDesc, final boolean doRevalidate){
        if( !isMackageVisible(mackageDesc) )
            return;
        else if( !isMackageStoreItem(mackageDesc) )
            return;
        final ButtonKey buttonKey = new ButtonKey(mackageDesc);
        final MNodeJButton mNodeJButton = new MNodeJButton(mackageDesc);
        mNodeJButton.setProcurableView(); // xxx i believe this is safe because it ends up on the EDT
        SwingUtilities.invokeLater( new Runnable(){ public void run(){
            // UPDATE GUI DATA MODEL
            storeMap.put(buttonKey, mNodeJButton);
            mNodeJButton.addActionListener( new StoreActionListener(mNodeJButton) );
            // UPDATE GUI VIEW MODEL
            int position = ((TreeMap)storeMap).headMap(buttonKey).size();
            storeJPanel.add(mNodeJButton, buttonGridBagConstraints, position);
            if(doRevalidate)
                storeJPanel.revalidate();
        }});
    }
    private void revalidateToolboxes(){
        SwingUtilities.invokeLater( new Runnable(){ public void run(){
            utilToolboxJPanel.revalidate();
            for(JPanel toolboxJPanel : policyToolboxJPanelMap.values())
                toolboxJPanel.revalidate();
            coreToolboxJPanel.revalidate();
        }});
    }
    private MNodeJButton addToToolbox(final Policy policy, final MackageDesc mackageDesc,
                                      final boolean isDeployed, final boolean doRevalidate){
        // ONLY UPDATE GUI MODELS IF THIS IS VISIBLE
        if( !isMackageVisible(mackageDesc) )
            return null;
        else if( isMackageStoreItem(mackageDesc) )
            return null;
        else if ( !isMackageNode(mackageDesc))
            return null;

        final ButtonKey buttonKey = new ButtonKey(mackageDesc);
        final MNodeJButton mNodeJButton = new MNodeJButton(mackageDesc);
        if( isDeployed )
            mNodeJButton.setDeployedView();
        else
            mNodeJButton.setDeployableView();
        SwingUtilities.invokeLater( new Runnable(){ public void run(){
            if(mackageDesc.isSecurity()){
                // UPDATE GUI DATA MODEL
                Map<ButtonKey,MNodeJButton> toolboxMap = policyToolboxMap.get(policy);
                MNodeJButton removeMNodeJButton = toolboxMap.remove(buttonKey); // to remove possible trial
                toolboxMap.put(buttonKey,mNodeJButton);
                mNodeJButton.addActionListener( new ToolboxActionListener(policy,mNodeJButton) );
                // UPDATE GUI VIEW MODEL
                JPanel toolboxJPanel = policyToolboxJPanelMap.get(policy);
                int position = ((TreeMap)toolboxMap).headMap(buttonKey).size();
                if(removeMNodeJButton!=null)
                    coreToolboxJPanel.remove(removeMNodeJButton);
                toolboxJPanel.add(mNodeJButton, buttonGridBagConstraints, position);
                if(doRevalidate)
                    toolboxJPanel.revalidate();
            }
            else if( mackageDesc.isCore() ){
                // UPDATE GUI DATA MODEL
                MNodeJButton removeMNodeJButton = coreToolboxMap.remove(buttonKey); // to remove possible trial
                coreToolboxMap.put(buttonKey,mNodeJButton);
                mNodeJButton.addActionListener( new ToolboxActionListener(null,mNodeJButton) );
                // UPDATE GUI VIEW MODEL
                int position = ((TreeMap)coreToolboxMap).headMap(buttonKey).size();
                if(removeMNodeJButton!=null)
                    coreToolboxJPanel.remove(removeMNodeJButton);
                coreToolboxJPanel.add(mNodeJButton, buttonGridBagConstraints, position);
                if(doRevalidate)
                    coreToolboxJPanel.revalidate();
            }
        }});
        return mNodeJButton;
    }
    private void revalidateRacks(){
        SwingUtilities.invokeLater( new Runnable() { public void run() {
            utilRackJPanel.revalidate();
            for( JPanel rackJPanel : policyRackJPanelMap.values() )
                rackJPanel.revalidate();
            coreRackJPanel.revalidate();
        }});
    }
    private void addToRack(final Policy policy, final MNodeJPanel mNodeJPanel, final boolean doRevalidate){
        final ButtonKey buttonKey = new ButtonKey(mNodeJPanel);
        SwingUtilities.invokeLater( new Runnable() { public void run() {

            if( mNodeJPanel.getMackageDesc().isUtil() || mNodeJPanel.getMackageDesc().isService()){
                // DEAL WITH SPACER
                if( utilRackMap.isEmpty() ){
                    rackViewJPanel.add( utilSeparator, utilSeparatorGridBagConstraints );
                    rackViewJPanel.add( utilRackJPanel, utilGridBagConstraints );
                    rackViewJPanel.revalidate();
                    rackViewJPanel.repaint();
                }
                // ADD TO RACK MODEL
                utilRackMap.put(buttonKey,mNodeJPanel);
                // UPDATE GUI VIEW MODEL
                int position = ((TreeMap)utilRackMap).headMap(buttonKey).size();
                utilRackJPanel.add(mNodeJPanel, applianceGridBagConstraints, position);
                if(doRevalidate)
                    utilRackJPanel.revalidate();
            }
            else if( mNodeJPanel.getMackageDesc().isSecurity()) {
                // SEE IF ALL POLICIES ARE EMPTY
                boolean allEmpty = true;
                for( Policy policy : policyRackMap.keySet() ){
                    if(policyRackMap.get(policy).size()>0){
                        allEmpty = false;
                        break;
                    }
                }
                // DEAL WITH SPACER
                if( allEmpty ){
                    rackViewJPanel.add( policySeparator, policySeparatorGridBagConstraints );
                    JPanel rackJPanel = policyRackJPanelMap.get(policy);
                    if( !rackJPanel.isAncestorOf(rackViewJPanel) && doRevalidate ){ // XXX doRevalidate is a nasty hack to make sure this only goes off after init
                        rackViewJPanel.add( rackJPanel, policyGridBagConstraints );
                    }
                    //Policy currentPolicy = (Policy) viewSelector.getSelectedItem();
                    //rackViewJPanel.add( policyRackJPanelMap.get(currentPolicy), rackGridBagConstraints );
                }
                rackViewJPanel.revalidate();
                rackViewJPanel.repaint();
                // ADD TO RACK MODEL
                policyRackMap.get(policy).put(buttonKey,mNodeJPanel);
                // UPDATE GUI VIEW MODEL
                final JPanel rackJPanel = policyRackJPanelMap.get(policy);
                int position = ((TreeMap)policyRackMap.get(policy)).headMap(buttonKey).size();
                rackJPanel.add(mNodeJPanel, applianceGridBagConstraints, position);
                if(doRevalidate)
                    rackJPanel.revalidate();
            }
            else if( mNodeJPanel.getMackageDesc().isCore() ){
                // DEAL WITH SPACER
                if( coreRackMap.isEmpty() ){
                    rackViewJPanel.add( coreSeparator, coreSeparatorGridBagConstraints );
                    rackViewJPanel.add( coreRackJPanel, coreGridBagConstraints );
                    rackViewJPanel.revalidate();
                    rackViewJPanel.repaint();
                }
                // ADD TO RACK MODEL
                coreRackMap.put(buttonKey,mNodeJPanel);
                // UPDATE GUI VIEW MODEL
                int position = ((TreeMap)coreRackMap).headMap(buttonKey).size();
                coreRackJPanel.add(mNodeJPanel, applianceGridBagConstraints, position);
                if(doRevalidate)
                    coreRackJPanel.revalidate();
            }
            int count = 0;
            boolean nonNatFound = false;
            for( Policy policy : policyRackMap.keySet() ){
                count += policyRackMap.get(policy).size();
            }
            if(count>0)
                nonNatFound = true;
            count += utilRackMap.size();
            if(count>0)
                nonNatFound = true;
            count += coreRackMap.size();
            for( MNodeJPanel target : coreRackMap.values() )
                if(!target.getMackageDesc().getName().equals("untangle-node-router"))
                    nonNatFound = true;
            if( ((count > 1) || (nonNatFound)) && (storeWizardJButton.getParent()!=null) )
                rackViewJPanel.remove(storeWizardJButton);
            //if( (coreRackMap.size() + utilRackMap.size() + policyCount) > 1 )
            //  Util.getMPipelineJPanel().setStoreWizardButtonVisible(false);
        }});
    }
    ///////////////////////////////////////
    // ADD API ////////////////////////////


    // Private CLASSES & UTILS /////////////////////
    ////////////////////////////////////////////////
    public synchronized MCasingJPanel[] loadAllCasings(boolean generateGuis){
        if( generateGuis )
            return doIt(true);
        else{
            (new Thread(new DoItRunnable())).start();
            return null;
        }
    }

    private List<MNodeJPanel> getAllNodePanels() {
        List<MNodeJPanel> panels = new LinkedList<MNodeJPanel>();

        /* Add all of the panels from the various racks */
        for( Map<ButtonKey,MNodeJPanel> policyMap : policyRackMap.values()){
            panels.addAll( policyMap.values());
        }
        
        /* Add all of the panels from the util rack (does this exist anymore?) */
        panels.addAll(utilRackMap.values());

        /* Add all of the panels from the core rack (does this exist anymore?) */
        panels.addAll(coreRackMap.values());
        
        return panels;
    }
    private class DoItRunnable implements Runnable {
        public void run(){ doIt(false); }
    }
    private MCasingJPanel[] doIt(boolean generateGuis){
        final String casingNames[] = {"untangle-casing-http", "untangle-casing-mail", "untangle-casing-ftp"};
        Vector<MCasingJPanel> mCasingJPanels = new Vector<MCasingJPanel>();
        List<Tid> casingInstances = null;
        NodeContext nodeContext = null;
        NodeDesc nodeDesc = null;
        String casingGuiClassName = null;
        Class casingGuiClass = null;
        Constructor casingGuiConstructor = null;
        MCasingJPanel mCasingJPanel = null;
        for(String casingName : casingNames){
            try{
                casingInstances = Util.getNodeManager().nodeInstances(casingName);
                if( casingInstances.size() == 0 )
                    continue;
                nodeContext = Util.getNodeManager().nodeContext(casingInstances.get(0));
                nodeDesc = nodeContext.getNodeDesc();
                casingGuiClassName = nodeDesc.getGuiClassName();
                casingGuiClass = Util.getClassLoader().mLoadClass( casingGuiClassName );
                if(generateGuis){
                    casingGuiConstructor = casingGuiClass.getConstructor(new Class[]{});
                    mCasingJPanel = (MCasingJPanel) casingGuiConstructor.newInstance(new Object[]{});
                    mCasingJPanels.add(mCasingJPanel);
                }
            }
            catch(Exception e){
                Util.handleExceptionNoRestart("Error loading all casings: " + casingName, e);
            }
        }
        return mCasingJPanels.toArray( new MCasingJPanel[0] );
    }

    private boolean isMackageVisible(MackageDesc mackageDesc){
        if( mackageDesc.getViewPosition() < 0 )
            return false;
        else
            return true;
    }
    private boolean isMackageStoreItem(MackageDesc mackageDesc){
        if( MackageDesc.Type.LIB_ITEM == mackageDesc.getType())
            return true;
        else
            return false;
    }

    private boolean isMackageTrial(MackageDesc mackageDesc){
        return MackageDesc.Type.TRIAL == mackageDesc.getType();
    }

    private boolean isMackageNode(MackageDesc mackageDesc){
        return MackageDesc.Type.NODE == mackageDesc.getType();
    }

    private boolean isMackageCasing(MackageDesc mackageDesc){
        return MackageDesc.Type.CASING == mackageDesc.getType();
    }


    private class PolicyRenderer implements ListCellRenderer{
        private ListCellRenderer listCellRenderer;
        public PolicyRenderer(ListCellRenderer listCellRenderer){
            this.listCellRenderer = listCellRenderer;
        }
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus){
            String text = (value instanceof Policy?((Policy)value).getName():value.toString());
            Component renderComponent = listCellRenderer.getListCellRendererComponent(list, text, index, isSelected, hasFocus);
            renderComponent.setForeground(Color.BLACK);

            //((JLabel)renderComponent).setBorder(new javax.swing.border.LineBorder(new Color(130,130,130), 1));

            return renderComponent;
        }
    }
    private class StoreActionListener implements java.awt.event.ActionListener {
        private MNodeJButton mNodeJButton;
        public StoreActionListener(MNodeJButton mNodeJButton){
            this.mNodeJButton = mNodeJButton;
        }
        public void actionPerformed(java.awt.event.ActionEvent evt){
            if( Util.getIsDemo() )
                return;
            if( Util.mustCheckUpgrades() ){
                new StoreCheckJDialog( Util.getMMainJFrame() );
            }
            if( Util.getUpgradeCount() != 0 )
                return;
            try{
                String authNonce = Util.getRemoteAdminManager().generateAuthNonce();
                URL newURL = new URL( Util.getServerCodeBase(), "../library/libitem.php?name="
                                      + mNodeJButton.getName() + "&" + authNonce);
                ((BasicService) ServiceManager.lookup("javax.jnlp.BasicService")).showDocument(newURL);
            }
            catch(Exception f){
                Util.handleExceptionNoRestart("error launching browser for Library", f);
                MOneButtonJDialog.factory(Util.getMMainJFrame(), "",
                                          "A problem occurred while trying to access the Library."
                                          + "<br>Please contact Untangle Support.",
                                          "Untangle Library Warning", "");
            }
        }
    }

    private class ToolboxActionListener implements java.awt.event.ActionListener {
        private Policy policy;
        private MNodeJButton mNodeJButton;
        public ToolboxActionListener(Policy policy, MNodeJButton mNodeJButton){
            this.policy = policy;
            this.mNodeJButton = mNodeJButton;
        }
        public void actionPerformed(java.awt.event.ActionEvent evt){
            if( Util.getIsDemo() )
                return;
            if( (evt.getModifiers() & ActionEvent.SHIFT_MASK) > 0){
                new MoveFromToolboxToStoreThread(mNodeJButton);
            }
            else{
                new MoveFromToolboxToRackThread(policy,mNodeJButton);
            }
        }
    }


    //////////////////////////////////////
    // PRIVATE CLASSES AND UTILS /////////

    private void updatePremiumStatus(){
        boolean premiumEnabled = (null==Util.getRemoteToolboxManager().mackageDesc("untangle-libitem-update-service"));
        Util.getMPipelineJPanel().setPremiumEnabled(premiumEnabled);
    }
    
    public void stopAllGraphs(){
        for ( MNodeJPanel mNodeJPanel : getAllNodePanels()) {
            mNodeJPanel.mNodeDisplayJPanel().setDoVizUpdates(false);
        }
    }
    private void focusInRack(final MNodeJPanel mNodeJPanel){
        SwingUtilities.invokeLater( new Runnable() { public void run() {
            if( (mNodeJPanel.getParent() != policyRackJPanelMap.get(selectedPolicy))
                && (mNodeJPanel.getParent() != coreRackJPanel) ) // the selected policy is not the policy of the app
                return;
            rackJScrollPane.getViewport().validate();
            Rectangle scrollRect = SwingUtilities.convertRectangle(mNodeJPanel.getParent(),
                                                                   mNodeJPanel.getBounds(),
                                                                   rackJScrollPane.getViewport());
            scrollRect.y -= 20;
            scrollRect.height += 40;
            rackJScrollPane.getViewport().scrollRectToVisible(scrollRect);
            mNodeJPanel.highlight();
        }});
    }
    private void focusInToolbox(final MNodeJButton mNodeJButton, final boolean doHighlight){
        SwingUtilities.invokeLater( new Runnable() { public void run() {
            MNodeJButton focusMNodeJButton;
            ButtonKey buttonKey = new ButtonKey(mNodeJButton);
            if( mNodeJButton.getMackageDesc().isUtil() || mNodeJButton.getMackageDesc().isService() ){
                focusMNodeJButton = utilToolboxMap.get(buttonKey);
            }
            else if( mNodeJButton.getMackageDesc().isSecurity() ){
                focusMNodeJButton = policyToolboxMap.get(selectedPolicy).get(buttonKey);
            }
            else if( mNodeJButton.getMackageDesc().isCore() ){
                focusMNodeJButton = coreToolboxMap.get(buttonKey);
            }
            else{
                focusMNodeJButton = null;
            }
            if( focusMNodeJButton != null )

                actionJTabbedPane.setSelectedIndex(1);
            toolboxJScrollPane.getViewport().validate();
            Rectangle scrollRect = SwingUtilities.convertRectangle(focusMNodeJButton.getParent(),
                                                                   focusMNodeJButton.getBounds(),
                                                                   toolboxJScrollPane.getViewport());
            toolboxJScrollPane.getViewport().scrollRectToVisible(scrollRect);
            if( doHighlight )
                focusMNodeJButton.highlight();
        } } );
    }



}
