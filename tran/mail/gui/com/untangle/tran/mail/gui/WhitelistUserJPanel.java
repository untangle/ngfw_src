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

package com.untangle.tran.mail.gui;

import com.untangle.gui.widgets.coloredTable.*;
import com.untangle.gui.widgets.dialogs.RefreshLogFailureDialog;
import com.untangle.gui.widgets.dialogs.MConfigJDialog;
import com.untangle.gui.widgets.dialogs.MOneButtonJDialog;
import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;

import com.untangle.mvvm.tran.*;
import com.untangle.mvvm.security.Tid;

import com.untangle.tran.mail.papi.*;
import com.untangle.tran.mail.papi.safelist.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import com.untangle.gui.widgets.editTable.*;

public class WhitelistUserJPanel extends javax.swing.JPanel
    implements Refreshable<MailTransformCompoundSettings>, ComponentListener {

    private static final Color TABLE_BACKGROUND_COLOR = new Color(213, 213, 226);
    private WhitelistUserTableModel whitelistUserTableModel;
    private MailTransformCompoundSettings mailTransformCompoundSettings;
    private String account;
    
    public WhitelistUserJPanel(String account) {
        this.account = account;
        
        // INIT GUI & CUSTOM INIT
        initComponents();
        entryJScrollPane.getViewport().setOpaque(true);
        entryJScrollPane.getViewport().setBackground(TABLE_BACKGROUND_COLOR);
        entryJScrollPane.setViewportBorder(new MatteBorder(2, 2, 2, 1, TABLE_BACKGROUND_COLOR));
        addComponentListener(WhitelistUserJPanel.this);
        
        // create actual table model
        whitelistUserTableModel = new WhitelistUserTableModel();
        setTableModel( whitelistUserTableModel );
        whitelistUserTableModel.setSortingStatus(2, whitelistUserTableModel.ASCENDING);
    }

    public void doRefresh(MailTransformCompoundSettings mailTransformCompoundSettings){
	this.mailTransformCompoundSettings = mailTransformCompoundSettings;
	whitelistUserTableModel.doRefresh(mailTransformCompoundSettings);
    }
    
    public void setTableModel(MSortedTableModel mSortedTableModel){
        entryJTable.setModel( mSortedTableModel );
        entryJTable.setColumnModel( mSortedTableModel.getTableColumnModel() );
        mSortedTableModel.setTableHeader( entryJTable.getTableHeader() );
        mSortedTableModel.hideColumns( entryJTable );
    }
    
    public MSortedTableModel getTableModel(){
        return (MSortedTableModel) entryJTable.getModel();
    }
    

    public MColoredJTable getJTable(){
        return (MColoredJTable) entryJTable;
    }
        

    public void componentHidden(ComponentEvent e){}
    public void componentMoved(ComponentEvent e){}
    public void componentShown(ComponentEvent e){}
    public void componentResized(ComponentEvent e){
	((MColoredJTable)entryJTable).doGreedyColumn(entryJScrollPane.getViewport().getExtentSize().width);
    }
    
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        contentJPanel = new javax.swing.JPanel();
        eventJPanel = new javax.swing.JPanel();
        removeJButton = new javax.swing.JButton();
        entryJScrollPane = new javax.swing.JScrollPane();
        entryJTable = new MColoredJTable();

        setLayout(new java.awt.GridBagLayout());

        setOpaque(false);
        contentJPanel.setLayout(new java.awt.GridBagLayout());

        contentJPanel.setOpaque(false);
        eventJPanel.setLayout(new java.awt.GridBagLayout());

        eventJPanel.setFocusCycleRoot(true);
        eventJPanel.setFocusable(false);
        eventJPanel.setOpaque(false);
        removeJButton.setFont(new java.awt.Font("Dialog", 0, 12));
        removeJButton.setText("<html><b>Remove</b> selected</html>");
        removeJButton.setDoubleBuffered(true);
        removeJButton.setFocusPainted(false);
        removeJButton.setFocusable(false);
        removeJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        removeJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        removeJButton.setMaximumSize(new java.awt.Dimension(125, 25));
        removeJButton.setMinimumSize(new java.awt.Dimension(125, 25));
        removeJButton.setPreferredSize(new java.awt.Dimension(125, 25));
        removeJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeJButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        eventJPanel.add(removeJButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        contentJPanel.add(eventJPanel, gridBagConstraints);

        entryJScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        entryJScrollPane.setDoubleBuffered(true);
        entryJTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        entryJTable.setDoubleBuffered(true);
        entryJScrollPane.setViewportView(entryJTable);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 0, 2);
        contentJPanel.add(entryJScrollPane, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(contentJPanel, gridBagConstraints);

    }//GEN-END:initComponents

    private void removeJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeJButtonActionPerformed
	if( Util.getIsDemo() )
	    return;
        int[] selectedModelRows = getSelectedModelRows();
        if( selectedModelRows.length == 0 )
                return;
        
        // release
        Vector<Vector> dataVector = whitelistUserTableModel.getDataVector();
        String[] emails = new String[selectedModelRows.length];
        for( int i=0; i<selectedModelRows.length; i++){
            emails[i] = (String) dataVector.elementAt(selectedModelRows[i]).elementAt(2);
        }
	new RemoveThread(account, emails);
    }//GEN-LAST:event_removeJButtonActionPerformed

    private class RemoveThread extends Thread {
	private String[] emails;
	private String account;
	public RemoveThread(String account, String[] emails){
	    this.account = account;
	    this.emails = emails;
	    setDaemon(true);
	    ((MConfigJDialog)WhitelistUserJPanel.this.getTopLevelAncestor()).getInfiniteProgressJComponent().start("Removing...");
	    start();
	}
	public void run(){
	    // DO RESCUE
            try{
		for( String email : emails )
		    mailTransformCompoundSettings.getSafelistAdminView().removeFromSafelist(account, email);
		WhitelistUserJDialog.instance().refreshAll();
            }
            catch(Exception e){
		Util.handleExceptionNoRestart("Error removing address", e);
		MOneButtonJDialog.factory(WhitelistUserJPanel.this.getTopLevelAncestor(), "",
					  "An address could not be removed.",
					  "Safelist Remove Address Warning","");
	    }
	    // DO REFRESH
	    ((MConfigJDialog)WhitelistUserJPanel.this.getTopLevelAncestor()).getInfiniteProgressJComponent().setTextLater("Refreshing...");
	    SwingUtilities.invokeLater( new Runnable(){ public void run(){
		whitelistUserTableModel.doRefresh(mailTransformCompoundSettings);
	    }});
	    ((MConfigJDialog)WhitelistUserJPanel.this.getTopLevelAncestor()).getInfiniteProgressJComponent().stopLater(1500l);
        }
    }
    
    private int[] getSelectedModelRows(){
        int[] selectedViewRows = entryJTable.getSelectedRows();
        if( (selectedViewRows==null) || (selectedViewRows.length==0) || (selectedViewRows[0]==-1) )
            return new int[0];

        // translate view row
        int[] selectedModelRows = new int[selectedViewRows.length];
        for( int i=0; i<selectedViewRows.length; i++ )
            selectedModelRows[i] = getTableModel().getRowViewToModelIndex(selectedViewRows[i]);
        
        return selectedModelRows;
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel contentJPanel;
    protected javax.swing.JScrollPane entryJScrollPane;
    protected javax.swing.JTable entryJTable;
    private javax.swing.JPanel eventJPanel;
    private javax.swing.JButton removeJButton;
    // End of variables declaration//GEN-END:variables
    
}



class WhitelistUserTableModel extends MSortedTableModel<MailTransformCompoundSettings> {

    private static final StringConstants sc = StringConstants.getInstance();
    
    public WhitelistUserTableModel(){

    }
    
    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #   min  rsz    edit   remv   desc   typ               def
        addTableColumn( tableColumnModel,  0,  Util.STATUS_MIN_WIDTH, false, false, true,  false, String.class,     null, sc.TITLE_STATUS );
	addTableColumn( tableColumnModel,  1,  Util.LINENO_MIN_WIDTH, false, false, true, false, Integer.class,    null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2, 150, true,  false,  false, true,  String.class, null, sc.html("Email Address") );
        return tableColumnModel;
    }


   
    public void generateSettings(MailTransformCompoundSettings mailTransformCompoundSettings,
				 Vector<Vector> tableVector, boolean validateOnly) throws Exception { }

    public Vector<Vector> generateRows(MailTransformCompoundSettings mailTransformCompoundSettings) {
        
        String[] addresses = mailTransformCompoundSettings.getSafelistContents();
        Vector<Vector> allRows = new Vector<Vector>(addresses.length);
	Vector tempRow = null;
        int rowIndex = 0;

	for( String address : addresses ){
	    rowIndex++;
            tempRow = new Vector(3);
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            tempRow.add( address );
	    allRows.add( tempRow );
        }
        
        return allRows;

    }





}
