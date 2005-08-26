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

package com.metavize.gui.pipeline;

import com.metavize.gui.main.*;
import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.*;
import com.metavize.mvvm.security.*;
import com.metavize.mvvm.tran.*;

import java.awt.*;
import java.lang.reflect.Constructor;
import java.util.*;
import javax.swing.*;


public class MPipelineJPanel extends javax.swing.JPanel {

    // FOR PROGRESS BAR DURING STARTUP
    private int initialInstallCount;
    private int installedCount = 0;


    /** Creates new form MPipeline */
    public MPipelineJPanel() {
        Util.setMPipelineJPanel(this);

        // INITIALIZE GUI
        initComponents();
        mPipelineJScrollPane.getVerticalScrollBar().setUnitIncrement(10);
        mPipelineJScrollPane.getVerticalScrollBar().setBorder( new javax.swing.border.EmptyBorder(15, 0, 15, 0) );
        mPipelineJScrollPane.getVerticalScrollBar().setOpaque(false);

        // ADD TRANSFORMS
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    Util.getStatusJProgressBar().setString("adding Software Appliances...");
	}});
        Tid installedTransformID[] = Util.getTransformManager().transformInstances();
        initialInstallCount = installedTransformID.length;
        TransformContext installedTransformContext;
	MackageDesc installedMackageDesc;
        for(int i=0; i<installedTransformID.length; i++){
	    installedTransformContext = Util.getTransformManager().transformContext(installedTransformID[i]);
	    installedMackageDesc = installedTransformContext.getMackageDesc();
	    if( installedMackageDesc.getType() != MackageDesc.TRANSFORM_TYPE ){
		installedCount++;
		continue;
	    }
	    else if( installedMackageDesc.getRackPosition() < 0 ){
		installedCount++;
		continue;
	    }
	    else{
		new AddTransformThread(installedTransformContext, installedMackageDesc.getDisplayName());
	    }	    
        }
	if( installedTransformID.length == 0 ){
	    SwingUtilities.invokeLater( new Runnable(){ public void run(){
		Util.getStatusJProgressBar().setValue(54);
	    }});
	}
	
        while(installedCount < initialInstallCount){
            try{
                Thread.sleep(100l);
            }
            catch(Exception e){}
        }
	loadAllCasings(false);
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    Util.getStatusJProgressBar().setValue(64);
	}});
    }
    
    // USED FOR LOADING/PRELOADING OF CASINGS
    public MCasingJPanel[] loadAllCasings(boolean generateGuis){
	final String casingNames[] = {"mail-casing", "http-casing", "ftp-casing"};
	Vector<MCasingJPanel> mCasingJPanels = new Vector<MCasingJPanel>();
	Tid casingInstances[] = null;
	TransformContext transformContext = null;
	TransformDesc transformDesc = null;
	String casingGuiClassName = null;
	Class casingGuiClass = null;
	Constructor casingGuiConstructor = null;
	MCasingJPanel mCasingJPanel = null;
        for(String casingName : casingNames){
	    try{
		casingInstances = Util.getTransformManager().transformInstances(casingName);
		if( Util.isArrayEmpty(casingInstances) )
		    continue;
		transformContext = Util.getTransformManager().transformContext(casingInstances[0]);
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

    public synchronized void removeTransform(final MTransformJPanel mTransformJPanel) {
	String removableName = null;
	int removablePosition;
        ButtonKey buttonKey = null;
	Tid removableTid = null;
        try{
	    // REMOVE AT SERVER SIDE
            removableName = mTransformJPanel.getMackageDesc().getDisplayName();
            buttonKey = new ButtonKey(mTransformJPanel);
            removableTid = mTransformJPanel.getTransformContext().getTid();
            Util.getTransformManager().destroy( removableTid );

	    // REMOVE AT CLIENT SIDE
	    SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
		((MRackJPanel)MPipelineJPanel.this.transformJPanel).removeTransform( mTransformJPanel );
	    }});
	    
            Util.getMMainJFrame().getToolboxMap().get(buttonKey).setDeployableView();
	}
	catch(Exception e){
            try{
                Util.handleExceptionWithRestart("Error removing transform: " + removableName,  e);
            }
            catch(Exception f){
                Util.handleExceptionNoRestart("Error removing transform: " + removableName,  f);
		Util.getMMainJFrame().getToolboxMap().get(buttonKey).setFailedRemoveFromRackView();
            }
        }

    }



    public MTransformJPanel addTransform(Object reference) throws Exception {

	String transformName = "unnamed";
	TransformDesc transformDesc = null;
	TransformContext transformContext = null;
	Class transformGUIClass = null;
	Constructor transformGUIConstructor = null;
        MTransformJPanel mTransformJPanel = null;

	// GET A TRANSFORM CONTEXT, OR BAIL
        if(reference instanceof String){ // ADD FROM BUTTON CLICK
            try{
		Tid tID = Util.getTransformManager().instantiate((String)reference);
		transformContext = Util.getTransformManager().transformContext(tID);
		transformDesc = transformContext.getTransformDesc();
		transformName = transformDesc.getDisplayName();
            }
            catch(Exception e){
                Util.handleExceptionWithRestart("Error building transform context from string " + (String) reference, e);
            }
        }
        else if(reference instanceof TransformContext){  // ADD DURING CLIENT INIT
	    transformContext = (TransformContext) reference;
	    transformDesc = transformContext.getTransformDesc();
	    transformName = transformDesc.getDisplayName();
	}
	else{
            Util.printMessage("unknown reference type: " + reference);
            return null;
        }
	
	// CONSTRUCT A GUI FROM THE TRANSFORM CONTEXT
	try{
	    transformGUIClass = Util.getClassLoader().loadClass( transformDesc.getGuiClassName(), transformDesc.getName() );
	    transformGUIConstructor = transformGUIClass.getConstructor(new Class[]{TransformContext.class});
	    mTransformJPanel = (MTransformJPanel) transformGUIConstructor.newInstance(new Object[]{transformContext});
	    ((MRackJPanel)transformJPanel).addTransform(mTransformJPanel);
	}
	catch(Exception e){
	    Util.handleExceptionWithRestart("Error building appliance from transform context: " + transformName, e);
	}
	finally{
	    synchronized(this){
		SwingUtilities.invokeLater( new Runnable(){ public void run(){
		    Util.getStatusJProgressBar().setValue(16 + (int) ((((float)installedCount)/(float)initialInstallCount)*38f) );
		    installedCount++;
		}});
	    }
	}
	return mTransformJPanel;
    }


    public synchronized void focusMTransformJPanel(Rectangle newBounds){
        if(newBounds == null)
            return;
        newBounds.x += transformJPanel.getX();
        newBounds.y += transformJPanel.getY() - 1;
        newBounds.x -= mPipelineJScrollPane.getViewport().getViewPosition().x;
        newBounds.y -= mPipelineJScrollPane.getViewport().getViewPosition().y;
        mPipelineJScrollPane.getViewport().scrollRectToVisible(newBounds);
    }



    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        mPipelineJScrollPane = new javax.swing.JScrollPane();
        transformJPanel = new MRackJPanel();
        scrollbarBackground = new com.metavize.gui.widgets.MTiledIconLabel();

        setLayout(new java.awt.GridBagLayout());

        setBackground(new java.awt.Color(0, 51, 51));
        setMinimumSize(new java.awt.Dimension(800, 500));
        setOpaque(false);
        setPreferredSize(new java.awt.Dimension(800, 500));
        mPipelineJScrollPane.setBackground(new java.awt.Color(51, 51, 51));
        mPipelineJScrollPane.setBorder(null);
        mPipelineJScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        mPipelineJScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        mPipelineJScrollPane.setDoubleBuffered(true);
        mPipelineJScrollPane.setMinimumSize(new java.awt.Dimension(720, 21));
        mPipelineJScrollPane.setOpaque(false);
        mPipelineJScrollPane.getViewport().setOpaque(false);
        transformJPanel.setBackground(new java.awt.Color(51, 255, 51));
        transformJPanel.setMaximumSize(null);
        transformJPanel.setMinimumSize(null);
        transformJPanel.setOpaque(false);
        transformJPanel.setPreferredSize(null);
        mPipelineJScrollPane.setViewportView(transformJPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        add(mPipelineJScrollPane, gridBagConstraints);

        scrollbarBackground.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/pipeline/VerticalScrollBar42x100.png")));
        scrollbarBackground.setDoubleBuffered(true);
        scrollbarBackground.setOpaque(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weighty = 1.0;
        add(scrollbarBackground, gridBagConstraints);

    }//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane mPipelineJScrollPane;
    private javax.swing.JLabel scrollbarBackground;
    private javax.swing.JPanel transformJPanel;
    // End of variables declaration//GEN-END:variables

    class AddTransformThread extends Thread {
        
	private TransformContext transformContext;
	private String transformName;
        
	AddTransformThread(TransformContext transformContext, String transformName){
	    super("MVCLIENT-AddTransformThread: " + transformName);
            this.transformContext = transformContext;
	    this.transformName = transformName;
	    this.setContextClassLoader( Util.getClassLoader() );
	    this.start();
        }
        public void run(){
            try{
                addTransform(transformContext);
            }
            catch(Exception e){
                Util.handleExceptionNoRestart("Error adding appliance during startup: " + transformName, e);

            }
        }
    }


}
