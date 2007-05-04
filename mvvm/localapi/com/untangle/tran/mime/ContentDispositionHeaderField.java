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

//TODO: bscott Make sure we also set the "name" attribute,
//      for compatability with crappy MUAs

/**
 * Object representing a "Content-Disposition" Header as found in an
 * RFC 821/RFC 2045 document.
 */
public class ContentDispositionHeaderField
    extends HeaderFieldWithParams {

    public static final String FILENAME_PARAM_NAME = "filename";
    public static final String CREATION_DATE_PARAM_NAME = "creation-date";
    public static final String MODIFICATION_DATE_PARAM_NAME = "modification-date";
    public static final String READ_DATE_PARAM_NAME = "read-date";
    public static final String SIZE_PARAM_NAME = "size";
    public static final String NAME_PARAM_NAME = "name";

    public static final LCString FILENAME_PARAM_NAME_KEY = new LCString(FILENAME_PARAM_NAME);
    public static final LCString CREATION_DATE_PARAM_NAME_KEY = new LCString(CREATION_DATE_PARAM_NAME);
    public static final LCString MODIFICATION_DATE_PARAM_NAME_KEY = new LCString(MODIFICATION_DATE_PARAM_NAME);
    public static final LCString READ_DATE_PARAM_NAME_KEY = new LCString(READ_DATE_PARAM_NAME);
    public static final LCString SIZE_PARAM_NAME_KEY = new LCString(SIZE_PARAM_NAME);
    public static final LCString NAME_PARAM_NAME_KEY = new LCString(NAME_PARAM_NAME);

    public static final String ATTACH_VAL = "attachment";
    public static final String INLINE_VAL = "inline";

    /**
     * Enum of the only two legal "disposition type" values.
     * If not specified or of an unknown string, implicitly converted
     * to "ATTACH"
     */
    public enum DispositionType {
        ATTACH,
        INLINE
    }

    private DispositionType m_dispType;

    public ContentDispositionHeaderField(String name) {
        super(name, HeaderNames.CONTENT_DISPOSITION_LC);
    }
    public ContentDispositionHeaderField() {
        super(HeaderNames.CONTENT_DISPOSITION, HeaderNames.CONTENT_DISPOSITION_LC);
    }


    /**
     * Get the DispositionType as defined by this
     * header.  As-per RFC2183, this defaults to
     * "attachment".
     *
     * @return the DispositionType
     */
    public DispositionType getDispositionType() {
        return m_dispType;
    }

    /**
     * Set the DispositionType as defined by this
     * header.  Note that converting from ATTACH
     * to INLINE does not implicitly remove
     * the {@link #getFilename filename} attribute.
     *
     * @param type the DispositionType
     */
    public void setDispositionType(DispositionType type) {
        m_dispType = type;
        changed();
    }

    /**
     * Note that an attachment type of "inline" <b>with</b>
     * a FileName is considered an attachment.  This was a bug
     * with some stupid mailer in the past.
     */
    public boolean isAttachment() {

        return m_dispType == DispositionType.ATTACH ||
            getFilename() != null;
    }

    /**
     * May be null, even if {@link #isAttachment isAttachment}
     * is true.
     */
    public String getFilename() {
        return getParam(FILENAME_PARAM_NAME_KEY);
    }
    /**
     * Set the filename attribute.  Note that this does
     * <b>not</b> implicitly set the {@link #getDispositionType DispositionType}
     * to ATTACH.  Passing null implicitly removes
     * this parameter.
     *
     * @param filename the name of the file
     */
    public void setFilename(String filename) {
        //Note that passing null to the
        //base class is an implicit remove
        setParam(FILENAME_PARAM_NAME, filename);
        changed();
    }


    /**
     * Converts the DispositionType to a String.
     */
    public static String dispositionTypeToString(DispositionType distType) {
        if(distType == null) {
            return "null";
        }
        return distType == ContentDispositionHeaderField.DispositionType.ATTACH?
            ATTACH_VAL:INLINE_VAL;
    }

    @Override
    protected ParamParsePolicy getParamParsePolicy(String paramName) {
        paramName = paramName.toLowerCase();
        if(
           FILENAME_PARAM_NAME_KEY.str.equals(paramName) ||
           CREATION_DATE_PARAM_NAME_KEY.str.equals(paramName) ||
           MODIFICATION_DATE_PARAM_NAME_KEY.str.equals(paramName) ||
           READ_DATE_PARAM_NAME_KEY.str.equals(paramName) ||
           NAME_PARAM_NAME_KEY.str.equals(paramName)) {
            return ParamParsePolicy.LOOSE;
        }
        if(SIZE_PARAM_NAME_KEY.str.equals(paramName)) {
            return ParamParsePolicy.ATOM_OR_QTEXT;
        }
        return super.getParamParsePolicy(paramName);
    }

    @Override
    protected void parsePrimaryValue(HeaderFieldTokenizer t)
        throws HeaderParseException {
        HeaderFieldTokenizer.Token token = t.nextTokenIgnoreComments();
        if(token == null ||
           (token.getType() != HeaderFieldTokenizer.TokenType.QTEXT &&
            token.getType() != HeaderFieldTokenizer.TokenType.ATOM) ) {
            throw new HeaderParseException("Illegal Content-Disposition header \"" +
                                           t.getOriginal() + "\"");
        }
        m_dispType = DispositionType.ATTACH;
        if(INLINE_VAL.equals(token.toString().trim().toLowerCase())) {
            m_dispType = DispositionType.INLINE;
        }
    }

    @Override
    protected void writePrimaryValue(StringBuilder sb) {
        sb.append(dispositionTypeToString(getDispositionType()));
    }
}
