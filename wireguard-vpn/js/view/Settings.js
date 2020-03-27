Ext.define('Ung.apps.wireguard-vpn.view.Settings', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-wireguard-vpn-settings',
    itemId: 'settings',
    title: 'Settings'.t(),
    scrollable: true,

    withValidation: true,
    padding: '8 5',

    items: [{
        fieldLabel: 'Listen port'.t(),
        xtype: 'textfield',
        vtype: 'isSinglePortValid',
        bind: {
            value: '{settings.listenPort}'
        },
        allowBlank: false
    },{
        fieldLabel: 'Keepalive interval'.t(),
        xtype: 'textfield',
        vtype: 'keepalive',
        bind: {
            value: '{settings.keepaliveInterval}'
        },
        allowBlank: false
    },{
        fieldLabel: 'MTU'.t(),
        xtype: 'textfield',
        vtype: 'mtu',
        bind: {
            value: '{settings.mtu}'
        },
        allowBlank: false
    },{
        fieldLabel: 'IP Address Assignment'.t(),
        xtype: 'combobox',
        bind: {
            value: '{settings.autoAddressAssignment}'
        },
        queryMode: 'local',
        store: [
            [true, 'Automatic'],
            [false, 'Self assigned']

        ],
        allowOnlyWhitespace: false,
        forceSelection: true,
        typeAhead: true
    },{
        fieldLabel: 'Address Space'.t(),
        xtype: 'textfield',
        vtype: 'cidrBlockOnlyRanges',
        bind: {
            value: '{settings.addressPool}',
            disabled: '{settings.autoAddressAssignment}',
            editable: '{!settings.autoAddressAssignment}'
        }
    }
    ]
});

