Ext.define('Ung.config.network.PortForwardRules', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.config.network.portforwardrules',

    viewModel: true,

    requires: [
        'Ung.config.network.ConditionWidget'
    ],

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
        trackMouseOver: false,
        disableSelection: true,
        columnLines: true,
        // plugins: [{
        //     ptype: 'rowwidget',
        //     widget: {
        //         xtype: 'dataview',
        //         bind: '{record.conditions.list}',
        //         tpl: '<tpl for=".">' +
        //             '<span>{conditionType}</span>' +
        //         '</tpl>',
        //         itemSelector: 'span'
        //     }
        // }],
        plugins: [{
            ptype: 'rowwidget',
            widget: {
                xtype: 'ung.condwidget',

                bind: {
                    // data: {
                    //     rule: '{record}'
                    // },
                    store: {
                        type: 'ruleconditions',
                        data: '{record.conditions.list}'
                    }
                    // title: 'Conditions'.t()
                },
            },
            // onWidgetAttach: function () {
            //     console.log('widget attach');
            // }
        },
        // {
        //     ptype: 'rowediting',
        //     clicksToMoveEditor: 1,
        //     autoCancel: false
        // }
        ],

        bind: '{portforwardrules}',
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
            width: 55,
            // renderer: function (val) {
            //     return '<i class="fa + ' + (val ? 'fa-check' : 'fa-check-o') + '"></i>';
            // }
        }, {
            header: 'Description',
            flex: 1,
            width: 200,
            dataIndex: 'description',
            editor: {
                xtype:'textfield',
                emptyText: '[no description]'.t()
            }
        },
        // {
        //     xtype: 'actioncolumn',
        //     iconCls: 'fa fa-edit',

        //     handler: function (view, rowIndex, colIndex, item, e, record) {
        //         console.log(record);
        //         Ext.widget('ung.config.network.ruleeditorwin', {
        //             // config: {
        //                 conditions: [
        //                     {name:"DST_LOCAL",displayName: 'Destined Local'.t(), type: "boolean", visible: true},
        //                     {name:"DST_ADDR",displayName: 'Destination Address'.t(), type: "text", visible: true, vtype:"ipMatcher"},
        //                     {name:"DST_PORT",displayName: 'Destination Port'.t(), type: "text",vtype:"portMatcher", visible: true},
        //                     {name:"SRC_ADDR",displayName: 'Source Address'.t(), type: "text", visible: true, vtype:"ipMatcher"},
        //                     {name:"SRC_PORT",displayName: 'Source Port'.t(), type: "text",vtype:"portMatcher", visible: rpc.isExpertMode},
        //                     {name:"SRC_INTF",displayName: 'Source Interface'.t(), type: "checkgroup", values: Ung.Util.getInterfaceList(true, true), visible: true},
        //                     {name:"PROTOCOL",displayName: 'Protocol'.t(), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"],["ICMP","ICMP"],["GRE","GRE"],["ESP","ESP"],["AH","AH"],["SCTP","SCTP"]], visible: true}
        //                 ],
        //                 rule: record,
        //             // }
        //             // conditions: {
        //             //     DST_LOCAL: {displayName: 'Destined Local'.t(), type: "boolean", visible: true},
        //             //     DST_ADDR: {displayName: 'Destination Address'.t(), type: "text", visible: true, vtype:"ipMatcher"},
        //             //     DST_PORT: {displayName: 'Destination Port'.t(), type: "text", vtype:"portMatcher", visible: true},
        //             //     PROTOCOL: {displayName: 'Protocol'.t(), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"],["ICMP","ICMP"],["GRE","GRE"],["ESP","ESP"],["AH","AH"],["SCTP","SCTP"]], visible: true}
        //             // },
        //             // viewModel: {
        //             //     data: {
        //             //         rule: record
        //             //     }
        //             // },
        //         });
        //     }
        // },
        {
            header: 'Conditions'.t(),
            dataIndex: 'conditions',
            renderer: function (conds) {
                var resp = '', i, cond;
                for (i = 0; i < conds.list.length; i += 1) {
                    cond = conds.list[i];
                    resp += cond.conditionType + (cond.invert ? ' &ne; ' : ' = ') + cond.value + ', ';
                }
                //console.log(val);
                return resp;
            }
            // width: 150
        },
        {
            // xtype: 'widgetcolumn',
            // tdCls: 'no-padding',
            // flex: 1,
            // widget: {
            //     xtype: 'ung.config.network.ruleeditor',
            //     conditions: [
            //         {name:"DST_LOCAL", text: 'Destined Local'.t(), type: "boolean", visible: true},
            //         {name:"DST_ADDR", text: 'Destination Address'.t(), type: "text", visible: true, vtype:"ipMatcher"},
            //         {name:"DST_PORT", text: 'Destination Port'.t(), type: "text",vtype:"portMatcher", visible: true},
            //         {name:"SRC_ADDR", text: 'Source Address'.t(), type: "text", visible: true, vtype:"ipMatcher"},
            //         {name:"SRC_PORT", text: 'Source Port'.t(), type: "text",vtype:"portMatcher", visible: rpc.isExpertMode},
            //         {name:"SRC_INTF", text: 'Source Interface'.t(), type: "checkgroup", values: ['a', 'b'], visible: true},
            //         {name:"PROTOCOL", text: 'Protocol'.t(), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"],["ICMP","ICMP"],["GRE","GRE"],["ESP","ESP"],["AH","AH"],["SCTP","SCTP"]], visible: true}
            //     ],
            //     // portForwardConditions: [
            //     //     {name:"DST_LOCAL",displayName: 'Destined Local'.t(), type: "boolean", visible: true},
            //     //     {name:"DST_ADDR",displayName: 'Destination Address'.t(), type: "text", visible: true, vtype:"ipMatcher"},
            //     //     {name:"DST_PORT",displayName: 'Destination Port'.t(), type: "text",vtype:"portMatcher", visible: true},
            //     //     {name:"SRC_ADDR",displayName: 'Source Address'.t(), type: "text", visible: true, vtype:"ipMatcher"},
            //     //     {name:"SRC_PORT",displayName: 'Source Port'.t(), type: "text",vtype:"portMatcher", visible: rpc.isExpertMode},
            //     //     {name:"SRC_INTF",displayName: 'Source Interface'.t(), type: "checkgroup", values: ['a', 'b'], visible: true},
            //     //     {name:"PROTOCOL",displayName: 'Protocol'.t(), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"],["ICMP","ICMP"],["GRE","GRE"],["ESP","ESP"],["AH","AH"],["SCTP","SCTP"]], visible: true}
            //     // ],
            //     bind: {
            //         store: {
            //             type: 'ruleconditions',
            //             data: '{record.conditions.list}'
            //         }
            //     }
            // }
        },
        {
            header: 'New Destination'.t(),
            dataIndex: 'newDestination',
            width: 150,
            editor: {
                xtype:'textfield',
            }
        }, {
            header: 'New Port'.t(),
            dataIndex: 'newPort',
            width: 65,
            editor: {
                // xtype: 'ung.config.network.ruleeditor',
                // bind: {
                //     store: {
                //         data: '{record.conditions.list}'
                //     }
                // }
            }
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