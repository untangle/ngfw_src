Ext.define('Ung.apps.wireguard-vpn.view.Settings', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-wireguard-vpn-settings',
    itemId: 'settings',
    title: 'Settings'.t(),
    scrollable: true,

    withValidation: true,
    padding: '8 5',

    defaults: {
        labelWidth: 175
    },

    items: [{
        fieldLabel: 'Listen port'.t(),
        xtype: 'textfield',
        vtype: 'isSinglePortValid',
        maxLength: 5,
        enforceMaxLength :true,
        bind: {
            value: '{settings.listenPort}'
        },
        allowBlank: false
    },{
        fieldLabel: 'Keepalive interval'.t(),
        xtype: 'textfield',
        maxLength: 5,
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
    }, {
        xtype: 'fieldset',
        title: 'Remote Client Configuration'.t(),
        layout: {
            type: 'vbox'
        },
        defaults: {
            labelWidth: 165,
            padding: "0 0 10 0"
        },
        items:[{
            xtype: 'textfield',
            fieldLabel: 'DNS Server'.t(),
            vtype: 'ipMatcher',
            bind: {
                value: '{settings.dnsServer}'
            }
        },{
            xtype: 'textarea',
            fieldLabel: 'Networks'.t(),
            vtype: 'cidrBlockArea',
            allowBlank: true,
            height: 50,
            bind: {
                value: '{settings.networks}'
            }
        }]
    }, {
        xtype: 'fieldset',
        title: 'Peer IP Address Pool'.t(),
        layout: {
            type: 'vbox'
        },
        defaults: {
            labelWidth: 165
        },
        items:[{
            fieldLabel: 'Assignment'.t(),
            xtype: 'combobox',
            bind: {
                value: '{settings.autoAddressAssignment}'
            },
            editable: false,
            queryMode: 'local',
            store: [
                [true, 'Automatic'.t()],
                [false, 'Self-assigned'.t()]
            ],
            forceSelection: true
        },{
            xtype: 'fieldcontainer',
            layout: 'hbox',
            defaults: {
                labelWidth: 165
            },
            items: [{
                    fieldLabel: 'Network Space'.t(),
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
                    text: 'New Network Space'.t(),
                    bind:{
                        disabled: '{!settings.autoAddressAssignment}'
                    },
                    listeners: {
                        click: 'getNewAddressSpace'
                    }
                }
            ]
        }]
    }]
});

