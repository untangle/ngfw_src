/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import com.metavize.tran.token.Chunk;

public class FileMapper {

    private static final int DEFAULT_CHUNK_SIZE = 1024*1024;

    private FileMapper() {}


    public static Chunk[] fileToChunks(String inFile)
    {
        return fileToChunks(inFile,DEFAULT_CHUNK_SIZE);
    }

    public static Chunk[] fileToChunks(String inFile, int chunkSize)
    {
        return fileToChunks(new File(inFile),chunkSize);
    }

    public static Chunk[] fileToChunks(File inFile)
    {
        return fileToChunks(inFile,DEFAULT_CHUNK_SIZE);
    }

    public static Chunk[] fileToChunks(File inFile, int chunkSize)
    {
        try {
            return fileToChunks((new FileInputStream(inFile)).getChannel(),chunkSize);
        } catch (IOException e) {
            System.err.println("IOException: " + e);
            return null; /* XXX */
        }
    }

    public static Chunk[] fileToChunks(FileChannel inFile)
    {
        return fileToChunks(inFile,DEFAULT_CHUNK_SIZE);
    }

    public static Chunk[] fileToChunks(FileChannel inFile, int chunkSize)
    {
        try {
            Chunk[] chunks = new Chunk[(int)(((inFile.size()-inFile.position())/chunkSize)+1) + 1];
            int    nodeCount = 0;

            /**
             * Add each meg, one at a time
             */
            for (int point=0; point< inFile.size(); point += chunkSize) {

                if (inFile.position() >= /*XXX >?*/ point+chunkSize)
                    continue;

                int startpoint = ( inFile.position() > point ? (int)inFile.position() : point );
                int endpoint = (int)( inFile.size() < point+chunkSize ? inFile.size() : point+chunkSize );

                MappedByteBuffer buf = inFile.map(FileChannel.MapMode.READ_ONLY,startpoint,endpoint-startpoint);
                Chunk ck = new Chunk(buf);

                chunks[nodeCount++] = ck;
            }

            return chunks;
        }
        catch (IOException e) {
            System.err.println("IOException: " + e);
            return null; /* XXX */
        }
    }

}
