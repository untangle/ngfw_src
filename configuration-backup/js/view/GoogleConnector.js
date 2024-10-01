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
            hidden: true,
            disabled: true,
             bind: {
                 hidden: '{googleDriveIsConfigured == true}',
                 disabled: '{googleDriveIsConfigured == true}'
             },
            items: [{
                xtype: 'component',
                html: 'The Google Connector must be configured in order to backup to Google Drive.'.t(),
                style: {color:'red'},
                cls: 'warning'
            }, {
                xtype: "button",
                margin: '5 0 0 0',
                text: 'Configure Google Drive'.t(),
                bind:{
                    handler: '{googleDriveConfigure}'
                }
            }]
        } ,{
            xtype: 'fieldset',
            collapsible: false,
            hidden: true,
            disabled: true,
             bind: {
                 hidden: '{googleDriveIsConfigured == false}',
                 disabled: '{googleDriveIsConfigured == false}'
             },
            items: [{
                xtype: 'component',
                html: 'The Google Connector is configured.'.t(),
                style: {color:'green'}
            }, {
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
                    xtype: "textfield",
                    margin: '10',
                    readOnly: true,
                    regex: /^[\w\. \/]+$/,
                    regexText: "The field can have only alphanumerics, spaces, or periods.".t(),
                    fieldLabel: "Google Drive Directory".t(),
                    labelWidth: 200,
                    bind: '{settings.googleDriveDirectory}',
                    autoEl: {
                        tag: 'div',
                        'data-qtip': "The destination directory in google drive.".t()
                    }
                }, {
                    xtype: 'button',
                    text: 'Select Directory',
                    itemId: 'selectDirButton',
                    margin: '10',
                    // handler: 'handleSelectDirectory',
                    handler: 'loadExternalScripts'
                }]
            }]
        }]
    }]
});
