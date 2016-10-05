Ext.define('Ung.view.grid.ConditionEditorController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.ung.conditioneditor',

    init: function (view) {
        console.log(this.getViewModel());
        this.getViewModel().bind({
            bindTo: '{condition}',
            single: true
        }, function (cond) {
            console.log(cond);
        });
    },

    onCancel: function () {
        this.getView().close();
    },

    onConditionTypeChange: function (combo, newValue, oldValue) {
        var vm = this.getViewModel(),
            form = this.getView().lookupReference('form'),
            comboRecord = combo.getStore().findRecord('name', newValue),
            xtype = comboRecord.get('editorType'),
            vtype = comboRecord.get('vtype');

        if (form.down('#condType')) {
            //form.lookupReference('condType').destroy();
            form.remove('condType', true);
        }

        switch (xtype) {
        case ('textfield'):
            form.add({
                xtype: xtype,
                id: 'condType',
                margin: '0 5 0 5',
                //width: 400,
                //fieldLabel: 'Value'.t(),
                allowBlank: false,
                vtype: vtype,
                bind: {
                    value: '{condition.value}'
                }
            });
            break;

        case ('countryselector'):
            form.add({
                xtype: 'tagfield',
                id: 'condType',
                margin: '0 5 0 5',
                //width: 400,
                //fieldLabel: 'Value'.t(),
                store: 'countries',
                queryMode: 'local',
                typeAhead: true,
                filterPickList: true,
                displayField: 'name',
                valueField: 'code',
                clearFilterOnEnter: true,
                bind: {
                    value: '{condition.value}'
                }

            });
            break;
        }
    }

});