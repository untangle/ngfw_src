<?xml version="1.0"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="html"></xsl:output>

  <xsl:template match="/">
    <html>
      <head>
        <title>HippieSoft Exploder</title>
        <link rel="stylesheet" type="text/css" href="browser.css"/>
        <script type="text/javascript" src="browser.js"></script>
      </head>
      <xsl:apply-templates/>
    </html>
  </xsl:template>

  <xsl:template match="dir">
    <span class="trigger">
      <xsl:attribute name="onClick">
        showDir('<xsl:value-of select="@name"/>');
      </xsl:attribute>
      <img src="closed.gif">
        <xsl:attribute name="id">I<xsl:value-of select="@name"/></xsl:attribute>
      </img>
      <xsl:value-of select="@name"/>
      <br/>
    </span>
    <span class="dir">
      <xsl:attribute name="id">
        <xsl:value-of select="@name"/>
      </xsl:attribute>
      <xsl:apply-templates/>
    </span>
  </xsl:template>

</xsl:stylesheet>
