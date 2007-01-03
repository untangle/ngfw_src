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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class DropdownTask implements ActionListener {
    
    // CONSTANTS ////////////////////
    private static final int STEPS = 6;
    private static final int STEP_DELAY = 50;
    /////////////////////////////////
    
    private Timer dropdownTask;
    private JComponent parentContainer;
    private JComponent childComponent;
    private JToggleButton controlsJToggleButton;
    private Dimension minDimension, maxDimension;
    private int width, height, x, y, yStart, yFinish;
    private boolean goingDown;
    private int currentStep;
    private int currentY;
    private JProgressBar jProgressBar;

    public DropdownTask(JComponent parentContainer, JComponent childComponent, JToggleButton controlsJToggleButton,
			Dimension minDimension, Dimension maxDimension,
			int width, int height, int x, int yStart, int yFinish){
	this.parentContainer = parentContainer;
	this.childComponent = childComponent;
	this.controlsJToggleButton = controlsJToggleButton;
	this.minDimension = minDimension;
	this.maxDimension = maxDimension;
	this.width = width;
	this.height = height;
	this.x = x;
	this.yStart = yStart;
	this.yFinish = yFinish;
	dropdownTask = new Timer(STEP_DELAY, this);
    }

    public void start(boolean goingDown, JProgressBar jProgressBar){
	this.goingDown = goingDown;
	this.jProgressBar = jProgressBar;
	controlsJToggleButton.setEnabled(false);
        if(goingDown){
	    currentStep = 0;
            controlsJToggleButton.setText("Hide Settings");
        }
        else{
	    currentStep = STEPS -1;
            controlsJToggleButton.setText("Show Settings");
        }
	dropdownTask.start();
    }

    public void actionPerformed(ActionEvent evt){

	if( goingDown ){
	    updatePanelPosition();
	    if( currentStep == 0 ){
		childComponent.setVisible(true);
		parentContainer.setPreferredSize(maxDimension);
		parentContainer.revalidate();
		parentContainer.repaint();
	    }
	    if( currentStep == STEPS-1 ){
		controlsJToggleButton.setEnabled(true);
		dropdownTask.stop();
		if(jProgressBar.isVisible())
		    jProgressBar.setVisible(false);
		return;
	    }
	    currentStep++;
	}
	else{
	    currentStep--;
	    if( currentStep > -1 ){
		updatePanelPosition();
	    }
	    else{
		childComponent.setVisible(false);
		parentContainer.setPreferredSize(minDimension);
		parentContainer.revalidate();
		parentContainer.repaint();
		controlsJToggleButton.setEnabled(true);
		dropdownTask.stop();
		return;
	    }
	}
    }
    
    private void updatePanelPosition(){
	currentY = (int)(((float)(yFinish - yStart) / (float)(STEPS-1))*currentStep + (float)yStart);
	parentContainer.remove( childComponent );
	parentContainer.add( childComponent, new org.netbeans.lib.awtextra.AbsoluteConstraints(x, currentY, width, height));
	parentContainer.revalidate();
	parentContainer.repaint();
    }

}
