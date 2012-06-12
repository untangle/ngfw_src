Ung.NodeWin.dependency["untangle-node-webfilter"] = {
    name : "untangle-base-webfilter",
    fn : function() {
        if (!Ung.hasResource["Ung.WebFilter"]) {
            Ung.hasResource["Ung.WebFilter"] = true;
            Ung.NodeWin.registerClassName('untangle-node-webfilter', 'Ung.WebFilter');
            Ung.WebFilter = Ext.extend(Ung.BaseWebFilter, {});
        }
    }
};
//@ sourceURL=webfilter-settings.js
