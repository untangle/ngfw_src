Ext.define('Webui.configuration-backup.settings', {
    extend:'Ung.NodeWin',
    gridEventLog: null,
    getAppSummary: function() {
        return i18n._('Configuration Backup automatically creates backups of settings uploads them to <i>My Account</i> and <i>Google Drive</i>.');
    },
    initComponent: function(container, position) {
        // builds the 2 tabs
        this.buildGoogle();
        // builds the tab panel with the tabs
        this.buildTabPanel([this.panelGoogle]);
        this.callParent(arguments);
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
        this.googleDriveConfigured = Ung.Main.isGoogleDriveConfigured();

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
                    text: i18n._("Configure Google Drive"),
                    handler: Ung.Main.configureGoogleDrive
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
    }
});
//# sourceURL=configuration-backup-settings.js