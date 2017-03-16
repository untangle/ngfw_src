/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.untangle.jvector.Crumb;
import com.untangle.jvector.DataCrumb;
import com.untangle.jvector.ObjectCrumb;
import com.untangle.jvector.IncomingSocketQueue;
import com.untangle.jvector.OutgoingSocketQueue;
import com.untangle.jvector.ResetCrumb;
import com.untangle.jvector.ShutdownCrumb;
import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.IPStreamer;
import com.untangle.uvm.vnet.TCPStreamer;

/**
 * This is the primary implementation class for TCP live sessions.
 */
public class AppTCPSessionImpl extends AppSessionImpl implements AppTCPSession
{
    private static final ByteBuffer EMPTY_BUF = ByteBuffer.allocate(0);
    private static final String TEMP_FILE_KEY = "temp_file_attachemnt_key";

    private final String logPrefix;

    protected long[] readLimit;
    protected long[] readBufferSize;
    protected boolean[] lineBuffering = new boolean[] { false, false };
    protected ByteBuffer[] readBuf = new ByteBuffer[] { null, null };

    protected AppTCPSessionImpl( Dispatcher disp, SessionEvent sessionEvent, int clientReadBufferSize, int serverReadBufferSize, IPNewSessionRequestImpl request )
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

    public void serverReadBufferSize( int numBytes )
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

    public void clientReadBufferSize( int numBytes )
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

    public void serverReadLimit( long numBytes )
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

    public void clientReadLimit( long numBytes )
    {
        if (numBytes > readBufferSize[CLIENT])
            numBytes = readBufferSize[CLIENT];
        if (numBytes < 1 || numBytes > TCP_MAX_CHUNK_SIZE)
            throw new IllegalArgumentException("Illegal maximum read limit: " + numBytes);
        readLimit[CLIENT] = numBytes;
    }

    public void clientLineBuffering( boolean oneLine )
    {
        lineBuffering[CLIENT] = oneLine;
    }

    public void serverLineBuffering( boolean oneLine )
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
                return AppSession.CLOSED;
            else
                return AppSession.HALF_OPEN_OUTPUT;
        else
            if (clientOutgoingSocketQueue() == null)
                return AppSession.HALF_OPEN_INPUT;
            else
                return AppSession.OPEN;
    }

    public byte serverState()
    {
        if (serverIncomingSocketQueue() == null)
            if (serverOutgoingSocketQueue() == null)
                return AppSession.CLOSED;
            else
                return AppSession.HALF_OPEN_OUTPUT;
        else
            if (serverOutgoingSocketQueue() == null)
                return AppSession.HALF_OPEN_INPUT;
            else
                return AppSession.OPEN;
    }

    public void shutdownServer()
    {
        shutdownSide(SERVER, serverOutgoingSocketQueue(), false);
    }

    public void shutdownServer( boolean force )
    {
        shutdownSide(SERVER, serverOutgoingSocketQueue(), force);
    }

    public void shutdownClient()
    {
        shutdownSide(CLIENT, clientOutgoingSocketQueue(), false);
    }

    public void shutdownClient( boolean force )
    {
        shutdownSide(CLIENT, clientOutgoingSocketQueue(), force);
    }

    private void shutdownSide( int side, OutgoingSocketQueue out, boolean force )
    {
        if (out != null) {
            if (writeQueue[side] != null && !force) {
                // Indicate the need to shutdown
                addToWriteQueue(side, ShutdownCrumb.getInstance(force));
                // we get handled later automatically by tryWrite()
                return;
            }
            Crumb crumb = ShutdownCrumb.getInstance(force);
            boolean success = out.write(crumb);
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
        }

        if ( ourcin != null )
            ourcin.reset();

        // Will result in client's outgoing and incoming socket queue being set to null 
    }

    public void sendStreamer( int side, TCPStreamer s )
    {
        addToWriteQueue(side, s);
    }

    public void sendStreamerToClient( TCPStreamer streamer )
    {
        sendStreamer( CLIENT, streamer );
    }

    public void sendStreamerToServer( TCPStreamer streamer )
    {
        sendStreamer( SERVER, streamer );
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
        DataCrumb crumb = createDataCrumb( buf2send );
        if ( crumb != null )
            addToWriteQueue(side, crumb);
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
    
    protected boolean isSideDieing( int side, IncomingSocketQueue in )
    {
        return (in.containsReset());
    }

    protected void sideDieing( int side )
    {
        sendRSTEvent(side);
    }

    protected boolean tryWrite( int side, OutgoingSocketQueue out )
    {
        String sideName = (side == CLIENT ? "client" : "server");

        if ( out == null ) {
            logger.error("Invalid arguments");
            return false;
        }

        if (out.isFull()) {
            logger.warn("tryWrite to full outgoing queue");
            return false;
        }
        
        Crumb crumb2send = getNextCrumb2Send(side);
        if ( crumb2send == null )
            return false;
            
        int numWritten = sendCrumb(crumb2send, out);

        if (logger.isDebugEnabled())
            logger.debug("wrote " + numWritten + " to " + sideName);
        return true;
    }

    protected Crumb readStreamer( IPStreamer streamer )
    {
        TCPStreamer tcpStreamer = (TCPStreamer)streamer;

        Object obj = tcpStreamer.nextChunk();

        if ( obj == null )
            return null;
        
        if ( obj instanceof ByteBuffer ) {
            DataCrumb crumb = createDataCrumb( (ByteBuffer) obj );
            return crumb;
        } else {
            ObjectCrumb crumb = new ObjectCrumb( obj );
            return crumb;
        }
    }

    protected void sendWritableEvent( int side )
    {
        if (side == CLIENT)
            dispatcher.dispatchTCPClientWritable( this );
        else
            dispatcher.dispatchTCPServerWritable( this );
    }

    protected void sendCompleteEvent()
    {
        dispatcher.dispatchTCPComplete( this );
    }

    protected void sendFINEvent( int side, ByteBuffer existingReadBuf )
    {
        // First give the node a chance to do something...
        ByteBuffer dataBuf = existingReadBuf != null ? existingReadBuf : EMPTY_BUF;
        if (side == CLIENT)
            dispatcher.dispatchTCPClientDataEnd( this, dataBuf );
        else
            dispatcher.dispatchTCPServerDataEnd( this, dataBuf );

        // Then run the FIN handler.  This will send the FIN along to the other side by default.
        if (side == CLIENT)
            dispatcher.dispatchTCPClientFIN( this );
        else
            dispatcher.dispatchTCPServerFIN( this );
    }

    protected void sendRSTEvent( int side )
    {
        if (side == CLIENT)
            dispatcher.dispatchTCPClientRST( this );
        else
            dispatcher.dispatchTCPServerRST( this );
    }

    protected void handleRead( int side, IncomingSocketQueue in )
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
            return;
        }

        if (in == null) {
            throw new RuntimeException("Invalid arguments");
        }
        
        boolean lineMode = lineBuffering[side];

        if (!lineMode || !gotLine) {
            if (in.isEmpty()) {
                logger.warn("tryRead from empty incoming queue");
                return;
            }

            Crumb crumb = in.peek();
            switch (crumb.type()) {
            case Crumb.TYPE_SHUTDOWN:
                logger.debug("read FIN");
                sendFINEvent(side, readBuf[side]);
                in.read();
                readBuf[side] = null;
                return;
            case Crumb.TYPE_DATA:
                readDataCrumb( side, in );
                return;
            case Crumb.TYPE_OBJECT:
                readObjectCrumb( side, in );
                return;
            case Crumb.TYPE_RESET:
            default:
                // Should never happen.
                logger.debug("read crumb " + crumb.type());
                in.read();
                throw new RuntimeException( "Invalid crumb type" + crumb.type() );
            }
        }

        return;
    }

    private void readObjectCrumb( int side, IncomingSocketQueue in )
    {
        if (in == null) {
            throw new RuntimeException("Invalid arguments");
        }

        String sideName = (side == CLIENT ? "client" : "server");
        Crumb crumb = in.read();  // Consume the crumb
        boolean lineMode = lineBuffering[side];

        if ( ! ( crumb instanceof ObjectCrumb ) ) {
            throw new RuntimeException("Wrong crumb type");
        }
        ObjectCrumb objectCrumb = (ObjectCrumb) crumb;
        
        /**
         * if in lineMode then the app is clearly expecting data
         * this would lead to unexpected results, so just throw an exception.
         *
         * This is commented out because it is sometimes useful to send an object
         * For example if the HTTP casing is in line-buffered mode, and the outer HTTPS
         * casing sends a release token, the HTTP casing needs to be able to receive it
         *
         * As long as there is no data in the current buffer there is no risk of out-of-order
         * transmissions.
         */
        // if ( lineMode ) {
        //     throw new RuntimeException("Object received while in line-mode buffering state.");
        // }

        /**
         * if there is data in the readbuf then the app is expecting data
         * this would lead to unexpected results, so just throw an exception.
         */
        if ( readBuf[side] != null && readBuf[side].position() > 0 ) {
            throw new RuntimeException("Object received while in data is in read buffer.");
        }

        if (side == CLIENT) {
            dispatcher.dispatchTCPClientObject( this, objectCrumb.getObject() );
        } else {
            dispatcher.dispatchTCPServerObject( this, objectCrumb.getObject() );
        }
        
        return; 
    }

    private void readDataCrumb( int side, IncomingSocketQueue in )
    {
        if (in == null) {
            throw new RuntimeException("Invalid arguments");
        }

        int numRead = 0;
        String sideName = (side == CLIENT ? "client" : "server");
        Crumb crumb = in.peek();
        DataCrumb dc = (DataCrumb)crumb;
        byte[] dcdata = dc.data();
        int dclimit = dc.limit();
        int dccap = dcdata.length;
        int dcoffset = dc.offset();
        int dcsize = dclimit - dcoffset;
        boolean lineMode = lineBuffering[side];

        if (dcoffset >= dclimit) {
            logger.warn("Zero length TCP crumb read");
            in.read();  // Consume the crumb
            return;
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
            readBuf[side].limit((int)readLimit[side]);
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

        if (logger.isDebugEnabled()) {
            logger.debug("read " + numRead + " size chunk from " + sideName);
        }

        // We have received bytes.  Give them to the user.

        // We duplicate the buffer so that the event handler can mess up
        // the position/mark/limit as desired.
        ByteBuffer userBuf = readBuf[side].duplicate();
        userBuf.flip();

        // automatically clear readBuf before calling app
        readBuf[side] = null;
        
        if (side == CLIENT) {
            dispatcher.dispatchTCPClientChunk( this, userBuf );
        } else {
            dispatcher.dispatchTCPServerChunk( this, userBuf );
        }

        return;

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
            dispatcher.dispatchTCPFinalized( this );
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

    private DataCrumb createDataCrumb( ByteBuffer buf )
    {
        byte[] array;
        int offset = buf.position();
        int size = buf.remaining();
        if (size <= 0) {
            // already done
            return null;
        }

        if (buf.hasArray()) {
            array = buf.array();
            offset += buf.arrayOffset();
        } else {
            logger.warn("out-of-heap byte buffer, had to copy");
            array = new byte[buf.remaining()];
            buf.get(array);
            buf.position(offset);
            offset = 0;
        }
        DataCrumb crumb = new DataCrumb(array, offset, offset + size);
        return crumb;
    }
                                      
}
