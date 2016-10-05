Ext.define('Ung.view.grid.ConditionEditor', {
    extend: 'Ext.window.Window',

    requires: [
        'Ung.view.grid.ConditionEditorController'
    ],

    controller: 'ung.conditioneditor',

    modal: true,
    draggable: false,
    resizable: false,
    layout: 'fit',

    //header: false,
    border: false,
    bodyBorder: false,
    bodyStyle: {
        background: '#FFF'
    },

    items: [{
        xtype: 'form',
        reference: 'form',
        padding: 0,
        border: false,
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        items: [{
            xtype: 'combo',
            store: 'conditions',
            //fieldLabel: 'Type'.t(),
            margin: 5,
            valueField: 'name',
            displayField: 'displayName',
            editable: false,
            //width: 400,
            bind: {
                value: '{condition.conditionType}'
            },
            listeners: {
                change: 'onConditionTypeChange'
            }
        }, {
            xtype: 'container',
            items: [{
                xtype: 'segmentedbutton',
                margin: '0 0 5 5',
                bind: {
                    value: '{condition.invert}'
                },
                items: [{
                    text: 'IS',
                    value: false
                }, {
                    text: 'IS NOT',
                    value: true
                }]
            }]
        }],

        buttons: [{
            text: Ung.Util.iconTitle('Cancel', 'cancel-16'),
            handler: 'onCancel'
        }, {
            text: Ung.Util.iconTitle('Done', 'check-16'),
            formBind: true
        }]
    }]
});