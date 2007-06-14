/*
 * $HeadURL:$
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

import com.untangle.gui.node.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.coloredTable.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.node.*;
import com.untangle.node.mail.papi.*;
import com.untangle.node.mail.papi.quarantine.*;

public class QuarantineSingleUserJPanel extends javax.swing.JPanel
    implements Refreshable<MailNodeCompoundSettings>, ComponentListener {

    private static final Color TABLE_BACKGROUND_COLOR = new Color(213, 213, 226);
    private QuarantineUserTableModel quarantineUserTableModel;
    private MailNodeCompoundSettings mailNodeCompoundSettings;
    private String account;

    public QuarantineSingleUserJPanel(String account) {
        this.account = account;

        // INIT GUI & CUSTOM INIT
        initComponents();
        entryJScrollPane.getViewport().setOpaque(true);
        entryJScrollPane.getViewport().setBackground(TABLE_BACKGROUND_COLOR);
        entryJScrollPane.setViewportBorder(new MatteBorder(2, 2, 2, 1, TABLE_BACKGROUND_COLOR));
        addComponentListener(QuarantineSingleUserJPanel.this);

        // create actual table model
        quarantineUserTableModel = new QuarantineUserTableModel(account);
        setTableModel( quarantineUserTableModel );
        // intern date - sort by descending order
        quarantineUserTableModel.setSortingStatus(3, quarantineUserTableModel.DESCENDING);
    }

    public void doRefresh(MailNodeCompoundSettings mailNodeCompoundSettings){
        this.mailNodeCompoundSettings = mailNodeCompoundSettings;
        quarantineUserTableModel.doRefresh(mailNodeCompoundSettings);
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
        purgeJButton = new javax.swing.JButton();
        releaseJButton = new javax.swing.JButton();
        entryJScrollPane = new javax.swing.JScrollPane();
        entryJTable = new MColoredJTable();

        setLayout(new java.awt.GridBagLayout());

        contentJPanel.setLayout(new java.awt.GridBagLayout());

        contentJPanel.setOpaque(false);
        eventJPanel.setLayout(new java.awt.GridBagLayout());

        eventJPanel.setFocusCycleRoot(true);
        eventJPanel.setFocusable(false);
        eventJPanel.setOpaque(false);
        purgeJButton.setFont(new java.awt.Font("Dialog", 0, 12));
        purgeJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconPurge_16x16.png")));
        purgeJButton.setText("Purge selected");
        purgeJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        purgeJButton.setMaximumSize(null);
        purgeJButton.setMinimumSize(null);
        purgeJButton.setOpaque(false);
        purgeJButton.setPreferredSize(null);
        purgeJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    purgeJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 5);
        eventJPanel.add(purgeJButton, gridBagConstraints);

        releaseJButton.setFont(new java.awt.Font("Dialog", 0, 12));
        releaseJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconRelease_16x16.png")));
        releaseJButton.setText("Release selected");
        releaseJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        releaseJButton.setMaximumSize(null);
        releaseJButton.setMinimumSize(null);
        releaseJButton.setOpaque(false);
        releaseJButton.setPreferredSize(null);
        releaseJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    releaseJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        eventJPanel.add(releaseJButton, gridBagConstraints);

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

    private void releaseJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_releaseJButtonActionPerformed
        if( Util.getIsDemo() )
            return;
        int[] selectedModelRows = getSelectedModelRows();
        if( selectedModelRows.length == 0 )
            return;

        // release
        Vector<Vector> dataVector = quarantineUserTableModel.getDataVector();
        String[] emails = new String[selectedModelRows.length];
        for( int i=0; i<selectedModelRows.length; i++){
            emails[i] = (String) dataVector.elementAt(selectedModelRows[i]).elementAt(2);
        }
        new ReleaseAndPurgeThread(account,emails,true);
    }//GEN-LAST:event_releaseJButtonActionPerformed

    private void purgeJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_purgeJButtonActionPerformed
        if( Util.getIsDemo() )
            return;
        int[] selectedModelRows = getSelectedModelRows();
        if( selectedModelRows.length == 0 )
            return;

        QuarantinePurgeProceedDialog purgeProceedDialog = new QuarantinePurgeProceedDialog( (Dialog) this.getTopLevelAncestor() );
        if( !purgeProceedDialog.isProceeding() )
            return;

        // purge
        Vector<Vector> dataVector = quarantineUserTableModel.getDataVector();
        String[] emails = new String[selectedModelRows.length];
        for( int i=0; i<selectedModelRows.length; i++){
            emails[i] = (String) dataVector.elementAt(selectedModelRows[i]).elementAt(2);
        }
        new ReleaseAndPurgeThread(account,emails,false);
    }//GEN-LAST:event_purgeJButtonActionPerformed

    private class ReleaseAndPurgeThread extends Thread {
        private String[] emails;
        private String account;
        private boolean doRelease;
        public ReleaseAndPurgeThread(String account, String[] emails, boolean doRelease){
            this.account = account;
            this.emails = emails;
            this.doRelease = doRelease;
            setDaemon(true);
            if( doRelease )
                ((MConfigJDialog)QuarantineSingleUserJPanel.this.getTopLevelAncestor()).getInfiniteProgressJComponent().start("Releasing...");
            else
                ((MConfigJDialog)QuarantineSingleUserJPanel.this.getTopLevelAncestor()).getInfiniteProgressJComponent().start("Purging...");
            start();
        }
        public void run(){
            // DO RESCUE
            try{
                if( doRelease ){
                    mailNodeCompoundSettings.getQuarantineMaintenanceView().rescue(account,emails);
                }
                else{
                    mailNodeCompoundSettings.getQuarantineMaintenanceView().purge(account,emails);
                }
                QuarantineSingleUserJDialog.instance().refreshAll();
            }
            catch(Exception e){
                if( doRelease ){
                    Util.handleExceptionNoRestart("Error releasing inbox", e);
                    MOneButtonJDialog.factory(QuarantineSingleUserJPanel.this.getTopLevelAncestor(), "",
                                              "An account could not be released.",
                                              "Quarantine Release Warning","");
                }
                else{
                    Util.handleExceptionNoRestart("Error purging inbox", e);
                    MOneButtonJDialog.factory(QuarantineSingleUserJPanel.this.getTopLevelAncestor(), "",
                                              "An account could not be purged.",
                                              "Quarantine Purge Warning","");
                }
            }
            // DO REFRESH
            ((MConfigJDialog)QuarantineSingleUserJPanel.this.getTopLevelAncestor()).getInfiniteProgressJComponent().setTextLater("Refreshing...");
            SwingUtilities.invokeLater( new Runnable(){ public void run(){
                quarantineUserTableModel.doRefresh(mailNodeCompoundSettings);
            }});
            ((MConfigJDialog)QuarantineSingleUserJPanel.this.getTopLevelAncestor()).getInfiniteProgressJComponent().stopLater(1500l);
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
    private javax.swing.JButton purgeJButton;
    private javax.swing.JButton releaseJButton;
    // End of variables declaration//GEN-END:variables
}


class QuarantineUserTableModel extends MSortedTableModel<MailNodeCompoundSettings> {

    private String account;
    private static final StringConstants sc = StringConstants.getInstance();

    public QuarantineUserTableModel(String account){
        this.account = account;
    }

    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #   min  rsz    edit   remv   desc   typ               def
        addTableColumn( tableColumnModel,  0,  Util.STATUS_MIN_WIDTH, false, false, true,  false, String.class,     null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1,  Util.LINENO_MIN_WIDTH, false, false, true,  false, Integer.class,    null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2, 150, true,  false,  true,  false, String.class, null, sc.html("MailID") );
        addTableColumn( tableColumnModel,  3, 150, true,  false,  false, false, Date.class,   null, sc.html("Date") );
        addTableColumn( tableColumnModel,  4, 150, true,  false,  false, false, String.class, null, sc.html("Sender") );
        addTableColumn( tableColumnModel,  5, 150, true,  false,  false, true,  String.class, null, sc.html("Subject") );
        addTableColumn( tableColumnModel,  6,  85, true,  false,  false, false, Long.class,   null, sc.html("Size (KB)") );
        addTableColumn( tableColumnModel,  7,  85, true,  false,  false, false, String.class, null, sc.html("Category") );
        addTableColumn( tableColumnModel,  8,  85, true,  false,  false, false, String.class, null, sc.html("Detail") );
        return tableColumnModel;
    }


    public void generateSettings(MailNodeCompoundSettings mailNodeCompoundSettings,
                                 Vector<Vector> tableVector, boolean validateOnly) throws Exception { }

    public Vector<Vector> generateRows(MailNodeCompoundSettings mailNodeCompoundSettings) {

        InboxIndex inboxIndex = mailNodeCompoundSettings.getInboxIndex();
        Vector<Vector> allRows = new Vector<Vector>(inboxIndex.size());
        Vector tempRow = null;
        MailSummary mailSummary = null;
        int rowIndex = 0;

        for( InboxRecord inboxRecord : inboxIndex ){
            rowIndex++;
            tempRow = new Vector(8);
            mailSummary = inboxRecord.getMailSummary();
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            tempRow.add( inboxRecord.getMailID() );
            tempRow.add( inboxRecord.getInternDateAsDate() );
            tempRow.add( mailSummary.getSender() );
            tempRow.add( mailSummary.getSubject() );
            tempRow.add( inboxRecord.getFormattedSize() );
            tempRow.add( mailSummary.getQuarantineCategory() );
            tempRow.add( mailSummary.getFormattedQuarantineDetail() );
            allRows.add( tempRow );
        }

        return allRows;
    }
}
