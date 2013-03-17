/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.smtp.quarantine.store;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.log4j.Logger;

import com.untangle.node.util.CharSequenceUtil;


/**
 * Base class for drivers (things that read/write a given
 * file format).  Someday, we may switch to XML as the baseline
 * format of files, and this will go away.  Note that XML
 * was <b>not</b> chosen because it was hard to append
 * to an XML file (and still make it parsable) without
 * having to read the entire file first.
 */
class AbstractDriver {

    private static final String VERSION_TAG = "Ver:";

    //New line.  I know we're always on Unix, but old
    //habits die hard...
    private static final String NEW_LINE =
        System.getProperty("line.separator", "\n");

    /**
     * Enum of outcomes for reading files.
     */
    static enum FileReadOutcome {
        OK,
        NO_SUCH_FILE,
        EXCEPTION,
        FILE_CORRUPT
    };


    /**
     * This method implicitly trims the line
     */
    static String readLine(BufferedReader reader)
        throws EOFException, IOException {
        return readLine(reader, true, true);
    }

    /**
     * Reads the next non-blank/comment line
     * and attempts to convert its contents
     * into a Long.
     *
     * @param reader the reader
     *
     * @return the long
     *
     * @exception IOException from the underlying reader
     *
     * @exception EOFExcetpion if end of file is encountered
     *
     * @exception BadFileEntry if the line was read, but
     *            could not be converted into a long.
     */
    static long readLong(BufferedReader reader)
        throws EOFException, IOException, BadFileEntry {
        return aToL(readLine(reader, true, true));
    }

    /**
     * This method implicitly trims the line
     *
     * @param reader the reader
     * @param skipBlanks if true, blank lines are skipped
     * @param skipComments if true, comments ("^#") are skipped
     *
     * @return the line as a String (no terminator)
     *
     * @exception IOException from the underlying reader
     *
     * @exception EOFExcetpion if end of file is encountered
     */
    static String readLine(BufferedReader reader,
                           boolean skipBlanks, boolean skipComments)
        throws EOFException, IOException {

        while(true) {
            String line = reader.readLine();
            if(line == null) {
                throw new EOFException();
            }
            line = line.trim();
            if(skipBlanks && "".equals(line)) {
                continue;
            }
            if(skipComments && line.startsWith("#")) {
                continue;
            }
            return line;
        }
    }

    /**
     * Reads the next non-blank/comment line,
     * throwing BadFileEntry if it does not
     * exactly match (case sensitive) the expected
     * value
     *
     * @param reader the reader
     * @param expect the expected value
     *
     * @exception IOException from the underlying reader
     *
     * @exception EOFExcetpion if end of file is encountered
     *
     * @exception BadFileEntry if the line was read, but
     *            did not match <code>expect</code>
     */
    static void readLineExact(BufferedReader reader,
                              String expect) throws EOFException,
                                                    BadFileEntry, IOException{
        String read = readLine(reader, true, true);
        if(!read.equals(expect)) {
            throw new BadFileEntry("Read \"" +
                                   read + "\" Expected \"" + expect + "\"");
        }
    }

    /**
     * Read a "tagged" entry.  A tag is simply a string
     * preceeding the value in question.
     *
     * Tagged entries may be preceeded by comments or LWS.
     * Tags <b>are</b> case sensitive.
     *
     * The returned entry has the tag already removed.
     *
     * @param reader the reader
     * @param tag the tag
     *
     * @exception IOException from the underlying reader
     *
     * @exception EOFExcetpion if end of file is encountered
     *
     * @exception BadFileEntry if the line was read, but
     *            did not start with <code>tag</code>
     */
    static String readTaggedEntry(BufferedReader reader,
                                  String tag) throws EOFException,
                                                     BadFileEntry, IOException{

        String read = readLine(reader, true, true);

        if(!read.startsWith(tag)) {
            throw new BadFileEntry("Expected line \"" +
                                   read + "\" to start with \"" + tag + "\"");
        }
        return read.substring(tag.length());
    }

    /**
     * Write a "tagged" entry ("tag+entry");
     *
     * @param pw the writer
     * @param tag the tag
     * @param entry the entry (cannot be multiline).
     *
     *
     * @exception IOException from the underlying IO system
     */
    static void writeTaggedEntry(PrintWriter pw,
                                 String tag,
                                 String entry) throws IOException {
        pw.print(tag);
        pw.println(nullToQQ(entry));
    }


    /**
     * Read a "multiline" entry.  A multiline entry need not
     * actualy span lines, but it could so the corresponding
     * write outputs the number of lines as the tag.
     *
     * Tagged entries may be preceeded by comments or LWS.
     * Tags <b>are</b> case sensitive.
     *
     * The returned entry has the leading "number of lines"
     * tag removed, and all lines are appended to a single
     * String
     *
     * @exception IOException from the underlying reader
     *
     * @exception EOFExcetpion if end of file is encountered
     *
     * @exception BadFileEntry if the line(s) was read, but
     *            did not conform to the correct multiline format
     */
    static String readMultilineEntry(BufferedReader reader)
        throws EOFException, BadFileEntry, IOException{

        String read = readLine(reader, true, true);

        int index = read.indexOf(':');
        if(index == -1) {
            throw new BadFileEntry("Expected line \"" +
                                   read + "\" to start with \"nnn:\"");
        }

        int numLines = 0;
        try {
            numLines = Integer.parseInt(read.substring(0, index));
        }
        catch(Exception ex) {
            throw new BadFileEntry("Expected line \"" +
                                   read + "\" to start with \"<number>:\"");
        }

        StringBuilder ret = new StringBuilder();
        ret.append(read.substring(index+1, read.length()));


        for(int i = 0; i<numLines; i++) {
            String aLine = reader.readLine();
            if(aLine == null) {
                throw new EOFException();
            }
            ret.append(NEW_LINE);
            ret.append(aLine);
        }
        return ret.toString();
    }

    /**
     * Write a (possibly) multiline entry.  Note that
     * if the value if null, it will implicitly be converted
     * to "".
     *
     * @param pw the writer
     * @param value the value to be written.
     *
     * @exception IOException from the underlying IO system
     */
    static void writeMultilineEntry(PrintWriter writer,
                                    String value) throws IOException {
        writer.print(Integer.toString(
                                      value==null?0:CharSequenceUtil.countLines(value)));
        writer.print(":");
        writer.println(nullToQQ(value));
    }

    /**
     * Read a version from the reader
     *
     * @param reader the reader
     * @return the version
     *
     * @exception IOException from the underlying reader
     *
     * @exception EOFExcetpion if end of file is encountered
     *
     * @exception BadFileEntry if the line was read, but
     *            was not in the correct format.
     */
    static int readVersion(BufferedReader reader)
        throws EOFException, BadFileEntry, IOException {
        String verString = readTaggedEntry(reader, VERSION_TAG);
        return aToI(verString);
    }

    /**
     * Write a "version" line to the writer
     *
     * @param version the version
     * @param pw the writer
     *
     *
     * @exception IOException from the underlying IO system
     */
    static void writeVersion(PrintWriter pw,
                             int version) throws IOException {
        writeTaggedEntry(pw, VERSION_TAG, Integer.toString(version));
    }

    /**
     * Convert a string to
     * a long.
     * <br>
     * Throws the correct type of exception
     * for use with file parsing
     *
     * @param str the long-as-string
     *
     * @return the long
     *
     * @exception BadFileEntry if the conversion fails
     */
    static long aToL(String str)
        throws BadFileEntry {
        try {
            return Long.parseLong(str);
        }
        catch(Exception ex) {
            throw new BadFileEntry("Unable to parse \"" +
                                   str + "\" into long");
        }
    }

    /**
     * Convert a string to
     * an integer.
     * <br>
     * Throws the correct type of exception
     * for use with file parsing
     *
     * @param str the int-as-string
     *
     * @return the int
     *
     * @exception BadFileEntry if the conversion fails
     */
    static int aToI(String str)
        throws BadFileEntry {
        return (int) aToL(str);
    }

    /**
     * Converts null to ""
     */
    static String nullToQQ(String s) {
        return s==null?"":s;
    }



    /**
     * Read until an exact match for the String
     * is found.  Consumes the String (since the caller
     * already knows what it looks like).
     *
     * @param reader the reader
     *
     * @return false if EOF is encountered before
     *         target or an exception is encountered.
     *
     */
    static boolean readUntil(BufferedReader reader,
                             String match) {
        Logger logger = Logger.getLogger(AbstractDriver.class);

        try {
            while(true) {
                String line = reader.readLine();
                if(line == null) {
                    return false;
                }
                line = line.trim();
                if(
                   ("".equals(line)) ||
                   (line.startsWith("#")) ||
                   (!line.equals(match))
                   ) {
                    continue;
                }
                return true;
            }
        }
        catch(Exception ex) {
            logger.warn("Exception reading line", ex);
            return false;
        }
    }

}
