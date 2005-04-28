/*
 * MPipeline.java
 *
 * Created on March 11, 2004, 9:55 PM
 */

package com.metavize.gui.pipeline;

import java.awt.*;
import java.lang.reflect.Constructor;
import java.util.*;
import javax.swing.*;

import com.metavize.gui.main.MMainJFrame;
import com.metavize.gui.transform.MTransformJPanel;
import com.metavize.gui.util.*;
import com.metavize.mvvm.*;
import com.metavize.mvvm.security.*;
import com.metavize.mvvm.tran.*;

/**
 *
 * @author  Ian Nieves
 */
public class MPipelineJPanel extends javax.swing.JPanel {

    private static Hashtable transformContextHashtable;

    // FOR PROGRESS BAR DURING STARTUP
    private int initialInstallCount;
    private int installedCount = 0;


    /** Creates new form MPipeline */
    public MPipelineJPanel() {
        Util.setMPipelineJPanel(this);
	transformContextHashtable = new Hashtable();

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
	    if( Util.getTransformManager().transformContext(installedTransformID[i]).getMackageDesc().getType() != MackageDesc.TRANSFORM_TYPE ){
		installedCount++;
		continue;
	    }
            installedTransformContext = Util.getTransformManager().transformContext(installedTransformID[i]);
            AddTransformThread addTransformThread = new AddTransformThread(installedTransformContext);
            addTransformThread.setContextClassLoader(Util.getClassLoader());
            addTransformThread.start();
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


    private synchronized MTransformJPanel getMTransformJPanel(String transformName){
	Tid[] transformInstances = Util.getTransformManager().transformInstances();
	if(transformInstances == null) return null;  if(transformInstances.length == 0) return null;
	return (MTransformJPanel) transformContextHashtable.get( transformInstances[0] );
    }

    private synchronized void addToRack(TransformContext transformContext, MTransformJPanel mTransformJPanel) {
        transformContextHashtable.put(transformContext.getTid(), mTransformJPanel);

        int rackPosition = transformContext.getMackageDesc().getRackPosition();
        if(rackPosition < 0)
            rackPosition = 0;
            ((MRackJPanel)transformJPanel).addTransform(mTransformJPanel, rackPosition);
            this.validate();
            this.repaint();
    }

    public synchronized boolean inRack(TransformContext transformContext){
        return transformContextHashtable.containsKey(transformContext.getTid());
    }

    public synchronized Vector getAllMTransformJPanels(){
        return new Vector( transformContextHashtable.values() );
    }


    public synchronized void removeAllTransforms(){
	Tid[] allTransforms = Util.getTransformManager().transformInstances();
	Tid tempTid;
	TransformContext tempTransformContext;
	for(int i = 0; i < allTransforms.length; i++){
	    tempTid = allTransforms[i];
	    tempTransformContext = Util.getTransformManager().transformContext(tempTid);
	    if( tempTransformContext.getMackageDesc().getType() != MackageDesc.TRANSFORM_TYPE )
		continue;
	    removeTransform(tempTransformContext);
	}
    }


    public synchronized void removeTransform(TransformContext transformContext) {
        String removableName = null;
        Tid removableTid = null;
        MTransformJPanel removableMTransformJPanel = null;
        try{
            removableName = transformContext.getTransformDesc().getName();
            removableTid = transformContext.getTid();
            removableMTransformJPanel = (MTransformJPanel) transformContextHashtable.get( removableTid );
            Util.getTransformManager().destroy( removableTid );
        }
        catch(Exception e){
            try{
                Util.handleExceptionWithRestart("Error removing transform: " + removableName,  e);
            }
            catch(Exception f){
                Util.handleExceptionNoRestart("Error removing transform: " + removableName,  f);
            }
        }

	try{            
            transformContextHashtable.remove( removableTid );
            ((MRackJPanel)transformJPanel).removeTransform( removableMTransformJPanel );
            Util.setEmailAndVirusJPanel(removableName, null);
	    Util.getMMainJFrame().getButton(removableName).setDeployableView();
	    this.validate();
	    this.repaint();
	}
	catch(Exception e){
            try{
                Util.handleExceptionWithRestart("Error removing transform gui: " + removableName,  e);
            }
            catch(Exception f){
                Util.handleExceptionNoRestart("Error removing transform gui: " + removableName,  f);
            }
        }

    }



    public MTransformJPanel addTransform(Object reference){

        MTransformJPanel mTransformJPanel = null;

        if(reference instanceof String){ // ADD DURING LIVE OPERATION
            Tid tID = null;
            Class transformGUIClass;
            TransformContext transformContext = null;
            Constructor transformConstructor = null;
            try{

		tID = Util.getTransformManager().instantiate((String)reference);
		transformContext = Util.getTransformManager().transformContext(tID);
		// System.out.println(" gui classname: " + transformManager.transformContext(tID).getTransformDesc().getGuiClassName());
		transformGUIClass = Util.getClassLoader().loadClass( Util.getTransformManager().transformContext(tID).getTransformDesc().getGuiClassName(), transformContext.getTransformDesc().getName());
		// System.out.println(" gui classname: " + transformGUIClass);
		transformConstructor = transformGUIClass.getConstructor(new Class[]{TransformContext.class});
		mTransformJPanel = (MTransformJPanel) transformConstructor.newInstance(new Object[]{transformContext});
		addToRack( transformContext, mTransformJPanel);
		
            }
            catch(Exception e){
                try{
                    Util.handleExceptionWithRestart("Error adding transform from string " + (String) reference, e);
                }
                catch(Exception f){
                    Util.handleExceptionNoRestart("Error adding transform from string " + (String) reference, f);
                    return null;
                }
            }
        }
        else if(reference instanceof TransformContext){  // ADD DURING CONNECTED INITIALIZATION ONLY
            try{
		Class transformGUIClass = Util.getClassLoader().loadClass( ((TransformContext)reference).getTransformDesc().getGuiClassName(), ((TransformContext)reference).getTransformDesc().getName() );
		Constructor transformGUIConstructor = transformGUIClass.getConstructor(new Class[]{TransformContext.class});
		mTransformJPanel = (MTransformJPanel) transformGUIConstructor.newInstance(new Object[]{((TransformContext)reference)});
		addToRack( ((TransformContext)reference), mTransformJPanel);
                
            }
            catch(Exception e){
                try{
                    Util.handleExceptionWithRestart("Error adding transform from transform context: " + ((TransformContext)reference).getTransformDesc().getName(), e);
                }
                catch(Exception f){
                    Util.handleExceptionNoRestart("Error adding transform from transform context: " + ((TransformContext)reference).getTransformDesc().getName(), f);
                    return null;
                }
            }
            finally{
                synchronized(this){
                    Util.getStatusJProgressBar().setValue(16 + (int) ((((float)installedCount)/(float)initialInstallCount)*48f) );
                    installedCount++;
                }
            }
        }
        else{
            Util.printMessage("unknown reference type: " + reference);
            return null;
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
            this.transformContext = transformContext;
        }
        public void run(){
            addTransform(transformContext);
        }
    }


}
