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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

public class MTiledIconLabel extends JLabel {

    private ImageIcon imageIcon;
    private static final Color startColor = new Color(255,0,0,0);
    private static final Color endColor = new Color(255,0,0,255);

    public MTiledIconLabel()
    {
        super();
        setPreferredSize(new Dimension(10,10));
    }

    public MTiledIconLabel(String a, ImageIcon b, int c)
    {
        super(a, b, c);
        setPreferredSize(new Dimension(10,10));
    }

    // this is useful because it doesn't mess with the labels preferred size...
    // so you can get a tiled background without influencing layouts....
    public MTiledIconLabel(ImageIcon b){
        this.imageIcon = b;
        setPreferredSize(new Dimension(10,10));
    }

    /*
      public Dimension getPreferredSize(){
      return new Dimension(10,10);
      }
    */
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
