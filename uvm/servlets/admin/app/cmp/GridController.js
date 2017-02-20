Ext.define('Ung.cmp.GridController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.ungrid',

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
        var v = this.getView(), newRecord;
        // if there are no editor fields defined, the record is added inline and has inline editing capability
        if (!v.editorFields) {
            newRecord = Ext.create('Ung.model.Rule', Ext.clone(v.emptyRow));
            v.getStore().add(newRecord);
        } else {
            this.editorWin(null);
        }
    },

    editRecord: function (view, rowIndex, colIndex, item, e, record) {
        // if (record.get('readOnly')) {
        //     Ext.MessageBox.alert('Info', '<strong>' + record.get('description') + '</strong> connot be edited!');
        //     return;
        // }
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
            editorFields: v.editorFields, // form fields needed to be displayed in the editor taken from grid columns
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


                    // datetime: function (get) {
                    //     return new Date(get('record.expirationTime'));
                    // },
                    // checked: {
                    //     get: function (get) {
                    //         return get('record.expirationTime') === 0;
                    //     }
                    // }
                }
            }
        });
    },

    deleteRecord: function (view, rowIndex, colIndex, item, e, record) {
        // if (record.get('readOnly')) {
        //     Ext.MessageBox.alert('Info', '<strong>' + record.get('description') + '</strong> connot be deleted!');
        //     return;
        // }
        record.set('markedForDelete', true);
    },

    moveUp: function (view, rowIndex, colIndex, item, e, record) {
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

    conditionsRenderer: function (value) {
        var view = this.getView(),
            conds = value.list,
            resp = [], i, valueRenderer = [];

        for (i = 0; i < conds.length; i += 1) {
            valueRenderer = [];
            if (conds[i].conditionType === 'SRC_INTF' || conds[i].conditionType === 'DST_INTF') {
                conds[i].value.toString().split(',').forEach(function (intfff) {
                    valueRenderer.push(Util.interfacesListNamesMap()[intfff]);
                });
            } else {
                valueRenderer = conds[i].value.toString().split(',');
            }
            resp.push(view.conditionsMap[conds[i].conditionType].displayName + '<strong>' + (conds[i].invert ? ' &nrArr; ' : ' &rArr; ') + '<span class="cond-val ' + (conds[i].invert ? 'invert' : '') + '">' + valueRenderer.join(', ') + '</span>' + '</strong>');
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
