/*
 * ProceedJDialog.java
 *
 * Created on July 28, 2004, 7:48 PM
 */

package com.metavize.gui.store;

import com.metavize.gui.util.Util;
import com.metavize.mvvm.*;

import javax.swing.*;

/**
 *
 * @author  inieves
 */
public class StoreCheckJDialog extends javax.swing.JDialog implements java.awt.event.WindowListener {
    
    private boolean upgradesAvailable = true;
    
    /** Creates new form ProceedJDialog */
    public StoreCheckJDialog() {
        super(Util.getMMainJFrame(), true);
        initComponents();
        this.addWindowListener(this);

	this.setBounds( Util.generateCenteredBounds(Util.getMMainJFrame().getBounds(), this.getWidth(), this.getHeight()) );
    }
    
    public boolean upgradesAvailable(){
        return upgradesAvailable;
    }
    
    public void setVisible(boolean isVisible){
        if(isVisible){
            new UpgradeCheckThread();
            super.setVisible(true);
        }
        else{
            super.setVisible(false);
	    dispose();
	}
    }
    
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        labelJLabel = new javax.swing.JLabel();
        messageJLabel = new javax.swing.JLabel();
        jProgressBar = new javax.swing.JProgressBar();
        proceedJButton = new javax.swing.JButton();
        backgroundJLabel = new com.metavize.gui.widgets.MTiledIconLabel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Upgrade Availability Check");
        setModal(true);
        setResizable(false);
        labelJLabel.setFont(new java.awt.Font("Dialog", 1, 24));
        labelJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelJLabel.setText("Checking Upgrades:");
        labelJLabel.setDoubleBuffered(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        getContentPane().add(labelJLabel, gridBagConstraints);

        messageJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        messageJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        messageJLabel.setText("<html><center>\nYou must perform all possible upgrades<br>\nbefore procuring a new Software Appliance.<br>\n<br>\nNow checking for available upgrades.<br>\n</center></html>");
        messageJLabel.setDoubleBuffered(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(60, 30, 0, 30);
        getContentPane().add(messageJLabel, gridBagConstraints);

        jProgressBar.setFont(new java.awt.Font("Dialog", 0, 12));
        jProgressBar.setForeground(new java.awt.Color(68, 91, 255));
        jProgressBar.setMaximumSize(new java.awt.Dimension(32767, 16));
        jProgressBar.setMinimumSize(new java.awt.Dimension(10, 16));
        jProgressBar.setPreferredSize(new java.awt.Dimension(148, 16));
        jProgressBar.setString("");
        jProgressBar.setStringPainted(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(130, 30, 0, 30);
        getContentPane().add(jProgressBar, gridBagConstraints);

        proceedJButton.setFont(new java.awt.Font("Default", 1, 12));
        proceedJButton.setText("Cancel");
        proceedJButton.setDoubleBuffered(true);
        proceedJButton.setFocusPainted(false);
        proceedJButton.setFocusable(false);
        proceedJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proceedJButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(160, 0, 13, 0);
        getContentPane().add(proceedJButton, gridBagConstraints);

        backgroundJLabel.setFont(new java.awt.Font("Default", 0, 12));
        backgroundJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        backgroundJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/images/LightGreyBackground1600x100.png")));
        backgroundJLabel.setDoubleBuffered(true);
        backgroundJLabel.setFocusable(false);
        backgroundJLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        backgroundJLabel.setOpaque(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(backgroundJLabel, gridBagConstraints);

        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width-456)/2, (screenSize.height-222)/2, 456, 222);
    }//GEN-END:initComponents

    private void proceedJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proceedJButtonActionPerformed
        setVisible(false);
    }//GEN-LAST:event_proceedJButtonActionPerformed
    
    
    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
        this.setVisible(false);
    }    
    
    
    public void windowActivated(java.awt.event.WindowEvent windowEvent) {}    
    public void windowClosed(java.awt.event.WindowEvent windowEvent) {}    
    public void windowDeactivated(java.awt.event.WindowEvent windowEvent) {}
    public void windowDeiconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowIconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowOpened(java.awt.event.WindowEvent windowEvent) {}
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel backgroundJLabel;
    private javax.swing.JProgressBar jProgressBar;
    private javax.swing.JLabel labelJLabel;
    private javax.swing.JLabel messageJLabel;
    private javax.swing.JButton proceedJButton;
    // End of variables declaration//GEN-END:variables
    
    private class UpgradeCheckThread extends Thread {
        public UpgradeCheckThread(){
            this.setDaemon(true);
	    this.setContextClassLoader(Util.getClassLoader());
            start();
        }
        public void run() {
            try{
                SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
                    StoreCheckJDialog.this.jProgressBar.setIndeterminate(true);
                    StoreCheckJDialog.this.jProgressBar.setString("Checking for upgrades...");
                }});
            
                Thread.sleep(1500l);
                
                Util.getToolboxManager().update();
                MackageDesc[] mackageDescs = Util.getToolboxManager().upgradable();
                if( Util.isArrayEmpty(mackageDescs) ){
                    upgradesAvailable = false;
                    Util.getMMainJFrame().updateJButton(0);
                    SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
                        StoreCheckJDialog.this.jProgressBar.setIndeterminate(false);
                        StoreCheckJDialog.this.jProgressBar.setString("No upgrades found.  Proceeding.");
                    }});
                    Thread.sleep(2000l);
                    StoreCheckJDialog.this.setVisible(false);
                }
                else{
                    upgradesAvailable = true;
                    Util.getMMainJFrame().updateJButton(mackageDescs.length);
                    SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
                        StoreCheckJDialog.this.jProgressBar.setIndeterminate(false);
                        StoreCheckJDialog.this.jProgressBar.setString("Upgrades found.  Please perform upgrades.");
                    }});
                }
                Util.checkedUpgrades();
            }
            catch(Exception e){
                Util.handleExceptionNoRestart("Error auto checking for upgrades on server", e);
                SwingUtilities.invokeLater( new Runnable(){ public void run(){
                    StoreCheckJDialog.this.jProgressBar.setIndeterminate(false);
                    StoreCheckJDialog.this.jProgressBar.setString("Upgrades check problem.  Please try again later.");
                }});
            }
	    
        }
    }
    
}

