// Copyright (c) 2003-2007 Untangle, Inc.
// All rights reserved.

function BookmarkDialog(parent, target)
{
    if (arguments.length == 0) {
        return;
    }

    var className = null; // XXX

    DwtDialog.call(this, parent, className, "New Bookmark");

    this._panel = new BookmarkPanel(this, target);
    this.addListener(DwtEvent.ONFOCUS, new AjxListener(this, this._focusListener));

    this.setView(this._panel);
    this.setTabOrder(this._panel._fields);
};

BookmarkDialog.prototype = new DwtDialog();
BookmarkDialog.prototype.constructor = BookmarkDialog;

// public methods -------------------------------------------------------------

BookmarkDialog.prototype.toString = function()
{
    return "BookmarkDialog";
};

BookmarkDialog.prototype.getName = function()
{
    return this._panel.getName();
};

// internal methods -----------------------------------------------------------

BookmarkDialog.prototype._focusListener = function(ev)
{
    this._panel.focus();
};

// ----------------------------------------------------------------------------
// BookmarkPanel
// ----------------------------------------------------------------------------

function BookmarkPanel(parent, target)
{
    if (0 == arguments.length) {
        return;
    }

    this.target = target;

    DwtComposite.call(this, parent);

    this._init();
};

BookmarkPanel.prototype = new DwtComposite();
BookmarkPanel.prototype.constructor = BookmarkPanel;

// public methods -------------------------------------------------------------

BookmarkPanel.prototype.toString = function()
{
    return "BookmarkPanel";
};

BookmarkPanel.prototype.getName = function()
{
    return this._nameField.getValue();
};

BookmarkPanel.prototype.focus = function()
{
    this._nameField.focus();
};

// private methods ------------------------------------------------------------

BookmarkPanel.prototype._init = function()
{
    var nameFieldId = Dwt.getNextId();

    var html = [];
    html.push("Create bookmark for target: ");
    html.push(this.target);
    html.push("<br/>");

    html.push("<table border=0>");
    html.push("<tr>");
    html.push("<td>Name: </td>");
    html.push("<td><div id='");
    html.push(nameFieldId);
    html.push("'/></td>");
    html.push("</tr>");

    html.push("</table>");
    this.getHtmlElement().innerHTML = html.join("");

    this._fields = new Array();

    this._nameField = new DwtInputField({ parent: this });
    this._nameField.reparentHtmlElement(nameFieldId);
    this._nameField.setRequired(true);
    this._fields.push(this._nameField);
};

