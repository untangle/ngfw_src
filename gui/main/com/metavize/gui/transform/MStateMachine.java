/*
 * MStateMachine.java
 *
 * Created on November 19, 2004, 3:41 PM
 */

package com.metavize.gui.transform;

import javax.swing.*;

import com.metavize.gui.transform.BlinkJLabel;
import com.metavize.gui.util.*;

import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.tran.TransformState;

/**
 *
 * @author  inieves
 */
public class MStateMachine implements java.awt.event.ActionListener {
    
    // references to components that can generate actions
    private JButton saveJButton;
    private JButton reloadJButton;
    private JButton removeJButton;
    private JToggleButton powerJToggleButton;
    private BlinkJLabel stateJLabel;
    private MTransformJPanel mTransformJPanel;
    private MTransformControlsJPanel mTransformControlsJPanel;
    private MTransformDisplayJPanel mTransformDisplayJPanel;
    private TransformContext transformContext;
    
    private TransformState lastTransformState;
    
    public MStateMachine(MTransformJPanel mTransformJPanel,
                         MTransformControlsJPanel mTransformControlsJPanel,
                         MTransformDisplayJPanel mTransformDisplayJPanel) {
                             
         this.mTransformJPanel = mTransformJPanel;
         this.mTransformControlsJPanel = mTransformControlsJPanel;
         this.mTransformDisplayJPanel = mTransformDisplayJPanel;
         
         this.powerJToggleButton = mTransformJPanel.powerJToggleButton();
         this.transformContext = mTransformJPanel.transformContext();
         this.stateJLabel = mTransformJPanel.stateJLabel();
         this.saveJButton = mTransformControlsJPanel.saveJButton();
         this.reloadJButton = mTransformControlsJPanel.reloadJButton();
         this.removeJButton = mTransformControlsJPanel.removeJButton();
         
         
         updateTransformState();
    }
 
    // handle any of the actions that might occur
    public void actionPerformed(java.awt.event.ActionEvent evt) {
                
        Object source = evt.getSource();
        
        if( Util.getIsDemo() && !source.equals(reloadJButton) )
            return;
        
        try{
            if( source.equals(saveJButton) ){ doSaveAll(); }
            else if( source.equals(reloadJButton) ){ doRefreshAll(); }
            else if( source.equals(removeJButton) ){ doRemove(); }
            else if( source.equals(powerJToggleButton) ){ 
		int modifiers = evt.getModifiers();
		if( (modifiers & java.awt.event.ActionEvent.SHIFT_MASK) > 0 ){
		    if( (modifiers & java.awt.event.ActionEvent.CTRL_MASK) == 0 ){
			doRemove();
		    }
		    else{
			Util.getMPipelineJPanel().removeAllTransforms();
			Util.setEmailDetectionFprotJPanel(null);
			Util.setEmailDetectionSophosJPanel(null);
                        Util.setEmailDetectionHauriJPanel(null);
			Util.setVirusMEditTableJPanel(null);

		    }
		}
		else{
		    doPower(); 
		}
	    }
            else{ Util.printMessage("error: unknown action source: " + source); }
        }
        catch(Exception e){
            try{
                Util.handleExceptionWithRestart("Error handling action", e);
            }
            catch(Exception f){
                Util.handleExceptionNoRestart("Error getting last state", f);
                doProblem();
            }
        }
    }
    
    // handle the various outcomes
    private void doSaveAll(){
        doProcessing();
        stateJLabel.setTransferState();
        Thread saveThread = new Thread() {
            public void run() {
                mTransformControlsJPanel.saveAll();
                updateTransformState();
            }
        };
        saveThread.start();
    }
    private void doRefreshAll(){
        doProcessing();
        stateJLabel.setTransferState();
        Thread refreshThread = new Thread() {
            public void run() {
                mTransformControlsJPanel.refreshAll();
                updateTransformState();
            }
        };
        refreshThread.start();
    }
    private void doRemove(){
        doProcessing();
        stateJLabel.setRemovingState();
        synchronized( Util.getPipelineSync() ){
            Thread removeThread = new Thread() {
                public void run() {
                    mTransformDisplayJPanel.killGraph();
		    String transformName = transformContext.getTransformDesc().getName();
		    if(transformName.equals("fprot-transform"))
			Util.setEmailDetectionFprotJPanel(null);
		    else if(transformName.equals("sophos-transform"))
			Util.setEmailDetectionSophosJPanel(null);
                    else if(transformName.equals("hauri-transform"))
			Util.setEmailDetectionHauriJPanel(null);
		    else if(transformName.equals("email-transform"))
			Util.setVirusMEditTableJPanel(null);
		    else
			System.err.println("REMOVING: " + transformName);
		    Util.updateDependencies();
		    mTransformControlsJPanel.collapseControlPanel();
                    Util.getMPipelineJPanel().removeTransform(transformContext);            
                }
            };
            removeThread.start();
        }
    }
    private void doPower(){
        //System.err.println("doPower: " + powerJToggleButton.isSelected() );
        doProcessing();
        Thread powerThread = new Thread() {
            public void run() {
                try{
                    if( powerJToggleButton.isSelected() ){
                        stateJLabel.setStartingState();
                        transformContext.transform().start();
                        mTransformDisplayJPanel.setUpdateGraph(true);
                        stateJLabel.setOnState();
                    }
                    else {
                        stateJLabel.setStoppingState();
                        mTransformDisplayJPanel.setUpdateGraph(false);
                        transformContext.transform().stop();
                        stateJLabel.setOffState();
                        
                    }
                }
                catch(Exception e){
                    try{
                        Util.handleExceptionWithRestart("Error doing power", e);
                    }
                    catch(Exception f){
                        Util.handleExceptionNoRestart("Error doing power", f);
                        updateTransformState();
                        return;
                    }
                }
                updateTransformState();
            }
        };
        powerThread.start();
    }
    
    
    // statically set the buttons and visuals for when there are problems
    private void doProblem(){
        //System.err.println("doProblem");
        mTransformControlsJPanel.setAllEnabled(false);
        //mTransformControlsJPanel.mTabbedPane.setEnabled(false);
        saveJButton.setEnabled(false);
        reloadJButton.setEnabled(false);
        removeJButton.setEnabled(true);
        powerJToggleButton.setEnabled(true);
        stateJLabel.setProblemState();
        mTransformDisplayJPanel.setUpdateGraph(false);
        
        if(Util.getIsDemo())
            powerJToggleButton.setEnabled(false);
    }
    
    // statically prevent buttons from being pressed
    private void doProcessing(){
        //System.err.println("doProcessing");
        //mTransformControlsJPanel.mTabbedPane.setEnabled(false);
        mTransformControlsJPanel.setAllEnabled(false);
        saveJButton.setEnabled(false);
        reloadJButton.setEnabled(false);
        removeJButton.setEnabled(false);
        powerJToggleButton.setEnabled(false);
    }
    
    // statically set the buttons and visuals for when there is no problem
    private void doNoProblem(TransformState transformState){
        if( !TransformState.RUNNING.equals(transformState) && !TransformState.INITIALIZED.equals(transformState) )
            return;
        mTransformControlsJPanel.setAllEnabled(true);
        //mTransformControlsJPanel.mTabbedPane.setEnabled(true);
        saveJButton.setEnabled(true);
        reloadJButton.setEnabled(true);
        removeJButton.setEnabled(true);
        powerJToggleButton.setEnabled(false);
        powerJToggleButton.setSelected( TransformState.RUNNING.equals(transformState) ? true : false );
        powerJToggleButton.setEnabled(true);
        if(TransformState.RUNNING.equals(transformState)){
            stateJLabel.setOnState();
            mTransformDisplayJPanel.setUpdateGraph(true);
        }
        else if(TransformState.INITIALIZED.equals(transformState)){
            stateJLabel.setOffState();
            mTransformDisplayJPanel.setUpdateGraph(false);
        }
        if(Util.getIsDemo())
            powerJToggleButton.setEnabled(false);
    }
    
    // statically set the visual for the latest state
    private boolean updateTransformState(){
        //System.err.println("updateTransformState");
         try{
            lastTransformState = transformContext.transform().getRunState();
         }
         catch(Exception e){
            try{
                Util.handleExceptionWithRestart("Error getting last state", e);
            }
            catch(Exception f){
                Util.handleExceptionNoRestart("Error getting last state: ", f);
                doProblem();
                return true;
            }
         }
         if( TransformState.RUNNING.equals(lastTransformState) || TransformState.INITIALIZED.equals(lastTransformState)){
             doNoProblem(lastTransformState);
         }
         else{
             doProblem();
             return true;
         }
         return false;
    }
    
}
