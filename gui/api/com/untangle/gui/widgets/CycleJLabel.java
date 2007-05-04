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

package com.untangle.gui.widgets;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.untangle.gui.util.*;

public class CycleJLabel extends JLabel implements ActionListener{

    Timer animateTimer;
    ImageIcon[] imageIcons;
    int currentImage = 0;
    boolean doRepeat;
    boolean doBackwardsRepeat;
    boolean goingForward;

    public CycleJLabel(ImageIcon[] imageIcons, int delay, boolean doRepeat, boolean doBackwardsRepeat){
        this.doRepeat = doRepeat;
        this.doBackwardsRepeat = doBackwardsRepeat;
        goingForward = true;
        this.imageIcons = imageIcons;

        animateTimer = new Timer(delay, this);
    }

    public void actionPerformed(ActionEvent evt){
        if( currentImage >= imageIcons.length )
            return;
        setIcon( imageIcons[currentImage] );
        if( doRepeat ){
            if( doBackwardsRepeat ){
                if( goingForward ){
                    if( currentImage == imageIcons.length-1 ){
                        goingForward = false;
                        currentImage--;
                    }
                    else{
                        currentImage++;
                    }
                }
                else{
                    if( currentImage == 0 ){
                        goingForward = true;
                        currentImage++;
                    }
                    else{
                        currentImage--;
                    }
                }
            }
            else{
                currentImage++;
                if( currentImage == imageIcons.length-1 )
                    currentImage = 0;
            }
        }
        else{
            currentImage++;
        }

    }

    public synchronized void start(){
        if( imageIcons.length <= 1 )
            return;
        currentImage = 0;
        animateTimer.start();
    }
    public synchronized void stop(){
        animateTimer.stop();
        SwingUtilities.invokeLater( new Runnable(){ public void run(){
            setIcon(null);
        }});
    }
    public synchronized boolean isRunning(){
        return animateTimer.isRunning();
    }
}
