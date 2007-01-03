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

package com.untangle.gui.widgets.editTable;

import com.untangle.gui.widgets.coloredTable.*;
import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;

import javax.swing.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Vector;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.*;


public class MEditTableJPanel extends javax.swing.JPanel
    implements ListSelectionListener, TableModelListener, ComponentListener,
	       Savable<Object>, Refreshable<Object> {

    private JLabel messageJLabel;    
    private MTransformJPanel mTransformJPanel;
    
    // the table model and table
    private MColoredJTable mColoredJTable;
            
    private boolean addRemoveUsable = false;
    private boolean fillUsable = false;
    private boolean alwaysAddLast = false;
    
    private static final Color TABLE_BACKGROUND_COLOR = new Color(213, 213, 226);
    
    private Insets lastInsets;   
    public void setInsets(Insets newInsets){
        GridBagConstraints newConstraints = ((GridBagLayout)this.getLayout()).getConstraints(contentJPanel);
        newConstraints.insets = newInsets;
        ((GridBagLayout)this.getLayout()).setConstraints(contentJPanel, newConstraints);
	lastInsets = newInsets;
    }

    public void setAlwaysAddLast(boolean alwaysAddLast){
	this.alwaysAddLast = alwaysAddLast;
    }
    public boolean getAlwaysAddLast(){
	return alwaysAddLast;
    }

    // SETTINGS CHANGE NOTIFICATION /////////
    private SettingsChangedListener settingsChangedListener;
    public void setSettingsChangedListener(SettingsChangedListener settingsChangedListener){
	this.settingsChangedListener = settingsChangedListener;
	if( getTableModel() != null)
	    getTableModel().setSettingsChangedListener(settingsChangedListener);
    }
    ///////////////////////////////////////////

    // SAVE/REFRESH ///////////
    public void doRefresh(Object compoundSettings){ getTableModel().doRefresh(compoundSettings); }
    public void doSave(Object compoundSettings, boolean validateOnly) throws Exception {
	getTableModel().doSave(compoundSettings, validateOnly);
    }
    
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
	entryJScrollPane.addComponentListener(this);
        this.setOpaque(true);
        contentJPanel.setOpaque(true);
        detailJTextArea.setText("no selection...");        
            
        if(!showButtonJPanel){
            tableJPanel.remove(addJButton);
            tableJPanel.remove(removeJButton);
            tableJPanel.remove(fillJButton);
        }
        else{
            addRemoveUsable = true;
	    fillUsable = true;
        }
                
        if(!showDetailJPanel)
            tableJPanel.remove(detailJScrollPane);
        
        auxJPanel.setVisible(false);
    }

    public void setFillJButtonEnabled(boolean enabled){
	fillJButton.setEnabled(enabled);
	fillUsable = enabled;
    }
    
    public void setShowDetailJPanelEnabled(boolean enabled){
	if(!enabled)
	    tableJPanel.remove(detailJScrollPane);
    }
    
    public void setInstantRemove(boolean enabled){
	getTableModel().setInstantRemove(enabled);
    }

    public void setAuxJPanelEnabled(boolean enabled){
        auxJPanel.setVisible(enabled);
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
	addRemoveUsable = enabled;
    }
    
    public void setTableModel(MSortedTableModel mSortedTableModel){
        mColoredJTable.setModel( mSortedTableModel );
        mColoredJTable.setColumnModel( mSortedTableModel.getTableColumnModel() );
        mSortedTableModel.setTableHeader( mColoredJTable.getTableHeader() );
        mSortedTableModel.hideColumns( mColoredJTable );
        mColoredJTable.getSelectionModel().addListSelectionListener(this);
        mColoredJTable.getModel().addTableModelListener(this);
	mSortedTableModel.setSettingsChangedListener(settingsChangedListener);
    }
    
    public MSortedTableModel getTableModel(){
        return (MSortedTableModel) mColoredJTable.getModel();
    }
    

    public MColoredJTable getJTable(){
        return mColoredJTable;
    }
        
    public void setDetailMessage(String message){
	detailJTextArea.setText(message);
    }
    
    public void valueChanged(ListSelectionEvent e) {
	int selectedViewRows[] = mColoredJTable.getSelectedRows();
        if( (selectedViewRows==null) || (selectedViewRows.length==0) || (selectedViewRows[0]==-1) ){
	    detailJTextArea.setText("no selection...");        
        }
        else{
	    int firstModelRow = getTableModel().getRowViewToModelIndex(selectedViewRows[0]);
	    String description = getTableModel().getDescription( firstModelRow );
            detailJTextArea.setText( description );
	    SwingUtilities.invokeLater( new Runnable(){ public void run(){ 
		MEditTableJPanel.this.detailJScrollPane.getVerticalScrollBar().setValue(0);
            }});	    
	}
    }
    
    public void tableChanged(TableModelEvent e){
        valueChanged(null);
    }
    
    public void setDetailsTitle(String s){}
    public void setTableTitle(String s){}
    
    public void setAllEnabled(boolean enabled){
        mColoredJTable.setEnabled(enabled);
	if(addRemoveUsable){
	    addJButton.setEnabled(enabled);
	    removeJButton.setEnabled(enabled);
	}
	if(fillUsable)
	    fillJButton.setEnabled(enabled);
    }

    public void componentHidden(ComponentEvent e){}
    public void componentMoved(ComponentEvent e){}
    public void componentShown(ComponentEvent e){}
    public void componentResized(ComponentEvent e){
	mColoredJTable.doGreedyColumn(entryJScrollPane.getViewport().getExtentSize().width);
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
                auxJPanel = new javax.swing.JPanel();

                setLayout(new java.awt.GridBagLayout());

                contentJPanel.setLayout(new java.awt.GridBagLayout());

                tableJPanel.setLayout(new java.awt.GridBagLayout());

                tableJPanel.setMinimumSize(new java.awt.Dimension(40, 40));
                entryJScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
                entryJScrollPane.setDoubleBuffered(true);
                entryJScrollPane.setRequestFocusEnabled(false);
                entryJTable.setBackground(new java.awt.Color(213, 213, 226));
                entryJTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
                entryJTable.setDoubleBuffered(true);
                entryJTable.setRequestFocusEnabled(false);
                entryJScrollPane.setViewportView(entryJTable);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.gridheight = 3;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                tableJPanel.add(entryJScrollPane, gridBagConstraints);

                addJButton.setFont(new java.awt.Font("Dialog", 0, 12));
                addJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/widgets/editTable/IconPlus24x24.png")));
                addJButton.setToolTipText("<html><b>Add New Row</b> - Use this button to insert a new row at a selected point in the table.<br>Hold down the shift key when clicking in order to insert at the end of the table.</html>");
                addJButton.setDoubleBuffered(true);
                addJButton.setFocusPainted(false);
                addJButton.setFocusable(false);
                addJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                addJButton.setIconTextGap(0);
                addJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
                addJButton.setMaximumSize(new java.awt.Dimension(32, 32));
                addJButton.setMinimumSize(new java.awt.Dimension(32, 32));
                addJButton.setOpaque(false);
                addJButton.setPreferredSize(new java.awt.Dimension(32, 32));
                addJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                addJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
                gridBagConstraints.weighty = 0.5;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 3, 4);
                tableJPanel.add(addJButton, gridBagConstraints);

                removeJButton.setFont(new java.awt.Font("Dialog", 0, 12));
                removeJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/widgets/editTable/IconMinus24x24.png")));
                removeJButton.setToolTipText("<html><b>Remove Row</b> - Use this button to remove selected rows in a table.</html>");
                removeJButton.setDoubleBuffered(true);
                removeJButton.setFocusPainted(false);
                removeJButton.setFocusable(false);
                removeJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                removeJButton.setIconTextGap(0);
                removeJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
                removeJButton.setMaximumSize(new java.awt.Dimension(32, 32));
                removeJButton.setMinimumSize(new java.awt.Dimension(32, 32));
                removeJButton.setOpaque(false);
                removeJButton.setPreferredSize(new java.awt.Dimension(32, 32));
                removeJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                removeJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 2;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weighty = 0.5;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 4);
                tableJPanel.add(removeJButton, gridBagConstraints);

                fillJButton.setFont(new java.awt.Font("Dialog", 0, 12));
                fillJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/widgets/editTable/IconFill24x24.png")));
                fillJButton.setToolTipText("<html><b>Fill Check Boxes</b> - Use this button to check or uncheck all the checkboxes in a selected column.</html>");
                fillJButton.setDoubleBuffered(true);
                fillJButton.setFocusPainted(false);
                fillJButton.setFocusable(false);
                fillJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                fillJButton.setIconTextGap(0);
                fillJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
                fillJButton.setMaximumSize(new java.awt.Dimension(32, 32));
                fillJButton.setMinimumSize(new java.awt.Dimension(32, 32));
                fillJButton.setOpaque(false);
                fillJButton.setPreferredSize(new java.awt.Dimension(32, 32));
                fillJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                fillJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 3;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
                gridBagConstraints.weighty = 0.5;
                gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 4);
                tableJPanel.add(fillJButton, gridBagConstraints);

                detailJScrollPane.setBorder(new javax.swing.border.EtchedBorder());
                detailJScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                detailJScrollPane.setDoubleBuffered(true);
                detailJScrollPane.setFocusable(false);
                detailJScrollPane.setMinimumSize(new java.awt.Dimension(31, 50));
                detailJScrollPane.setPreferredSize(new java.awt.Dimension(104, 50));
                detailJScrollPane.setRequestFocusEnabled(false);
                detailJTextArea.setEditable(false);
                detailJTextArea.setLineWrap(true);
                detailJTextArea.setWrapStyleWord(true);
                detailJTextArea.setBorder(null);
                detailJTextArea.setDoubleBuffered(true);
                detailJTextArea.setFocusable(false);
                detailJTextArea.setMargin(new java.awt.Insets(2, 2, 2, 2));
                detailJTextArea.setMinimumSize(new java.awt.Dimension(100, 50));
                detailJTextArea.setPreferredSize(null);
                detailJTextArea.setRequestFocusEnabled(false);
                detailJScrollPane.setViewportView(detailJTextArea);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 4;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
                tableJPanel.add(detailJScrollPane, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
                tableJPanel.add(auxJPanel, gridBagConstraints);

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
	    // find the selection point, return if not a proper cell
	    int[] selectedViewRows = entryJTable.getSelectedRows();
	    if( (selectedViewRows==null) || (selectedViewRows.length!=1) || (selectedViewRows[0]==-1) )
		return;
	    int[] selectedViewCols = entryJTable.getSelectedColumns();
	    if( (selectedViewCols==null) || (selectedViewCols.length!=1) || (selectedViewCols[0]==-1) )
		return;
	    
	    // translate view cell
	    int selectedModelCol = getTableModel().getColViewToModelIndex(selectedViewCols[0]);
	    int selectedModelRow = getTableModel().getRowViewToModelIndex(selectedViewRows[0]);
	    
	    // stop editing, if editing
	    if(mColoredJTable.isEditing())
		if(mColoredJTable.getCellEditor().stopCellEditing() == false)
		    return;
	    
	    // fill actual rows, and determine the new view location
	    getTableModel().fillColumn(selectedModelRow, selectedModelCol);
	    int newViewRow = getTableModel().getRowModelToViewIndex(selectedModelRow);
	    
	    // highlight row
	    entryJTable.clearSelection();
	    entryJTable.getSelectionModel().addSelectionInterval(newViewRow, newViewRow);
	    
	    // scroll to row
	    Rectangle rect = entryJTable.getCellRect(newViewRow, 0, true);
	    entryJTable.scrollRectToVisible(rect);
	}
	catch(Exception e){
	    Util.handleExceptionNoRestart("Error filling row", e);
	}
    }//GEN-LAST:event_fillJButtonActionPerformed


    protected boolean generateNewRow(int selectedModelRow){
	    getTableModel().insertNewRow(selectedModelRow);
        return true;
    }

    private void addJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addJButtonActionPerformed
	try{
	    int[] selectedViewRows;
	    if( ((evt.getModifiers() & java.awt.event.ActionEvent.SHIFT_MASK) > 0) || (getAlwaysAddLast()) ){
		selectedViewRows = new int[]{-1};
	    }
	    else{
		// find the insertion point, at the top if none specified
		selectedViewRows = entryJTable.getSelectedRows();
		if( (selectedViewRows==null) || (selectedViewRows.length==0) || (selectedViewRows[0]==-1) )
		    selectedViewRows = new int[]{0};
	    }

	    // stop editing, if editing
	    if(mColoredJTable.isEditing())
		if(mColoredJTable.getCellEditor().stopCellEditing() == false)
		    return;
	    
	    // translate view row, being careful in case there are no rows in the table that can be translated
	    int selectedModelRow;
	    if( selectedViewRows[0] == -1 )
		selectedModelRow = getTableModel().getRowCount();
	    else if( getTableModel().getRowCount() == 0 )
		selectedModelRow = 0;
	    else
		selectedModelRow = getTableModel().getRowViewToModelIndex(selectedViewRows[0]);
	    
	    // insert actual row, and determine its new view location
        if( !generateNewRow(selectedModelRow))
            return;
	    int newViewRow = getTableModel().getRowModelToViewIndex(selectedModelRow);
	    
	    // highlight row
	    entryJTable.clearSelection();
	    entryJTable.getSelectionModel().addSelectionInterval(newViewRow, newViewRow);
	    
	    // scroll to row
	    Rectangle rect = entryJTable.getCellRect(newViewRow, 0, true);
	    entryJTable.scrollRectToVisible(rect);
	}
	catch(Exception e){
	    Util.handleExceptionNoRestart("Error adding row", e);
	}
    }//GEN-LAST:event_addJButtonActionPerformed
    
    
    private void removeJButtonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeJButtonActionPerformed
	try{
	    // find the removal selection, return if none
	    int[] selectedViewRows = entryJTable.getSelectedRows();
	    if( (selectedViewRows==null) || (selectedViewRows.length==0) || (selectedViewRows[0]==-1) )
		return;
	    
	    // stop editing, if editing
	    if(mColoredJTable.isEditing())
		if(mColoredJTable.getCellEditor().stopCellEditing() == false)
		    return;
	    
	    // determine the selections within the actual model
	    int[] selectedModelRows = new int[selectedViewRows.length];
	    int minViewIndex = -1;
	    int minViewPosition = -1;
	    for( int i=0; i<selectedViewRows.length; i++ ){
		selectedModelRows[i] = getTableModel().getRowViewToModelIndex(selectedViewRows[i]);
		if( minViewIndex == -1 ){
		    minViewIndex = i;
		    minViewPosition = selectedViewRows[i];
		}
		else if( minViewPosition > selectedViewRows[i] ){
		    minViewIndex = i;
		    minViewPosition = selectedViewRows[i];
		}	 
	    }
	    
	    // XXX had to remove the following because it was error prone... it doesnt properly handle the case where all rows are deleted...  and who knows what else...

	    // remove the actual rows, and determine the view location of the first selection
	    entryJTable.clearSelection(); // XXX because if there is a selection after row removal... some null pointer exception gets thrown
	    MEditTableJPanel.this.getTableModel().removeSelectedRows(selectedModelRows);
	    //int newViewRow = getTableModel().getRowModelToViewIndex(selectedModelRows[minViewIndex]);
	    
	    // highlight row
	    //entryJTable.clearSelection();
	    //entryJTable.getSelectionModel().addSelectionInterval(newViewRow, newViewRow);
	    
	    // scroll to row
	    //Rectangle rect = entryJTable.getCellRect(newViewRow, 0, true);
	    //entryJTable.scrollRectToVisible(rect);
	}
	catch(Exception e){
	    Util.handleExceptionNoRestart("Error removing row", e);
	}
    }//GEN-LAST:event_removeJButtonActionPerformed
    
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton addJButton;
        protected javax.swing.JPanel auxJPanel;
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

