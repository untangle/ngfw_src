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
            [true, 'Automatic'.t()],
            [false, 'Self assigned'.t()]
        ],
        allowOnlyWhitespace: false,
        forceSelection: true,
        typeAhead: true
    },{
        xtype: 'fieldcontainer',
        layout: 'hbox',
        items: [
            {
                fieldLabel: 'Address Space'.t(),
                xtype: 'textfield',
                vtype: 'cidrAddr',
                bind: {
                    value: '{settings.addressPool}',
                    disabled: '{settings.autoAddressAssignment}',
                    editable: '{!settings.autoAddressAssignment}'
                }
            },
            {
                xtype:'button',
                text: 'Get New Address Space'.t(),
                listeners: {
                    click: 'getNewAddressSpace'
                }
            }
        ]
    }]
});

