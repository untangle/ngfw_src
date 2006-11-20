/*
 * Copyright (c) 2003-2006 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.gui.install;

import javax.swing.table.*;
import java.util.Vector;
import java.util.List;

import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.MSortedTableModel;
import com.untangle.gui.widgets.editTable.MEditTableJPanel;
import com.untangle.gui.transform.CompoundSettings;
import com.untangle.mvvm.networking.Interface;
import com.untangle.mvvm.NetworkingConfiguration;

import com.untangle.gui.widgets.wizard.*;
import com.untangle.gui.util.Util;
import javax.swing.SwingUtilities;
import javax.swing.JLabel;
import javax.swing.ListSelectionModel;
import java.awt.Color;
import java.awt.Insets;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;

public class InstallDiskJPanel extends MWizardPageJPanel {
    
    private DiskJPanel diskJPanel;
    private DiskListCompoundSettings diskListCompoundSettings;
    private InstallWizard installWizard;

    public InstallDiskJPanel(InstallWizard installWizard) {
	this.installWizard = installWizard;
	diskJPanel = new DiskJPanel();
	diskListCompoundSettings = new DiskListCompoundSettings();
	setLayout(new GridBagLayout());
	GridBagConstraints gridBagConstraints = new GridBagConstraints(0,0,1,1,1d,1d,GridBagConstraints.NORTH,GridBagConstraints.BOTH,new Insets(15,15,100,15),0,0);
	add(diskJPanel, gridBagConstraints);

	JLabel backgroundJLabel = new JLabel();
	backgroundJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/untangle/gui/install/ProductShot.png")));
	gridBagConstraints = new java.awt.GridBagConstraints();
	gridBagConstraints.gridx = 0;
	gridBagConstraints.gridy = 1;
	gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
	gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
	gridBagConstraints.weightx = 1.0;
	add(backgroundJLabel, gridBagConstraints);
    }


    public boolean enteringForwards(){
	try{
	    diskListCompoundSettings.refresh();
	}
	catch(Exception e){
	    Util.handleExceptionNoRestart("Error scanning disks", e);
	}
	diskJPanel.doRefresh(diskListCompoundSettings);
	return true;
    }
    public boolean leavingForwards(){
	int selectedRow = diskJPanel.getJTable().getSelectedRow();
	if(selectedRow > -1){
	    String selectedDisk = (String) diskJPanel.getJTable().getValueAt(selectedRow,0);
	    InstallWizard.setTargetDisk(selectedDisk);
	    return true;
	}
	else{
	    MOneButtonJDialog dialog = MOneButtonJDialog.factory(InstallDiskJPanel.this.getTopLevelAncestor(), "Install Wizard",
								 "You must select a boot disk which you would like to install Untangle onto.",
								 "Install Wizard Message", "Message");
	    return false;
	}
    }
        
    class DiskListCompoundSettings{
	private List<DiskInfo> diskInfoList;
	public DiskListCompoundSettings(){}
	public void save(){}
	public void refresh() throws Exception{
	    Vector<DiskInfo> diskInfoVector = new Vector<DiskInfo>();
	    for(String diskName : SystemStats.getAvailableDisks() ){
		diskInfoVector.add(new DiskInfo(diskName,SystemStats.getDiskGigs(diskName)));
	    }
	    diskInfoList = diskInfoVector;
	}
	public void validate(){}
	public List<DiskInfo> getDiskInfoList(){ return diskInfoList; }
    }

    class DiskInfo{
	private String name;
	private float gigs;
	public DiskInfo(String name, float gigs){
	    this.name = name;
	    this.gigs = gigs;
	}
	public String getName(){ return name; }
	public float getGigs(){ return gigs; }
    }

    class DiskJPanel extends MEditTableJPanel{
	private DiskModel diskModel;
	public DiskJPanel() {
	    super(false, false);
	    super.setFillJButtonEnabled( false );
	    super.setInsets(new Insets(4, 4, 2, 2));
	    super.setTableTitle("");
	    super.setDetailsTitle("");
	    super.setAddRemoveEnabled(false);
	    super.setAuxJPanelEnabled(true);

	    // add a basic description
	    JLabel descriptionJLabel = new JLabel("<html>This table shows the boot disks that were found.  <font color=\"FF0000\">Please choose a disk for the Untangle Platform installation.  Warning, all the data on the boot disk you select will be deleted.</font></html>");
	    descriptionJLabel.setFont(new Font("Default", 0, 12));
	    auxJPanel.setLayout(new BorderLayout());
	    auxJPanel.add(descriptionJLabel);

	    // create actual table model
	    diskModel = new DiskModel();
	    this.setTableModel( diskModel );

	    // make only single row selection
	    getJTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}
	public void doRefresh(DiskListCompoundSettings diskListCompoundSettings){
	    diskModel.doRefresh(diskListCompoundSettings);
	}
    }

    class DiskModel extends MSortedTableModel<DiskListCompoundSettings>{ 
    
	private static final int  T_TW  = Util.TABLE_TOTAL_WIDTH;
	private static final int  C0_MW = Util.STATUS_MIN_WIDTH; /* status */
	private static final int  C1_MW = Util.LINENO_MIN_WIDTH; /* # */
	private static final int  C2_MW = 200;  /* disk name */
	private static final int  C3_MW = 100;  /* disk size */
	
    
    
	public TableColumnModel getTableColumnModel(){
        
	    DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
	    //                                 #   min    rsz    edit   remv   desc   typ            def
	    addTableColumn( tableColumnModel,  0,  C0_MW, false, false, true,  false, String.class,  null, sc.TITLE_STATUS );
	    addTableColumn( tableColumnModel,  1,  C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX );
	    addTableColumn( tableColumnModel,  2,  C2_MW, false, false, false, false, String.class, null, sc.html("disk name") );
	    addTableColumn( tableColumnModel,  3,  C3_MW, false, false, false, false, String.class, null, sc.html("disk size (GB)") );
	    return tableColumnModel;
	}
    
    
	public void generateSettings(DiskListCompoundSettings diskListCompoundSettings, Vector<Vector> tableVector,
				     boolean validateOnly) throws Exception {}


	public Vector<Vector> generateRows(DiskListCompoundSettings diskListCompoundSettings) {
	    List<DiskInfo> diskInfoList = diskListCompoundSettings.getDiskInfoList();
	    Vector<Vector> allRows = new Vector<Vector>(diskInfoList.size());
	    Vector tempRow = null;
	    int rowIndex = 0;

	    for( DiskInfo diskInfo : diskInfoList ){
		rowIndex++;
		tempRow = new Vector(4);
		tempRow.add( super.ROW_SAVED );
		tempRow.add( rowIndex );
		tempRow.add( diskInfo.getName() );
		tempRow.add( diskInfo.getGigs() );
		allRows.add( tempRow );
	    }
	    return allRows;
	}
    
    
    }


}
