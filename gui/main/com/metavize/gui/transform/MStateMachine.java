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

package com.metavize.gui.transform;

import javax.swing.*;

import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.transform.BlinkJLabel;
import com.metavize.gui.util.*;

import com.metavize.mvvm.tran.*;


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
    
    public MStateMachine( MTransformJPanel mTransformJPanel ) {
                             
         this.mTransformJPanel = mTransformJPanel;
         this.mTransformControlsJPanel = mTransformJPanel.mTransformControlsJPanel();
         this.mTransformDisplayJPanel = mTransformJPanel.mTransformDisplayJPanel();
         this.powerJToggleButton = mTransformJPanel.powerJToggleButton();
         this.transformContext = mTransformJPanel.transformContext();
         this.stateJLabel = mTransformJPanel.stateJLabel();
         this.saveJButton = mTransformControlsJPanel.saveJButton();
         this.reloadJButton = mTransformControlsJPanel.reloadJButton();
         this.removeJButton = mTransformControlsJPanel.removeJButton();
                  
         new RefreshStateThread();
    }

 
    // ACTION MULTIPLEXER ///////////////////////////////
    public void actionPerformed(java.awt.event.ActionEvent evt) {              
        Object source = evt.getSource();

        if( Util.getIsDemo() && !source.equals(reloadJButton) )
            return;
        try{
            String name = transformContext.getTransformDesc().getName();
            String displayName = transformContext.getTransformDesc().getDisplayName();
            
            if( source.equals(saveJButton) ){
                boolean isProceeding = true;
                if( name.equals("nat-transform") ){
                    isProceeding = (new SaveProceedDialog( displayName )).isProceeding();
                }
                if(isProceeding)
                    new SaveThread();
            }
            else if( source.equals(reloadJButton) ){ new RefreshThread(); }
            else if( source.equals(removeJButton) ){
                if( (new RemoveProceedDialog(displayName)).isProceeding() ){
                    new RemoveThread(false);
                }
            }
            else if( source.equals(powerJToggleButton) ){ 
		int modifiers = evt.getModifiers();
		if( (modifiers & java.awt.event.ActionEvent.SHIFT_MASK) > 0 ){
		    if( (modifiers & java.awt.event.ActionEvent.CTRL_MASK) == 0 ){
                        if( (new RemoveProceedDialog(displayName)).isProceeding() ){
                            new RemoveThread(false);
                        }
                        else{
                            boolean wasEnabled = powerJToggleButton.isEnabled();
                            powerJToggleButton.setEnabled(false);
                            powerJToggleButton.setSelected( !powerJToggleButton.isSelected() );
                            powerJToggleButton.setEnabled(wasEnabled);
                        }
		    }
		    else{
			// new RemoveThread(true); not implemented properly now
		    }
		}
		else{
                    boolean isProceeding = true;
                    if( name.equals("nat-transform") ){
                        isProceeding = (new PowerProceedDialog(displayName, powerJToggleButton.isSelected())).isProceeding();
                    }
                    if(isProceeding)
                        new PowerThread();
                    else{
                            boolean wasEnabled = powerJToggleButton.isEnabled();
                            powerJToggleButton.setEnabled(false);
                            powerJToggleButton.setSelected( !powerJToggleButton.isSelected() );
                            powerJToggleButton.setEnabled(wasEnabled);
                    }
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
                setProblemView(true);
            }
        }
    }
    ////////////////////////////////////////////


    // ACTION THREADS //////////////////////////
    class SaveThread extends Thread{
	public SaveThread(){
	    super("MVCLIENT-MStateMachine.SaveThread: " + transformContext.getMackageDesc().getDisplayName());
	    saveJButton.setText("<html>(saving)</html>");
	    setProcessingView(false);
	    this.start();
	}
	public void run(){
	    try{
		mTransformControlsJPanel.saveAll();
		refreshState(true);
	    }
	    catch(Exception e){
		try{
		    Util.handleExceptionWithRestart("Error doing save", e);
		}
		catch(Exception f){
		    Util.handleExceptionNoRestart("Error doing save", f);
		    setProblemView(true);
		}
	    }
	    finally{
		saveJButton.setText("<html><b>Save</b> Settings</html>");
	    }
	}
    }


    class RefreshThread extends Thread{
	public RefreshThread(){
	    super("MVCLIENT-MStateMachine.RefreshThread: " + transformContext.getMackageDesc().getDisplayName());
	    reloadJButton.setText("<html>(reloading)</html>");
	    setProcessingView(false);
	    this.start();
	}
	public void run(){
	    try{
		mTransformControlsJPanel.refreshAll();
		refreshState(true);
	    }
	    catch(Exception e){
		try{
		    Util.handleExceptionWithRestart("Error doing refresh", e);
		}
		catch(Exception f){
		    Util.handleExceptionNoRestart("Error doing refresh", f);
		    setProblemView(true);
		}
	    }
	    finally{
		reloadJButton.setText("<html><b>Reload</b> Settings</html>");
	    }
	}
    }

    class RemoveThread extends Thread{
	private boolean removeAll;
	public RemoveThread(boolean removeAll){
	    super("MVCLIENT-MStateMachine.RemoveThread: " + transformContext.getMackageDesc().getDisplayName());
	    this.removeAll = removeAll;
	    setRemovingView(false);
            mTransformControlsJPanel.collapseControlPanel();   
	    this.start();
	}
	public void run(){
	    try{
		String transformName = transformContext.getTransformDesc().getName();

		mTransformDisplayJPanel.killGraph();
		Util.getMPipelineJPanel().removeTransform(mTransformJPanel);

	    }
	    catch(Exception e){
		try{
		    Util.handleExceptionWithRestart("Error doing remove", e);
		}
		catch(Exception f){
		    Util.handleExceptionNoRestart("Error doing remove", f);
		    setProblemView(true);
		}
	    }
	}
    }

    class PowerThread extends Thread{
	private final boolean powerOn;
	public PowerThread(){
	    super("MVCLIENT-MStateMachine.PowerThread: " + transformContext.getMackageDesc().getDisplayName());
	    powerOn = powerJToggleButton.isSelected();
	    if( powerOn )
		setStartingView(false);
	    else
		setStoppingView(false);
	    this.start();
	}

	public void run(){
	    try{
		if(powerOn)
		    transformContext.transform().start();
		else
		    transformContext.transform().stop();
		
		SwingUtilities.invokeLater( new Runnable() { public void run(){
		    if( powerOn )
			setOnView(true);
		    else
			setOffView(true);
		}});
	    }
	    catch(Exception e){
		try{
		    Util.handleExceptionWithRestart("Error doing power", e);
		}
		catch(Exception f){
		    Util.handleExceptionNoRestart("Error doing power", f);
		    setProblemView(true);
		}
	    }	    
	}
    }
    ///////////////////////////////////////////////

    
    // VIEW SETTING //////////////////////////////// 
    private void setProcessingView(boolean doLater){ setView( doLater, false, false, false, false, false, null, true, BlinkJLabel.PROCESSING_STATE ); }
    private void setProblemView(boolean doLater){    setView( doLater, false, false, false, true,  false, null, true, BlinkJLabel.PROBLEM_STATE ); }
    private void setStartingView(boolean doLater){ setView( doLater, false, false, false, false, false, null, false, BlinkJLabel.STARTING_STATE ); }
    private void setStoppingView(boolean doLater){ setView( doLater, false, false, false, false, false, null, false, BlinkJLabel.STOPPING_STATE ); }
    private void setRemovingView(boolean doLater){ setStoppingView(doLater); }
    private void setOnView(boolean doLater){ 	    setView( doLater, true, true, true, true, true, true, true,  BlinkJLabel.ON_STATE ); }
    private void setOffView(boolean doLater){ 	    setView( doLater, true, true, true, true, true, false, false, BlinkJLabel.OFF_STATE ); }
    
    private void setView(final boolean doLater, final boolean allControlsEnabled, final boolean saveEnabled,
			 final boolean refreshEnabled, final boolean removeEnabled, final boolean powerEnabled,
			 final Boolean powerOn, final boolean updateGraph, final int ledState){

	Runnable runnable = new Runnable(){
		public void run(){
		    mTransformControlsJPanel.setAllEnabled( allControlsEnabled );
		    saveJButton.setEnabled( saveEnabled );
		    reloadJButton.setEnabled( refreshEnabled );
		    removeJButton.setEnabled( removeEnabled );
		    if( Util.getIsDemo() )
			powerJToggleButton.setEnabled(false);
		    else
			powerJToggleButton.setEnabled( powerEnabled );
		    
		    if( powerOn != null){
			boolean wasEnabled = powerJToggleButton.isEnabled();
			powerJToggleButton.setEnabled(false);
			powerJToggleButton.setSelected( powerOn );
			powerJToggleButton.setEnabled(wasEnabled);
		    }
		    mTransformDisplayJPanel.setUpdateGraph( updateGraph );
		    stateJLabel.setViewState( ledState );
		}
	    };
	if( doLater )
	    SwingUtilities.invokeLater( runnable );
	else
	    runnable.run();
    }
    ///////////////////////////////////////////////


    // STATE REFRESHING //////////////////////////
    private void refreshState(boolean doLater){
	TransformState transformState = transformContext.transform().getRunState();
	if( TransformState.RUNNING.equals( transformState ) )
	    setOnView(doLater);
	else if( TransformState.INITIALIZED.equals( transformState ) )
	    setOffView(doLater);
	else
	    setProblemView(doLater);
    }
     
    class RefreshStateThread extends Thread{
	public RefreshStateThread(){
	    super("MVCLIENT-MStateMachine.RefreshStateThread: " + transformContext.getMackageDesc().getDisplayName());
	    this.start();
	}
	public void run(){
	    try{
		refreshState(true);
	    }
	    catch(Exception e){
		try{
		    Util.handleExceptionWithRestart("Error refreshing state", e);
		}
		catch(Exception f){
		    Util.handleExceptionNoRestart("Error refreshing state: ", f);
		    setProblemView(true);
		}
	    }
	}
    }
    ///////////////////////////////////
    
}
