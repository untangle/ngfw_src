Ext.define('Ung.config.network.RuleEditor', {
    extend: 'Ext.grid.Panel',
    // width: 500,
    // height: 300,
    xtype: 'ung.config.network.ruleeditor',
    requires: [
        'Ung.config.network.RuleEditorController',
        'Ung.overrides.form.CheckboxGroup'
    ],
    // controller: 'ruleeditor',

    collapsed: true,
    collapsible: true,
    animCollapse: false,
    border: false,
    trackMouseOver: false,
    disableSelection: true,


    // forceFit: true,
    // bind: {
    //     title: 'Conditions'.t(),
    // },
    // bind: {
    //     store: {
    //         data: '{record.conditions.list}'
    //     }
    // },
    // store: {
    //     data: [
    //         {name:"DST_LOCAL",displayName: 'Destined Local'.t(), type: "boolean", visible: true},
    //         {name:"DST_ADDR",displayName: 'Destination Address'.t(), type: "text", visible: true, vtype:"ipMatcher"},
    //         {name:"DST_PORT",displayName: 'Destination Port'.t(), type: "text",vtype:"portMatcher", visible: true},
    //         {name:"SRC_ADDR",displayName: 'Source Address'.t(), type: "text", visible: true, vtype:"ipMatcher"},
    //         {name:"SRC_PORT",displayName: 'Source Port'.t(), type: "text",vtype:"portMatcher", visible: rpc.isExpertMode},
    //         {name:"SRC_INTF",displayName: 'Source Interface'.t(), type: "checkgroup", values: ['a', 'b'], visible: true},
    //         {name:"PROTOCOL",displayName: 'Protocol'.t(), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"],["ICMP","ICMP"],["GRE","GRE"],["ESP","ESP"],["AH","AH"],["SCTP","SCTP"]], visible: true}
    //     ]
    // },
    tbar: [{
        text: 'Add Condition',
        itemId: 'conditions-menu',
        menu: [
            {
                text: 'regular item 1'
            },{
                text: 'regular item 2'
            },{
                text: 'regular item 3'
            }
        ]
    }],
    columns: [{
        dataIndex: 'groupValue'
    }, {
        dataIndex: 'conditionType',
        width: 200,
        renderer: 'conditionRenderer'
    }, {
        xtype: 'widgetcolumn',
        width: 50,
        widget: {
            xtype: 'combo',
            editable: false,
            bind: '{record.invert}',
            store: [[true, 'is not'], [false, 'is']]
        }
        // widget: {
        //     xtype: 'segmentedbutton',
        //     bind: '{record.invert}',
        //     // bind: {
        //     //     value: '{record.invert}',
        //     // },
        //     items: [{
        //         text: 'IS',
        //         value: true
        //     }, {
        //         text: 'IS NOT',
        //         value: false
        //     }]
        // }
    }, {
        xtype: 'widgetcolumn',
        flex: 1,
        widget: {
            xtype: 'container',
            items: [{
                xtype: 'textfield',
                bind: {
                    value: '{record.value}',
                    // disabled: '{record.editor !== "textfield"}'
                }
            }, {
                xtype: 'checkboxgroup',
                bind: {
                    value: '{record.value}',
                    // disabled: '{record.editor !== "checkboxgroup"}',
                },
                items: [
                    { boxLabel: 'TCP', name: 'cb', inputValue: 'TCP' },
                    { boxLabel: 'UDP', name: 'cb', inputValue: 'UDP' },
                    { boxLabel: 'ICMP', name: 'cb', inputValue: 'ICMP' }
                ]
            }]
        }
    }]
});