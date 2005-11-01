<?xml version="1.0" encoding="iso-8859-1"?>

<!-- >e-novative> DocBook Environment (eDE)                                  -->
<!-- (c) 2002 e-novative GmbH, Munich, Germany                               -->
<!-- http://www.e-novative.de                                                -->

<!-- e-novative configuration for books                                      -->

<!-- This file is part of eDE                                                -->

<!-- eDE is free software; you can redistribute it and/or modify             -->
<!-- it under the terms of the GNU General Public License as published by    -->
<!-- the Free Software Foundation; either version 2 of the License, or       -->
<!-- (at your option) any later version.                                     -->

<!-- eDE is distributed in the hope that it will be useful,                  -->
<!-- but WITHOUT ANY WARRANTY; without even the implied warranty of          -->
<!-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           -->
<!-- GNU General Public License for more details.                            -->

<!-- You should have received a copy of the GNU General Public License       -->
<!-- along with eDe; if not, write to the Free Software                      -->
<!-- Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA -->


<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">


<!-- General configuration (all output formats)                              -->


<!-- Enumerate parts (<part>)                                                -->
<!-- value: 0: parts are unnumbered                                          -->
<!-- value: 1: parts are numbered (with roman numbers)                       -->
<!--<xsl:param name="part.autolabel">0</xsl:param>-->
<xsl:param name="part.autolabel">1</xsl:param>


<!-- Enumerate chapters (<chapter>)                                          -->
<!-- value: 0: chapters are unnumbered                                       -->
<!-- value: 1: chapters are numbered (with arabic numbers)                   -->
<!--<xsl:param name="chapter.autolabel">0</xsl:param>-->
<xsl:param name="chapter.autolabel">1</xsl:param>


<!-- Reset the chapter numbers for each part                                 -->
<!-- applies to: all output formats (book only)                              -->
<!-- value: 0: chapters are numbered sequentiall thoughout the book          -->
<!-- value: 1: chapters numbers start with 1 for each part                   -->
<!--<xsl:param name="label.from.part">0</xsl:param>-->
<xsl:param name="label.from.part">1</xsl:param>


<!-- Enumerate the preface (<preface>)                                       -->
<!-- Enumerates the preface as if it were a chapter                          -->
<!-- value: 0: preface is unnumbered                                         -->
<!-- value: 1: preface is numbered                                           -->
<xsl:param name="preface.autolabel">0</xsl:param>
<!--<xsl:param name="preface.autolabel">1</xsl:param>-->


<!-- Enumerate appendices (<appendix>)                                       -->
<!-- Enumerates appendices as if it were a chapter                           -->
<!-- value: 0: appendices are unnumbered                                     -->
<!-- value: 1: appendices are numbered                                       -->
<!--<xsl:param name="appendix.autolabel">0</xsl:param>-->
<xsl:param name="appendix.autolabel">1</xsl:param>


<!-- Number of levels displayed in a table of contents (<sect1> - <sect5>)   -->
<!-- This controls the table of contents depth, not where they are created.  -->
<!-- Value: number                                                           -->
<!--<xsl:param name="toc.section.depth">0</xsl:param>-->
<xsl:param name="toc.section.depth">4</xsl:param>
<!--<xsl:param name="toc.section.depth">2</xsl:param>-->
<!--<xsl:param name="toc.section.depth">3</xsl:param>-->
<!--<xsl:param name="toc.section.depth">4</xsl:param>-->
<!--<xsl:param name="toc.section.depth">5</xsl:param>-->


<!-- Section level to display a table of contents for (<sect1> - <sect5>)    -->
<!-- This controls where to display a table of contents, not the depth.      -->
<!-- If 0, <sect1> has no table of contents, resulting in no table of        -->
<!-- contents (articles), or a table of contents for each chapter (books)    -->
<!--<xsl:param name="generate.section.toc.level">0</xsl:param>-->
<xsl:param name="generate.section.toc.level">1</xsl:param>
<!--<xsl:param name="generate.section.toc.level">2</xsl:param>-->
<!--<xsl:param name="generate.section.toc.level">3</xsl:param>-->
<!--<xsl:param name="generate.section.toc.level">4</xsl:param>-->
<!--<xsl:param name="generate.section.toc.level">5</xsl:param>-->


<!-- HTML configuration                                                      -->




<!-- FO- (PDF) related configuration                                         -->


<!-- page sided layout                                                       -->
<!-- 0: single-sided-layout (page numbers are centered)                      -->
<!-- 1: double-sided layout (page numbers alternate at left and right)       -->
<!--<xsl:param name="double.sided">0</xsl:param>-->
<xsl:param name="double.sided">1</xsl:param>






<!-- hyphenation                                                           -->
<!-- a language attribute ("lang") must be defined for top level element   -->
<!-- the most common languages are supported (see c:\docbook\fop\hyph)     -->
<!-- applies to: fo                                                        -->
<!-- false: words are not hyphenated                                       -->
<!-- true: words are hyphenated                                            -->
<xsl:param name="hyphenate">true</xsl:param>

<!-- text alignment -->
<!-- left, right, justify -->
<xsl:param name="alignment">justify</xsl:param>

<!-- number of columns in text body                                        -->
<!-- value: number of columns -->
<xsl:param name="column.count.body">1</xsl:param>

<!-- default font for pdf -->
<xsl:param name="body.font.family">Times</xsl:param>





<!-- add part/chapter label to section label                                 -->
<!-- applies to: html, fo                                                    -->
<!-- value: 0: regular section labels                                        -->
<!-- value: 1: add part/chapter labels to section label                      -->
<xsl:param name="section.label.includes.component.label">1</xsl:param>
<!-- DOES NOT SEEM TO WORK -->




</xsl:stylesheet>
