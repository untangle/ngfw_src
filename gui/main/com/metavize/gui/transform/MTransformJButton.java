/*
 * MTransformJButton.java
 *
 * Created on March 22, 2004, 2:30 PM
 */

package com.metavize.gui.transform;

import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.text.*;
import java.util.Hashtable;
import javax.swing.*;
import javax.swing.border.*;

import com.metavize.gui.main.*;
import com.metavize.gui.pipeline.*;
import com.metavize.gui.util.*;
import com.metavize.gui.widgets.MMultilineToolTip;
import com.metavize.mvvm.MackageDesc;
import com.metavize.mvvm.ToolboxManager;

/**
 *
 * @author  Ian Nieves
 */
public class MTransformJButton extends JButton {

    private MackageDesc mackageDesc;


    private GridBagConstraints gridBagConstraints;
    private JProgressBar statusJProgressBar;
    private JLabel statusJLabel;
    private JLabel nameJLabel;
    private JLabel organizationIconJLabel;
    private JLabel descriptionIconJLabel;

    private String toolTipString;


    /** Creates a new instance of MTransformJButton */
    public MTransformJButton(MackageDesc mackageDesc) {

        this.mackageDesc = mackageDesc;

        // INITIAL LAYOUT
        this.setLayout(new GridBagLayout());

        // ORG ICON
        organizationIconJLabel = new JLabel();
        if( mackageDesc.getOrgIcon() != null )
            organizationIconJLabel.setIcon( new javax.swing.ImageIcon(mackageDesc.getOrgIcon()) );
        //organizationIconJLabel.setDisabledIcon(this.orgIcon);
        organizationIconJLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        organizationIconJLabel.setFocusable(false);
        organizationIconJLabel.setPreferredSize(new java.awt.Dimension(42, 42));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 4);
        add(organizationIconJLabel, gridBagConstraints);

        // DESC ICON
        descriptionIconJLabel = new JLabel();
        if( mackageDesc.getDescIcon() != null )
            descriptionIconJLabel.setIcon( new javax.swing.ImageIcon(mackageDesc.getDescIcon()) );
        //descriptionIconJLabel.setDisabledIcon(this.descIcon);
        descriptionIconJLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        descriptionIconJLabel.setFocusable(false);
        descriptionIconJLabel.setPreferredSize(new java.awt.Dimension(42, 42));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(2, 4, 2, 2);
        add(descriptionIconJLabel, gridBagConstraints);

        //DISPLAY NAME
        nameJLabel = new JLabel();
        nameJLabel.setText( "<html><b><center>" + Util.wrapString(mackageDesc.getDisplayName(), 20) + "</center></b></html>");
        nameJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        nameJLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        nameJLabel.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        add(nameJLabel, gridBagConstraints);

        //status progressbar
        statusJProgressBar = new JProgressBar();
        statusJProgressBar.setBorderPainted(false);
        //statusJProgressBar.setVisible(true);
        statusJProgressBar.setVisible(false);
        statusJProgressBar.setStringPainted(false);
        statusJProgressBar.setOpaque(false);
        statusJProgressBar.setIndeterminate(false);
        statusJProgressBar.setValue(0);
        statusJProgressBar.setForeground(new java.awt.Color(68, 91, 255));
        statusJProgressBar.setFont(new java.awt.Font("Dialog", 0, 12));
        statusJProgressBar.setPreferredSize(new java.awt.Dimension(130, 16));
        statusJProgressBar.setMaximumSize(new java.awt.Dimension(130, 16));
        statusJProgressBar.setMinimumSize(new java.awt.Dimension(50, 16));
        //status label
        statusJLabel = new JLabel();
        statusJLabel.setHorizontalAlignment(JLabel.CENTER);
        statusJLabel.setOpaque(false);
        statusJLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        statusJLabel.setPreferredSize(new java.awt.Dimension(130, 16));
        statusJLabel.setMaximumSize(new java.awt.Dimension(130, 16));
        statusJLabel.setMinimumSize(new java.awt.Dimension(50, 16));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        add(statusJLabel, gridBagConstraints);
        add(statusJProgressBar, gridBagConstraints);

        // TOOLTIP
        toolTipString = Util.wrapString( mackageDesc.getLongDescription(), 80);
        this.setTT("Initializing...");

        this.setMargin(new Insets(0,0,0,0));
        this.setFocusPainted(false);
        this.setContentAreaFilled(true);
        this.setOpaque(true);


    }

    public void setTT(String status){
    this.setToolTipText( "<html>" + "<b>Description:</b><br>" + toolTipString + "<br>" + "<b>Status:</b><br>" + status + "<br>" + "</html>");
    }

    public void setMessage(String message){
        if(message == null){
            statusJLabel.setVisible(false);
        }
        else{
            statusJLabel.setText(message);
            statusJLabel.setVisible(true);
        }
    }

    public void setIndeterminate(boolean indeterminate){
        statusJProgressBar.setVisible(indeterminate);
        statusJProgressBar.setIndeterminate(indeterminate);
    }

    public void setEnabled(boolean enabled){
        super.setEnabled(enabled);

        organizationIconJLabel.setEnabled(enabled);
        descriptionIconJLabel.setEnabled(enabled);
        nameJLabel.setEnabled(enabled);
        //statusJProgressBar.setEnabled(enabled);
        //statusJLabel.setEnabled(enabled);

    }

    public void install(){
        if(Util.getIsDemo())
            return;
        Thread installThread = new InstallThread(Util.getMPipelineJPanel());
    }

    public void uninstall(Hashtable storeHashtableX, Hashtable toolboxHashtableX,
                        JPanel storeJPanelX, JPanel toolboxJPanelX,
                        JTabbedPane tabbedPaneX)  {
        if(Util.getIsDemo())
            return;
        UninstallThread uninstallThread = new UninstallThread(Util.getMvvmContext().toolboxManager(), storeHashtableX,
                                                              toolboxHashtableX, storeJPanelX, toolboxJPanelX,
                                                              Util.getMMainJFrame(), tabbedPaneX);
    }

    public void purchase(Hashtable storeHashtableX, Hashtable toolboxHashtableX,
                        JPanel storeJPanelX, JPanel toolboxJPanelX,
                        JTabbedPane tabbedPaneX)  {

        if(Util.getIsDemo())
            return;
        PurchaseThread purchaseThread = new PurchaseThread(Util.getMvvmContext().toolboxManager(), storeHashtableX,
                                                           toolboxHashtableX, storeJPanelX, toolboxJPanelX,
                                                           Util.getMMainJFrame(), tabbedPaneX);
    }


    //    public JToolTip createToolTip(){
    //    return new MMultilineToolTip(300);
    // }

    public MTransformJButton duplicate(){
        return new  MTransformJButton( mackageDesc );
    }

    public String getFullDescription(){
        return new String( mackageDesc.getLongDescription() );
    }

    public String getShortDescription(){
        return new String( mackageDesc.getShortDescription() );
    }

    public String getName(){
        return mackageDesc.getName();
    }


    public String getDisplayName(){
        return mackageDesc.getDisplayName();
    }

    public double getPrice(){
        return mackageDesc.getPrice();
    }





    private class InstallThread extends Thread {

        MTransformJPanel mTransformJPanel;
        MPipelineJPanel mPipelineJPanel;

        public InstallThread(MPipelineJPanel mPipelineJPanel){
        try{
        this.mPipelineJPanel = mPipelineJPanel;
        this.setContextClassLoader( Util.getClassLoader() );
        MTransformJButton.this.setEnabled(false);
        (new Thread(this)).start();
        }
        catch(Exception e){
        e.printStackTrace();
        }
        }

        public void run(){
            MTransformJButton.this.setMessage("(waiting)");
            MTransformJButton.this.setIndeterminate(true);
            MTransformJButton.this.setEnabled(false);

            synchronized( Util.getPipelineSync() ){
                MTransformJButton.this.setMessage("(installing)");
                try{
                    mTransformJPanel = mPipelineJPanel.addTransform(MTransformJButton.this.getName());
                }
                catch(Exception e){
                    try{
                        Util.handleExceptionWithRestart("Error installing transform",  e);
                    }
                    catch(Exception f){
                        statusJLabel.setVisible(true);
                        MTransformJButton.this.setMessage("(Fail install)");
                        MTransformJButton.this.setIndeterminate(false);
                        MTransformJButton.this.setEnabled(true);
            MTransformJButton.this.setTT("Failed install...");
                        Util.handleExceptionNoRestart("Error installing transform",  f);
                    }
                    return;
                }
                MTransformJButton.this.setMessage(null);
                MTransformJButton.this.setIndeterminate(false);
                MTransformJButton.this.setEnabled(false);
        MTransformJButton.this.setTT("Successfully installed...");
                mTransformJPanel.focus();
            }
            return;
        }
    }


    private class PurchaseThread extends Thread {

        ToolboxManager toolboxManager;
        ActionListener[] actionListeners;
        Hashtable storeHashtable;
        Hashtable toolboxHashtable;
        JPanel storeJPanel;
        JPanel toolboxJPanel;
        MMainJFrame parentFrame;
        JTabbedPane tabbedPane;

        public PurchaseThread(ToolboxManager toolboxManagerX, Hashtable storeHashtableX,
                              Hashtable toolboxHashtableX, JPanel storeJPanelX,
                              JPanel toolboxJPanelX, MMainJFrame parentFrameX, JTabbedPane tabbedPaneX){
            toolboxManager = toolboxManagerX;
            storeHashtable = storeHashtableX;
            toolboxHashtable = toolboxHashtableX;
            storeJPanel = storeJPanelX;
            toolboxJPanel = toolboxJPanelX;
            parentFrame = parentFrameX;
            tabbedPane = tabbedPaneX;

        try{
        this.setContextClassLoader( Util.getClassLoader() );
        (new Thread(this)).start();
        }
        catch(Exception e){
        e.printStackTrace();
        }
    }

        public void run() {

        try{
        SwingUtilities.invokeAndWait( new Runnable() {
            public void run() {
            MTransformJButton.this.setMessage("(waiting)");
            MTransformJButton.this.setIndeterminate(true);
            MTransformJButton.this.setEnabled(false);
            } } );
        }catch(Exception e){e.printStackTrace();}

            synchronized( Util.getPipelineSync() ){

        try{
        SwingUtilities.invokeAndWait( new Runnable() {
            public void run() {
                MTransformJButton.this.setMessage("downloading");
                MTransformJButton.this.setIndeterminate(true);
                MTransformJButton.this.setEnabled(false);
            } } );
        }catch(Exception e){e.printStackTrace();}

                try{
                    int dashIndex = MTransformJButton.this.getName().indexOf('-');
                    toolboxManager.install(MTransformJButton.this.getName().substring(0, dashIndex));

            SwingUtilities.invokeAndWait( new Runnable() {
                public void run() {
                MTransformJButton.this.setMessage("downloaded");
                } } );
                }
                catch(Exception e){
                    try{
                        Util.handleExceptionWithRestart("error purchasing transform: " +  MTransformJButton.this.getName(),  e);
                    }
                    catch(Exception f){

            try{
            SwingUtilities.invokeAndWait( new Runnable() {
                public void run() {
                MTransformJButton.this.setMessage("(Fail download)");
                MTransformJButton.this.setIndeterminate(false);
                MTransformJButton.this.setEnabled(true);
                MTransformJButton.this.setTT("Failed download...");
                } } );
            }catch(Exception g){g.printStackTrace();}

            Util.handleExceptionNoRestart("Error purchasing transform",  f);
                        return;
                    }
                }
                actionListeners = MTransformJButton.this.getActionListeners();
                for(int i=0; i<actionListeners.length; i++){
                    MTransformJButton.this.removeActionListener(actionListeners[i]);
                }
        try{
            SwingUtilities.invokeAndWait( new Runnable() {
                public void run() {
                storeHashtable.remove(MTransformJButton.this);
                toolboxHashtable.put(MTransformJButton.this.getName(), MTransformJButton.this);
                storeJPanel.remove(MTransformJButton.this);
                storeJPanel.revalidate();
                storeJPanel.repaint();
                parentFrame.addMTransformJButtonToToolbox(MTransformJButton.this);
                toolboxJPanel.revalidate();
                toolboxJPanel.repaint();
                } } );
        }
        catch(Exception e){
            e.printStackTrace();
        }

        try{
        SwingUtilities.invokeAndWait( new Runnable() {
            public void run() {
                MTransformJButton.this.setMessage(null);
                MTransformJButton.this.setIndeterminate(false);
                MTransformJButton.this.setEnabled(true);
                MTransformJButton.this.setTT("Ready to install...");
            } } );
        }catch(Exception e){e.printStackTrace();}

            }
        }
    }

    private class UninstallThread extends Thread {

        ToolboxManager toolboxManager;
        ActionListener[] actionListeners;
        Hashtable storeHashtable;
        Hashtable toolboxHashtable;
        JPanel storeJPanel;
        JPanel toolboxJPanel;
        MMainJFrame parentFrame;
        JTabbedPane tabbedPane;

        public UninstallThread(ToolboxManager toolboxManagerX, Hashtable storeHashtableX,
                               Hashtable toolboxHashtableX, JPanel storeJPanelX, JPanel toolboxJPanelX,
                               MMainJFrame parentFrameX, JTabbedPane tabbedPaneX){
            toolboxManager = toolboxManagerX;
            storeHashtable = storeHashtableX;
            toolboxHashtable = toolboxHashtableX;
            storeJPanel = storeJPanelX;
            toolboxJPanel = toolboxJPanelX;
            parentFrame = parentFrameX;
            tabbedPane = tabbedPaneX;
        try{
        MTransformJButton.this.setEnabled(false);
        (new Thread(this)).start();
        }
        catch(Exception e){
        e.printStackTrace();
        }
    }

        public void run() {
            MTransformJButton.this.setMessage("(waiting)");
            MTransformJButton.this.setIndeterminate(true);
            MTransformJButton.this.setEnabled(false);
            synchronized( Util.getPipelineSync() ){

                MTransformJButton.this.setMessage("deleting");
                MTransformJButton.this.setIndeterminate(true);
                MTransformJButton.this.setEnabled(false);
                try{
                    int dashIndex = MTransformJButton.this.getName().indexOf('-');
                    toolboxManager.uninstall(MTransformJButton.this.getName().substring(0, dashIndex));
                    MTransformJButton.this.setMessage("deleted");
                }
                catch(Exception e){
                    try{
                        Util.handleExceptionWithRestart("error deleting transform: " +  MTransformJButton.this.getName(),  e);
                    }
                    catch(Exception f){
                        MTransformJButton.this.setMessage("(fail delete)");
                        MTransformJButton.this.setIndeterminate(false);
                        MTransformJButton.this.setEnabled(true);
            MTransformJButton.this.setTT("Failed remove from toolbox...");
                        Util.handleExceptionNoRestart("Error deleting transform",  f);
                        return;
                    }
                }
                actionListeners = MTransformJButton.this.getActionListeners();
                for(int i=0; i<actionListeners.length; i++){
                    MTransformJButton.this.removeActionListener(actionListeners[i]);
                }
                toolboxHashtable.remove(MTransformJButton.this);
                storeHashtable.put(MTransformJButton.this.getName(), MTransformJButton.this);
                toolboxJPanel.remove(MTransformJButton.this);
                toolboxJPanel.revalidate();
                toolboxJPanel.repaint();
                storeJPanel.revalidate();
                storeJPanel.repaint();
                parentFrame.addMTransformJButtonToStore(MTransformJButton.this);
        tabbedPane.repaint();

                MTransformJButton.this.setMessage("(available)");
                MTransformJButton.this.setIndeterminate(false);
                MTransformJButton.this.setEnabled(true);
        MTransformJButton.this.setTT("Available for procurememt...");

            }
        }
    }

}
