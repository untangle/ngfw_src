/**
 * $Id$
 */
package com.untangle.uvm.network.generic;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.network.DeviceStatus.DuplexStatus;

/**
 * Network settings v2.
 */
@SuppressWarnings("serial")
public class InterfaceStatusGeneric implements Serializable, JSONString {

    private String device;

    private boolean connected;
    private boolean offline;
    private boolean wan;

    private String ethDuplex;
    private int ethSpeed;

    private List<String> ip4Addr = new LinkedList<>();
    private InetAddress ip4Gateway;
    private List<String> addressSource = new LinkedList<>();
    
    private List<String> ip6Addr = new LinkedList<>();
    private InetAddress ip6Gateway;
    private List<String> ip6addressSource = new LinkedList<>();

    private List<InetAddress> dnsServers = new LinkedList<>();
    private List<InetAddress> ip6DnsServer = new LinkedList<>();

    private int rxbytes;
    private int rxpkts;
    private int rxerr;
    private int rxdrop;
    private int txbytes;
    private int txpkts;
    private int txerr;
    private int txdrop;

    private String macAddress;
    private String macVendor;


    public String getDevice() { return device; }
    public void setDevice(String device) { this.device = device; }

    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) { this.connected = connected; }

    public boolean isOffline() { return offline; }
    public void setOffline(boolean offline) { this.offline = offline; }

    public boolean isWan() { return wan; }
    public void setWan(boolean wan) { this.wan = wan; }

    public String getEthDuplex() { return ethDuplex; }
    public void setEthDuplex(String ethDuplex) { this.ethDuplex = ethDuplex; }

    public int getEthSpeed() { return ethSpeed; }
    public void setEthSpeed(int ethSpeed) { this.ethSpeed = ethSpeed; }

    public List<String> getIp4Addr() { return ip4Addr; }
    public void setIp4Addr(List<String> ip4Addr) { this.ip4Addr = ip4Addr; }

    public InetAddress getIp4Gateway() { return ip4Gateway; }
    public void setIp4Gateway(InetAddress ip4Gateway) { this.ip4Gateway = ip4Gateway; }

    public List<String> getAddressSource() { return addressSource; }
    public void setAddressSource(List<String> addressSource) { this.addressSource = addressSource; }

    public List<String> getIp6Addr() { return ip6Addr; }
    public void setIp6Addr(List<String> ip6Addr) { this.ip6Addr = ip6Addr; }

    public InetAddress getIp6Gateway() { return ip6Gateway; }
    public void setIp6Gateway(InetAddress ip6Gateway) { this.ip6Gateway = ip6Gateway; }

    public List<String> getIp6addressSource() { return ip6addressSource; }
    public void setIp6addressSource(List<String> ip6addressSource) { this.ip6addressSource = ip6addressSource; }

    public List<InetAddress> getDnsServers() { return dnsServers; }
    public void setDnsServers(List<InetAddress> dnsServers) { this.dnsServers = dnsServers; }

    public int getRxbytes() { return rxbytes; }
    public void setRxbytes(int rxbytes) { this.rxbytes = rxbytes; }

    public int getRxpkts() { return rxpkts; }
    public void setRxpkts(int rxpkts) { this.rxpkts = rxpkts; }

    public int getRxerr() { return rxerr; }
    public void setRxerr(int rxerr) { this.rxerr = rxerr; }

    public int getRxdrop() { return rxdrop; }
    public void setRxdrop(int rxdrop) { this.rxdrop = rxdrop; }

    public int getTxbytes() { return txbytes; }
    public void setTxbytes(int txbytes) { this.txbytes = txbytes; }

    public int getTxpkts() { return txpkts; }
    public void setTxpkts(int txpkts) { this.txpkts = txpkts; }

    public int getTxerr() { return txerr; }
    public void setTxerr(int txerr) { this.txerr = txerr; }

    public int getTxdrop() { return txdrop; }
    public void setTxdrop(int txdrop) { this.txdrop = txdrop; }

    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }
    
    public String getMacVendor() { return macVendor; }
    public void setMacVendor(String macVendor) { this.macVendor = macVendor; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
