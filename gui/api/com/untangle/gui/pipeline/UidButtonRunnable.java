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

import java.awt.Window;
import java.awt.event.*;
import java.util.*;
import javax.swing.CellEditor;
import javax.swing.SwingUtilities;

import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.uvm.portal.*;
import com.untangle.uvm.node.firewall.user.UserMatcherConstants;

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
        StringTokenizer st = new StringTokenizer(s," \t\n\r\f"+UserMatcherConstants.MARKER_SEPERATOR);
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
