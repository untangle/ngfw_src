// Copyright (c) 2003-2007 Untangle, Inc.
// All rights reserved.

function Browser(shell, url)
{
    if (0 == arguments.length) {
        return;
    }

    if (url && '/' != url.charAt(url.length - 1)) {
        url += '/';
    }

    var cifsNode;
    if (url) {
        cifsNode = new CifsNode(null, url, null, CifsNode.SHARE); // XXX TYPE?
    }

    this._shell = shell;

    this._shell.addControlListener(new AjxListener(this, this._shellListener));

    DwtComposite.call(this, this._shell, "Browser", DwtComposite.RELATIVE_STYLE);

    this._listerAuthCallback = new AjxCallback(this, this._listerAuthFn, { });
    this._msgAuthCallback = new AjxCallback(this, this._msgAuthFn, { });
    this._refreshCallback = new AjxCallback(this, this.refresh, { });

    this._toolbar = this._makeToolbar();
    this._toolbar.zShow(true);

    this._addressBar = this._makeAddressBar();
    this._addressBar.zShow(true);

    var dropTarget = new DwtDropTarget(CifsNode);
    dropTarget.addDropListener(new AjxListener(this, this._treeDropListener));
    this._dirTree = new DirTree(this, null, DwtControl.ABSOLUTE_STYLE,
                                null, dropTarget);

    if (cifsNode) {
        this._dirTree.addRoot(cifsNode);
    }
    this._dirTree.setScrollStyle(DwtControl.SCROLL);

    this._dirTree.addSelectionListener(new AjxListener(this, this._dirSelectionListener));
    this._dirTree.zShow(true);

    this._sash = new DwtSash(this, DwtSash.HORIZONTAL_STYLE, null, 3);
    this._sashPos = 200;
    this._sash.registerCallback(this._sashCallback, this);
    this._sash.zShow(true);

    var dragSource = new DwtDragSource(Dwt.DND_DROP_MOVE | Dwt.DND_DROP_COPY);
    dragSource.addDragListener(new AjxListener(this, this._detailDragListener));
    dropTarget = new DwtDropTarget(CifsNode);
    dropTarget.addDropListener(new AjxListener(this, this._detailDropListener));
    this._detailPanel = new DetailPanel(this, this._listerAuthCallback);
    this._detailPanel.setUI();
    this._detailPanel.zShow(true);
    this._detailPanel.addSelectionListener(new AjxListener(this, this._detailSelectionListener));
    this._detailPanel.setDragSource(dragSource);
    this._detailPanel.setDropTarget(dropTarget);

    this._dirActionMenu = this._makeDirActionMenu();
    this._fileActionMenu = this._makeFileActionMenu();


    this._detailPanel.addActionListener(new AjxListener(this, this._listActionListener));

    if (!cifsNode) {
        this._broadcastRoots();
    } else {
        var action = new AjxTimedAction(this, this._broadcastRoots, {});
        AjxTimedAction.scheduleAction(action, 2000);
    }

    this.layout();

    if (cifsNode) {
        this.chdir(cifsNode, false);
    }

    this.zShow(true);
};

Browser.prototype = new DwtComposite();
Browser.prototype.constructor = Browser;

// fields ---------------------------------------------------------------------

Browser.CIFS_NODE = "cifsNode";

// public methods -------------------------------------------------------------

Browser.prototype.chdir = function(cifsNode, expandTree, expandDetail)
{
    this._cwd = cifsNode;

    this._addressField.setValue(cifsNode.url.replace(/\//g, "\\"));
    document.title = "Network File Browser (" + this._addressField.getValue() + ")";

    if (undefined == expandTree || expandTree) {
        this._dirTree.chdir(cifsNode);
    }

    if (undefined == expandDetail || expandDetail) {
        this._detailPanel.chdir(cifsNode);
    }
};

Browser.prototype.mv = function(src, dest)
{
    var reqStr = Browser._mkSrcDestCommand("mv", src, dest)

    // XXX handle error
    MvRpc.invoke(reqStr, "secure/exec", Browser._POST_HEADERS, false,
                 this._refreshCallback, MvRpc.reloadPageCallback,
                 this._msgAuthCallback);
};

Browser.prototype.cp = function(src, dest)
{
    var reqStr = Browser._mkSrcDestCommand("cp", src, dest)

    // XXX handle error
    MvRpc.invoke(reqStr, "secure/exec", Browser._POST_HEADERS, false,
                 this._refreshCallback, MvRpc.reloadPageCallback,
                 this._msgAuthCallback);
};

Browser.prototype.refresh = function()
{
    this._detailPanel.refresh();
    this._dirTree.refresh();
};

Browser.prototype.layout = function(ignoreSash)
{
    var size = this._shell.getSize();
    var width = size.x;
    var height = size.y;

    var x = 0;
    var y = 0;

    this._toolbar.setLocation(0, 0);
    var size = this._toolbar.getSize();
    y += size.y;

    this._addressBar.setLocation(0, y);
    size = this._addressBar.getSize();
    y += size.y;

    this._dirTree.setBounds(x, y, this._sashPos, height - y);
    x += this._dirTree.getSize().x;

    if (!ignoreSash) {
        this._sash.setBounds(x, y, 2, height - y);
    }
    x += this._sash.getSize().x;

    this._detailPanel.setBounds(x, y, width - x, height - y);
    x += this._detailPanel.getSize().x;
};

// private fields -------------------------------------------------------------

Browser._POST_HEADERS = {};
Browser._POST_HEADERS["Content-Type"] = "application/x-www-form-urlencoded";

// init -----------------------------------------------------------------------

Browser.prototype._makeToolbar = function()
{
    var toolbar = new DwtToolBar(this, "ToolBar", DwtControl.ABSOLUTE_STYLE, 2);

    var b = new DwtButton(toolbar, DwtButton.ALIGN_CENTER);
    b.setText("Refresh");
    b.setToolTipContent("Display latest contents");
    b.addSelectionListener(new AjxListener(this, this._refreshButtonListener));

    b = new DwtButton(toolbar, DwtButton.ALIGN_CENTER);
    b.setText("Upload");
    b.setToolTipContent("Upload files to share");
    b.addSelectionListener(new AjxListener(this, this._uploadButtonListener));

    b = new DwtButton(toolbar, DwtButton.ALIGN_CENTER);
    b.setText("Delete");
    b.setToolTipContent("Delete selected files");
    b.addSelectionListener(new AjxListener(this, this._deleteButtonListener));

    b = new DwtButton(toolbar, DwtButton.ALIGN_CENTER);
    b.setText("New Folder");
    b.setToolTipContent("Create a new folder");
    b.addSelectionListener(new AjxListener(this, this._mkdirButtonListener));

    return toolbar;
};

Browser.prototype._makeAddressBar = function()
{
    var toolbar = new DwtToolBar(this, "ToolBar", DwtControl.ABSOLUTE_STYLE, 2);

    var l = new DwtLabel(toolbar);
    l.setText("Address");

    this._addressField = new DwtInputField({ parent: toolbar, size: 50 });

    with (this) {
        this._addressField.getInputElement().onkeyup = function(ev) {
            DwtInputField._keyUpHdlr(ev);

            var keyEv = DwtShell.keyEvent;
            keyEv.setFromDhtmlEvent(ev);
            if (DwtKeyEvent.KEY_RETURN == keyEv.keyCode) {
                var val = keyEv.dwtObj.getValue().replace(/\\/g, "/");
                if (val.charAt(val.length - 1) != '/') {
                    val += '/';
                }
                var cifsNode = new CifsNode(null, val);
                chdir(cifsNode);
            }
        };
    }

    return toolbar;
};

Browser.prototype._makeDirActionMenu = function()
{
    var actionMenu = new DwtMenu(this._detailPanel, DwtMenu.POPUP_STYLE);

    var i = new DwtMenuItem(actionMenu, DwtMenuItem.NO_STYLE);
    i.setText("Open");
    i.addSelectionListener(new AjxListener(this, this._openDirListener));

    i = new DwtMenuItem(actionMenu, DwtMenuItem.NO_STYLE);
    i.setText("Delete");
    i.addSelectionListener(new AjxListener(this, this._deleteButtonListener));

    i = new DwtMenuItem(actionMenu, DwtMenuItem.NO_STYLE);
    i.setText("Rename...");
    i.addSelectionListener(new AjxListener(this, this._renameButtonListener));


    i = new DwtMenuItem(actionMenu, DwtMenuItem.NO_STYLE);
    i.setText("Bookmark...");
    i.addSelectionListener(new AjxListener(this, this._bookmarkButtonListener));

    return actionMenu;
};

Browser.prototype._makeFileActionMenu = function()
{
    var actionMenu = new DwtMenu(this._detailPanel, DwtMenu.POPUP_STYLE);

    var i = new DwtMenuItem(actionMenu, DwtMenuItem.NO_STYLE);
    i.setText("Display");
    i.addSelectionListener(new AjxListener(this, this._displayListener));
    i = new DwtMenuItem(actionMenu, DwtMenuItem.NO_STYLE);
    i.setText("Save As...");
    i.addSelectionListener(new AjxListener(this, this._saveAsListener));
    i = new DwtMenuItem(actionMenu, DwtMenuItem.NO_STYLE);
    i.setText("Delete");
    i.addSelectionListener(new AjxListener(this, this._deleteButtonListener));
    i = new DwtMenuItem(actionMenu, DwtMenuItem.NO_STYLE);
    i.setText("Rename...");
    i.addSelectionListener(new AjxListener(this, this._renameButtonListener));

    return actionMenu;
};

Browser.prototype._broadcastRoots = function()
{
    var actionCb = new AjxCallback(this, this._broadcastRootsCbFn, new Object());
    MvRpc.invoke(null, "secure/ls?url=//", null, true,
                 actionCb, MvRpc.reloadPageCallback, this._msgAuthCallback);
};

Browser.prototype._broadcastRootsCbFn = function(obj, results)
{
    var root = results.xml.getElementsByTagName("root")[0];
    var children = root.childNodes;
    for (var i = 0; i < children.length; i++) {
        var child = children[i];
        var tagName = child.tagName;
        if ("dir" == tagName || "file" == tagName) {
            var name = "//" + child.getAttribute("name");
            var type = CifsNode.TYPES[child.getAttribute("type")];
            var n = new CifsNode(null, name, null, type);
            this._dirTree.addWorkGroup(n);
        }
    }
};

// listeners ------------------------------------------------------------------

Browser.prototype._detailSelectionListener = function(ev)
{
    switch (ev.detail) {
    case DwtListView.ITEM_DBL_CLICKED:
    var item = ev.item;
    if (item.isDirectory()) {
        this.chdir(item);
    } else {
        AjxWindowOpener.open("secure/get/" + item.getReqUrl());
    }
    break;
    }
};

// toolbar buttons ------------------------------------------------------------

Browser.prototype._dirSelectionListener = function(evt)
{
    switch (evt.detail) {
    case DwtTree.ITEM_SELECTED:
    var item = evt.item;
    if (!item.getData(DirTree._POPULATED)) {
        this._dirTree._populate(item);
    }
    var n = item.getData(Browser.CIFS_NODE);
    this.chdir(n, false);
    break;

    case DwtTree.ITEM_DESELECTED:
    break;

    case DwtTree.ITEM_CHECKED:
    break;

    case DwtTree.ITEM_ACTIONED:
    break;

    case DwtTree.ITEM_DBL_CLICKED:
    var item = evt.item;
    var n = item.getData(Browser.CIFS_NODE);
    if (!n.authorized) {
        var d = new LoginDialog(this._shell, n.getDomain());
        var o = { dialog: d, item: item };
        var l = new AjxListener(this, this._authenticateDialogListener, o);
        d.setButtonListener(DwtDialog.OK_BUTTON, l);
        d.addListener(DwtEvent.ENTER, l);
        d.popup();
    } else {
        if (!item.getData(DirTree._POPULATED)) {
            this._dirTree._populate(item);
        }
        item.setExpanded(!evt.item.getExpanded());
    }
    break;

    default:
    }
};

Browser.prototype._authenticateDialogListener = function(obj, evt)
{
    var d = obj.dialog;
    var domain = d.getDomain();
    var username = d.getUser();
    var password = d.getPassword();

    var reqStr = "domain=" + domain + "&username=" + username
    + "&password=" + password;

    var obj = { dialog: d, item: obj.item }
    var actionCb = new AjxCallback(this, this._loginCallback, obj);
    MvRpc.invoke(reqStr, "secure/login", Browser._POST_HEADERS, false,
                 actionCb, MvRpc.reloadPageCallback, null);
};

Browser.prototype._loginCallback = function(obj, results)
{
    var auth = results.xml.getElementsByTagName("auth")[0];
    var status = auth.getAttribute("status");
    var principal = auth.getAttribute("principal");

    if ("success" == status) {
        obj.dialog.popdown();
        this._dirTree.repopulate(obj.item, principal);
        this._detailPanel.refresh();
    } else {
        obj.dialog.reportFailure("Bad Login");
    }
};

Browser.prototype._listActionListener = function(ev)
{
    var sel = this._detailPanel.getSelection();
    if (1 == sel.length) {
        if (sel[0].isDirectory()) {
            this._dirActionMenu.popup(0, ev.docX, ev.docY);
        } else {
            this._fileActionMenu.popup(0, ev.docX, ev.docY);
        }
    } else if (1 < sel.length) {
        // XXX group menu
        this._dirActionMenu.popup(0, ev.docX, ev.docY);
    }
};

Browser.prototype._refreshButtonListener = function(ev)
{
    this.refresh();
};

Browser.prototype._uploadButtonListener = function(ev)
{
    var dialog = new FileUploadDialog(this._shell, "secure/put", this._cwd.getPostUrl());

    var obj = { dialog: dialog };
    var l = new AjxListener(this, this._popdownAndRefreshCbFn, obj);
    dialog.addUploadCompleteListener(l);

    dialog.popup();
};

Browser.prototype._displayListener = function(ev)
{
    var sel = this._detailPanel.getSelection();
    if (0 < sel.length) {
        var item = sel[0]; // XXX first item only
        AjxWindowOpener.open("secure/get/" + item.getReqUrl());
    }
}

    Browser.prototype._saveAsListener = function(ev)
{
    var sel = this._detailPanel.getSelection();
    if (0 < sel.length) {
        var item = sel[0]; // XXX first item only
        window.location.href = "secure/get/" + item.getReqUrl() + "?mode=save";
    }
}

        Browser.prototype._openDirListener = function(ev)
{
    var sel = this._detailPanel.getSelection();
    if (0 < sel.length) {
        var item = sel[0]; // XXX first item only
        if (item.isDirectory()) {
            this.chdir(item);
        }
    }
};

Browser.prototype._deleteButtonListener = function(ev)
{
    var sel = this._detailPanel.getSelection();
    if (0 == sel.length) {
        return;
    }

    var reqStr = "command=rm";
    for (var i = 0; i < sel.length; i++) {
        reqStr += "&file=" + sel[i].getReqUrl();
    }

    var numFilesMsg;
    if (1 == sel.length) {
        numFilesMsg = sel[0].label;
    } else {
        numFilesMsg = sel.length + " files";
    }

    var dialog = new DwtMessageDialog(DwtShell.getShell(window), null,
                                      [DwtDialog.OK_BUTTON, DwtDialog.CANCEL_BUTTON]);
    dialog.setMessage("Are you sure you want to delete " + numFilesMsg + "?");

    var fn = function() {
        dialog.popdown();

        MvRpc.invoke(reqStr, "secure/exec", Browser._POST_HEADERS, false,
                     this._refreshCallback, MvRpc.reloadPageCallback,
                     this._msgAuthCallback);
    }

    var cb = new AjxListener(this, fn, {});
    dialog.setButtonListener(DwtDialog.OK_BUTTON, cb)
    dialog.addListener(DwtEvent.ENTER, l);

    dialog.popup();
};

Browser.prototype._renameButtonListener = function(ev)
{
    var sel = this._detailPanel.getSelection();
    if (0 == sel.length) {
        return;
    }

    var dialog = new RenameDialog(this._shell, sel[0]);

    var obj = { dialog: dialog };
    var l = new AjxListener(this, this._renameDialogListenerFn, obj);

    dialog.setButtonListener(DwtDialog.OK_BUTTON, l);
    dialog.addListener(DwtEvent.ENTER, l);

    dialog.popup();
};

Browser.prototype._bookmarkButtonListener = function(ev)
{
    var sel = this._detailPanel.getSelection();

    var target = sel[0].getFullPath();

    var dialog = new BookmarkDialog(this._shell, target);

    var obj = { dialog: dialog, target: target };
    var l = new AjxListener(this, this._bookmarkDialogListenerFn, obj);

    dialog.setButtonListener(DwtDialog.OK_BUTTON, l);
    dialog.addListener(DwtEvent.ENTER, l);

    dialog.popup();
};

Browser.prototype._renameDialogListenerFn = function(obj, evt)
{
    // XXX first selection only
    var dest = obj.dialog.getDest();

    if (dest) {
        var sel = this._detailPanel.getSelection();
        var reqStr = "command=rename&src=" + sel[0].getReqUrl() + "&dest=" + dest;

        var obj = { dialog: obj.dialog };
        var actionCb = new AjxCallback(this, this._popdownAndRefreshCbFn, obj)

            MvRpc.invoke(reqStr, "secure/exec", Browser._POST_HEADERS, false,
                         actionCb, MvRpc.reloadPageCallback, this._msgAuthCallback);
    }
};

Browser.prototype._mkdirButtonListener = function(ev)
{
    var dialog = new MkdirDialog(this._shell, this._cwd);

    var obj = { dialog: dialog };
    var l = new AjxListener(this, this._mkdirDialogListenerFn, obj);

    dialog.setButtonListener(DwtDialog.OK_BUTTON, l);
    dialog.addListener(DwtEvent.ENTER, l);

    dialog.popup();
};

Browser.prototype._mkdirDialogListenerFn = function(obj, evt)
{
    var dir = obj.dialog.getDir();

    if (dir) {
        var url = "secure/exec";
        var reqStr = "command=mkdir&url=" + dir;

        var obj = { dialog: obj.dialog }
        var actionCb = new AjxCallback(this, this._popdownAndRefreshCbFn, obj);

        MvRpc.invoke(reqStr, url, Browser._POST_HEADERS, false, actionCb,
                     MvRpc.reloadPageCallback, this._msgAuthCallback);
    }
};

Browser.prototype._bookmarkDialogListenerFn = function(obj, evt)
{
    var dialog = obj.dialog;

    var name = dialog.getName();

    if (name) {
        var url = "/portal/secure/bookmark?command=add&name=" + escape(name)
            + "&app=CIFS&target=" + escape(obj.target);
        // XXX feedback from call
        MvRpc.invoke(null, url, null, true,
                     null,
                     MvRpc.reloadPageCallback);
        dialog.popdown();
    }
};

Browser.prototype._popdownAndRefreshCbFn = function(obj, evt)
{
    obj.dialog.popdown();
    this.refresh();
};

Browser.prototype._listerAuthFn = function(obj, response)
{
    this._detailPanel.set(new AjxVector());
};

Browser.prototype._msgAuthFn = function(obj, response)
{
    var authError = response.xml.getElementsByTagName("auth-error")[0];
    var msg = authError.getAttribute("msg");
    var dialog = new DwtMessageDialog(this._shell);
    dialog.setMessage(msg, DwtMessageDialog.WARNING_STYLE, "Error");
    dialog.popup();
};

// shell ----------------------------------------------------------------------

Browser.prototype._shellListener = function(ev)
{
    if (ev.oldWidth != ev.newWidth || ev.oldHeight != ev.newHeight) {
        this.layout();
    }
};

// sash -----------------------------------------------------------------------

Browser.prototype._sashCallback = function(d)
{
    var oldPos = this._sashPos;

    this._sashPos += d;
    if (0 > this._sashPos) {
        this._sashPos = 0;
    }

    if (this._shell.getSize().x < this._sashPos) {
        this._sashPos = x;
    }

    this.layout(true);

    return this._sashPos - oldPos;
};

// dnd ------------------------------------------------------------------------

Browser.prototype._treeDragListener = function(evt)
{

};

Browser.prototype._treeDropListener = function(evt)
{
    var targetControl = evt.targetControl;

    switch (evt.action) {
    case DwtDropEvent.DRAG_ENTER:
        break;

    case DwtDropEvent.DRAG_LEAVE:
        break;

    case DwtDropEvent.DRAG_OP_CHANGED:
        break;

    case DwtDropEvent.DRAG_DROP:
        var dest = evt.targetControl.getData(Browser.CIFS_NODE);
        var src = evt.srcData;

        switch (evt.operation) {
        case Dwt.DND_DROP_COPY:
            this.cp(src, dest);
            break;

        case Dwt.DND_DROP_MOVE:
            this.mv(src, dest);
            break;
        }
        break;
    }
};

Browser.prototype._detailDragListener = function(evt)
{
    switch (evt.action) {
    case DwtDragEvent.DRAG_START:
    break;

    case DwtDragEvent.SET_DATA:
    evt.srcData = evt.srcControl.getDnDSelection();
    break;

    case DwtDragEvent.DRAG_END:
    break;
    }
};

Browser.prototype._detailDropListener = function(evt)
{
    var targetControl = evt.targetControl;

    switch (evt.action) {
    case DwtDropEvent.DRAG_ENTER:
        break;

    case DwtDropEvent.DRAG_LEAVE:
        break;

    case DwtDropEvent.DRAG_OP_CHANGED:
        break;

    case DwtDropEvent.DRAG_DROP:
        break;
    }
};

// util -----------------------------------------------------------------------

Browser._mkSrcDestCommand = function(command, src, dest)
{
    var reqStr = "command=" + command;

    for (var i = 0; i < src.length; i++) {
        reqStr += "&src=" + src[i].getReqUrl(); // XXX does this get escaped ?
    }

    reqStr += "&dest=" + dest.getReqUrl(); // XXX does this get escaped ?

    return reqStr;
};
