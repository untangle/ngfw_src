Ext.define('Ung.cmp.GridController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.ungrid',

    addCount: 0,

    control: {
        '#': {
            afterrender: 'onAfterRender',
            drop: 'onDropRecord'
        }
    },

    onAfterRender: function (view) {
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
            newRecord = Ext.create('Ung.model.Rule', Ung.util.Util.activeClone(v.emptyRow)), rowIndex;

        newRecord.set('markedForNew', true);
        if (v.topInsert) {
            v.getStore().insert(0, newRecord);
        } else {
            v.getStore().add(newRecord);
        }

        // start edit the record right after it was added
        var cellediting = Ext.Array.findBy(v.getPlugins(), function (plugin) {
            return plugin.ptype === 'cellediting';
        });

        if (cellediting) {
            rowIndex = v.getStore().indexOf(newRecord);
            cellediting.startEditByPosition({ row: rowIndex, column: 0 });
        }
    },

    /** used for Port Forward Rules */
    addSimpleRecord: function () {
        this.simpleEditorWin(null);
    },

    /**
     * Copy a record and mark it as new.
     *
     * If specified in the view, copyId specifies a unique identifier to update.
     * This identifier defaults to the value of -1 unless copyIdPreserve is specified
     * where the current value is preserved (e.g.,-1 is bad valuefor snort rules).
     *
     * If specified, the copyAppendField is a text field that will have thhe " (copy)"
     * identifier appended.
     */
    copyRecord: function (view, rowIndex, colIndex, item, e, record) {
        var me = this,
            v = me.getView(),
            grid = v.down('grid'),
            newRecord = record.copy(null);

        var newRecordData_Id = newRecord.data._id;
        newRecord.data = JSON.parse(JSON.stringify(record.data));
        newRecord.data._id = newRecordData_Id;
        newRecord.set('markedForNew', true);

        if( v.copyId ){
            var value = -1;
            if( v.copyIdPreserve ){
                value = newRecord.get( v.copyId );
            }
            newRecord.set( v.copyId, value );
        }

        if( v.copyAppendField ){
            newRecord.set( v.copyAppendField, newRecord.get(v.copyAppendField) + ' ' + '(copy)'.t() );
        }

        if( newRecord.get('readOnly') == true){
            delete newRecord.data['readOnly'];
        }

        if (v.topInsert) {
            v.getStore().insert(0, newRecord);
        } else {
            v.getStore().add(newRecord);
        }
    },

    editRecord: function (view, rowIndex, colIndex, item, e, record) {
        if (!record.get('simple')) {
            this.editorWin(record);
        } else {
            this.simpleEditorWin(record);
        }
    },

    editorWin: function (record) {
        this.dialog = this.getView().add({
            xtype: this.getView().editorXtype,
            record: record
        });

        // look for window overrides in the parent grid
        if (this.dialog.ownerCt.editorWidth) this.dialog.width = this.dialog.ownerCt.editorWidth;
        if (this.dialog.ownerCt.editorHeight) this.dialog.height = this.dialog.ownerCt.editorHeight;

        this.dialog.show();
    },

    simpleEditorWin: function (record) {
        this.simpledialog = this.getView().add({
            xtype: this.getView().simpleEditorAlias,
            record: record
        });

        // look for window overrides in the parent grid
        if (this.simpledialog.ownerCt.editorWidth) this.simpledialog.width = this.simpledialog.ownerCt.editorWidth;
        if (this.simpledialog.ownerCt.editorHeight) this.simpledialog.height = this.simpledialog.ownerCt.editorHeight;

        this.simpledialog.show();
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
        if (!value) { return ''; } // if conditions are null return empty string

        var view = this.getView(),
            conds = value.list,
            resp = [], i, valueRenderer = [];

        for (i = 0; i < conds.length; i += 1) {
            valueRenderer = [];

            switch (conds[i].conditionType) {
            case 'SRC_INTF':
            case 'DST_INTF':
                conds[i].value.toString().split(',').forEach(function (intfff) {
                    valueRenderer.push(Util.interfacesListNamesMap()[intfff]);
                });
                break;
            case 'DST_LOCAL':
            case 'WEB_FILTER_FLAGGED':
                valueRenderer.push('true'.t());
                break;
            case 'DAY_OF_WEEK':
                conds[i].value.toString().split(',').forEach(function (day) {
                    valueRenderer.push(Util.weekdaysMap[day]);
                });
                break;
            default:
                // to avoid exceptions, in some situations condition value is null
                if (conds[i].value !== null) {
                    valueRenderer = conds[i].value.toString().split(',');
                } else {
                    valueRenderer = [];
                }
            }
            // for boolean conditions just add 'True' string as value
            if (view.conditionsMap[conds[i].conditionType].type === 'boolean') {
                valueRenderer = ['True'.t()];
            }

            resp.push(view.conditionsMap[conds[i].conditionType].displayName + '<strong>' + (conds[i].invert ? ' &nrArr; ' : ' &rArr; ') + '<span class="cond-val ' + (conds[i].invert ? 'invert' : '') + '">' + valueRenderer.join(', ') + '</span>' + '</strong>');
        }
        return resp.length > 0 ? resp.join(' &nbsp;&bull;&nbsp; ') : '<em>' + 'No conditions' + '</em>';
    },

    priorityRenderer: function(value) {
        if (Ext.isEmpty(value)) {
            return '';
        }
        switch (value) {
        case 0: return '';
        case 1: return 'Very High'.t();
        case 2: return 'High'.t();
        case 3: return 'Medium'.t();
        case 4: return 'Low'.t();
        case 5: return 'Limited'.t();
        case 6: return 'Limited More'.t();
        case 7: return 'Limited Severely'.t();
        default: return Ext.String.format('Unknown Priority: {0}'.t(), value);
        }
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

    // same as import but without prepend or append options
    replaceData: function () {
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
                    xtype: 'component',
                    margin: 10,
                    html: 'Replace current settings with settings from:'.t()
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
                                me.importHandler('replace', action.result.msg);
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

        grid.getStore().loadData(newData);
        grid.getStore().each(function(record){
            record.set('markedForNew', true);
        });
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
        var grid = this.getView();

        Ext.MessageBox.wait('Exporting Settings...'.t(), 'Please wait'.t());

        var exportForm = document.getElementById('exportGridSettings');
        exportForm.gridName.value = grid.getXType();
        exportForm.gridData.value = this.getExportData(false);
        exportForm.submit();
        Ext.MessageBox.hide();
    },

    changePassword: function (view, rowIndex, colIndex, item, e, record) {
        var me = this;
        this.pswdDialog = this.getView().add({
            xtype: 'window',
            title: 'Change Password'.t(),
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
    },

    beforeEdit: function( editor, context ){
        if( context &&
            context.record &&
            context.record.get('readOnly') === true ){
            return false;
        }
        return true;
    },

    columnRenderer: function(value, metaData, record, rowIndex, columnIndex, store, view){
        var rtype = view.grid.getColumns()[columnIndex].rtype;
        if(rtype != null){
            if( !Renderer[rtype] ){
                var gview = this.getView();
                var parentController = gview.up(gview.parentView).getController();
                if(parentController[rtype+'Renderer']){
                    return parentController[rtype+'Renderer'](value);
                }else{
                    console.log('Missing renderer for rtype=' + rtype);
                }
            }else{
                return Renderer[rtype](value, metaData);
            }
        }
        return value;
    },

    /**
     * Used for extra column actions which can be added to the grid but are very specific to that context
     * The grid requires to have defined a parentView tied to the controller on which action method is implemented
     * action - is an extra configuration set on actioncolumn and represents the name of the method to be called
     * see Users/UsersController implementation
     */
    externalAction: function (v, rowIndex, colIndex, item, e, record) {
        var view = this.getView(),
            parentController = null,
            action = item && item.action ? item.action : v.action;

        while( view != null){
            parentController = view.getController();

            if( parentController && parentController[action]){
                break;
            }
            view = view.up();
        }

        if (!parentController) {
            console.log('Unable to get the extra controller');
            return;
        }

        // call the action from the extra controller in extra controller scope, and pass all the actioncolumn arguments
        if (action) {
            parentController[action].apply(parentController, arguments);
        } else {
            console.log('External action not defined!');
        }
    }

});
