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
import com.metavize.mvvm.*;
import com.metavize.mvvm.security.*;
import com.metavize.mvvm.tran.*;
import com.metavize.mvvm.policy.*;

public class PolicyStateMachine implements ActionListener {

    // MVVM DATA MODELS (USED ONLY DURING INIT) //////
    private List<MackageDesc> purchasableMackageList;
    private List<MackageDesc> installedMackageList;
    private Map<Policy,List<Tid>> policyTidMap;
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
    private JTabbedPane actionJTabbedPane;
    private JComboBox viewSelector;
    private JPanel lastRackJPanelSelected;
    private int lastScrollPosition = -1;
    // CONSTANTS /////////////
    private GridBagConstraints buttonGridBagConstraints;
    private GridBagConstraints applianceGridBagConstraints;
    private GridBagConstraints rackGridBagConstraints;

    public PolicyStateMachine(JTabbedPane actionJTabbedPane, JComboBox viewSelector, JPanel rackViewJPanel,
			      JScrollPane toolboxJScrollPane, JPanel storeJPanel) {
	// MVVM DATA MODELS
	purchasableMackageList = new LinkedList<MackageDesc>();
	installedMackageList = new LinkedList<MackageDesc>();
	policyTidMap = new HashMap<Policy,List<Tid>>();
	// GUI DATA MODELS
        storeMap = new TreeMap<ButtonKey,MTransformJButton>();
	policyToolboxMap = new HashMap<Policy,Map<ButtonKey,MTransformJButton>>();
	policyRackMap = new HashMap<Policy,Map<ButtonKey,MTransformJPanel>>();
	// GUI VIEW MODELS
	this.storeJPanel = storeJPanel;
	this.toolboxJScrollPane = toolboxJScrollPane;
	this.rackViewJPanel = rackViewJPanel;
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
    }
    
    // PUBLIC API /////////////////////////////////////////
    ///////////////////////////////////////////////////////
    public void actionPerformed(ActionEvent e){
	if( e.getSource().equals(viewSelector) ){
	    handleViewSelector();
	}
    }

    ///////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////


    // PUBLIC API /////////////////////////////////////////
    ///////////////////////////////////////////////////////
    public synchronized void addedPolicyRacks(final List<Policy> policies){
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
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
		// ADD TO VIEW SELECTOR
		viewSelector.addItem(policy);
		// POPULATE THE TOOLBOX
		for( Map.Entry<ButtonKey,MTransformJButton> firstPolicyEntry : policyToolboxMap.get(firstPolicy).entrySet() ){
		    addToToolbox(policy,firstPolicyEntry.getValue().getMackageDesc(),false);
		}
	    }
	}});
    }

    public synchronized void removedPolicyRacks(final List<Policy> policies){
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    Policy currentPolicy = (Policy) viewSelector.getSelectedItem();
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
		// REMOVE FROM VIEW SELECTOR
		viewSelector.removeItem(policy);
	    }
	    if( ((DefaultComboBoxModel)viewSelector.getModel()).getIndexOf(currentPolicy) < 0 ){
		changePolicyRackViewImpl( (Policy) viewSelector.getItemAt(0) );
	    }
	}});
    }

    public synchronized void moveFromToolboxToRack(final MTransformJButton mTransformJButton){
	try{
	    // VIEW: DEPLOYING
	    mTransformJButton.setDeployingView();
	    // INSTANTIATE IN MVVM
	    Policy currentPolicy = (Policy) viewSelector.getSelectedItem();
	    Tid tid = Util.getTransformManager().instantiate(mTransformJButton.getName(),currentPolicy);
	    // CREATE APPLIANCE
	    TransformContext transformContext = Util.getTransformManager().transformContext( tid );
	    MTransformJPanel mTransformJPanel = MTransformJPanel.instantiate(transformContext);
	    // DEPLOY APPLIANCE TO CURRENT POLICY RACK
	    addToRack(currentPolicy, mTransformJPanel);
	    // UPDATE PROTOCOL SETTINGS CACHE
	    loadAllCasings(false);
	    // VIEW: DEPLOYED
	    mTransformJButton.setDeployedView();
	}
	catch(Exception e){
	    try{ Util.handleExceptionWithRestart("Error moving from toolbox to rack", e); }
	    catch(Exception f){ Util.handleExceptionNoRestart("Error moving from toolbox to rack", f); }
	}
    }

    public synchronized void moveFromRackToToolbox(final MTransformJPanel mTransformJPanel){
	try{
	    // DESTROY IN MVVM
	    Util.getTransformManager().destroy(mTransformJPanel.getTid());
	    // REMOVE APPLIANCE FROM THE CURRENT POLICY RACK
	    Policy currentPolicy = (Policy) viewSelector.getSelectedItem();
	    removeFromRack(mTransformJPanel, currentPolicy);
	    // VIEW: DEPLOYABLE
	    ButtonKey buttonKey = new ButtonKey(mTransformJPanel);
	    policyToolboxMap.get(currentPolicy).get(buttonKey).setDeployableView();
	}
	catch(Exception e){
	    try{ Util.handleExceptionWithRestart("Error moving from rack to toolbox", e); }
	    catch(Exception f){ Util.handleExceptionNoRestart("Error moving from rack to toolbox", f); }
	}
    }

    public synchronized void moveFromToolboxToStore(final MTransformJButton mTransformJButton){
	try{
	    // DECIDE IF WE CAN REMOVE
	    for( Policy policy : policyTidMap.keySet() ){
		for( Tid tid : policyTidMap.get(policy) ){
		    if( mTransformJButton.getName().equals(tid.getName()) )
			return;
		}
	    }
	    // VIEW: REMOVING FROM TOOLBOX
	    mTransformJButton.setRemovingFromToolboxView();
	    // UNINSTALL IN MVVM
	    Util.getToolboxManager().uninstall(mTransformJButton.getName());
	    // REMOVE FROM TOOLBOX
	    removeFromToolbox(mTransformJButton.getMackageDesc());
	    // ADD TO STORE
	    addToStore(mTransformJButton.getMackageDesc());
	    // VIEW: PROCURABLE
	    mTransformJButton.setProcurableView();
	}
	catch(Exception e){
	    try{ Util.handleExceptionWithRestart("Error moving from toolbox to store", e); }
	    catch(Exception f){ Util.handleExceptionNoRestart("Error moving from toolbox to store", f); }
	}
    }
    
    public void moveFromStoreToToolbox(final MTransformJButton mTransformJButton){
	try{
	    // VIEW: PROCURING
	    mTransformJButton.setProcuringView();
	    // INSTALL IN MVVM
	    Util.getToolboxManager().install(mTransformJButton.getName());
	    // REMOVE FROM STORE
	    removeFromStore(mTransformJButton.getMackageDesc());
	    // ADD TO TOOLBOX (VIEW: DEPLOYABLE)
	    for( Policy policy : policyToolboxMap.keySet() ){
		addToToolbox(policy,mTransformJButton.getMackageDesc(),false);
	    }
	}
	catch(Exception e){
	    try{ Util.handleExceptionWithRestart("Error moving from store to toolbox", e); }
	    catch(Exception f){ Util.handleExceptionNoRestart("Error moving from store to toolbox", f); }
	}
    }

    public synchronized void changePolicyRackView(final Policy policy){
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    changePolicyRackView(policy);
	}});
    }

    private void changePolicyRackViewImpl(final Policy policy){
	Policy currentPolicy = (Policy) viewSelector.getSelectedItem();
	JPanel oldRackJPanel = policyRackJPanelMap.get(currentPolicy);
	JPanel newRackJPanel = policyRackJPanelMap.get(policy);
	rackViewJPanel.remove( oldRackJPanel );
	rackViewJPanel.add( newRackJPanel, rackGridBagConstraints );
	rackViewJPanel.revalidate();
    }
    ///////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////


    // INIT API ////////////////////////////////////////////
    ////////////////////////////////////////////////////////
    private void initMvvmModel() throws Exception {
	for( MackageDesc mackageDesc : Util.getToolboxManager().uninstalled() )
	    purchasableMackageList.add( mackageDesc );
	for( MackageDesc mackageDesc : Util.getToolboxManager().installed() )
	    installedMackageList.add( mackageDesc );
	for( Policy policy : Util.getPolicyManager().getPolicies() )
	    policyTidMap.put( policy, Util.getTransformManager().transformInstances(policy) );
    }

    private void initStoreModel(final JProgressBar progressBar){
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    progressBar.setValue(80);
	    progressBar.setString("Populating Store...");
	}});
	int progress = 0;
	for( MackageDesc mackageDesc : purchasableMackageList ){
	    progress++;
	    addToStore(mackageDesc);	    
	    final float progressFinal = (float) progress;
	    final float overallFinal = (float) purchasableMackageList.size();
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
	    for( MackageDesc mackageDesc : installedMackageList ){
		progress++;
		boolean isDeployed = false;
		for( Tid tid : policyTidMap.get(policy) ){ // XXX this is slow but works
		    if( mackageDesc.getName().equals(tid.getTransformName()) ){
			isDeployed = true;
			break;
		    }
		}
		addToToolbox(policy,mackageDesc,isDeployed);
		final float progressFinal = (float) progress;
		final float overallFinal = (float) (installedMackageList.size() * policyTidMap.size());
		SwingUtilities.invokeLater( new Runnable(){ public void run(){
		    progressBar.setValue(64 + (int) (16f*progressFinal/overallFinal) );
		}});
	    }
	}
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    progressBar.setValue(80);
	}});
    }

    private void initRackModel(final JProgressBar progressBar) throws Exception {
	if( progressBar != null ){
	    SwingUtilities.invokeLater( new Runnable(){ public void run(){
	        progressBar.setValue(16);
		if( policyTidMap.keySet().size() == 1 )
		    progressBar.setString("Populating Rack...");
		else
		    progressBar.setString("Populating Racks...");
	    }});
	}
	int progress = 0;
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
		progress++;
		// GET THE TRANSFORM CONTEXT AND MACKAGE DESC
		TransformContext transformContext = Util.getTransformManager().transformContext( tid );
		MackageDesc mackageDesc = transformContext.getMackageDesc();
		if( !isMackageVisible(mackageDesc) )
		    continue;
		// CONSTRUCT AND ADD THE APPLIANCE
		final MTransformJPanel mTransformJPanel = MTransformJPanel.instantiate(transformContext);
		addToRack(policy,mTransformJPanel);
		if( progressBar != null ){
		    final float progressFinal = (float) progress;
		    final float overallFinal = (float) overall;
		    SwingUtilities.invokeLater( new Runnable(){ public void run(){
			progressBar.setValue(16 + (int) (48f*progressFinal/overallFinal) );
		    }});
		}
	    }
	}
	if( progressBar != null ){
	    SwingUtilities.invokeLater( new Runnable(){ public void run(){
	        progressBar.setValue(64);
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
    private class PolicyRenderer implements ListCellRenderer{
	private ListCellRenderer listCellRenderer;
	public PolicyRenderer(ListCellRenderer listCellRenderer){
	    this.listCellRenderer = listCellRenderer;
	}
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus){
	    return listCellRenderer.getListCellRendererComponent(list, ((Policy)value).getName(), index, isSelected, hasFocus);
	}
    }
    ////////////////////////////////////////
    ////////////////////////////////////////


    // PUBLIC API /////////////////////////////////////////
    ///////////////////////////////////////////////////////
    private void handleViewSelector(){
	Policy currentPolicy = (Policy) viewSelector.getSelectedItem();
	JPanel toolboxJPanel = policyToolboxJPanelMap.get(currentPolicy);
	if( toolboxJScrollPane.getViewport().getView().equals(toolboxJPanel) )
	    return;
	int currentScrollPosition = toolboxJScrollPane.getVerticalScrollBar().getValue();
	toolboxJScrollPane.setViewportView( toolboxJPanel );
	toolboxJPanel.revalidate();
	if( lastScrollPosition >= 0 )
	    toolboxJScrollPane.getVerticalScrollBar().setValue( currentScrollPosition );
	lastScrollPosition = currentScrollPosition;
	JPanel rackJPanel = policyRackJPanelMap.get(currentPolicy);
	int lastScrollPosition = 0;
	if( lastRackJPanelSelected != null )
	    rackViewJPanel.remove( lastRackJPanelSelected );
	lastRackJPanelSelected = rackJPanel;
	rackViewJPanel.add( rackJPanel, rackGridBagConstraints );
	rackViewJPanel.revalidate();
	System.err.println("Changed toolbox and rack view to policy: " + currentPolicy.getName());
    }
    ///////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////

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
	// REMOVE FROM RACK MODEL
	policyRackMap.get(policy).remove(mTransformJPanel);
	// REMOVE FROM RACK VIEW
	final JPanel rackJPanel = policyRackJPanelMap.get(policy);
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    rackJPanel.remove(mTransformJPanel);
	    rackJPanel.revalidate();
	}});
    }
    ///////////////////////////////////////
    ///////////////////////////////////////


    // ADD API ////////////////////////////
    ///////////////////////////////////////
    private void addToStore(final MackageDesc mackageDesc){
	// ONLY UPDATE GUI MODELS IF THIS IS VISIBLE
	if( !isMackageVisible(mackageDesc) )
	    return;
	final ButtonKey buttonKey = new ButtonKey(mackageDesc);
	final int position = ((TreeMap)storeMap).headMap(buttonKey).size();
	final MTransformJButton mTransformJButton = new MTransformJButton(mackageDesc);
	mTransformJButton.setProcurableView(); // xxx i believe this is safe because it ends up on the EDT
	// UPDATE GUI DATA MODEL
	storeMap.put(buttonKey, mTransformJButton);
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    mTransformJButton.addActionListener( new StoreActionListener(mTransformJButton) );
	    // UPDATE GUI VIEW MODEL
	    storeJPanel.add(mTransformJButton, buttonGridBagConstraints, position);
	    storeJPanel.revalidate();
	}});
	System.err.println("Added to store: " + mackageDesc.getDisplayName());
    }

    private void addToToolbox(final Policy policy, final MackageDesc mackageDesc, final boolean isDeployed){
	// ONLY UPDATE GUI MODELS IF THIS IS VISIBLE
	if( !isMackageVisible(mackageDesc) )
	    return;
	final ButtonKey buttonKey = new ButtonKey(mackageDesc);
	final Map<ButtonKey,MTransformJButton> toolboxMap = policyToolboxMap.get(policy);
	final JPanel toolboxJPanel = policyToolboxJPanelMap.get(policy);
	final MTransformJButton mTransformJButton = new MTransformJButton(mackageDesc);
	final int position = ((TreeMap)toolboxMap).headMap(buttonKey).size();
	if( isDeployed )
	    mTransformJButton.setDeployedView();
	else
	    mTransformJButton.setDeployableView();
	// UPDATE GUI DATA MODEL
	toolboxMap.put(buttonKey,mTransformJButton);
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    mTransformJButton.addActionListener( new ToolboxActionListener(policy,mTransformJButton) );
	    // UPDATE GUI VIEW MODEL
	    toolboxJPanel.add(mTransformJButton, buttonGridBagConstraints, position);
	    toolboxJPanel.revalidate();				    
	}});
	System.err.println("Added to toolbox (" + policy.getName() + "): " + mackageDesc.getDisplayName() + " deployed: " + isDeployed);
    }

    private void addToRack(final Policy policy, final MTransformJPanel mTransformJPanel){
	// ADD TO RACK MODEL
	ButtonKey buttonKey = new ButtonKey(mTransformJPanel);
	policyRackMap.get(policy).put(buttonKey,mTransformJPanel);
	// ADD TO RACK VIEW
	final JPanel rackJPanel = policyRackJPanelMap.get(policy);
	Map<ButtonKey,MTransformJPanel> rackMap = policyRackMap.get(policy);
	final int position = ((TreeMap)rackMap).headMap(buttonKey).size();
	SwingUtilities.invokeLater( new Runnable() { public void run() {
	    rackJPanel.add(mTransformJPanel, applianceGridBagConstraints, position);
	    rackJPanel.revalidate();		
	}});
	System.err.println("Added to rack (" + policy.getName() + "): " + mTransformJPanel.getMackageDesc().getDisplayName() );
    }

    // USED FOR LOADING/PRELOADING OF CASINGS
    public MCasingJPanel[] loadAllCasings(boolean generateGuis){
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
		Util.handleExceptionNoRestart("Error building gui from casing context: " + casingName, e);
	    }
	}
	return mCasingJPanels.toArray( new MCasingJPanel[0] );
    }

    ///////////////////////////////////////
    ///////////////////////////////////////


    // PRIVATE CLASSES & UTILS /////////////////////
    ////////////////////////////////////////////////

    private boolean isMackageVisible(MackageDesc mackageDesc){
	if( mackageDesc.getType() != MackageDesc.TRANSFORM_TYPE )
	    return false;
	else if( mackageDesc.getRackPosition() < 0 )
	    return false;
	else
	    return true;
    }

    private class StoreActionListener implements java.awt.event.ActionListener {
	private MTransformJButton mTransformJButton;
	public StoreActionListener(MTransformJButton mTransformJButton){
	    this.mTransformJButton = mTransformJButton;
	}
	public void actionPerformed(java.awt.event.ActionEvent evt){
	    MTransformJButton targetMTransformJButton = (MTransformJButton) evt.getSource();
	    targetMTransformJButton.setEnabled(false);
	    StoreJDialog storeJDialog = new StoreJDialog(targetMTransformJButton);
	    storeJDialog.setVisible(true);
	    if( storeJDialog.getPurchasedMTransformJButton() == null)
		targetMTransformJButton.setEnabled(true); // nothing was purchased
	    // else - something was purchased, handled in the above dialog
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
	    MTransformJButton targetMTransformJButton = ((MTransformJButton) evt.getSource());
	    if( (evt.getModifiers() & ActionEvent.SHIFT_MASK) > 0)
		targetMTransformJButton.uninstall();
	    else
		targetMTransformJButton.install();
	}
    }
    //////////////////////////////////////
    //////////////////////////////////////

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
