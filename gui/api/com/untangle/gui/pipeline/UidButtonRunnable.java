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


package com.untangle.gui.pipeline;

import com.untangle.mvvm.portal.*;
import com.untangle.mvvm.tran.firewall.user.UserMatcherConstants;

import com.untangle.gui.transform.MTransformControlsJPanel;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.util.*;
import java.awt.Window;
import java.awt.event.*;
import java.util.*;
import javax.swing.CellEditor;
import javax.swing.SwingUtilities;

public class UidButtonRunnable implements ButtonRunnable, Comparable<UidButtonRunnable> {

    private List<String> uidList = new Vector<String>();
    private Window topLevelWindow;
    private MConfigJDialog mConfigJDialog;
    private CellEditor cellEditor;
    private boolean valueChanged;

    public UidButtonRunnable(String isEnabled){
			uidList.add(UidSelectJDialog.ANY_USER);
    }

    public String getButtonText(){
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for(String s : uidList){
					if(!first)
							sb.append(", " + s);
					else
							sb.append(s);
					first = false;
			}
			return sb.toString();
	}
    public boolean isEnabled(){ return true; }
    public void setEnabled(boolean isEnabled){ }
    public boolean valueChanged(){ return valueChanged; }
	public void setUid(String s){
        StringTokenizer st = new StringTokenizer(s,UserMatcherConstants.MARKER_SEPERATOR);
			uidList.clear();
			while(st.hasMoreTokens()){
                String ss = st.nextToken();
					uidList.add(ss);
            }
	}
    public void setUidList(List<String> uidList){ this.uidList = uidList; }
    public List<String> getUidList(){ return uidList; }
	public String getUids(){
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for(String s : uidList){
					if(!first)
							sb.append(UserMatcherConstants.MARKER_SEPERATOR + s);
					else
							sb.append(s);
					first = false;
			}
			String outputString = sb.toString();
			return outputString;
	}

    public void setTopLevelWindow(Window topLevelWindow){ this.topLevelWindow = topLevelWindow; }
    public void setCellEditor(CellEditor cellEditor){ this.cellEditor = cellEditor; }

    public void actionPerformed(ActionEvent evt){ run(); }
	
	
    public int compareTo(UidButtonRunnable uidButtonRunnable){
			List<String> thisList = uidList;
			List<String> thatList = uidButtonRunnable.uidList;
			int thisLen = thisList.size();
			int thatLen = thatList.size();
			int minSize = Math.min(thisLen, thatLen);
			for(int i=0; i<minSize; i++){
				if(thisList.get(i).compareTo(thatList.get(i)) != 0)
						return thisList.get(i).compareTo(thatList.get(i));
			}
			if(thisLen==thatLen)
					return 0;
			else if(thisLen > thatLen)
					return 1;
			else
					return -1;
    }
	 
	
    public void run(){
	UidSelectJDialog uidSelectJDialog = UidSelectJDialog.factory(topLevelWindow);
	uidSelectJDialog.setUidList(uidList);
	uidSelectJDialog.setVisible(true);
	List<String> newUidList = uidSelectJDialog.getUidList();
	
	if(Arrays.equals(uidList.toArray(), newUidList.toArray()))
		valueChanged = false;
	else
		valueChanged = true;
	
	if(valueChanged)
	    uidList = newUidList;
	
	SwingUtilities.invokeLater( new Runnable(){ public void run(){
	    cellEditor.stopCellEditing();
	}});
    }
}
