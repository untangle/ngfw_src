/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.mail.impl.quarantine.store;

import com.metavize.tran.util.IOUtil;

import java.io.*;


//TODO bscott Some way to ensure unique names of directories, in
//an atomic fashon.

/**
 * Responsible for creating/destroying directories for the
 * Quarantine.  Current implementation is "dumb", but may be
 * enhanced to have nested folders for quicker access times.
 */
class InboxDirectoryTree {

  private static final String DATA_DIR_NAME = "inboxes";

  private File m_rootDir;

  InboxDirectoryTree(File quarantineRootDir) {
    m_rootDir = new File(quarantineRootDir, DATA_DIR_NAME);
    if(!m_rootDir.exists()) {
      m_rootDir.mkdirs();
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
    visitInboxesImpl(m_rootDir, visitor, DATA_DIR_NAME);
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
    //TODO bscott this is temp
    String dirName = Long.toString(System.currentTimeMillis());
    File ret = new File(m_rootDir, dirName);
    ret.mkdirs();
    return new RelativeFile(
      (DATA_DIR_NAME + File.separator + dirName),
      ret);
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
    IOUtil.rmDir(new File(m_rootDir, dir.relativePath));
  }

}