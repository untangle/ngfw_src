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
            /*
	    System.err.println("-- start ------------------------");
	    System.err.println("tid: " + installedTransformID[i] );
	    System.err.println("transform context: " + Util.getTransformManager().transformContext(installedTransformID[i]) );
	    System.err.println("mackage desc: " + Util.getTransformManager().transformContext(installedTransformID[i]).getMackageDesc() );
	    System.err.println("mackage type: " + Util.getTransformManager().transformContext(installedTransformID[i]).getMackageDesc().getType() );
	    System.err.println("-- end ------------------------");
             */
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
	    
	    Util.setEmailAndVirusJPanel(removableName, null);
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

        MTransformJPanel mTransformJPanel = null;

        if(reference instanceof String){ // ADD FROM BUTTON CLICK
            Tid tID = null;
            Class transformGUIClass;
            TransformContext transformContext = null;
            Constructor transformConstructor = null;
            try{
		tID = Util.getTransformManager().instantiate((String)reference);
		transformContext = Util.getTransformManager().transformContext(tID);
		transformGUIClass = Util.getClassLoader().loadClass( Util.getTransformManager().transformContext(tID).getTransformDesc().getGuiClassName(), transformContext.getTransformDesc().getName());
		transformConstructor = transformGUIClass.getConstructor(new Class[]{TransformContext.class});
		mTransformJPanel = (MTransformJPanel) transformConstructor.newInstance(new Object[]{transformContext});
                ((MRackJPanel)transformJPanel).addTransform(mTransformJPanel);
            }
            catch(Exception e){
                Util.handleExceptionWithRestart("Error adding transform from string " + (String) reference, e);
            }
        }
        else if(reference instanceof TransformContext){  // ADD DURING CLIENT INIT
            try{
		Class transformGUIClass = Util.getClassLoader().loadClass( ((TransformContext)reference).getTransformDesc().getGuiClassName(), ((TransformContext)reference).getTransformDesc().getName() );
		Constructor transformGUIConstructor = transformGUIClass.getConstructor(new Class[]{TransformContext.class});
		mTransformJPanel = (MTransformJPanel) transformGUIConstructor.newInstance(new Object[]{((TransformContext)reference)});
                ((MRackJPanel)transformJPanel).addTransform(mTransformJPanel);
            }
            catch(Exception e){
                Util.handleExceptionWithRestart("Error adding transform from transform context: " + ((TransformContext)reference).getTransformDesc().getName(), e);
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
