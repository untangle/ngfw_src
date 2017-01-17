Ext.define('Ung.config.network.PortForwardRules', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.config.network.portforwardrules',

    viewModel: true,

    title: 'Port Forward Rules'.t(),

    layout: { type: 'vbox', align: 'stretch' },

    tbar: [{
        xtype: 'displayfield',
        value: "Port Forward rules forward sessions matching the configured criteria from a public IP to an IP on an internal (NAT'd) network. The rules are evaluated in order.".t()
    }],

    portForwardConditions: [
        {name:"DST_LOCAL",displayName: 'Destined Local'.t(), type: "boolean", visible: true},
        {name:"DST_ADDR",displayName: 'Destination Address'.t(), type: "text", visible: true, vtype:"ipMatcher"},
        {name:"DST_PORT",displayName: 'Destination Port'.t(), type: "text",vtype:"portMatcher", visible: true},
        {name:"SRC_ADDR",displayName: 'Source Address'.t(), type: "text", visible: true, vtype:"ipMatcher"},
        {name:"SRC_PORT",displayName: 'Source Port'.t(), type: "text",vtype:"portMatcher", visible: rpc.isExpertMode},
        {name:"SRC_INTF",displayName: 'Source Interface'.t(), type: "checkgroup", values: ['a', 'b'], visible: true},
        {name:"PROTOCOL",displayName: 'Protocol'.t(), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"],["ICMP","ICMP"],["GRE","GRE"],["ESP","ESP"],["AH","AH"],["SCTP","SCTP"]], visible: true}
    ],

    items: [{
        xtype: 'grid',
        // columnFeatures: ['edit'],
        flex: 3,
        tbar: [{
            text: 'Add Rule'.t(),
            iconCls: 'fa fa-plus'
        }],
        leadingBufferZone: 8,
        trailingBufferZone: 8,
        plugins: [{
            ptype: 'rowwidget',
            widget: {
                xtype: 'grid',
                hideHeaders: true,
                bind: {
                    store: {
                        data: '{record.conditions.list}'
                    },
                    // title: 'Conditions'.t()
                },
                // columns: [{
                //     text: 'Condition',
                //     // dataIndex: 'conditionType',
                //     renderer: function (val, record) {
                //         console.log(record);
                //         return record.conditionType + ' ' + (record.get('invert') ? 'is Not' : 'is') + ' ' + record.get('value');
                //     }
                // }]
                fields: ['conditionType', 'invert', 'value'],
                columns: [{
                    xtype: 'widgetcolumn',
                    text: 'Condition',
                    width: 200,
                    // dataIndex: 'conditionType',
                    widget: {
                        xtype: 'combo',
                        editable: false,
                        bind: '{record.conditionType}',
                        store: [
                            ['DST_LOCAL', 'Destined Local'.t()],
                            ['DST_ADDR', 'Destinatoin Address'.t()],
                        ]
                    }
                }, {
                    xtype: 'widgetcolumn',
                    widget: {
                        xtype: 'combo',
                        editable: false,
                        bind: '{record.invert}',
                        store: [[true, 'is not'], [false, 'is']]
                    }
                }, {
                    xtype: 'widgetcolumn',
                    widget: {
                        xtype: 'container',
                        items: [{
                            xtype: 'textfield',
                            hidden: true,
                            bind: {
                                value: '{record.value}',
                                hidden: '{record.conditionType === "DST_LOCAL"}'
                            }
                        }, {
                            html: 'b'
                        }]
                    }
                }]
            }
        }],

        bind: '{portforwardrules}',
        fields: [{
            name: 'ruleId'
        }, {
            name: 'enabled'
        }, {
            name: 'newDestination',
            sortType: 'asIp'
        }, {
            name: 'newPort',
            sortType: 'asInt'
        }, {
            name: 'conditions'
        }, {
            name: 'description'
        }, {
            name: 'simple'
        }, {
            name: 'javaClass'
        }],
        columns: [{
            header: 'Rule Id'.t(),
            width: 50,
            dataIndex: 'ruleId',
            renderer: function(value) {
                if (value < 0) {
                    return 'new'.t();
                } else {
                    return value;
                }
            }
        }, {
            xtype:'checkcolumn',
            header: 'Enable',
            dataIndex: 'enabled',
            resizable: false,
            width: 55
        }, {
            header: 'Description',
            width: 200,
            dataIndex: 'description',
            flex: 1,
            editor: {
                xtype:'textfield',
                emptyText: '[no description]'.t()
            }
        }, {
            header: 'New Destination'.t(),
            dataIndex: 'newDestination',
            width: 150
        }, {
            header: 'New Port'.t(),
            dataIndex: 'newPort',
            width: 65
        }],
    }, {
        xtype: 'fieldset',
        flex: 2,
        margin: 10,
        // border: true,
        collapsible: true,
        collapsed: false,
        autoScroll: true,
        title: 'The following ports are currently reserved and can not be forwarded:'.t(),
        items: [{
            xtype: 'component',
            name: 'portForwardWarnings',
            html: ' '
        }]
    }]
});