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
import java.awt.image.BufferedImage;
import javax.swing.*;


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


        /*
        // CREATE MASK/GRADIENT
        BufferedImage gradient = new BufferedImage(getWidth(),getHeight(),BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = gradient.createGraphics();
        GradientPaint gradientPaint = new GradientPaint(0,0,startColor,getWidth()/2,0,endColor,true);
        g2d.setPaint(gradientPaint);
        g2d.fill(new Rectangle(getWidth(),getHeight()));
        g2d.dispose();
        gradient.flush();

        // CREATE BACKGROUND
        BufferedImage background = new BufferedImage(getWidth(),getHeight(),BufferedImage.TYPE_INT_ARGB);
        Graphics2D backG2D = background.createGraphics();
        for(int x=0; x <= getWidth(); x+=iconWidth){
        for(int y=0; y<= getHeight(); y+=iconHeight){
        backG2D.drawImage(icon.getImage(),x,y,this);
        }
        }
        backG2D.setComposite(AlphaComposite.SrcOver);
        backG2D.drawImage(gradient,0,0,this);
        backG2D.dispose();
        background.flush();

        // PAINT BACKGROUND
        g.drawImage(background,0,0,this);
        **/
    }
}
