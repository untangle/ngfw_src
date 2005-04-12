/*
 * NetworkJDialog.java
 *
 * Created on December 12, 2004, 1:06 AM
 */

package com.metavize.gui.configuration;

import com.metavize.gui.widgets.configWindow.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.widgets.restartWindow.*;
import com.metavize.gui.util.*;

import java.awt.*;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;

import com.metavize.mvvm.security.*;
import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;

import com.metavize.gui.util.StringConstants;


/**
 *
 * @author  inieves
 */
public class NetworkJDialog extends ConfigJDialog {

    private NetworkingManager networkingManager;
    private NetworkJPanel networkJPanel;

    private Color INVALID_COLOR = Color.PINK;

    public NetworkJDialog( ) {
        super(Util.getMMainJFrame());

        // INIT GENERAL GUI
        networkJPanel = new NetworkJPanel();
        JScrollPane contentJScrollPane = new JScrollPane( networkJPanel );
        contentJScrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
        contentJScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
        this.contentJTabbedPane.setTitleAt(0, "Network Settings");
        this.contentJPanel.add(contentJScrollPane);
        this.setTitle("Network Settings");
        MIN_SIZE = new Dimension(640, 480);
        MAX_SIZE = new Dimension(640, 1200);

        reload();
    }
    

    protected void doSaveJButtonActionPerformed(java.awt.event.ActionEvent evt) {
        save();
    }

    protected void doReloadJButtonActionPerformed(java.awt.event.ActionEvent evt) {
        reload();
    }

    private void save(){
        String tempString1;
        boolean isValid = true;

        boolean dhcpEnabled;
        IPaddr  dhcpHostIPaddr = null;
        IPaddr  dhcpNetmaskIPaddr = null;
        IPaddr  dhcpRouteIPaddr = null;
        IPaddr  dnsPrimaryIPaddr = null;
        IPaddr  dnsSecondaryIPaddr = null;
        boolean externalAdminEnabled;
        boolean externalAdminRestrictEnabled;
        IPaddr  restrictNetworkIPaddr = null;
        IPaddr  restrictNetmaskIPaddr = null;
        boolean sshEnabled;
        boolean internalAdminEnabled;
        boolean tcpWindowEnabled;

        // DHCP
        if( networkJPanel.dhcpDisabledRadioButton.isSelected() ){
            try{
                tempString1 = networkJPanel.dhcpIPaddrJTextField.getText().trim();
                dhcpHostIPaddr = IPaddr.parse( tempString1 );

                if(dhcpHostIPaddr.isEmpty())
                    throw new Exception("cannot use any address");

                networkJPanel.dhcpIPaddrJTextField.setBackground( Color.WHITE );
            }
            catch(Exception e){
                isValid = false;
                networkJPanel.dhcpIPaddrJTextField.setBackground( INVALID_COLOR );
            }

            try{
                tempString1 = networkJPanel.dhcpNetmaskJTextField.getText().trim();
                dhcpNetmaskIPaddr = IPaddr.parse( tempString1 );

                if(dhcpNetmaskIPaddr.isEmpty())
                    throw new Exception("cannot use any address");

                networkJPanel.dhcpNetmaskJTextField.setBackground( Color.WHITE );
            } 
            catch(Exception e){
                isValid = false;
                networkJPanel.dhcpNetmaskJTextField.setBackground( INVALID_COLOR );
            }

            try{
                tempString1 = networkJPanel.dhcpRouteJTextField.getText().trim();
                dhcpRouteIPaddr = IPaddr.parse( tempString1 );
                if( dhcpRouteIPaddr.isEmpty() )
                    throw new Exception("cannot use any address");
                networkJPanel.dhcpRouteJTextField.setBackground( Color.WHITE );
            }
            catch(Exception e){
                isValid = false;
                networkJPanel.dhcpRouteJTextField.setBackground( INVALID_COLOR );
            }

            try{
                tempString1 = networkJPanel.dnsPrimaryJTextField.getText().trim();
                dnsPrimaryIPaddr = IPaddr.parse( tempString1 );
                if( dnsPrimaryIPaddr.isEmpty() )
                    throw new Exception("cannot use any address");
                networkJPanel.dnsPrimaryJTextField.setBackground( Color.WHITE );
            }
            catch(Exception e){
                isValid = false;
                networkJPanel.dnsPrimaryJTextField.setBackground( INVALID_COLOR );
            }

            try{
                tempString1 = networkJPanel.dnsSecondaryJTextField.getText().trim();
                dnsSecondaryIPaddr = IPaddr.parse( tempString1 );
                networkJPanel.dnsSecondaryJTextField.setBackground( Color.WHITE );
            }
            catch(Exception e){
                isValid = false;
                networkJPanel.dnsSecondaryJTextField.setBackground( INVALID_COLOR );
            }
        }
        else{
            networkJPanel.dhcpIPaddrJTextField.setBackground( Color.WHITE );
            networkJPanel.dhcpNetmaskJTextField.setBackground( Color.WHITE );
            networkJPanel.dhcpRouteJTextField.setBackground( Color.WHITE );
            networkJPanel.dnsPrimaryJTextField.setBackground( Color.WHITE );
            networkJPanel.dnsSecondaryJTextField.setBackground( Color.WHITE );    
        }

        // REMOTE RESTRICTION
        networkJPanel.restrictIPaddrJTextField.setBackground( Color.WHITE );
        networkJPanel.restrictNetmaskJTextField.setBackground( Color.WHITE );

        try{
            tempString1 = networkJPanel.restrictIPaddrJTextField.getText().trim();
            restrictNetworkIPaddr = IPaddr.parse( tempString1 );
            if( restrictNetworkIPaddr.isEmpty())
                throw new Exception("cannot use any address");
        }
        catch(Exception e){
            if ( networkJPanel.externalAdminRestrictEnabledRadioButton.isSelected()
                 && networkJPanel.externalAdminEnabledRadioButton.isSelected()) {
                isValid = false;
                networkJPanel.restrictIPaddrJTextField.setBackground( INVALID_COLOR );
            } else {
                restrictNetworkIPaddr = null;
            }
        }

        try {
            tempString1 = networkJPanel.restrictNetmaskJTextField.getText().trim();
            restrictNetmaskIPaddr = IPaddr.parse( tempString1 );
        } catch ( Exception e ) {
            if ( networkJPanel.externalAdminRestrictEnabledRadioButton.isSelected()
                 && networkJPanel.externalAdminEnabledRadioButton.isSelected()) {
                isValid = false;
                networkJPanel.restrictNetmaskJTextField.setBackground( INVALID_COLOR );
            } else {
                restrictNetmaskIPaddr = null;
            }
        }
        
        // SAVE
        if( !isValid )
            return;

        // ASK THE USER IF HE REALLY WANTS TO SAVE SETTINGS
        NetworkProceedJDialog networkProceedJDialog = new NetworkProceedJDialog(Util.getMMainJFrame(), true);
        networkProceedJDialog.setBounds( Util.generateCenteredBounds(NetworkJDialog.this.getBounds(), networkProceedJDialog.getWidth(), networkProceedJDialog.getHeight()) );
        networkProceedJDialog.setVisible(true);
        boolean isProceeding = networkProceedJDialog.isProceeding();
        if( !isProceeding ) 
            return;

        dhcpEnabled = networkJPanel.dhcpEnabledRadioButton.isSelected();
        externalAdminEnabled = networkJPanel.externalAdminEnabledRadioButton.isSelected();
        externalAdminRestrictEnabled = networkJPanel.externalAdminRestrictEnabledRadioButton.isSelected();
        sshEnabled = networkJPanel.sshEnabledRadioButton.isSelected();
        internalAdminEnabled = networkJPanel.internalAdminEnabledRadioButton.isSelected();
        tcpWindowEnabled = networkJPanel.tcpWindowEnabledRadioButton.isSelected();

        NetworkingConfiguration networkingConfiguration = Util.getMvvmContext().networkingManager().get();
        networkingConfiguration.isDhcpEnabled( dhcpEnabled );
        networkingConfiguration.host( dhcpHostIPaddr );
        networkingConfiguration.netmask( dhcpNetmaskIPaddr );
        networkingConfiguration.gateway( dhcpRouteIPaddr );
        networkingConfiguration.dns1( dnsPrimaryIPaddr );
        networkingConfiguration.dns2( dnsSecondaryIPaddr );
        networkingConfiguration.isOutsideAccessEnabled( externalAdminEnabled );
        networkingConfiguration.isOutsideAccessRestricted( externalAdminRestrictEnabled );
        networkingConfiguration.outsideNetwork( restrictNetworkIPaddr );
        networkingConfiguration.outsideNetmask( restrictNetmaskIPaddr );
        networkingConfiguration.isSshEnabled( sshEnabled );
        networkingConfiguration.isInsideInsecureEnabled( internalAdminEnabled );
        networkingConfiguration.isTcpWindowScalingEnabled( tcpWindowEnabled );
        Util.getMvvmContext().networkingManager().set(networkingConfiguration);
        
        (new RestartJDialog(Util.getMMainJFrame(), true)).setVisible(true);
    }

    private void reload(){
        networkingManager = Util.getMvvmContext().networkingManager();
        NetworkingConfiguration networkingConfiguration = networkingManager.get();
                
        if( networkingConfiguration.isDhcpEnabled() )
            networkJPanel.dhcpEnabledRadioButton.doClick();
        else
            networkJPanel.dhcpDisabledRadioButton.doClick();
        
        if( networkingConfiguration.isOutsideAccessRestricted() )
            networkJPanel.externalAdminRestrictEnabledRadioButton.doClick();
        else
            networkJPanel.externalAdminRestrictDisabledRadioButton.doClick();
        
        if( networkingConfiguration.isOutsideAccessEnabled() )
            networkJPanel.externalAdminEnabledRadioButton.doClick();
        else
            networkJPanel.externalAdminDisabledRadioButton.doClick();
        
        if( networkingConfiguration.isSshEnabled() )
            networkJPanel.sshEnabledRadioButton.doClick();
        else
            networkJPanel.sshDisabledRadioButton.doClick();
        
        if( networkingConfiguration.isInsideInsecureEnabled() )
            networkJPanel.internalAdminEnabledRadioButton.doClick();
        else
            networkJPanel.internalAdminDisabledRadioButton.doClick();
        
        if( networkingConfiguration.isTcpWindowScalingEnabled() )
            networkJPanel.tcpWindowEnabledRadioButton.doClick();
        else
            networkJPanel.tcpWindowDisabledRadioButton.doClick();
        
        networkJPanel.dhcpIPaddrJTextField.setText( networkingConfiguration.host().toString() );
        networkJPanel.dhcpNetmaskJTextField.setText( networkingConfiguration.netmask().toString() );
        networkJPanel.dhcpRouteJTextField.setText( networkingConfiguration.gateway().toString() );
        networkJPanel.dnsPrimaryJTextField.setText( networkingConfiguration.dns1().toString() );
        networkJPanel.dnsSecondaryJTextField.setText( networkingConfiguration.dns2().toString() );
        networkJPanel.restrictIPaddrJTextField.setText( networkingConfiguration.outsideNetwork().toString());
        networkJPanel.restrictNetmaskJTextField.setText( networkingConfiguration.outsideNetmask().toString());
	
    }

}
