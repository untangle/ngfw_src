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

import java.awt.*;
import java.lang.reflect.Constructor;
import java.util.*;
import javax.swing.*;

import com.metavize.gui.main.*;
import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;
import com.metavize.mvvm.*;
import com.metavize.mvvm.security.*;
import com.metavize.mvvm.tran.*;

/**
 *
 * @author  Ian Nieves
 */
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
        Util.getStatusJProgressBar().setString("adding Software Appliances...");
        Tid installedTransformID[] = Util.getTransformManager().transformInstances();
        TransformContext installedTransformContext;
        initialInstallCount = installedTransformID.length;
        for(int i=0; i<installedTransformID.length; i++){
            installedTransformContext = Util.getTransformManager().transformContext(installedTransformID[i]);	    
	    if( installedTransformContext.getMackageDesc().getType() != MackageDesc.TRANSFORM_TYPE ){
		installedCount++;
		continue;
	    }
	    else if( installedTransformContext.getMackageDesc().getRackPosition() < 0 ){
		installedCount++;
		continue;
	    }
	    else{
		new AddTransformThread(installedTransformContext);
	    }
        }
	if( installedTransformID.length == 0 )
	    Util.getStatusJProgressBar().setValue(64);
	
        while(installedCount < initialInstallCount){
            try{
                Thread.sleep(1000l);
            }
            catch(Exception e){}
        }
    }
    
    // USED FOR LOADING/PRELOADING OF CASINGS
    public MCasingJPanel[] loadAllCasings(boolean generateGuis){
	Vector<MCasingJPanel> mCasingJPanels = new Vector<MCasingJPanel>();
	Tid allTransformInstances[] = Util.getTransformManager().transformInstances();
	TransformContext transformContext = null;
	String casingName = null;
	String casingGuiClassName = null;
	Class casingGuiClass = null;
	Constructor casingGuiConstructor = null;
	MCasingJPanel mCasingJPanel = null;
        for(Tid tid : allTransformInstances){
	    transformContext = Util.getTransformManager().transformContext(tid);
	    casingName = transformContext.getTransformDesc().getName();
	    casingGuiClassName = transformContext.getTransformDesc().getGuiClassName();
	    if( transformContext.getMackageDesc().getType() == MackageDesc.CASING_TYPE ){
		if( casingName.equals("mail-casing")
		    || casingName.equals("http-casing")
		    || casingName.equals("ftp-casing") ){
		    try{
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
            removableName = mTransformJPanel.getTransformContext().getTransformDesc().getDisplayName();
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

	TransformContext transformContext = null;
	Class transformGUIClass = null;
	Constructor transformGUIConstructor = null;
        MTransformJPanel mTransformJPanel = null;

	// GET A TRANSFORM CONTEXT, OR BAIL
        if(reference instanceof String){ // ADD FROM BUTTON CLICK
            try{
		Tid tID = Util.getTransformManager().instantiate((String)reference);
		transformContext = Util.getTransformManager().transformContext(tID);
            }
            catch(Exception e){
                Util.handleExceptionWithRestart("Error building transform context from string " + (String) reference, e);
            }
        }
        else if(reference instanceof TransformContext){  // ADD DURING CLIENT INIT
	    transformContext = (TransformContext) reference;
	}
	else{
            Util.printMessage("unknown reference type: " + reference);
            return null;
        }
	
	// CONSTRUCT A GUI FROM THE TRANSFORM CONTEXT
	try{
	    transformGUIClass = Util.getClassLoader().loadClass( transformContext.getTransformDesc().getGuiClassName(),
								 transformContext.getTransformDesc().getName() );
	    transformGUIConstructor = transformGUIClass.getConstructor(new Class[]{TransformContext.class});
	    mTransformJPanel = (MTransformJPanel) transformGUIConstructor.newInstance(new Object[]{transformContext});
	    ((MRackJPanel)transformJPanel).addTransform(mTransformJPanel);
	}
	catch(Exception e){
	    Util.handleExceptionWithRestart("Error building appliance from transform context: " + transformContext.getTransformDesc().getName(), e);
	}
	finally{
	    synchronized(this){
		Util.getStatusJProgressBar().setValue(16 + (int) ((((float)installedCount)/(float)initialInstallCount)*48f) );
		installedCount++;
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
        
	AddTransformThread(TransformContext transformContext){
	    super("MVCLIENT-AddTransformThread: " + transformContext.getMackageDesc().getDisplayName());
            this.transformContext = transformContext;
	    this.setContextClassLoader( Util.getClassLoader() );
	    this.start();
        }
        public void run(){
            try{
                addTransform(transformContext);
            }
            catch(Exception e){
                ButtonKey buttonKey = new ButtonKey(transformContext);
                Util.handleExceptionNoRestart("Error adding appliance during startup: " + transformContext.getTransformDesc().getName(), e);

            }
        }
    }


}
