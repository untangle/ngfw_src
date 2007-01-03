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

import static com.untangle.tran.util.ASCIIUtil.*;
import static com.untangle.tran.util.Ascii.*;

import java.nio.*;
import java.util.*;

/**
 * Class which creates HeaderFields from raw data (or creates new ones).  This
 * class has been created to let subclasses define further parsing for headers
 * known to be interesting.  The default implementation simply returns
 * the basic HeaderField class.
 */
public class HeaderFieldFactory
{

  /**
   * Create a new HeaderField based on the name.  SUbclasses
   * should override to provide more typed implementations.
   *
   * @param mixedCaseName the name of the header
   *
   * @return a new HeaderField with the given name
   */
  protected HeaderField createHeaderField(String mixedCaseName) {
    return new HeaderField(mixedCaseName,
      new LCString(mixedCaseName));
  }


  /**
   * Create a new Headers, with the given contents
   * and source.  Subclasses may wish to override to
   * provide more typed implementation.
   *
   * @param source the MIMESource from-which the headers
   *        were read (assumed to be shared).
   * @param sourceStart the start of the headers
   *        within the source
   * @param sourceLen the length within source
   *        of the Header bytes
   * @param headersInOrder the HeaderFields as
   *        found (order preserved).
   * @param headersByName a map of HeaderFields
   *        by name
   *
   * @return a new Headers (or subclass).
   */
  protected Headers createHeaders(MIMESource source,
    int sourceStart,
    int sourceLen,
    List<HeaderField> headersInOrder,
    Map<LCString, List<HeaderField>> headersByName) {

    return new Headers(this,
      source,
      sourceStart,
      sourceLen,
      headersInOrder,
      headersByName);

  }


  /**
   * Helper method.  Reads a HeaderFieldName from the Buffer.
   * <p>
   * Returns null if a header name (key) cannot be found, meaning there
   * was nothing before the ":", or there was no ":".  If not found, the Buffer
   * is reset.  Otherwise, the Buffer is advanced past the colon (":") and any
   * LWS
   */
  public static String readHeaderFieldName(ByteBuffer buf) {

    buf.mark();
    String headerFieldName = readString(buf,
      (byte) COLON,
      false);

    if(headerFieldName == null) {
      buf.reset();
      return null;
    }
    eatWhitespace(buf, false);

    return headerFieldName;
  }

}