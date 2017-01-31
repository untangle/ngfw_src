Ext.define('Ung.cmp.RulesController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.rules',

    addCount: 0,

    control: {
        '#': {
            beforerender: 'onBeforeRender',
            cellclick: 'onCellClick'
        },
    },

    onBeforeRender: function (view) {
        // create conditions map
        view.setConditionsMap(Ext.Array.toValueMap(view.getConditions(), 'name'));
        console.log(view.getViewModel());
    },


    addRecord: function () {
        var grid = this.getView();
        var vm = this.getViewModel();
        var r = Ext.create('Ung.model.Rule', Ext.clone(this.getView().emptyRow));
        vm.get('portforwardrules').add(r);
        r.dirty = true;
        grid.view.refresh();
        console.log(vm.get('portforwardrules').getRange());
        // grid.getStore().insert(0, newRule);
        // vm.get('portforwardrules').sync();
        // grid.getStore().reload();
    },

    editRecord: function (view, rowIndex, colIndex, item, e, record, row) {
        Ext.widget('ung.cmp.recordeditor', {
            viewModel: {
                data: {
                    record: record
                }
            }
        });
    },


    deleteRecord: function (view, rowIndex, colIndex, item, e, record, row) {
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
            resp = '', i, cond;
        for (i = 0; i < conds.length; i += 1) {
            resp += view.getConditionsMap()[conds[i].conditionType].displayName + '<strong>' + (conds[i].invert ? ' &ne; ' : ' = ') + '<span class="cond-val ' + (conds[i].invert ? 'invert' : '') + '">' + conds[i].value + '</span>' + '</strong> &nbsp;&bull;&nbsp; ';
        }
        return resp;
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