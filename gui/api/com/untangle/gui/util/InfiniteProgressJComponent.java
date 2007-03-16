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

package com.untangle.gui.util;

import java.awt.Insets;
import java.awt.Color;
import java.awt.Font;
import java.awt.Dimension;
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

import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

public class InfiniteProgressJComponent extends JComponent implements MouseListener, ActionListener {
    protected Area[]  ticker      = null;
    protected Thread  animation   = null;
    protected int     alphaLevel  = 0;
    protected int     rampDelay   = 0;
    protected float   shield      = 0.70f;
    protected String  text        = "";
    protected int     barsCount   = 12;
    protected int     gradLength  = 6;
    protected float   fps         = 4.0f;
    protected Timer   actionTimer = null;
    protected volatile boolean started = false;
    protected volatile boolean rampUp  = false;
    protected volatile boolean doInit  = false;
    protected volatile boolean paintContent = false;
    protected RenderingHints hints = null;
    private JProgressBar jProgressBar = null;
    private GridBagConstraints progressBarGridBagConstraints = null;

    private volatile long startTime, minRunTime;

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

    // TEXT ///////////////////
    public void setText(String text){
        this.text = text;
        repaint();
    }
    public void setTextLater(final String text){
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    setText(text);
	}});
    }
    public String getText(){
        return text;
    }
    /////////////////////////////

    // PROGRESS BAR //////////////
    public JProgressBar getProgressBar(){
	if(jProgressBar == null){
	    jProgressBar = new JProgressBar();
	    jProgressBar.setStringPainted(true);
	    jProgressBar.setForeground(new Color(68,91,255));
	    jProgressBar.setMaximumSize(new Dimension(16000,16));
	    jProgressBar.setMinimumSize(new Dimension(10,16));
	    jProgressBar.setPreferredSize(new Dimension(400,16));
	    add(jProgressBar, progressBarGridBagConstraints);	    
	}
	return jProgressBar;
    }
    public void setProgressBarVisible(boolean isVisible){
	getProgressBar().setVisible(isVisible);
	getProgressBar().setIndeterminate(false);
	getProgressBar().setValue(0);
	getProgressBar().setString("");	
    }
    /////////////////////////////

    // START ///////////////////
    public void start(String text){
	setText(text);
	start();
    }
    public void start(){
	if( actionTimer.isRunning() )
	    return;
	addMouseListener(this);
	paintContent = false;
	setVisible(true);
	rampUp = started = doInit = true;
	startTime = System.currentTimeMillis();
	actionTimer.start();
    }
    public void startLater(){ startLater(""); }
    public void startLater(final String text){
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    start(text);
	}});
    }
    ///////////////////////////

    // STOP //////////////////
    private long sleepTime;
    public void stop(){
	if( !actionTimer.isRunning() )
	    return;
	actionTimer.stop();
	removeMouseListener(this);
	setVisible(false);
	getTopLevelAncestor().repaint();
	for (int i = 0; i < ticker.length; i++)  // put back to corner for the next use
	    ticker[i].transform(AffineTransform.getTranslateInstance(-newCenterX, -newCenterY));	
    }
    public void stopLater(){ stopLater(-1); }
    public void stopLater(final long minRunTime){
	if( minRunTime > 0 ){
	    try{
		SwingUtilities.invokeAndWait(new Runnable(){ public void run(){
		    sleepTime = minRunTime - (System.currentTimeMillis()-startTime);
		}});
	    }
	    catch(Exception e){ Util.handleExceptionNoRestart("Error sleeping", e); }
	    if( sleepTime > 0 ){
		try{ Thread.currentThread().sleep(sleepTime); }
		catch(Exception e){ Util.handleExceptionNoRestart("Error sleeping", e); }
	    }
	}
	SwingUtilities.invokeLater(new Runnable(){ public void run(){
	    stop();
	}});
    }
    //////////////////////////


    public boolean isRunning(){
	return actionTimer.isRunning();
    }
    
    public void paintComponent(Graphics g){
        if (started){
            int width  = getWidth();
            int height = getHeight();

            double maxY = 0.0; 
	    
            Graphics2D g2 = (Graphics2D) g;
	    RenderingHints oldRenderingHints = g2.getRenderingHints();
            g2.setRenderingHints(hints);
            
            g2.setColor(new Color(255, 255, 255, (int) (alphaLevel * shield)));
            g2.fillRect(0, 0, width, height);

	    if( paintContent ){
		for (int i = 0; i < ticker.length; i++){
		    float scale;
		    if( i < gradLength )
			scale = (float)i / (float)(gradLength); 
		    else
			scale = (float)(gradLength-1) / (float)(gradLength); 
		    int channelR = 104 + (int)(151f * scale);
		    int channelG = 189 + (int)(66f  * scale);
		    int channelB =  73 + (int)(182f * scale);
		    g2.setColor(new Color(channelR, channelG, channelB, alphaLevel));
		    //int channel = 224 - 128 / (i + 1);
		    //g2.setColor(new Color(channel, channel, channel, alphaLevel));
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
	    g2.setRenderingHints(oldRenderingHints);
        }
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
		
	if(rampDelay>0){
	    if (alphaLevel < 255){
		alphaLevel = (int) (255 * (System.currentTimeMillis() - start) / rampDelay);
		if (alphaLevel >= 255){
		    alphaLevel = 255;
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
	AffineTransform rotateTransform = AffineTransform.getRotateInstance(Math.toRadians(15));
	tick.transform(rotateTransform);
        return tick;
    }


    public void mouseClicked(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
}
