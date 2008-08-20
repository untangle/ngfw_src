/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
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
