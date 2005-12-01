<?xml version="1.0" encoding="iso-8859-1"?>

<!--
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * Note that this file was modeled after a whole mess of tangled
 * templates from
 *
 * >e-novative> DocBook Environment (eDE)
 * http://www.e-novative.de
 *
 * $Id$
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0"
                xmlns="http://www.w3.org/TR/xhtml1/transitional">

<xsl:import href="../../../../../mvdocbook/xsl/xhtml/profile-chunk.xsl" />


<xsl:param name="section.autolabel">1</xsl:param>
<xsl:param name="qandadiv.autolabel">1</xsl:param>
<xsl:param name="generate.index">1</xsl:param>
<xsl:param name="paper.type">A4</xsl:param>
<xsl:param name="page.orientation">portrait</xsl:param>
<xsl:param name="header.rule">1</xsl:param>
<xsl:param name="suppress.footer.navigation">0</xsl:param>
<xsl:param name="footer.rule">1</xsl:param>


<!-- Section level to create individual pages (chunks) for                   -->
<!-- Note that for chapters always an individual page is created             -->
<!-- applies to: html chunked output only                                    -->
<!-- value: number: section level                                            -->

<!--<xsl:param name="chunk.section.depth">1</xsl:param>-->
<!--<xsl:param name="chunk.section.depth">2</xsl:param>-->
<xsl:param name="chunk.section.depth">3</xsl:param>
<!--<xsl:param name="chunk.section.depth">4</xsl:param>-->
<!--<xsl:param name="chunk.section.depth">5</xsl:param>-->


<!-- chunk-specific settings -->


<!-- create new page for top-level units                                     -->
<!-- applies to: ???                                                         -->
<!-- effectively creates an extra page for each table of contents            -->
<!-- value: 0: first section on same page as toc                             -->
<!-- value: 1: first section new page (toc on individual page)               -->
<xsl:param name="chunk.first.sections">1</xsl:param>


<!-- Use character entities instead of numeric entities where possible       -->
<xsl:param name="saxon.character.representation">native</xsl:param>


<!-- multiple profile values are semicolon-separated                         -->
<!-- applies to: html, fo                                                    -->
<xsl:param name="profile.separator">;</xsl:param>


<!-- profiling: limit output to given attribute values                     -->
<!-- imagine profiling as "filtering" out elements with certain attributes -->
<!-- from the full docbook source. this requires adding attributes         -->
<!-- to some elements, e.g. <para os="win98">                              -->
<!-- a param line must be present here for every profiled attribute        -->
<!-- individual entries are separated by the "profile.separator",          -->
<!-- by default the separator is a semicolon                               -->
<!-- applies to: all                                                       -->
<!-- value: list of attributes appearing in the document                   -->
<xsl:param name="profile.arch"></xsl:param>
<xsl:param name="profile.condition"></xsl:param>
<xsl:param name="profile.conformance"></xsl:param>
<xsl:param name="profile.lang"></xsl:param>
<!-- operating system: e.g. "Windows" or "Linux"                           -->
<xsl:param name="profile.os"></xsl:param>
<xsl:param name="profile.revision"></xsl:param>
<xsl:param name="profile.revisionflag"></xsl:param>
<xsl:param name="profile.role"></xsl:param>
<xsl:param name="profile.security"></xsl:param>
<!-- userlevel: e.g. "beginner", "advanced"                                -->
<xsl:param name="profile.userlevel"></xsl:param>
<xsl:param name="profile.vendor"></xsl:param>
<xsl:param name="profile.attribute"></xsl:param>
<xsl:param name="profile.value"></xsl:param>
<!-- TBD: complete these examples -->



<!-- where to place titles for certain elements                            -->
<!-- do not quote element names and parameters                             -->
<!-- applies to: all                                                       -->
<!-- value: "elementname" before                                           -->
<!-- value: "elementname" after                                            -->
<xsl:param name="formal.title.placement">
figure after
example before
equation after
table after
procedure after
</xsl:param>


<!-- display headers on blank page                                         -->
<!-- applies to: html, fo                                                  -->
<!-- value: 0: hide headers on blank pages                                 -->
<!-- value: 1: show headers on blank pages                                 -->
<xsl:param name="headers.on.blank.pages">1</xsl:param>


<xsl:param name="menuchoice.menu.separator">-&gt;</xsl:param>
<xsl:param name="firstterm.only.link">0</xsl:param>


<!-- add reference purpose and reference entry to toc -->
<xsl:param name="annotate.toc">1</xsl:param>

<!-- render callout lists as definition lists -->
<xsl:param name="callout.list.table">0</xsl:param>


<!-- do not create draft documents -->
<xsl:param name="draft.mode" select="'no'"/>
<!--<xsl:param name="draft.watermark.image" /> -->

<!-- enable better table sizing -->
<xsl:param name="tablecolumns.extension" select="'1'"/>

<!-- bibliography entry separator -->
<xsl:param name="biblioentry.item.separator">. </xsl:param>

<!-- number bibliography entries-->
<xsl:param name="bibliography.numbered">1</xsl:param>

<!-- -->
<!--<xsl:param name="css.decoration">1</xsl:param>-->


<!-- create link to description (textobjects) for mediaobjects -->
<!-- value: 0: no link -->
<!-- value: 1: a link -->
<xsl:param name="html.longdesc">0</xsl:param>


<!-- draft watermark graphic -->
<xsl:param name="draft.watermark.image">images/draft.png</xsl:param>


<!-- create empty paragraphs for formatting-->
<!-- value: 0: no empty paragraphs -->
<!-- value: 1: create empty paragraphs -->
<xsl:param name="spacing.paras">0</xsl:param>

<!-- render segmented list as html table -->
<xsl:param name="segmentedlist.as.table">0</xsl:param>


<!-- render variable list as html table -->
<xsl:param name="variablelist.as.table">0</xsl:param>

<!-- glossterm auto link -->
<xsl:param name="glossterm.auto.link">1</xsl:param>


<!-- admonitions (caution, note, warning, important, tip)                    -->

<!-- use graphical symbols admonitions                                     -->
<!-- applies to: all                                                       -->
<!-- value: 0: use textual description for admonitions                     -->
<!-- value: 1: use graphical symbols for admonitions                       -->
<xsl:param name="admon.graphics">1</xsl:param>

<!-- generate numeric callouts                                             -->
<xsl:param name="callout.graphics">0</xsl:param>

<!--
 WRS: This controls which types of sections have what at the
      top (table of contents, etc).  For now, I don't like
      the "list of figures" as it is too long and adds
      little value.  To add it back, use a commma separated list
      such as

      book  toc,figure,table
-->        
<xsl:param name="generate.toc">
book   toc
chapter     toc
preface toc
sect1 toc
appendix toc
</xsl:param>


<!-- add page number to cross references -->
<xsl:param name="insert.xref.page.number">1</xsl:param>

<!-- Indent generated html                                                   -->
<!-- Not supported by all XSLT processors.                                   -->
<xsl:param name="chunker.output.indent">yes</xsl:param>


<!-- Use element id as HTML filename                                         -->
<xsl:param name="use.id.as.filename">1</xsl:param>


<!-- Generate valid HTML                                                     -->
<!-- Avoids creation of nested html paragraphs (<p>) from nested <para> tags -->
<!-- (in strict HTML, nested paragraphs are not allowed).                    -->
<xsl:param name="make.valid.html">1</xsl:param>


<!-- Clean up HTML                                                           -->
<!-- Try to create "better" HTML by transforming the result HTML. Does not   -->
<!-- work with all XSLT processors.                                          -->
<xsl:param name="html.cleanup">0</xsl:param>


<!-- Use XSLT processor extensions                                           -->
<!-- Enables extensions that offer functionality beyond regular XML proessor -->
<!-- capabilities. -->
<xsl:param name="use.extensions">1</xsl:param>

<!-- Use Tablecolumns extensions                                             -->
<!-- Improves HTML table display                                             -->
<xsl:param name="tablecolumns.extensions">0</xsl:param>


<!-- Use Graphicsize extensions                                              -->
<!-- Allows XSLT processor to retrieve the size from graphics. Does not work -->
<!-- with all XSLT processors.                                               -->
<xsl:param name="graphicsize.extensions">0</xsl:param>


<!-- Use Textinsert extensions                                               -->
<!-- Allows inserting text files directly into the XML source.               -->
<xsl:param name="textinsert.extensions">0</xsl:param>

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
<!-- WRS: Changed -->
<xsl:param name="toc.section.depth">2</xsl:param>
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


<!-- add part/chapter label to section label                                 -->
<!-- applies to: html, fo                                                    -->
<!-- value: 0: regular section labels                                        -->
<!-- value: 1: add part/chapter labels to section label                      -->
<xsl:param name="section.label.includes.component.label">1</xsl:param>
<!-- DOES NOT SEEM TO WORK -->

<!-- WRS: Added to make glossary prettier -->
<xsl:param name="glossary.as.blocks">1</xsl:param>

<!-- ENDOF inlined e-novative_book.xsl -->

<!-- Empty imports (nothing to in-line -->
<!--
<xsl:import href="custom.xsl" />
<xsl:import href="custom_book_chunk.xsl" />
-->

<!-- make all gui elements appear bold                                       -->
<!-- This adds html bold tags to each gui element (guibutton, guiicon,       -->
<!-- guilabel, guimenu, guimenuitem, guisubmenu)                             -->
<!-- applies to: html output only                                            -->
<!-- Comment the whole block out to make gui elements appear as regular      -->
<!-- text                                                                    -->

<xsl:template match="guibutton">
<b><xsl:call-template name="inline.charseq"/></b>
</xsl:template>

<xsl:template match="guiicon">
<b><xsl:call-template name="inline.charseq"/></b>
</xsl:template>

<xsl:template match="guilabel">
<b><xsl:call-template name="inline.charseq"/></b>
</xsl:template>

<xsl:template match="guimenu">
<b><xsl:call-template name="inline.charseq"/></b>
</xsl:template>

<xsl:template match="guimenuitem">
<b><xsl:call-template name="inline.charseq"/></b>
</xsl:template>

<xsl:template match="guisubmenu">
<b><xsl:call-template name="inline.charseq"/></b>
</xsl:template>


<!-- WRS: Added -->
<xsl:param name="html.stylesheet">metavize.css</xsl:param>

<!-- WRS: Added, then removed.  The graphics they provide are really stupid.  We could
          re-enable if we make our own
       
<xsl:param name="navig.graphics">1</xsl:param>
-->   
<xsl:param name="navig.showtitles">1</xsl:param>

<!-- WRS: Added to cause header at the top of page -->
<xsl:param name="suppress.header.navigation">0</xsl:param>

<!-- WRS: I prefer not to have a huge page for TOC, so split
          off the List of Figures/Tables (which for the life of
          me I cannot seem to supress)
-->          
<xsl:param name="chunk.toc.and.lots">1</xsl:param>

<!-- WRS: Split out leagl notice -->
<xsl:param name="generate.legalnotice.link">1</xsl:param>

<!-- WRS: Copyright junk.  Looked like crap so I nuked it -->
<!--
<xsl:template name="user.footer.content">
  <HR/><P class="copyright">&#x00A9; 2005  Metavize, Inc.</P>
</xsl:template>
-->

<!-- WRS: Crazy attempt to get fancy w/ the headers -->
<xsl:template name="header.navigation">
  <xsl:param name="prev" select="/foo"/>
  <xsl:param name="next" select="/foo"/>
  <xsl:param name="nav.context"/>

  <xsl:variable name="home" select="/*[1]"/>
  <xsl:variable name="up" select="parent::*"/>

  <xsl:variable name="row1" select="$navig.showtitles != 0"/>
  <xsl:variable name="row2" select="count($prev) &gt; 0
                                    or (count($up) &gt; 0 
          and generate-id($up) != generate-id($home)
                                        and $navig.showtitles != 0)
                                    or count($next) &gt; 0"/>

  <xsl:if test="$suppress.navigation = '0' and $suppress.header.navigation = '0'">
    <div class="navheader">
      <xsl:if test="$row1 or $row2">
        <table width="100%" summary="Navigation header">
          <xsl:if test="$row1">
            <tr>
              <th colspan="3" align="center">
                <!-- I "borrowed" this stuff from the KDE manuals -->
                <div style="background-image: url(figure/middle-10.png);width: 100%;height: 127px;">
                <!--
                  Comment this back in for an image to the right
                  <div style="position: absolute; right: 0px;">
                    <img src="figure/right.png" style="margin: 0px;" alt=""/>
                  </div>
                  -->
                  
                  <div style="position: absolute; left: 0px;">
                    <img src="figure/left.png" style="margin: 0px;" alt=""/>
                  </div>
                  
                  <div style="position: absolute; top: 25px; right: 20px; text-align: right; font-size: xx-large; font-weight: bold; text-shadow: rgb(255, 255, 255) 0px 0px 5px; color: rgb(68, 68, 68);">
                  <xsl:apply-templates select="." mode="object.title.markup"/>
                  </div>
                </div>
              
                
              </th>
            </tr>
          </xsl:if>

          <xsl:if test="$row2">
            <tr>
              <td width="20%" align="left">
                <xsl:if test="count($prev)>0">
                  <a accesskey="p">
                    <xsl:attribute name="href">
                      <xsl:call-template name="href.target">
                        <xsl:with-param name="object" select="$prev"/>
                      </xsl:call-template>
                    </xsl:attribute>
                    <xsl:call-template name="navig.content">
                      <xsl:with-param name="direction" select="'prev'"/>
                    </xsl:call-template>
                  </a>
                </xsl:if>
                <xsl:text>&#160;</xsl:text>
              </td>
              <th width="60%" align="center">
<!--
Commented out printing of the parent section.  Didn't look too cool          
                <xsl:choose>
                  <xsl:when test="count($up) > 0
          and generate-id($up) != generate-id($home)
                                  and $navig.showtitles != 0">
                    <xsl:apply-templates select="$up" mode="object.title.markup"/>
                  </xsl:when>
                  <xsl:otherwise>&#160;</xsl:otherwise>
                </xsl:choose>
-->                
              </th>
              <td width="20%" align="right">
                <xsl:text>&#160;</xsl:text>
                <xsl:if test="count($next)>0">
                  <a accesskey="n">
                    <xsl:attribute name="href">
                      <xsl:call-template name="href.target">
                        <xsl:with-param name="object" select="$next"/>
                      </xsl:call-template>
                    </xsl:attribute>
                    <xsl:call-template name="navig.content">
                      <xsl:with-param name="direction" select="'next'"/>
                    </xsl:call-template>
                  </a>
                </xsl:if>
              </td>
            </tr>
          </xsl:if>
        </table>
      </xsl:if>
      <xsl:if test="$header.rule != 0">
        <hr/>
      </xsl:if>
    </div>
  </xsl:if>
</xsl:template>




<!-- WRS: More crazy stuff for custom footer -->
<xsl:template name="footer.navigation">
  <xsl:param name="prev" select="/foo"/>
  <xsl:param name="next" select="/foo"/>
  <xsl:param name="nav.context"/>

  <xsl:variable name="home" select="/*[1]"/>
  <xsl:variable name="up" select="parent::*"/>

  <xsl:variable name="row1" select="count($prev) &gt; 0
                                    or count($up) &gt; 0
                                    or count($next) &gt; 0"/>

  <xsl:variable name="row2" select="($prev and $navig.showtitles != 0)
                                    or (generate-id($home) != generate-id(.)
                                        or $nav.context = 'toc')
                                    or ($chunk.tocs.and.lots != 0
                                        and $nav.context != 'toc')
                                    or ($next and $navig.showtitles != 0)"/>

  <xsl:if test="$suppress.navigation = '0' and $suppress.footer.navigation = '0'">
    <div class="navfooter">
      <xsl:if test="$footer.rule != 0">
        <hr/>
      </xsl:if>

      <xsl:if test="$row1 or $row2">
        <table width="100%" summary="Navigation footer">
          <xsl:if test="$row1">
            <tr>
              <td width="40%" align="left">
                <xsl:if test="count($prev)>0">
                  <a accesskey="p">
                    <xsl:attribute name="href">
                      <xsl:call-template name="href.target">
                        <xsl:with-param name="object" select="$prev"/>
                      </xsl:call-template>
                    </xsl:attribute>
                    <xsl:call-template name="navig.content">
                      <xsl:with-param name="direction" select="'prev'"/>
                    </xsl:call-template>
                  </a>
                </xsl:if>
                <xsl:text>&#160;</xsl:text>
              </td>
              <td width="20%" align="center">
                <xsl:choose>
                  <xsl:when test="count($up)>0">
                    <a accesskey="u">
                      <xsl:attribute name="href">
                        <xsl:call-template name="href.target">
                          <xsl:with-param name="object" select="$up"/>
                        </xsl:call-template>
                      </xsl:attribute>
                      <xsl:call-template name="navig.content">
                        <xsl:with-param name="direction" select="'up'"/>
                      </xsl:call-template>
                    </a>
                  </xsl:when>
                  <xsl:otherwise>&#160;</xsl:otherwise>
                </xsl:choose>
              </td>
              <td width="40%" align="right">
                <xsl:text>&#160;</xsl:text>
                <xsl:if test="count($next)>0">
                  <a accesskey="n">
                    <xsl:attribute name="href">
                      <xsl:call-template name="href.target">
                        <xsl:with-param name="object" select="$next"/>
                      </xsl:call-template>
                    </xsl:attribute>
                    <xsl:call-template name="navig.content">
                      <xsl:with-param name="direction" select="'next'"/>
                    </xsl:call-template>
                  </a>
                </xsl:if>
              </td>
            </tr>
          </xsl:if>

          <xsl:if test="$row2">
            <tr>
              <td width="40%" align="left" valign="top">
                <xsl:if test="$navig.showtitles != 0">
                  <xsl:apply-templates select="$prev" mode="object.title.markup"/>
                </xsl:if>
                <xsl:text>&#160;</xsl:text>
              </td>
              <td width="20%" align="center">
                <xsl:choose>
                  <xsl:when test="$home != . or $nav.context = 'toc'">
                    <a accesskey="h">
                      <xsl:attribute name="href">
                        <xsl:call-template name="href.target">
                          <xsl:with-param name="object" select="$home"/>
                        </xsl:call-template>
                      </xsl:attribute>
                      <xsl:call-template name="navig.content">
                        <xsl:with-param name="direction" select="'home'"/>
                      </xsl:call-template>
                    </a>
                    <xsl:if test="$chunk.tocs.and.lots != 0 and $nav.context != 'toc'">
                      <xsl:text>&#160;|&#160;</xsl:text>
                    </xsl:if>
                  </xsl:when>
                  <xsl:otherwise>&#160;</xsl:otherwise>
                </xsl:choose>

                <xsl:if test="$chunk.tocs.and.lots != 0 and $nav.context != 'toc'">
                  <a accesskey="t">
                    <xsl:attribute name="href">
                      <xsl:apply-templates select="/*[1]"
                                           mode="recursive-chunk-filename"/>
                      <xsl:text>-toc</xsl:text>
                      <xsl:value-of select="$html.ext"/>
                    </xsl:attribute>
                    <xsl:call-template name="gentext">
                      <xsl:with-param name="key" select="'nav-toc'"/>
                    </xsl:call-template>
                  </a>
                </xsl:if>
              </td>
              <td width="40%" align="right" valign="top">
                <xsl:text>&#160;</xsl:text>
                <xsl:if test="$navig.showtitles != 0">
                  <xsl:apply-templates select="$next" mode="object.title.markup"/>
                </xsl:if>
              </td>
            </tr>
          </xsl:if>
        </table>
      </xsl:if>
    </div>
    <div style="background-image: url(figure/bottom.png);width: 100%;height: 80px;">
    </div>
  </xsl:if>
</xsl:template>



<!-- WRS: There is a goofy duplicate title on the title page.  Nuke it -->
<xsl:template match="title" mode="titlepage.mode">
  <xsl:variable name="id">
    <xsl:choose>
      <!-- if title is in an *info wrapper, get the grandparent -->
      <xsl:when test="contains(local-name(..), 'info')">
        <xsl:call-template name="object.id">
          <xsl:with-param name="object" select="../.."/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="object.id">
          <xsl:with-param name="object" select=".."/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
<!--
 Comment out extra title
  <h1 class="{name(.)}">
    <a name="{$id}"/>
  
    <xsl:choose>
      <xsl:when test="$show.revisionflag != 0 and @revisionflag">
  <span class="{@revisionflag}">
    <xsl:apply-templates mode="titlepage.mode"/>
  </span>
      </xsl:when>
      <xsl:otherwise>
  <xsl:apply-templates mode="titlepage.mode"/>
      </xsl:otherwise>
    </xsl:choose>
  </h1>
-->    
</xsl:template>



</xsl:stylesheet>
