/**
 * $Id$
 */

package com.untangle.app.wireguard_vpn;

// import java.io.BufferedReader;
// import java.io.BufferedWriter;
// import java.io.FileReader;
// import java.io.FileWriter;
// import java.io.File;
// import java.net.InetAddress;
// import java.net.UnknownHostException;
// import java.util.Date;
// import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.ExecManagerResult;
// import com.untangle.uvm.app.IPMaskedAddress;
// import com.untangle.uvm.network.NetworkSettings;
// import com.untangle.uvm.network.InterfaceSettings;

/**
 * This class has all the logic for "managing" the WireguardVpn daemon. This includes
 * writing all the server and client config files and starting/stopping the
 * daemon
 * 
 * @author mahotz
 * 
 */
public class WireguardVpnManager
{
    private static final String WireguardApp = "/usr/bin/wg";

    private final Logger logger = Logger.getLogger(this.getClass());
    private final WireguardVpnApp app;

    /**
     * Constructor
     * 
     * @param app
     *        The Wireguard Vpn application
     */
    protected WireguardVpnManager(WireguardVpnApp app)
    {
        this.app = app;
    }

    /**
     * Generate private key from wireguard app.
     * 
     * @return String of generated private key.
     */
    public String createPrivateKey()
    {
        return wireguardCommand(WireguardApp + " genkey");
    }

    /**
     * Derive public key from private key.
     *
     * @param privateKey private key to derive public key from
     * @return String of derived public key.
     */
    public String getPublicKey(String privateKey)
    {
        return wireguardCommand("echo " + privateKey + " | " + WireguardApp + " pubkey");
    }

    /**
     * Call wireguard and get output
     * @param command String of command to pass to wg
     * @return String of result of call to wg.
     */
    private String wireguardCommand(String command)
    {
        String wireguardResult = "";
        ExecManagerResult result = UvmContextFactory.context().execManager().exec(command);
        try {
            wireguardResult = result.getOutput();
        } catch (Exception e) {
        }
        return wireguardResult;
    }

}
