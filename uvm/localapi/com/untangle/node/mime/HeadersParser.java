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

import java.io.*;
import java.nio.*;
import java.util.*;

import org.apache.log4j.Logger;

/**
 * Class to parse headers.
 * <p>
 * This class exists because of classloader issues and Log4J (I'd have preferred
 * to make it a static method of Headers).
 * <p>
 * This class maintains no state, and may be reused.
 */
public class HeadersParser {
    private final Logger m_logger = Logger.getLogger(HeadersParser.class);

    /**
     * When concluded, the Stream will be advanced beyond the
     * CRLF which ended the headers.
     */
    public Headers parseHeaders(MIMEParsingInputStream stream,
                                MIMESource streamSource,
                                HeaderFieldFactory fieldFactory,
                                MIMEPolicy policy)
        throws IOException,
               InvalidHeaderDataException,
               HeaderParseException {

        m_logger.debug("Begin Parsing Headers");

        int startPos = (int) stream.position();//TODO: bscott Someday w/ big emails this cast will haunt me.

        List<HeaderField> headersInOrder = new ArrayList<HeaderField>();
        Map<LCString, List<HeaderField>> headersByName = new HashMap<LCString, List<HeaderField>>();

        List<Line> allLines = new ArrayList<Line>();

        //These members are reset each time we encounter a new header
        List<Line> currentLines = null;
        int valueStartOffset = 0;
        String headerName = null;
        int linesRead = 0;

        try {
            Line line;
            ByteBuffer bb;

            while(true) {
                //Assume this is either a blank line, or the start of a header
                line = stream.readLine(policy.getMaxHeaderLineLen());
                //       m_logger.debug("(read line) \"" + bbToString(line.getBuffer()) + "\"");
                if(line == null) {
                    break;
                }

                //Check for policy violation
                if(linesRead++ > policy.getMaxHeaderLines()) {
                    //XXXX bscott I think this exception should be some "policy violation"
                    //            or "attack suspected" exception.
                    throw new InvalidHeaderDataException("Number of header lines exceeded: \"" +
                                                         policy.getMaxHeaderLines() + "\", \n\"" +
                                                         Line.linesToString((Line[]) allLines.toArray(new Line[allLines.size()]), 0, false) + "\"");
                }

                //Get the byte buffer for this line.  Check if it
                //is blank, starts with LWS, or is "normal".
                bb = line.getBuffer(false);

                if(bb.remaining() == 0) {
                    //Blank line.  This terminates the set of headers.  Note if there
                    //was a trailing header, it is dealt with outside this loop.
                    break;
                }
                if(isNextLWS(bb)) {//BEGIN Starts with LWS
                    //Check boundary case where entire line is LWS
                    if(isAllLWS(bb)) {//BEGIN All Blank Line
                        if(policy.isLwsLineTerminatesHeaders()) {
                            //OK.  We'll treat this (bad) line as a terminator
                            //for the header set
                            m_logger.warn("Encountered a LWS line while parsing headers.  " +
                                          "Treating as blank line as per policy (line " + linesRead + ")");
                            break;
                        }
                        else {
                            //We either need to append to the previous
                            //Line, or this was the first line in which case we
                            //ignore it.
                            if(currentLines == null) {
                                m_logger.warn("Encountered a LWS line as first line in headers.  Ignore " +
                                              "(line " + linesRead + ")");
                            }
                            else {
                                m_logger.warn("Encountered a LWS line in headers.  Append " +
                                              "to previous header as-per policy (line " + linesRead + ")");
                                allLines.add(line);
                                currentLines.add(line);
                            }
                            continue;//Redundant
                        }
                    }//ENDOF All Blank Line
                    else {//BEGIN Not all Blank line
                        eatWhitespace(bb, false);

                        if(currentLines == null) {//BEGIN First Line not all LWS
                            //Odd case.  This nasty line is the first line of the headers.
                            //By policy, we *may* consider this part of the
                            //headers, or body.  Note we break out granular cases below
                            //for better logging.
                            String badHeaderName = HeaderFieldFactory.readHeaderFieldName(bb);
                            if(badHeaderName == null) {
                                //We cannot consider this a header line, regardless of policy
                                stream.unreadLine(line);
                                m_logger.warn("Encountered a non-conformant header line as first line " +
                                              "of header set: \"" + bbToString(line.getBuffer(false)) + "\"" +
                                              ".  Consider this start of the body (line " + linesRead + ")");
                                break;
                            }
                            else {
                                if(policy.isIgnoreFoldedFirstLine()) {
                                    m_logger.warn("As per policy, starting Headers with ill-formed " +
                                                  "first line: \"" + bbToString(line.getBuffer(false)) +
                                                  "\" (line " + linesRead + ")");

                                    allLines.add(line);

                                    //Start a new header
                                    currentLines = new ArrayList<Line>();
                                    currentLines.add(line);
                                    valueStartOffset = bb.position();
                                    headerName = badHeaderName;
                                    continue;//redundant
                                }
                                else {
                                    m_logger.warn("Encountered folded line as first line of headers " +
                                                  "which may be a header line: \"" + bbToString(line.getBuffer(false)) + "\"" +
                                                  ".  Treat this as part of the body (line " + linesRead + ")");
                                    stream.unreadLine(line);
                                    break;
                                }
                            }
                        }//ENDOF First Line not all LWS
                        else {
                            allLines.add(line);
                            currentLines.add(line); //Append this fold to the current
                            continue;//redundant
                        }
                    }//ENDOF Not all Blank line
                }//ENDOF Starts with LWS
                else {//BEGIN Starts with non LWS
                    //Complete previous header, if it exists
                    if(currentLines != null) {
                        HeaderField newField = fieldFactory.createHeaderField(headerName);
                        newField.assignFromLines(new RawHeaderField(
                                                                    (Line[]) currentLines.toArray(new Line[currentLines.size()]), valueStartOffset),
                                                 false);
                        headersInOrder.add(newField);
                        List<HeaderField> headerHolder = headersByName.get(newField.getNameLC());
                        if(headerHolder == null) {
                            headerHolder = new ArrayList<HeaderField>();
                            headersByName.put(newField.getNameLC(), headerHolder);
                        }
                        headerHolder.add(newField);
                        m_logger.debug("Added HeaderField with name \"" + newField.getName() + "\"");
                        currentLines = null;
                    }
                    //Read the header name
                    headerName = HeaderFieldFactory.readHeaderFieldName(bb);
                    if(headerName == null) {
                        //Edge case.  We either ignore this line alltogether,
                        //or consider it the start of the body
                        switch(policy.getNonHeaderLineInHeadersPolicy()) {
                        case TREAT_AS_BODY:
                            m_logger.warn("Encountered non-header line in headers: \"" +
                                          bbToString(line.getBuffer(false)) + "\".  Treat as part " +
                                          "of body as-per policy (line " + linesRead + ")");
                            stream.unreadLine(line);
                            break;
                        case IGNORE:
                            m_logger.warn("Encountered non-header line in headers: \"" +
                                          bbToString(line.getBuffer(false)) + "\".  Ignore" +
                                          " as-per policy (line " + linesRead + ")");
                            break;
                        case RAISE_EXCEPTION:
                            //XXXX bscott I think this exception should be some "policy vioilation"
                            //            or "attack suspected" exception.
                            throw new InvalidHeaderDataException("Invalid line: \"" +
                                                                 bbToString(line.getBuffer(false)) + " Encountered (line " + linesRead + ")");
                        }
                        break;
                    }
                    else {
                        allLines.add(line);

                        //Start a new header
                        currentLines = new ArrayList<Line>();
                        currentLines.add(line);
                        valueStartOffset = bb.position();
                    }
                }//ENDOF Starts with non LWS
            }
        }
        catch(LineTooLongException ltl) {
            throw new InvalidHeaderDataException("Header line exceeds length \"" +
                                                 policy.getMaxHeaderLineLen() + "\" set by policy (line " + linesRead + ")", ltl);
        }
        //Clean-up the remaing header
        if(currentLines != null) {
            HeaderField newField = fieldFactory.createHeaderField(headerName);
            newField.assignFromLines(new RawHeaderField(
                                                        (Line[]) currentLines.toArray(new Line[currentLines.size()]), valueStartOffset),
                                     false);
            headersInOrder.add(newField);
            List<HeaderField> headerHolder = headersByName.get(newField.getNameLC());
            if(headerHolder == null) {
                headerHolder = new ArrayList<HeaderField>();
                headersByName.put(newField.getNameLC(), headerHolder);
            }
            headerHolder.add(newField);
            m_logger.debug("Added HeaderField with name \"" + newField.getName() + "\"");
        }

        allLines.clear();
        allLines = null;

        return fieldFactory.createHeaders(streamSource,
                                          startPos,
                                          (int) (stream.position() - startPos),
                                          headersInOrder,
                                          headersByName);
    }
}
