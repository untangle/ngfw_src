/**
 * $Id$
 */
package com.untangle.node.ips;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.PortRange;
import com.untangle.uvm.node.IPMatcher;
import com.untangle.uvm.vnet.Protocol;

public class IpsStringParser
{
    public static final String HOME_IP = "Home"+0xBEEF;
    public static final String EXTERNAL_IP = "External"+0xBEEF;

    private static final Pattern maskPattern = Pattern.compile("\\d\\d");

    public static String[] parseRuleSplit(String rule)
        throws ParseException
    {
        int first = rule.indexOf("(");
        int last = rule.lastIndexOf(")");
        if (first < 0 || last < 0)
            throw new ParseException("Could not split rule: "+rule);
        String parts[] = { rule.substring(0,first).trim(), rule.substring(first+1,last).trim() };

        return parts;
    }

    // Returns null if the rule is to be removed (like an 'ip' rule
    // for instance)
    public static IpsRuleHeader parseHeader(String header, int action)
        throws ParseException
    {
        boolean clientIPFlag = false;
        boolean clientPortFlag = false;
        boolean serverIPFlag = false;
        boolean serverPortFlag = false;

        /* Header should match: prot sourceIP sourcePort -> destIP destPort */
        String tokens[] = header.split(" ");
        if (tokens.length != 6) {
            throw new ParseException("Not a valid String Header:\n" + header);
        }

        /*Objects needed for a IpsRuleHeader constructor*/
        Protocol protocol;
        List<IPMatcher> clientIPList, serverIPList;
        PortRange clientPortRange, serverPortRange;
        boolean direction = parseDirection(tokens[3]);

        /*Parse Protocol*/
        protocol = parseProtocol(tokens[0]);
        if (protocol == null)
            return null;

        /*Parse server and client IP data - this will throw exceptions*/
        clientIPFlag    = parseNegation(tokens[1]);
        tokens[1]       = stripNegation(tokens[1]);
        clientIPList    = parseIPToken(tokens[1]);

        serverIPFlag    = parseNegation(tokens[4]);
        tokens[4]       = stripNegation(tokens[4]);
        serverIPList    = parseIPToken(tokens[4]);

        /*Parse server and client port data - this will not throw exceptions*/
        clientPortFlag  = parseNegation(tokens[2]);
        tokens[2]       = stripNegation(tokens[2]);
        clientPortRange = parsePortToken(tokens[2]);

        serverPortFlag  = parseNegation(tokens[5]);
        tokens[5]       = stripNegation(tokens[5]);
        serverPortRange = parsePortToken(tokens[5]);

        /*So we throw them ourselves*/
        if (clientPortRange == null)
            throw new ParseException("Invalid source port: " + tokens[2]);
        if (serverPortRange == null) {
            throw new ParseException("Invalid destination port: " +tokens[5]);
        }

        /*Build and return the rule header*/
        IpsRuleHeader ruleHeader = IpsRuleHeader.getHeader
            (action, direction, protocol, clientIPList, clientPortRange,
             serverIPList, serverPortRange, clientIPFlag, clientPortFlag,
             serverIPFlag, serverPortFlag);
        return ruleHeader;
    }

    private static Protocol parseProtocol(String protoString)
        throws ParseException
    {
        if (protoString.equalsIgnoreCase("tcp"))
            return Protocol.TCP;
        else if (protoString.equalsIgnoreCase("udp"))
            return Protocol.UDP;
        else if (protoString.equalsIgnoreCase("ip"))
            return null;
        else if (protoString.equalsIgnoreCase("icmp"))
            return null;
        else
            throw new ParseException("Invalid Protocol string: " + protoString);
    }

    private static boolean parseDirection(String direction)
        throws ParseException
    {
        if (direction.equals("<>"))
            return IpsRuleHeader.IS_BIDIRECTIONAL;
        else if (direction.equals("->"))
            return !IpsRuleHeader.IS_BIDIRECTIONAL;
        else
            throw new ParseException("Invalid direction opperator: " + direction);
    }

    private static String stripNegation(String str)
    {
        return str.replaceAll("!","");
    }

    private static boolean parseNegation(String negationString)
    {
        if (negationString.contains("!"))
            return true;
        return false;
    }

    private static List<IPMatcher> parseIPToken(String ipString)
        throws ParseException
    {
        List<IPMatcher> ipList = new ArrayList<IPMatcher>();
        if (ipString.equalsIgnoreCase("any"))
            ipList.add(IPMatcher.getAnyMatcher());
        else {
            ipString = ipString.replaceAll("\\[","");
            ipString = ipString.replaceAll("\\]","");

            String allAddrs[] = ipString.split(",");
            for (int i=0; i < allAddrs.length; i++)
                ipList.add(new IPMatcher(validateMask(allAddrs[i])));
        }
        return ipList;
    }

    private static PortRange parsePortToken(String portString)
    {
        if (portString.equalsIgnoreCase("any")) {
            return PortRange.ANY;
        } else {
            int port  = -1;
            int port2 = -1;
            int index = portString.indexOf(":");
            /**
             * Matches port string style xxxx (no range)
             * */
            if (index == -1) {
                port = Integer.parseInt(portString);
                if (port >= 0)
                    return new PortRange(port, port);
            }
            /**
             * Matches port string style :xxxx (0 to xxxx)
             * */
            else if (index == 0) {
                port = Integer.parseInt(portString.substring(1,portString.length()));
                if (port >= 0 && port <= 65535)
                    return new PortRange(0,port);
            }
            /**
             * Matches port string style xxxx: (xxxx to 65535)
             * */
            else if (index == portString.length() - 1) {
                port = Integer.parseInt(portString.substring(0,index));
                if (port >= 0 && port <= 65535)
                    return new PortRange(port,65535);
            }
            /**
             * Matches port string style xxxx:yyyy (xxxx to yyyy)
             * */
            else {
                port = Integer.parseInt(portString.substring(0,index));
                port2 = Integer.parseInt(portString.substring(index+1,portString.length()));
                if ( port >= 0 && port2 >= 0 && port <= 65535 && port2 <= 65535)
                    return new PortRange(port, port2);
            }
            return null;
        }
    }

    /**
     * This function converts an ip mask in the form of x.x.x.x/24 to
     * an ip type mask, eg x.x.x.x/255.255.255.0 (24 ones followed by
     * 8 zeros) Useful becuase IPMatcher cannot parse the first, but
     * can parse the second.
     **/
    private static String validateMask(String ipAddr)
    {
        String mask[] = ipAddr.split("/");
        if (mask.length != 2)
            return ipAddr;
        Matcher m = maskPattern.matcher(mask[1]);
        if (m.matches()) {
            int maskNum = Integer.parseInt(mask[1]);
            if (maskNum > 32 || maskNum < 0)
                validateMask(mask[0]+"/32");

            long tmp = 0xFFFFFFFF;
            tmp = tmp << (32-maskNum);
            return mask[0]+"/"+longToIPv4String(tmp);
        }
        return mask[0];
    }

    private static String longToIPv4String(long addr)
    {
        String addrString = "";

        for ( int c = 4 ; --c >= 0  ; ) {
            addrString += (int)((addr >> ( 8 * c )) & 0xFF);
            if ( c > 0 )
                addrString += ".";
        }
        return addrString;
    }
}
