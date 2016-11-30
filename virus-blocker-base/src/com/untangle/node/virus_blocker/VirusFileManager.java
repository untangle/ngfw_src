package com.untangle.node.virus_blocker;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;

public class VirusFileManager extends OutputStream
{
    private final Logger logger = Logger.getLogger(VirusFileManager.class);
    private boolean memoryMode = false;

    private File diskFile = null;
    private FileInputStream fileInStream = null;
    private FileOutputStream fileOutStream = null;
    private FileChannel fileInChannel = null;
    private ByteArrayOutputStream dummyStream = null;
    private MessageDigest hashDigest = null;
    private DigestOutputStream hashStream = null;
    private LinkedList<ByteBuffer> memoryBuffer = null;
    private ByteBuffer byteWriter = null;

    VirusFileManager(boolean argMemory, String filePrefix) throws Exception
    {
        memoryMode = argMemory;

        if (memoryMode == true) {
            logger.debug("VIRUS: Using memory buffering mode");
            memoryBuffer = new LinkedList<ByteBuffer>();
            byteWriter = ByteBuffer.allocate(1024);
            dummyStream = new ByteArrayOutputStream();
            hashDigest = MessageDigest.getInstance("MD5");
            hashStream = new DigestOutputStream(dummyStream, hashDigest);
        } else {
            diskFile = File.createTempFile(filePrefix, null);

            logger.debug("VIRUS: Using temporary file: " + diskFile.toString());

            fileInStream = new FileInputStream(diskFile);
            fileInChannel = fileInStream.getChannel();

            fileOutStream = new FileOutputStream(diskFile);
            hashDigest = MessageDigest.getInstance("MD5");
            hashStream = new DigestOutputStream(fileOutStream, hashDigest);
        }
    }

    public int read(ByteBuffer bb)
    {
        // in file mode we read from the disk and return
        if (memoryMode == false) {
            int retval = 0;

            try {
                retval = fileInChannel.read(bb);
            } catch (Exception exn) {
                logger.error("Virus scan read failed: ", exn);
            }
            return (retval);
        }

        /*
         * we are in memory mode so start by making sure anything in byte writer
         * gets pushed into the memory buffer so it can be read
         */
        localFlush();

        // in memory mode we return end of file if the memory list is empty
        if (memoryBuffer.size() == 0) return (-1);

        int total = 0;
        int count = 0;

        // we have data so fill passed buffer until full or until we're empty
        while (bb.remaining() > 0) {
            count = filler(bb);
            if (count <= 0) break;
            total += count;
        }

        return (total);
    }

    private int filler(ByteBuffer bb)
    {
        // grab the first buffer from the list
        ByteBuffer local = memoryBuffer.peek();

        // if we run out of stuff in the list signal end of data
        if (local == null) return (0);

        // figure out how much space is available in each buffer
        int arg = bb.remaining();
        int mem = (local.limit() - local.position());

        /*
         * if the passed buffer has enough space to hold everything in the next
         * list item we copy all the data and remove the item from list
         */
        if (arg >= mem) {
            bb.put(local.array(), local.position(), mem);
            memoryBuffer.remove();
            return (mem);
        }

        /*
         * passed buffer can only hold part of the next list item so we copy as
         * much as we can, adjust position, and leave the item on the list
         */

        bb.put(local.array(), local.position(), arg);
        int cur = local.position();
        local.position(cur + arg);
        return (arg);
    }

    public void write(int b)
    {
        /*
         * If we're not in memory mode pass directly to the file and return
         */
        if (memoryMode == false) {
            try {
                hashStream.write(b);
            } catch (Exception exn) {
                logger.error("Virus scan write file failed: ", exn);
            }
            return;
        }

        /*
         * In memory mode we don't want to fill our list with a bunch of single
         * character objects so we take single characters and buffer them
         * locally until we have enough to do the actual write. First we make
         * sure the local buffer isn't full before we put the new data.
         */
        if (byteWriter.remaining() == 0) {
            localFlush();
        }

        byteWriter.put((byte) b);
    }

    public void write(ByteBuffer bb)
    {
        /*
         * If there is anything in the byte writer buffer we have to flush it
         * first so things don't end up out of order
         */
        localFlush();

        int pos = bb.position();
        int rem = bb.remaining();

        if (memoryMode == true) {

            /*
             * In memory mode we store the passed data in our linked list and we
             * also write to our dummyStream (via hashStream) which allows
             * MessageDigest to calculate the checksum as we stream the file. We
             * then immediately clear the data sent to the dummy stream so it
             * doesn't grow, since we only use it to allow MessageDigest to do
             * its thing.
             */

            ByteBuffer local = ByteBuffer.allocate(rem - pos);
            local.put(bb.array(), pos, rem);
            local.flip();
            memoryBuffer.add(local);
            try {
                hashStream.write(bb.array(), pos, rem);
                dummyStream.reset();
            } catch (Exception exn) {
                logger.error("Virus scan write memory failed: ", exn);
            }

        } else {

            /*
             * In file mode the MessageDigest is associated directly with the
             * actual disk file so we just do a normal write
             */

            try {
                hashStream.write(bb.array(), pos, rem);
            } catch (Exception exn) {
                logger.error("Virus scan write file failed: ", exn);
            }
        }

        bb.position(pos + rem);
    }

    private void localFlush()
    {
        // if the byte writer is null or empty just return
        if (byteWriter == null) return;
        if (byteWriter.position() == 0) return;

        // we have something in the byte writer so we first make a copy
        ByteBuffer local = ByteBuffer.allocate(byteWriter.position());
        byteWriter.flip();
        local.put(byteWriter);
        local.flip();

        // add the data to the memory buffer 
        memoryBuffer.add(local);

        // make sure we also push the byte writer data to the MD5 generator
        try {
            byteWriter.rewind();
            hashStream.write(byteWriter.array(), 0, byteWriter.limit());
            dummyStream.reset();
        } catch (Exception exn) {
            logger.error("Virus scan write memory failed: ", exn);
        }

        byteWriter.clear();
    }

    public void position(int location)
    {
        /*
         * This is only called when the HTTP handler switches to trickle mode to
         * position the input stream at the end of the file. In memory mode we
         * clear our buffer since it has already been streamed to the client.
         */

        if (memoryMode == true) {
            memoryBuffer.clear();
            return;
        }

        try {
            fileInChannel.position(location);
        } catch (Exception exn) {
            logger.error("Virus scan position failed: ", exn);
        }
    }

    public void flush()
    {
        // handle any data hanging around in the byte writer
        localFlush();

        // for memory mode there are no file streams so just return
        if (memoryMode == true) return;

        // for file mode flush the actual file streams 
        try {
            hashStream.flush();
            fileOutStream.flush();
        } catch (Exception exn) {
            logger.error("Virus scan flush failed: ", exn);
        }
    }

    public void close()
    {
        // handle any data hanging around in the byte writer
        localFlush();

        // for memory mode there is nothing to close so just return
        if (memoryMode == true) return;

        // for file mode we close the actual file stream
        try {
            fileOutStream.close();
        } catch (Exception exn) {
            logger.error("Virus scan close failed: ", exn);
        }
    }

    public boolean delete()
    {
        // nothing to delete in memory mode
        if (memoryMode == true) return (true);

        boolean retstat = false;

        try {
            retstat = diskFile.delete();
        } catch (Exception exn) {
            logger.error("Viruse delete file failed: ", exn);
        }

        return (retstat);
    }

    protected int getMemoryCounter()
    {
        int total = 0;

        // calculate the total amount of data stored in our linked list
        for (int i = 0; i < memoryBuffer.size(); i++) {
            ByteBuffer local = memoryBuffer.get(i);
            total += local.remaining();
        }

        return (total);
    }

    protected File getFileObject()
    {
        // return the actual file object we are using which could be null for memory mode
        return (diskFile);
    }

    protected String getTempFileAbsolutePath()
    {
        if (memoryMode == true) return (null);
        return (diskFile.getAbsolutePath());
    }

    protected String getFileDisplayName()
    {
        if (memoryMode == true) return ("Memory_Hashcode_Only");
        return (diskFile.getName());
    }

    protected String getFileHash()
    {
        String fileHash = "00000000000000000000000000000000";
        if (hashDigest == null) return (fileHash);

        // handle any data hanging around in the byte writer        
        localFlush();

        // get the hash from the MessageDigest and close the stream
        try {
            BigInteger val = new BigInteger(1, hashDigest.digest());
            fileHash = String.format("%1$032x", val);
            hashStream.close();
        } catch (IOException exn) {
            logger.warn("Virus close channel failed: ", exn);
        }
        return (fileHash);
    }
}
