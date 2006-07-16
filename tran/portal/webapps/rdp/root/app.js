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

  iconUrl: "/rdp/icon.png"
};
