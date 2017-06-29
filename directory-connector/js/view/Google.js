Ext.define('Ung.apps.directoryconnector.view.Google', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-directory-connector-google',
    itemId: 'google',
    title: 'Google'.t(),

    viewModel: true,
    bodyPadding: 10,
    scrollable: true,

    items:[{
        xtype: 'fieldset',
        title: 'Google Authentication Connector'.t(),
        name: 'fieldsetEnabledAuth',
        collapsible: false,
        items: [{
            xtype: 'container',
            margin: '10 0 0 0',
            html: Ext.String.format( 'This allows your server to connect to {0}Google{1} in order to identify users for use by Captive Portal.'.t(),'<b>','</b>')
        }, {
            xtype: 'component',
            margin: '10 0 0 0',
            html: 'WARNING: Google Authentication is experimental and uses an unofficial API. Read the documentation for details.'.t(),
            style: {color:'red'},
            cls: 'warning'
        }, {
            xtype: "checkbox",
            bind: '{settings.googleSettings.authenticationEnabled}',
            fieldLabel: 'Enable Google Authentication Connector'.t(),
            labelWidth: 250,
            margin: '10 0 10 0',
            listeners: {
                disable: function (ck) {
                    ck.setValue(false);
                }
            }
        }, {
            xtype: 'fieldset',
            title: 'Google Authentication Test'.t(),
            name: 'fieldsetAuthTest',
            collapsible: false,
            hidden: true,
            bind: {
                hidden: '{settings.googleSettings.authenticationEnabled == false}',
            },
            items: [{
                xtype: 'component',
                margin: '10 0 10 0',
                html: Ext.String.format( 'The {0}Google Authentication Test{1} verifies that the server can authenticate the provided username/password.'.t(),'<b>','</b>')
            },{
                xtype:'textfield',
                name: 'google_test_username',
                fieldLabel: 'Username'.t(),
            },{
                xtype:'textfield',
                name: 'google_test_password',
                inputType: 'password',
                fieldLabel: 'Password'.t(),
            },{
                xtype: 'button',
                text: 'Google Authentication Test'.t(),
                margin: '10 0 10 0',
                iconCls: 'fa fa-cogs',
                handler: 'googleAuthenticationTest'
            }]
        }]
    },{
        xtype: 'fieldset',
        title: 'Google Drive Connector'.t(),
        items: [{
            xtype: 'fieldset',
            name: 'fieldsetDriveDisabled',
            collapsible: false,
            border: 0,
            hidden: true,
            bind: {
                hidden: '{googleDriveIsConfigured == true}',
            },
            items: [{
                xtype: 'component',
                html: 'The Google Drive is unconfigured.'.t(),
                margin: '10 0 0 0',
                style: {color:'red'},
                cls: 'warning'
            }, {
                xtype: "button",
                text: 'Configure Google Drive'.t(),
                iconCls: "fa fa-check-circle",
                margin: '10 0 10 0',
                handler: 'googleDriveConfigure'
            }]
        },{
            xtype: 'fieldset',
            name: 'fieldsetDriveEnabled',
            border: 0,
            collapsible: false,
            hidden: true,
            bind: {
                hidden: '{googleDriveIsConfigured == false}',
            },
            items: [{
                xtype: 'component',
                html: 'The Google Drive is configured.'.t(),
                margin: '10 0 0 0',
                style: {color:'green'}
            }, {
                xtype: "button",
                text: 'Reconfigure Google Drive'.t(),
                iconCls: "fa fa-refresh",
                margin: '10 0 10 0',
                handler: 'googleDriveConfigure'
            }, {
                xtype: "button",
                text: "Disconnect Google Drive".t(),
                margin: '10 0 10 0',
                iconCls: "fa fa-ban",
                handler: 'googleDriveDisconnect'
            }]
        }]
    }]
});
