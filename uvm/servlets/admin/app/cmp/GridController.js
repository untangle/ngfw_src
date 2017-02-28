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
        var me = this;
        this.importDialog = this.getView().add({
            xtype: 'window',
            title: 'Import Settings'.t(),
            modal: true,
            layout: 'fit',
            width: 450,
            items: [{
                xtype: 'form',
                border: false,
                url: 'gridSettings',
                bodyPadding: 10,
                layout: 'anchor',
                items: [{
                    xtype: 'radiogroup',
                    name: 'importMode',
                    simpleValue: true,
                    value: 'replace',
                    columns: 1,
                    vertical: true,
                    items: [
                        { boxLabel: '<strong>' + 'Replace current settings'.t() + '</strong>', inputValue: 'replace' },
                        { boxLabel: '<strong>' + 'Prepend to current settings'.t() + '</strong>', inputValue: 'prepend' },
                        { boxLabel: '<strong>' + 'Append to current settings'.t() + '</strong>', inputValue: 'append' }
                    ]
                }, {
                    xtype: 'component',
                    margin: 10,
                    html: 'with settings from'.t()
                }, {
                    xtype: 'filefield',
                    anchor: '100%',
                    fieldLabel: 'File'.t(),
                    labelAlign: 'right',
                    allowBlank: false,
                    validateOnBlur: false
                }, {
                    xtype: 'hidden',
                    name: 'type',
                    value: 'import'
                }],
                buttons: [{
                    text: 'Cancel'.t(),
                    iconCls: 'fa fa-ban fa-red',
                    handler: function () {
                        me.importDialog.close();
                    }
                }, {
                    text: 'Import'.t(),
                    iconCls: 'fa fa-check',
                    formBind: true,
                    handler: function (btn) {
                        btn.up('form').submit({
                            waitMsg: 'Please wait while the settings are uploaded...'.t(),
                            success: function(form, action) {
                                if (!action.result) {
                                    Ext.MessageBox.alert('Warning'.t(), 'Import failed.'.t());
                                    return;
                                }
                                if (!action.result.success) {
                                    Ext.MessageBox.alert('Warning'.t(), action.result.msg);
                                    return;
                                }
                                me.importHandler(form.getValues().importMode, action.result.msg);
                                me.importDialog.close();
                            },
                            failure: function(form, action) {
                                Ext.MessageBox.alert('Warning'.t(), action.result.msg);
                            }
                        });
                    }
                }]
            }],
        });
        this.importDialog.show();
    },

    importHandler: function (importMode, newData) {
        var grid = this.getView(),
            vm = this.getViewModel(),
            existingData = Ext.Array.pluck(grid.getStore().getRange(), 'data');

        Ext.Array.forEach(existingData, function (rec, index) {
            delete rec._id;
        });

        if (importMode === 'replace') {
            grid.getStore().removeAll();
        }
        if (importMode === 'append') {
            Ext.Array.insert(existingData, existingData.length, newData);
            newData = existingData;
        }
        if (importMode === 'prepend') {
            Ext.Array.insert(existingData, 0, newData);
            newData = existingData;
        }
        vm.set(grid.listProperty, newData);
        // grid.getView().refresh();
    },

    getExportData: function (useId) {
        var data = Ext.Array.pluck(this.getView().getStore().getRange(), 'data');
        Ext.Array.forEach(data, function (rec, index) {
            delete rec._id;
            if (useId) {
                rec.id = index + 1;
            }
        });
        return Ext.encode(data);
    },

    exportData: function () {
        var grid = this.getView(),
            gridName = (grid.name !== null) ? grid.name : grid.recordJavaClass;

        Ext.MessageBox.wait('Exporting Settings...'.t(), 'Please wait'.t());

        gridName = gridName.trim().replace(/ /g, '_');

        var exportForm = document.getElementById('exportGridSettings');
        exportForm.gridName.value = gridName;
        exportForm.gridData.value = this.getExportData(false);
        exportForm.submit();
        Ext.MessageBox.hide();
    },

    changePassword: function (view, rowIndex, colIndex, item, e, record) {
        var me = this;
        this.pswdDialog = this.getView().add({
            xtype: 'window',
            title: 'Change Password'.t() + ' for ' + record.get('username'),
            modal: true,
            resizable: false,
            layout: 'fit',
            width: 350,
            items: [{
                xtype: 'form',
                layout: 'anchor',
                border: false,
                bodyPadding: 10,
                defaults: {
                    xtype: 'textfield',
                    labelWidth: 120,
                    labelAlign: 'right',
                    anchor: '100%',
                    inputType: 'password',
                    allowBlank: false,
                    listeners: {
                        keyup: function (el) {
                            var form = el.up('form'),
                                vals = form.getForm().getValues();
                            if (vals.pass1.length < 3 || vals.pass2.length < 3 || vals.pass1 !== vals.pass2) {
                                form.down('#done').setDisabled(true);
                            } else {
                                form.down('#done').setDisabled(false);
                            }
                        }
                    }
                },
                items: [{
                    fieldLabel: 'Password'.t(),
                    name: 'pass1',
                    enableKeyEvents: true,
                    // minLength: 3,
                    // minLengthText: Ext.String.format('The password is shorter than the minimum {0} characters.'.t(), 3),
                }, {
                    fieldLabel: 'Confirm Password'.t(),
                    name: 'pass2',
                    enableKeyEvents: true
                }],
                buttons: [{
                    text: 'Cancel'.t(),
                    handler: function () {
                        me.pswdDialog.close();
                    }
                }, {
                    text: 'Done'.t(),
                    itemId: 'done',
                    disabled: true,
                    // formBind: true
                    handler: function (btn)  {
                        record.set('password', btn.up('form').getForm().getValues().pass1);
                        me.pswdDialog.close();
                    }
                }]
            }]
        });
        this.pswdDialog.show();
    }

});
