/*
 * MRackJPanel.java
 *
 * Created on January 10, 2005, 1:21 PM
 */

package com.metavize.gui.pipeline;

import com.metavize.gui.transform.ButtonKey;
import com.metavize.gui.transform.MTransformJPanel;
import com.metavize.gui.util.Util;

import java.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;


/**
 *
 * @author  inieves
 */
public class MRackJPanel extends JPanel {

    private Map<ButtonKey,MTransformJPanel> rackMap;
    public Map<ButtonKey,MTransformJPanel> getRackMap(){ return rackMap; }
    
    private static final int MIN_RACK_HEIGHT = 3;
    private static final int RACK_BUFFER = 1;

    private GridBagConstraints transformConstraints;
    private GridBagConstraints transformPanelConstraints;
    // private ImageIcon RackBottom;
    // private ImageIcon RackTop;
    private ImageIcon RackLeftFull;
    private ImageIcon RackRightFull;
    private ImageIcon RackLeftShort;
    private ImageIcon RackRightShort;

    private GridBagConstraints rackTopConstraints, rackMiddleConstraints, rackBottomConstraints, glueConstraints;
    private JLabel rackTopJLabel, rackBottomJLabel;

    private JPanel transformJPanel;


    /** Creates a new instance of MRackJPanel */
    public MRackJPanel() {

        transformConstraints = new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0d, 0d, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(1,0,0,0), 0, 0);
        glueConstraints = new GridBagConstraints(0, GridBagConstraints.REMAINDER, 1, 1, 0d, 1d, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0);

        rackTopConstraints    = new GridBagConstraints(0, 0, 1, 1, 0d, 0d, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,0,101*RACK_BUFFER,12), 0, 0);
        rackMiddleConstraints = new GridBagConstraints(0, 1, 1, 1, 0d, 0d, GridBagConstraints.NORTH, GridBagConstraints.NONE, new Insets(0,0,0,12), 0, 0);
        rackBottomConstraints = new GridBagConstraints(0, 2, 1, 1, 0d, 1d, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(1 + 101*RACK_BUFFER,0,0,12), 0, 0);

        // RackBottom = new ImageIcon( getClass().getResource("/com/metavize/gui/pipeline/BottomRack718x39.png") );
        // RackTop = new ImageIcon( getClass().getResource("/com/metavize/gui/pipeline/TopRack718x39.png") );
        RackLeftFull = new ImageIcon( getClass().getResource("/com/metavize/gui/pipeline/LeftRack59x100.png") );
        RackRightFull = new ImageIcon( getClass().getResource("/com/metavize/gui/pipeline/RightRack59x100.png") );
        RackLeftShort = new ImageIcon( getClass().getResource("/com/metavize/gui/pipeline/LeftRack59x1.png") );
        RackRightShort = new ImageIcon( getClass().getResource("/com/metavize/gui/pipeline/RightRack59x1.png") );

        rackTopJLabel = new JLabel();
        //rackTopJLabel.setIcon(RackTop);
        rackTopJLabel.setOpaque(false);
        rackTopJLabel.setHorizontalAlignment(SwingConstants.CENTER);
        rackTopJLabel.setVerticalAlignment(SwingConstants.BOTTOM);
        //rackTopJLabel.setVisible(false);
        //rackTopJLabel.add(Box.createVerticalStrut(39));
        rackBottomJLabel = new JLabel();
        //rackBottomJLabel.setIcon(RackBottom);
        rackBottomJLabel.setOpaque(false);
        rackBottomJLabel.setHorizontalAlignment(SwingConstants.CENTER);
        rackBottomJLabel.setVerticalAlignment(SwingConstants.TOP);
        //rackBottomJLabel.setVisible(false);
        //rackBottomJLabel.add(Box.createVerticalStrut(39));



        transformJPanel = new JPanel();
        transformJPanel.setOpaque(false);
        transformJPanel.setBackground(Color.RED);
        transformJPanel.setLayout(new GridBagLayout());


        this.setLayout(new GridBagLayout());
        this.add(rackTopJLabel, rackTopConstraints);
        this.add(transformJPanel, rackMiddleConstraints);
        this.add(Box.createRigidArea(new Dimension(718, 101*MIN_RACK_HEIGHT)), rackMiddleConstraints);
        this.add(rackBottomJLabel, rackBottomConstraints);

        rackMap = new TreeMap<ButtonKey,MTransformJPanel>();
	Util.setMRackJPanel(this);
    }

    public synchronized void addTransform(MTransformJPanel mTransformJPanel){
        ButtonKey buttonKey = new ButtonKey(mTransformJPanel);
        if( rackMap.containsKey(buttonKey) )
            return;
        
        rackMap.put(buttonKey, mTransformJPanel);
        int position = ((TreeMap)rackMap).headMap(buttonKey).size();
        transformJPanel.add(mTransformJPanel, transformConstraints, position);
        this.revalidate();
    }

    public synchronized void removeTransform(MTransformJPanel mTransformJPanel){
        ButtonKey buttonKey = new ButtonKey(mTransformJPanel);
        rackMap.remove(buttonKey);
        transformJPanel.remove(mTransformJPanel);
        this.revalidate();
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

        Graphics2D g2d = (Graphics2D)g.create();

        paintIndex = (paintHeight-1)/101;
        if(paintIndex < MIN_RACK_HEIGHT)
            paintIndex = MIN_RACK_HEIGHT;

        for(paintI=0; paintI <= paintIndex; paintI++){
            RackLeftShort.paintIcon(this, g2d, (paintWidth-718)/2 -6, paintI*100 + (paintI));
            RackRightShort.paintIcon(this, g2d, (paintWidth-718)/2 + 659 -6,  paintI*100 + (paintI));

            RackLeftFull.paintIcon(this, g2d, (paintWidth-718)/2 -6, paintI*100 + (paintI+1));
            RackRightFull.paintIcon(this, g2d, (paintWidth-718)/2 + 659 -6,  paintI*100 + (paintI+1));
    }
        RackLeftShort.paintIcon(this, g2d, (paintWidth-718)/2 -6, paintI*100 + (paintIndex));
        RackRightShort.paintIcon(this, g2d, (paintWidth-718)/2 + 659 -6,  paintI*100 + (paintIndex));

        g2d.dispose();
    }


}
