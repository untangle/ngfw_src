Ext.define('Ung.cmp.RecordEditor', {
    extend: 'Ext.window.Window',
    width: 800,
    height: 500,

    xtype: 'ung.cmp.recordeditor',
    requires: [
        'Ung.cmp.RecordEditorController',
        'Ung.cmp.ConditionsGrid',
        'Ung.overrides.form.CheckboxGroup',
        'Ung.overrides.form.field.VTypes'
    ],
    controller: 'recordeditor',

    // config: {
    //     conditions: null
    // },

    viewModel: true,

    actions: {
        apply: {
            text: 'Apply',
            formBind: true,
            iconCls: 'fa fa-check',
            handler: 'onApply'
        },
        cancel: {
            text: 'Cancel',
            iconCls: 'fa fa-check',
            // handler: 'onCancel'
        }
    },


    bodyStyle: {
        // background: '#FFF'
    },

    autoShow: true,
    // shadow: false,

    // layout: 'border',

    bind: {
        title: '{record.description}',
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


    // scrollable: true,

    layout: 'fit',

    items: [{
        xtype: 'form',
        // region: 'center',
        scrollable: 'y',
        bodyPadding: 10,
        border: false,
        layout: 'anchor',
        defaults: {
            anchor: '100%',
            labelWidth: 180,
            labelAlign : 'right',
        },
        bbar: ['->', '@cancel', '@apply'],
        items: []
    }],

    // initComponent: function () {
    //     var items = this.items;
    //     var form = items[0];

    //     for (var i = 0; i < this.fields.length; i++) {
    //         console.log();
    //         if (this.fields[i].editor) {
    //             if (this.fields[i].getItemId() !== 'conditions') {
    //                 form.items.push(this.fields[i].editor);
    //             } else {
    //                 this.items.push({
    //                     xtype: 'component',
    //                     html: 'some panel'
    //                 });
    //             }
    //         }
    //     }

    //     this.callParent(arguments);

    // }
});