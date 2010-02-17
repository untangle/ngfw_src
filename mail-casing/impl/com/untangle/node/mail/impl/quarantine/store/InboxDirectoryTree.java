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

import java.io.File;

import com.untangle.node.util.IOUtil;



/**
 * Responsible for creating/destroying directories for the
 * Quarantine.  Current implementation is "dumb", but may be
 * enhanced to have nested folders for quicker access times.
 */
class InboxDirectoryTree {

    private static final String DATA_DIR_NAME = "inboxes";
    private static final int MAX_TRIES = 10000;//Just to avoid the infinite loop

    private Object m_lock = new Object();

    private File m_inboxRootDir;
    private File m_quarantineRootDir;

    InboxDirectoryTree(File quarantineRootDir) {

        m_quarantineRootDir = quarantineRootDir;

        m_inboxRootDir = new File(m_quarantineRootDir, DATA_DIR_NAME);
        if(!m_inboxRootDir.exists()) {
            m_inboxRootDir.mkdirs();
        }
    }


    /**
     * Pass a visitor through this
     * tree's managed directory structure.
     * <br><br>
     * This is a depth-first tree walk, so the visitor
     * <b>can</b> delete files.
     *
     * @param visitor the visitor
     */
    void visitInboxes(InboxDirectoryTreeVisitor visitor) {
        visitInboxesImpl(m_inboxRootDir, visitor, DATA_DIR_NAME);
    }

    private void visitInboxesImpl(File dir,
                                  InboxDirectoryTreeVisitor visitor,
                                  String relativePathAsString) {
        File[] kids = dir.listFiles();
        for(File kid : kids) {
            if(kid.isDirectory()) {
                visitInboxesImpl(kid,
                                 visitor,
                                 relativePathAsString + File.separator + kid.getName());
            }
        }
        visitor.visit(new RelativeFile(relativePathAsString, dir));
    }

    /**
     * Create a new inbox directory.
     * This method *does* create the directory file
     *
     * @return the new directory, or null if there
     *         is some terrible problem with the
     *         underlying store.
     */
    RelativeFile createInboxDir() {
        return createInboxDirImpl();
    }

    /**
     * Delete the inbox directory.  Note that this
     * method is "dumb" in that it (a) does not update
     * any counts and (b) does not check if the folder
     * is empty.
     *
     * @param dir the doomed directory.
     */
    void deleteInboxDir(RelativeFileName dir) {
        IOUtil.rmDir(new File(m_quarantineRootDir, dir.relativePath));
    }

    private RelativeFile createInboxDirImpl() {
        long num = System.currentTimeMillis();
        File f = null;
        for(int i = 0; i<MAX_TRIES; i++) {
            f = new File(m_inboxRootDir, Long.toString(num + i));
            synchronized(m_lock) {
                if(!f.exists()) {
                    f.mkdirs();
                }
                else {
                    f = null;
                }
            }
            if(f != null) {
                break;
            }
        }
        return f==null?
            null:new RelativeFile(
                                  (DATA_DIR_NAME + File.separator + f.getName()),
                                  f);
    }

}
