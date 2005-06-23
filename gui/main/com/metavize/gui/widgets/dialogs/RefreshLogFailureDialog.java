/*
 * ValidateFailureDialog.java
 *
 * Created on April 27, 2005, 2:03 PM
 */

package com.metavize.gui.widgets.dialogs;


/**
 *
 * @author inieves
 */
final public class RefreshLogFailureDialog extends MOneButtonJDialog {
    
    public RefreshLogFailureDialog(String applianceName) {
        this.setTitle(applianceName + " Warning");
        messageJLabel.setText("<html><center>" + applianceName + " was unable to properly refresh its event log.<br>Please try again later.</center></html>");
        this.setVisible(true);
    }
    
}
