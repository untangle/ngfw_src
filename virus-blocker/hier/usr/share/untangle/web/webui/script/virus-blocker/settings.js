Ext.define('Webui.virus-blocker.settings', {
    extend:'Webui.virus-blocker-base.settings',
    helpSourceName: 'virus_blocker',
    aboutInfo: '&copy; BitDefender 1997-2017',
    getExtraAdvancedOptions: function() {
        return [{
            xtype: 'checkbox',
            boxLabel: i18n._('Enable ScoutIQ&trade; Cloud Scan'),
            hideLabel: true,
            name: 'Enable Cloud Scan',
            checked: this.settings.enableCloudScan,
            listeners: {
                "change": {
                    fn: Ext.bind(function(elem, checked) {
                        this.settings.enableCloudScan = checked;
                    }, this)
                }
            }
        },{
            xtype: 'checkbox',
            boxLabel: i18n._('Enable BitDefender&reg; Scan'),
            hideLabel: true,
            name: 'Enable Local Scan',
            checked: this.settings.enableLocalScan,
            listeners: {
                "change": {
                    fn: Ext.bind(function(elem, checked) {
                        this.settings.enableLocalScan = checked;
                    }, this)
                }
            }
        }];
    },
    getAppSummary: function() {
        return i18n._("Virus Blocker detects and blocks malware before it reaches users' desktops or mailboxes.");
    },
    initComponent: function() {
        this.callParent(arguments);
    }
});
//# sourceURL=virus-blocker-settings.js