/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.widgets.separator;

import javax.swing.*;
import java.awt.*;

public class Separator extends JLabel {

    private static ImageIcon backgroundImageIcon;
    private static ImageIcon arrowImageIcon;
    private String foregroundText = "";
    private static Color foregroundTextColor;
    private static Font foregroundTextFont;

    public Separator(){
	init();
    }

    private void init(){
	if( backgroundImageIcon == null ){
	    backgroundImageIcon = new ImageIcon( getClass().getResource("/com/metavize/gui/widgets/separator/PlainSeparator688x50.png") );
	    arrowImageIcon = new ImageIcon( getClass().getResource("/com/metavize/gui/widgets/separator/SeparatorArrow.png") );
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

    public void paint(Graphics g){
	super.paint(g);
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
