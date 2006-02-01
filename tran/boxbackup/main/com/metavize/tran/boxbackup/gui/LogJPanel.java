/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.boxbackup.gui;

import com.metavize.gui.util.Util;
import com.metavize.gui.transform.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.boxbackup.*;

import javax.swing.table.*;
import java.util.*;

public class LogJPanel extends MLogTableJPanel {

    public LogJPanel(Transform transform, MTransformControlsJPanel mTransformControlsJPanel){
        super(transform, mTransformControlsJPanel);
	setTableModel(new LogTableModel());
	queryJComboBox.setVisible(false);
    }

    protected void refreshSettings(){
	// XXX settings = ((BoxBackup)super.logTransform).getLogs(depthJSlider.getValue());
    }
    
    class LogTableModel extends MSortedTableModel{
	
	public TableColumnModel getTableColumnModel(){
	    DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
	    //                                 #   min  rsz    edit   remv   desc   typ               def
	    addTableColumn( tableColumnModel,  0,  150, true,  false, false, false, String.class, null,  "timestamp" );
	    addTableColumn( tableColumnModel,  1,  100, true,  false, false, false, String.class, null,  sc.html("action") );
	    return tableColumnModel;
	}
	
	public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {}
	
	public Vector<Vector> generateRows(Object settings){
	    /* XXX
	    List<ShieldRejectionLogEntry> logList = (List<ShieldRejectionLogEntry>) settings;
	    Vector<Vector> allEvents = new Vector<Vector>(logList.size());
	    Vector event;
	    
	    for ( ShieldRejectionLogEntry log : logList ) {
		event = new Vector(7);
		event.add( Util.getLogDateFormat().format( log.getCreateDate() ));
		event.add( "backup" );
		allEvents.add( event );
	    }
	    return allEvents;
	    */
	    return null;
	}
	
    }       
    
}
