Ext.define('Ung.cmp.RecordEditorController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.recordeditor',

    control: {
        '#': {
            afterrender: 'onBeforeRender',
            // beforerender: 'onBeforeRender',
            // close: 'onClose',
        },
    },

    onBeforeRender: function (view) {
        console.log(view.getViewModel());


        var fields = view.fields, form = view.down('form');

        // add editable column fields into the form
        for (var i = 0; i < fields.length; i++) {
            if (fields[i].editor) {
                form.add(fields[i].editor);
            }
            if (fields[i].getItemId() === 'conditions') {
                form.add({
                    xtype: 'component',
                    padding: '10 0 0 0',
                    html: '<strong>' + 'If all of the following conditions are met:'.t() + '</strong>'
                });
                form.add({
                    xtype: 'ung.cmp.conditionsgrid',
                    // split: true,
                    collapsible: false,
                    resizable: false,
                    // title: 'If all of the following conditions are met:'.t(),
                    region: 'south',
                    // minHeight: 100,
                    layout: 'fit',

                    conditions: view.conditions,
                    conditionsMap: view.conditionsMap,
                    store: {
                        data: view.record.get('conditions').list
                        // data: '{record.conditions.list}'
                    }
                });
                form.add({
                    xtype: 'component',
                    padding: '0 0 10 0',
                    html: '<strong>' + view.label + '</strong>'
                });
            }
        }
        form.isValid();
    },

    setMenuConditions: function () {
        var menu = this.getView().down('#addConditionBtn').getMenu(),
            store = this.getView().down('grid').getStore();
        menu.items.each(function (item) {
            item.setDisabled(store.findRecord('conditionType', item.conditionType) ? true : false);
        });
    },

    addCondition: function (menu, item) {
        // console.log('add');
        item.setDisabled(true);
        var newCond = {
            conditionType: item.conditionType,
            invert: false,
            // javaClass: 'com.untangle.uvm.network.PortForwardRuleCondition',
            value: ''
        };
        this.getView().down('grid').getStore().add(newCond);
        // this.getView().ruleConditions.push(newCond);
        // this.addRowView(newCond);
    },

    onApply: function (btn) {
        var v = this.getView(),
            vm = this.getViewModel();


        if (v.down('grid') && v.down('grid').getStore().getModifiedRecords().length > 0) {
            v.record.set('conditions', {
                javaClass: 'java.util.LinkedList',
                list: Ext.Array.pluck(v.down('grid').getStore().getRange(), 'data')
            });
        }


        for (var field in vm.get('record').modified) {
            if (field !== 'conditions') {
                v.record.set(field, vm.get('record').get(field));
            }
        }

        if (v.action === 'add') {
            v.store.add(v.record);
        }
        v.close();
    }

});
