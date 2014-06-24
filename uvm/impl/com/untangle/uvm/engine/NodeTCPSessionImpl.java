/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.untangle.jvector.Crumb;
import com.untangle.jvector.DataCrumb;
import com.untangle.jvector.IncomingSocketQueue;
import com.untangle.jvector.OutgoingSocketQueue;
import com.untangle.jvector.ResetCrumb;
import com.untangle.jvector.ShutdownCrumb;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.event.IPStreamer;
import com.untangle.uvm.vnet.event.TCPChunkEvent;
import com.untangle.uvm.vnet.event.TCPSessionEvent;
import com.untangle.uvm.vnet.event.TCPStreamer;

/**
 * This is the primary implementation class for TCP live sessions.
 */
public class NodeTCPSessionImpl extends NodeSessionImpl implements NodeTCPSession
{
    protected static final ByteBuffer SHUTDOWN_COOKIE_BUF = ByteBuffer.allocate(1);

    private static final ByteBuffer EMPTY_BUF = ByteBuffer.allocate(0);
    
    private static final String TEMP_FILE_KEY = "temp_file_attachemnt_key";

    private final String logPrefix;

    protected long[] readLimit;
    protected long[] readBufferSize;
    protected boolean[] lineBuffering = new boolean[] { false, false };
    protected ByteBuffer[] readBuf = new ByteBuffer[] { null, null };

    protected NodeTCPSessionImpl( Dispatcher disp, SessionEvent sessionEvent, int clientReadBufferSize, int serverReadBufferSize, IPNewSessionRequestImpl request )
    {
        super( disp, sessionEvent, request );

        logPrefix = "TCP" + id();

        if (clientReadBufferSize < 2 || clientReadBufferSize > TCP_MAX_CHUNK_SIZE)
            throw new IllegalArgumentException("Illegal maximum client read bufferSize: " + clientReadBufferSize);
        if (serverReadBufferSize < 2 || serverReadBufferSize > TCP_MAX_CHUNK_SIZE)
            throw new IllegalArgumentException("Illegal maximum server read bufferSize: " + serverReadBufferSize);
        this.readBufferSize = new long[] { clientReadBufferSize, serverReadBufferSize };
        this.readLimit = new long[] { clientReadBufferSize, serverReadBufferSize };

        PipelineConnectorImpl pipelineConnector = disp.pipelineConnector();
    }

    public int serverReadBufferSize()
    {
        return (int)readBufferSize[SERVER];
    }

    public void serverReadBufferSize(int numBytes)
    {
        if (numBytes < 2 || numBytes > TCP_MAX_CHUNK_SIZE)
            throw new IllegalArgumentException("Illegal maximum read bufferSize: " + numBytes);
        readBufferSize[SERVER] = numBytes;
        if (readLimit[SERVER] > numBytes)
            readLimit[SERVER] = numBytes;
    }

    public int clientReadBufferSize()
    {
        return (int)readBufferSize[CLIENT];
    }

    public void clientReadBufferSize(int numBytes)
    {
        if (numBytes < 2 || numBytes > TCP_MAX_CHUNK_SIZE)
            throw new IllegalArgumentException("Illegal maximum read bufferSize: " + numBytes);
        readBufferSize[CLIENT] = numBytes;
        if (readLimit[CLIENT] > numBytes)
            readLimit[CLIENT] = numBytes;
    }

    public long serverReadLimit()
    {
        return readLimit[SERVER];
    }

    public void serverReadLimit(long numBytes)
    {
        if (numBytes > readBufferSize[SERVER])
            numBytes = readBufferSize[SERVER];
        if (numBytes < 1 || numBytes > TCP_MAX_CHUNK_SIZE)
            throw new IllegalArgumentException("Illegal maximum read limit: " + numBytes);
        readLimit[SERVER] = numBytes;
    }

    public long clientReadLimit()
    {
        return readLimit[CLIENT];
    }

    public void clientReadLimit(long numBytes)
    {
        if (numBytes > readBufferSize[CLIENT])
            numBytes = readBufferSize[CLIENT];
        if (numBytes < 1 || numBytes > TCP_MAX_CHUNK_SIZE)
            throw new IllegalArgumentException("Illegal maximum read limit: " + numBytes);
        readLimit[CLIENT] = numBytes;
    }

    public void clientLineBuffering(boolean oneLine)
    {
        lineBuffering[CLIENT] = oneLine;
    }

    public void serverLineBuffering(boolean oneLine)
    {
        lineBuffering[SERVER] = oneLine;
    }

    public void release()
    {
        /**
         * Since we're releasing, make things as fast as possible for any last bits of data
         * that come through.
         */
        clientLineBuffering(false);
        serverLineBuffering(false);
        readBufferSize[CLIENT] = 8192;
        readBufferSize[SERVER] = 8192;
        readLimit[CLIENT] = readBufferSize[CLIENT];
        readLimit[SERVER] = readBufferSize[SERVER];

        super.release();
    }

    public byte clientState()
    {
        if (clientIncomingSocketQueue() == null)
            if (clientOutgoingSocketQueue() == null)
                return NodeSession.CLOSED;
            else
                return NodeSession.HALF_OPEN_OUTPUT;
        else
            if (clientOutgoingSocketQueue() == null)
                return NodeSession.HALF_OPEN_INPUT;
            else
                return NodeSession.OPEN;
    }

    public byte serverState()
    {
        if (serverIncomingSocketQueue() == null)
            if (serverOutgoingSocketQueue() == null)
                return NodeSession.CLOSED;
            else
                return NodeSession.HALF_OPEN_OUTPUT;
        else
            if (serverOutgoingSocketQueue() == null)
                return NodeSession.HALF_OPEN_INPUT;
            else
                return NodeSession.OPEN;
    }

    public void shutdownServer()
    {
        shutdownSide(SERVER, serverOutgoingSocketQueue(), false);
    }

    public void shutdownServer(boolean force)
    {
        shutdownSide(SERVER, serverOutgoingSocketQueue(), force);
    }

    public void shutdownClient()
    {
        shutdownSide(CLIENT, clientOutgoingSocketQueue(), false);
    }

    public void shutdownClient(boolean force)
    {
        shutdownSide(CLIENT, clientOutgoingSocketQueue(), force);
    }

    private void shutdownSide(int side, OutgoingSocketQueue out, boolean force)
    {
        if (out != null) {
            if (crumbs2write[side] != null && !force) {
                // Indicate the need to shutdown
                addCrumb(side, ShutdownCrumb.getInstance(force));
                // we get handled later automatically by tryWrite()
                return;
            }
            Crumb crumb = ShutdownCrumb.getInstance(force);
            boolean success = out.write(crumb);
            assert success;
        }
    }

    public void resetServer()
    {
        // Go ahead and write out the reset.
        OutgoingSocketQueue oursout = serverOutgoingSocketQueue();
        IncomingSocketQueue oursin  = serverIncomingSocketQueue();
        if (oursout != null) {
            Crumb crumb = ResetCrumb.getInstance();
            boolean success = oursout.write(crumb);
            assert success;
        }

        // Reset the incoming socket queue
        if ( oursin != null )
            oursin.reset();

        // Will result in server's outgoing and incoming socket queue being set to null.
    }

    public void resetClient()
    {
        // Go ahead and write out the reset.
        OutgoingSocketQueue ourcout = clientOutgoingSocketQueue();
        IncomingSocketQueue ourcin  = clientIncomingSocketQueue();

        if (ourcout != null) {
            Crumb crumb = ResetCrumb.getInstance();
            boolean success = ourcout.write(crumb);
            assert success;
        }

        if ( ourcin != null )
            ourcin.reset();

        // Will result in client's outgoing and incoming socket queue being set to null 
    }

    public void beginClientStream(TCPStreamer streamer)
    {
        beginStream(CLIENT, streamer);
    }

    public void beginServerStream(TCPStreamer streamer)
    {
        beginStream(SERVER, streamer);
    }

    public void sendDataToServer( ByteBuffer[] bufs2send )
    {
        sendData( SERVER, bufs2send );
    }

    public void sendDataToClient( ByteBuffer[] bufs2send )
    {
        sendData( CLIENT, bufs2send );
    }

    public void sendData( int side, ByteBuffer[] bufs2send )
    {
        if ( bufs2send == null || bufs2send.length == 0 )
            return;
        for (int i = 0; i < bufs2send.length; i++)
            sendData(side, bufs2send[i]);
    }

    public void sendDataToServer( ByteBuffer buf2send )
    {
        sendData( SERVER, buf2send );
    }

    public void sendDataToClient( ByteBuffer buf2send )
    {
        sendData( CLIENT, buf2send );
    }
    
    public void sendData( int side, ByteBuffer buf2send )
    {
        byte[] array;
        int offset = buf2send.position();
        int size = buf2send.remaining();
        if (size <= 0) {
            if (logger.isInfoEnabled())
                // logger.info("ignoring empty send to " + (side == CLIENT ? "client" : "server") + ", pos: " +
                //             buf2send.position() + ", rem: " + buf2send.remaining() + ", ao: " +
                //             buf2send.arrayOffset(), new Exception());
            return;
        }

        if (buf2send.hasArray()) {
            array = buf2send.array();
            offset += buf2send.arrayOffset();
        } else {
            logger.warn("out-of-heap byte buffer, had to copy");
            array = new byte[buf2send.remaining()];
            buf2send.get(array);
            buf2send.position(offset);
            offset = 0;
        }
        DataCrumb crumb = new DataCrumb(array, offset, offset + size);
        addCrumb(side, crumb);
    }
    
    public void setClientBuffer( ByteBuffer buf )
    {
        setBuffer( CLIENT, buf );
    }
    
    public void setServerBuffer( ByteBuffer buf )
    {
        setBuffer( SERVER, buf );
    }

    public void setBuffer( int side, ByteBuffer buf )
    {
        readBuf[side] = buf;
    }
    
    protected void beginStream(int side, TCPStreamer s)
    {
        if (streamer != null) {
            String message = "Already streaming";
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        if (side == CLIENT)
            streamer = new TCPStreamer[] { s, null };
        else
            streamer = new TCPStreamer[] { null, s };
    }

    protected void endStream()
    {
        IPStreamer cs = streamer[CLIENT];
        IPStreamer ss = streamer[SERVER];

        if (cs != null) {
            if (cs.closeWhenDone())
                shutdownClient();
        } else if (ss != null) {
            if (ss.closeWhenDone())
                shutdownServer();
        }
        streamer = null;
    }


    protected boolean isSideDieing(int side, IncomingSocketQueue in)
    {
        return (in.containsReset());
    }

    protected void sideDieing(int side)
        
    {
        sendRSTEvent(side);
    }

    protected void tryWrite(int side, OutgoingSocketQueue out, boolean warnIfUnable)
        
    {
        String sideName = (side == CLIENT ? "client" : "server");
        assert out != null;
        if (out.isFull()) {
            if (warnIfUnable)
                logger.warn("tryWrite to full outgoing queue");
            else
                logger.debug("tryWrite to full outgoing queue");
        } else {
            // Old busted comment:
            // We know it's a data crumb since there can be nothing else
            // enqueued for TCP.
            // New hotness comment:
            // It can be a shutdown crumb as well as a data crumb.
            Crumb crumb2send = getNextCrumb2Send(side);
            assert crumb2send != null;
            int numWritten = sendCrumb(crumb2send, out);

            stats.wroteData(side, numWritten);

            if (logger.isDebugEnabled())
                logger.debug("wrote " + numWritten + " to " + sideName);
        }
    }

    protected void addStreamBuf(int side, IPStreamer ipStreamer)
    {
        TCPStreamer streamer = (TCPStreamer)ipStreamer;

        String sideName = (side == CLIENT ? "client" : "server");

        ByteBuffer buf2send = streamer.nextChunk();
        if (buf2send == null) {
            logger.debug("end of stream");
            endStream();
            return;
        }

        sendData(side, buf2send);

        if (logger.isDebugEnabled())
            logger.debug("streamed " + buf2send.remaining() + " to " + sideName);
    }

    protected void sendWritableEvent(int side)
    {
        TCPSessionEvent wevent = new TCPSessionEvent(pipelineConnector, this);

        if (side == CLIENT)
            dispatcher.dispatchTCPClientWritable(wevent);
        else
            dispatcher.dispatchTCPServerWritable(wevent);
    }

    protected void sendCompleteEvent()
        
    {
        TCPSessionEvent wevent = new TCPSessionEvent(pipelineConnector, this);
        dispatcher.dispatchTCPComplete(wevent);
    }

    protected void sendFINEvent(int side, ByteBuffer existingReadBuf)
    {
        // First give the node a chance to do something...
        ByteBuffer dataBuf = existingReadBuf != null ? existingReadBuf : EMPTY_BUF;
        TCPChunkEvent cevent = new TCPChunkEvent(pipelineConnector, this, dataBuf);
        if (side == CLIENT)
            dispatcher.dispatchTCPClientDataEnd(cevent);
        else
            dispatcher.dispatchTCPServerDataEnd(cevent);

        // Then run the FIN handler.  This will send the FIN along to the other side by default.
        TCPSessionEvent wevent = new TCPSessionEvent(pipelineConnector, this);
        if (side == CLIENT)
            dispatcher.dispatchTCPClientFIN(wevent);
        else
            dispatcher.dispatchTCPServerFIN(wevent);
    }

    protected void sendRSTEvent(int side)
        
    {
        TCPSessionEvent wevent = new TCPSessionEvent(pipelineConnector, this);
        if (side == CLIENT)
            dispatcher.dispatchTCPClientRST(wevent);
        else
            dispatcher.dispatchTCPServerRST(wevent);
    }

    protected void tryRead(int side, IncomingSocketQueue in, boolean warnIfUnable)
        
    {
        tryReadInt(side, in, warnIfUnable);
    }

    // Handles the actual reading from the client
    protected int tryReadInt(int side, IncomingSocketQueue in, boolean warnIfUnable)
    {
        int numRead = 0;
        boolean gotLine = false;
        String sideName = (side == CLIENT ? "client" : "server");

        if (readBuf[side] == null) {
            // Defer the creation of the buffer until we are sure we need it.
        } else if (!readBuf[side].hasRemaining()) {
            // This isn't supposed to happen.
            // We need to kill the session to keep it from spinning.
            logger.error("read with full read buffer (" + readBuf[side].position() + "," + readBuf[side].limit() + "," + readBuf[side].capacity() + ", killing session");
            readBuf[side] = null;
            killSession();
            return numRead;
        }

        boolean lineMode = lineBuffering[side];
        // Currently we have no special handling for \r. XXX

        if (!lineMode || !gotLine) {
            assert in != null;
            if (in.isEmpty()) {
                if (warnIfUnable)
                    logger.warn("tryRead from empty incoming queue");
                else
                    logger.debug("tryRead from empty incoming queue");
                return numRead;
            }

            Crumb crumb = in.peek();
            switch (crumb.type()) {
            case Crumb.TYPE_SHUTDOWN:
                logger.debug("read FIN");
                sendFINEvent(side, readBuf[side]);
                in.read();
                readBuf[side] = null;
                return numRead;
            case Crumb.TYPE_RESET:
            default:
                // Should never happen.
                logger.debug("read crumb " + crumb.type());
                in.read();
                assert false;
                break;
            case Crumb.TYPE_DATA:
                break;
            }
            // Wrap a byte buffer around the data.
            DataCrumb dc = (DataCrumb)crumb;
            byte[] dcdata = dc.data();
            int dclimit = dc.limit();
            int dccap = dcdata.length;
            int dcoffset = dc.offset();
            int dcsize = dclimit - dcoffset;

            if (dcoffset >= dclimit) {
                logger.warn("Zero length TCP crumb read");
                in.read();  // Consume the crumb
                return numRead;
            } else if (readBuf[side] == null && (dcoffset != 0 || dcsize > readLimit[side] || dccap < readLimit[side] || lineMode)) {
                if (logger.isDebugEnabled()) {
                    if (dcoffset != 0)
                        logger.debug("Creating readbuf because dcoffset = " + dcoffset);
                    else if (dcsize > readLimit[side])
                        logger.debug("Creating readbuf because dcsize = " + dcsize + " but readLimit = " + readLimit[side]);
                    else if (dccap < readLimit[side])
                        logger.debug("Creating readbuf because dccap = " + dccap + " but readLimit = " + readLimit[side]);
                    else if (lineMode)
                        logger.debug("Creating readbuf because lineMode");
                }
                readBuf[side] = ByteBuffer.allocate((int)readBufferSize[side]);
                readBuf[side].limit((int)readLimit[side]); //TODO: check the safety of this conversion
            }
            if (readBuf[side] != null) {
                logger.debug("putting into existing readbuf");
                // We have to put the crumb into the buffer, using the overflow if necessary.
                int s = dcsize;
                if (s > readBuf[side].remaining())
                    s = readBuf[side].remaining();
                int i = 0;
                if (lineMode) {
                    // Have to do the copy one char at a time.
                    while (i < s) {
                        byte c = dcdata[dcoffset + i++];
                        numRead++;
                        readBuf[side].put(c);
                        if (c == '\n')
                            break;
                    }
                } else {
                    readBuf[side].put(dcdata, dcoffset, s);
                    i = s;
                    numRead += s;
                }
                if (i < dcsize) {
                    // We have to adjust the crumb and leave it there as an overflow.
                    // 'i' now means 'eolPosition', or 'last char I want in there'
                    dc.offset(dcoffset + i);
                    if (logger.isDebugEnabled())
                        logger.debug("Leaving " + (dcsize - i) + " bytes in the " + sideName +
                                     " incoming queue");
                } else {
                    in.read();  // Consume the crumb
                    if (logger.isDebugEnabled())
                        logger.debug("Removing incoming crumb for " + sideName);
                }
            } else {
                in.read();  // Consume the crumb
                logger.debug("using jvector buf as new readbuf");
                readBuf[side] = ByteBuffer.wrap(dcdata, 0, dcsize);
                readBuf[side].position(dcsize);
                readBuf[side].limit((int)readLimit[side]); //TODO: check the safety of this conversion
                numRead = dcsize;
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("read " + numRead + " size chunk from " + sideName);
        }

        stats.readData(side, numRead);

        // We have received bytes.  Give them to the user.

        // We duplicate the buffer so that the event handler can mess up
        // the position/mark/limit as desired.
        ByteBuffer userBuf = readBuf[side].duplicate();
        userBuf.flip();
        TCPChunkEvent event = new TCPChunkEvent(pipelineConnector, this, userBuf);

        // automatically clear readBuf before calling app
        readBuf[side] = null;
        
        if (side == CLIENT) {
            dispatcher.dispatchTCPClientChunk(event);
        } else {
            dispatcher.dispatchTCPServerChunk(event);
        }

        return numRead;
    }

    @Override
    protected String idForMDC()
    {
        return logPrefix;
    }

    @Override
    protected void closeFinal()
    {
        try {
            TCPSessionEvent wevent = new TCPSessionEvent(pipelineConnector, this);
            dispatcher.dispatchTCPFinalized(wevent);
        } catch (Exception x) {
            logger.warn("Exception in Finalized", x);
        }

        readBuf[CLIENT] = null;
        readBuf[SERVER] = null;
        super.closeFinal();
    }
    
    @Override
    public void killSession()
    {
        cleanupTempFiles();
        super.killSession();
    }
    
    @SuppressWarnings("unchecked")
    public void cleanupTempFiles()
    {
        try {
            Object attachment = globalAttachment(TEMP_FILE_KEY);
            if (attachment != null) {
                for (Object path : (List<String>) attachment) {
                    try {
                        File f = new File((String) path);
                        if (f.exists())
                            f.delete();
                    } catch (Exception e) {
                        logger.error("Could not delete temp file on session finalized!", e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Could not delete temp files on session finalized!", e);
        }
        globalAttach(TEMP_FILE_KEY, null);
    }

    @SuppressWarnings("unchecked")
    public void attachTempFile(String filePath)
    {
        try {
            List<String> attachment = (List<String>) (globalAttachment(TEMP_FILE_KEY));
            if (attachment == null) {
                attachment = new ArrayList<String>();
                globalAttach(TEMP_FILE_KEY, attachment);
            }
            attachment.add(filePath);
        } catch (Exception e) {
            logger.error("Could not attach temp file to session!", e);
        }
    }
}
