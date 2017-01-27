Ext.define('Ung.cmp.RuleEditor', {
    extend: 'Ext.window.Window',
    width: 600,
    height: 400,
    xtype: 'ung.cmp.ruleeditor',
    requires: [
        'Ung.cmp.RuleEditorController',
        'Ung.overrides.form.CheckboxGroup',
        'Ung.overrides.form.field.VTypes'
    ],
    controller: 'ruleeditor',

    bodyStyle: {
        background: '#FFF'
    },

    autoShow: true,
    shadow: false,

    bind: {
        title: '{rule.description}',
    },
    modal: true,
    constrain: true,
    // layout: {
    //     type: 'vbox',
    //     align: 'stretch'
    // },
    tbar: [{
        itemId: 'addConditionBtn',
        text: 'Add Condition'.t(),
        iconCls: 'fa fa-plus',
        // handler: 'onAdd'
    }],
    bbar: ['->', {
        text: 'Cancel',
        // iconCls: 'fa fa-add'
    }, {
        text: 'Apply',
        itemId: 'applyBtn',
        iconCls: 'fa fa-check',
        handler: 'onApply'
    }],

    layout: 'fit',
    items: [{
        xtype: 'grid',
        // border: false,
        trackMouseOver: false,
        disableSelection: true,
        bind: {
            store: {
                data: '{conditionsData}'
            }
        },


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
    }]
});