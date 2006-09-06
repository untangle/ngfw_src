// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

function AddBookmarkPanel(parent, apps)
{
    if (0 == arguments.length) {
        return;
    }

    DwtComposite.call(this, parent);

    this._apps = [];
    for (var i = 0; i < apps.length; i++) {
        this._apps.push(new DwtSelectOption(apps[i], false, apps[i].description));
    }

    this._init();
};

AddBookmarkPanel.prototype = new DwtComposite();
AddBookmarkPanel.prototype.constructor = AddBookmarkPanel;

// public methods -------------------------------------------------------------

AddBookmarkPanel.prototype.getBookmark = function()
{
    var app = this._appField.getValue();
    var fn = app.getBookmarkFunction();

    var target;
    if (fn) {
        target = fn(this._properties);
    } else {
        target = this._targetField.getValue();
    }

    return new Bookmark(null, this._nameField.getValue(), app.name, target);
};

AddBookmarkPanel.prototype.focus = function()
{
    this._nameField.focus();
};

// private methods ------------------------------------------------------------

AddBookmarkPanel.prototype._init = function()
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

    this.valuePanel = new DwtComposite(this);

    this._showFields();
};

AddBookmarkPanel.prototype._showFields = function()
{
    this._properties = { };

    var app = this._appField.getValue();
    var props = app.getBookmarkProperties();
    if (props) {
        this._showPropFields(props);
    } else {
        this._showDefaultFields();
    }
};

AddBookmarkPanel.prototype._showDefaultFields = function()
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

    html.push("</table>");
    this.valuePanel.getHtmlElement().innerHTML = html.join("");

    this._fields = new Array();

    this._nameField = new DwtInputField({ parent: this });
    this._nameField.reparentHtmlElement(nameFieldId);
    this._fields.push(this._nameField);

    this._targetField = new DwtInputField({ parent: this });
    this._targetField.reparentHtmlElement(targetFieldId);
    this._fields.push(this._targetField);
};

AddBookmarkPanel.prototype._showPropFields = function(props)
{
    var nameFieldId = Dwt.getNextId();

    var fields = { };

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
        var field = prop.getField(this);
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
    this._nameField.reparentHtmlElement(nameFieldId);

    this._fields = new Array();
    for (var label in fields) {
        var field = fields[label];
        field.reparentHtmlElement(label);
        this._fields.push(field);
    }
};
