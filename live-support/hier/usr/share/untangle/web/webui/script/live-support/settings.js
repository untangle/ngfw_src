Ext.define('Webui.live-support.settings', {
    extend:'Ung.AppWin',
    hasReports: false,
    hasApply: false, // do not need save
    getAppSummary: function() {
        return i18n._('Live Support provides on-demand help for any technical issues.');
    },
    initComponent: function(container, position) {
        this.buildTabPanel([]);
        this.callParent(arguments);
    },
    // Support Panel
    buildStatus: function() {
        this.panelStatus = Ext.create('Ung.panel.Status', {
            settingsCmp: this,
            helpSource: 'live_support_support',
            itemsToAppend: [{
                title: i18n._('Live Support'),
                items: [{
                    xtype: 'component',
                    html: Ext.String.format(i18n._("This {0} Server is entitled to Live Support."), rpc.companyName) 
                }, {
                    xtype: 'button',
                    margin: '10 0 0 0',
                    text: i18n._('Get Support!'),
                    name: 'Get Support',
                    iconCls: 'action-icon',
                    handler: Ext.bind(function() {
                        Ung.Main.openSupportScreen();
                    }, this)
                }]
            },{
                title: i18n._('Support Information'),
                defaults: {
                    xtype: 'displayfield',
                    labelWidth: 50
                },
                items: [{
                    fieldLabel: i18n._("UID"),
                    value: rpc.serverUID
                }, {
                    fieldLabel: i18n._("Build"),
                    value: rpc.fullVersionAndRevision
                }]
            }]
        });
    },
    // save function
    saveAction: function() {
        this.closeWindow();
    }
});
//# sourceURL=live-support-settings.js