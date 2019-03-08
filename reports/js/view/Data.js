Ext.define('Ung.apps.reports.view.Data', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-reports-data',
    itemId: 'data',
    title: 'Data'.t(),
    scrollable: true,

    defaults: {
        xtype: 'fieldset',
        padding: 10,
        margin: 10
    },
    items: [{
        title: 'Data Retention'.t(),
        labelWidth: 150,
        items: [{
            xtype: 'component',
            margin: '0 0 5 0',
            html: 'Keep event data for this number of days. The smaller the number the lower the disk space requirements.'.t()
        },{
            xtype: 'numberfield',
            fieldLabel: 'Data Retention days'.t(),
            bind: '{settings.dbRetention}',
            toValidate: true,
            labelWidth: 150,
            width: 220,
            allowDecimals: false,
            minValue: 1,
            maxValue: 366,
            // hideTrigger:true,
        }]
    },{
        title: 'Google Drive Backup'.t(),
        labelWidth: 150,
        items: [{
            xtype: 'container',
            margin: '5 0 15 0',
            html: 'If enabled, Configuration Backup uploads reports data backup files to Google Drive.'.t()
        }, {
            xtype: 'component',
            bind: {
                html: '{driveConfiguredText}',
                style: { color: '{googleDriveConfigured ? "green" : "red"}'}
            }
        }, {
            xtype: 'button',
            text: 'Configure Google Drive'.t(),
            margin: '10 0 15 0',
            handler: 'configureGoogleDrive'
        }, {
            xtype: 'checkbox',
            boxLabel: 'Upload Data to Google Drive'.t(),
            disabled: true,
            bind: {
                value: '{settings.googleDriveUploadData}',
                disabled: '{!googleDriveConfigured}'
            },
            listeners: {
                render: function(obj) {
                    obj.getEl().set({'data-qtip': 'If enabled and configured Configuration Backup will upload backups to google drive.'.t()});
                }
            }
        }, {
            xtype: 'checkbox',
            boxLabel: 'Upload CSVs to Google Drive'.t(),
            disabled: true,
            bind: {
                value: '{settings.googleDriveUploadCsv}',
                disabled: '{!googleDriveConfigured}'
            },
            listeners: {
                render: function(obj) {
                    obj.getEl().set({'data-qtip': 'If enabled and configured Configuration Backup will upload backups to google drive.'.t()});
                }
            }
        }, {
            xtype: 'textfield',
            regex: /^[\w\. \/]+$/,
            regexText: 'The field can have only alphanumerics, spaces, or periods.'.t(),
            fieldLabel: 'Google Drive Directory'.t(),
            labelWidth: 150,
            disabled: true,
            bind: {
                value: '{settings.googleDriveDirectory}',
                disabled: '{!googleDriveConfigured}'
            },
            listeners: {
                // change: Ext.bind(function(elem, checked) {
                //     this.getSettings().googleDriveDirectory = checked;
                // }, this),
                render: function(obj) {
                    obj.getEl().set({'data-qtip': 'The destination directory in google drive.'.t()});
                }
            }
        }]
    }, {
        title: 'Import / Restore Data Backup Files'.t(),
        labelWidth: 150,
        items: [{
            xtype: 'form',
            url: 'upload',
            border: false,
            items: [{
                xtype: 'filefield',
                fieldLabel: 'File'.t(),
                name: 'uploadDataFile',
                width: 500,
                labelWidth: 50,
                allowBlank: false,
                validateOnBlur: false
            }, {
                xtype: 'button',
                text: 'Upload'.t(),
                formBind: true,
                handler: 'onUpload'
            }, {
                xtype: 'hidden',
                name: 'type',
                value: 'reportsDataRestore'
            }]
        }]
    }, {
        // fix isExpertMode
        title: 'Data'.t(),
        hidden: !Rpc.directData('rpc.isExpertMode'),
        items: [{
            xtype: 'textfield',
            fieldLabel: 'Host'.t(),
            width: 300,
            bind: '{settings.dbHost}',
            allowBlank: false,
            blankText: 'A Host must be specified.'.t()
        }, {
            xtype: 'numberfield',
            fieldLabel: 'Port'.t(),
            width: 200,
            bind: '{settings.dbPort}',
            allowDecimals: false,
            minValue: 0,
            allowBlank: false,
            blankText: 'You must provide a valid port.'.t(),
            vtype: 'port'
        }, {
            xtype: 'textfield',
            fieldLabel: 'User'.t(),
            width: 300,
            bind: '{settings.dbUser}',
            allowBlank: false,
            blankText: 'A User must be specified.'.t()
        }, {
            xtype: 'textfield',
            fieldLabel: 'Password'.t(),
            width: 300,
            bind: '{settings.dbPassword}',
            allowBlank: false,
            blankText: 'A Password must be specified.'.t()
        }, {
            xtype: 'textfield',
            fieldLabel: 'Name'.t(),
            width: 300,
            bind: '{settings.dbName}',
            allowBlank: false,
            blankText: 'A Name must be specified.'.t()
        }]
    }],
});
