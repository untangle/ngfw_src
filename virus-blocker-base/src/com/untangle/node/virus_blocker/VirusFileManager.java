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
    private ByteArrayOutputStream arrayStream = null;
    private MessageDigest hashDigest = null;
    private DigestOutputStream hashStream = null;
    private LinkedList<ByteBuffer> memoryBuffer = null;
    private ByteBuffer byteWriter = null;

    VirusFileManager(boolean argMemory, String filePrefix) throws Exception
    {
        memoryMode = argMemory;
        byteWriter = ByteBuffer.allocate(1024);

        if (memoryMode == true) {
            logger.debug("VIRUS: Using memory buffering mode");
            memoryBuffer = new LinkedList<ByteBuffer>();
            arrayStream = new ByteArrayOutputStream();
            hashDigest = MessageDigest.getInstance("MD5");
            hashStream = new DigestOutputStream(arrayStream, hashDigest);
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
        if (memoryMode == false) {
            int retval = 0;

            try {
                retval = fileInChannel.read(bb);
            } catch (Exception exn) {
                logger.error("Virus scan read failed: ", exn);
            }
            return (retval);
        }

        // return end of file if the list is empty
        if (memoryBuffer.size() == 0) return (-1);

        int total = 0;
        int count = 0;

        // fill the passed buffer from the memory buffer
        while (bb.remaining() > 0) {
            count = filler(bb);
            if (count <= 0) break;
            total += count;
        }

        return (total);
    }

    private int filler(ByteBuffer bb)
    {
        ByteBuffer local = memoryBuffer.peek();

        // if we run out of stuff in the list signal end of data
        if (local == null) return (0);

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

    public int write(ByteBuffer bb)
    {
        int pos = bb.position();
        int rem = bb.remaining();

        if (memoryMode == true) {
            ByteBuffer local = ByteBuffer.allocate(rem - pos);
            local.put(bb.array(), pos, rem);
            local.flip();
            memoryBuffer.add(local);
            try {
                hashStream.write(bb.array(), pos, rem);
                arrayStream.reset();
            } catch (Exception exn) {
                logger.error("Virus scan write memory failed: ", exn);
            }
        } else {

            try {
                hashStream.write(bb.array(), pos, rem);
            } catch (Exception exn) {
                logger.error("Virus scan write file failed: ", exn);
            }
        }

        bb.position(pos + rem);

        return (rem - pos);
    }

    public void write(int b)
    {
        if (byteWriter.remaining() == 0) {
            byteWriter.flip();
            write(byteWriter);
            byteWriter.clear();
        }

        byteWriter.put((byte) b);
    }

    public void position(int location)
    {
        if (memoryMode == true) return;

        try {
            fileInChannel.position(location);
        } catch (Exception exn) {
            logger.error("Virus scan position failed: ", exn);
        }
    }

    public void flush()
    {
        if (byteWriter.position() > 0) {
            byteWriter.flip();
            write(byteWriter);
            byteWriter.clear();
        }

        if (memoryMode == true) return;

        try {
            hashStream.flush();
            fileOutStream.flush();
        } catch (Exception exn) {
            logger.error("Virus scan flush failed: ", exn);
        }
    }

    public void close()
    {
        flush();

        if (memoryMode == true) return;

        try {
            fileOutStream.close();
        } catch (Exception exn) {
            logger.error("Virus scan close failed: ", exn);
        }
    }

    public boolean delete()
    {
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

        for (int i = 0; i < memoryBuffer.size(); i++) {
            ByteBuffer local = memoryBuffer.get(i);
            total += local.remaining();
        }

        return (total);
    }

    protected File getFileObject()
    {
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

        flush();

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
