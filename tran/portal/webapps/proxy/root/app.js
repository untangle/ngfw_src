{
 openBookmark: function(portal, bookmark) {
    var o = portal.splitUrl(bookmark.target);
    portal.showApplicationUrl('/proxy/' + o.proto + '/' + o.host + o.path, this, bookmark);
  },

  iconUrl: "/proxy/icon.gif"
};
