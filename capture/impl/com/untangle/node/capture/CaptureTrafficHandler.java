/**
 * $Id: CaptureTrafficHandler.java,v 1.00 2011/12/14 01:02:03 mahotz Exp $
 */
package com.untangle.node.capture; // IMPL

import java.nio.ByteBuffer;
import java.util.List;

import com.untangle.uvm.vnet.NodeIPSession;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.UDPNewSessionRequest;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.event.IPSessionEvent;
import com.untangle.uvm.vnet.event.TCPSessionEvent;
import com.untangle.uvm.vnet.event.UDPSessionEvent;
import com.untangle.uvm.vnet.event.TCPChunkResult;
import com.untangle.uvm.vnet.event.TCPChunkEvent;
import com.untangle.uvm.vnet.event.UDPPacketEvent;
import com.untangle.uvm.vnet.event.IPDataResult;
import com.untangle.uvm.vnet.event.IPDataEvent;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.NodeUDPSession;
import com.untangle.uvm.vnet.NodeSession;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

public class CaptureTrafficHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(getClass());
    private CaptureNodeImpl node = null;

    public CaptureTrafficHandler(CaptureNodeImpl node)
    {
        super(node);
        this.node = node;
    }

///// TCP stuff --------------------------------------------------

    @Override
    public void handleTCPNewSession(TCPSessionEvent event)
    {
        NodeTCPSession sess = event.session();
        CaptureStatus status = new CaptureStatus(sess);
        logger.debug("handleTCPNewSession " + status.toString());
        sess.attach(status);
        super.handleTCPNewSession(event);
    }

    @Override
    public void handleTCPFinalized(TCPSessionEvent event)
    {
        NodeTCPSession sess = event.session();
        CaptureStatus status = (CaptureStatus)sess.attachment();
        logger.debug("handleTCPFinalized " + status.toString());
        sess.attach(null);
        super.handleTCPFinalized(event);
    }

    @Override
    public IPDataResult handleTCPClientChunk(TCPChunkEvent event)
    {
        NodeTCPSession sess = event.session();
        CaptureStatus status = (CaptureStatus)sess.attachment();
        if (sess.getServerPort() == 80) return(ProcessWebRequest(event));
        if (sess.getServerPort() == 443) return(ProcessWebRequest(event));
        return(super.handleTCPClientChunk(event));
    }

    @Override
    public IPDataResult handleTCPServerChunk(TCPChunkEvent event)
    {
        NodeTCPSession sess = event.session();
        CaptureStatus status = (CaptureStatus)sess.attachment();
        return(super.handleTCPServerChunk(event));
    }

///// UDP stuff --------------------------------------------------

    @Override
    public void handleUDPNewSession(UDPSessionEvent event)
    {
        NodeUDPSession sess = event.session();
        CaptureStatus status = new CaptureStatus(sess);
        logger.debug("handleUDPNewSession " + status.toString());
        sess.attach(status);
        super.handleUDPNewSession(event);
    }

    @Override
    public void handleUDPFinalized(UDPSessionEvent event)
    {
        NodeUDPSession sess = event.session();
        CaptureStatus status = (CaptureStatus)sess.attachment();
        logger.debug("handleUDPFinalized " + status.toString());
        sess.attach(null);
        super.handleUDPFinalized(event);
    }

    @Override
    public void handleUDPClientPacket(UDPPacketEvent event)
    {
        NodeUDPSession sess = event.session();
        CaptureStatus status = (CaptureStatus)sess.attachment();
        super.handleUDPClientPacket(event);
    }

    @Override
    public void handleUDPServerPacket(UDPPacketEvent event)
    {
        NodeUDPSession sess = event.session();
        CaptureStatus status = (CaptureStatus)sess.attachment();
        super.handleUDPServerPacket(event);
    }

///// PRIVATE stuff ----------------------------------------------

    private IPDataResult ProcessWebRequest(TCPChunkEvent event)
    {
        NodeTCPSession sess = event.session();
        CaptureStatus status = (CaptureStatus)sess.attachment();
        ByteBuffer chunk = event.data();
        ByteBuffer[] buff = new ByteBuffer[1];
        int head,page,mark,tail,top,end;

            // if new data would overflow client buffer we allocate a new buffer
            // and stuff everything we have thus far into it and then release
            // the session returning all the data we have to the server
            if (status.clientBuffer.remaining() < chunk.limit())
            {
            logger.debug("----- RELEASING OVERFLOW SESSION -----");
            sess.attach(null);
            sess.release();

            status.clientBuffer.flip();
            buff[0] = ByteBuffer.allocate(status.clientBuffer.limit() + chunk.limit());
            buff[0].put(status.clientBuffer);
            buff[0].put(chunk);
            buff[0].flip();
            return(new TCPChunkResult(null,buff,null));
            }

        // add the received data to our session buffer
        status.clientBuffer.put(chunk);
        chunk.rewind();

        // convert the client buffer to a string we can scan
        String orgstr = new String(status.clientBuffer.array(),0,status.clientBuffer.position());

        // see if we have a complete request
        tail = orgstr.indexOf("\r\n\r\n");

            // request not complete so no data is passed either way yet
            if (tail < 0)
            {
            return(IPDataResult.SEND_NOTHING);
            }

        // extract the method from the web request
        head = orgstr.indexOf(' ');
        if (head > 0) status.method = new String(orgstr.substring(0, head)).toUpperCase();

        // extract the target page from the request
        page = head;
        while (Character.isWhitespace(orgstr.charAt(page))) page++;
        mark = orgstr.indexOf(' ', page + 1);
        if ((page > 0) && (mark > 0)) status.pagename = new String(orgstr.substring(page,mark));

        // extract the target host from the request
        String look = "HOST: ";
        top = orgstr.toUpperCase().indexOf(look);
        end = orgstr.indexOf("\r\n",top);
        if ((top >= 0) && (end >= 0)) status.hostname = new String(orgstr.substring(top + look.length(),end));

        logger.debug("METHOD:" + status.method + " HOST:" + status.hostname + " PAGE:" + status.pagename);
        
        String modstr = new String();
        modstr+="HTTP/1.1 302 Moved Temporarily\r\n";
        modstr+="Server: Untangle\r\n";
        modstr+="Expires: Sat, 1 Jan 2000 00:00:00 GMT\r\n";
        modstr+="Cache-Control: no-store, no-cache, must-revalidate, post-check=0, pre-check=0\r\n";
        modstr+="Pragma: no-cache\r\n";
        modstr+="Location: http://172.16.37.1/cpd/index.php?server_name=" + status.hostname + "&method=" + status.method + "&path=" + status.pagename + "\r\n";
        modstr+="\r\n";
        
        logger.debug("REDIRECT=" + modstr);
        
        ByteBuffer modbb = ByteBuffer.wrap(modstr.getBytes(),0,modstr.length());
        buff[0] = modbb;
        if (1 > 0) return(new TCPChunkResult(buff,null,null));

        // nothing gets sent to the original target server and the streamer
        // will handle sending the squid content back to the client
        return(IPDataResult.SEND_NOTHING);
    }
}
