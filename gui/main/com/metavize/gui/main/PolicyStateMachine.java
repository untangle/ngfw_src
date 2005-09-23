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

package com.metavize.gui.main;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.*;
import java.lang.reflect.*;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Collection;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.border.*;

import com.metavize.gui.configuration.*;
import com.metavize.gui.pipeline.*;
import com.metavize.gui.store.*;
import com.metavize.gui.transform.*;
import com.metavize.gui.upgrade.*;
import com.metavize.gui.util.*;
import com.metavize.gui.widgets.dialogs.*;
import com.metavize.mvvm.*;
import com.metavize.mvvm.security.*;
import com.metavize.mvvm.tran.*;
import com.metavize.mvvm.policy.*;

public class PolicyStateMachine implements ActionListener {

    // MVVM DATA MODELS (USED ONLY DURING INIT) //////
    private Map<String,MackageDesc> purchasableMackageMap;
    private Map<String,MackageDesc> installedMackageMap;
    private Map<Policy,List<Tid>> policyTidMap;
    private Map<Policy,Map<String,Object>> policyNameMap;
    // GUI DATA MODELS /////////
    private Map<ButtonKey,MTransformJButton> storeMap;
    private Map<Policy,Map<ButtonKey,MTransformJButton>> policyToolboxMap;
    private Map<Policy,Map<ButtonKey,MTransformJPanel>> policyRackMap;
    // GUI VIEW MODELS //////////
    private JPanel storeJPanel;
    private JScrollPane toolboxJScrollPane;
    private JPanel rackViewJPanel;
    private Map<Policy,JPanel> policyToolboxJPanelMap;
    private Map<Policy,JPanel> policyRackJPanelMap;
    // MISC REFERENCES ////////
    private JButton policyManagerJButton;
    private JTabbedPane actionJTabbedPane;
    private JComboBox viewSelector;
    private Policy selectedPolicy;
    private JPanel lastRackJPanelSelected;
    private int lastScrollPosition = -1;
    private volatile static int applianceLoadProgress = 0;
    // CONSTANTS /////////////
    private GridBagConstraints buttonGridBagConstraints;
    private GridBagConstraints applianceGridBagConstraints;
    private GridBagConstraints rackGridBagConstraints;
    // DOWNLOAD DELAYS //////////////
    private static final int DOWNLOAD_INITIAL_SLEEP_MILLIS = 3000;
    private static final int DOWNLOAD_SLEEP_MILLIS = 500;
    private static final int DOWNLOAD_FINAL_SLEEP_MILLIS = 3000;

    public PolicyStateMachine(JTabbedPane actionJTabbedPane, JComboBox viewSelector, JPanel rackViewJPanel,
			      JScrollPane toolboxJScrollPane, JPanel storeJPanel, JButton policyManagerJButton) {
	// MVVM DATA MODELS
	purchasableMackageMap = new HashMap<String,MackageDesc>();
	installedMackageMap = new HashMap<String,MackageDesc>();
	policyTidMap = new HashMap<Policy,List<Tid>>();
	policyNameMap = new HashMap<Policy,Map<String,Object>>();
	// GUI DATA MODELS
        storeMap = new TreeMap<ButtonKey,MTransformJButton>();
	policyToolboxMap = new HashMap<Policy,Map<ButtonKey,MTransformJButton>>();
	policyRackMap = new HashMap<Policy,Map<ButtonKey,MTransformJPanel>>();
	// GUI VIEW MODELS
	this.storeJPanel = storeJPanel;
	this.toolboxJScrollPane = toolboxJScrollPane;
	this.rackViewJPanel = rackViewJPanel;
	this.policyManagerJButton = policyManagerJButton;
	policyToolboxJPanelMap = new HashMap<Policy,JPanel>();
	policyRackJPanelMap = new HashMap<Policy,JPanel>();
	// MISC REFERENCES
	this.actionJTabbedPane = actionJTabbedPane;
	this.viewSelector = viewSelector;
	// CONSTANTS
        buttonGridBagConstraints = new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1d, 0d,
							  GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
							  new Insets(0,1,3,3), 0, 0);
        applianceGridBagConstraints = new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0d, 0d,
							     GridBagConstraints.CENTER, GridBagConstraints.NONE,
							     new Insets(1,0,0,0), 0, 0);
        rackGridBagConstraints = new GridBagConstraints(0, 1, 1, 1, 0d, 0d,
							GridBagConstraints.NORTH, GridBagConstraints.NONE,
							new Insets(0,0,0,12), 0, 0);
	
	try{
	    // LET THE FUN BEGIN
	    initMvvmModel();
	    // the order of the following three is based on their output to the progress bar, thats all
	    initRackModel(Util.getStatusJProgressBar());
	    initToolboxModel(Util.getStatusJProgressBar());
	    initStoreModel(Util.getStatusJProgressBar());
	    initViewSelector();	    
	    // CACHING OF CASING CLASSES SO THE PROTOCOL SETTINGS DIALOG LOADS FASTER
	    loadAllCasings(false);
	}
	catch(Exception e){
	    try{ Util.handleExceptionWithRestart("Error instantiating policy model", e); }
	    catch(Exception f){ Util.handleExceptionNoRestart("Error instantiating policy model", f); }
	}

	// CHOOSE A DEFAULT ACTIONBAR POSITION
	if( storeMap.size() > 0 )
	    actionJTabbedPane.setSelectedIndex(0);
	else
	    actionJTabbedPane.setSelectedIndex(1);

	Util.setPolicyStateMachine(this);
    }
    
    // HANDLERS ///////////////////////////////////////////
    ///////////////////////////////////////////////////////
    public synchronized void actionPerformed(ActionEvent actionEvent){
	if( actionEvent.getSource().equals(viewSelector) ){
	    handleViewSelector();
	}
	else if(actionEvent.getSource().equals(policyManagerJButton)){
	    handlePolicyManagerJButton();
	}
    }
    private void handleViewSelector(){
	Policy currentPolicy = (Policy) viewSelector.getSelectedItem();
	if( currentPolicy.equals(selectedPolicy) )
	    return;
	JPanel toolboxJPanel = policyToolboxJPanelMap.get(currentPolicy);
	int currentScrollPosition = toolboxJScrollPane.getVerticalScrollBar().getValue();
	toolboxJScrollPane.setViewportView( toolboxJPanel );
	toolboxJPanel.revalidate();
	toolboxJScrollPane.repaint();
	if( lastScrollPosition >= 0 )
	    toolboxJScrollPane.getVerticalScrollBar().setValue( currentScrollPosition );
	lastScrollPosition = currentScrollPosition;
	JPanel rackJPanel = policyRackJPanelMap.get(currentPolicy);
	int lastScrollPosition = 0;
	if( lastRackJPanelSelected != null )
	    rackViewJPanel.remove( lastRackJPanelSelected );
	lastRackJPanelSelected = rackJPanel;
	rackViewJPanel.add( rackJPanel, rackGridBagConstraints );
	rackJPanel.revalidate();
	rackViewJPanel.repaint();
	//if( selectedPolicy == null )
	//    System.err.println("Policy Rack view set:" + currentPolicy.getName());
	//else
	//    System.err.println("Policy Rack view changed: " + selectedPolicy.getName() + " -> " + currentPolicy.getName());
	selectedPolicy = currentPolicy;
    }
    private void handlePolicyManagerJButton() {
	try{
	    policyManagerJButton.setEnabled(false);
	    PolicyJDialog policyJDialog = new PolicyJDialog();
	    policyJDialog.setVisible(true);
	    updatePolicyRacks();
	}
	catch(Exception e){
	    try{ Util.handleExceptionWithRestart("Error handling policy manager action", e); }
	    catch(Exception f){ Util.handleExceptionNoRestart("Error handling policy manager action", f); }
	}
	finally{
	    policyManagerJButton.setEnabled(true);
	}
    }
    ///////////////////////////////////////////////////////
    // HANDLERS ///////////////////////////////////////////


    // POLICY UPDATING ////////////////////////////////////
    ///////////////////////////////////////////////////////
    private void updatePolicyRacks() throws Exception {
	// BUILD A GUI MODEL AND MVVM MODEL
	Map<Policy,Object> currentPolicyRacks = new HashMap<Policy,Object>();
	Map<Policy,Object> newPolicyRacks = new HashMap<Policy,Object>();
	for(int i=0; i<((DefaultComboBoxModel)viewSelector.getModel()).getSize(); i++)
	    currentPolicyRacks.put( (Policy) ((DefaultComboBoxModel)viewSelector.getModel()).getElementAt(i), null );
	for( Policy policy : (List<Policy>) Util.getPolicyManager().getPolicyConfiguration().getPolicies() ){
	    newPolicyRacks.put( policy, null );
	}
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
	Policy selectedPolicy = (Policy) viewSelector.getSelectedItem();
	DefaultComboBoxModel defaultComboBoxModel = new DefaultComboBoxModel();
	for( Policy newPolicy : newPolicyRacks.keySet() ){
	    defaultComboBoxModel.addElement(newPolicy);
	    if( selectedPolicy.equals(newPolicy) ){
		defaultComboBoxModel.setSelectedItem(newPolicy);
	    }
	}
	if( defaultComboBoxModel.getSelectedItem() == null )
	    defaultComboBoxModel.setSelectedItem( defaultComboBoxModel.getElementAt(0)  );
	viewSelector.setModel(defaultComboBoxModel);
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
	    policyToolboxMap.put(policy, new TreeMap<ButtonKey,MTransformJButton>());
	    policyRackMap.put(policy,new TreeMap<ButtonKey,MTransformJPanel>());
	    // ADD TO GUI VIEW MODEL
	    JPanel toolboxJPanel = new JPanel();
	    toolboxJPanel.setLayout(new GridBagLayout());
	    toolboxJPanel.setOpaque(false);
	    policyToolboxJPanelMap.put(policy, toolboxJPanel);
	    JPanel rackJPanel = new JPanel();
	    rackJPanel.setLayout(new GridBagLayout());
	    rackJPanel.setOpaque(false);
	    policyRackJPanelMap.put(policy, rackJPanel);
	    // POPULATE THE TOOLBOX
	    for( Map.Entry<ButtonKey,MTransformJButton> firstPolicyEntry : policyToolboxMap.get(firstPolicy).entrySet() ){
		addToToolbox(policy,firstPolicyEntry.getValue().getMackageDesc(),false);
	    }
	}
    }
    private void removedPolicyRacks(final List<Policy> policies){
	for( Policy policy : policies ){
	    // SHUTDOWN ALL APPLIANCES
	    for( MTransformJPanel mTransformJPanel : policyRackMap.get(policy).values() ){
		mTransformJPanel.doShutdown();
	    }		
	    // REMOVE FROM GUI DATA MODEL
	    policyRackMap.get(policy).clear();
	    policyRackMap.remove(policy);
	    // REMOVE FROM GUI VIEW MODEL
	    policyRackJPanelMap.get(policy).removeAll();
	    policyRackJPanelMap.remove(policy);
	}
    }
    /////////////////////////////////////////
    // POLICY UPDATING //////////////////////

    // TOOLBOX / STORE OPERATIONS //////////
    ////////////////////////////////////////
    private class MoveFromToolboxToRackThread extends Thread {
	private Policy policy;
	private MTransformJButton mTransformJButton;
	public MoveFromToolboxToRackThread(final Policy policy, final MTransformJButton mTransformJButton){
	    this.policy = policy;
	    this.mTransformJButton = mTransformJButton;
	    setContextClassLoader( Util.getClassLoader() );
	    setName("MVCLIENT-MoveFromToolboxToRackThread: " + mTransformJButton.getDisplayName() + " -> " + policy.getName());
	    mTransformJButton.setDeployingView();
	    start();
	}
	public void run(){
	    try{
		// INSTANTIATE IN MVVM
		Tid tid = Util.getTransformManager().instantiate(mTransformJButton.getName(),policy);
		// CREATE APPLIANCE
		TransformContext transformContext = Util.getTransformManager().transformContext( tid );
		MTransformJPanel mTransformJPanel = MTransformJPanel.instantiate(transformContext);
		// DEPLOY APPLIANCE TO CURRENT POLICY RACK
		addToRack(policy, mTransformJPanel);
	    }
	    catch(Exception e){
		try{ Util.handleExceptionWithRestart("Error moving from toolbox to rack", e); }
		catch(Exception f){ 		    
		    Util.handleExceptionNoRestart("Error moving from toolbox to rack", f);
		    mTransformJButton.setFailedDeployView();
		    new MOneButtonJDialog(mTransformJButton.getDisplayName(),
					  "A problem occurred while installing to the rack:<br>"
					  + mTransformJButton.getDisplayName()
					  + "<br>Please contact Metavize support.");
		    return;
		}
	    }
	    // VIEW: DEPLOYED
	    mTransformJButton.setDeployedView();
	    // UPDATE PROTOCOL SETTINGS CACHE
	    loadAllCasings(false);
	}
    }
    public void moveFromRackToToolbox(final Policy policy, final MTransformJPanel mTransformJPanel){
	new MoveFromRackToToolboxThread(policy,mTransformJPanel);
    }
    private class MoveFromRackToToolboxThread extends Thread{
	private Policy policy;
	private MTransformJPanel mTransformJPanel;
	private MTransformJButton mTransformJButton;
	public MoveFromRackToToolboxThread(final Policy policy, final MTransformJPanel mTransformJPanel){
	    this.policy = policy;
	    this.mTransformJPanel = mTransformJPanel;
	    this.mTransformJButton = policyToolboxMap.get(policy).get(new ButtonKey(mTransformJPanel));
	    setContextClassLoader( Util.getClassLoader() );
	    setName("MVCLIENT-MoveFromRackToToolboxThread: " + mTransformJPanel.getMackageDesc().getDisplayName() + " -> " + policy.getName());
	    mTransformJPanel.setRemovingView(false);
	    start();
	}
	public void run(){
	    try{
		// DESTROY IN MVVM
		Util.getTransformManager().destroy(mTransformJPanel.getTid());
		// REMOVE APPLIANCE FROM THE CURRENT POLICY RACK
		mTransformJPanel.doShutdown();
		removeFromRack(mTransformJPanel, policy);
	    }
	    catch(Exception e){
		try{ Util.handleExceptionWithRestart("Error moving from rack to toolbox", e); }
		catch(Exception f){
		    Util.handleExceptionNoRestart("Error moving from rack to toolbox", f);
		    mTransformJPanel.setProblemView(true);
		    mTransformJButton.setFailedRemoveFromRackView();
		    new MOneButtonJDialog(mTransformJPanel.getMackageDesc().getDisplayName(),
					  "A problem occurred while removing from the rack:<br>"
					  + mTransformJPanel.getMackageDesc().getDisplayName()
					  + "<br>Please contact Metavize support.");
		    return;
		}
	    }
	    // VIEW: DEPLOYABLE
	    ButtonKey buttonKey = new ButtonKey(mTransformJPanel);
	    policyToolboxMap.get(policy).get(buttonKey).setDeployableView();
	}
    }
    private class MoveFromToolboxToStoreThread extends Thread{
	private MTransformJButton mTransformJButton;
	private Vector<MTransformJButton> buttonVector;
	public MoveFromToolboxToStoreThread(final MTransformJButton mTransformJButton){
	    this.mTransformJButton = mTransformJButton;
	    ButtonKey buttonKey = new ButtonKey(mTransformJButton);
	    buttonVector = new Vector<MTransformJButton>();
	    setContextClassLoader( Util.getClassLoader() );
	    setName("MVCLIENT-MoveFromToolboxToStoreThread: " + mTransformJButton.getDisplayName() );
	    // DECIDE IF WE CAN REMOVE
	    for( Map.Entry<Policy,Map<ButtonKey,MTransformJButton>> policyToolboxMapEntry : policyToolboxMap.entrySet() ){
		Map<ButtonKey,MTransformJButton> toolboxMap = policyToolboxMapEntry.getValue();		
		if( toolboxMap.containsKey(buttonKey) && toolboxMap.get(buttonKey).isEnabled() ){
		    buttonVector.add( toolboxMap.get(buttonKey) );
		}
		else{
		    new MOneButtonJDialog(mTransformJButton.getDisplayName(),
					  mTransformJButton.getDisplayName()
					  + " cannot be removed from the toolbox because it is being"
					  + " used by the following policy rack:<br>"
					  + policyToolboxMapEntry.getKey().getName()
					  + "<br><br>You must remove the appliance from all policy racks first.");
		    return;
		}
	    }
	    for( MTransformJButton button : buttonVector )
		button.setRemovingFromToolboxView();
	    start();
	}
	public void run(){
	    try{
		// UNINSTALL IN MVVM
		Util.getToolboxManager().uninstall(mTransformJButton.getName());
		// REMOVE FROM TOOLBOX
		removeFromToolbox(mTransformJButton.getMackageDesc());
		// ADD TO STORE
		addToStore(mTransformJButton.getMackageDesc());
	    }
	    catch(Exception e){
		try{ Util.handleExceptionWithRestart("Error moving from toolbox to store", e); }
		catch(Exception f){
		    Util.handleExceptionNoRestart("Error moving from toolbox to store", f);
		    mTransformJButton.setFailedRemoveFromToolboxView();
		    new MOneButtonJDialog(mTransformJButton.getDisplayName(),
					  "A problem occurred while removing from the toolbox:<br>"
					  + mTransformJButton.getDisplayName()
					  + "<br>Please contact Metavize support.");
		    return;
		}
	    }
	}
    }
    public Thread moveFromStoreToToolbox(final MTransformJButton mTransformJButton, final JProgressBar progressBar, final JDialog dialog){
	return new MoveFromStoreToToolboxThread(mTransformJButton, progressBar, dialog);
    }
    private class MoveFromStoreToToolboxThread extends Thread{
	private MTransformJButton mTransformJButton;
	private JProgressBar progressBar;
	private JDialog dialog;
	public MoveFromStoreToToolboxThread(final MTransformJButton mTransformJButton, final JProgressBar progressBar, final JDialog dialog){
	    this.mTransformJButton = mTransformJButton;
	    this.progressBar = progressBar;
	    this.dialog = dialog;
	    ButtonKey buttonKey = new ButtonKey(mTransformJButton);
	    setContextClassLoader( Util.getClassLoader() );
	    setName("MVCLIENT-MoveFromStoreToToolboxThread: " + mTransformJButton.getDisplayName() );
	    mTransformJButton.setProcuringView();
	    start();
	}
	public void run(){
	    try{
		// WARM UP
		SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
		    progressBar.setValue(0);
		    progressBar.setString("Starting download...");
		    progressBar.setIndeterminate(true);		    
		}});
		Thread.currentThread().sleep(DOWNLOAD_INITIAL_SLEEP_MILLIS);                		
		// DO THE DOWNLOAD AND INSTALL
		SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
		    progressBar.setIndeterminate(false);
		}});
                long key = Util.getToolboxManager().install(mTransformJButton.getName());
		com.metavize.gui.util.Visitor visitor = new com.metavize.gui.util.Visitor(progressBar);
		while (!visitor.isDone()) {
		    java.util.List<InstallProgress> lip = Util.getToolboxManager().getProgress(key);
		    for (InstallProgress ip : lip) {
			ip.accept(visitor);
		    }
		    if (0 == lip.size()) {
			Thread.currentThread().sleep(DOWNLOAD_SLEEP_MILLIS);
		    }
		}
		// GIVE OPTIONS BASED ON RESULTS
		if( visitor.isSuccessful() ){
		    Thread.currentThread().sleep(DOWNLOAD_FINAL_SLEEP_MILLIS);
		    SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
			dialog.setVisible(false);
		    }});
		    removeFromStore(mTransformJButton.getMackageDesc());
		    for( Policy policy : policyToolboxMap.keySet() )
			addToToolbox(policy,mTransformJButton.getMackageDesc(),false);
		}
		else{
		    SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
			progressBar.setValue(0);
			((StoreJDialog)dialog).resetButtons();
		    }});
		    mTransformJButton.setFailedProcureView();
		    new MOneButtonJDialog(mTransformJButton.getDisplayName(),
					  "A problem occurred while purchasing:<br>"
					  + mTransformJButton.getDisplayName()
					  + "<br>Please contact Metavize for assistance.");
		}				
	    }
	    catch(Exception e){
		try{
		    Util.handleExceptionWithRestart("error purchasing transform: " +  mTransformJButton.getName(),  e);
		}
		catch(Exception f){
		    Util.handleExceptionNoRestart("Error purchasing transform:", f);
		    mTransformJButton.setFailedProcureView();
                    SwingUtilities.invokeLater( new Runnable(){ public void run(){
                        progressBar.setString("Purchase problem occurred...");
                        progressBar.setValue(0);
                        dialog.setVisible(false);
                        new MOneButtonJDialog(mTransformJButton.getDisplayName(),
					      "A problem occurred while purchasing:<br>"
					      + mTransformJButton.getDisplayName()
					      + "<br>Please contact Metavize for assistance.");
                    }});		    
		}
	    }	    	    
	}
    }
    ///////////////////////////////////////////////////////
    // TOOLBOX / STORE OPERATIONS /////////////////////////


    // INIT API ////////////////////////////////////////////
    ////////////////////////////////////////////////////////
    private void initMvvmModel() throws Exception {
	for( MackageDesc mackageDesc : Util.getToolboxManager().uninstalled() )
	    purchasableMackageMap.put(mackageDesc.getName(),mackageDesc);
	for( MackageDesc mackageDesc : Util.getToolboxManager().installed() )
	    installedMackageMap.put(mackageDesc.getName(),mackageDesc);
	for( Policy policy : Util.getPolicyManager().getPolicies() )
	    policyTidMap.put( policy, Util.getTransformManager().transformInstances(policy) );
	for( Policy policy : policyTidMap.keySet() ){
	    Map<String,Object> nameMap = new HashMap<String,Object>();
	    policyNameMap.put(policy,nameMap);
	    for( Tid tid : policyTidMap.get(policy) ){
		nameMap.put(tid.getTransformName(),null);
	    }
	}
    }
    private void initStoreModel(final JProgressBar progressBar){
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    progressBar.setValue(80);
	    progressBar.setString("Populating Store...");
	}});
	int progress = 0;
	for( MackageDesc mackageDesc : purchasableMackageMap.values() ){
	    addToStore(mackageDesc);
	    progress++;
	    final float progressFinal = (float) progress;
	    final float overallFinal = (float) purchasableMackageMap.size();
	    SwingUtilities.invokeLater( new Runnable(){ public void run(){
		progressBar.setValue(80 + (int) (16f*progressFinal/overallFinal) );
	    }});
	}
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    progressBar.setValue(96);
	}});
    }
    private void initToolboxModel(final JProgressBar progressBar){
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    progressBar.setValue(64);
	    progressBar.setString("Populating Toolbox...");
	}});
	int progress = 0;
	for( Policy policy : policyTidMap.keySet() ){
	    JPanel toolboxJPanel = new JPanel();
	    toolboxJPanel.setLayout(new GridBagLayout());
	    toolboxJPanel.setOpaque(false);
	    policyToolboxJPanelMap.put(policy, toolboxJPanel);
	    policyToolboxMap.put(policy, new TreeMap<ButtonKey,MTransformJButton>());
	    for( MackageDesc mackageDesc : installedMackageMap.values() ){
		if( !isMackageVisible(mackageDesc) )
		    continue;
		boolean isDeployed = policyNameMap.get(policy).containsKey(mackageDesc.getName());
		addToToolbox(policy,mackageDesc,isDeployed);
		progress++;
	    }
	    final float progressFinal = (float) progress;
	    final float overallFinal = (float) (installedMackageMap.size() * policyTidMap.size());
	    SwingUtilities.invokeLater( new Runnable(){ public void run(){
		progressBar.setValue(64 + (int) (16f*progressFinal/overallFinal) );
	    }});
	}
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    progressBar.setValue(80);
	}});
    }
    private void initRackModel(final JProgressBar progressBar){
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    progressBar.setValue(16);
	    if( policyTidMap.keySet().size() == 1 )
		progressBar.setString("Populating Rack...");
	    else
		progressBar.setString("Populating Racks...");
	}});
	int overall = 0;
	for( Policy policy : policyTidMap.keySet() )
	    for( Tid tid : policyTidMap.get(policy) )
		overall++;
	for( Policy policy : policyTidMap.keySet() ){
	    JPanel rackJPanel = new JPanel();
	    rackJPanel.setLayout(new GridBagLayout());
	    rackJPanel.setOpaque(false);
	    policyRackJPanelMap.put(policy,rackJPanel);
	    policyRackMap.put(policy,new TreeMap<ButtonKey,MTransformJPanel>());
	    for( Tid tid : policyTidMap.get(policy) ){
		new LoadApplianceThread(policy,tid,overall,progressBar);
	    }
	}
	try{
	    while( applianceLoadProgress < overall )
		Thread.currentThread().sleep(100);
	}
	catch(Exception e){ Util.handleExceptionNoRestart("Error sleeping while appliances loading",e); }
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
	    this.policy = policy;
	    this.tid = tid;
	    this.overallProgress = overallProgress;
	    this.progressBar = progressBar;
	    setName("MVCLIENT-LoadApplianceThread: " + tid.getId());
	    setContextClassLoader( Util.getClassLoader() );
	    start();
	}
	public void run(){
	    // GET THE TRANSFORM CONTEXT AND MACKAGE DESC
	    TransformContext transformContext = Util.getTransformManager().transformContext( tid );
	    MackageDesc mackageDesc = transformContext.getMackageDesc();
	    if( isMackageVisible(mackageDesc) ){
		// CONSTRUCT AND ADD THE APPLIANCE
		try{
		    final MTransformJPanel mTransformJPanel = MTransformJPanel.instantiate(transformContext);
		    addToRack(policy,mTransformJPanel);
		}
		catch(Exception e){
		    try{ Util.handleExceptionWithRestart("Error instantiating appliance", e); }
		    catch(Exception f){ Util.handleExceptionNoRestart("Error instantiating appliance", f); }		    
		}
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
	for( Policy policy : policyTidMap.keySet() ){
	    newModel.addElement(policy);
	}
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
	final MTransformJButton mTransformJButton = storeMap.get(buttonKey);
        SwingUtilities.invokeLater( new Runnable() { public void run() {
	    for( ActionListener actionListener : mTransformJButton.getActionListeners() )
		mTransformJButton.removeActionListener(actionListener);
	    storeMap.remove(buttonKey);
	    storeJPanel.remove(mTransformJButton);
	    storeJPanel.revalidate();
	}});
    }
    private void removeFromToolbox(final MackageDesc mackageDesc){
	final ButtonKey buttonKey = new ButtonKey(mackageDesc);
        SwingUtilities.invokeLater( new Runnable() { public void run() {
	    for( Policy policy : policyToolboxMap.keySet() ){
		Map<ButtonKey,MTransformJButton> toolboxMap = policyToolboxMap.get(policy);
		final int position = ((TreeMap)toolboxMap).headMap(buttonKey).size();
		MTransformJButton mTransformJButton = toolboxMap.get(buttonKey);
		for( ActionListener actionListener : mTransformJButton.getActionListeners() )
		    mTransformJButton.removeActionListener(actionListener);
		toolboxMap.remove(buttonKey);
		JPanel toolboxJPanel = policyToolboxJPanelMap.get(policy);
		toolboxJPanel.remove(position);
		toolboxJPanel.revalidate();
	    }
	}});
    }	
    private void removeFromRack(final MTransformJPanel mTransformJPanel, final Policy policy){
	final ButtonKey buttonKey = new ButtonKey(mTransformJPanel);
	final JPanel rackJPanel = policyRackJPanelMap.get(policy);
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    // REMOVE FROM RACK MODEL
	    policyRackMap.get(policy).remove(buttonKey);
	    // REMOVE FROM RACK VIEW
	    rackJPanel.remove(mTransformJPanel);
	    rackJPanel.revalidate();
	}});
    }
    ///////////////////////////////////////
    // REMOVE API /////////////////////////


    // ADD API ////////////////////////////
    ///////////////////////////////////////
    private void addToStore(final MackageDesc mackageDesc){
	// ONLY UPDATE GUI MODELS IF THIS IS VISIBLE
	if( !isMackageVisible(mackageDesc) )
	    return;
	final ButtonKey buttonKey = new ButtonKey(mackageDesc);
	final MTransformJButton mTransformJButton = new MTransformJButton(mackageDesc);
	mTransformJButton.setProcurableView(); // xxx i believe this is safe because it ends up on the EDT
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    // UPDATE GUI DATA MODEL
	    storeMap.put(buttonKey, mTransformJButton);
	    mTransformJButton.addActionListener( new StoreActionListener(mTransformJButton) );
	    // UPDATE GUI VIEW MODEL
	    int position = ((TreeMap)storeMap).headMap(buttonKey).size();
	    storeJPanel.add(mTransformJButton, buttonGridBagConstraints, position);
	    storeJPanel.revalidate();
	}});
	//System.err.println("Added to store: " + mackageDesc.getDisplayName());
    }
    private void addToToolbox(final Policy policy, final MackageDesc mackageDesc, final boolean isDeployed){
	// ONLY UPDATE GUI MODELS IF THIS IS VISIBLE
	if( !isMackageVisible(mackageDesc) )
	    return;
	final ButtonKey buttonKey = new ButtonKey(mackageDesc);
	final MTransformJButton mTransformJButton = new MTransformJButton(mackageDesc);
	final JPanel toolboxJPanel = policyToolboxJPanelMap.get(policy);
	final Map<ButtonKey,MTransformJButton> toolboxMap = policyToolboxMap.get(policy);
	if( isDeployed )
	    mTransformJButton.setDeployedView();
	else
	    mTransformJButton.setDeployableView();
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    // UPDATE GUI DATA MODEL
	    toolboxMap.put(buttonKey,mTransformJButton);
	    mTransformJButton.addActionListener( new ToolboxActionListener(policy,mTransformJButton) );
	    // UPDATE GUI VIEW MODEL
	    int position = ((TreeMap)toolboxMap).headMap(buttonKey).size();
	    toolboxJPanel.add(mTransformJButton, buttonGridBagConstraints, position);
	    toolboxJPanel.revalidate();				    
	}});
	//System.err.println("Added to toolbox (" + policy.getName() + "): " + mackageDesc.getDisplayName() + " deployed: " + isDeployed);
    }
    private synchronized void addToRack(final Policy policy, final MTransformJPanel mTransformJPanel){
	final ButtonKey buttonKey = new ButtonKey(mTransformJPanel);
	final JPanel rackJPanel = policyRackJPanelMap.get(policy);
	SwingUtilities.invokeLater( new Runnable() { public void run() {
	    // ADD TO RACK MODEL
	    policyRackMap.get(policy).put(buttonKey,mTransformJPanel);
	    // UPDATE GUI VIEW MODEL
	    int position = ((TreeMap)policyRackMap.get(policy)).headMap(buttonKey).size();
	    rackJPanel.add(mTransformJPanel, applianceGridBagConstraints, position);
	    rackJPanel.revalidate();		
	}});
	//System.err.println("Added to rack (" + policy.getName() + "): " + mTransformJPanel.getMackageDesc().getDisplayName() );
    }
    ///////////////////////////////////////
    // ADD API ////////////////////////////


    // Private CLASSES & UTILS /////////////////////
    ////////////////////////////////////////////////
    public synchronized MCasingJPanel[] loadAllCasings(boolean generateGuis){
	final String casingNames[] = {"mail-casing", "http-casing", "ftp-casing"};
	Vector<MCasingJPanel> mCasingJPanels = new Vector<MCasingJPanel>();
	List<Tid> casingInstances = null;
	TransformContext transformContext = null;
	TransformDesc transformDesc = null;
	String casingGuiClassName = null;
	Class casingGuiClass = null;
	Constructor casingGuiConstructor = null;
	MCasingJPanel mCasingJPanel = null;
        for(String casingName : casingNames){
	    try{
		casingInstances = Util.getTransformManager().transformInstances(casingName);
		if( casingInstances.size() == 0 )
		    continue;
		transformContext = Util.getTransformManager().transformContext(casingInstances.get(0));
		transformDesc = transformContext.getTransformDesc();
		casingGuiClassName = transformDesc.getGuiClassName();
		casingGuiClass = Util.getClassLoader().loadClass( casingGuiClassName, casingName );
		if(generateGuis){
		    casingGuiConstructor = casingGuiClass.getConstructor(new Class[]{TransformContext.class});
		    mCasingJPanel = (MCasingJPanel) casingGuiConstructor.newInstance(new Object[]{transformContext});
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
	if( mackageDesc.getType() != MackageDesc.TRANSFORM_TYPE )
	    return false;
	else if( mackageDesc.getRackPosition() < 0 )
	    return false;
	else
	    return true;
    }
    private class PolicyRenderer implements ListCellRenderer{
	private ListCellRenderer listCellRenderer;
	public PolicyRenderer(ListCellRenderer listCellRenderer){
	    this.listCellRenderer = listCellRenderer;
	}
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus){
	    return listCellRenderer.getListCellRendererComponent(list, ((Policy)value).getName(), index, isSelected, hasFocus);
	}
    }
    private class StoreActionListener implements java.awt.event.ActionListener {
	private MTransformJButton mTransformJButton;
	public StoreActionListener(MTransformJButton mTransformJButton){
	    this.mTransformJButton = mTransformJButton;
	}
	public void actionPerformed(java.awt.event.ActionEvent evt){
	    // ASK IF THY REALLY WANT THE STUFF
	    mTransformJButton.setEnabled(false);
	    StoreJDialog storeJDialog = new StoreJDialog(mTransformJButton);
	    storeJDialog.setVisible(true);
	    if( storeJDialog.getPurchasedMTransformJButton() == null ){
		mTransformJButton.setEnabled(true);
	    }
	}
    }
    private class ToolboxActionListener implements java.awt.event.ActionListener {
	private Policy policy;
	private MTransformJButton mTransformJButton;
	public ToolboxActionListener(Policy policy, MTransformJButton mTransformJButton){
	    this.policy = policy;
	    this.mTransformJButton = mTransformJButton;
	}
	public void actionPerformed(java.awt.event.ActionEvent evt){
	    if( (evt.getModifiers() & ActionEvent.SHIFT_MASK) > 0)
		new MoveFromToolboxToStoreThread(mTransformJButton);
	    else
		new MoveFromToolboxToRackThread(policy,mTransformJButton);
	}
    }
    //////////////////////////////////////
    // PRIVATE CLASSES AND UTILS /////////

    /*    
    public synchronized void focusMTransformJPanel(java.awt.Rectangle newBounds){
        if(newBounds == null)
            return;
	newBounds.x += transformJPanel.getX();
        newBounds.y += transformJPanel.getY() - 1;
        newBounds.x -= mPipelineJScrollPane.getViewport().getViewPosition().x;
        newBounds.y -= mPipelineJScrollPane.getViewport().getViewPosition().y;
        mPipelineJScrollPane.getViewport().scrollRectToVisible(newBounds);
    }

    public synchronized void focusInToolbox(final MTransformJButton mTransformJButton){
        SwingUtilities.invokeLater( new Runnable() { public void run() {
	    MMainJFrame.this.toolboxJScrollPane.validate();
	    MMainJFrame.this.mTabbedPane.setSelectedIndex(1);
	    Rectangle scrollRect = SwingUtilities.convertRectangle(mTransformJButton,
								   mTransformJButton.getBounds(),
								   MMainJFrame.this.toolboxJScrollPane.getViewport());
	    MMainJFrame.this.toolboxJScrollPane.getViewport().scrollRectToVisible(scrollRect);
	    mTransformJButton.highlight();
	} } );
    }
    
    */

}
