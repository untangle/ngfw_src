/*
 * MConfigurationDialog.java
 *
 * Created on March 19, 2004, 12:36 AM
 */

package com.metavize.gui.store;

import java.awt.*;

import com.metavize.gui.transform.*;
import com.metavize.gui.util.Util;

/**
 *
 * @author  Ian Nieves
 */
public class MStoreJDialog extends javax.swing.JDialog implements java.awt.event.WindowListener {
    
    private MTransformJButton mTransformJButton;
    private GridBagConstraints gridBagConstraints;
    private boolean purchasedTransform = false;
    
    public MStoreJDialog(java.awt.Frame parent, boolean modal, MTransformJButton mTransformJButton){
        super(parent, modal);
        gridBagConstraints = new GridBagConstraints(0, 0, 1, 1, 0d, 0d, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0);
        this.mTransformJButton = mTransformJButton;
        
        // INITIALIZE GUI
        initComponents();
        this.addWindowListener(this);
        storeJScrollPane.getVerticalScrollBar().setOpaque(false);

        if(mTransformJButton != null){
            mTransformJPanel.add(mTransformJButton, gridBagConstraints);
            descriptionJTextArea.setText(mTransformJButton.getFullDescription());
        }
    }
    

    private void initComponents() {//GEN-BEGIN:initComponents
        acceptJButton = new javax.swing.JButton();
        rejectJButton = new javax.swing.JButton();
        mTransformJPanel = new javax.swing.JPanel();
        storeJScrollPane = new javax.swing.JScrollPane();
        descriptionJTextArea = new javax.swing.JTextArea();
        noteJLabel = new javax.swing.JLabel();
        mTransformJPanel1 = new javax.swing.JPanel();
        accountJLabel1 = new javax.swing.JLabel();
        priceJLabel = new javax.swing.JLabel();
        backgroundJLabel = new javax.swing.JLabel();

        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Purchase a Software Appliance");
        setBackground(java.awt.Color.gray);
        setResizable(false);
        acceptJButton.setFont(new java.awt.Font("Arial", 0, 12));
        acceptJButton.setText("Purchase");
        acceptJButton.setDoubleBuffered(true);
        acceptJButton.setFocusPainted(false);
        acceptJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                acceptJButtonActionPerformed(evt);
            }
        });

        getContentPane().add(acceptJButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(474, 310, 96, 30));

        rejectJButton.setFont(new java.awt.Font("Arial", 0, 12));
        rejectJButton.setText("Cancel");
        rejectJButton.setDoubleBuffered(true);
        rejectJButton.setFocusPainted(false);
        rejectJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rejectJButtonActionPerformed(evt);
            }
        });

        getContentPane().add(rejectJButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(378, 310, 96, 30));

        mTransformJPanel.setLayout(new java.awt.GridBagLayout());

        mTransformJPanel.setBorder(new javax.swing.border.TitledBorder(null, "Software Appliance", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial", 0, 11)));
        mTransformJPanel.setOpaque(false);
        getContentPane().add(mTransformJPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 30, 210, 150));

        storeJScrollPane.setBorder(new javax.swing.border.TitledBorder(null, "Full Description", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial", 0, 11)));
        storeJScrollPane.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        storeJScrollPane.setDoubleBuffered(true);
        storeJScrollPane.setOpaque(false);
        storeJScrollPane.getViewport().setOpaque(false);
        descriptionJTextArea.setEditable(false);
        descriptionJTextArea.setFont(new java.awt.Font("Arial", 0, 12));
        descriptionJTextArea.setLineWrap(true);
        descriptionJTextArea.setWrapStyleWord(true);
        descriptionJTextArea.setDoubleBuffered(true);
        descriptionJTextArea.setMargin(new java.awt.Insets(5, 5, 5, 5));
        descriptionJTextArea.setOpaque(false);
        storeJScrollPane.setViewportView(descriptionJTextArea);

        getContentPane().add(storeJScrollPane, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 30, 300, 150));

        noteJLabel.setFont(new java.awt.Font("Arial", 0, 12));
        noteJLabel.setText("<html><B>Note:</B>  This Software Appliance will become a part of your <u>Toolbox</u> if you choose to purchase it.  Once in your <u>Toolbox</u>, you can then add instances of it to the <u>Rack</u>.</html>");
        noteJLabel.setDoubleBuffered(true);
        getContentPane().add(noteJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(273, 210, 300, -1));

        mTransformJPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        mTransformJPanel1.setBorder(new javax.swing.border.TitledBorder(null, "Purchase Information", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial", 0, 11)));
        mTransformJPanel1.setOpaque(false);
        accountJLabel1.setFont(new java.awt.Font("Arial", 0, 12));
        accountJLabel1.setText("Account #:");
        accountJLabel1.setDoubleBuffered(true);
        mTransformJPanel1.add(accountJLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 20, -1, -1));

        priceJLabel.setFont(new java.awt.Font("Arial", 0, 12));
        priceJLabel.setText("Price:");
        priceJLabel.setDoubleBuffered(true);
        mTransformJPanel1.add(priceJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 40, -1, -1));

        getContentPane().add(mTransformJPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 200, 210, 90));

        backgroundJLabel.setFont(new java.awt.Font("Arial", 0, 12));
        backgroundJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/images/LightGreyBackground1600x1200.png")));
        backgroundJLabel.setDoubleBuffered(true);
        getContentPane().add(backgroundJLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width-610)/2, (screenSize.height-400)/2, 610, 400);
    }//GEN-END:initComponents

    private void acceptJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_acceptJButtonActionPerformed
        this.purchasedTransform = true;
        
        if(Util.getIsDemo())
            this.purchasedTransform = false;
        
        this.windowClosing(null);
    }//GEN-LAST:event_acceptJButtonActionPerformed

    private void rejectJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rejectJButtonActionPerformed
        this.windowClosing(null);
    }//GEN-LAST:event_rejectJButtonActionPerformed
    
    public MTransformJButton getPurchasedMTransformJButton(){
        if(purchasedTransform)
            return mTransformJButton;
        else
            return null;
    }
    
    
    
    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
        mTransformJButton.setEnabled(true);
        this.setVisible(false);
    }    
    
    public void windowActivated(java.awt.event.WindowEvent windowEvent) {}    
    public void windowClosed(java.awt.event.WindowEvent windowEvent) {}    
    public void windowDeactivated(java.awt.event.WindowEvent windowEvent) {}
    public void windowDeiconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowIconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowOpened(java.awt.event.WindowEvent windowEvent) {}
    
            
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        new MStoreJDialog(new javax.swing.JFrame(), true, null).setVisible(true);
    }
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton acceptJButton;
    private javax.swing.JLabel accountJLabel1;
    private javax.swing.JLabel backgroundJLabel;
    private javax.swing.JTextArea descriptionJTextArea;
    private javax.swing.JPanel mTransformJPanel;
    private javax.swing.JPanel mTransformJPanel1;
    private javax.swing.JLabel noteJLabel;
    private javax.swing.JLabel priceJLabel;
    private javax.swing.JButton rejectJButton;
    private javax.swing.JScrollPane storeJScrollPane;
    // End of variables declaration//GEN-END:variables
    
}
