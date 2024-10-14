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
            xtype: 'fieldset',
            collapsible: false,
            border: 0,
            hidden: true,
            disabled: true,
            bind: {
                hidden: '{googleDriveIsConfigured == true}',
                disabled: '{googleDriveIsConfigured == true}'
            },
            items: [{
                xtype: 'component',
                html: 'The Google Connector must be configured in order to backup to Google Drive.'.t(),
                margin: '10 0 0 0',
                style: {color:'red'},
                cls: 'warning'
            }]
        },{
            xtype: 'fieldset',
            collapsible: false,
            border: 0,
            hidden: true,
            disabled: true,
            bind: {
                hidden: '{googleDriveIsConfigured == false || rootDirectory}',
                disabled: '{googleDriveIsConfigured == false || rootDirectory}'
            },
            items: [{
                xtype: 'component',
                html: 'The Google Drive directory must be selected in order to backup to Google Drive.'.t(),
                margin: '10 0 0 0',
                style: {color:'red'},
                cls: 'warning'
            }]
        },{
            xtype: 'fieldset',
            collapsible: false,
            border: 0,
            hidden: true,
            disabled: true,
            bind: {
                hidden: '{googleDriveIsConfigured == false || !rootDirectory}',
                disabled: '{googleDriveIsConfigured == false || !rootDirectory}'
            },
            items: [{
                xtype: 'component',
                html: 'The Google Connector is configured.'.t(),
                margin: '10 0 0 0',
                style: {color:'green'}
            }]
        },{
            xtype: "button",
            text: 'Configure Google Drive'.t(),
            iconCls: "fa fa-check-circle",
            margin: '0 0 10 10',
            bind:{
                handler: '{googleDriveConfigure}'
            }
        },{
            xtype: 'fieldset',
            collapsible: false,
            hidden: true,
            disabled: true,
             bind: {
                 hidden: '{googleDriveIsConfigured == false || !rootDirectory}',
                 disabled: '{googleDriveIsConfigured == false || !rootDirectory}'
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
                        return Ext.String.format('<strong><span class="cond-val"> {0}</span></strong>', rootDirectory + " /");
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
