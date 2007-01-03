// Copyright (c) 2003-2007 Untangle, Inc.
// All rights reserved.

function RenameDialog(parent, cifsNode)
{
  if (arguments.length == 0) {
    return;
  }

  var className = null; // XXX

  DwtDialog.call(this, parent, className, "Rename File");

  this._panel = new RenamePanel(this, cifsNode);
  this.addListener(DwtEvent.ONFOCUS, new AjxListener(this, this._focusListener));

  this.setView(this._panel);
};

RenameDialog.prototype = new DwtDialog();
RenameDialog.prototype.constructor = RenameDialog;

// public methods -------------------------------------------------------------

RenameDialog.prototype.getDest = function()
{
  return this._panel.getDest();
};

// internal methods -----------------------------------------------------------

RenameDialog.prototype._uploadCompleteListener = function(evt)
{
  evt.dialog = this;

  this.notifyListeners(FileUploadPanel.UPLOAD_COMPLETE, evt);
};

RenameDialog.prototype._focusListener = function(ev)
{
  this._panel.focus();
};

// RenamePanel ----------------------------------------------------------------

function RenamePanel(parent, cifsNode)
{
  if (0 == arguments.length) {
    return;
  }

  this._cifsNode = cifsNode;

  DwtComposite.call(this, parent, "RenamePanel");

  var fieldId = Dwt.getNextId();

  var html = [];
  html.push("<table border=0>");
  html.push("<tr>");
  html.push("<td>Rename ");
  html.push(cifsNode.label);
  html.push(" to:</td>");
  html.push("<td>");
  html.push("<div id='");
  html.push(fieldId);
  html.push("'/>");
  html.push("</td>");
  html.push("</tr>");
  html.push("</table>");

  this.getHtmlElement().innerHTML = html.join("");

  this._field = new DwtInputField({ parent: this });
  this._field.setValue(cifsNode.label);
  this._field.reparentHtmlElement(fieldId);
};

RenamePanel.prototype = new DwtComposite();
RenamePanel.prototype.constructor = FileUploadPanel;

// public methods -------------------------------------------------------------

RenamePanel.prototype.getDest = function()
{
  return this._cifsNode.parent + this._field.getValue() + (this._cifsNode.isDirectory ? "/" : "");
};

RenamePanel.prototype.focus = function()
{
  this._field.focus();
};
