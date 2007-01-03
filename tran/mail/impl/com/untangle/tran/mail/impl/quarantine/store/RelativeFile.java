/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.mail.impl.quarantine.store;
import org.apache.log4j.Logger;

import java.io.*;


/**
 * An association of a File with
 * its "relative" name.
 */
final class RelativeFile
  extends RelativeFileName {

  /**
   * The real file represented by this object
   */
  final File file;

  RelativeFile(String path,
    File file) {
    super(path);
    this.file = file;
  }

  @Override
  public boolean equals(Object obj) {
    //Don't bother with superclass
    return file.equals(obj);
  }

  @Override
  public int hashCode() {
    //Don't bother with superclass
    return file.hashCode();
  }
}