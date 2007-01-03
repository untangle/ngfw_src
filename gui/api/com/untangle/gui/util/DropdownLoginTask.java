/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */


package com.untangle.gui.util;

import com.untangle.gui.login.MLoginJFrame;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class DropdownLoginTask implements ActionListener {
    
    // CONSTANTS ////////////////////
    private static final int STEPS = 10;
    private static final int STEP_DELAY = 100;
    /////////////////////////////////
    
    private MLoginJFrame mLoginJFrame;
    private Timer dropdownTask;
    private Window parentWindow;
    private JComponent childComponent;
    private Dimension minDimension, maxDimension;
    private boolean goingDown;
    private int currentStep;
    private int currentY;

    public DropdownLoginTask(MLoginJFrame mLoginJFrame, JComponent childComponent){
	this.mLoginJFrame = mLoginJFrame;
	this.childComponent = childComponent;
	this.minDimension = childComponent.getMinimumSize();
	this.maxDimension = childComponent.getMaximumSize();
	dropdownTask = new Timer(STEP_DELAY, this);
    }

    public void start(boolean goingDown){
	this.goingDown = goingDown;
        if(goingDown){
	    currentStep = 0;
        }
        else{
	    currentStep = STEPS -1;
        }
	dropdownTask.start();
    }

    public void actionPerformed(ActionEvent evt){

	if( goingDown ){
	    if( currentStep == 0 ){
	    }
	    else if( (currentStep>0) && (currentStep<STEPS-1) ){
		updatePanelPosition();
	    }
	    else{ //( currentStep == STEPS-1 )
		updatePanelPosition();
		mLoginJFrame.setInputsEnabled(true);
		dropdownTask.stop();
		return;
	    }
	    currentStep++;
	}
	else{
	    currentStep--;
	    if( currentStep == STEPS-1 ){
		updatePanelPosition();
	    }
	    else if( (currentStep<STEPS-1) && (currentStep>-1) ){
		updatePanelPosition();
	    }
	    else{ // (currentStep==-1)
		dropdownTask.stop();
		return;
	    }
	}
    }
    
    private void updatePanelPosition(){
	currentY = (int) (minDimension.height + (((float)(maxDimension.height - minDimension.height))/((float)STEPS-1))*(float)currentStep);
	childComponent.setPreferredSize(new Dimension(childComponent.getWidth(), currentY));
	mLoginJFrame.pack();
	mLoginJFrame.repaint();
    }

}
