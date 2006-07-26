{
    openApplication: function(portal) {
        portal.showApplicationUrl('/browser', this);
    },

    openBookmark: function(portal, bookmark) {
        var url = bookmark.target.replace(/\\/g, "/");
        portal.showApplicationUrl('/browser?target=' + url, this,
                                  bookmark);
    },

    iconUrl: "/browser/secure/icon.gif"
};
