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


    private static MvvmContext mvvmContext;
    private static TransformManager transformManager;
    private static ToolboxManager toolboxManager;
    private static Hashtable transformContextHashtable;

    private MMainJFrame mMainJFrame;

    private JProgressBar statusJProgressBar;
    private int initialInstallCount;
    private int installedCount = 0;


    Tid casingTid = null;
    Tid httpTid = null;
    Tid virusTid = null;
    Tid mimexTid = null;


    /** Creates new form MPipeline */
    public MPipelineJPanel() {
        this.mMainJFrame = Util.getMMainJFrame();
        this.statusJProgressBar = Util.getStatusJProgressBar();
        Util.setMPipelineJPanel(this);

        // INITIALIZE GUI
        initComponents();
        mPipelineJScrollPane.getVerticalScrollBar().setUnitIncrement(10);

        // INITIALIZE STATICS
        mvvmContext = Util.getMvvmContext();
        transformManager = mvvmContext.transformManager();
        toolboxManager = mvvmContext.toolboxManager();
        transformContextHashtable = new Hashtable();

        // ADD TRANSFORMS
        statusJProgressBar.setString("adding Software Appliances...");
        Tid installedTransformID[] = transformManager.transformInstances();
        TransformContext installedTransformContext;
        initialInstallCount = installedTransformID.length;
        for(int i=0; i<installedTransformID.length; i++){
            installedTransformContext = transformManager.transformContext(installedTransformID[i]);
            AddTransformThread addTransformThread = new AddTransformThread(installedTransformContext);
            addTransformThread.setContextClassLoader(Util.getClassLoader());
            addTransformThread.start();
        }
    if( installedTransformID.length == 0 )
        statusJProgressBar.setValue(64);

        while(installedCount < initialInstallCount){
            try{
                Thread.sleep(1000l);
            }
            catch(Exception e){}
        }
    }


    private synchronized MTransformJPanel getMTransformJPanel(String transformName){
    Tid[] transformInstances = transformManager.transformInstances();
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

    public synchronized void removeFromRack(TransformContext transformContext){
        MTransformJPanel removableMTransformJPanel = (MTransformJPanel) transformContextHashtable.get( transformContext.getTid() );
        transformContextHashtable.remove( transformContext.getTid() );
        ((MRackJPanel)transformJPanel).removeTransform( removableMTransformJPanel );
        this.validate();
        this.repaint();
    }

    public synchronized Vector getAllMTransformJPanels(){
        return new Vector( transformContextHashtable.values() );
    }


    public void removeAllTransforms(){
    Tid[] allTransforms = transformManager.transformInstances();
    Tid tempTid;
    TransformContext tempTransformContext;
    for(int i = 0; i < allTransforms.length; i++){
        tempTid = allTransforms[i];
        tempTransformContext = transformManager.transformContext(tempTid);
        if( tempTransformContext.getTransformDesc().getName().equals("http-transform") )
        continue;
        removeTransform(tempTransformContext);
    }
    }


    public synchronized void removeTransform(TransformContext transformContext) {
        String name = null;
        try{
            name = transformContext.getTransformDesc().getName();
            transformManager.destroy( transformContext.getTid() );
        }
        catch(Exception e){
            try{
                Util.handleExceptionWithRestart("Error removing transform: " + name,  e);
            }
            catch(Exception f){
                Util.handleExceptionNoRestart("Error removing transform: " + name,  f);
            }
        }

        mMainJFrame.setButtonEnabled(name, true);
        removeFromRack(transformContext);
    }



    public MTransformJPanel addTransform(Object reference){


        MTransformJPanel mTransformJPanel = null;

        if(reference instanceof String){ // ADD DURING LIVE OPERATION
            Tid tID = null;
            Class transformGUIClass;
            TransformContext transformContext = null;
            Constructor transformConstructor = null;
            try{

                if( !((String)reference).equals("http-transform") ){
                    tID = transformManager.instantiate((String)reference);
                    transformContext = transformManager.transformContext(tID);
                    // System.out.println(" gui classname: " + transformManager.transformContext(tID).getTransformDesc().getGuiClassName());
                    transformGUIClass = Util.getClassLoader().loadClass(transformManager.transformContext(tID).getTransformDesc().getGuiClassName(), transformContext.getTransformDesc().getName());
                    // System.out.println(" gui classname: " + transformGUIClass);
                    transformConstructor = transformGUIClass.getConstructor(new Class[]{TransformContext.class});
                    mTransformJPanel = (MTransformJPanel) transformConstructor.newInstance(new Object[]{transformContext});
                    addToRack( transformContext, mTransformJPanel);
                }
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
                if( !((TransformContext)reference).getTransformDesc().getName().equals("http-transform")){
                    Class transformGUIClass = Util.getClassLoader().loadClass( ((TransformContext)reference).getTransformDesc().getGuiClassName(), ((TransformContext)reference).getTransformDesc().getName() );
                    Constructor transformGUIConstructor = transformGUIClass.getConstructor(new Class[]{TransformContext.class});
                    mTransformJPanel = (MTransformJPanel) transformGUIConstructor.newInstance(new Object[]{((TransformContext)reference)});
                    addToRack( ((TransformContext)reference), mTransformJPanel);
                }
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
                    statusJProgressBar.setValue(32 + (int) ((((float)installedCount)/(float)initialInstallCount)*32f) );
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


    public void focusMTransformJPanel(Rectangle newBounds){
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
        contentJPanel = new javax.swing.JPanel();
        transformJPanel = new MRackJPanel();
        jLabel1 = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        setBackground(new java.awt.Color(0, 51, 51));
        setMinimumSize(new java.awt.Dimension(800, 500));
        setPreferredSize(new java.awt.Dimension(800, 500));
        setOpaque(false);
        mPipelineJScrollPane.setBackground(new java.awt.Color(51, 51, 51));
        mPipelineJScrollPane.setBorder(null);
        mPipelineJScrollPane.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        mPipelineJScrollPane.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        mPipelineJScrollPane.setDoubleBuffered(true);
        mPipelineJScrollPane.setMinimumSize(new java.awt.Dimension(720, 21));
        mPipelineJScrollPane.setOpaque(false);
        mPipelineJScrollPane.getViewport().setOpaque(false);
        contentJPanel.setLayout(new java.awt.GridBagLayout());

        contentJPanel.setOpaque(false);
        transformJPanel.setBackground(new java.awt.Color(51, 255, 51));
        transformJPanel.setMaximumSize(null);
        transformJPanel.setMinimumSize(null);
        transformJPanel.setPreferredSize(null);
        transformJPanel.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        contentJPanel.add(transformJPanel, gridBagConstraints);

        mPipelineJScrollPane.setViewportView(contentJPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 10);
        add(mPipelineJScrollPane, gridBagConstraints);

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/pipeline/VerticalScrollBar42x1200.png")));
        jLabel1.setDoubleBuffered(true);
        jLabel1.setOpaque(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weighty = 1.0;
        add(jLabel1, gridBagConstraints);

    }//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel contentJPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane mPipelineJScrollPane;
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
