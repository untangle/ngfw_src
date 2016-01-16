Ext.define('Webui.untangle-node-web-filter-lite.settings', {
    extend:'Webui.untangle-base-web-filter.settings',
    helpSourceName: 'web_filter_lite',
    getAppSummary: function() {
        return i18n._("Web Filter Lite scans and categorizes to monitor and enforce network usage policies.") + " <br/>" + " <br/>" +
            i18n._("WARNING: The Web Filter Lite categorization database is <b>unmaintained</b> and <b>deprecated</b>.");
    }
});
//# sourceURL=web-filter-lite-settings.js