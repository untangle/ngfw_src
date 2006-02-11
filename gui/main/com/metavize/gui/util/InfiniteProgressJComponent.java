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

package com.metavize.gui.util;

import java.awt.Insets;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.JProgressBar;

public class InfiniteProgressJComponent extends JComponent implements MouseListener, ActionListener {
    protected Area[]  ticker      = null;
    protected Thread  animation   = null;
    protected int     alphaLevel  = 0;
    protected int     rampDelay   = 0;
    protected float   shield      = 0.70f;
    protected String  text        = "";
    protected int     barsCount   = 12;
    protected float   fps         = 4.0f;
    protected Timer   actionTimer = null;
    protected volatile boolean started = false;
    protected volatile boolean rampUp  = false;
    protected volatile boolean doInit  = false;
    protected volatile boolean paintContent = false;
    protected RenderingHints hints = null;
    private JProgressBar jProgressBar = null;
    private GridBagConstraints progressBarGridBagConstraints = null;

    public InfiniteProgressJComponent(){
        this("");
    }

    public InfiniteProgressJComponent(String text){
        this(text, 12);
    }

    public InfiniteProgressJComponent(String text, int barsCount){
        this(text, barsCount, 0.70f);
    }

    public InfiniteProgressJComponent(String text, int barsCount, float shield){
        this(text, barsCount, shield, 4.0f);
    }

    public InfiniteProgressJComponent(String text, int barsCount, float shield, float fps){
        this(text, barsCount, shield, fps, 0);
    }

    public InfiniteProgressJComponent(String text, int barsCount, float shield, float fps, int rampDelay){
        this.text      = text;
        this.rampDelay = rampDelay >= 0 ? rampDelay : 0;
        this.shield    = shield >= 0.0f ? shield : 0.0f;
        this.fps       = fps > 0.0f ? fps : 4.0f;
        this.barsCount = barsCount > 0 ? barsCount : 12;
	
        this.hints = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        this.hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        this.hints.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

	this.setFont(new Font("Arial", Font.BOLD, 16));
	this.setLayout(new GridBagLayout());

	this.progressBarGridBagConstraints = new GridBagConstraints(0,0,1,1,1d,1d,GridBagConstraints.SOUTH,GridBagConstraints.HORIZONTAL, new Insets(0,50,50,50),0,0);

	int mspf = (int) ((1f/fps)*1000f);
	this.actionTimer = new Timer(mspf, this);
	this.actionTimer.setInitialDelay(0);
	ticker = buildTicker();

    }

    public void setText(String text){
        repaint();
        this.text = text;
    }

    public String getText(){
        return text;
    }

    public JProgressBar getProgressBar(){
	if(jProgressBar == null){
	    jProgressBar = new JProgressBar();
	    jProgressBar.setForeground(new Color(68,91,255));
	    add(jProgressBar, progressBarGridBagConstraints);	    
	}
	return jProgressBar;
    }

    public void setProgressBarVisible(boolean isVisible){
	getProgressBar().setVisible(isVisible);
    }

    public void start(){
	if( !actionTimer.isRunning() ){
	    addMouseListener(this);
	    paintContent = false;
	    setVisible(true);
	    rampUp = started = doInit = true;
	    actionTimer.start();
	}
    }

    public void stop(){
	if( actionTimer.isRunning() ){
	    rampUp = false;
	}
    }

    public boolean isRunning(){
	return actionTimer.isRunning();
    }
    
    public void interrupt(){
	if( actionTimer.isRunning() ){
	    actionTimer.stop();
	}
	started = doInit = false;
	removeMouseListener(this);
	setVisible(false);
	repaint();
	ticker = buildTicker();
    }

    public void paintComponent(Graphics g){
        if (started){
            int width  = getWidth();
            int height = getHeight();

            double maxY = 0.0; 
	    
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHints(hints);
            
            g2.setColor(new Color(255, 255, 255, (int) (alphaLevel * shield)));
            g2.fillRect(0, 0, width, height);

	    if( paintContent ){
		for (int i = 0; i < ticker.length; i++){
		    int channel = 224 - 128 / (i + 1);
		    g2.setColor(new Color(channel, channel, channel, alphaLevel));
		    g2.fill(ticker[i]);
		    
		    Rectangle2D bounds = ticker[i].getBounds2D();
		    if (bounds.getMaxY() > maxY)
			maxY = bounds.getMaxY();
		}
		
		if (text != null && text.length() > 0){
		    FontRenderContext context = g2.getFontRenderContext();
		    TextLayout layout = new TextLayout(text, getFont(), context);
		    Rectangle2D bounds = layout.getBounds();
		    g2.setColor(getForeground());
		    layout.draw(g2, (float) (width - bounds.getWidth()) / 2,
				(float) (maxY + layout.getLeading() + 2 * layout.getAscent()));
		}
	    }
        }
    }
    
    private Area[] buildTicker(){
        Area[] ticker = new Area[barsCount];
        double fixedAngle = 2.0 * Math.PI / ((double) barsCount);
	
        for (double i = 0.0; i < (double) barsCount; i++){
            Area primitive = buildPrimitive();

            AffineTransform fullTransform = AffineTransform.getRotateInstance(-i * fixedAngle);
	    fullTransform.concatenate( AffineTransform.getTranslateInstance(45.0, -6.0) );
            primitive.transform(fullTransform);
            
            ticker[(int) i] = primitive;
        }

        return ticker;
    }

    private Area buildPrimitive(){
        Rectangle2D.Double body = new Rectangle2D.Double(6, 0, 30, 12);
        Ellipse2D.Double   head = new Ellipse2D.Double(0, 0, 12, 12);
        Ellipse2D.Double   tail = new Ellipse2D.Double(30, 0, 12, 12);

        Area tick = new Area(body);
        tick.add(new Area(head));
        tick.add(new Area(tail));

        return tick;
    }

    private long start;
    private double fixedIncrement;
    private AffineTransform rotateTransform;
    private AffineTransform translateTransform;
    private AffineTransform fullTransform;
    private double lastCenterX, lastCenterY;
    private double newCenterX, newCenterY;
    private boolean doUpdateTransform;
    public void actionPerformed(ActionEvent evt){
	newCenterX = getWidth() / 2;
	newCenterY = getHeight() / 2;
	if( doInit ){
	    lastCenterX = lastCenterY = 0d;
            fixedIncrement = 2.0 * Math.PI / ((double) barsCount);
            translateTransform = AffineTransform.getTranslateInstance(newCenterX, newCenterY);
	    rotateTransform = AffineTransform.getRotateInstance(fixedIncrement, newCenterX, newCenterY);
	    fullTransform = rotateTransform;
	    fullTransform.concatenate(translateTransform);
	    start = System.currentTimeMillis();
	    doUpdateTransform = true;
	    doInit = false;
	}
	else if( (lastCenterX!=newCenterX) || (lastCenterY!=newCenterY) ) {
	    translateTransform = AffineTransform.getTranslateInstance(newCenterX-lastCenterX, newCenterY-lastCenterY);
	    rotateTransform = AffineTransform.getRotateInstance(fixedIncrement, newCenterX, newCenterY);
	    fullTransform = rotateTransform;
	    fullTransform.concatenate(translateTransform);
	    doUpdateTransform = true;
	}
	else if(doUpdateTransform){
            translateTransform = AffineTransform.getTranslateInstance(0d, 0d);
	    rotateTransform = AffineTransform.getRotateInstance(fixedIncrement, newCenterX, newCenterY);
	    fullTransform = rotateTransform;
	    fullTransform.concatenate(translateTransform);
	    doUpdateTransform = false;
	}
	lastCenterX = newCenterX;
	lastCenterY = newCenterY;
 

	if (rampDelay == 0)
	    alphaLevel = rampUp ? 255 : 0;
	
	for (int i = 0; i < ticker.length; i++)
	    ticker[i].transform(fullTransform);	
	paintContent = true;
	repaint();
	
	
	if(rampUp){
	    if(rampDelay>0){
		if (alphaLevel < 255){
		    alphaLevel = (int) (255 * (System.currentTimeMillis() - start) / rampDelay);
		    if (alphaLevel >= 255){
			alphaLevel = 255;
		    }
		}
	    }
	}
	else{ // !rampUp
	    if(rampDelay>0){
		if (alphaLevel >= 0) {
		    alphaLevel = (int) (255 - (255 * (System.currentTimeMillis() - start) / rampDelay));
		}
	    }
	    if (alphaLevel <= 0){
		alphaLevel = 0;
		started = false;
		setVisible(false);
		repaint();		    
		removeMouseListener(InfiniteProgressJComponent.this);
		actionTimer.stop();
		for (int i = 0; i < ticker.length; i++)
		    ticker[i].transform(AffineTransform.getTranslateInstance(-newCenterX, -newCenterY));	
		return;
	    }
	}
    }



    public void mouseClicked(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
}
