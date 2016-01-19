Ext.define('Webui.untangle-node-virus-blocker.settings', {
    extend:'Webui.untangle-base-virus-blocker.settings',
    helpSourceName: 'virus_blocker',
    aboutInfo: '&copy; BitDefender 1997-2014',
    getAppSummary: function() {
        return i18n._("Virus Blocker detects and blocks malware before it reaches users' desktops or mailboxes.");
    },
    initComponent: function() {
        this.callParent(arguments);
    }
});
//# sourceURL=virus-blocker-settings.js