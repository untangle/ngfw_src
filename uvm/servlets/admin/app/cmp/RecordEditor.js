                                                                                        Ext.define('Ung.cmp.RecordEditor', {
    extend: 'Ext.window.Window',
    width: 800,
    height: 500,

    xtype: 'ung.cmp.recordeditor',
    requires: [
        'Ung.cmp.RecordEditorController',
        'Ung.overrides.form.CheckboxGroup',
        'Ung.overrides.form.field.VTypes'
    ],
    controller: 'recordeditor',
    closeAction: 'destroy',
    // config: {
    //     conditions: null
    // },

    viewModel: true,
    disabled: true,
    bind: {
        title: '{windowTitle} | {record.description}',
        disabled: '{!record}'
    },

    actions: {
        apply: {
            // text: 'Save'.t(),
            bind: {
                text: '{actionTitle}'
            },
            formBind: true,
            iconCls: 'fa fa-check',
            handler: 'onApply'
        },
        cancel: {
            text: 'Cancel',
            iconCls: 'fa fa-ban',
            handler: 'onCancel'
        },
        addCondition: {
            itemId: 'addConditionBtn',
            text: 'Add Condition'.t(),
            iconCls: 'fa fa-plus'
        }
    },


    bodyStyle: {
        // background: '#FFF'
    },

    autoShow: true,
    // shadow: false,

    // layout: 'border',

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