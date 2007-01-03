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

package com.untangle.gui.pipeline;

import com.untangle.gui.transform.ButtonKey;
import com.untangle.gui.transform.MTransformJPanel;
import com.untangle.gui.util.Util;

import java.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;



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
