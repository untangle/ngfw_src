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
final public class RemoveProceedDialog extends MTwoButtonJDialog {
    
    public RemoveProceedDialog(String applianceName) {
        this.setTitle(applianceName + " Warning");
        this.cancelJButton.setText("<html><b>Cancel</b> remove</html>");
        this.proceedJButton.setText("<html><b>Continue</b> removing</html>");
        messageJLabel.setText("<html><center>" + applianceName + " is about to be removed from the rack.  Its settings will be lost and it will stop processing network traffic.<br><br><b>Would you like to proceed?<b></center></html>");
        this.setVisible(true);
    }
    
}
