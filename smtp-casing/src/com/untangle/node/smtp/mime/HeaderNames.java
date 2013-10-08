/*
 * $HeadURL: svn://chef/work/src/smtp-casing/src/com/untangle/node/smtp/mime/HeaderNames.java $
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.smtp.mime;

/**
 * Constants for popular header names.
 */
public interface HeaderNames
{

    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
    public static final String SUBJECT = "Subject";
    public static final String TO = "to";
    public static final String CC = "cc";
    public static final String FROM = "from";
    public static final String CONTENT_DISPOSITION = "Content-Disposition";
    public static final String DATE = "Date";
    public static final String MIME_VERSION = "Mime-Version";

    // Composite primary types
    public static final String MULTIPART_PRIM_TYPE_STR = "multipart";
    public static final String MESSAGE_PRIM_TYPE_STR = "message";

    // discrete primary types
    public static final String TEXT_PRIM_TYPE_STR = "text";
    public static final String IMAGE_PRIM_TYPE_STR = "image";
    public static final String AUDIO_PRIM_TYPE_STR = "audio";
    public static final String VIDEO_PRIM_TYPE_STR = "video";
    public static final String APPLICATION_PRIM_TYPE_STR = "application";

    // popular subtypes
    public static final String PLAIN_SUB_TYPE_STR = "plain";
    public static final String HTML_SUB_TYPE_STR = "html";
    public static final String RFC222_SUB_TYPE_STR = "rfc822";
    public static final String MIXED_SUB_TYPE_STR = "mixed";
    public static final String ALTERNATIVE_SUB_TYPE_STR = "alternative";
    
    public static final String ATTACHMENT_DISPOSITION_STR= "ATTACHMENT";
    public static final String INLINE_DISPOSITION_STR = "inline";

    public static final String MULTIPART_MIXED = MULTIPART_PRIM_TYPE_STR + "/" + MIXED_SUB_TYPE_STR;
    public static final String MESSAGE_RFC822 = MESSAGE_PRIM_TYPE_STR + "/" + RFC222_SUB_TYPE_STR;
    public static final String TEXT_PLAIN = TEXT_PRIM_TYPE_STR + "/" + PLAIN_SUB_TYPE_STR;
    public static final String TEXT_HTML = TEXT_PRIM_TYPE_STR + "/" + HTML_SUB_TYPE_STR;
    public static final String MULTIPART_ALTERNATIVE = MULTIPART_PRIM_TYPE_STR + "/" + ALTERNATIVE_SUB_TYPE_STR;
    
    
    
    public static final String SEVEN_BIT_STR = "7bit";
    public static final String EIGHT_BIT_STR = "8bit";
    public static final String BINARY_STR = "binary";
    public static final String QP_STR = "quoted-printable";
    public static final String BASE64_STR = "base64";
    public static final String UUENCODE_STR = "uuencode";
    public static final String UUENCODE_STR_ALT = "x-uuencode";
    public static final String UNKNOWN_STR = BINARY_STR;


}
