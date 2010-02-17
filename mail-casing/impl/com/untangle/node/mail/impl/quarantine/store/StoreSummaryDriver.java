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

package com.untangle.node.mail.impl.quarantine.store;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Map;

import org.apache.log4j.Logger;

import com.untangle.node.util.IOUtil;
import com.untangle.node.util.Pair;


/**
 * Holds methods to maipulate the on-disk
 * representation of a StoreSummary
 */
public /*temp*/ class StoreSummaryDriver
    extends AbstractDriver {

    private static final String CLOSED_FILE_NAME = "summary.closed";
    private static final String OPEN_FILE_NAME = "summary.open";

    //Blurb added to top of file
    private static final String[] BLURB = new String[] {
        "################################################",
        "#          Quarantine Master Metadata          #",
        "#                                              #",
        "#               DO NOT MODIFY                  #",
        "#                                              #",
        "# Although this is a text file, it is intended #",
        "# only for machine reading and writing.        #",
        "################################################"
    };

    private static final String RECORD_SEP = "--";
    private static final int VERSION = 1;


    private static final Logger s_logger = Logger.getLogger(StoreSummaryDriver.class);

    /**
     * Marks the file as "open", meaning if there is a crash
     * the file will be assumed to be out-of-date upon
     * the next startup.
     */
    static boolean openSummary(File dir) {
        File f = new File(dir, CLOSED_FILE_NAME);
        if(f.exists()) {
            return f.renameTo(new File(dir, OPEN_FILE_NAME));
        }
        return false;
    }

    /**
     * Read the summary in the given quarantine directory
     *
     * @param inboxDir the quarantine <b>directory</b> (i.e. not the
     *        summary data file itself).
     *
     * @return the "result".  The "StoreSummary" will be null
     *         unless the result was "OK".
     */
    static Pair<FileReadOutcome, StoreSummary> readSummary(File inboxDir) {

        FileInputStream fIn = null;

        try {
            File f = new File(inboxDir, CLOSED_FILE_NAME);
            if(!f.exists()) {
                return new Pair<FileReadOutcome, StoreSummary>(FileReadOutcome.NO_SUCH_FILE);
            }
            fIn = new FileInputStream(f);
            BufferedInputStream bIn = new BufferedInputStream(fIn);
            BufferedReader reader = new BufferedReader(new InputStreamReader(bIn));

            //Read version
            int version = readVersion(reader);
            //Someday, we'll care about version...

            StoreSummary ret = new StoreSummary();

            Pair<String, InboxSummary> mapping =
                readInboxSummary(reader);

            while(mapping != null) {
                ret.addInbox(mapping.a, mapping.b);
                mapping = readInboxSummary(reader);
            }
            IOUtil.close(fIn);
            return new Pair<FileReadOutcome, StoreSummary>(FileReadOutcome.OK, ret);
        }
        catch(BadFileEntry ex) {
            s_logger.warn("", ex);
            IOUtil.close(fIn);
            return new Pair<FileReadOutcome, StoreSummary>(FileReadOutcome.FILE_CORRUPT);
        }
        catch(EOFException ex) {
            s_logger.warn("", ex);
            IOUtil.close(fIn);
            return new Pair<FileReadOutcome, StoreSummary>(FileReadOutcome.FILE_CORRUPT);
        }
        catch(IOException ex) {
            s_logger.warn("", ex);
            IOUtil.close(fIn);
            return new Pair<FileReadOutcome, StoreSummary>(FileReadOutcome.EXCEPTION);
        }
    }





    static boolean writeSummary(File dir,
                                StoreSummary summary) {

        if(!dir.exists()) {
            dir.mkdir();
        }

        File f = null;
        FileOutputStream fOut = null;
        try {
            f = File.createTempFile("mdf", ".tmp", dir);
            fOut = new FileOutputStream(f, false);
            BufferedOutputStream bOut = new BufferedOutputStream(fOut);
            PrintWriter pw = new PrintWriter(fOut);

            //Write blurb
            for(String s : BLURB) {
                pw.println(s);
            }
            pw.println();

            //Write version
            writeVersion(pw, VERSION);

            for(Map.Entry<String,InboxSummary> entry : summary.entries()) {
                writeInboxSummary(entry, pw);
            }
            pw.flush();
            bOut.flush();
            fOut.flush();
            IOUtil.close(fOut);
            if(!f.renameTo(new File(dir, CLOSED_FILE_NAME))) {
                IOUtil.delete(f);
                return false;
            }
            return true;
        }
        catch(Exception ex) {
            s_logger.error("", ex);
            IOUtil.close(fOut);
            IOUtil.delete(f);
            return false;
        }
    }


    private static void writeInboxSummary(Map.Entry<String,InboxSummary> entry,
                                          PrintWriter pw) throws IOException {
        pw.println(RECORD_SEP);
        pw.println(entry.getKey());
        pw.println(entry.getValue().getDir().relativePath);
        pw.println(Long.toString(entry.getValue().getTotalSz()));
        pw.println(Integer.toString(entry.getValue().getTotalMails()));
    }

    /**
     * Method returns null of EOF is encountered
     */
    private static Pair<String, InboxSummary> readInboxSummary(BufferedReader reader)
        throws BadFileEntry, IOException {

        try {
            readUntil(reader, RECORD_SEP);
            String addr = readLine(reader);
            InboxSummary summary = new InboxSummary(
                                                    new RelativeFileName(readLine(reader)),
                                                    readLong(reader),
                                                    (int) readLong(reader));
            return new Pair<String, InboxSummary>(addr,summary);
        }
        catch(EOFException ex) {
            return null;
        }

    }



    //====================================
    // TEST CODE

    /*
      public static void main(String[] args) throws Exception {
      File targetDir = new File(new File(System.getProperty("user.dir")), "test");

      if(!targetDir.exists()) {
      targetDir.mkdirs();
      }

      String[] names = new String[] {
      "a@foo.com",
      "b@foo.com",
      "c@foo.com"
      };

      QuarantineMetadata map = new QuarantineMetadata();
      for(String s : names) {
      map.addInbox(s, new RelativeFileName(s.substring(0, 1) + "XX/123"));
      }


      System.out.println("1 (write): " + writeMetadata(new File(targetDir, "test1"), map));


      File dir2 = new File(targetDir, "test2");
      System.out.println("2 (write): " + writeMetadata(dir2, map));
      Pair<FileReadOutcome, QuarantineMetadata> read = readMetadata(dir2);
      System.out.println("2 (read): " + read.a);

      for(String s : names) {
      RelativeFileName dirName = read.b.getInboxDir(s);
      System.out.println("2 Dir for: " + s + ": \"" +
      (dirName==null?"<null return>":dirName.relativePath) + "\"");

      }



      }

    */



}
