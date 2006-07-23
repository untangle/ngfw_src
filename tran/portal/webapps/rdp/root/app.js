{
    openBookmark: function(portal, bookmark) {
        portal.showApplicationUrl('/rdp/rdp.jsp?t=' + bookmark.id, this, bookmark);
    },

    bookmarkProperties: [ new BookmarkProperty('size', 'Size', ['640x480', '800x600', '1024x768', '1280x1024'], '800x600'),
                          new BookmarkProperty('host', 'Host'),
                          new BookmarkProperty('command', 'Command (optional)'),
                          new BookmarkProperty('console', 'Console', ['true', 'false'], 'false') ],

    bookmarkFunction: function(obj) {
        var s = '';
        for (var f in obj) {
            var v = obj[f].getValue();
            if (null != v && '' != v) {
                s += '' == s ? '' : '&';
                s += escape(f) + '=' + escape(v);
            }
        }
        return s;
    },

    tooltipFunction: function(bookmark) {
        var obj = bookmark.splitTarget();
        var tip = "host: " + obj.host;
        if (obj.size) {
            tip += " size: " + obj.size;
        }
        if (obj.command) {
            tip += " command: " + obj.command;
        }
        if (obj.console) {
            tip += " console: " + obj.console;
        }

        return tip;
    },

    iconUrl: "/rdp/icon.gif"
};
