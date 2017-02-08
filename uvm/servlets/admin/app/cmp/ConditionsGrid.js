Ext.define('Ung.cmp.ConditionsGrid', {
    extend: 'Ext.grid.Panel',
    xtype: 'ung.cmp.conditionsgrid',
    requires: [
        'Ung.cmp.ConditionsGridController',
        // 'Ung.overrides.form.CheckboxGroup',
        // 'Ung.overrides.form.field.VTypes'
    ],
    controller: 'conditionsgrid',

    viewModel: true,

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        items: [{
            itemId: 'addConditionBtn',
            text: 'Add Condition'.t()
            // iconCls: 'fa fa-plus',
            // handler: 'onAdd'
        }]
    }],

    // tbar: [{
    //     xtype: 'component',
    //     html: 'If all of the following conditions are met:'.t(),
    // }, '->', {
    //     itemId: 'addConditionBtn',
    //     text: 'Add Condition'.t(),
    //     iconCls: 'fa fa-plus',
    //     // handler: 'onAdd'
    // }],
    // bbar: ['->', {
    //     text: 'Cancel',
    //     // iconCls: 'fa fa-add'
    // }, {
    //     text: 'Apply',
    //     itemId: 'applyBtn',
    //     iconCls: 'fa fa-check',
    //     handler: 'onApply'
    // }],

    trackMouseOver: false,
    disableSelection: true,

    padding: '10 0',

    // bind: {
    //     store: {
    //         data: '{record.conditions.list}'
    //     }
    // },

    // bind: '{conds}',


    columns: [{
        header: 'Type'.t(),
        menuDisabled: true,
        // sortable: false,
        dataIndex: 'conditionType',
        width: 200,
        renderer: 'conditionRenderer'
    }, {
        xtype: 'widgetcolumn',
        menuDisabled: true,
        sortable: false,
        width: 70,
        widget: {
            xtype: 'combo',
            editable: false,
            bind: '{record.invert}',
            store: [[true, 'is NOT'.t()], [false, 'is'.t()]]
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
        header: 'Value'.t(),
        xtype: 'widgetcolumn',
        menuDisabled: true,
        sortable: false,
        flex: 1,
        widget: {
            xtype: 'container',
            padding: '0 3'
            // layout: {
            //     type: 'hbox'
            // }
        },
        onWidgetAttach: 'onWidgetAttach'
    }, {
        xtype: 'actioncolumn',
        menuDisabled: true,
        sortable: false,
        width: 30,
        align: 'center',
        iconCls: 'fa fa-trash',
        handler: 'removeCondition'
    }]
});