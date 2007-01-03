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

package com.untangle.gui.widgets.separator;


import javax.swing.border.*;
import javax.swing.*;
import java.awt.*;

public class Separator extends JLabel {

    private static ImageIcon backgroundImageIcon;
    private static ImageIcon arrowImageIcon;
    private String foregroundText = "";
    private static Color foregroundTextColor;
    private static Font foregroundTextFont;

    private JComboBox jComboBox;

    public Separator(boolean showComboBox){
	init();
	if(showComboBox){
	    setLayout(new GridBagLayout());
	    jComboBox = new JComboBox();
	    jComboBox.setFocusable(false);
	    jComboBox.setBackground(new Color(173,173,173));
	    jComboBox.setForeground(new Color(129,129,129));
	    jComboBox.setFont(new Font("Arial", Font.PLAIN, 20));
	    jComboBox.setRenderer( new SeparatorRenderer(jComboBox.getRenderer()) );
	    jComboBox.setMinimumSize(new Dimension(240,30));
	    jComboBox.setMaximumSize(new Dimension(240,30));
	    jComboBox.setPreferredSize(new Dimension(240,30));
	    GridBagConstraints comboBoxConstraints = new GridBagConstraints(0,0,1,1,1d,1d,GridBagConstraints.WEST,GridBagConstraints.NONE, new Insets(0,84,0,0),0,0);
	    add(jComboBox, comboBoxConstraints);
	}
    }

    public JComboBox getJComboBox(){ return jComboBox; }

    private void init(){
	if( backgroundImageIcon == null ){
	    backgroundImageIcon = new ImageIcon( getClass().getResource("/com/untangle/gui/widgets/separator/PlainSeparator688x50.png") );
	    arrowImageIcon = new ImageIcon( getClass().getResource("/com/untangle/gui/widgets/separator/SeparatorArrow.png") );
	    foregroundTextColor = new Color(129,129,129);
	    foregroundTextFont = new Font("Arial",Font.PLAIN, 20);
	}
	super.setOpaque(false);
	super.setIcon(backgroundImageIcon);
    }

    public void setForegroundText(String foregroundText){
	this.foregroundText = foregroundText;
	repaint();
    }

    public void paintComponent(Graphics g){
	super.paintComponent(g);
	if( foregroundText.length() > 0 ){
	    //g.drawImage(arrowImageIcon.getImage(), 67, 20, (java.awt.image.ImageObserver)null);
	    g.drawImage(arrowImageIcon.getImage(), 60, 15, (java.awt.image.ImageObserver)null);
	    Graphics2D g2 = (Graphics2D) g;
	    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
	    g.setColor(foregroundTextColor);
	    g.setFont(foregroundTextFont);
	    // g.drawString(foregroundText, 51, 32);
	    g.drawString(foregroundText, 88, 32);
	}
    }
    


}

class SeparatorRenderer implements ListCellRenderer {
    private ListCellRenderer listCellRenderer;
    public SeparatorRenderer(ListCellRenderer listCellRenderer){
	this.listCellRenderer = listCellRenderer;
    }
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus){
	Component tempComponent = listCellRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
	try{ ((JComponent)tempComponent).putClientProperty(com.sun.java.swing.SwingUtilities2.AA_TEXT_PROPERTY_KEY, new Boolean(true)); }
	catch(Exception e){}
	return tempComponent;
    }
}
