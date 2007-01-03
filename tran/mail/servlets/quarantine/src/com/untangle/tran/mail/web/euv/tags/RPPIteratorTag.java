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
package com.untangle.tran.mail.web.euv.tags;

import javax.servlet.ServletRequest;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * Tag which iterates over the possible rows-per-page values,
 * and assigns the "current" to the RPPOptionTag
 */
public final class RPPIteratorTag
  extends IteratingTag<String> {

  private static final String IKEY = "untangle.RPPIteratorTag";

  private static final String[] CHOICES = {
    "25",
    "50",
    "100",
    "150",
    "200"
  };
  

  @Override
  protected Iterator<String> createIterator() {
    return Arrays.asList(CHOICES).iterator(); 
  }

  @Override
  protected void setCurrent(String s) {
    RPPCurrentOptionTag.setCurrent(pageContext, s);
  }
}
