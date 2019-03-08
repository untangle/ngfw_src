/**
 * $Id$
 */

package com.untangle.app.captive_portal;

import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * Class for creating and extracting DNS queries. It's used in the traffic
 * handler for intercepting and responding to queries from clients that have not
 * authenticated.
 * 
 * WARNING: There is currently no error checking, so calling any of the methods
 * before first calling ExtractQuery will cause all kinds of chaos.
 * 
 * @author mahotz
 * 
 */

class DNSPacket
{
    private InetAddress raddr = null;
    private String qname = null;
    private int qtype = 0;
    private int qclass = 0;

    private int query_id = 0;
    private int qr_flag = 0;
    private int op_code = 0;
    private int aa_flag = 0;
    private int tc_flag = 0;
    private int rd_flag = 0;
    private int ra_flag = 0;
    private int zz_flag = 0;
    private int ad_flag = 0;
    private int cd_flag = 0;
    private int re_code = 0;
    private int qd_count = 0;
    private int an_count = 0;
    private int ns_count = 0;
    private int ar_count = 0;

    /**
     * Constructor
     */
    DNSPacket()
    {
    }

    /**
     * Extracts the fields from a DNS query.
     * 
     * @param buffer
     *        The raw DNS query data
     * @param length
     *        The size of the buffer
     */
    void ExtractQuery(byte[] buffer, int length)
    {
        int flags, count, x;

        StringBuffer ss = new StringBuffer(256);
        ByteBuffer bb = ByteBuffer.wrap(buffer, 0, length);

        // extract all the stuff from the query header masking all the high bits
        // since we have to work with int's because java can't do unsigned
        query_id = (bb.getShort() & 0xFFFF);
        flags = (bb.getShort() & 0xFFFF);
        qd_count = (bb.getShort() & 0xFFFF);
        an_count = (bb.getShort() & 0xFFFF);
        ns_count = (bb.getShort() & 0xFFFF);
        ar_count = (bb.getShort() & 0xFFFF);

        // now we extract the individual flags and other bitfield values
        qr_flag = ((flags >> 15) & 1);
        op_code = ((flags >> 14) & 15);
        aa_flag = ((flags >> 10) & 1);
        tc_flag = ((flags >> 9) & 1);
        rd_flag = ((flags >> 8) & 1);
        ra_flag = ((flags >> 7) & 1);
        zz_flag = ((flags >> 6) & 1);
        ad_flag = ((flags >> 5) & 1);
        cd_flag = ((flags >> 4) & 1);
        re_code = ((flags >> 0) & 15);

        // extract the qname from the packet
        for (;;) {
            // get the size of the next label
            count = bb.get();

            // if zero then we are finished
            if (count == 0) break;

            // grab each character of the lable and append to our string
            for (x = 0; x < count; x++) {
                ss.append((char) bb.get());
            }

            // append the dot corresponding to the next label
            ss.append('.');
        }

        // grab the qname string from the buffer we just assembled
        qname = ss.toString();

        // extract the query type and class from the packet
        qtype = (bb.getShort() & 0xFFFF);
        qclass = (bb.getShort() & 0xFFFF);
    }

    /**
     * Creates a DNS response packet using the qname that was previously
     * extracted and the argumented address.
     * 
     * @param address
     *        The address to be used in the response A record
     * @return The raw DNS response packet
     */
    public ByteBuffer GenerateResponse(InetAddress address)
    {
        ByteBuffer bb = ByteBuffer.allocate(256);
        int flags, find, len, x;

        // save the response address we were passed
        raddr = address;

        aa_flag = 0; // turn off the authoritative answer flag
        qr_flag = 1; // set the query/response flag to response
        ra_flag = 1; // set the recursion available flag to one
        re_code = 0; // set the response code to zero
        qd_count = 1; // set the question count
        an_count = 1; // set the answer count

        if (address == null) {
            an_count = 0;
            re_code = 5;
        }

        // assemble the flags value from all the bitfield values
        flags = 0;
        flags |= (qr_flag << 15);
        flags |= (op_code << 14);
        flags |= (aa_flag << 10);
        flags |= (tc_flag << 9);
        flags |= (rd_flag << 8);
        flags |= (ra_flag << 7);
        flags |= (zz_flag << 6);
        flags |= (ad_flag << 5);
        flags |= (cd_flag << 4);
        flags |= (re_code << 0);

        // stuff the query id, flags, and record counts
        bb.putShort((short) query_id);
        bb.putShort((short) flags);
        bb.putShort((short) qd_count);
        bb.putShort((short) an_count);
        bb.putShort((short) ns_count);
        bb.putShort((short) ar_count);

        // stuff the length of the first label
        len = qname.indexOf('.');
        bb.put((byte) len);

        // stuff each character of the qname
        for (x = 0; x < qname.length(); x++) {
            // if we find a dot we stuff the length of the next label
            if (qname.charAt(x) == '.') {
                find = qname.indexOf('.', x + 1);
                if (find < 0) len = 0;
                else len = (find - x - 1);
                bb.put((byte) len);
            }

            // otherwise we stuff the next character of the name
            else {
                bb.put((byte) qname.charAt(x));
            }
        }

        // stuff the query type and class values
        bb.putShort((short) qtype);
        bb.putShort((short) qclass);

        // now we stuff an answer record in the packet but
        // only if we were passed a valid address
        if (address != null) {
            bb.putShort((short) 0xC00C); // pointer to the qname
            bb.putShort((short) 0x0001); // type = A
            bb.putShort((short) 0x0001); // class = IN
            bb.putInt(0x00000001); // TTL = 1 second
            bb.putShort((short) 0x0004); // size of A record data
            bb.put(address.getAddress(), 0, 4); // insert IP address passed to
                                                // us
        }

        // flip the buffer and return
        bb.flip();
        return (bb);
    }

    /**
     * Checks a previously extracted DNS query to see if it seems valid.
     * 
     * @return true if valid, otherwise false
     */
    public boolean isValidDNSQuery()
    {
        if (qtype != 1) return (false); // must be query for A record
        if (qclass != 1) return (false); // most be the IN class
        if (qr_flag != 0) return (false); // must be a query not response
        if (op_code != 0) return (false); // must be standard query
        if (aa_flag != 0) return (false); // AA shouldn't be set in a query
        if (tc_flag != 0) return (false); // TC shouldn't be set in a query
        if (ra_flag != 0) return (false); // RA shouldn't be set in a query
        if (zz_flag != 0) return (false); // Z field should always be zero
        if (re_code != 0) return (false); // response should be zero
        if (qd_count != 1) return (false); // expect a single question
        return (true);
    }

    /**
     * Returns the QNAME from the previously extracted DNS Query.
     * 
     * @return The QNAME
     */
    public String getQname()
    {
        return (qname);
    }

    /**
     * @return A String with details of the query for debug logging
     */
    public String queryString()
    {
        String string = new String();
        string += String.format("QUERY ID:%d ", query_id);
        string += String.format("QR:%d OPCODE:%d AA:%d TC:%d RD:%d RA:%d Z:%d AD:%d CD:%dRCODE:%d ", qr_flag, op_code, aa_flag, tc_flag, rd_flag, ra_flag, zz_flag, ad_flag, cd_flag, re_code);
        string += String.format("QD:%d AN:%d NS:%d AR:%d ", qd_count, an_count, ns_count, ar_count);
        string += String.format("QNAME:%s QTYPE:%d QCLASS:%d ", qname, qtype, qclass);
        return (string);
    }

    /**
     * @return a String with details of the response for debug logging
     */
    public String replyString()
    {
        String string = new String();
        string += String.format("REPLY ID:%d ", query_id);
        if (raddr == null) string += String.format("REFUSED ");
        else string += String.format("ADDR:%s ", raddr.getHostAddress().toString());
        return (string);
    }
}
