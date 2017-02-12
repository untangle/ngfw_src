Ext.define('Ung.cmp.RulesController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.rules',

    addCount: 0,

    control: {
        '#': {
            afterrender: 'onBeforeRender',
            drop: 'onDropRecord'
        }
    },

    onBeforeRender: function (view) {
        // create conditionsMap for later use
        if (view.conditions) {
            view.conditionsMap = Ext.Array.toValueMap(view.conditions, 'name');
        }
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
        // var v = this.getView();
        // console.log(this.getView().down('grid').getColumns());
        this.editorWin(record);
    },

    editorWin: function (record) {
        var v = this.getView(), newRecord;
        // open recordeditor window

        if (!record) {
            newRecord = Ext.create('Ung.model.Rule', Ext.clone(v.emptyRow));
            newRecord.set('markedForNew', true);
        }

        Ext.widget('ung.cmp.recordeditor', {
            action: record ? 'edit' : 'add',
            fields: v.getColumns(), // form fields needed to be displayed in the editor taken from grid columns
            actionDescription: v.actionDescription || 'Perform the following action(s):'.t(), // the label in the form
            conditions: v.conditions, // the available conditions which can be applied
            conditionsMap: v.conditionsMap, // a map with the above conditions as helper
            // ruleJavaClass: v.ruleJavaClass,
            // recordCopy: record.copy(null), // a clean copy of the record to be edited
            record: record || newRecord,

            store: v.getStore(),

            viewModel: {
                data: {
                    record: record ? record.copy(null) : newRecord,
                    ruleJavaClass: v.ruleJavaClass,
                    addAction: record ? false : true
                },
                formulas: {
                    actionTitle: function (get) {
                        return get('addAction') ? 'Add'.t() : 'Update'.t();
                    },
                    windowTitle: function (get) {
                        return get('addAction') ? 'Add'.t() : 'Edit'.t();
                    }
                }
            }
        });
    },

    deleteRecord: function (view, rowIndex, colIndex, item, e, record, row) {
        record.set('markedForDelete', true);
    },

    moveUp: function (view, rowIndex, colIndex, item, e, record, row) {
        var store = this.getView().down('grid').getStore();
        store.remove(record, true); // just moving
        store.insert(rowIndex + item.direction, record);
    },


    // onCellClick: function (table, td, cellIndex, record) {
    //     var me = this;
    //     if (td.dataset.columnid === 'conditions') {
    //         Ext.widget('ung.cmp.ruleeditor', {
    //             conditions: me.getView().conditions,
    //             conditionsMap: me.getView().conditionsMap,
    //             viewModel: {
    //                 data: {
    //                     rule: record
    //                 },
    //                 formulas: {
    //                     conditionsData: {
    //                         bind: '{rule.conditions.list}',
    //                         get: function (coll) {
    //                             return coll || [];
    //                         }
    //                     },
    //                 },
    //             }
    //         });
    //     }
    // },

    conditionsRenderer: function (value, metaData, record) {
        var view = this.getView(),
            conds = value.list,
            resp = [], i, cond;

        for (i = 0; i < conds.length; i += 1) {
            resp.push(view.conditionsMap[conds[i].conditionType].displayName + '<strong>' + (conds[i].invert ? ' &nrArr; ' : ' &rArr; ') + '<span class="cond-val ' + (conds[i].invert ? 'invert' : '') + '">' + conds[i].value.toString().split(',').join(', ') + '</span>' + '</strong>');
        }
        return resp.join(' &nbsp;&bull;&nbsp; ');
    },

    conditionRenderer: function (val) {
        return this.getView().conditionsMap[val].displayName;
    },

    onDropRecord: function () {
        this.getView().getStore().isReordered = true;
    },

    // import/export features
    importData: function () {
        Ext.widget('dataimporter', {

        });
    },

    exportData: function () {
        // to be implemented


        // this.getView().down('#exportForm').submit();
        // console.log('export');
        // Ext.Ajax.request({
        //     method: 'POST',
        //     url: 'http://localhost:8002/webui/gridSettings', // test url
        //     params: {
        //         type: 'export',
        //         gridName: 'a',
        //         gridData: 'b'
        //     },
        //     success: function (resp) {
        //         console.log('success');
        //     },
        //     failure: function (resp) {
        //         console.log('fail');
        //     }
        // });
    }

});