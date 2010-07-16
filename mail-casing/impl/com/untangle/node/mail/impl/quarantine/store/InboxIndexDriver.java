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

import org.apache.log4j.Logger;

import com.untangle.node.mail.papi.quarantine.InboxIndex;
import com.untangle.node.mail.papi.quarantine.InboxRecord;
import com.untangle.node.mail.papi.quarantine.MailSummary;
import com.untangle.node.util.IOUtil;
import com.untangle.node.util.Pair;

/**
 * Driver for reading/writing Inbox Indecies
 * on disk
 */
class InboxIndexDriver
    extends AbstractDriver {

    private static final Logger s_logger = Logger.getLogger(InboxIndexDriver.class);

    //Blurb added to top of file
    private static final String[] BLURB = new String[] {
        "################################################",
        "#             Inbox Index File                 #",
        "#                                              #",
        "#               DO NOT MODIFY                  #",
        "#                                              #",
        "# Although this is a text file, it is intended #",
        "# only for machine reading and writing.        #",
        "################################################"
    };
    private static final String INDEX_FILE_NAME = "index.mqi";
    private static final String RECORD_SEP = "--SEP--";
    private static final String INBOX_OWNER_TAG = "Address:";
    private static final int VERSION = 3;


    /**
     * Read the index contained in the given inbox directory
     *
     * @param inboxDir the inbox <b>directory</b> (i.e. not the
     *        inbox data file itself).
     *
     * @return the "result".  The "InboxIndexImpl" will be null
     *         unless the result was "OK".
     */
    static Pair<FileReadOutcome, InboxIndexImpl> readIndex(File inboxDir) {

        FileInputStream fIn = null;

        try {
            File f = new File(inboxDir, INDEX_FILE_NAME);
            if(!f.exists()) {
                return new Pair<FileReadOutcome, InboxIndexImpl>(FileReadOutcome.NO_SUCH_FILE);
            }
            fIn = new FileInputStream(f);
            BufferedInputStream bIn = new BufferedInputStream(fIn);
            BufferedReader reader = new BufferedReader(new InputStreamReader(bIn));

            //Read version
            readVersion(reader);
            //Someday, we'll care about version...

            InboxIndexImpl ret = new InboxIndexImpl();

            //Read inbox owner
            ret.setOwnerAddress(readTaggedEntry(reader, INBOX_OWNER_TAG));
            ret.setLastAccessTimestamp(f.lastModified());

            //Read entries
            InboxRecordImpl record = readRecord(reader, ret.getOwnerAddress());
            while(record != null) {
                ret.put(record.getMailID(), record);
                record = readRecord(reader, ret.getOwnerAddress());
            }
            IOUtil.close(fIn);
            return new Pair<FileReadOutcome, InboxIndexImpl>(FileReadOutcome.OK, ret);
        }
        catch(BadFileEntry ex) {
            s_logger.warn("", ex);
            IOUtil.close(fIn);
            return new Pair<FileReadOutcome, InboxIndexImpl>(FileReadOutcome.FILE_CORRUPT);
        }
        catch(EOFException ex) {
            s_logger.warn("", ex);
            IOUtil.close(fIn);
            return new Pair<FileReadOutcome, InboxIndexImpl>(FileReadOutcome.FILE_CORRUPT);
        }
        catch(IOException ex) {
            s_logger.warn("", ex);
            IOUtil.close(fIn);
            return new Pair<FileReadOutcome, InboxIndexImpl>(FileReadOutcome.EXCEPTION);
        }
    }

    /**
     * Create a new blank (empty) index for the given
     * address in the given Inbox directory.
     *
     * @param address the user email address
     * @param dir the Inbox folder
     *
     * @return true if successful, false otherwise.
     */
    static boolean createBlankIndex(String address,
                                    File dir) {

        InboxIndexImpl impl = new InboxIndexImpl();
        impl.setOwnerAddress(address);
        return replaceIndex(impl, dir);
    }

    /**
     * Append a new record to the given
     * user's index.  If there is no
     * Inbox data for the given user, the
     * inbox data file is implicitly created.
     *
     * @param address the user address
     * @param inboxDir the inbox directory
     *
     * @return false if the append failed.  The state
     * of the file after the failure is undefined.
     */
    static boolean appendIndex(String address,
                               File inboxDir,
                               InboxRecord newRecord) {

        File f = new File(inboxDir, INDEX_FILE_NAME);

        //Make sure index exists
        if(!f.exists()) {
            InboxIndexImpl impl = new InboxIndexImpl();
            impl.setOwnerAddress(address);
            impl.put(newRecord.getMailID(), newRecord);
            return replaceIndex(impl, inboxDir);
        }

        FileOutputStream fOut = null;
        try {

            fOut = new FileOutputStream(f, true);
            PrintWriter pw = new PrintWriter(fOut);

            writeRecord(newRecord, pw);

            pw.flush();
            fOut.flush();
            fOut.close();
            return true;
        }
        catch(Exception ex) {
            s_logger.error("Exception appending to index file", ex);
            IOUtil.close(fOut);
            return false;
        }
    }

    /**
     * Replace the index file for a given user
     *
     * @param index the new index
     * @param inboxDir the inbox directory
     *
     * @return false if the append failed.  If the append
     *         failed, the existing (if there was one)
     *         inbox data file remains.
     */
    static boolean replaceIndex(InboxIndex index,
                                File inboxDir) {

        if (false == inboxDir.exists()) {
            if (false == inboxDir.mkdirs()) {
                // mkdirs can fail when multiple threads simultaneously attempt
                // to create different directories on a shared directory tree and
                // the tree has branches that need to be created
                // - getInboxDir/AddressLock prevents this problem
                //   (while thread X is creating a directory,
                //    other threads wait for thread X to finish)
                //   (i.e., this condition should never occur)
                s_logger.error("Unable to create: " + inboxDir + ", and to update index");
                return false;
            }
        }

        File f = null;
        FileOutputStream fOut = null;

        try {
            f = File.createTempFile("idx", ".tmp", inboxDir);
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

            //Write "owner"
            writeTaggedEntry(pw, INBOX_OWNER_TAG, index.getOwnerAddress());

            //Write records
            for(InboxRecord record : index) {
                writeRecord(record, pw);
            }

            pw.flush();
            bOut.flush();
            fOut.flush();
            fOut.close();

            if(!f.renameTo(new File(inboxDir, INDEX_FILE_NAME))) {
                f.delete();
                return false;
            }
            return true;
        } catch(Exception ex) {
            s_logger.error("Unable to update index", ex);
            IOUtil.close(fOut);
            IOUtil.delete(f);
            return false;
        }
    }



    private static void writeRecord(InboxRecord record,
                                    PrintWriter pw) throws IOException {

        MailSummary summary = record.getMailSummary();

        pw.println(RECORD_SEP);
        writeVersion(pw, VERSION);
        pw.println(nullToQQ(record.getMailID()));
        pw.println(Long.toString(record.getInternDate()));
        pw.println(Long.toString(record.getSize()));

        String[] recipients = record.getRecipients();
        if(recipients == null) {recipients = new String[0];}

        pw.println(Long.toString(recipients.length));
        for(String s : recipients) {
            writeMultilineEntry(pw, s);
        }

        writeMultilineEntry(pw, summary.getSender());
        writeMultilineEntry(pw, summary.getSubject()); // write non-truncated value
        writeMultilineEntry(pw, summary.getQuarantineCategory());
        writeMultilineEntry(pw, summary.getQuarantineDetail());
        pw.println(Long.toString(summary.getAttachmentCount()));
        pw.println();
    }


    /**
     * Method returns null of EOF is encountered
     */
    private static InboxRecordImpl readRecord(BufferedReader reader,
                                              String inboxOwnerAddress)
        throws BadFileEntry, IOException {

        try {
            //Read up-to the next record separator
            if(!readUntil(reader, RECORD_SEP)) {
                return null;
            }

            //Read version
            int version = readVersion(reader);

            if(version == 1) {
                return readV1Record(reader, inboxOwnerAddress);
            }
            if(version == 2) {
                return readV2Record(reader, inboxOwnerAddress);
            }
            return readV3Record(reader);
        }
        catch(EOFException ex) {
            return null;
        }

    }

    private static InboxRecordImpl readV3Record(BufferedReader reader)
        throws BadFileEntry, IOException {

        try {
            InboxRecordImpl ret = new InboxRecordImpl();
            MailSummary summary = new MailSummary();
            ret.setMailSummary(summary);

            ret.setMailID(readLine(reader));
            ret.setInternDate(readLong(reader));
            ret.setSize(readLong(reader));

            String[] recipients = new String[(int) readLong(reader)];
            for(int i = 0; i<recipients.length; i++) {
                recipients[i] = readMultilineEntry(reader);
            }
            ret.setRecipients(recipients);

            summary.setSender(readMultilineEntry(reader));
            summary.setSubject(readMultilineEntry(reader));
            summary.setQuarantineCategory(readMultilineEntry(reader));
            summary.setQuarantineDetail(readMultilineEntry(reader));
            summary.setAttachmentCount((int) readLong(reader));
            return ret;
        }
        catch(EOFException ex) {
            return null;
        }
    }


    private static InboxRecordImpl readV2Record(BufferedReader reader,
                                                String inboxOwnerAddress)
        throws BadFileEntry, IOException {

        try {
            InboxRecordImpl ret = new InboxRecordImpl();
            MailSummary summary = new MailSummary();
            ret.setMailSummary(summary);

            ret.setMailID(readLine(reader));
            ret.setInternDate(readLong(reader));
            ret.setSize(readLong(reader));
            //Fake the recipients (just assume same as inbox)
            ret.setRecipients(new String[] {inboxOwnerAddress});

            summary.setSender(readMultilineEntry(reader));
            summary.setSubject(readMultilineEntry(reader));
            summary.setQuarantineCategory(readMultilineEntry(reader));
            summary.setQuarantineDetail(readMultilineEntry(reader));
            summary.setAttachmentCount((int) readLong(reader));
            return ret;
        }
        catch(EOFException ex) {
            return null;
        }
    }


    private static InboxRecordImpl readV1Record(BufferedReader reader,
                                                String inboxOwnerAddress)
        throws BadFileEntry, IOException {

        try {
            InboxRecordImpl ret = new InboxRecordImpl();
            MailSummary summary = new MailSummary();
            ret.setMailSummary(summary);

            ret.setMailID(readLine(reader));
            ret.setInternDate(readLong(reader));
            ret.setSize(readLong(reader));
            //Fake the recipients (just assume same as inbox)
            ret.setRecipients(new String[] {inboxOwnerAddress});

            summary.setSender(readMultilineEntry(reader));
            summary.setSubject(readMultilineEntry(reader));
            summary.setQuarantineCategory(readMultilineEntry(reader));
            summary.setQuarantineDetail(readMultilineEntry(reader));
            //Just fake that there are no attachments
            summary.setAttachmentCount(0);
            return ret;
        }
        catch(EOFException ex) {
            return null;
        }
    }


    /*
      public static void main(String[] args) throws Exception {
      File root = new File(System.getProperty("user.dir"));

      System.out.println("\n\n\n--------- 1 -----------");
      File dir = new File(root, "test1");
      createBlankIndex("1@foo.com", dir);

      System.out.println("\n\n\n--------- 2 -----------");
      dir = new File(root, "test2");
      InboxIndexImpl index = new InboxIndexImpl();
      index.setOwnerAddress("2@foo.com");
      System.out.println("\n(2) PRE");
      index.debugPrint();
      replaceIndex(index, dir);
      System.out.println("\n(2) POST");
      readIndex(dir).b.debugPrint();

      System.out.println("\n\n\n--------- 3 -----------");
      dir = new File(root, "test3");
      createBlankIndex("3@foo.com", dir);
      appendIndex("3@foo.com", dir, new InboxRecordImpl(
      "3a",
      System.currentTimeMillis(),
      300,
      new MailSummary("sender3a@foo.com", "subject 3a", "5.0", "this\nhas\nlines")));
      appendIndex("3@foo.com", dir, new InboxRecordImpl(
      "3b",
      System.currentTimeMillis(),
      300,
      new MailSummary("sender3b@foo.com", "subject 3b", "5.0", "this has no lines")));
      System.out.println("\n(3) Read (a)");
      readIndex(dir).b.debugPrint();
      Thread.currentThread().sleep(2000);
      appendIndex("3@foo.com", dir, new InboxRecordImpl(
      "3c",
      System.currentTimeMillis(),
      300,
      new MailSummary("sender3c@foo.com", "subject 3c", "5.0", "this has no lines")));
      System.out.println("\n(3) Read (b)");
      readIndex(dir).b.debugPrint();
      }
    */

}
