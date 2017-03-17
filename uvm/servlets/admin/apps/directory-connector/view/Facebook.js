Ext.define('Ung.apps.directoryconnector.view.Facebook', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-directory-connector-facebook',
    itemId: 'facebook',
    title: 'Facebook'.t(),

    viewModel: true,
    bodyPadding: 10,
    scrollable: true,

    items:[{
        xtype: 'fieldset',
        title: 'Facebook Authentication Connector'.t(),
        items: [{
            xtype: 'component',
            html: Ext.String.format(
                "This allows your server to connect to {0}Facebook{1} in order to identify users for use by Captive Portal.".t(),
                '<b>','</b>')
        }, {
            xtype: 'component',
            html: 'WARNING: Facebook Authentication is experimental and uses an unofficial API. Read the documentation for details.'.t(),
            cls: 'warning',
            style: {color:'red'},
        }, {
            xtype: "checkbox",
            bind: '{settings.facebookSettings.authenticationEnabled}',
            fieldLabel: 'Enable Facebook Authentication Connector'.t(),
            labelWidth: 250,
            listeners: {
                disable: function (ck) {
                    ck.setValue(false);
                }
            }
        },{
            xtype: 'fieldset',
            title: 'Facebook Authentication Test'.t(),
            collapsible: false,
            hidden: true,
            bind: {
                hidden: '{settings.facebookSettings.authenticationEnabled == false}',
            },
            items:[{
                xtype: 'component',
                html: Ext.String.format( 'The {0}Facebook Authentication Test{1} verifies that the server can authenticate the provided username/password.'.t(),'<b>','</b>')
            },{
                xtype:'textfield',
                name: 'facebook_test_username',
                fieldLabel: 'Username'.t(),
            },{
                xtype:'textfield',
                name: 'facebook_test_password',
                fieldLabel: 'Password'.t(),
                inputType: 'password',
            },{
                xtype: 'button',
                text: 'Facebook Authentication Test'.t(),
                iconCls: 'test-icon',
                handler: 'facebookAuthenticationTest'
            }]
        }]
    }]
});
