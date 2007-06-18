/*
 * $HeadURL$
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

package com.untangle.gui.pipeline;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import javax.swing.*;

import com.untangle.gui.configuration.DirectoryCompoundSettings;
import com.untangle.gui.configuration.DirectoryJDialog;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.premium.PremiumJPanel;
import com.untangle.uvm.addrbook.RepositoryType;
import com.untangle.uvm.addrbook.UserEntry;
import com.untangle.uvm.node.firewall.user.UserMatcherConstants;

import com.untangle.uvm.license.ProductIdentifier;

public class UidSelectJDialog extends javax.swing.JDialog implements java.awt.event.WindowListener {

    private static InfiniteProgressJComponent infiniteProgressJComponent = new InfiniteProgressJComponent();

    public static final String ANY_USER = UserMatcherConstants.MARKER_ANY;
    public static final String MULTIPLE_USERS = "[multiple selections]";

    private List<String> uidList;
    private boolean isProceeding;
    private Window window;

    private boolean isPremium;

    public static UidSelectJDialog factory(Container topLevelContainer){
        UidSelectJDialog uidSelectJDialog;
        if(topLevelContainer instanceof Frame)
            uidSelectJDialog = new UidSelectJDialog((Frame)topLevelContainer);
        else
            uidSelectJDialog = new UidSelectJDialog((Dialog)topLevelContainer);
        return uidSelectJDialog;
    }

    public UidSelectJDialog(Dialog topLevelDialog) {
        super( topLevelDialog, true);
        init( topLevelDialog );

    }

    public UidSelectJDialog(Frame topLevelFrame) {
        super( topLevelFrame, true);
        init( topLevelFrame );
    }

    private void init(Window window) {
        this.isPremium = Util.getIsPremium(ProductIdentifier.ADDRESS_BOOK);
        if ( !this.isPremium ) {
            getContentPane().add(new PremiumJPanel());
            this.window = window;
            return;
        }

        initComponents();
        userJScrolPane.getVerticalScrollBar().setFocusable(false);
        uidJList.setCellRenderer(new UidCellRenderer());

        uidJList.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent me) {
                    int selectedIndex = uidJList.locationToIndex(me.getPoint());
                    if (selectedIndex < 0)
                        return;
                    UserEntryWrapper item = (UserEntryWrapper)uidJList.getModel().getElementAt(selectedIndex);
                    if(item.getUID().equals(ANY_USER)){
                        boolean enabled;
                        if(item.isSelected()){
                            enabled = true;
                        }
                        else{
                            enabled = false;
                        }
                        for(int i=1; i < uidJList.getModel().getSize(); i++){
                            UserEntryWrapper target = (UserEntryWrapper)uidJList.getModel().getElementAt(i);
                            target.setEnabled(enabled);
                        }
                    }
                    if(item.isEnabled())
                        item.setSelected(!item.isSelected());

                    uidJList.repaint();
                }
            });
        
        this.addWindowListener(this);
        this.window = window;
        setGlassPane(infiniteProgressJComponent);
    }

    public List<String> getUidList(){ return uidList; }
    public void setUidList(List<String> uidList){
        this.uidList = uidList;
    }

    public void updateUidModel(List<UserEntry> userEntries){
        if (!this.isPremium ) return;

        Vector<UserEntryWrapper> uidVector = new Vector<UserEntryWrapper>();

        UserEntryWrapper t = new UserEntryWrapper(null);
        uidVector.add(t);
        for( UserEntry userEntry : userEntries ){
            t = new UserEntryWrapper(userEntry);
            uidVector.add( t );
        }

        boolean any=false;
        for(String s : uidList){
            if(s.equals(ANY_USER))
                any=true;
        }
        if(any){
            boolean first = true;
            for(UserEntryWrapper u : uidVector){

                if(!first){
                    u.setEnabled(false);
                    u.setSelected(false);
                }
                else{
                    u.setSelected(true);
                }
                first = false;
            }
        }
        else{
            for(String s : uidList){
                for(UserEntryWrapper u : uidVector){
                    if(s.equals(u.getUID())){
                        u.setSelected(true);
                    }

                }
            }
        }
        uidJList.setListData(uidVector);
    }

    public void setVisible(boolean isVisible){
        if(isVisible){
            pack();
            this.setBounds( Util.generateCenteredBounds(window, this.getWidth(), this.getHeight()) );
            if ( this.isPremium ) new PerformRefreshThread();
        }
        super.setVisible(isVisible);
        if(!isVisible){
            dispose();
        }
    }

    protected class UidCellRenderer implements ListCellRenderer {
        private JCheckBox target = new JCheckBox();
        public Component getListCellRendererComponent( JList list, Object value, int index,
                                                       boolean isSelected, boolean cellHasFocus){
            JCheckBox cb = (JCheckBox) value;
            target.setEnabled(cb.isEnabled());
            target.setSelected(cb.isSelected());
            target.setText(cb.getText());
            target.setBorderPainted(cellHasFocus);
            target.setFocusPainted(true);
            return target;
        }

    }
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        buttonGroup1 = new javax.swing.ButtonGroup();
        iconJLabel = new javax.swing.JLabel();
        dividerJPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        labelJLabel = new javax.swing.JLabel();
        messageJLabel = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        userJScrolPane = new javax.swing.JScrollPane();
        uidJList = new javax.swing.JList();
        jLabel2 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        cancelJButton = new javax.swing.JButton();
        proceedJButton = new javax.swing.JButton();
        backgroundJLabel = new com.untangle.gui.widgets.MTiledIconLabel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Portal Question");
        setModal(true);
        setResizable(false);
        iconJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconDialogQuestion_96x96.png")));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(20, 20, 20, 20);
        getContentPane().add(iconJLabel, gridBagConstraints);

        dividerJPanel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(154, 154, 154)));
        dividerJPanel.setMaximumSize(new java.awt.Dimension(1, 1600));
        dividerJPanel.setMinimumSize(new java.awt.Dimension(1, 10));
        dividerJPanel.setOpaque(false);
        dividerJPanel.setPreferredSize(new java.awt.Dimension(1, 10));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(20, 0, 20, 0);
        getContentPane().add(dividerJPanel, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        jPanel1.setOpaque(false);
        labelJLabel.setFont(new java.awt.Font("Dialog", 1, 24));
        labelJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelJLabel.setText("Select users:");
        labelJLabel.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
        jPanel1.add(labelJLabel, gridBagConstraints);

        messageJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        messageJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        messageJLabel.setText("<html>\n<center>\nYou may choose user IDs/Logins that exist in the User Directory<br>\n(either local or remote Active Directory), or you can add a new <br>\nuser to the User Directory, and then choose that user.\n</center></html>");
        messageJLabel.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
        jPanel1.add(messageJLabel, gridBagConstraints);

        jLabel1.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Select an existing user or users:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        jPanel1.add(jLabel1, gridBagConstraints);

        userJScrolPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        userJScrolPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        userJScrolPane.setMaximumSize(new java.awt.Dimension(300, 200));
        userJScrolPane.setMinimumSize(new java.awt.Dimension(300, 200));
        userJScrolPane.setPreferredSize(new java.awt.Dimension(300, 200));
        uidJList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        uidJList.setMaximumSize(null);
        uidJList.setMinimumSize(null);
        uidJList.setPreferredSize(null);
        userJScrolPane.setViewportView(uidJList);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
        jPanel1.add(userJScrolPane, gridBagConstraints);

        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12));
        jLabel2.setText("Add a new user:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        jPanel1.add(jLabel2, gridBagConstraints);

        jButton1.setFont(new java.awt.Font("Dialog", 0, 12));
        jButton1.setText("Open User Directory");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    jButton1ActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 20, 0);
        jPanel1.add(jButton1, gridBagConstraints);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jPanel2.setOpaque(false);
        cancelJButton.setFont(new java.awt.Font("Default", 0, 12));
        cancelJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconCancel_16x16.png")));
        cancelJButton.setText("Cancel");
        cancelJButton.setDoubleBuffered(true);
        cancelJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        cancelJButton.setMaximumSize(null);
        cancelJButton.setMinimumSize(null);
        cancelJButton.setPreferredSize(null);
        cancelJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    cancelJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        jPanel2.add(cancelJButton, gridBagConstraints);

        proceedJButton.setFont(new java.awt.Font("Default", 0, 12));
        proceedJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/IconSave_23x16.png")));
        proceedJButton.setText("Proceed");
        proceedJButton.setDoubleBuffered(true);
        proceedJButton.setMargin(new java.awt.Insets(4, 8, 4, 8));
        proceedJButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    proceedJButtonActionPerformed(evt);
                }
            });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        jPanel2.add(proceedJButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(20, 0, 0, 0);
        jPanel1.add(jPanel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(20, 20, 20, 20);
        getContentPane().add(jPanel1, gridBagConstraints);

        backgroundJLabel.setFont(new java.awt.Font("Default", 0, 12));
        backgroundJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        backgroundJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/images/LightGreyBackground1600x100.png")));
        backgroundJLabel.setDoubleBuffered(true);
        backgroundJLabel.setFocusable(false);
        backgroundJLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        backgroundJLabel.setOpaque(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(backgroundJLabel, gridBagConstraints);

    }//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        try{
            DirectoryJDialog directoryJDialog = new DirectoryJDialog((Dialog)this);
            directoryJDialog.setVisible(true);
            DirectoryCompoundSettings directoryCompoundSettings = (DirectoryCompoundSettings) directoryJDialog.getCompoundSettings();
            new PerformRefreshThread();
        }
        catch(Exception e){
            try{ Util.handleExceptionWithRestart("Error showing directory", e); }
            catch(Exception f){ Util.handleExceptionNoRestart("Error showing directory", f); }
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void proceedJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proceedJButtonActionPerformed
        ListModel uidModel = uidJList.getModel();
        List<String> outputUidList = new Vector<String>();
        for(int i=0; i<uidModel.getSize(); i++){
            UserEntryWrapper target = ((UserEntryWrapper)uidModel.getElementAt(i));
            if(target.isSelected() && target.isEnabled()){
                outputUidList.add(target.getUID());
            }
        }

        uidList = outputUidList;
        isProceeding = true;
        windowClosing(null);
    }//GEN-LAST:event_proceedJButtonActionPerformed

    private void cancelJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelJButtonActionPerformed
        isProceeding = false;
        windowClosing(null);
    }//GEN-LAST:event_cancelJButtonActionPerformed


    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
        this.setVisible(false);
    }


    class PerformRefreshThread extends Thread {
        public PerformRefreshThread(){
            super("MVCLIENT-PerformRefreshThread");
            setDaemon(true);
            infiniteProgressJComponent.start("Refreshing...");
            start();
        }
        public void run(){
            try{
                final List<UserEntry> allUserEntries = new Vector<UserEntry>();
                try{
                    allUserEntries.addAll( Util.getAddressBook().getUserEntries(RepositoryType.MS_ACTIVE_DIRECTORY) );
                }
                catch(Exception f){
                    Util.handleExceptionNoRestart("Error doing refresh", f);
                    MOneButtonJDialog.factory(UidSelectJDialog.this, "Policy Manager", "There was a problem refreshing Active Directory users.  Please check your Active Directory settings and then try again.",
                                              "Policy Manager Warning", "Policy Manager Warning");
                }

                try{
                    allUserEntries.addAll( Util.getAddressBook().getUserEntries(RepositoryType.LOCAL_DIRECTORY) );
                }
                catch(Exception f){
                    Util.handleExceptionNoRestart("Error doing refresh", f);
                    MOneButtonJDialog.factory(UidSelectJDialog.this, "Policy Manager", "There was a problem refreshing Local Directory users.  Please check your Local Directory settings and try again.",
                                              "Policy Manager Warning", "Policy Manager Warning");
                }
                SwingUtilities.invokeLater( new Runnable(){ public void run(){
                    updateUidModel(allUserEntries);
                }});
            }
            catch(Exception e){
                try{ Util.handleExceptionWithRestart("Error doing refresh", e); }
                catch(Exception f){
                    Util.handleExceptionNoRestart("Error doing refresh", f);
                    MOneButtonJDialog.factory(UidSelectJDialog.this, "Policy Manager", "There was a problem refreshing.  Please try again.",
                                              "Policy Manager Warning", "Policy Manager Warning");
                }
            }
            infiniteProgressJComponent.stopLater(1000l);
        }
    }

    class UserEntryWrapper extends JCheckBox implements Comparable<UserEntryWrapper>{
        private UserEntry userEntry;
        public UserEntryWrapper(UserEntry userEntry){
            this.userEntry = userEntry;
            setText(toString());
            setSelected(false);
            setOpaque(false);
        }

        public String toString(){
            if(userEntry==null)
                return ANY_USER;
            String repository;
            switch(userEntry.getStoredIn()){
            case MS_ACTIVE_DIRECTORY : repository = "Active Directory";
                break;
            case LOCAL_DIRECTORY : repository = "Local";
                break;
            default:
            case NONE : repository = "UNKNOWN";
                break;
            }
            return userEntry.getUID() + " (" + repository + ")";
        }
        public String getUID(){
            if(userEntry!=null)
                return userEntry.getUID();
            else
                return ANY_USER;
        }
        public UserEntry getUserEntry(){
            return userEntry;
        }

        public boolean equals(Object obj){
            if( ! (obj instanceof UserEntryWrapper) )
                return false;
            else if(userEntry==null)
                return false;
            UserEntry other = ((UserEntryWrapper) obj).getUserEntry();
            return getUserEntry().equals(other);
        }

        public int compareTo(UserEntryWrapper userEntryWrapper){
            if(userEntry==null)
                return -1;
            else if(userEntryWrapper.userEntry==null)
                return 1;
            switch(userEntry.getStoredIn().compareTo(userEntryWrapper.userEntry.getStoredIn())){
            case -1 :
                return -1;
            case 1 :
                return 1;
            default:
            case 0 :
                return userEntry.getUID().compareTo(userEntryWrapper.userEntry.getUID());
            }
        }
    }


    public void windowActivated(java.awt.event.WindowEvent windowEvent) {}
    public void windowClosed(java.awt.event.WindowEvent windowEvent) {}
    public void windowDeactivated(java.awt.event.WindowEvent windowEvent) {}
    public void windowDeiconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowIconified(java.awt.event.WindowEvent windowEvent) {}
    public void windowOpened(java.awt.event.WindowEvent windowEvent) {}


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel backgroundJLabel;
    private javax.swing.ButtonGroup buttonGroup1;
    protected javax.swing.JButton cancelJButton;
    private javax.swing.JPanel dividerJPanel;
    private javax.swing.JLabel iconJLabel;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JLabel labelJLabel;
    protected javax.swing.JLabel messageJLabel;
    protected javax.swing.JButton proceedJButton;
    private javax.swing.JList uidJList;
    private javax.swing.JScrollPane userJScrolPane;
    // End of variables declaration//GEN-END:variables

}
