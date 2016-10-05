Ext.define('Ung.view.grid.Editor', {
    extend: 'Ext.window.Window',

    requires: [
        'Ung.view.grid.EditorController',
        'Ung.model.Rule',
        'Ung.model.Condition'
    ],

    controller: 'ung.grideditor',
    //viewModel: true,

    config: {
        closeAction: undefined
    },

    modal: true,
    draggable: false,
    resizable: false,
    //bodyPadding: 10,
    bodyStyle: {
        background: '#FFF'
    },
    layout: 'fit',
    items: [{
        xtype: 'form',
        padding: 10,
        layout: {
            type: 'anchor'
            //align: 'stretch'
        },
        border: 0,
        reference: 'form',
        buttons: [{
            text: Ung.Util.iconTitle('Cancel', 'cancel-16'),
            handler: 'onCancel'
        }, {
            text: Ung.Util.iconTitle('Done', 'check-16'),
            formBind: true,
            disabled: true,
            handler: 'onSave'
        }],
        listeners: {
            //afterrender: 'onAfterRender'
        }
    }],
    listeners: {
        beforeRender: 'onBeforeRender'
    }
    /*
    dockedItems: [{
        xtype: 'toolbar',
        dock: 'bottom',
        items: ['->', {
            bind: {
                text: 'Cancel {record.dirty}'
            }
        }, {
            text: Ung.Util.iconTitle('Save', 'save-16')
        }]
    }]
    */
});
