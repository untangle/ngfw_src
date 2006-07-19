{
 openApplication: function(portal) {
    portal.showApplicationUrl('/browser', this);
  },

 openBookmark: function(portal, bookmark) {
    portal.showApplicationUrl('/browser?target=' + bookmark.target, this, bookmark);
  },

  iconUrl: "/browser/secure/icon.gif"
};
