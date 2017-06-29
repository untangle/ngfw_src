Ext.define('Ung.apps.directoryconnector.view.Radius', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-directory-connector-radius',
    itemId: 'radius',
    title: 'RADIUS'.t(),

    viewModel: true,
    bodyPadding: 10,
    scrollable: true,

    items:[{
        xtype: 'fieldset',
        title: 'RADIUS Connector'.t(),
        items: [{
            xtype: 'component',
            margin: '10 0 0 0',
            html: Ext.String.format(
                "This allows your server to connect to a {0}RADIUS Server{1} in order to identify users for use by Captive Portal and L2TP/IPsec.".t(),
                '<b>','</b>')
        }, {
            xtype: "checkbox",
            bind: '{settings.radiusSettings.enabled}',
            fieldLabel: 'Enable RADIUS Connector'.t(),
            labelWidth: 200,
            margin: '10 0 10 0',
            listeners: {
                disable: function (ck) {
                    ck.setValue(false);
                }
            }
        }, {
            xtype: 'fieldset',
            border: 0,
            hidden: true,
            disabled: true,
            bind: {
                hidden: '{settings.radiusSettings.enabled == false}',
                disabled: '{settings.radiusSettings.enabled == false}'
            },
            defaults: {
                labelWidth: 190,
                width: 400
            },
            items: [{
                xtype: 'textfield',
                fieldLabel: 'RADIUS Server IP or Hostname'.t(),
                bind: '{settings.radiusSettings.server}'
            }, {
                xtype: 'textfield',
                fieldLabel: 'Authentication Port'.t(),
                bind: '{settings.radiusSettings.authPort}'
            }, {
                xtype: 'textfield',
                fieldLabel: 'Accounting Port'.t(),
                bind: '{settings.radiusSettings.acctPort}'
            },{
                xtype: 'textfield',
                fieldLabel: 'Shared Secret'.t(),
                bind: '{settings.radiusSettings.sharedSecret}',
                maxLength: 48,
                enforceMaxLength: true,
            },{
                xtype: "combo",
                fieldLabel: "Authentication Method".t(),
                bind: '{settings.radiusSettings.authenticationMethod}',
                store: [
                    [ "MSCHAPV2", "MS-CHAP v2".t() ],
                    [ "MSCHAPV1", "MS-CHAP v1".t() ],
                    [ "CHAP", "CHAP".t() ],
                    [ "PAP", "PAP".t() ]
                ],
                queryMode: 'local',
            },{
                xtype: 'container',
                width: 'auto',
                html: 'IMPORTANT'.t() + ':</b>&nbsp;&nbsp' + 'When using Windows as a RADIUS server, the best security and compatibility is achieved by selecting MS-CHAP v2.  Please also make sure the MS-CHAP v2 protocol is enabled for RADIUS clients in the Windows Network Policy Server.'.t()
            }]
        }, {
            xtype: 'fieldset',
            title: 'RADIUS Test'.t(),
            hidden: true,
            disabled: true,
            bind: {
                hidden: '{settings.radiusSettings.enabled == false}',
                disabled: '{settings.radiusSettings.enabled == false}'
            },
            defaults: {
                labelWidth: 190,
                width: 400
            },
            items: [{
                xtype: 'component',
                width: 'auto',
                margin: '10 0 10 0',
                html: Ext.String.format( 'The {0}RADIUS Test{1} verifies that the server can authenticate the provided username/password.'.t(),'<b>','</b>')
            },{
                xtype:'textfield',
                name: 'radiusTestUsername',
                fieldLabel: 'Username'.t(),
            },{
                xtype:'textfield',
                name: 'radiusTestPassword',
                fieldLabel: 'Password'.t(),
            },{
                xtype: 'button',
                text: 'RADIUS Test'.t(),
                iconCls: 'fa fa-cogs',
                labelWidth: 'auto',
                width: 'auto',
                margin: '10 0 10 0',
                handler: 'radiusTest'
            }]
        }]
    }]
});
