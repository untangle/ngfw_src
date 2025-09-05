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

    private int interfaceId;
    private String device;

    private boolean connected;
    private boolean offline;
    private boolean wan;

    private String ethDuplex;
    private long ethSpeed;

    private List<String> ip4Addr;
    private InetAddress v4Address;
    private InetAddress ip4Gateway;
    private List<String> addressSource;
    
    private List<String> ip6Addr;
    private InetAddress ip6Gateway;
    private List<String> ip6addressSource;

    private List<InetAddress> dnsServers;
    private List<InetAddress> ip6DnsServer;

    private long rxbytes;
    private long rxpkts;
    private long rxerr;
    private long rxdrop;
    private long txbytes;
    private long txpkts;
    private long txerr;
    private long txdrop;

    private String macAddress;
    private String macVendor;

    public int getInterfaceId() { return interfaceId; }
    public void setInterfaceId(int interfaceId) { this.interfaceId = interfaceId; }

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

    public long getEthSpeed() { return ethSpeed; }
    public void setEthSpeed(long ethSpeed) { this.ethSpeed = ethSpeed; }

    public List<String> getIp4Addr() { return ip4Addr; }
    public void setIp4Addr(List<String> ip4Addr) { this.ip4Addr = ip4Addr; }

    public InetAddress getV4Address() { return v4Address; }
    public void setV4Address(InetAddress v4Address) { this.v4Address = v4Address; }

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

    public long getRxbytes() { return rxbytes; }
    public void setRxbytes(long rxbytes) { this.rxbytes = rxbytes; }

    public long getRxpkts() { return rxpkts; }
    public void setRxpkts(long rxpkts) { this.rxpkts = rxpkts; }

    public long getRxerr() { return rxerr; }
    public void setRxerr(long rxerr) { this.rxerr = rxerr; }

    public long getRxdrop() { return rxdrop; }
    public void setRxdrop(long rxdrop) { this.rxdrop = rxdrop; }

    public long getTxbytes() { return txbytes; }
    public void setTxbytes(long txbytes) { this.txbytes = txbytes; }

    public long getTxpkts() { return txpkts; }
    public void setTxpkts(long txpkts) { this.txpkts = txpkts; }

    public long getTxerr() { return txerr; }
    public void setTxerr(long txerr) { this.txerr = txerr; }

    public long getTxdrop() { return txdrop; }
    public void setTxdrop(long txdrop) { this.txdrop = txdrop; }

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
