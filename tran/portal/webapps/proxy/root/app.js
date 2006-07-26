{
    openBookmark: function(portal, bookmark) {
        var target = bookmark.target;
        if (0 != target.indexOf("http://") && 0 != target.indexOf("https://")) {
            target = "http://" + target;
        }
        var o = portal.splitUrl(target);
        portal.showApplicationUrl('/proxy/' + o.proto + '/' + o.host + o.path, this, bookmark);
    },

    iconUrl: "/proxy/icon.gif"
};
