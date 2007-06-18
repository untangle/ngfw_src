/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.mail.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.configuration.EmailCompoundSettings;
import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.coloredTable.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.node.*;
import com.untangle.node.mail.papi.*;
import com.untangle.node.mail.papi.safelist.*;

public class WhitelistAllUsersJPanel extends javax.swing.JPanel
    implements Refreshable<EmailCompoundSettings>, ComponentListener {

    private static final Color TABLE_BACKGROUND_COLOR = new Color(213, 213, 226);

    private WhitelistAllUsersTableModel whitelistAllUsersTableModel;
    private MailNodeCompoundSettings mailNodeCompoundSettings;

    public WhitelistAllUsersJPanel() {
        // INIT GUI & CUSTOM INIT
        initComponents();
        entryJScrollPane.getViewport().setOpaque(true);
        entryJScrollPane.getViewport().setBackground(TABLE_BACKGROUND_COLOR);
        entryJScrollPane.setViewportBorder(new MatteBorder(2, 2, 2, 1, TABLE_BACKGROUND_COLOR));
        addComponentListener(WhitelistAllUsersJPanel.this);

        // create actual table model
        whitelistAllUsersTableModel = new WhitelistAllUsersTableModel();
        setTableModel( whitelistAllUsersTableModel );
        whitelistAllUsersTableModel.setSortingStatus(2, whitelistAllUsersTableModel.ASCENDING);
    }

    public void doRefresh(EmailCompoundSettings emailCompoundSettings){
        mailNodeCompoundSettings = (MailNodeCompoundSettings) emailCompoundSettings.getMailNodeCompoundSettings();
        whitelistAllUsersTableModel.doRefresh(mailNodeCompoundSettings);
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
        detailJButton = new javax.swing.JButton();
        entryJScrollPane = new javax.swing.JScrollPane();
        entryJTable = new MColoredJTable();

        setLayout(new java.awt.GridBagLayout());

        contentJPanel.setLayout(new java.awt.GridBagLayout());

        contentJPanel.setOpaque(false);
        eventJPanel.setLayout(new java.awt.GridBagLayout());

        eventJPanel.setFocusCycleRoot(true);
        eventJPanel.setFocusable(false);
        eventJPanel.setOpaque(false);
        removeJButton.setFont(new java.awt.Font("Dialog", 0, 12));
        removeJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconPurge_16x16.png")));
        removeJButton.setText("Purge selected");
        removeJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        removeJButton.setMaximumSize(null);
        removeJButton.setMinimumSize(null);
        removeJButton.setOpaque(false);
        removeJButton.setPreferredSize(null);
        removeJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    removeJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        eventJPanel.add(removeJButton, gridBagConstraints);

        detailJButton.setFont(new java.awt.Font("Dialog", 0, 12));
        detailJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconDetail_16x16.png")));
        detailJButton.setText("Show detail");
        detailJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        detailJButton.setMaximumSize(null);
        detailJButton.setMinimumSize(null);
        detailJButton.setOpaque(false);
        detailJButton.setPreferredSize(null);
        detailJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    detailJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        eventJPanel.add(detailJButton, gridBagConstraints);

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

    private void detailJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_detailJButtonActionPerformed
        int[] selectedModelRows = getSelectedModelRows();
        if( selectedModelRows.length == 0 )
            return;

        // show detail dialog
        Vector<Vector> dataVector = whitelistAllUsersTableModel.getDataVector();
        String account = (String) dataVector.elementAt(selectedModelRows[0]).elementAt(2);
        (new WhitelistUserJDialog((Dialog)getTopLevelAncestor(), mailNodeCompoundSettings, account)).setVisible(true);

        // refresh
        whitelistAllUsersTableModel.doRefresh(mailNodeCompoundSettings);
    }//GEN-LAST:event_detailJButtonActionPerformed

    private void removeJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeJButtonActionPerformed
        if( Util.getIsDemo() )
            return;
        int[] selectedModelRows = getSelectedModelRows();
        if( selectedModelRows.length == 0 )
            return;

        WhitelistRemoveProceedDialog whitelistRemoveProceedDialog = new WhitelistRemoveProceedDialog( (Dialog) this.getTopLevelAncestor() );
        if( !whitelistRemoveProceedDialog.isProceeding() )
            return;

        // release
        String account;
        Vector<Vector> dataVector = whitelistAllUsersTableModel.getDataVector();
        for( int i : selectedModelRows ){
            account = (String) dataVector.elementAt(i).elementAt(2);
            try{
                mailNodeCompoundSettings.getSafelistAdminView().deleteSafelist(account);
            }
            catch(Exception e){Util.handleExceptionNoRestart("Error deleting whitelist: " + account, e);}
        }

        // refresh
        whitelistAllUsersTableModel.doRefresh(mailNodeCompoundSettings);
    }//GEN-LAST:event_removeJButtonActionPerformed

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
    private javax.swing.JButton detailJButton;
    protected javax.swing.JScrollPane entryJScrollPane;
    protected javax.swing.JTable entryJTable;
    private javax.swing.JPanel eventJPanel;
    private javax.swing.JButton removeJButton;
    // End of variables declaration//GEN-END:variables

}



class WhitelistAllUsersTableModel extends MSortedTableModel<MailNodeCompoundSettings> {

    private SafelistAdminView safelistAdminView;
    private static final StringConstants sc = StringConstants.getInstance();

    public WhitelistAllUsersTableModel(){
    }

    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #   min  rsz    edit   remv   desc   typ               def
        addTableColumn( tableColumnModel,  0,  Util.STATUS_MIN_WIDTH, false, false, true, false, String.class,     null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1,  Util.LINENO_MIN_WIDTH, false, false, true, false, Integer.class,    null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2, 300, true,  false,  false, true,  String.class, null, sc.html("account address") );
        addTableColumn( tableColumnModel,  3,  85, true,  false,  false, false, Integer.class, null, sc.html("safe list<br>size") );
        return tableColumnModel;
    }



    public void generateSettings(MailNodeCompoundSettings mailNodeCompoundSettings,
                                 Vector<Vector> tableVector, boolean validateOnly) throws Exception { }


    public Vector<Vector> generateRows(MailNodeCompoundSettings mailNodeCompoundSettings) {

        java.util.List<String> safelists = mailNodeCompoundSettings.getSafelists();
        int[] counts = mailNodeCompoundSettings.getSafelistCounts();

        Vector<Vector> allRows = new Vector<Vector>(safelists.size());
        Vector tempRow = null;
        int rowIndex = 0;
        int countIndex = 0;
        for( String safelist : safelists ){
            countIndex++;
            if( safelist.equalsIgnoreCase("GLOBAL") )
                continue;
            rowIndex++;
            tempRow = new Vector(4);
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            tempRow.add( safelist );
            tempRow.add( counts[countIndex-1] );
            allRows.add( tempRow );
        }
        return allRows;
    }





}
