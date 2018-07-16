Ext.define('Ung.cmp.GridEditorFields', {
    singleton: true,
    alternateClassName: 'Field',

    conditions: function(javaClassValue, conditions){
        return Ung.cmp.ConditionsEditor.build({
            xtype: 'conditionseditor',
            bind: '{record.conditions}',
            fields: {
                type: 'conditionType',
                comparator: 'invert',
                value: 'value',
            },
            javaClassValue: javaClassValue,
            conditions: conditions
        });
    },

    enableRule: function (label) {
        return {
            xtype: 'checkbox',
            fieldLabel: label || 'Enable'.t(),
            bind: '{record.enabled}',
        };
    },

    enableIpv6: {
        xtype: 'checkbox',
        fieldLabel: 'Enable IPv6 Support'.t(),
        bind: '{record.ipv6Enabled}',
    },

    blockedCombo: {
        xtype: 'combo',
        fieldLabel: 'Action'.t(),
        bind: '{record.blocked}',
        store: [[true, 'Block'.t()], [false, 'Pass'.t()]],
        queryMode: 'local',
        editable: false,
    },

    blocked: {
        xtype: 'checkbox',
        bind: '{record.blocked}',
        fieldLabel: 'Block'.t(),
        width: 100
    },

    flagged: {
        xtype: 'checkbox',
        bind: '{record.flagged}',
        fieldLabel: 'Flag'.t(),
        width: 100
    },

    allow: {
        xtype: 'combo',
        fieldLabel: 'Action'.t(),
        bind: '{record.allow}',
        store: [[false, 'Deny'.t()], [true, 'Allow'.t()]],
        queryMode: 'local',
        editable: false
    },

    bypass: {
        xtype: 'combo',
        fieldLabel: 'Action'.t(),
        bind: '{record.bypass}',
        store: [[true, 'Bypass'.t()], [false, 'Process'.t()]],
        queryMode: 'local',
        editable: false
    },

    macAddress: {
        xtype: 'textfield',
        fieldLabel: 'MAC Address'.t(),
        allowBlank: false,
        bind: '{record.macAddress}',
        emptyText: '[enter MAC name]'.t(),
        vtype: 'macAddress',
        maskRe: /[a-fA-F0-9:]/
    },

    ipAddress: {
        xtype: 'textfield',
        fieldLabel: 'Address'.t(),
        emptyText: '[enter address]'.t(),
        bind: '{record.address}',
        allowBlank: false,
        vtype: 'ipAddress',
    },

    network: {
        xtype: 'textfield',
        fieldLabel: 'Network'.t(),
        emptyText: '1.2.3.0'.t(),
        allowBlank: false,
        vtype: 'ipAddress',
        bind: '{record.network}',
    },

    netMask: {
        xtype: 'combo',
        fieldLabel: 'Netmask/Prefix'.t(),
        bind: '{record.prefix}',
        store: Util.getV4NetmaskList(false),
        queryMode: 'local',
        editable: false
    },

    natType: {
        xtype: 'combo',
        fieldLabel: 'NAT Type'.t(),
        bind: '{record.auto}',
        allowBlank: false,
        editable: false,
        store: [[true, 'Auto'.t()], [false, 'Custom'.t()]],
        queryMode: 'local',
    },
    natSource: {
        xtype: 'textfield',
        fieldLabel: 'New Source'.t(),
        width: 100,
        bind: {
            value: '{record.newSource}',
            disabled: '{record.auto}'
        },
        allowBlank: true,
        vtype: 'ipAddress'
    },
    description: {
        xtype: 'textfield',
        fieldLabel: 'Description'.t(),
        bind: '{record.description}',
        emptyText: '[no description]'.t(),
        allowBlank: false
    },

    newDestination: {
        xtype: 'textfield',
        fieldLabel: 'New Destination'.t(),
        bind: '{record.newDestination}',
        allowBlank: false,
        vtype: 'ipAddress'
    },

    newPort: {
        xtype: 'numberfield',
        fieldLabel: 'New Port'.t(),
        width: 100,
        bind: '{record.newPort}',
        allowBlank: true,
        minValue : 1,
        maxValue : 0xFFFF,
        vtype: 'port'
    },

    priority: {
        xtype: 'combo',
        fieldLabel: 'Priority'.t(),
        store: [
            [1, 'Very High'.t()],
            [2, 'High'.t()],
            [3, 'Medium'.t()],
            [4, 'Low'.t()],
            [5, 'Limited'.t()],
            [6, 'Limited More'.t()],
            [7, 'Limited Severely'.t()]
        ],
        bind: '{record.priority}',
        queryMode: 'local',
        editable: false
    },

    // string: {
    //     xtype: 'textfield',
    //     name: "Site",
    //     dataIndex: "string",
    //     fieldLabel: i18n._("Site"),
    //     emptyText: i18n._("[enter site]"),
    //     allowBlank: false,
    //     width: 400,
    // }

});

