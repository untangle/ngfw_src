// Copyright (c) 2003-2007 Untangle, Inc.
// All rights reserved.

// -----------------------------------------------------------------------------
// BookmarkDialog
// -----------------------------------------------------------------------------

function BookmarkDialog(parent, apps, bm)
{
    if (arguments.length == 0) {
        return;
    }

    var className = null; // XXX

    DwtDialog.call(this, parent, className, (bm ? "Edit" : "Add") + " Bookmark");

    this._panel = new BookmarkDialogPanel(this, apps, bm);
    this.addListener(DwtEvent.ONFOCUS, new AjxListener(this, this._focusListener));

    this.setView(this._panel);
    this.setTabOrder(this._panel._fields);
}

BookmarkDialog.prototype = new DwtDialog();
BookmarkDialog.prototype.constructor = BookmarkDialog;

// public methods -------------------------------------------------------------

BookmarkDialog.prototype.getBookmark = function()
{
    return this._panel.getBookmark();
};

// internal methods -----------------------------------------------------------

BookmarkDialog.prototype._focusListener = function(ev)
{
    this._panel.focus();
};

// -----------------------------------------------------------------------------
// BookmarkDialogPanel
// -----------------------------------------------------------------------------

function BookmarkDialogPanel(parent, apps, bm)
{
    if (0 == arguments.length) {
        return;
    }

    this._bookmark = bm;

    DwtComposite.call(this, parent);

    this._apps = [];
    for (var i = 0; i < apps.length; i++) {
        var app = apps[i];
        if (app.isBookmarkable()) {
            this._apps.push(new DwtSelectOption(app, false, app.description));
        }
    }

    this._init();
};

BookmarkDialogPanel.prototype = new DwtComposite();
BookmarkDialogPanel.prototype.constructor = BookmarkDialogPanel;

// public methods --------------------------------------------------------------

BookmarkDialogPanel.prototype.getBookmark = function()
{
    var app = this._appField.getValue();
    var fn = app.getBookmarkFunction();

    if (!this._fieldsValid()) {
        return null;
    } else {
        var target;
        if (fn) {
            target = fn(this._properties);
        } else {
            target = this._targetField.getValue();
        }

        if (target) {
            return new Bookmark(this._bookmark ? this._bookmark.id : null,
                                this._nameField.getValue(), app.name, target,
                                Bookmark.USER_TYPE);
        } else {
            return null;
        }
    }
};

BookmarkDialogPanel.prototype.focus = function()
{
    this._nameField.focus();
};

// private methods ------------------------------------------------------------

BookmarkDialogPanel.prototype._init = function()
{
    var appFieldId = Dwt.getNextId();

    var appPanel = new DwtComposite(this);

    var html = [];
    html.push("<table border=0>");
    html.push("<tr>");
    html.push("<td>Application:</td>");
    html.push("<td><div id='");
    html.push(appFieldId);
    html.push("'/></td>");
    html.push("</tr>");
    html.push("</table>");
    html.push("<hr/>");

    appPanel.getHtmlElement().innerHTML = html.join("");

    this._appField = new DwtSelect(this, this._apps);
    var l = new AjxListener(this, this._showFields);
    this._appField.addChangeListener(l);
    this._appField.reparentHtmlElement(appFieldId);
    if (this._bookmark) {
        var appName = this._bookmark.app;
        for (var i = 0; i < this._apps.length; i++) {
            var app = this._apps[i].getValue();
            if (app.name == appName) {
                this._appField.setSelectedValue(app);
                break;
            }
        }
        this._appField.setEnabled(false);
    }

    this.valuePanel = new DwtComposite(this);

    this._showFields();
};

BookmarkDialogPanel.prototype._showFields = function()
{
    this._properties = { };

    var app = this._appField.getValue();
    var props = app.getBookmarkProperties();
    if (props) {
        this._showPropFields(props, app);
    } else {
        this._showDefaultFields(app);
    }
};

BookmarkDialogPanel.prototype._showDefaultFields = function(app)
{
    var nameFieldId = Dwt.getNextId();
    var targetFieldId = Dwt.getNextId();

    var html = new Array();
    html.push("<table border=0>");

    html.push("<tr>");
    html.push("<td>Name:</td>");
    html.push("<td><div id='");
    html.push(nameFieldId);
    html.push("'/></td>");
    html.push("</tr>");

    html.push("<tr>");
    html.push("<td>Target:</td>");
    html.push("<td><div id='");
    html.push(targetFieldId);
    html.push("'/></td>");
    html.push("</tr>");

    var targetExample = app.getTargetExample();
    if (targetExample) {
        html.push("<tr>");
        html.push("<td><i>Example: ");
        html.push(targetExample);
        html.push("</td>")
        html.push("</tr>");
    }

    html.push("</table>");
    this.valuePanel.getHtmlElement().innerHTML = html.join("");

    this._fields = new Array();

    this._nameField = new DwtInputField({ parent: this });
    this._nameField.reparentHtmlElement(nameFieldId);
    this._nameField.setRequired(true);
    this._fields.push(this._nameField);

    this._targetField = new DwtInputField({ parent: this });
    this._targetField.reparentHtmlElement(targetFieldId);
    this._targetField.setRequired(true);
    this._fields.push(this._targetField);

    if (this._bookmark) {
        this._nameField.setValue(this._bookmark.name);
        this._targetField.setValue(this._bookmark.target);
    }
};

BookmarkDialogPanel.prototype._showPropFields = function(props, app)
{
    var nameFieldId = Dwt.getNextId();

    var fields = { };

    var propValues;
    if (this._bookmark) {
        var propFn = app.getPropertiesFunction();
        propValues = propFn(this._bookmark);
    }

    var html = new Array();

    html.push("<table border=0>");

    html.push("<tr>");
    html.push("<td>Name:</td>");
    html.push("<td><div id='");
    html.push(nameFieldId);
    html.push("'/></td>");
    html.push("</tr>");

    for (var f in props) {
        var prop = props[f];
        var label = prop.name;
        var field;
        if (this._bookmark) {
            var val = propValues[prop.id];
            field = prop.getField(this, val);
        } else {
            field = prop.getField(this);
        }
        this._properties[prop.id] = field;

        var fieldId = Dwt.getNextId();
        fields[fieldId] = field;

        html.push("<tr>");
        html.push("<td>");
        html.push(label);
        html.push(":</td>");
        html.push("<td><div id='");
        html.push(fieldId);
        html.push("'/></td>");
        html.push("</tr>");
    }


    html.push("</table>");
    this.valuePanel.getHtmlElement().innerHTML = html.join("");

    this._nameField = new DwtInputField({ parent: this });
    if (this._bookmark) {
        this._nameField.setValue(this._bookmark.name);
    }
    this._nameField.reparentHtmlElement(nameFieldId);

    this._fields = new Array();
    for (var label in fields) {
        var field = fields[label];
        field.reparentHtmlElement(label);
        this._fields.push(field);
    }
};

BookmarkDialogPanel.prototype._fieldsValid = function()
{
    for (f in this._properties) {
        var prop = this._properties[f];
        if (prop._validator) {
            if (null == prop.isValid()) {
                return false;
            }
        }
    }

    return true;
};
