{
    openApplication: function(portal) {
        portal.showApplicationUrl('/browser', this);
    },

    openBookmark: function(portal, bookmark) {
        portal.showApplicationUrl('/browser?target=' + bookmark.target, this, bookmark);
    },

    tooltipFunction: function(bookmark) {
        var obj = bookmark.splitTarget();

        return "target: " + obj.target;
    },

    iconUrl: "/browser/secure/icon.gif"
};
