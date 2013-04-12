/*
 * $HeadURL$
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

import static com.untangle.node.util.ASCIIUtil.bbToString;
import static com.untangle.node.util.ASCIIUtil.eatWhitespace;
import static com.untangle.node.util.ASCIIUtil.isAllLWS;
import static com.untangle.node.util.ASCIIUtil.isNextLWS;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                                                         Line.linesToString(allLines.toArray(new Line[allLines.size()]), 0, false) + "\"");
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
                        newField.assignFromLines(new RawHeaderField(currentLines.toArray(new Line[currentLines.size()]), valueStartOffset), false);
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
            newField.assignFromLines(new RawHeaderField(currentLines.toArray(new Line[currentLines.size()]), valueStartOffset), false);
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
