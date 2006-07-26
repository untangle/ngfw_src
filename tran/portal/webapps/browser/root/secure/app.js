{
    openApplication: function(portal) {
        portal.showApplicationUrl('/browser', this);
    },

    openBookmark: function(portal, bookmark) {
        var target = bookmark.target;
        var url = "";
        for (var i = 0; i < target.length; i++) {
            var c = target.charAt(i);
            url += '\\' == c ? "/" : c;
        }
        portal.showApplicationUrl('/browser?target=' + url, this,
                                  bookmark);
    },

    iconUrl: "/browser/secure/icon.gif"
};
