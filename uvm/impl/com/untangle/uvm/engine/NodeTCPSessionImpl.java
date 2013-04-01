/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.nio.ByteBuffer;

import com.untangle.jvector.Crumb;
import com.untangle.jvector.DataCrumb;
import com.untangle.jvector.IncomingSocketQueue;
import com.untangle.jvector.OutgoingSocketQueue;
import com.untangle.jvector.ResetCrumb;
import com.untangle.jvector.ShutdownCrumb;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.argon.ArgonIPNewSessionRequest;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.util.MetaEnv;
import com.untangle.uvm.vnet.NodeSessionStats;
import com.untangle.uvm.vnet.NodeIPSession;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.event.IPDataResult;
import com.untangle.uvm.vnet.event.IPStreamer;
import com.untangle.uvm.vnet.event.TCPChunkEvent;
import com.untangle.uvm.vnet.event.TCPChunkResult;
import com.untangle.uvm.vnet.event.TCPSessionEvent;
import com.untangle.uvm.vnet.event.TCPStreamer;
import com.untangle.uvm.node.SessionTuple;

/**
 * This is the primary implementation class for TCP live sessions.
 */
public class NodeTCPSessionImpl extends NodeIPSessionImpl implements NodeTCPSession
{
    protected static final ByteBuffer SHUTDOWN_COOKIE_BUF = ByteBuffer.allocate(1);

    private static final ByteBuffer EMPTY_BUF = ByteBuffer.allocate(0);

    private final String logPrefix;

    protected int[] readLimit;
    protected int[] readBufferSize;
    protected boolean[] lineBuffering = new boolean[] { false, false };
    protected ByteBuffer[] readBuf = new ByteBuffer[] { null, null };

    protected NodeTCPSessionImpl( Dispatcher disp, SessionEvent sessionEvent, int clientReadBufferSize, int serverReadBufferSize, ArgonIPNewSessionRequest request )
    {
        super( disp, sessionEvent, request );

        logPrefix = "TCP" + id();

        if (clientReadBufferSize < 2 || clientReadBufferSize > TCP_MAX_CHUNK_SIZE)
            throw new IllegalArgumentException("Illegal maximum client read bufferSize: " + clientReadBufferSize);
        if (serverReadBufferSize < 2 || serverReadBufferSize > TCP_MAX_CHUNK_SIZE)
            throw new IllegalArgumentException("Illegal maximum server read bufferSize: " + serverReadBufferSize);
        this.readBufferSize = new int[] { clientReadBufferSize, serverReadBufferSize };
        this.readLimit = new int[] { clientReadBufferSize, serverReadBufferSize };

        ArgonConnectorImpl argonConnector = disp.argonConnector();

        logger = argonConnector.sessionLoggerTCP();
    }

    public int serverReadBufferSize()
    {
        return readBufferSize[SERVER];
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
        return readBufferSize[CLIENT];
    }

    public void clientReadBufferSize(int numBytes)
    {
        if (numBytes < 2 || numBytes > TCP_MAX_CHUNK_SIZE)
            throw new IllegalArgumentException("Illegal maximum read bufferSize: " + numBytes);
        readBufferSize[CLIENT] = numBytes;
        if (readLimit[CLIENT] > numBytes)
            readLimit[CLIENT] = numBytes;
    }

    public int serverReadLimit()
    {
        return readLimit[SERVER];
    }

    public void serverReadLimit(int numBytes)
    {
        if (numBytes > readBufferSize[SERVER])
            numBytes = readBufferSize[SERVER];
        if (numBytes < 1 || numBytes > TCP_MAX_CHUNK_SIZE)
            throw new IllegalArgumentException("Illegal maximum read limit: " + numBytes);
        readLimit[SERVER] = numBytes;
    }

    public int clientReadLimit()
    {
        return readLimit[CLIENT];
    }

    public void clientReadLimit(int numBytes)
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
        // Since we're releasing, make things as fast as possible.
        clientLineBuffering(false);
        serverLineBuffering(false);
        readBufferSize[CLIENT] = 8192; // XXX
        readBufferSize[SERVER] = 8192; // XXX
        readLimit[CLIENT] = readBufferSize[CLIENT];
        readLimit[SERVER] = readBufferSize[SERVER];
        super.release();
    }

    public byte clientState()
    {
        if (clientIncomingSocketQueue() == null)
            if (clientOutgoingSocketQueue() == null)
                return NodeIPSession.CLOSED;
            else
                return NodeIPSession.HALF_OPEN_OUTPUT;
        else
            if (clientOutgoingSocketQueue() == null)
                return NodeIPSession.HALF_OPEN_INPUT;
            else
                return NodeIPSession.OPEN;
    }

    public byte serverState()
    {
        if (serverIncomingSocketQueue() == null)
            if (serverOutgoingSocketQueue() == null)
                return NodeIPSession.CLOSED;
            else
                return NodeIPSession.HALF_OPEN_OUTPUT;
        else
            if (serverOutgoingSocketQueue() == null)
                return NodeIPSession.HALF_OPEN_INPUT;
            else
                return NodeIPSession.OPEN;
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

        addBuf(side, buf2send);

        if (logger.isDebugEnabled())
            logger.debug("streamed " + buf2send.remaining() + " to " + sideName);
    }

    private void addBufs(int side, ByteBuffer[] new2send)
    {
        if (new2send == null || new2send.length == 0)
            return;
        for (int i = 0; i < new2send.length; i++)
            addBuf(side, new2send[i]);
    }

    private void addBuf(int side, ByteBuffer buf2send)
    {
        byte[] array;
        int offset = buf2send.position();
        int size = buf2send.remaining();
        if (size <= 0) {
            if (logger.isInfoEnabled())
                logger.info("ignoring empty send to " + (side == CLIENT ? "client" : "server") + ", pos: " +
                            buf2send.position() + ", rem: " + buf2send.remaining() + ", ao: " +
                            buf2send.arrayOffset());
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

    protected void sendWritableEvent(int side)
        
    {
        TCPSessionEvent wevent = new TCPSessionEvent(argonConnector, this);

        IPDataResult result = side == CLIENT ? dispatcher.dispatchTCPClientWritable(wevent)
            : dispatcher.dispatchTCPServerWritable(wevent);
        
        if (result == IPDataResult.SEND_NOTHING)
            // Optimization
            return;
        if (result.readBuffer() != null)
            // Not allowed
            logger.warn("Ignoring readBuffer returned from writable event");
        addBufs(CLIENT, result.bufsToClient());
        addBufs(SERVER, result.bufsToServer());
    }

    protected void sendCompleteEvent()
        
    {
        TCPSessionEvent wevent = new TCPSessionEvent(argonConnector, this);
        dispatcher.dispatchTCPComplete(wevent);
    }

    protected void sendFINEvent(int side, ByteBuffer existingReadBuf)
        
    {
        // First give the node a chance to do something...
        IPDataResult result;
        ByteBuffer dataBuf = existingReadBuf != null ? existingReadBuf : EMPTY_BUF;
        TCPChunkEvent cevent = new TCPChunkEvent(argonConnector, this, dataBuf);
        if (side == CLIENT)
            result = dispatcher.dispatchTCPClientDataEnd(cevent);
        else
            result = dispatcher.dispatchTCPServerDataEnd(cevent);

        if (result != null) {
            if (result.readBuffer() != null)
                // Not allowed
                logger.warn("Ignoring readBuffer returned from FIN event");
            addBufs(CLIENT, result.bufsToClient());
            addBufs(SERVER, result.bufsToServer());
        }

        // Then run the FIN handler.  This will send the FIN along to the other side by default.
        TCPSessionEvent wevent = new TCPSessionEvent(argonConnector, this);
        if (side == CLIENT)
            dispatcher.dispatchTCPClientFIN(wevent);
        else
            dispatcher.dispatchTCPServerFIN(wevent);
    }

    protected void sendRSTEvent(int side)
        
    {
        TCPSessionEvent wevent = new TCPSessionEvent(argonConnector, this);
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
            // This isn't really supposed to happen XXX
            // We need to kill the session to keep it from spinning.
            logger.error("read with full read buffer (" + readBuf[side].position() + "," +
                         readBuf[side].limit() + "," + readBuf[side].capacity() +
                         ", killing session");
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
            } else if (readBuf[side] == null &&
                       (dcoffset != 0 || dcsize > readLimit[side] || dccap < readLimit[side] || lineMode)) {
                if (logger.isDebugEnabled()) {
                    if (dcoffset != 0)
                        logger.debug("Creating readbuf because dcoffset = " + dcoffset);
                    else if (dcsize > readLimit[side])
                        logger.debug("Creating readbuf because dcsize = " + dcsize + " but readLimit = " +
                                     readLimit[side]);
                    else if (dccap < readLimit[side])
                        logger.debug("Creating readbuf because dccap = " + dccap + " but readLimit = " +
                                     readLimit[side]);
                    else if (lineMode)
                        logger.debug("Creating readbuf because lineMode");
                }
                readBuf[side] = ByteBuffer.allocate(readBufferSize[side]);
                readBuf[side].limit(readLimit[side]);
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
                readBuf[side].limit(readLimit[side]);
                numRead = dcsize;
            }
        }

        if (logger.isDebugEnabled())
            logger.debug("read " + numRead + " size chunk from " + sideName);

        stats.readData(side, numRead);

        // We have received bytes.  Give them to the user.

        // We duplicate the buffer so that the event handler can mess up
        // the position/mark/limit as desired.
        ByteBuffer userBuf;
        /*
          if (readOnly())
          userBuf = readBuf[side].asReadOnlyBuffer();
          else
        */
        userBuf = readBuf[side].duplicate();
        userBuf.flip();
        IPDataResult result;
        TCPChunkEvent event = new TCPChunkEvent(argonConnector, this, userBuf);

        if (side == CLIENT)
            result = dispatcher.dispatchTCPClientChunk(event);
        else
            result = dispatcher.dispatchTCPServerChunk(event);
        if (/* readOnly() || */ result == IPDataResult.PASS_THROUGH) {
            readBuf[side].flip();
            // Write it to the other side.
            addBuf(1 - side, readBuf[side]);
            readBuf[side] = null;
        } else if (result == TCPChunkResult.READ_MORE_NO_WRITE) {
            // Check for full here. XX

            // (re)Save the buffer for later.
        } else {
            addBufs(CLIENT, result.bufsToClient());
            addBufs(SERVER, result.bufsToServer());
            readBuf[side] = result.readBuffer();
        }
        // XXX also needs to work if readbuf same as before?

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
            TCPSessionEvent wevent = new TCPSessionEvent(argonConnector, this);
            dispatcher.dispatchTCPFinalized(wevent);
        } catch (Exception x) {
            logger.warn("Exception in Finalized", x);
        }

        readBuf[CLIENT] = null;
        readBuf[SERVER] = null;
        super.closeFinal();
    }
}
