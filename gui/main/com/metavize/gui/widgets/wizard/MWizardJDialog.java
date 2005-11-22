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

package com.metavize.gui.widgets.wizard;


import com.metavize.gui.transform.*;
import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.widgets.coloredTable.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.security.PasswordUtil;
import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;

import javax.swing.border.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.util.*;

public class MWizardJDialog extends javax.swing.JDialog implements java.awt.event.WindowListener {

    // BUTTON STRINGS ///////
    protected String STRING_NEXT_PAGE = "<html><b>Next</b> Page</html>";
    protected String STRING_PREVIOUS_PAGE = "<html><b>Previous</b> Page</html>";
    protected String STRING_FINAL_PAGE = "<html><b>Finish</b></html>";
    protected String STRING_CLOSE_WIZARD = "<html><b>Close</b> Wizard</html>";

    // LABEL COLORS /////
    protected Color COLOR_COMPLETED = Color.GRAY;
    protected Color COLOR_CURRENT = Color.BLUE;
    protected Color COLOR_UNCOMPLETED = Color.BLACK;

    // LABEL ICONS ////
    protected Icon ICON_COMPLETED = null;
    protected Icon ICON_CURRENT = null;
    protected Icon ICON_UNCOMPLETED = null;

    // LABEL SPECS ////
    protected String FONT_TYPE = "Default";
    protected int FONT_MODE = 0;
    protected int FONT_SIZE = 16;
    protected int FONT_SPACING = 10;
    
    // WIZARD STATE //////
    protected int currentPage = 0;
    protected Map<String, MWizardPageJPanel> wizardPageMap = new LinkedHashMap<String, MWizardPageJPanel>();
    protected Map<String, JLabel> labelMap = new LinkedHashMap<String, JLabel>();
    protected Vector<Boolean> checkpointVector = new Vector<Boolean>();
    protected Vector<Boolean> saveVector = new Vector<Boolean>();

    public MWizardJDialog(Dialog topLevelDialog, boolean isModal){
	super(topLevelDialog, isModal);
	init(topLevelDialog);
    }

    public MWizardJDialog(Frame topLevelFrame, boolean isModal){
	super(topLevelFrame, isModal);
	init(topLevelFrame);
    }

    public MWizardJDialog(){
	super();
	init(null);
    }

    protected void init(Window topLevelWindow){
        initComponents();
        setBounds( Util.generateCenteredBounds( topLevelWindow,
						getPreferredSize().width,
						getPreferredSize().height) );
        addWindowListener(this);

	ICON_COMPLETED = new ImageIcon( getClass().getResource("/com/metavize/gui/widgets/wizard/IconCompleteState14x14.png") );
	ICON_CURRENT = new ImageIcon( getClass().getResource("/com/metavize/gui/widgets/wizard/IconUncompleteState14x14.png") );
	ICON_UNCOMPLETED = new ImageIcon( getClass().getResource("/com/metavize/gui/widgets/wizard/IconUncompleteState14x14.png") );
    }

    public void setVisible(boolean isVisible){
	if( isVisible ){
	    updateButtonState(false);
	    updateLabelState();
	    updatePageState();	    
	}
	super.setVisible(isVisible);
    }
    
    protected void updateButtonState(boolean disableAll){
	if( disableAll ){
	    nextJButton.setEnabled(false);
	    previousJButton.setEnabled(false);
	}
	else{
	    if( currentPage == 0 ){
		nextJButton.setEnabled(true);
		previousJButton.setEnabled(false);
		nextJButton.setText(STRING_NEXT_PAGE);
	    }
	    else if( currentPage < (wizardPageMap.size()-1) ){
		nextJButton.setEnabled(true);
		previousJButton.setEnabled(true);
		nextJButton.setText(STRING_NEXT_PAGE);
	    }
	    else{
		nextJButton.setEnabled(true);
		previousJButton.setEnabled(true);
		nextJButton.setText(STRING_FINAL_PAGE);
	    }
	    if( checkpointVector.elementAt(currentPage) )
		previousJButton.setEnabled(false);
	}
    }

    protected void updateLabelState(){
	int index = 0;
	for( JLabel label : labelMap.values() ){
	    if( index < currentPage ){
		label.setForeground(COLOR_COMPLETED);
		label.setIcon(ICON_COMPLETED);
	    }
	    else if( index == currentPage ){
		label.setForeground(COLOR_CURRENT);
		label.setIcon(ICON_CURRENT);
	    }
	    else{ //( index > currentPage )
		label.setForeground(COLOR_UNCOMPLETED);
		label.setIcon(ICON_UNCOMPLETED);
	    }
	    index++;
	}	
    }

    protected void updatePageState(){
	if( currentPage <= (wizardPageMap.size()-1) ){
	    contentJPanel.removeAll();
	    MWizardPageJPanel wizardPageJPanel = (MWizardPageJPanel) wizardPageMap.values().toArray()[currentPage];
	    contentJPanel.add( wizardPageJPanel );
	    contentJPanel.revalidate();
	    contentJPanel.repaint();
	}
    }

    public void addWizardPageJPanel(MWizardPageJPanel wizardPageJPanel, String title, boolean isCheckpoint, boolean doSave){
        wizardPageMap.put(title, wizardPageJPanel);
	
        JLabel newJLabel = new JLabel(title);
        newJLabel.setBorder(new EmptyBorder(0,0,FONT_SPACING,0));
        newJLabel.setFont(new Font(FONT_TYPE,FONT_MODE,FONT_SIZE));
        titleJPanel.add(newJLabel);
	labelMap.put(title, newJLabel);

	checkpointVector.add(isCheckpoint);
	saveVector.add(doSave);
    }

    private boolean finishedNormal;
    protected void wizardFinishedNormal(){
	finishedNormal = true;
	setVisible(false);
	dispose();
    }
    protected void wizardFinishedAbnormal(int currentPage){
	finishedNormal = false;
	setVisible(false);
	dispose();
    }
    public boolean getFinishedNormal(){ return finishedNormal; }

    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        contentJPanel = new javax.swing.JPanel();
        titleJPanel = new javax.swing.JPanel();
        closeJButton = new javax.swing.JButton();
        previousJButton = new javax.swing.JButton();
        nextJButton = new javax.swing.JButton();
        backgroundJLabel = new com.metavize.gui.widgets.MTiledIconLabel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        contentJPanel.setLayout(new java.awt.BorderLayout());

        contentJPanel.setBorder(new javax.swing.border.CompoundBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(4, 4, 4, 4)), new javax.swing.border.EtchedBorder()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 250, 15, 15);
        getContentPane().add(contentJPanel, gridBagConstraints);

        titleJPanel.setLayout(new javax.swing.BoxLayout(titleJPanel, javax.swing.BoxLayout.Y_AXIS));

        titleJPanel.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 15, 520);
        getContentPane().add(titleJPanel, gridBagConstraints);

        closeJButton.setFont(new java.awt.Font("Default", 0, 12));
        closeJButton.setText("<html><b>Close</b> Window</html>");
        closeJButton.setDoubleBuffered(true);
        closeJButton.setFocusPainted(false);
        closeJButton.setFocusable(false);
        closeJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        closeJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        closeJButton.setMaximumSize(new java.awt.Dimension(140, 25));
        closeJButton.setMinimumSize(new java.awt.Dimension(140, 25));
        closeJButton.setPreferredSize(new java.awt.Dimension(140, 25));
        closeJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeJButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 15, 0);
        getContentPane().add(closeJButton, gridBagConstraints);

        previousJButton.setFont(new java.awt.Font("Arial", 0, 12));
        previousJButton.setText("<html><b>Previous</b> page</html>");
        previousJButton.setDoubleBuffered(true);
        previousJButton.setFocusPainted(false);
        previousJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        previousJButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        previousJButton.setMaximumSize(new java.awt.Dimension(120, 25));
        previousJButton.setMinimumSize(new java.awt.Dimension(120, 25));
        previousJButton.setPreferredSize(new java.awt.Dimension(120, 25));
        previousJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                previousJButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 15);
        getContentPane().add(previousJButton, gridBagConstraints);

        nextJButton.setFont(new java.awt.Font("Arial", 0, 12));
        nextJButton.setText("<html><b>Next</b> page</html>");
        nextJButton.setDoubleBuffered(true);
        nextJButton.setFocusPainted(false);
        nextJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        nextJButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        nextJButton.setMaximumSize(new java.awt.Dimension(78, 25));
        nextJButton.setMinimumSize(new java.awt.Dimension(78, 25));
        nextJButton.setPreferredSize(new java.awt.Dimension(78, 25));
        nextJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextJButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipadx = 40;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 15);
        getContentPane().add(nextJButton, gridBagConstraints);

        backgroundJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/images/DarkGreyBackground1600x100.png")));
        backgroundJLabel.setDoubleBuffered(true);
        backgroundJLabel.setMaximumSize(new java.awt.Dimension(750, 450));
        backgroundJLabel.setMinimumSize(new java.awt.Dimension(750, 450));
        backgroundJLabel.setPreferredSize(new java.awt.Dimension(750, 450));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(backgroundJLabel, gridBagConstraints);

        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width-750)/2, (screenSize.height-450)/2, 750, 450);
    }//GEN-END:initComponents

    private void previousJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_previousJButtonActionPerformed
	new PreviousPageThread();
    }//GEN-LAST:event_previousJButtonActionPerformed

    private class PreviousPageThread extends Thread{
	public PreviousPageThread(){
	    setDaemon(true);
	    updateButtonState(true);
	    start();
	}
	public void run(){
	    boolean changePage = true;
	    // SEND LEAVING EVENT TO CURRENT PAGE
	    MWizardPageJPanel currentWizardPageJPanel = (MWizardPageJPanel) wizardPageMap.values().toArray()[currentPage];
	    changePage &= currentWizardPageJPanel.leavingBackwards();	      
	    // SEND ENTERING EVENT TO PREVIOUS PAGE
	    if( changePage ){
		MWizardPageJPanel previousWizardPageJPanel = (MWizardPageJPanel) wizardPageMap.values().toArray()[currentPage-1];
		changePage &= previousWizardPageJPanel.enteringBackwards();
	    }
	    // UPDATE CURRENT PAGE
	    if( changePage ){
		currentPage--;
	    }
	    // UPDATE VIEW OF CURRENT PAGE
	    try{
		SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
		    updateButtonState(false);
		    updateLabelState();
		    updatePageState();
		}});
	    }catch(Exception e){ Util.handleExceptionNoRestart("Error updating wizard on move previous", e); }
	}
    }

    private void nextJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextJButtonActionPerformed
        new NextPageThread();
    }//GEN-LAST:event_nextJButtonActionPerformed

    private class NextPageThread extends Thread{
        public NextPageThread(){
            setDaemon(true);
	    updateButtonState(true);
            start();
        }
        public void run(){
	    boolean changePage = true;
            // VALIDATE AND SAVE CURRENT PAGE
	    MWizardPageJPanel currentWizardPageJPanel = (MWizardPageJPanel) wizardPageMap.values().toArray()[currentPage];
            try{
		currentWizardPageJPanel.doSave(null, true);  // validate
		if( saveVector.elementAt(currentPage) ){
		    currentWizardPageJPanel.doSave(null, false);  // save
		    int i=1;
		    MWizardPageJPanel previousWizardPageJPanel;
		    while( (currentPage-i >= 0) && !saveVector.elementAt(currentPage-i) ){ // save all previous
			previousWizardPageJPanel = (MWizardPageJPanel) wizardPageMap.values().toArray()[currentPage-i];
			previousWizardPageJPanel.doSave(null, false);
			i++;
		    }
		}
            }
            catch(Exception e){		
                Util.handleExceptionNoRestart("Error validating: ", e);
                new MOneButtonJDialog(MWizardJDialog.this, "Wizard", e.getMessage());
		changePage &= false;
            }
	    // SEND LEAVING EVENT TO CURRENT PAGE
	    if( changePage ){
		changePage &= currentWizardPageJPanel.leavingForwards();
	    }
	    // SEND ENTERING EVENT TO NEXT PAGE
	    if( changePage && (currentPage < wizardPageMap.size()-1) ){
		MWizardPageJPanel previousWizardPageJPanel = (MWizardPageJPanel) wizardPageMap.values().toArray()[currentPage+1];
		changePage &= previousWizardPageJPanel.enteringForwards();
	    }
	    // UPDATE CURRENT PAGE
	    if( currentPage < wizardPageMap.size()-1 ){
		if( changePage  ){
		    currentPage++;
		}
		// UPDATE VIEW OF CURRENT PAGE
		try{
		    SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
			updateButtonState(false);
			updateLabelState();
			updatePageState();
		    }});
		}catch(Exception e){ Util.handleExceptionNoRestart("Error updating wizard on move next", e); }
		// FINISH WIZARD
	    }
	    else{
		wizardFinishedNormal();
	    }

	}
    }
        
    protected void closeJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeJButtonActionPerformed
	wizardFinishedAbnormal(currentPage);
    }//GEN-LAST:event_closeJButtonActionPerformed
    
    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
	wizardFinishedAbnormal(currentPage);
    }

    public void windowClosed(java.awt.event.WindowEvent windowEvent) {}
    public void windowActivated(java.awt.event.WindowEvent windowEvent) {}
    public void windowDeactivated(java.awt.event.WindowEvent windowEvent) {}
    public void windowDeiconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowIconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowOpened(java.awt.event.WindowEvent windowEvent) {}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel backgroundJLabel;
    private javax.swing.JButton closeJButton;
    private javax.swing.JPanel contentJPanel;
    protected javax.swing.JButton nextJButton;
    protected javax.swing.JButton previousJButton;
    private javax.swing.JPanel titleJPanel;
    // End of variables declaration//GEN-END:variables
    
}
