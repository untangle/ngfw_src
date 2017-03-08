/**
 * $Id: AddressCalculator.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.node.ipsec_vpn;

import java.util.List;
import java.io.*;

/*
 * Found on the interwebs and modified slightly to meet our needs and
 * to remove unused stuff.  From the original source it seems to have
 * been written by Methias Talamantes for CS56, Winter 12, UCSB
 */

public class AddressCalculator
{
    private int baseIPnumeric;
    private int netmaskNumeric;
    private String baseNetwork;
    private String firstIP;
    private String secondIP;
    private String lastIP;

    public AddressCalculator(String IPinCIDRFormat) throws NumberFormatException
    {
        String[] st = IPinCIDRFormat.split("\\/");
        if (st.length != 2) {
            throw new NumberFormatException("Invalid CIDR format '" + IPinCIDRFormat + "', should be: xx.xx.xx.xx/xx");
        }

        String symbolicIP = st[0];
        String symbolicCIDR = st[1];
        Integer numericCIDR = new Integer(symbolicCIDR);
        if (numericCIDR > 32) {
            throw new NumberFormatException("CIDR can not be greater than 32");
        }

        st = symbolicIP.split("\\.");
        if (st.length != 4) {
            throw new NumberFormatException("Invalid IP address: " + symbolicIP);
        }

        int i = 24;
        baseIPnumeric = 0;

        for (int n = 0; n < st.length; n++) {
            int value = Integer.parseInt(st[n]);
            if (value != (value & 0xff)) {
                throw new NumberFormatException("Invalid IP address: " + symbolicIP);
            }
            baseIPnumeric += value << i;
            i -= 8;
        }

        if (numericCIDR < 8) throw new NumberFormatException("Netmask CIDR can not be less than 8");
        netmaskNumeric = 0xffffffff;
        netmaskNumeric = netmaskNumeric << (32 - numericCIDR);

        int numberOfBits;
        for (numberOfBits = 0; numberOfBits < 32; numberOfBits++) {
            if ((netmaskNumeric << numberOfBits) == 0) break;
        }
        Integer numberOfIPs = 0;
        for (int n = 0; n < (32 - numberOfBits); n++) {
            numberOfIPs = numberOfIPs << 1;
            numberOfIPs = numberOfIPs | 0x01;
        }

        Integer baseIP = baseIPnumeric & netmaskNumeric;
        baseNetwork = (convertNumericIpToSymbolic(baseIP) + "/" + Integer.toString(numberOfBits));
        firstIP = convertNumericIpToSymbolic(baseIP + 1);
        secondIP = convertNumericIpToSymbolic(baseIP + 2);
        lastIP = convertNumericIpToSymbolic(baseIP + numberOfIPs - 1);
    }

    private String convertNumericIpToSymbolic(Integer ip)
    {
        StringBuffer sb = new StringBuffer(15);
        for (int shift = 24; shift > 0; shift -= 8) {
            sb.append(Integer.toString((ip >>> shift) & 0xff));
            sb.append('.');
        }
        sb.append(Integer.toString(ip & 0xff));
        return sb.toString();
    }

    public String getNetmask()
    {
        StringBuffer sb = new StringBuffer(15);
        for (int shift = 24; shift > 0; shift -= 8) {
            sb.append(Integer.toString((netmaskNumeric >>> shift) & 0xff));
            sb.append('.');
        }
        sb.append(Integer.toString(netmaskNumeric & 0xff));
        return sb.toString();
    }

    public Long getNumberOfHosts()
    {
        int numberOfBits;
        for (numberOfBits = 0; numberOfBits < 32; numberOfBits++) {
            if ((netmaskNumeric << numberOfBits) == 0) break;
        }
        Double x = Math.pow(2, (32 - numberOfBits));
        if (x == -1) {
            x = 1D;
        }
        return x.longValue();
    }

    public String getWildcardMask()
    {
        Integer wildcardMask = netmaskNumeric ^ 0xffffffff;
        StringBuffer sb = new StringBuffer(15);
        for (int shift = 24; shift > 0; shift -= 8) {
            sb.append(Integer.toString((wildcardMask >>> shift) & 0xff));
            sb.append('.');
        }
        sb.append(Integer.toString(wildcardMask & 0xff));
        return sb.toString();
    }

    public String getBroadcastAddress()
    {
        if (netmaskNumeric == 0xffffffff) {
            return "0.0.0.0";
        }
        int numberOfBits;
        for (numberOfBits = 0; numberOfBits < 32; numberOfBits++) {
            if ((netmaskNumeric << numberOfBits) == 0) break;
        }
        Integer numberOfIPs = 0;
        for (int n = 0; n < (32 - numberOfBits); n++) {
            numberOfIPs = numberOfIPs << 1;
            numberOfIPs = numberOfIPs | 0x01;
        }
        Integer baseIP = baseIPnumeric & netmaskNumeric;
        Integer ourIP = baseIP + numberOfIPs;
        String ip = convertNumericIpToSymbolic(ourIP);
        return ip;
    }

    public String getBaseNetwork()
    {
        return (baseNetwork);
    }

    public String getFirstIP()
    {
        return (firstIP);
    }

    public String getSecondIP()
    {
        return (secondIP);
    }

    public String getLastIP()
    {
        return (lastIP);
    }

    public String getOffsetIP(int offset)
    {
        Integer baseIP = baseIPnumeric & netmaskNumeric;
        String address = convertNumericIpToSymbolic(baseIP + offset);
        return (address);
    }
}
