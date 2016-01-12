Ext.define('Webui.untangle-node-configuration-backup.settings', {
    extend:'Ung.NodeWin',
    gridEventLog: null,
    getAppSummary: function() {
        return i18n._('Configuration Backup automatically saves your configuration settings and uploads them to <i>My Account</i>');
    },
    initComponent: function(container, position) {
        // builds the 2 tabs
        this.buildGoogle();
        // builds the tab panel with the tabs
        this.buildTabPanel([this.panelGoogle]);
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
    getStatusMessage: function () {
        var event = this.getLatestEvent();
        if (event == null)
            return i18n._("Backup pending.");
        else if (event.success == true)
            return i18n._("Successfully backed up on") + " " + i18n.timestampFormat(event.timeStamp) + "</i>";
        else
            return i18n._("Last backup failed on") + " <i>" + i18n.timestampFormat(event.timeStamp) + "</i>";
    },
    // Status Panel
    buildStatus: function() {
        var contactName;
        try {
            contactName = Ung.Main.getBrandingManager().getContactName();
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }
        this.panelStatus = Ext.create('Ung.panel.Status', {
            settingsCmp: this,
            helpSource: 'configuration_backup_status',
            itemsToAppend: [{
                xtype: 'fieldset',
                title: i18n._('Status'),
                html: this.getStatusMessage()
            }, {
                xtype: 'fieldset',
                title: i18n._('Backup Now'),
                items: [{
                    xtype: 'container',
                    html: i18n._('Force an immediate backup now.')
                }, {
                    xtype: 'button',
                    margin: '10 0 0 0',
                    text: i18n._("Backup now"),
                    name: 'Backup now',
                    iconCls: 'action-icon',
                    handler: Ext.bind(function(callback) {
                        Ext.MessageBox.wait(i18n._("Backing up... This may take a few minutes."), i18n._("Please wait"));
                        this.getRpcNode().sendBackup(Ext.bind(function(result, exception) {
                            Ext.MessageBox.hide();
                            if(Ung.Util.handleException(exception)) return;
                        }, this));
                    }, this)
                }]
            }]
        });
    },
    buildGoogle: function() {
        var directoryConnectorLicense;
        try {
            directoryConnectorLicense = Ung.Main.getLicenseManager().isLicenseValid("untangle-node-adconnector") || Ung.Main.getLicenseManager().isLicenseValid("untangle-node-directory-connector");
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }
        var directoryConnectorNode;
        try {
            directoryConnectorNode = rpc.nodeManager.node("untangle-node-directory-connector");
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }
        this.googleDriveConfigured =
            directoryConnectorLicense != null &&
            directoryConnectorLicense &&
            directoryConnectorNode != null &&
            directoryConnectorNode.getGoogleManager() != null &&
            directoryConnectorNode.getGoogleManager().isGoogleDriveConnected();

        this.panelGoogle = Ext.create('Ext.panel.Panel',{
            name: 'Google Drive',
            helpSource: 'directory_connector_google_connector',
            title: i18n._('Google Connector'),
            cls: 'ung-panel',
            autoScroll: true,
            items: [{
                title: i18n._('Google Connector'),
                name: 'Google Connector',
                xtype: 'fieldset',
                labelWidth: 250,
                items: [{
                    xtype: 'container',
                    margin: '5 0 15 0',
                    html: i18n._('If enabled, Configuration Backup uploads backup files to Google Drive.')
                }, {
                    xtype: 'component',
                    html: (this.googleDriveConfigured ? i18n._("The Google Connector is configured.") : i18n._("The Google Connector is unconfigured.")),
                    style: (this.googleDriveConfigured ? {color:'green'} : {color:'red'}),
                    cls: (this.googleDriveConfigured ? null : 'warning')
                }, {
                    xtype: "button",
                    margin: '5 0 0 0',
                    disabled: this.googleDriveConfigured,
                    name: "configureGoogleDrive",
                    text: i18n._("Configure Google Drive"),
                    handler: Ext.bind(this.configureGoogleDrive, this )
                },{
                    xtype: "checkbox",
                    margin: '15 0 0 0',
                    disabled: !this.googleDriveConfigured,
                    boxLabel: i18n._("Upload to Google Drive"),
                    hideLabel: true,
                    checked: this.settings.googleDriveEnabled,
                    listeners: {
                        "change": Ext.bind(function(elem, checked) {
                            this.settings.googleDriveEnabled = checked;
                        }, this),
                        "render": function(obj) {
                            obj.getEl().set({'data-qtip': i18n._("If enabled and configured Configuration Backup will upload backups to google drive.")});
                        }
                    }
                },{
                    xtype: "textfield",
                    disabled: !this.googleDriveConfigured,
                    regex: /^[\w\. \/]+$/,
                    regexText: i18n._("The field can have only alphanumerics, spaces, or periods."),
                    fieldLabel: i18n._("Google Drive Directory"),
                    labelWidth: 150,
                    value: this.settings.googleDriveDirectory,
                    listeners: {
                        "change": Ext.bind(function(elem, checked) {
                            this.settings.googleDriveDirectory = checked;
                        }, this),
                        "render": function(obj) {
                            obj.getEl().set({'data-qtip': i18n._("The destination directory in google drive.")});
                        }
                    }
                }]
            }]
        });
    },
    // There is no way to select the google tab because we don't get a callback once the settings are loaded.
    configureGoogleDrive: function() {
        var node = Ung.Main.getNode("untangle-node-directory-connector");
        if (node != null) {
            var nodeCmp = Ung.Node.getCmp(node.nodeId);
            if (nodeCmp != null) {
                nodeCmp.loadSettings();
            }
        }
    }
});
//# sourceURL=configuration-backup-settings.js