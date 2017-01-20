Ext.define('Ung.config.network.RuleEditorWin', {
    extend: 'Ext.window.Window',
    width: 700,
    height: 300,
    xtype: 'ung.config.network.ruleeditorwin',
    requires: [
        'Ung.config.network.RuleEditorController'
        // 'Ung.overrides.form.CheckboxGroup'
    ],
    controller: 'ruleeditor',

    // config: {
    //     conditions: [],
    //     conditionsMap: {}
    // },

    bodyStyle: {
        background: '#FFF'
    },

    // viewmodel: true,
    autoShow: true,

    title: 'Edit',
    modal: true,
    constrain: true,
    layout: {
        type: 'vbox',
        align: 'stretch'
    },
    tbar: [{
        itemId: 'conditionsBtn',
        text: 'Add Condition',
        iconCls: 'fa fa-plus'
    }],
    bbar: ['->', {
        text: 'Cancel',
        // iconCls: 'fa fa-add'
    }, {
        text: 'Apply',
        itemId: 'applyBtn',
        iconCls: 'fa fa-check'
    }],
    // items: [{
    //     xtype: 'grid',
    //     // collapsed: true,
    //     // collapsible: true,
    //     // animCollapse: false,
    //     border: false,
    //     trackMouseOver: false,
    //     disableSelection: true,

    //     bind: {
    //         store: {
    //             type: 'ruleconditions',
    //             data: '{rule.conditions.list}'
    //         }
    //     },

    //     columns: [{
    //         dataIndex: 'groupValue'
    //     }, {
    //         dataIndex: 'conditionType',
    //         width: 200,
    //         renderer: 'conditionRenderer'
    //     }, {
    //         xtype: 'widgetcolumn',
    //         width: 50,
    //         widget: {
    //             xtype: 'combo',
    //             editable: false,
    //             bind: '{record.invert}',
    //             store: [[true, 'is not'], [false, 'is']]
    //         }
    //         // widget: {
    //         //     xtype: 'segmentedbutton',
    //         //     bind: '{record.invert}',
    //         //     // bind: {
    //         //     //     value: '{record.invert}',
    //         //     // },
    //         //     items: [{
    //         //         text: 'IS',
    //         //         value: true
    //         //     }, {
    //         //         text: 'IS NOT',
    //         //         value: false
    //         //     }]
    //         // }
    //     }, {
    //         xtype: 'widgetcolumn',
    //         flex: 1,
    //         widget: {
    //             xtype: 'container',
    //             items: [{
    //                 xtype: 'displayfield',
    //                 value: 'TRUE'
    //             }, {
    //                 xtype: 'textfield',
    //                 bind: {
    //                     value: '{record.value}',
    //                     // disabled: '{record.editor !== "textfield"}'
    //                 }
    //             }, {
    //                 xtype: 'checkboxgroup',
    //                 bind: {
    //                     value: '{record.value}',
    //                     disabled: '{record.editor !== "checkboxgroup"}',
    //                 },
    //                 items: [
    //                     { boxLabel: 'TCP', name: 'cb', inputValue: 'TCP' },
    //                     { boxLabel: 'UDP', name: 'cb', inputValue: 'UDP' },
    //                     { boxLabel: 'ICMP', name: 'cb', inputValue: 'ICMP' }
    //                 ],
    //                 listeners: {
    //                     change: 'groupCheckChange'
    //                 }
    //             }]
    //         }
    //     }]
    // }]
});