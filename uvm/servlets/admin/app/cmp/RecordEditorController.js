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
        // this.cleanRecord = view.getViewModel().get('originalRecord');
        // view.getViewModel().bind({
        //     bindTo: '{record}',
        //     // single: true
        // }, function (rec) {
        //     console.log(rec);
        // });
        view.getViewModel().set('record', view.record.copy());


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
                        data: view.record.get('conditions.list')
                    }
                    // conditionsList: view.record.get('conditions').list
                    // viewModel: {
                    //     formulas: {
                    //         conditionsData: {
                    //             bindTo: '{record.conditions.list}',
                    //             // deep: true,
                    //             get: function (coll) {
                    //                 console.log(coll);
                    //                 return coll || [];
                    //             }
                    //         },
                    //     }
                    //     // store: {
                    //     //     conds: {
                    //     //         data: '{collectionData}'
                    //     //     }
                    //     // }
                    // }
                    // height: 200
                });
                form.add({
                    xtype: 'component',
                    padding: '0 0 10 0',
                    html: '<strong>' + view.label + '</strong>'
                });
            }
        }

        // // console.log(view.getViewModel());
        // console.log(view.fields);

        // view.getViewModel().bind({
        //     bindTo: '{conditionsData}',
        //     deep: true
        // }, this.setMenuConditions);

        // var menuConditions = [], i;

        // for (i = 0; i < view.conditions.length; i += 1) {
        //     menuConditions.push({
        //         text: view.conditions[i].displayName,
        //         conditionType: view.conditions[i].name,
        //     });
        // }

        // view.down('#addConditionBtn').setMenu({
        //     showSeparator: false,
        //     plain: true,
        //     items: menuConditions,
        //     mouseLeaveDelay: 0,
        //     listeners: {
        //         // click: 'addCondition'
        //     }
        // });

    },

    setMenuConditions: function () {
        var menu = this.getView().down('#addConditionBtn').getMenu(),
            store = this.getView().down('grid').getStore();
        menu.items.each(function (item) {
            item.setDisabled(store.findRecord('conditionType', item.conditionType) ? true : false);
        });
    },

    onWidgetAttach: function (column, container, record) {
        // if widget aklready attached do nothing
        // if (container.items.length >= 1) {
        //     return;
        // }

        container.removeAll(true);

        var condition = this.getView().conditionsMap[record.get('conditionType')], i, ckItems = [];

        switch (condition.type) {
        case 'boolean':
            container.add({
                xtype: 'component',
                padding: 3,
                html: 'True'.t()
            });
            break;
        case 'textfield':
            console.log(condition.vtype);
            container.add({
                xtype: 'textfield',
                bind: {
                    value: '{record.value}'
                },
                vtype: condition.vtype
            });
            break;
        case 'checkboxgroup':
            // console.log(condition.values);
            // var values_arr = (cond.value !== null && cond.value.length > 0) ? cond.value.split(',') : [], i, ckItems = [];
            for (i = 0; i < condition.values.length; i += 1) {
                ckItems.push({
                    inputValue: condition.values[i][0],
                    boxLabel: condition.values[i][1]
                });
            }
            container.add({
                xtype: 'checkboxgroup',
                bind: {
                    value: '{record.value}'
                },
                columns: 3,
                vertical: true,
                defaults: {
                    padding: '0 10 0 0'
                },
                items: ckItems
            });
        }


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

    onDataChanged: function () {
        console.log('datachanged');
    },

    conditionRenderer: function (val) {
        return this.getView().conditionsMap[val].displayName;
        // return [val].displayName;
    },

    removeCondition: function (view, rowIndex, colIndex, item, e, record) {
        this.getView().down('grid').getStore().remove(record);
        this.setMenuConditions();
    },


    onApply: function (btn) {
        var v = this.getView(),
            vm = this.getViewModel();
        // this.getViewModel().get('record').set('description', 'uuuu');
        // this.getView().record = this.getViewModel().get('record');

        // this.getView().record.set('description', 'aaa');


        vm.get('record').set('conditions', {
            javaClass: 'java.util.LinkedList',
            list: Ext.Array.pluck(v.down('grid').getStore().getRange(), 'data')
        });

        // console.log(vm.get('record'));

        for (var field in vm.get('record').modified) {
            console.log(field);
            v.record.set(field, vm.get('record').get(field));
        }

        // console.log(this.getView().down('grid'));
        // this.getViewModel().get('record').commit();

        // this.getView().record.beginEdit();
        // console.log(v.record.copyFrom(vm.get('record')));
        // this.getView().record.endEdit();
        // console.log(vm.get('record'));
        // v.record.modified = vm.get('record').modified;

        // console.log(vm.get('record'));

        // v.record = vm.get('record');


        if (v.store) {
            v.store.add(v.record);
            v.store.sync();
        }


        // Ext.apply(v.record, vm.get('record'));

        // v.record.save();
        // v.getStore().refresh();
        // v.record.load();

        v.close();

        // this.getView().record.set('newPort', 100);
        // console.log(this.getViewModel().get('record'));
        // console.log(this.getView().record);

        // console.log(this.getView().record);
    },

});
