#!/bin/sh

# Copyright (c) 2004, 2005 Metavize Inc.
# All rights reserved.
#
# This software is the confidential and proprietary information of
# Metavize Inc. ("Confidential Information").  You shall
# not disclose such Confidential Information.
#
# $Id$


# ==============================================
# Functions
# ==============================================


# ------------------------------------------
function trace() {
  [ $VERBOSE -eq 0 ] || echo "[VERBOSE] "$*
}

# ------------------------------------------
function punt() {
  echo "[ERROR]" $*
  exit 100
}

# ------------------------------------------
function doHelp() {
  echo "$0 [-h] -ts (arg) [-o]"
  echo "  -h help (this)"
  echo "  -t type of output (chunk|pdf|html)"
  echo "  -s source file"
  echo "  -o output dir"
  echo "  -v verbose"
  echo ""
  echo "  Note that the recommended setup is to maintain"
  echo "  source XML file(s) under a directory called \"src\""
  echo "  with any custom images in a directory called"
  echo "  \"figure\".  Output is usualy a peer of \"src\", and called"
  echo "  \"output\""
}

# ------------------------------------------
function mkdirimpl() {
  trace "Creating directory $1"
  mkdir -p $1
}

# ------------------------------------------
function doHtml() {
  trace "Making HTML"
  OUTPUT_DIR=$OUTPUT_ROOT/html
  [ -d $OUTPUT_DIR ] || mkdirimpl $OUTPUT_DIR

  copyImages
  
  # Copy over the CSS
  cp -u ${DIST_HOME}/mvstyle/metavize.css ${OUTPUT_DIR}

  echo "I never finished this -wrs"
}

# ------------------------------------------
function doPdf() {
  trace "Making PDF"
  OUTPUT_DIR=$OUTPUT_ROOT/pdf
  [ -d $OUTPUT_DIR ] || mkdirimpl $OUTPUT_DIR

  copyImages

  trace "Invoke transform.  This may take a minute"
  $JAVA_HOME/bin/java -classpath $SAXON_CLASSPATH com.icl.saxon.StyleSheet \
    -o ${OUTPUT_DIR}/output.fo \
    $SOURCE_FILE $FO_XSL \
    profile.condition=pdf
  [ $? -gt 0 ] && punt "Error invoking transform"

  trace "Convert FO to PDF"
  $XEP -fo ${OUTPUT_DIR}/output.fo -out ${OUTPUT_DIR}/output.pdf
  [ $? -gt 0 ] && punt "Error invoking transform"
  
  # Delete the bogus image directories - we don't need them
  rm -rf ${OUTPUT_DIR}/images
  rm -rf ${OUTPUT_DIR}/figure
  rm ${OUTPUT_DIR}/output.fo

  echo "Finished PDF can be found at ${OUTPUT_DIR}/output.pdf"
}

# ------------------------------------------
function doChunk() {
  trace "Making Chunked HTML"
  OUTPUT_DIR=$OUTPUT_ROOT/chunk
  [ -d $OUTPUT_DIR ] || mkdirimpl $OUTPUT_DIR

  copyImages

  # Copy over the CSS
  cp -u ${DIST_HOME}/mvstyle/metavize.css ${OUTPUT_DIR}

  trace "Invoke transform.  This may take a minute"
  $JAVA_HOME/bin/java -classpath $SAXON_CLASSPATH com.icl.saxon.StyleSheet \
    $SOURCE_FILE \
    $CHUNK_XSL \
    profile.condition=chunk \
    base.dir=${OUTPUT_DIR}/
  [ $? -gt 0 ] && punt "Error invoking transform"
}

# ------------------------------------------
function copyImages() {
  [ -d ${OUTPUT_DIR}/images ] || mkdirimpl ${OUTPUT_DIR}/images

  # Copy-over the images needed from the style sheets
  cp -uR ${DOCBK_DIR}/images/ ${OUTPUT_DIR}
  
  # Copy-over images/figures unique to this document
  if [ -d $SOURCE_DIR/figure ]
  then
    trace "Copy figures from $SOURCE_DIR/figure to ${OUTPUT_DIR}"
    cp -uR $SOURCE_DIR/figure/ ${OUTPUT_DIR}
  else
    trace "No figures to copy"
  fi
}

# ==============================================
# "Main"
# ==============================================

JAVA_HOME=${JAVA_HOME:-"/opt/java/jre"}

# Variables/locations for the book
# distribution
REL_PATH=`dirname $0`
pushd $REL_PATH > /dev/null 2>&1
DIST_HOME=${PWD}
popd > /dev/null 2>&1

DOCBK_DIR=$DIST_HOME/docbook-xsl-1.69.1
SAXON_CLASSPATH=$DOCBK_DIR/extensions/saxon65.jar:$DIST_HOME/saxon65/saxon.jar
XEP_HOME=$DIST_HOME/XEP

CHUNK_XSL=$DIST_HOME/mvstyle/mv_book_chunk.xsl
FO_XSL=$DOCBK_DIR/fo/docbook.xsl
HTML_XSL=$DOCBK_DIR/html/chunk.xsl

XEP_CP="$JAVA_HOME/lib/tools.jar:$XEP_HOME/lib/xep.jar:$XEP_HOME/lib/saxon.jar:$XEP_HOME/lib/xt.jar"

XEP="$JAVA_HOME/bin/java -classpath $XEP_CP -Dcom.renderx.xep.CONFIG=$XEP_HOME/xep.xml com.renderx.xep.XSLDriver"



# Variables
OUTPUT_TYPE="pdf"
OUTPUT_ROOT=""
SOURCE_FILE=""
SOURCE_DIR=""
OUTPUT_DIR=
VERBOSE=0

while getopts t:ho:s:v arg
do
  case $arg in
    t) OUTPUT_TYPE="$OPTARG";;
    h) doHelp;exit 0;;
    o) OUTPUT_ROOT="$OPTARG";;
    s) SOURCE_FILE="$OPTARG";;
    v) VERBOSE=1;;
  esac
done


[ -f $SOURCE_FILE ] || punt "Please provide a valid source file"


# Get the directory of the source file, so we can
# find any images
SRC_DIR_REL=`dirname $SOURCE_FILE`
pushd $SRC_DIR_REL  > /dev/null 2>&1
SOURCE_DIR=${PWD}
popd  > /dev/null 2>&1

# Prepare the root of the output directory
if [ -z "$OUTPUT_ROOT" ]; then
  OUTPUT_ROOT=${SOURCE_DIR}/../output
fi

[ -d $OUTPUT_ROOT ] || mkdirimpl $OUTPUT_ROOT

pushd $OUTPUT_ROOT  > /dev/null 2>&1
OUTPUT_ROOT=${PWD}
popd  > /dev/null 2>&1

[ -d $OUTPUT_ROOT ] || mkdirimpl $OUTPUT_ROOT

# Verbose-ness
trace SOURCE_FILE $SOURCE_FILE
trace OUTPUT_TYPE $OUTPUT_TYPE
trace OUTPUT_ROOT $OUTPUT_ROOT

case $OUTPUT_TYPE in
  pdf) doPdf;;
  chunk) doChunk;;
  html) doHtml;;
esac

exit 0

