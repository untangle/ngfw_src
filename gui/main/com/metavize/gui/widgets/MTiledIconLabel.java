/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

/*
 * MPasswordField.java
 *
 * Created on December 14, 2004, 7:00 PM
 */

package com.metavize.gui.widgets;

import java.awt.*;
import javax.swing.*;


public class MTiledIconLabel extends JLabel {

    public MTiledIconLabel()
    {
        super();
    }
    
    public MTiledIconLabel(String a, ImageIcon b, int c)
    {
        super(a, b, c);
    }
    
    public void paintComponent(Graphics g)
    {
        ImageIcon icon = (ImageIcon) getIcon();
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
