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

package com.untangle.tran.mime;

/**
 * Lightweight class to associate an
 * EmailAddress with its RcptType
 * in a MIMEMessage.
 */
public class EmailAddressWithRcptType {

  /**
   * The address member
   */
  public final EmailAddress address;

  /**
   * The type of recipient member
   */
  public final RcptType type;

  /**
   *
   */
  public EmailAddressWithRcptType(EmailAddress address,
    RcptType type) {
    this.address = address;
    this.type = type;
  }

  /**
   * Tests for equality based on equality of addresses,
   * then equivilancy of type.
   */
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if(obj instanceof EmailAddressWithRcptType) {
      EmailAddressWithRcptType other = (EmailAddressWithRcptType) obj;
      return address==null?
        //Our address is null
        (other.address == null?
          type == other.type:
          false):
        //Our address is not null
        (other.address == null?
          false:
          other.address.equals(address) && type == other.type);
    }
    return false;
  }

  @Override
  public int hashCode() {
    //TODO bscott.  What should null's hashcode be?  How
    //     should we combine the hashcode of the enum with address?
    return address==null?
      (type==null?
        0:
        type.hashCode()):
      (type==null?
        address.hashCode():
        address.hashCode() + type.hashCode());
  }

  /**
   * For debugging
   */
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append('(');
    sb.append(type==null?"null":type);
    sb.append(") ");
    sb.append(address==null?"null":address.toMIMEString());
    return sb.toString();
  }


}