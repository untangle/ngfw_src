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
import javax.swing.*;


public class MTiledIconLabel extends JLabel {

	ImageIcon imageIcon;
		
    public MTiledIconLabel()
    {
        super();
    }
    
    public MTiledIconLabel(String a, ImageIcon b, int c)
    {
        super(a, b, c);
    }
	
	// this is useful because it doesn't mess with the labels preferred size...
	// so you can get a tiled background without influencing layouts....
	public MTiledIconLabel(ImageIcon b){
		this.imageIcon = b;
	}
    
    public void paintComponent(Graphics g)
    {
        ImageIcon icon = (ImageIcon) getIcon();
		if(imageIcon != null)
				icon = imageIcon;
        if (icon == null || icon.getImage() == null) {
            // Don't have an icon (yet), just draw the background.
            super.paintComponent(g);
            return;
        }

        Rectangle clipRect = g.getClipBounds();
        int clipRight = clipRect.x + clipRect.width;
        int clipBottom = clipRect.y + clipRect.height;

        int iconWidth = icon.getIconWidth();
        int iconHeight = icon.getIconHeight();
        if (iconWidth == 0 || iconHeight == 0) {
            System.err.println("Ack -- icon width or height is zero");
            return;
        }
        int startx = (clipRect.x / iconWidth) * iconWidth;
        int starty = (clipRect.y / iconHeight) * iconHeight;
        for (int y = starty; y < clipBottom; y = y + iconHeight) {
            for (int x = startx; x < clipRight; x = x + iconWidth) {
                // g.drawImage(icon, x, y, this);
                g.drawImage(icon.getImage(), x, y, this);
            }
        }
    }
}
