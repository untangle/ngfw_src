/*
 * $HeadURL:$
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

package com.untangle.gui.widgets.wizard;


import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.*;

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.coloredTable.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.*;
import com.untangle.uvm.node.*;

public class MWizardJDialog extends javax.swing.JDialog implements java.awt.event.WindowListener {

    // BUTTON STRINGS ///////
    protected String STRING_NEXT_PAGE = "Next";
    protected String STRING_PREVIOUS_PAGE = "Previous";
    protected String STRING_FINAL_PAGE = "Finish";
    protected String STRING_CLOSE_WIZARD = "Close";

    // LABEL COLORS /////
    protected Color COLOR_COMPLETED = Color.GRAY;
    protected Color COLOR_CURRENT = Color.BLUE;
    protected Color COLOR_UNCOMPLETED = Color.BLACK;

    // LABEL ICONS ////
    protected Icon ICON_COMPLETED = null;
    protected Icon ICON_CURRENT = null;
    protected Icon ICON_UNCOMPLETED = null;

    // LABEL SPECS ////
    protected Font NORMAL_FONT = new Font("Default", 0, 16);
    protected Font BOLD_FONT = new Font("Default", 1, 16);
    protected int  FONT_SPACING = 10;

    // WIZARD STATE //////
    protected int currentPage = 0;
    protected Map<String, MWizardPageJPanel> wizardPageMap = new LinkedHashMap<String, MWizardPageJPanel>();
    protected Map<String, JLabel> labelMap = new LinkedHashMap<String, JLabel>();
    protected Vector<Boolean> checkpointVector = new Vector<Boolean>();
    protected Vector<Boolean> saveVector = new Vector<Boolean>();
    protected boolean isShiftDown;

    // INFINITE PROGRESS INDICATOR //
    private static InfiniteProgressJComponent infiniteProgressJComponent = new InfiniteProgressJComponent();
    public static InfiniteProgressJComponent getInfiniteProgressJComponent(){ return infiniteProgressJComponent; }

    // RENDERING HINTS //
    private RenderingHints hints;

    public MWizardJDialog(Dialog parentDialog, boolean isModal){
        super(parentDialog, isModal);
        init(parentDialog);
    }

    public MWizardJDialog(Frame parentFrame, boolean isModal){
        super(parentFrame, isModal);
        init(parentFrame);
    }

    public MWizardJDialog(){
        super();
        init(null);
    }

    protected void init(Window parentWindow){
        setGlassPane(infiniteProgressJComponent);
        initComponents();
        titleJPanel.setPreferredSize( getTitleJPanelPreferredSize() );
        titleJPanel.setMinimumSize( getTitleJPanelPreferredSize() );
        titleJPanel.setMaximumSize( getTitleJPanelPreferredSize() );
        contentJPanel.setPreferredSize( getContentJPanelPreferredSize() );
        contentJPanel.setMinimumSize( getContentJPanelPreferredSize() );
        contentJPanel.setMaximumSize( getContentJPanelPreferredSize() );
        pack();

        setBounds( Util.generateCenteredBounds( parentWindow,
                                                getPreferredSize().width,
                                                getPreferredSize().height) );
        addWindowListener(this);

        ICON_COMPLETED = new ImageIcon( getClass().getResource("/com/untangle/gui/widgets/wizard/IconCompleteState14x14.png") );
        ICON_CURRENT = ICON_COMPLETED; //new ImageIcon( getClass().getResource("/com/untangle/gui/widgets/wizard/IconCurrentState14x14.png") );
        ICON_UNCOMPLETED = new ImageIcon( getClass().getResource("/com/untangle/gui/widgets/wizard/IconUncompleteState14x14.png") );

        hints = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    protected Dimension getTitleJPanelPreferredSize(){ return new Dimension(250,360); }
    protected Dimension getContentJPanelPreferredSize(){ return new Dimension(485,360); }

    public void setVisible(boolean isVisible){
        if( isVisible ){
            updateButtonState(false);
            updateLabelState();
            updatePageState();
            nextJButton.requestFocus();
        }
        super.setVisible(isVisible);
    }

    public void updateButtonState(boolean disableAll){
        if( disableAll ){
            nextJButton.setEnabled(false);
            previousJButton.setEnabled(false);
            closeJButton.setEnabled(false);
        }
        else{
            previousJButton.setEnabled(true);
            closeJButton.setEnabled(true);
            if( currentPage == 0 ){
                nextJButton.setEnabled(true);
                previousJButton.setVisible(false);
                nextJButton.setText(STRING_NEXT_PAGE);
                nextJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/widgets/wizard/IconNext_16x16.png")));
            }
            else if( currentPage < (wizardPageMap.size()-1) ){
                nextJButton.setEnabled(true);
                previousJButton.setVisible(true);
                nextJButton.setText(STRING_NEXT_PAGE);
                nextJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/widgets/wizard/IconNext_16x16.png")));
            }
            else{
                closeJButton.setEnabled(false);
                nextJButton.setEnabled(true);
                previousJButton.setVisible(true);
                nextJButton.setText(STRING_FINAL_PAGE);
                nextJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/widgets/wizard/IconFinish_16x16.png")));
            }
            if( checkpointVector.elementAt(currentPage) )
                previousJButton.setVisible(false);
        }
        buttonJPanel.repaint();
    }

    protected void updateLabelState(){
        int index = 0;
        for( JLabel label : labelMap.values() ){
            if( index < currentPage ){
                label.setForeground(COLOR_COMPLETED);
                label.setIcon(ICON_COMPLETED);
                label.setFont(NORMAL_FONT);
            }
            else if( index == currentPage ){
                label.setForeground(COLOR_CURRENT);
                label.setIcon(ICON_CURRENT);
                label.setFont(BOLD_FONT);
            }
            else{ //( index > currentPage )
                label.setForeground(COLOR_UNCOMPLETED);
                label.setIcon(ICON_UNCOMPLETED);
                label.setFont(NORMAL_FONT);
            }
            index++;
        }
        titleJPanel.repaint();
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
        newJLabel.setFocusable(false);
        newJLabel.setBorder(new EmptyBorder(0,0,FONT_SPACING,0));
        newJLabel.setFont(NORMAL_FONT);
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

    public boolean isShiftDown(){return isShiftDown; }

    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        titleJPanel = new javax.swing.JPanel();
        contentJPanel = new javax.swing.JPanel();
        buttonJPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        nextJButton = new javax.swing.JButton();
        previousJButton = new javax.swing.JButton();
        closeJButton = new javax.swing.JButton();
        backgroundJLabel = new com.untangle.gui.widgets.MTiledIconLabel(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/DarkGreyBackground1600x100.png")));

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        titleJPanel.setLayout(new javax.swing.BoxLayout(titleJPanel, javax.swing.BoxLayout.Y_AXIS));

        titleJPanel.setFocusable(false);
        titleJPanel.setOpaque(false);
        titleJPanel.setRequestFocusEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 15, 8, 8);
        getContentPane().add(titleJPanel, gridBagConstraints);

        contentJPanel.setLayout(new java.awt.BorderLayout());

        contentJPanel.setBorder(new javax.swing.border.CompoundBorder(new javax.swing.border.EmptyBorder(new java.awt.Insets(4, 4, 4, 4)), new javax.swing.border.EtchedBorder()));
        contentJPanel.setFocusable(false);
        contentJPanel.setRequestFocusEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 7, 8, 15);
        getContentPane().add(contentJPanel, gridBagConstraints);

        buttonJPanel.setLayout(new java.awt.GridBagLayout());

        buttonJPanel.setFocusable(false);
        buttonJPanel.setOpaque(false);
        buttonJPanel.setRequestFocusEnabled(false);
        jPanel1.setLayout(new java.awt.GridBagLayout());

        jPanel1.setOpaque(false);
        nextJButton.setFont(new java.awt.Font("Arial", 0, 12));
        nextJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/widgets/wizard/IconNext_16x16.png")));
        nextJButton.setText("Next");
        nextJButton.setDoubleBuffered(true);
        nextJButton.setIconTextGap(6);
        nextJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        nextJButton.setOpaque(false);
        nextJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    nextJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 15, 0, 0);
        jPanel1.add(nextJButton, gridBagConstraints);

        previousJButton.setFont(new java.awt.Font("Arial", 0, 12));
        previousJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/widgets/wizard/IconPrevious_16x16.png")));
        previousJButton.setText("Previous");
        previousJButton.setDoubleBuffered(true);
        previousJButton.setIconTextGap(6);
        previousJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        previousJButton.setOpaque(false);
        previousJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    previousJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        jPanel1.add(previousJButton, gridBagConstraints);

        closeJButton.setFont(new java.awt.Font("Default", 0, 12));
        closeJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/widgets/wizard/IconClose_16x16.png")));
        closeJButton.setText("Close");
        closeJButton.setDoubleBuffered(true);
        closeJButton.setIconTextGap(6);
        closeJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        closeJButton.setOpaque(false);
        closeJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    closeJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(closeJButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        buttonJPanel.add(jPanel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(7, 15, 15, 15);
        getContentPane().add(buttonJPanel, gridBagConstraints);

        backgroundJLabel.setDoubleBuffered(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(backgroundJLabel, gridBagConstraints);

    }//GEN-END:initComponents



    private void previousJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_previousJButtonActionPerformed
        new PreviousPageThread();
    }//GEN-LAST:event_previousJButtonActionPerformed

    private class PreviousPageThread extends Thread{
        public PreviousPageThread(){
            super("MVCLIENT-WizardPreviousPageThread");
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
                previousWizardPageJPanel = (MWizardPageJPanel) wizardPageMap.values().toArray()[currentPage-1];
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
                    nextJButton.requestFocus();
                    previousWizardPageJPanel.initialFocus();
                }});
            }catch(Exception e){ Util.handleExceptionNoRestart("Error updating wizard on move previous", e); }
        }
    }

    private void nextJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextJButtonActionPerformed
        isShiftDown = (evt.getModifiers()&java.awt.event.ActionEvent.SHIFT_MASK)>0;
        new NextPageThread();
    }//GEN-LAST:event_nextJButtonActionPerformed

    MWizardPageJPanel previousWizardPageJPanel;
    MWizardPageJPanel nextWizardPageJPanel;

    private class NextPageThread extends Thread{
        public NextPageThread(){
            super("MVCLIENT-WizardNextPageThread");
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
                    while( (currentPage-i >= 0) && !saveVector.elementAt(currentPage-i) ){ // save all previous
                        previousWizardPageJPanel = (MWizardPageJPanel) wizardPageMap.values().toArray()[currentPage-i];
                        previousWizardPageJPanel.doSave(null, false);
                        i++;
                    }
                }
            }
            catch(Exception e){
                // REMOVED BECAUSE ITS SCARY Util.handleExceptionNoRestart("Error validating: ", e);
                MOneButtonJDialog.factory(MWizardJDialog.this, "", e.getMessage(), "Wizard", "");
                changePage &= false;
            }
            // SEND LEAVING EVENT TO CURRENT PAGE
            if( changePage ){
                changePage &= currentWizardPageJPanel.leavingForwards();
            }
            // SEND ENTERING EVENT TO NEXT PAGE
            if( changePage && (currentPage < wizardPageMap.size()-1) ){
                nextWizardPageJPanel = (MWizardPageJPanel) wizardPageMap.values().toArray()[currentPage+1];
                changePage &= nextWizardPageJPanel.enteringForwards();
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
                        nextJButton.requestFocus();
                        nextWizardPageJPanel.initialFocus();
                    }});
                }catch(Exception e){ Util.handleExceptionNoRestart("Error updating wizard on move next", e); }
                // FINISH WIZARD
            }
            else{
                if( changePage )
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
    private javax.swing.JPanel buttonJPanel;
    private javax.swing.JButton closeJButton;
    private javax.swing.JPanel contentJPanel;
    private javax.swing.JPanel jPanel1;
    protected javax.swing.JButton nextJButton;
    protected javax.swing.JButton previousJButton;
    private javax.swing.JPanel titleJPanel;
    // End of variables declaration//GEN-END:variables

}
