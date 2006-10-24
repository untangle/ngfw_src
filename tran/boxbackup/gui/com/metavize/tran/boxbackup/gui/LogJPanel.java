/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.boxbackup.gui;

import java.util.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;

import com.metavize.gui.util.Util;
import com.metavize.gui.transform.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.mvvm.tran.Transform;
import com.metavize.mvvm.logging.EventRepository;
import com.metavize.mvvm.logging.EventManager;
import com.metavize.mvvm.logging.RepositoryDesc;
import com.metavize.tran.boxbackup.*;

import com.metavize.tran.boxbackup.*;

public class LogJPanel extends MLogTableJPanel {

    public LogJPanel(Transform transform, MTransformControlsJPanel mTransformControlsJPanel){
        super(transform, mTransformControlsJPanel);

        final BoxBackup boxBackup = (BoxBackup)logTransform;

        depthJSlider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent ce) {
                    int v = depthJSlider.getValue();
                    EventManager<BoxBackupEvent> em = boxBackup.getEventManager();
                    em.setLimit(v);
                }
            });

        setTableModel(new LogTableModel());

        EventManager<BoxBackupEvent> eventManager = boxBackup.getEventManager();
        for (RepositoryDesc fd : eventManager.getRepositoryDescs()) {
            queryJComboBox.addItem(fd.getName());
        }

    }

    protected void refreshSettings(){
        BoxBackup boxBackup = (BoxBackup)logTransform;
        EventManager<BoxBackupEvent> em = boxBackup.getEventManager();
        EventRepository<BoxBackupEvent> ef = em.getRepository((String)queryJComboBox.getSelectedItem());
        settings = ef.getEvents();
    }
    
    class LogTableModel extends MSortedTableModel<Object>{
	
	public TableColumnModel getTableColumnModel(){
	    DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
	    //                                 #   min  rsz    edit   remv   desc   typ               def
	    addTableColumn( tableColumnModel,  0,  150, true,  false, false, false, Date.class,   null,  sc.html("timestamp") );
	    addTableColumn( tableColumnModel,  1,  100, true,  false, false, false, String.class, null,  sc.html("action") );
	    addTableColumn( tableColumnModel,  2,  100, true,  false, false, false, String.class, null,  sc.html("result") );
	    addTableColumn( tableColumnModel,  3,  150, true,  false, false, false, String.class, null,  sc.html("details") );
	    return tableColumnModel;
	}
	
	public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {}
	
	public Vector<Vector> generateRows(Object settings){
            List<BoxBackupEvent> requestLogList = (List<BoxBackupEvent>) settings;
            Vector<Vector> allEvents = new Vector<Vector>(requestLogList.size());
            Vector event;

            for( BoxBackupEvent requestLog : requestLogList ){
		event = new Vector(2);
		event.add( requestLog.getTimeStamp() );
		event.add( "backup" );
		event.add( requestLog.isSuccess() ? "success" : "failed" );
		event.add( requestLog.isSuccess() ? "no details" : requestLog.getDetail() );
		allEvents.add( event );
	    }
	    return allEvents;
	}
	
    }       
    
}
