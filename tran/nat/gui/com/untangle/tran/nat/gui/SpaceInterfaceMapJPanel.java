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
package com.untangle.tran.nat.gui;


import com.untangle.gui.transform.*;
import com.untangle.gui.pipeline.MPipelineJPanel;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.util.*;

import com.untangle.mvvm.tran.*;
import com.untangle.mvvm.networking.*;
import com.untangle.tran.nat.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

public class SpaceInterfaceMapJPanel extends MEditTableJPanel{

    public SpaceInterfaceMapJPanel() {
        super(true, true);
        super.setFillJButtonEnabled( true );
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("");
        super.setDetailsTitle("");
        super.setAddRemoveEnabled(false);
	super.setFillJButtonEnabled(false);
        
        // create actual table model
        InterfaceTableModel interfaceTableModel = new InterfaceTableModel();
        this.setTableModel( interfaceTableModel );
    }
    
class InterfaceTableModel extends MSortedTableModel<Object>{ 
    
    private static final int  T_TW  = Util.TABLE_TOTAL_WIDTH;
    private static final int  C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int  C1_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int  C2_MW = 150;  /* ethernet interface */
    private static final int  C3_MW = 150;  /* net space */
    private static final int  C4_MW = 100;  /* description */
    
    
    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #   min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0,  C0_MW, false, false, true,  false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1,  C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2,  C2_MW, true,  false, false, false, String.class, null, sc.html("map this<br>ethernet <b>interface</b>") );
        addTableColumn( tableColumnModel,  3,  C3_MW, true,  true,  false, false, ComboBoxModel.class, null, sc.html("to this<br>net <b>space</b>") );
        addTableColumn( tableColumnModel,  4,  C4_MW, true,  true,  false, true,  String.class,  null, sc.TITLE_DESCRIPTION );
        addTableColumn( tableColumnModel,  5,  10,    false, false, true,  false, Interface.class, null, "");
        return tableColumnModel;
    }
    
    private class NetworkSpaceWrapper {
	private NetworkSpace networkSpace;
	public NetworkSpaceWrapper(NetworkSpace networkSpace){
	    this.networkSpace = networkSpace;
	}
	public String toString(){ return networkSpace.getName(); }
	public NetworkSpace getNetworkSpace(){ return networkSpace; }
	public boolean equals(Object o){
	    if( !(o instanceof NetworkSpaceWrapper) )
		return false;
	    else if( o == null )
		return false;
	    NetworkSpaceWrapper other = (NetworkSpaceWrapper) o;
	    return other.getNetworkSpace().getBusinessPapers() == networkSpace.getBusinessPapers();
	}
    }
    
    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {        
        List<Interface> elemList = new ArrayList(tableVector.size());
	Interface newElem = null;
        int rowIndex = 0;

        for( Vector rowVector : tableVector ){
	    rowIndex++;
            newElem = (Interface) rowVector.elementAt(5);
	    newElem.setNetworkSpace( ((NetworkSpaceWrapper) ((ComboBoxModel)rowVector.elementAt(3)).getSelectedItem()).getNetworkSpace() );
	    newElem.setDescription( (String) rowVector.elementAt(4) );
            elemList.add(newElem);
        }
        
	// SAVE SETTINGS //////////
	if( !validateOnly ){
	    NetworkSpacesSettings networkSettings = (NetworkSpacesSettings) settings;
	    networkSettings.setInterfaceList(elemList);
	}
    }

    public Vector<Vector> generateRows(Object settings){ 
	NetworkSpacesSettings networkSettings = (NetworkSpacesSettings) settings;
	List<Interface> interfaceList = 
            (List<Interface>) networkSettings.getInterfaceList();
        Vector<Vector> allRows = new Vector<Vector>(interfaceList.size());
	Vector tempRow = null;
        int rowIndex = 0;

        for( Interface intf : interfaceList ){
	    rowIndex++;
	    tempRow = new Vector(6);
	    tempRow.add( super.ROW_SAVED );
	    tempRow.add( rowIndex );
            tempRow.add( intf.getName() );

	    List<NetworkSpaceWrapper> allNetworkSpaceWrappers = new ArrayList<NetworkSpaceWrapper>();
	    for( NetworkSpace networkSpace : networkSettings.getNetworkSpaceList() )
		allNetworkSpaceWrappers.add(new NetworkSpaceWrapper(networkSpace));
	    NetworkSpaceWrapper currentNetworkSpaceWrapper = new NetworkSpaceWrapper(intf.getNetworkSpace());
	    tempRow.add( super.generateComboBoxModel( allNetworkSpaceWrappers, currentNetworkSpaceWrapper) );
            tempRow.add( intf.getDescription() );
	    tempRow.add( intf );
	    allRows.add( tempRow );
        }
        return allRows;
    }
    
    
}

}
