// Copyright (c) 2003-2007 Untangle, Inc.
// All rights reserved.

function MkdirDialog(parent, cifsNode)
{
  if (arguments.length == 0) {
    return;
  }

  var className = null; // XXX

  DwtDialog.call(this, parent, className, "New Folder");

  this._panel = new MkdirPanel(this, cifsNode);
  this.addListener(DwtEvent.ONFOCUS, new AjxListener(this, this._focusListener));

  this.setView(this._panel);
};

MkdirDialog.prototype = new DwtDialog();
MkdirDialog.prototype.constructor = MkdirDialog;

// public methods -------------------------------------------------------------

MkdirDialog.prototype.getDir = function()
{
  return this._panel.getDir();
};

// internal methods -----------------------------------------------------------

MkdirDialog.prototype._focusListener = function(ev)
{
  this._panel.focus();
};

// MkdirPanel -----------------------------------------------------------------

function MkdirPanel(parent, cifsNode)
{
  if (0 == arguments.length) {
    return;
  }

  this._cifsNode = cifsNode;

  DwtComposite.call(this, parent, "MkdirPanel");

  var folderFieldId = Dwt.getNextId();

  var html = [];
  html.push("<table border=0>");

  html.push("<tr>");
  html.push("<td>Folder Name:</td>");
  html.push("<td><div id='");
  html.push(folderFieldId);
  html.push("'/></td>");
  html.push("</tr>");
  html.push("</table>");
  this.getHtmlElement().innerHTML = html.join("");

  this._field = new DwtInputField({ parent: this });
  this._field.reparentHtmlElement(folderFieldId);
};

MkdirPanel.prototype = new DwtComposite();
MkdirPanel.prototype.constructor = FileUploadPanel;

// public methods -------------------------------------------------------------

MkdirPanel.prototype.getDir = function()
{
  // XXX resolve absolute vs relative paths
  return this._cifsNode.getReqUrl() + this._field.getValue();
};

MkdirPanel.prototype.focus = function()
{
  this._field.focus();
};
