Ext.define('Ung.cmp.RulesController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.rules',

    addCount: 0,

    control: {
        '#': {
            afterrender: 'onBeforeRender',
        },
        // 'grid': {
        //     cellclick: 'onCellClick'
        // }
    },

    listen: {
        global: {
            applysettings: 'onApplySettings'
        }
    },


    onApplySettings: function () {
        // console.log(this.getViewModel().set('settings.port'));

        // console.log(this.getViewModel().get('settings'));
        // this.getViewModel().get('settings').portForwardRules.list = Ext.Array.pluck(this.getView().getStore().getRange(), 'data');

        var store = this.getView().getStore();

        // store.each(function (record, id) {
        //     if (record.get('markedForDelete')) {
        //         store.remove(record);
        //         // record.drop();
        //         // record.commit();
        //     }
        // });

        this.getViewModel().get('settings')[this.getView().dataProperty].list = Ext.Array.pluck(this.getView().getStore().getRange(), 'data');

        // Ext.Array.pluck(this.getView().getStore().getRange(), 'data');
        // setTimeout(function () {
            store.reload();
        // }, 500);
    },

    onBeforeRender: function (view) {
        // view.setStore()
        // view.down('grid').setBind('{portforwardrules}');
        // create conditions map
        var vm = this.getViewModel();

        console.log(vm.get('portForwardRulesData'));
        // console.log(view.getViewModel().get('ruleJavaClass'));

        // Ext.getStore('rule').loadRawData(vm.get('portForwardRulesData'));
        view.setConditionsMap(Ext.Array.toValueMap(view.getConditions(), 'name'));
        // console.log(view.getViewModel());
    },


    addRecord: function () {
        var v = this.getView();
        var vm = this.getViewModel();

        console.log(v.getStore());

        // var r = grid.emptyRow;
        // r = v.getStore().add(Ext.clone(v.emptyRow));
        // console.log(r[0]);
        this.editorWin(null);
        // vm.get('portforwardrules').add(r);
        // r.dirty = true;
        // grid.view.refresh();
        // console.log(vm.get('portforwardrules').getRange());
        // grid.getStore().insert(0, Ext.create('Ung.model.Rule'));
        // vm.get('portforwardrules').commitChanges();
        // grid.getStore().reload();


        // console.log(v.getStore().getModel());

        // Ext.widget('ung.cmp.recordeditor', {
        //     fields: v.getColumns(), // form fields needed to be displayed in the editor taken from grid columns
        //     label: v.label, // the label in the form
        //     conditions: v.getConditions(), // the available conditions which can be applied
        //     conditionsMap: v.getConditionsMap(), // a map with the above conditions as helper
        //     ruleJavaClass: v.ruleJavaClass,
        // });




    },

    editRecord: function (view, rowIndex, colIndex, item, e, record, row) {
        var v = this.getView();
        // console.log(this.getView().down('grid').getColumns());
        this.editorWin(record);
    },

    editorWin: function (record) {
        var v = this.getView();
        // open recordeditor window
        Ext.widget('ung.cmp.recordeditor', {
            action: record ? 'edit' : 'add',
            fields: v.getColumns(), // form fields needed to be displayed in the editor taken from grid columns
            label: v.label, // the label in the form
            conditions: v.getConditions(), // the available conditions which can be applied
            conditionsMap: v.getConditionsMap(), // a map with the above conditions as helper
            // ruleJavaClass: v.ruleJavaClass,
            // recordCopy: record.copy(null), // a clean copy of the record to be edited
            record: record || Ext.create('Ung.model.Rule', v.emptyRow),

            store: v.getStore(),

            viewModel: {
                data: {
                    record: record ? record.copy(null) : Ext.create('Ung.model.Rule', v.emptyRow),
                    ruleJavaClass: v.ruleJavaClass,
                    addAction: record ? false : true
                },
                formulas: {
                    actionTitle: function (get) {
                        return get('addAction') ? 'Add'.t() : 'Update'.t();
                    },
                    windowTitle: function (get) {
                        return get('addAction') ? 'Add new Rule'.t() : 'Edit Rule'.t();
                    }
                }
            }
        });
    },

    // addRecord: function (btn) {
    //     var v = this.getView();
    //     Ext.widget('ung.cmp.recordeditor', {
    //         fields: v.getColumns(), // form fields needed to be displayed in the editor taken from grid columns
    //         label: v.label, // the label in the form
    //         conditions: v.getConditions(), // the available conditions which can be applied
    //         conditionsMap: v.getConditionsMap(), // a map with the above conditions as helper
    //         // recordCopy: record.copy(null), // a clean copy of the record to be edited
    //         record: Ext.create('Ung.model.Rule', Ext.clone(v.emptyRow)),
    //         store: v.getStore()
    //         // bind: {
    //         //     data: {
    //         //         record: '{record}'
    //         //     }
    //         // }
    //         // conditionsMap: this.getView().getConditionsMap(),
    //         // viewModel: {
    //         //     data: {
    //         //         originalRecord: record
    //         //     },
    //         //     // formulas: {
    //         //     //     conditionsData: {
    //         //     //         bind: '{record.conditions.list}',
    //         //     //         get: function (coll) {
    //         //     //             return coll || [];
    //         //     //         }
    //         //     //     },
    //         //     // }
    //         // }
    //         // listeners: {
    //         //     close: function (a) {
    //         //         console.log(a);
    //         //     }
    //         // }
    //     });
    // },


    deleteRecord: function (view, rowIndex, colIndex, item, e, record, row) {
        console.log(record);
        // record.drop();
        record.set('markedForDelete', true);
        // console.log(record);
        // record.drop();
        // console.log(record);
        // this.getView().getStore().remove(record);
        //console.log(record);
        //record.setConfig('markDelete', true);
        //record.markDelete = true;
        //console.log(record);
    },

    moveUp: function (view, rowIndex, colIndex, item, e, record, row) {
        var store = this.getView().down('grid').getStore();
        store.remove(record, true); // just moving
        store.insert(rowIndex + item.direction, record);
    },


    onCellClick: function (table, td, cellIndex, record) {
        var me = this;
        if (td.dataset.columnid === 'conditions') {
            Ext.widget('ung.cmp.ruleeditor', {
                conditions: me.getView().getConditions(),
                conditionsMap: me.getView().getConditionsMap(),
                viewModel: {
                    data: {
                        rule: record
                    },
                    formulas: {
                        conditionsData: {
                            bind: '{rule.conditions.list}',
                            get: function (coll) {
                                return coll || [];
                            }
                        },
                    },
                }
            });
        }
    },

    conditionsRenderer: function (value, metaData, record) {
        var view = this.getView(),
            conds = record.get('conditions').list,
            resp = [], i, cond;
        for (i = 0; i < conds.length; i += 1) {
            resp.push(view.getConditionsMap()[conds[i].conditionType].displayName + '<strong>' + (conds[i].invert ? ' &nrArr; ' : ' &rArr; ') + '<span class="cond-val ' + (conds[i].invert ? 'invert' : '') + '">' + conds[i].value.split(',').join(', ') + '</span>' + '</strong>');
        }
        return resp.join(' &nbsp;&bull;&nbsp; ');
    },

    conditionRenderer: function (val) {
        return this.getView().conditionsMap[val].displayName;
    },

    editRuleWin: function (view, rowIndex, colIndex, item, e, record) {
        var me = this;
        Ext.widget('ung.cmp.ruleeditor', {
            conditions: me.getView().getConditions(),
            conditionsMap: me.getView().getConditionsMap(),
            viewModel: {
                data: {
                    rule: record
                },
                formulas: {
                    conditionsData: {
                        bind: '{rule.conditions.list}',
                        get: function (coll) {
                            return coll || [];
                        }
                    },
                },
            }
        });
    }

});