Ext.define('Webui.untangle-node-virus-blocker-lite.settings', {
    extend:'Webui.untangle-base-virus-blocker.settings',
    helpSourceName: 'virus_blocker_lite',
    aboutInfo: null,
    getAppSummary: function() {
        return i18n._("Virus Blocker Lite stops virus outbreaks before they reach users' desktops.");
    },
    initComponent: function() {
        this.callParent(arguments);
    }
});
//# sourceURL=virus-blocker-lite-settings.js