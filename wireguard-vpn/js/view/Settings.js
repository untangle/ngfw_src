Ext.define('Ung.apps.wireguard-vpn.view.Settings', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-wireguard-vpn-settings',
    itemId: 'settings',
    title: 'Settings'.t(),
    scrollable: true,

    withValidation: true,
    padding: 10,

    defaults: {
        labelWidth: 200
    },

    items: [{
        fieldLabel: 'Listen port'.t(),
        xtype: 'textfield',
        vtype: 'isSinglePortValid',
        bind: {
            value: '{settings.listenPort}'
        },
        allowBlank: false
    },{
        fieldLabel: 'Keepalive interval (seconds)'.t(),
        xtype: 'textfield',
        vtype: 'keepalive',
        bind: {
            value: '{settings.keepaliveInterval}'
        },
        allowBlank: false
    },{
        fieldLabel: 'MTU (bytes)'.t(),
        xtype: 'textfield',
        vtype: 'mtu',
        bind: {
            value: '{settings.mtu}'
        },
        allowBlank: false
    },{
        fieldLabel: 'IP Address Assignment'.t(),
        xtype: 'combobox',
        editable: false,
        bind: {
            value: '{settings.autoAddressAssignment}'
        },
        queryMode: 'local',
        store: [
            [true, 'Automatic'.t()],
            [false, 'Self assigned'.t()]
        ]
    },{
        xtype: 'fieldcontainer',
        layout: 'hbox',
        items: [
            {
                xtype: 'textfield',
                fieldLabel: 'Address Space'.t(),
                labelWidth: 200,
                vtype: 'cidrAddr',
                bind: {
                    value: '{settings.addressPool}',
                    disabled: '{settings.autoAddressAssignment}'
                }
            },
            {
                xtype:'button',
                text: 'Get New Address Space'.t(),
                handler: 'getNewAddressSpace',
                margin: '0 0 0 10',
                bind: {
                    disabled: '{settings.autoAddressAssignment}'
                }
            }
        ]
    }]
});

