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
