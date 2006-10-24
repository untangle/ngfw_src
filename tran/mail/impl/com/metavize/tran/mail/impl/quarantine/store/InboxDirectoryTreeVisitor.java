/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.mail.impl.quarantine.store;

/**
 * Callback interface for Objects
 * which wish to visit the contents of
 * a {com.metavize.tran.mail.impl.quarantine.store.InboxDirectoryTree InboxDirectoryTree}.
 *
 */
interface InboxDirectoryTreeVisitor {

  /**
   * Visit the given directory within the
   * InboxDirectoryTree.
   *
   * @param f the relative file representing
   *        a directory.  Note that this
   *        may not be a terminal (inbox)
   *        directory.
   */
  void visit(RelativeFile f);
}