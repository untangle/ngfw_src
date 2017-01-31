Ext.define('Ung.cmp.RecordEditor', {
    extend: 'Ext.window.Window',
    width: 600,
    height: 400,
    xtype: 'ung.cmp.recordeditor',
    requires: [
        'Ung.cmp.RecordEditorController',
        // 'Ung.overrides.form.CheckboxGroup',
        // 'Ung.overrides.form.field.VTypes'
    ],
    controller: 'recordeditor',

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
    // tbar: [{
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

    items: [{
        html: 'test'
    }]
});