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

package com.untangle.gui.widgets.dialogs;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;

final public class ValidateFailureDialog extends MOneButtonJDialog {

    public static ValidateFailureDialog factory(Window parentWindow, String applianceName, String componentName, String failureMessage){
        if( parentWindow instanceof Dialog )
            return new ValidateFailureDialog((Dialog)parentWindow, applianceName, componentName, failureMessage);
        else if( parentWindow instanceof Frame )
            return new ValidateFailureDialog((Frame)parentWindow, applianceName, componentName, failureMessage);
        else
            return null;
    }

    private ValidateFailureDialog(Dialog parentDialog, String applianceName, String componentName, String failureMessage) {
        super(parentDialog);
        init(applianceName, componentName, failureMessage);
    }
    private ValidateFailureDialog(Frame parentFrame, String applianceName, String componentName, String failureMessage) {
        super(parentFrame);
        init(applianceName, componentName, failureMessage);
    }

    private void init(String applianceName, String componentName, String failureMessage) {
        if (null == applianceName) applianceName = "";
        applianceName = applianceName.trim();

        if (applianceName.length() > 0) {
            setTitle(applianceName + " Warning");
        } else {
            setTitle("Warning");
        }

        messageJLabel.setText("<html><center>"
                              + (( applianceName.length() > 0 ) ? applianceName + " was u" : "U" )
                              + "nable to save settings in<br>"
                              + componentName
                              + " for the following reason:<br><br><b>"
                              + failureMessage
                              + "</b><br>"
                              + "Please correct this and then save again.</center></html>");
        setVisible(true);
    }

}
