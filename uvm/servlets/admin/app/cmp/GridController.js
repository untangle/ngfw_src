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
        this.editorWin(null);
    },

    addRecordInline: function () {
        var v = this.getView(),
            newRecord = Ext.create('Ung.model.Rule', Ext.clone(v.emptyRow));
        newRecord.set('markedForNew', true);
        v.getStore().add(newRecord);
    },

    editRecord: function (view, rowIndex, colIndex, item, e, record) {
        this.editorWin(record);
    },

    editorWin: function (record) {
        this.dialog = this.getView().add({
            xtype: 'ung.cmp.recordeditor',
            record: record
        });
        this.dialog.show();
    },

    deleteRecord: function (view, rowIndex, colIndex, item, e, record) {
        if (record.get('markedForNew')) {
            record.drop();
        } else {
            record.set('markedForDelete', true);
        }
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
