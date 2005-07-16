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

package com.metavize.gui.widgets.editTable;

import com.metavize.gui.widgets.coloredTable.*;
import com.metavize.gui.transform.*;
import com.metavize.gui.util.*;

import javax.swing.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Vector;
import java.awt.*;
import javax.swing.border.*;
/**
 *
 * @author  inieves
 */
public class MEditTableJPanel extends javax.swing.JPanel implements ListSelectionListener, TableModelListener, Refreshable, Savable {

    private JLabel messageJLabel;
    
    private MTransformJPanel mTransformJPanel;
    
    // the table model and table
    private MColoredJTable mColoredJTable;
            
    private boolean addRemoveUsable = false;
    
    private static final Color TABLE_BACKGROUND_COLOR = new Color(213, 213, 226);
    
    private Insets lastInsets;
    
    public void setInsets(Insets newInsets){
        GridBagConstraints newConstraints = ((GridBagLayout)this.getLayout()).getConstraints(contentJPanel);
        newConstraints.insets = newInsets;
        ((GridBagLayout)this.getLayout()).setConstraints(contentJPanel, newConstraints);
	lastInsets = newInsets;
    }

    // SAVE/REFRESH ///////////
    public void doRefresh(Object settings){ getTableModel().doRefresh(settings); }
    public void doSave(Object settings, boolean validateOnly) throws Exception { getTableModel().doSave(settings, validateOnly); }
    
    public void setMTransformJPanel(MTransformJPanel mTransformJPanel){
        this.mTransformJPanel = mTransformJPanel;
    }
    
    public MEditTableJPanel() {
        this(false, false);
    }
    
    public MEditTableJPanel(boolean showButtonJPanel, boolean showDetailJPanel) {
        
        
        // INIT GUI
        mColoredJTable = new MColoredJTable();
        //mColoredJTable.getSelectionModel().addListSelectionListener(this);
        initComponents();
	messageJLabel = new JLabel();
	messageJLabel.setFont(new java.awt.Font("Arial", 0, 11));
        entryJScrollPane.getViewport().setOpaque(true);
        entryJScrollPane.getViewport().setBackground(TABLE_BACKGROUND_COLOR);
        entryJScrollPane.setViewportBorder(new MatteBorder(2, 2, 2, 1, TABLE_BACKGROUND_COLOR));
        this.setOpaque(false);
        contentJPanel.setOpaque(false);
        detailJTextArea.setText("no selection...");
        
            
        if(!showButtonJPanel){
            tableJPanel.remove(addJButton);
            tableJPanel.remove(removeJButton);
            tableJPanel.remove(fillJButton);
        }
        else{
            addRemoveUsable = true;
        }
                
        if(!showDetailJPanel)
            tableJPanel.remove(detailJScrollPane);
    }

    public void setFillJButtonEnabled(boolean enabled){
	fillJButton.setEnabled(enabled);
    }


    public void setMessage(String message){
	if(message == null){
	    this.removeAll();
	    GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
	    gridBagConstraints.gridx = 0;
	    gridBagConstraints.gridy = 0;
	    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
	    gridBagConstraints.weightx = 1.0;
	    gridBagConstraints.weighty = 1.0;
	    gridBagConstraints.insets = lastInsets;
	    this.add(contentJPanel, gridBagConstraints);
	    this.validate();
	    this.repaint();
	}
	else{
	    messageJLabel.setText(message);
	    this.removeAll();
	    GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
	    gridBagConstraints.gridx = 0;
	    gridBagConstraints.gridy = 0;
	    gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
	    gridBagConstraints.weightx = 0;
	    gridBagConstraints.weighty = 0;
	    this.add(messageJLabel, gridBagConstraints);
	    this.validate();
	    this.repaint();
	}
    }
    
    public void setAddRemoveEnabled(boolean enabled){
        addJButton.setEnabled(enabled);
        removeJButton.setEnabled(enabled);
        
        if(!enabled)
            addRemoveUsable = false;
    }
    
    public void setTableModel(MSortedTableModel mSortedTableModel){
        mColoredJTable.setModel( mSortedTableModel );
        mColoredJTable.setColumnModel( mSortedTableModel.getTableColumnModel() );
        mSortedTableModel.setTableHeader( mColoredJTable.getTableHeader() );
        mSortedTableModel.hideColumns( mColoredJTable );
        mColoredJTable.getSelectionModel().addListSelectionListener(this);
        mColoredJTable.getModel().addTableModelListener(this);
    }
    
    public MSortedTableModel getTableModel(){
        return (MSortedTableModel) mColoredJTable.getModel();
    }
    

    public MColoredJTable getJTable(){
        return mColoredJTable;
    }
        
    public void valueChanged(ListSelectionEvent e) {
	int firstIndex = mColoredJTable.getSelectedRow();
        if( firstIndex>=0 ){
            detailJTextArea.setText(((MSortedTableModel) mColoredJTable.getModel()).getDescription( firstIndex ));
            SwingUtilities.invokeLater( new Runnable(){ public void run(){ 
                MEditTableJPanel.this.detailJScrollPane.getVerticalScrollBar().setValue(0);
            }});
        }
        else
            detailJTextArea.setText("no selection...");        
    }
    
    public void tableChanged(TableModelEvent e){
        valueChanged(null);
    }
    
    public void setDetailsTitle(String s){}
    public void setTableTitle(String s){}
    
    public void setAllEnabled(boolean enabled){
        mColoredJTable.setEnabled(enabled);
        addJButton.setVisible(enabled);
        removeJButton.setVisible(enabled);
        fillJButton.setVisible(enabled);
    }
    
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        contentJPanel = new javax.swing.JPanel();
        tableJPanel = new javax.swing.JPanel();
        entryJScrollPane = new javax.swing.JScrollPane();
        entryJTable = mColoredJTable;
        addJButton = new javax.swing.JButton();
        removeJButton = new javax.swing.JButton();
        fillJButton = new javax.swing.JButton();
        detailJScrollPane = new javax.swing.JScrollPane();
        detailJTextArea = new javax.swing.JTextArea();

        setLayout(new java.awt.GridBagLayout());

        contentJPanel.setLayout(new java.awt.GridBagLayout());

        tableJPanel.setLayout(new java.awt.GridBagLayout());

        tableJPanel.setMinimumSize(new java.awt.Dimension(40, 40));
        entryJScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        entryJScrollPane.setDoubleBuffered(true);
        entryJTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        entryJTable.setDoubleBuffered(true);
        entryJScrollPane.setViewportView(entryJTable);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        tableJPanel.add(entryJScrollPane, gridBagConstraints);

        addJButton.setFont(new java.awt.Font("Dialog", 0, 12));
        addJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/widgets/editTable/IconPlus24x24.png")));
        addJButton.setToolTipText("<html><b>Add New Row</b> - Use this button to insert new rows in a table.</html>");
        addJButton.setDoubleBuffered(true);
        addJButton.setFocusPainted(false);
        addJButton.setFocusable(false);
        addJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        addJButton.setIconTextGap(0);
        addJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        addJButton.setMaximumSize(new java.awt.Dimension(32, 32));
        addJButton.setMinimumSize(new java.awt.Dimension(32, 32));
        addJButton.setPreferredSize(new java.awt.Dimension(32, 32));
        addJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addJButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 3, 4);
        tableJPanel.add(addJButton, gridBagConstraints);

        removeJButton.setFont(new java.awt.Font("Dialog", 0, 12));
        removeJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/widgets/editTable/IconMinus24x24.png")));
        removeJButton.setToolTipText("<html><b>Remove Row</b> - Use this button to remove a row in a table.</html>");
        removeJButton.setDoubleBuffered(true);
        removeJButton.setFocusPainted(false);
        removeJButton.setFocusable(false);
        removeJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        removeJButton.setIconTextGap(0);
        removeJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        removeJButton.setMaximumSize(new java.awt.Dimension(32, 32));
        removeJButton.setMinimumSize(new java.awt.Dimension(32, 32));
        removeJButton.setPreferredSize(new java.awt.Dimension(32, 32));
        removeJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeJButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 4);
        tableJPanel.add(removeJButton, gridBagConstraints);

        fillJButton.setFont(new java.awt.Font("Dialog", 0, 12));
        fillJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/widgets/editTable/IconFill24x24.png")));
        fillJButton.setToolTipText("<html><b>Fill Check Boxes</b> - Use this button to check or uncheck all the checkboxes in a column.</html>");
        fillJButton.setDoubleBuffered(true);
        fillJButton.setFocusPainted(false);
        fillJButton.setFocusable(false);
        fillJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        fillJButton.setIconTextGap(0);
        fillJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        fillJButton.setMaximumSize(new java.awt.Dimension(32, 32));
        fillJButton.setMinimumSize(new java.awt.Dimension(32, 32));
        fillJButton.setPreferredSize(new java.awt.Dimension(32, 32));
        fillJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fillJButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 4);
        tableJPanel.add(fillJButton, gridBagConstraints);

        detailJScrollPane.setBorder(new javax.swing.border.EtchedBorder());
        detailJScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        detailJScrollPane.setDoubleBuffered(true);
        detailJScrollPane.setFocusable(false);
        detailJScrollPane.setMinimumSize(new java.awt.Dimension(31, 50));
        detailJScrollPane.setOpaque(false);
        detailJScrollPane.setPreferredSize(new java.awt.Dimension(104, 50));
        detailJTextArea.setBackground(new java.awt.Color(204, 204, 204));
        detailJTextArea.setEditable(false);
        detailJTextArea.setLineWrap(true);
        detailJTextArea.setWrapStyleWord(true);
        detailJTextArea.setBorder(null);
        detailJTextArea.setFocusable(false);
        detailJTextArea.setMargin(new java.awt.Insets(2, 2, 2, 2));
        detailJTextArea.setMinimumSize(new java.awt.Dimension(100, 50));
        detailJTextArea.setOpaque(false);
        detailJTextArea.setPreferredSize(null);
        detailJScrollPane.setViewportView(detailJTextArea);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
        tableJPanel.add(detailJScrollPane, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        contentJPanel.add(tableJPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(contentJPanel, gridBagConstraints);

    }//GEN-END:initComponents

    private void fillJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fillJButtonActionPerformed
        try{
	    final int selectedColumn = entryJTable.getSelectedColumn();
	    final int selectedRow    = entryJTable.getSelectedRow();
	    
	    if(selectedColumn < 0)
		return;
	    
	    if(selectedRow < 0)
		return;        
	    
	    entryJTable.getCellEditor().stopCellEditing();
	    this.getTableModel().fillColumn(selectedRow, selectedColumn);
	    entryJTable.changeSelection(selectedRow, selectedColumn, false, false);
	    entryJTable.requestFocus();
	}
	catch(Exception e){
	    e.printStackTrace();
	}
    }//GEN-LAST:event_fillJButtonActionPerformed


    private void addJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addJButtonActionPerformed
        try{
	    if(mColoredJTable.isEditing())
		if(mColoredJTable.getCellEditor().stopCellEditing() == false)
		    return;
	    
	    int selectedRow    = entryJTable.getSelectedRow();
	    
	    if(selectedRow < 0)
		selectedRow = 0;
	    
	    // add the new row
            this.getTableModel().insertNewRow(selectedRow);
	    entryJTable.clearSelection();
	    entryJTable.getSelectionModel().addSelectionInterval(selectedRow, selectedRow);
	    Rectangle rect = entryJTable.getCellRect(selectedRow, 0, true);
	    entryJTable.scrollRectToVisible(rect);
        }
        catch(Exception e){
	    e.printStackTrace();
        }

        // scroll the scroll pane to see the new row
        //Rectangle cellRect = entryJTable.getCellRect(minIndex, 0, true);
        //entryJScrollPane.getViewport().setViewPosition( new Point(0, cellRect.height) );
        //entryJTable.getSelectionModel().setSelectionInterval(minIndex, minIndex);
    }//GEN-LAST:event_addJButtonActionPerformed

    
    
    private void removeJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeJButtonActionPerformed
        try{
	    if(mColoredJTable.isEditing())
		if(mColoredJTable.getCellEditor().stopCellEditing() == false)
		    return;
	    
	    int selectedRow    = entryJTable.getSelectedRow();
	    
	    if(selectedRow < 0)
		return;
	    
	    int[] selectedRows = entryJTable.getSelectedRows();
	    Vector[] selectedVectors = new Vector[selectedRows.length];
	    
	    this.getTableModel().removeSelectedRows(selectedRows);
	    
	    entryJTable.clearSelection();
	}
	catch(Exception e){
	    e.printStackTrace();
	}
    }//GEN-LAST:event_removeJButtonActionPerformed
   
    
 
    
    
    
   
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addJButton;
    private javax.swing.JPanel contentJPanel;
    private javax.swing.JScrollPane detailJScrollPane;
    protected javax.swing.JTextArea detailJTextArea;
    protected javax.swing.JScrollPane entryJScrollPane;
    protected javax.swing.JTable entryJTable;
    private javax.swing.JButton fillJButton;
    private javax.swing.JButton removeJButton;
    private javax.swing.JPanel tableJPanel;
    // End of variables declaration//GEN-END:variables
    
}

