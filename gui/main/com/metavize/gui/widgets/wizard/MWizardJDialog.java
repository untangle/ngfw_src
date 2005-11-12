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

public abstract class MWizardJDialog extends javax.swing.JDialog implements java.awt.event.WindowListener {

    private static final String NEXT_PAGE = "<html><b>Next</b> page</html>";
    private static final String FINAL_PAGE = "<html><b>Finish</b></html>";
    
    private int currentPage = 0;
    protected Map<String, Savable> savableMap = new LinkedHashMap<String, Savable>();

    public MWizardJDialog(Dialog topLevelDialog, boolean isModal){
	super(topLevelDialog, isModal);
	init(topLevelDialog);
    }
    
    public MWizardJDialog(Frame topLevelFrame, boolean isModal){
	super(topLevelFrame, isModal);
	init(topLevelFrame);
    }

    private void init(Window window){
        this.initComponents();
        this.setBounds( Util.generateCenteredBounds( window,
						     getPreferredSize().width,
						     getPreferredSize().height) );
        this.addWindowListener(this);

        // SETUP BUTTONS
        previousJButton.setEnabled(false);

    }

    public void addSavableJPanel(JPanel jPanel, String title){
        if( !(jPanel instanceof Savable) )
            return;
        savableMap.put(title, (Savable)jPanel);
        JLabel newJLabel = new JLabel(title);
        newJLabel.setBorder(new EmptyBorder(0,0,10,0));
        newJLabel.setFont(new Font("Default", 0, 18));
        titleJPanel.add(newJLabel);
        
        if( savableMap.size() == 1 ){  // the first panel
            contentJPanel.add(jPanel);
            newJLabel.setForeground(Color.BLUE);
        }
    }


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

        contentJPanel.setBorder(new javax.swing.border.EtchedBorder());
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
        // REMOVE CURRENT PAGE, PUT IN PREVIOUS ONE
        ((JLabel)titleJPanel.getComponent(currentPage)).setForeground(Color.BLACK);
        currentPage--;
        ((JLabel)titleJPanel.getComponent(currentPage)).setForeground(Color.BLUE);
        contentJPanel.removeAll();
        String key = (String) savableMap.keySet().toArray()[currentPage];
        Savable savable = savableMap.get(key);
        contentJPanel.add( (JPanel) savable);       
        contentJPanel.revalidate();
        contentJPanel.repaint();
        
        // UPDATE BUTTONS
        if( currentPage == 0 ){
            previousJButton.setEnabled(false);             
        }
        nextJButton.setText(NEXT_PAGE);
    }//GEN-LAST:event_previousJButtonActionPerformed

    protected void wizardFinished(){}

    private void nextJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextJButtonActionPerformed
        // VALIDATE CURRENT PAGE
        String keyValidate = (String) savableMap.keySet().toArray()[currentPage];
        Savable savableValidate = savableMap.get(keyValidate);
        try{
            savableValidate.doSave(null, true);  // verify only, dont save
        }
        catch(Exception e){
	    Util.handleExceptionNoRestart("Error validating: ", e);
            new MOneButtonJDialog(this, "Wizard", e.getMessage());
            return;
        }
        
        // TAKE ACTION BASED ON CURRENT PAGE
        if( currentPage == (savableMap.size()-1) ){ // last page
            // save/send everything
            try{
                for( Savable savable : savableMap.values() )
                    savable.doSave(null, false);
            }
            catch(Exception e){
		Util.handleExceptionNoRestart("Error validating: ", e);
                new MOneButtonJDialog(this, "Wizard", e.getMessage());
                return;
            }
            // close dialog
            windowClosing(null);
	    wizardFinished();
        }
        else{ // not last page
            ((JLabel)titleJPanel.getComponent(currentPage)).setForeground(Color.BLACK);
            currentPage++;
            ((JLabel)titleJPanel.getComponent(currentPage)).setForeground(Color.BLUE);
            contentJPanel.removeAll();
            String key = (String) savableMap.keySet().toArray()[currentPage];
            Savable savable = savableMap.get(key);
            contentJPanel.add( (JPanel) savable);
            contentJPanel.revalidate();
            contentJPanel.repaint();
        }

        // UPDATE BUTTONS
        if( currentPage == (savableMap.size()-1) ){
            nextJButton.setText(FINAL_PAGE);              
        }
        else{
            nextJButton.setText(NEXT_PAGE);
        }
        previousJButton.setEnabled(true);
    }//GEN-LAST:event_nextJButtonActionPerformed

    protected void closeJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeJButtonActionPerformed
         windowClosing(null);
    }//GEN-LAST:event_closeJButtonActionPerformed
    
    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
        this.setVisible(false);
        this.dispose();
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
