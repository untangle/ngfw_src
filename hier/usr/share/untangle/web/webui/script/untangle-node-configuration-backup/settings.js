Ext.define('Webui.untangle-node-configuration-backup.settings', {
    extend:'Ung.NodeWin',
    panelStatus: null,
    gridEventLog: null,
    hasApply: false, // do not need save
    initComponent: function(container, position) {
        // builds the 2 tabs
        this.buildStatus();
        // builds the tab panel with the tabs
        this.buildTabPanel([this.panelStatus]);
        this.callParent(arguments);
    },
    // get branding settings
    getLatestEvent: function(forceReload) {
        if (forceReload || this.rpc.latestEvent === undefined) {
            try {
                this.rpc.latestEvent = this.getRpcNode().getLatestEvent();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.rpc.latestEvent;
    },
    getStatus: function () {
        var event = this.getLatestEvent();
        if (event == null)
            return this.i18n._("Backup pending.");
        else if (event.success == true)
            return this.i18n._("Successfully backed up on") + " " + i18n.timestampFormat(event.timeStamp) + "</i>";
        else
            return this.i18n._("Last backup failed on") + " <i>" + i18n.timestampFormat(event.timeStamp) + "</i>";
    },
    saveAction: function() {
        this.closeWindow();
    },
    // Status Panel
    buildStatus: function() {
        var contactName;
        try {
            contactName = Ung.Main.getBrandingManager().getContactName();
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }
        this.panelStatus = Ext.create('Ext.panel.Panel',{
            title: this.i18n._('Status'),
            name: 'Status',
            helpSource: 'configuration_backup_status',
            autoScroll: true,
            cls: 'ung-panel',
            items: [{
                xtype: 'fieldset',
                title: this.i18n._('Status'),
                html: this.getStatus()
            },{
                xtype: 'fieldset',
                title: this.i18n._('Note'),
                html: Ext.String.format(this.i18n._('Configuration Backup automatically saves your configuration settings and sends them to {0} for safe storage. If you need a backup of your configuration settings, contact {1}'),
                        rpc.companyName, contactName)
            }, {
                xtype: 'fieldset',
                title: this.i18n._('Backup Now'),
                layout: {type: 'hbox', align: 'middle'},
                items: [{
                    xtype: 'container',
                    margin: '0 50 0 0',
                    html: this.i18n._('Force an immediate backup now.')
                }, {
                    xtype: 'button',
                    text: this.i18n._("Backup now"),
                    name: 'Backup now',
                    iconCls: 'action-icon',
                    handler: Ext.bind(function(callback) {
                        Ext.MessageBox.wait(this.i18n._("Backing up... This may take a few minutes."), i18n._("Please wait"));
                        this.getRpcNode().sendBackup(Ext.bind(function(result, exception) {
                            Ext.MessageBox.hide();
                            if(Ung.Util.handleException(exception)) return;
                        }, this));
                    }, this)
                }]
            }]
        });
    }
});
//# sourceURL=configuration-backup-settings.js