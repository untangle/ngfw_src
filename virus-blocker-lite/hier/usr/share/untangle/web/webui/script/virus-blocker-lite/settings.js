Ext.define('Webui.virus-blocker-lite.settings', {
    extend:'Webui.virus-blocker-base.settings',
    helpSourceName: 'virus_blocker_lite',
    aboutInfo: null,
    getExtraAdvancedOptions: function() {
        return [{
            xtype: 'checkbox',
            boxLabel: i18n._('Enable ScoutIQ&trade; Cloud Feedback'),
            hideLabel: true,
            name: 'Enable Cloud Feedback',
            checked: this.settings.enableCloudScan,
            listeners: {
                "change": {
                    fn: Ext.bind(function(elem, checked) {
                        this.settings.enableCloudScan = checked;
                    }, this)
                }
            }
        }];
    },
    getAppSummary: function() {
        return i18n._("Virus Blocker Lite detects and blocks malware before it reaches users' desktops or mailboxes.");
    },
    initComponent: function() {
        this.callParent(arguments);
    }
});
//# sourceURL=virus-blocker-lite-settings.js