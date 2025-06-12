Ext.define('Ung.apps.configurationbackup.view.GoogleConnector', {
    extend: 'Ext.form.Panel',
    alias: 'widget.app-configuration-backup-googleconnector',
    itemId: 'google-connector',
    title: 'Google Connector'.t(),
    scrollable: true,
    withValidation: false,
    viewModel: true,
    bodyPadding: 10,

    items: [{
        title: 'Google Connector'.t(),
        items:[{
            xtype: 'component',
            margin: '10 0 0 10',
            bind: {
                html: '{driveConfiguredText}',
                style: { color: '{googleDriveIsConfigured ? "green" : "red"}'}
            }
        },{
            xtype: "button",
            text: 'Configure Google Drive'.t(),
            iconCls: "fa fa-check-circle",
            margin: '10 0 10 10',
            bind:{
                handler: '{googleDriveConfigure}'
            }
        },{
            xtype: 'fieldset',
            collapsible: false,
            hidden: true,
            disabled: true,
             bind: {
                 hidden: '{googleDriveIsConfigured == false}',
                 disabled: '{googleDriveIsConfigured == false}'
             },
            items: [{
                xtype: "checkbox",
                bind: '{settings.googleDriveEnabled}',
                fieldLabel: 'Enable upload to Google Drive'.t(),
                labelWidth: 200,
                autoEl: {
                    tag: 'div',
                    'data-qtip': "If enabled and configured Configuration Backup will upload backups to google drive.".t()
                },
                listeners: {
                    disable: function (ck) {
                        ck.setValue(false);
                    }
                }
            }, {
                xtype: 'fieldset',
                hidden: true,
                disabled: true,
                bind: {
                    hidden: '{settings.googleDriveEnabled == false}',
                    disabled: '{settings.googleDriveEnabled == false}'
                },
                layout: {
                    type: 'hbox'
                },
                items: [{
                    xtype: 'displayfield',
                    margin: '10 0 10 10',
                    fieldLabel: 'Google Drive Directory',
                    labelWidth: 150,
                    renderer: function() {
                        var rootDirectory = Rpc.directData('rpc.UvmContext.googleManager.getAppSpecificGoogleDrivePath', null);
                        if (!rootDirectory || rootDirectory.trim() === '')
                            return '';
                        return Ext.String.format('<strong><span class="cond-val">{0}</span></strong>', rootDirectory + " /");
                    }
                },{
                    xtype: 'textfield',
                    margin: '10 10 10 0',
                    regex: /^[\w\. \/]+$/,
                    regexText: "The field can have only alphanumerics, spaces, or periods.".t(),
                    bind: '{settings.googleDriveDirectory}',
                    width: 200,
                    emptyText: 'Enter folder name',
                    autoEl: {
                        tag: 'div',
                        'data-qtip': "The destination directory in google drive.".t()
                    }
                }]
            }]
        }]
    }]
});
