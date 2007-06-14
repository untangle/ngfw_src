/*
 * $HeadURL:$
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

package com.untangle.gui.pipeline;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

import com.untangle.gui.util.Util;



public class MRackJPanel extends JPanel {

    private static final int HALF_U_HEIGHT = 50;
    private static final int FULL_U_HEIGHT = 100;
    private static final int MIN_RACK_HEIGHT = 2; // in half U
    private static final int RACK_BUFFER_TOP = 1; // in half U
    private static final int RACK_BUFFER_BOT = 1; // in half U

    // private ImageIcon RackBottom;
    // private ImageIcon RackTop;
    private ImageIcon RackLeftFull;
    private ImageIcon RackRightFull;
    private ImageIcon RackLeftShort;
    private ImageIcon RackRightShort;

    private GridBagConstraints rackTopConstraints, rackMiddleConstraints, rackBottomConstraints;//, glueConstraints;
    private JLabel rackTopJLabel, rackBottomJLabel;


    public MRackJPanel() {
        setDoubleBuffered(true);
        /*
          glueConstraints = new GridBagConstraints(0, GridBagConstraints.REMAINDER, 1, 1, 0d, 1d,
          GridBagConstraints.CENTER, GridBagConstraints.BOTH,
          new Insets(0,0,0,0), 0, 0);
        */
        rackTopConstraints = new GridBagConstraints(0, 0, 1, 1, 0d, 0d,
                                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                    new Insets(0,0,1 + HALF_U_HEIGHT*RACK_BUFFER_TOP,12), 0, 0);
        rackMiddleConstraints = new GridBagConstraints(0, 1, 1, 1, 0d, 0d,
                                                       GridBagConstraints.NORTH, GridBagConstraints.NONE,
                                                       new Insets(0,0,0,12), 0, 0);
        rackBottomConstraints = new GridBagConstraints(0, 6, 1, 1, 0d, 1d,
                                                       GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                       new Insets(1 + 1 + HALF_U_HEIGHT*RACK_BUFFER_BOT,0,0,12), 0, 0);

        // RackBottom = new ImageIcon( getClass().getResource("/com/untangle/gui/pipeline/BottomRack718x39.png") );
        // RackTop = new ImageIcon( getClass().getResource("/com/untangle/gui/pipeline/TopRack718x39.png") );
        RackLeftFull = new ImageIcon( getClass().getResource("/com/untangle/gui/pipeline/LeftRack59x100.png") );
        RackRightFull = new ImageIcon( getClass().getResource("/com/untangle/gui/pipeline/RightRack59x100.png") );
        RackLeftShort = new ImageIcon( getClass().getResource("/com/untangle/gui/pipeline/LeftRack59x1.png") );
        RackRightShort = new ImageIcon( getClass().getResource("/com/untangle/gui/pipeline/RightRack59x1.png") );

        rackTopJLabel = new JLabel();
        rackTopJLabel.setOpaque(false);
        rackTopJLabel.setBackground(new Color(200,200,200,200));
        rackTopJLabel.setHorizontalAlignment(SwingConstants.CENTER);
        rackTopJLabel.setVerticalAlignment(SwingConstants.BOTTOM);
        //rackTopJLabel.setIcon(RackTop);
        //rackTopJLabel.setVisible(false);
        //rackTopJLabel.add(Box.createVerticalStrut(39));
        rackBottomJLabel = new JLabel();
        rackBottomJLabel.setOpaque(false);
        rackBottomJLabel.setBackground(new Color(200,200,200,200));
        rackBottomJLabel.setHorizontalAlignment(SwingConstants.CENTER);
        rackBottomJLabel.setVerticalAlignment(SwingConstants.TOP);
        //rackBottomJLabel.setIcon(RackBottom);
        //rackBottomJLabel.setVisible(false);
        //rackBottomJLabel.add(Box.createVerticalStrut(39));

        this.setLayout(new GridBagLayout());
        this.add(rackTopJLabel, rackTopConstraints);
        //        this.add(Box.createRigidArea(new Dimension(718, 1+HALF_U_HEIGHT*MIN_RACK_HEIGHT)), rackMiddleConstraints);
        this.add(rackBottomJLabel, rackBottomConstraints);

        Util.setMRackJPanel(this);
    }


    private int paintIndex;
    private int paintHeight;
    private int paintWidth;
    private int paintI;

    protected void paintComponent(Graphics g){
        super.paintComponent(g);

        paintHeight = this.getHeight();
        paintWidth  = this.getWidth();

        if (isOpaque()) { //paint background
            g.setColor(getBackground());
            g.fillRect(0, 0, paintWidth, paintHeight);
        }

        Graphics2D g2d = (Graphics2D) g.create();

        paintIndex = (paintHeight-1)/(1 + FULL_U_HEIGHT);
        if(paintIndex < MIN_RACK_HEIGHT)
            paintIndex = MIN_RACK_HEIGHT;

        for(paintI=0; paintI <= paintIndex; paintI++){
            RackLeftShort.paintIcon(this, g2d, (paintWidth-718)/2 -6, paintI*FULL_U_HEIGHT + (paintI));
            RackRightShort.paintIcon(this, g2d, (paintWidth-718)/2 + 659 -6,  paintI*FULL_U_HEIGHT + (paintI));

            RackLeftFull.paintIcon(this, g2d, (paintWidth-718)/2 -6, paintI*FULL_U_HEIGHT + (paintI+1));
            RackRightFull.paintIcon(this, g2d, (paintWidth-718)/2 + 659 -6,  paintI*FULL_U_HEIGHT + (paintI+1));
        }
        RackLeftShort.paintIcon(this, g2d, (paintWidth-718)/2 -6, paintI*FULL_U_HEIGHT + (paintIndex));
        RackRightShort.paintIcon(this, g2d, (paintWidth-718)/2 + 659 -6,  paintI*FULL_U_HEIGHT + (paintIndex));

        g2d.dispose();
    }


}
