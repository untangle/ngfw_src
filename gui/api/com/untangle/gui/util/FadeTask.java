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

public class FadeTask implements ActionListener {

    // FADED BACKGROUND /////////////
    private static final int FADE_DELAY_MILLIS = 100;
    private static final float FADE_DECREMENT = .05f;
    private static final int FADE_CONSTANT_COUNTS = 6;
    /////////////////////////////////
    int redBack;
    int greenBack;
    int blueBack;
    private int fadeConstantCount = 0;
    private float fadeLeft = 1f;
    private javax.swing.Timer fadeTimer;
    private int redInit   = 104;
    private int greenInit = 189;
    private int blueInit  = 73;
    private int alpha;
    private JComponent jComponent;
    private Color backgroundColor;
    private boolean hasBackground;
    public FadeTask(JComponent jComponent, boolean hasBackground){
        fadeTimer = new javax.swing.Timer(FADE_DELAY_MILLIS, this);
        backgroundColor = jComponent.getBackground();
        this.hasBackground = hasBackground;
        redBack = backgroundColor.getRed();
        greenBack = backgroundColor.getGreen();
        blueBack = backgroundColor.getBlue();
        this.jComponent = jComponent;
        fadeTimer.start();
    }
    public void actionPerformed(ActionEvent evt){
        if ( fadeLeft > 0f ) {
            if( hasBackground ){
                jComponent.setBackground(new Color(
                                                   redInit,
                                                   greenInit,
                                                   blueInit,
                                                   (int)(255f*fadeLeft)));
            }
            else{
                jComponent.setBackground(new Color(
                                                   (int)(((float)redInit)+((float)(redBack-redInit)*(1f-fadeLeft))),
                                                   (int)(((float)greenInit)+((float)(greenBack-greenInit)*(1f-fadeLeft))),
                                                   (int)(((float)blueInit)+((float)(blueBack-blueInit)*(1f-fadeLeft))),
                                                   (int)255));
            }
            if( hasBackground )
                jComponent.getParent().repaint();
            else
                jComponent.repaint();
        }
        else{
            jComponent.setBackground(backgroundColor);
            if( hasBackground )
                jComponent.getParent().repaint();
            else
                jComponent.repaint();
            fadeTimer.stop();
        }
        if( fadeConstantCount < FADE_CONSTANT_COUNTS )
            fadeConstantCount++;
        else
            fadeLeft -= FADE_DECREMENT;
        if(fadeLeft<0f)
            fadeLeft = 0f;
    }
}
