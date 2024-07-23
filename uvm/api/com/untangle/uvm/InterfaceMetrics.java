/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.Serializable;
import java.net.InetAddress;

import com.untangle.uvm.app.IPMaskedAddress;
import com.untangle.uvm.util.ValidSerializable;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * This object represents the current metrics of an interface.
 * This is not a settings object.
 */
@SuppressWarnings("serial")
@ValidSerializable
public class InterfaceMetrics implements Serializable, JSONString
{
    private Integer portId;

    private InetAddress v4Address = null;
    private InetAddress v4Netmask = null;
    private InetAddress v4Gateway = null;
    private InetAddress v4Dns1 = null;
    private InetAddress v4Dns2 = null;
    private Integer v4PrefixLength = null;

    private InetAddress v6Address = null;
    private InetAddress v6Gateway = null;
    private Integer v6PrefixLength = null;

    private String portName;
    private String portMac;
    private String portStatus;
    private String portDuplex;
    private Integer portSpeed;

    private Long rxBytes;
    private Long rxPackets;
    private Long rxErrors;
    private Long rxDrop;
    private Long rxFifo;
    private Long rxFrame;
    private Long rxCompressed;
    private Long rxMulticast;

    private Long txBytes;
    private Long txPackets;
    private Long txErrors;
    private Long txDrop;
    private Long txFifo;
    private Long txCollisions;
    private Long txCarrier;
    private Long txCompressed;

    public InterfaceMetrics() {}

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    public int getPortId( ) { return this.portId; }
    public void setPortId( int argValue ) { this.portId = argValue; }

    public InetAddress getV4Address( ) { return this.v4Address; }
    public void setV4Address( InetAddress newValue ) { this.v4Address = newValue; }

    public InetAddress getV4Netmask( ) { return this.v4Netmask; }
    public void setV4Netmask( InetAddress newValue ) { this.v4Netmask = newValue; }

    public InetAddress getV4Gateway( ) { return this.v4Gateway; }
    public void setV4Gateway( InetAddress newValue ) { this.v4Gateway = newValue; }

    public InetAddress getV4Dns1( ) { return this.v4Dns1; }
    public void setV4Dns1( InetAddress newValue ) { this.v4Dns1 = newValue; }

    public InetAddress getV4Dns2( ) { return this.v4Dns2; }
    public void setV4Dns2( InetAddress newValue ) { this.v4Dns2 = newValue; }

    public Integer getV4PrefixLength( ) { return this.v4PrefixLength; }
    public void setV4PrefixLength( Integer newValue ) { this.v4PrefixLength = newValue; }

    public InetAddress getV6Address( ) { return this.v6Address; }
    public void setV6Address( InetAddress newValue ) { this.v6Address = newValue; }

    public Integer getV6PrefixLength( ) { return this.v6PrefixLength; }
    public void setV6PrefixLength( Integer newValue ) { this.v6PrefixLength = newValue; }

    public InetAddress getV6Gateway( ) { return this.v6Gateway; }
    public void setV6Gateway( InetAddress newValue ) { this.v6Gateway = newValue; }

    public String getPortName( ) { return this.portName; }
    public void setPortName( String argValue ) { this.portName = argValue; }

    public String getPortMac( ) { return this.portMac; }
    public void setPortMac( String argValue ) { this.portMac = argValue; }

    public String getPortStatus( ) { return this.portStatus; }
    public void setPortStatus( String argValue ) { this.portStatus = argValue; }

    public String getPortDuplex( ) { return this.portDuplex; }
    public void setPortDuplex( String argValue ) { this.portDuplex = argValue; }

    public int getPortSpeed( ) { return this.portSpeed; }
    public void setPortSpeed( int argValue ) { this.portSpeed = argValue; }

    public Long getRxBytes( ) { return this.rxBytes; }
    public void setRxBytes( Long argValue ) { this.rxBytes = argValue; }

    public Long getRxPackets( ) { return this.rxPackets; }
    public void setRxPackets( Long argValue ) { this.rxPackets = argValue; }

    public Long getRxErrors( ) { return this.rxErrors; }
    public void setRxErrors( Long argValue ) { this.rxErrors = argValue; }

    public Long getRxDrop( ) { return this.rxDrop; }
    public void setRxDrop( Long argValue ) { this.rxDrop = argValue; }

    public Long getRxFifo( ) { return this.rxFifo; }
    public void setRxFifo( Long argValue ) { this.rxFifo = argValue; }

    public Long getRxFrame( ) { return this.rxFrame; }
    public void setRxFrame( Long argValue ) { this.rxFrame = argValue; }

    public Long getRxCompressed( ) { return this.rxCompressed; }
    public void setRxCompressed( Long argValue ) { this.rxCompressed = argValue; }

    public Long getRxMulticast( ) { return this.rxMulticast; }
    public void setRxMulticast( Long argValue ) { this.rxMulticast = argValue; }

    public Long getTxBytes( ) { return this.txBytes; }
    public void setTxBytes( Long argValue ) { this.txBytes = argValue; }

    public Long getTxPackets( ) { return this.txPackets; }
    public void setTxPackets( Long argValue ) { this.txPackets = argValue; }

    public Long getTxErrors( ) { return this.txErrors; }
    public void setTxErrors( Long argValue ) { this.txErrors = argValue; }

    public Long getTxDrop( ) { return this.txDrop; }
    public void setTxDrop( Long argValue ) { this.txDrop = argValue; }

    public Long getTxFifo( ) { return this.txFifo; }
    public void setTxFifo( Long argValue ) { this.txFifo = argValue; }

    public Long getTxCollisions( ) { return this.txCollisions; }
    public void setTxCollisions( Long argValue ) { this.txCollisions = argValue; }

    public Long getTxCarrier( ) { return this.txCarrier; }
    public void setTxCarrier( Long argValue ) { this.txCarrier = argValue; }

    public Long getTxCompressed( ) { return this.txCompressed; }
    public void setTxCompressed( Long argValue ) { this.txCompressed = argValue; }
}
