// Copyright (c) 2003-2007 Untangle, Inc.
// All rights reserved.

function AppWindow(parent, url)
{
    if (arguments.length == 0) {
        return;
    }

    DwtDialog.call(this, parent, null, "TITLE HERE", null, null, null,
                   DwtBaseDialog.MODELESS);

    this.initializeResizing();

    this.setContent("<iframe src='" + url + "'></iframe>");
}

AppWindow.prototype = new DwtDialog();
AppWindow.prototype.constructor = AppWindow;

// public methods -------------------------------------------------------------

// internal methods -----------------------------------------------------------

AppWindow.prototype.initializeResizing = function()
{
    var htmlEl = this.getHtmlElement();
    var table = htmlEl.childNodes[0];
    var tbody = table.tBodies[0];
    var br = tbody.rows[5];
    var cell = br.cells[2];

    cell.style.cursor = DwtDragTracker.STYLE_RESIZE_SOUTHEAST;

    with (this) {
        var moveFn = function(evt) {
            var bounds = getBounds();

            var h = evt.screenY - bounds.y;
            var w = evt.screenX - bounds.x;

            //setBounds(bounds.x, bounds.y, w, h);
            //setBounds(bounds.x, bounds.y, bounds.width + 1, bounds.height + 1);
            setSize(50, 150);
            popup();
        }
    }

    var upFn = function() {
        Dwt.clearHandler(cell, DwtEvent.ONMOUSEMOVE);
    }

    var downFn = function() {
        Dwt.setHandler(cell, DwtEvent.ONMOUSEUP, upFn);
        Dwt.setHandler(cell, DwtEvent.ONMOUSEMOVE, moveFn);
    }

    Dwt.setHandler(cell, DwtEvent.ONMOUSEDOWN, downFn);
}


