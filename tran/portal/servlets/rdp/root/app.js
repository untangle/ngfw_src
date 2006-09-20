{
    openBookmark: function(portal, bookmark) {
        portal.showApplicationUrl('/rdp/rdp.jsp?t=' + bookmark.id, this, bookmark);
    },

    bookmarkProperties: [ new BookmarkProperty('size', 'Size', ['640x480', '800x600', '1024x768', '1280x1024'], '800x600'),
                          new BookmarkProperty('host', 'Host', null, null, true),
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

    propertiesFunction: function(bookmark) {
        return bookmark.splitTarget();
    },

    tooltipFunction: function(bookmark) {
        var obj = bookmark.splitTarget();
        var tip = "<html><b>Application: </b>Remote Desktop<br><b>host:</b> " + obj.host;
        if (obj.size) {
            tip += "<br><b>size:</b> " + obj.size;
        }
        if (obj.command) {
            tip += "<br><b>command:</b> " + obj.command;
        }
        if (obj.console) {
            tip += "<br><b>console:</b> " + obj.console;
        }
    tip += "</html>";
        return tip;
    },

    iconUrl: "/rdp/icon.gif"
};
