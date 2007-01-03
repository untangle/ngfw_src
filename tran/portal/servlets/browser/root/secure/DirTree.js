// Copyright (c) 2003-2007 Untangle, Inc.
// All rights reserved.

function DirTree(parent, className, posStyle, dragSource, dropTarget) {
    if (0 == arguments.length) {
        return;
    }
    DwtTree.call(this, parent, DwtTree.SINGLE_STYLE, "DirTree", posStyle);

    this.addTreeListener(new AjxListener(this, this._treeListener));

    this._dragSource = dragSource;
    this._dropTarget = dropTarget;

    this._popQueue = [];
    this._popWorkerTimedCb = new AjxTimedAction(this, this._popWorker, {});
    this._popWorkerCb = new AjxCallback(this, this._popWorker, {});
};

DirTree.prototype = new DwtTree();
DwtTree.prototype.constructor = DirTree;

// fields ---------------------------------------------------------------------

DirTree._POPULATED = "populated";

// public methods -------------------------------------------------------------

DirTree.prototype.addRoot = function(n)
{
    this.cwd = n;

    var root = new DwtTreeItem(this);
    root.setText(n.label);
    root.setImage(n.getIconName());
    root.setData(Browser.CIFS_NODE, n);
    if (!root._initialized) {
        root._initialize();
    }

    this._pushPopulate(root);
};

DirTree.prototype.addWorkGroup = function(n)
{
    var root = new DwtTreeItem(this);
    if (!root._initialized) {
        root._initialize();
    }
    root.setText(n.label);
    root.setImage("WorkGroup");
    root.setData(Browser.CIFS_NODE, n);

    this._pushPopulate(root);
};

DirTree.prototype.chdir = function(cifsNode)
{
    if (this.cwd == cifsNode) {
        return;
    }
    this.cwd = cifsNode;

    var url = cifsNode.url;

    var match = false;
    var children = this.getItems();
    for (var i = 0; i < children.length; i++) {
        var child = children[i];
        var n = child.getData(Browser.CIFS_NODE);
        var nUrl = n.url;
        var matches = true;

        if (nUrl.length > url.length) {
            matches = false;
        } else {
            for (var j = 0; j < nUrl.length; j++) {
                if (nUrl.charAt(j) != url.charAt(j)) {
                    matches = false;
                    break;
                }
            }
        }

        if (matches) {
            match = true;
            break;
        }
    }

    if (match) {
        this._expandNode(cifsNode.url, this);
    } else {
        this.addRoot(cifsNode);
    }
};

DirTree.prototype.refresh = function(url)
{
    var unpopulateStack = [ ];

    var children = this.getItems();
    for (var i = 0; i < children.length; i++) {
        unpopulateStack.push(children[i]);
    }

    while (0 < unpopulateStack.length) {
        var item = unpopulateStack.pop();
        if (item.getData(DirTree._POPULATED)) {
            this._populate(item, null, true);
        }
        var children = item.getItems();
        for (var i = 0; i < children.length; i++) {
            unpopulateStack.push(children[i]);
        }
    }
};

DirTree.prototype.repopulate = function(node, principal)
{
    var cn = node.getData(Browser.CIFS_NODE);
    cn.principal = principal;
    cn.authorized = true;
    node.setImage(cn.getIconName());
    this._populate(node, null, true);
    node.setExpanded(true);
};

// internal methods -----------------------------------------------------------

DirTree.prototype._expandNode = function(url, node)
{
    var match;

    var children = node.getItems();
    for (var i = 0; i < children.length; i++) {
        var child = children[i];
        var cifsNode = child.getData(Browser.CIFS_NODE);
        var childUrl = cifsNode.url;
        var matches = true;

        if (childUrl.length > url.length) {
            matches = false;
        } else {
            for (var j = 0; j < childUrl.length; j++) {
                if (childUrl.charAt(j) != url.charAt(j)) {
                    matches = false;
                    break;
                }
            }
        }

        if (matches) {
            match = child;
            break;
        }
    }

    if (match) {
        if (childUrl.length == url.length) {
            this.setSelection(child, true);
        } else {
            this._populate(match, new AjxCallback(this, this._expandNode, url));
        }
    }
};

DirTree.prototype._populate = function(item, cb, repopulate)
{
    Dwt.setCursor(item._textCell, "wait");
    Dwt.setCursor(item._imageCell, "wait");

    var n = item.getData(Browser.CIFS_NODE);

    if (repopulate || !item.getData(DirTree._POPULATED)) {
        item.setData(DirTree._POPULATED, true);

        var obj = { parent: item, cb: cb };

        var url = "secure/ls?url=" + n.getReqUrl() + "&type=dir";

        var actionCb = new AjxCallback(this, this._populateCallback, obj);
        var authCallback = new AjxCallback(this, this._populateAuthCallback, obj);
        MvRpc.invoke(null, url, null, true,
                     actionCb, MvRpc.reloadPageCallback, authCallback);
    } else {
        if (cb) {
            cb.run(item);
        }
    }
};

DirTree.prototype._pushPopulate = function(node)
{
    return;

    // XXX this is what it might do:
    this._popQueue.push(node);
    if (!this._populating) {
        this._populating = true;
        AjxTimedAction.scheduleAction(this._popWorkerTimedCb, 10);
    }
};

DirTree.prototype._popWorker = function()
{
    while (0 < this._popQueue.length) {
        var n = this._popQueue.pop();
        if (!n.getData(DirTree._POPULATED)) {
            this._populate(n, this._popWorkerCb, {});
            break;
        }
    }

    if (0 == this._popQueue.length) {
        this._populating = false;
    }
}

DirTree.prototype._populateCallback = function(obj, results)
{
    var p = obj.parent;
    var pcn = p.getData(Browser.CIFS_NODE);

    var dom = results.xml;
    var dirs = dom.getElementsByTagName("dir");

    var current = { };

    var children = p.getItems();
    for (var i = 0; i < children.length; i++) {
        var n = children[i].getData(Browser.CIFS_NODE);
        current[n.name] = children[i];
    }

    for (var i = 0; i < dirs.length; i++) {
        var c = dirs[i];
        var type = CifsNode.TYPES[c.getAttribute("type")];
        var name;
        if (CifsNode.WORKGROUP == type || CifsNode.SERVER == type) {
            name = "//" + c.getAttribute("name");
        } else {
            name = c.getAttribute("name");
        }

        var principal = c.getAttribute("principal");
        if (current[name]) {
            delete current[name];
        } else {
            var n;
            if (CifsNode.WORKGROUP == type || CifsNode.SERVER == type) {
                n = new CifsNode(null, name, principal, type);
            } else {
                n = new CifsNode(pcn.url, name, principal, type);
            }

            var tn = new DwtTreeItem(obj.parent, null, n.label, n.getIconName());
            tn.setData(Browser.CIFS_NODE, n);
            if (!tn._initialized) {
                tn._initialize();
            }
            var e = tn.getHtmlElement();
            if (this._dragSource) {
                tn.setDragSource(this._dragSource);
            }
            if (this._dropTarget) {
                tn.setDropTarget(this._dropTarget);
            }
        }
    }

    var tc = p._textCell;
    if (tc) {
        Dwt.setCursor(tc, "auto");
    }

    var ic = p._imageCell;
    if (ic) {
        Dwt.setCursor(ic, "auto");
    }

    for (var i in current) {
        var old = current[i];
        obj.parent.removeChild(old);
    }
};

DirTree.prototype._populateAuthCallback = function(obj, results)
{
    var p = obj.parent;
    var pcn = p.getData(Browser.CIFS_NODE);
    pcn.authorized = false;
    p.setImage(pcn.getIconName());

    var tc = p._textCell;
    if (tc) {
        Dwt.setCursor(tc, "auto");
    }

    var ic = p._imageCell;
    if (ic) {
        Dwt.setCursor(ic, "auto");
    }
};

DirTree.prototype._treeListener = function(evt)
{
    switch (evt.detail) {
    case DwtTree.ITEM_EXPANDED:
    var children = evt.item.getItems();
    for (var i = 0; i < children.length; i++) {
        this._pushPopulate(children[i]);
    }
    break;

    case DwtTree.ITEM_COLLAPSED:
    break;
    }
};
