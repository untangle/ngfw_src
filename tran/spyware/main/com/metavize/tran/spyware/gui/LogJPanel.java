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

package com.metavize.tran.spyware.gui;

import com.metavize.gui.util.Util;
import com.metavize.gui.transform.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.spyware.*;

import javax.swing.table.*;
import java.util.*;

public class LogJPanel extends MLogTableJPanel {

    private static final String BLOCKED_EVENTS_STRING = "Spyware blocked events";

    public LogJPanel(Transform transform, MTransformControlsJPanel mTransformControlsJPanel){
        super(transform, mTransformControlsJPanel);
	setTableModel(new LogTableModel());
	queryJComboBox.addItem(BLOCKED_EVENTS_STRING);
    }

    protected void refreshSettings(){
	settings = ((Spyware)super.logTransform).getEventLogs(depthJSlider.getValue(),
							      queryJComboBox.getSelectedItem().equals(BLOCKED_EVENTS_STRING));
    }
    
    class LogTableModel extends MSortedTableModel{
	
	public TableColumnModel getTableColumnModel(){
	    DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
	    //                                 #   min  rsz    edit   remv   desc   typ           def
	    addTableColumn( tableColumnModel,  0,  150, true,  false, false, false, Date.class,   null, "timestamp" );
	    addTableColumn( tableColumnModel,  1,  55,  true,  false, false, false, String.class, null, "action" );
	    addTableColumn( tableColumnModel,  2,  165, true,  false, false, false, String.class, null, "client" );
	    addTableColumn( tableColumnModel,  3,  200, true,  false, false, true,  String.class, null, "request" );
	    addTableColumn( tableColumnModel,  4,  100, true,  false, false, false, String.class, null, sc.html("reason for<br>action") );
	    addTableColumn( tableColumnModel,  5,  100, true,  false, false, false, String.class, null, sc.html("direction") );
	    addTableColumn( tableColumnModel,  6,  165, true,  false, false, false, String.class, null, "server" );
	    return tableColumnModel;
	}

	public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {}
	
	public Vector<Vector> generateRows(Object settings){
	    List<SpywareLog> requestLogList = (List<SpywareLog>) settings;
	    Vector<Vector> allEvents = new Vector<Vector>(requestLogList.size());
	    Vector event;
	    
	    for( SpywareLog requestLog : requestLogList ){
		event = new Vector(7);
		event.add( requestLog.getCreateDate() );
		event.add( requestLog.getAction() );
		event.add( requestLog.getClientAddr() + ":" + ((Integer)requestLog.getClientPort()).toString() );
		event.add( requestLog.getLocation() + " : " + requestLog.getIdent() );
		event.add( requestLog.getReason() );
		event.add( requestLog.getDirection().getDirectionName() );
		event.add( requestLog.getServerAddr() + ":" + ((Integer)requestLog.getServerPort()).toString() );
		allEvents.add( event );
	    }
	    
	    return allEvents;
	}
	
	
    }       

}
