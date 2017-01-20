Ext.define('Ung.config.network.ConditionWidget', {
    extend: 'Ext.grid.Panel',

    xtype: 'ung.condwidget',

    requires: [
        'Ung.config.network.ConditionWidgetController'
    ],

    controller: 'condwidget',

    // bind: {
    //     store: {
    //         data: '{record.conditions.list}'
    //     },
    // },

    // layout: 'fit',

    trackMouseOver: false,
    disableSelection: true,

    conditions: [
        {name:"DST_LOCAL",displayName: 'Destined Local'.t(), type: "boolean", visible: true},
        {name:"DST_ADDR",displayName: 'Destination Address'.t(), type: "textfield", visible: true, vtype:"ipMatcher"},
        {name:"DST_PORT",displayName: 'Destination Port'.t(), type: "textfield",vtype:"portMatcher", visible: true},
        {name:"SRC_ADDR",displayName: 'Source Address'.t(), type: "textfield", visible: true, vtype:"ipMatcher"},
        {name:"SRC_PORT",displayName: 'Source Port'.t(), type: "textfield",vtype:"portMatcher", visible: rpc.isExpertMode},
        {name:"SRC_INTF",displayName: 'Source Interface'.t(), type: "checkboxgroup", values: [['a', 'a'], ['b', 'b']], visible: true},
        {name:"PROTOCOL",displayName: 'Protocol'.t(), type: "checkboxgroup", values: [["TCP","TCP"],["UDP","UDP"],["ICMP","ICMP"],["GRE","GRE"],["ESP","ESP"],["AH","AH"],["SCTP","SCTP"]], visible: true}
    ],
    // border: false,
    hideHeaders: true,
    // columns: [{
    //     text: 'Condition',
    //     // dataIndex: 'conditionType',
    //     renderer: function (val, record) {
    //         console.log(record);
    //         return record.conditionType + ' ' + (record.get('invert') ? 'is Not' : 'is') + ' ' + record.get('value');
    //     }
    // }]
    dockedItems: [{
        xtype: 'toolbar',
        dock: 'left',
        items: [{
            text: 'Add Condition',
            itemId: 'conditionsBtn',
            menuAlign: 'tl-br',
            menu: Ext.create('Ext.menu.Menu', {
                items: [{
                    text: 'regular item 1'
                },{
                    text: 'regular item 2'
                },{
                    text: 'regular item 3'
                }
            ]}),
            listeners: {
                // click: 'addCondition'
            }
        }]
    }],

    // fields: ['conditionType', 'invert', 'value'],
    columns: [{
        text: 'Condition',
        width: 200,
        dataIndex: 'conditionType',
        renderer: 'conditionRenderer'
    },
    {
        xtype: 'widgetcolumn',
        widget: {
            xtype: 'combo',
            editable: false,
            bind: '{record.invert}',
            store: [[true, 'is not'], [false, 'is']]
        }
    }, {
        xtype: 'widgetcolumn',
        flex: 1,
        widget: {
            xtype: 'container',
            layout: {
                type: 'hbox'
            },
            // items: [{
            //     xtype: 'textfield',
            //     // hidden: true,
            //     bind: {
            //         value: '{record.value}',
            //         // hidden: '{record.conditionType === "DST_LOCAL"}'
            //     }
            // }, {
            //     xtype: 'checkboxgroup',
            //     bind: {
            //         value: '{record.value}',
            //         // disabled: '{record.editor !== "checkboxgroup"}',
            //     },
            //     items: [
            //         { boxLabel: 'TCP', name: 'cb', inputValue: 'TCP' },
            //         { boxLabel: 'UDP', name: 'cb', inputValue: 'UDP' },
            //         { boxLabel: 'ICMP', name: 'cb', inputValue: 'ICMP' }
            //     ],
            //     listeners: {
            //         change: 'groupCheckChange'
            //     }
            // }]
        },
        onWidgetAttach: 'onWidgetAttach'
    }
    ]
});