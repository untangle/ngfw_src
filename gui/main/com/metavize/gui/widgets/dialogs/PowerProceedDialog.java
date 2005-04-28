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
final public class PowerProceedDialog extends MTwoButtonJDialog {
    
    public PowerProceedDialog(String applianceName, boolean powerOn) {
        this.setTitle(applianceName + " Warning");
        if(powerOn){
            this.cancelJButton.setText("<html><b>Cancel</b> power on</html>");
            this.proceedJButton.setText("<html><b>Continue</b> power on</html>");
            messageJLabel.setText("<html><center>" + applianceName + " is about to be powered on.<br><br><b>Would you like to proceed?<b></center></html>");
        }
        else{
            this.cancelJButton.setText("<html><b>Cancel</b> power off</html>");
            this.proceedJButton.setText("<html><b>Continue</b> power off</html>");
            messageJLabel.setText("<html><center>" + applianceName + " is about to be powered off.<br><br><b>Would you like to proceed?<b></center></html>");
        }
        
        this.setVisible(true);
    }
    
}
