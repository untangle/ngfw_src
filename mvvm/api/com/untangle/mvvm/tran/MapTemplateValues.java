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

package com.untangle.mvvm.tran;

/**
 * Implementation of TemplateValues which acts as
 * a map (really, a java.util.Properties).
 *
 * @see Template
 */
public class MapTemplateValues
  extends java.util.Properties
  implements TemplateValues {


  public String getTemplateValue(String key) {
    return getProperty(key);
  }

}
