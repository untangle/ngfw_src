Ext.define('Ung.view.grid.ConditionsController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.ung.gridconditions',

    init: function (view) {
    },
    onBeforeRender: function (view) {
        var vm = view.getViewModel();
        var conditions = vm.get('record.conditions.list');
        //console.log(vm.get('record'));
        /*
        conditions.forEach(function (cond) {
            console.log(cond);
            view.add({
                text: cond.conditionType,
                columnWidth: 1
            });
        });
        */
    },
    onChange: function () {
        console.log('on change');
    },
    typeRenderer: function (value) {
        return Ext.getStore('conditions').findRecord('name', value).get('displayName');
    },
    valueRenderer: function (value, metaData, record) {
        var editorType = Ext.getStore('conditions').findRecord('name', record.get('conditionType')).get('editorType');
        switch (editorType) {
        case ('countryselector'):
            var str = '';
            value.split(',').forEach(function (iso) {
                str += Ext.getStore('countries').findRecord('code', iso).get('name') + ', ';
            });
            return str;
        }
        return value;
    },
    onConditionSelect: function (row, record, index) {
        var me = this;
        var condEditorWin = Ext.create('Ung.view.grid.ConditionEditor', {
            bind: {
                title: 'Edit'.t()
            },
            width: 400,
            y: 250,
            //constrain: true,
            //constrainTo: this.getView().getEl(),

            viewModel: {
                data: {
                    condition: record
                }
            }

        }).show();

        condEditorWin.on('close', function () {
            console.log(this.getViewModel().get('condition'));
        });

        me.getView().getSelectionModel().deselectAll();
    },

    onConditionTypeChange: function () {
        console.log('cond chage');
    },
    onAddCondition: function () {
        var rec = Ext.create('Ung.model.Condition');
        var condEditorWin = Ext.create('Ung.view.grid.ConditionEditor', {
            bind: {
                title: 'Add Condition'.t()
            },
            width: 400,
            y: 250,
            viewModel: {
                data: {
                    condition: rec
                }
            }
        }).show();
    }

});