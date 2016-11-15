// Grid edit column
Ext.define('Ung.grid.EditColumn', {
    extend:'Ext.grid.column.Action',
    menuDisabled: true,
    resizable: false,
    hideable: false,
    iconCls: 'icon-edit-row',
    hasReadOnly: false,
    constructor: function(config) {
        Ext.applyIf(config, {
            header: i18n._("Edit"),
            width: 50
        });
        if(config.hasReadOnly) {
            this.getClass = this.getClassReadOnly;
        }
        this.callParent(arguments);
    },
    init: function(grid) {
        this.grid = grid;
    },
    handler: function(view, rowIndex, colIndex, item, e, record) {
        this.grid.editHandler(record);
    },
    getClassReadOnly: function(value, metadata, record) {
        if(!record.get("readOnly")) {
            return this.iconCls;
        } else {
            return 'icon-detail-row';
        }
    }
});

// Grid edit column
Ext.define('Ung.grid.DeleteColumn', {
    extend:'Ext.grid.column.Action',
    menuDisabled: true,
    resizable: false,
    hideable: false,
    iconCls: 'icon-delete-row',
    hasReadOnly: false,
    constructor: function(config) {
        Ext.applyIf(config, {
            header: i18n._("Delete"),
            width: 55
        });
        if(config.hasReadOnly) {
            this.getClass = this.getClassReadOnly;
        }
        this.callParent(arguments);
    },
    init:function(grid) {
        this.grid=grid;
    },
    handler: function(view, rowIndex, colIndex, item, e, record) {
        this.grid.deleteHandler(record);
    },
    getClassReadOnly: function(value, metadata, record) {
        if(!record.get("readOnly")) {
            return this.iconCls;
        } else {
            return 'x-hide-display';
        }
    }
});

// Grid reorder column
Ext.define('Ung.grid.ReorderColumn', {
    extend:'Ext.grid.column.Template',
    menuDisabled:true,
    resizable: false,
    hideable: false,
    header: "Reorder",
    width: 55,
    tpl:'<img src="'+Ext.BLANK_IMAGE_URL+'" class="icon-drag"/>'
});

// Grid copy column
Ext.define('Ung.grid.CopyColumn', {
    extend:'Ext.grid.column.Action',
    menuDisabled: true,
    resizable: false,
    hideable: false,
    iconCls: 'icon-copy-row',
    hasReadOnly: false,
    constructor: function(config) {
        Ext.applyIf(config, {
            header: i18n._("Copy"),
            width: 55
        });
        this.callParent(arguments);
    },
    init:function(grid) {
        this.grid=grid;
    },
    handler: function(view, rowIndex, colIndex, item, e, record) {
        this.grid.copyHandler(record);
    }
});

// Editor Grid class
Ext.define('Ung.grid.Panel', {
    extend:'Ext.grid.Panel',
    selType: 'rowmodel',
    settingsCmp: null,
    // the list of fields used to by the Store
    fields: null,
    // has Add button
    hasAdd: true,
    // should add add rows at top or bottom
    addAtTop: true,
    // has Import Export buttons
    hasImportExport: null,
    // has Edit button on each record
    hasEdit: true,
    // has Edit button on each record
    hasCopy: false,
    copyField: null,
    // has Delete button on each record
    hasDelete: true,
    // the default Empty record for a new row
    hasReorder: false,
    hasInlineEditor:true,
    // has Refresh button
    hasRefresh: false,
    // the default Empty record for a new row
    emptyRow: null,
    // implements readOnly rows feaure
    hasReadOnly: null,
    // input lines used by the row editor
    rowEditorInputLines: null,
    // label width for row editor input lines
    rowEditorLabelWidth: null,
    //size row editor to component
    rowEditorConfig: null,
    // the columns are sortable by default, if sortable is not specified
    columnsDefaultSortable: true,
    // is the column header dropdown disabled
    columnMenuDisabled: true,
    // javaClass of the record, used in save function to create correct json-rpc object
    recordJavaClass: null,
    // used by rendering functions and by save
    enableColumnHide: false,
    enableColumnMove: false,
    dirtyFlag: false,
    addedId: 0,
    useServerIds: false,
    sortingDisabled: false,
    scale: 'small',
    constructor: function(config) {
        var defaults = {
            plugins: [],
            viewConfig: {
                enableTextSelection: true,
                listeners: {
                    "drop": {
                        fn: Ext.bind(function() {
                            this.markDirty();
                        }, this)
                    }
                },
                loadMask: {
                    msg: i18n._("Loading...")
                }
            },
            changedData: {},
            subCmps:[]
        };
        Ext.applyIf(config, defaults);
        this.callParent(arguments);
    },
    initComponent: function() {
        var grid=this, i, col;
        if(this.hasInlineEditor) {
            this.inlineEditor=Ext.create('Ext.grid.plugin.CellEditing', {
                clicksToEdit: 1
            });
            this.plugins.push(this.inlineEditor);
        }
        if (this.hasReorder) {
            var reorderColumn = Ext.create('Ung.grid.ReorderColumn', {
                header: i18n._("Reorder")
            });
            this.columns.push(reorderColumn);
            this.viewConfig.plugins= {
                ptype: 'gridviewdragdrop',
                dragText: i18n._('Drag and drop to reorganize')
            };
            this.columnsDefaultSortable = false;
        }
        for (i = 0; i < this.columns.length; i++) {
            col=this.columns[i];
            if( col.menuDisabled == null) {
                col.menuDisabled = this.columnMenuDisabled;
            }
            if( col.sortable == null) {
                col.sortable = this.columnsDefaultSortable;
            }
            if(this.hasReadOnly && (this.changableFields || []).indexOf(col.dataIndex) == -1) {
                if(col.xtype == "checkcolumn") {
                    if (!col.listeners) {
                        col.listeners = {};
                    }
                    col.listeners["beforecheckchange"] = {
                        fn: function(elem, rowIndex, checked, eOpts) {
                            var record = elem.getView().getRecord(elem.getView().getRow(rowIndex));
                            if (record.get('readOnly') == true) {
                                return false;
                            }
                        }
                    };
                }
            }
            if(col.xtype == "checkcolumn" && col.checkAll) {
                this.hasCheckAll = true;
            }
        }

        if (this.hasEdit) {
            var editColumn = Ext.create('Ung.grid.EditColumn', {hasReadOnly: this.hasReadOnly});
            this.plugins.push(editColumn);
            this.columns.push(editColumn);
        }
        if (this.hasCopy) {
            var copyColumn = Ext.create('Ung.grid.CopyColumn', {hasReadOnly: this.hasReadOnly});
            this.plugins.push(copyColumn);
            this.columns.push(copyColumn);
        }
        if (this.hasDelete) {
            var deleteColumn = Ext.create('Ung.grid.DeleteColumn', {hasReadOnly: this.hasReadOnly});
            this.plugins.push(deleteColumn);
            this.columns.push(deleteColumn);
        }
        //Use internal ids for all operations
        this.fields.push({
            name: 'internalId',
            mapping: null
        });
        this.modelName = 'Ung.Model'+this.getId();
        var model = Ext.define(this.modelName , {
            extend: 'Ext.data.Model',
            fields: this.fields
        });
        this.subCmps.push(model);
        var storeData = this.dataProperty? this.settingsCmp.settings[this.dataProperty]?this.settingsCmp.settings[this.dataProperty].list:[]:
                        this.dataExpression? eval("this.settingsCmp."+this.dataExpression):
                        this.storeData || [];
        this.store = Ext.create('Ext.data.Store',{
            data: this.formatData(Ext.clone(storeData)),
            model: this.modelName,
            proxy: {
                type: 'memory',
                reader: {
                    type: 'json'
                }
            },
            sorters: this.sortField ? {
                property: this.sortField,
                direction: this.sortOrder ? this.sortOrder: "ASC"
            }: undefined,
            groupField: this.groupField,
            listeners: {
                "update": {
                    fn: Ext.bind(function(store, record, operation) {
                        this.updateChangedData(record, "modified");
                    }, this)
                },
                "load": {
                    fn: Ext.bind(function(store, records, successful, options, eOpts) {
                        this.updateFromChangedData(store,records);
                    }, this)
                }
            }
        });
        if(!this.dockedItems)  {
            this.dockedItems = [];
        }
        if (this.tbar == null) {
            this.tbar=[];
        }
        
        if(this.hasRefresh) {
            if (this.bbar == null) {
                this.bbar=[];
            }
            this.bbar.splice(0, 0, '-', {
                xtype: 'button',
                text: i18n._('Refresh'),
                name: "Refresh",
                tooltip: i18n._('Refresh'),
                iconCls: 'icon-refresh',
                handler: Ext.bind(function() {
                    this.reload();
                }, this)
            });
        }
        if(this.hasImportExport===null) {
            this.hasImportExport=this.hasAdd;
        }
        if (this.hasAdd) {
            this.tbar.push({
                text: '<i class="material-icons">add</i> <span>' + i18n._('Add') + '</span>',
                cls: 'material-button',
                scale: this.scale,
                tooltip: i18n._('Add New Row'),
                //iconCls: 'icon-add-row',
                name: 'Add',
                handler: Ext.bind(this.addHandler, this)
            });
        }

        if (this.filterFields) {
            this.tbar.push({ xtype: 'tbseparator' });
            this.tbar.push(i18n._('Search'));
            this.tbar.push({
                xtype: 'textfield',
                enableKeyEvents: true,
                listeners: {
                    keyup: {
                        fn: function (string, e) {
                            if (e.getKey() !== e.ENTER) {
                                return;
                            }
                            if (string.getValue().length === 0) {
                                grid.store.clearFilter();
                                return;
                            }
                            var re = new RegExp(string.getValue(), 'ig');
                            this.filter = new Ext.util.Filter({
                                filterFn: function (item) {
                                    var filtered = false;
                                    grid.filterFields.forEach(function (field) {
                                        if (!filtered && item.get(field) && re.test(item.get(field))) {
                                            filtered = true;
                                        }
                                    });
                                    return filtered;
                                }
                            });
                            grid.store.filter(this.filter);
                        }
                    }
                }
            });
        }

        if (this.hasCheckAll) {
            for (i = 0; i < this.columns.length; i++) {
                col=this.columns[i];
                if(col.xtype == "checkcolumn" && col.checkAll) {
                    var colDataIndex = col.dataIndex;
                    this.tbar.push(Ext.applyIf(col.checkAll, {
                        xtype: 'checkbox',
                        hidden: !rpc.isExpertMode,
                        hideLabel: true,
                        margin: '0 5px 0 5px',
                        boxLabel: Ext.String.format(i18n._("{0} All"), col.header),
                        handler: function(checkbox, checked) {
                            var records=checkbox.up("grid").getStore().getRange();
                            for(var i=0; i<records.length; i++) {
                                records[i].set(this.colDataIndex, checked);
                            }
                        },
                        scope: {colDataIndex: colDataIndex}
                    }));
                }
            }
        }
        if (this.hasImportExport) {
            this.tbar.push('->', {
                text: '<i class="material-icons">file_download</i> <span>' + i18n._('Import') + '</span>',
                cls: 'material-button',
                tooltip: i18n._('Import From File'),
                scale: this.scale,
                //iconCls: 'icon-import',
                name: 'Import',
                handler: Ext.bind(this.importHandler, this)
            }, {
                text: '<i class="material-icons">file_upload</i> <span>' + i18n._('Export') + '</span>',
                cls: 'material-button',
                tooltip: i18n._('Export To File'),
                scale: this.scale,
                //iconCls: 'icon-export',
                name: 'export',
                handler: Ext.bind(this.exportHandler, this)
            });
        }
        if(this.hasReadOnly) {
            this.on('beforeedit', function(editor, e) {
                if (e.record.get('readOnly') == true) return false;
            });
        }
        this.callParent(arguments);
    },
    afterRender: function() {
        this.callParent(arguments);
        var grid=this;
        this.getView().getRowClass = function(record, index, rowParams, store) {
            var id = record.get("internalId");
            if (id == null || id < 0) {
                return "grid-row-added";
            } else {
                var d = grid.changedData[id];
                if (d) {
                    if (d.op == "deleted") {
                        return "grid-row-deleted";
                    } else {
                        return "grid-row-modified";
                    }
                }
            }
            return "";
        };

        if (this.rowEditor==null) {
            if(this.rowEditorInputLines != null) {
                this.rowEditor = Ext.create('Ung.RowEditorWindow', {
                    grid: this,
                    inputLines: this.rowEditorInputLines,
                    rowEditorLabelWidth: this.rowEditorLabelWidth,
                    helpSource: this.rowEditorHelpSource
                });
            } else if (this.rowEditorConfig != null) {
                this.rowEditor = Ext.create('Ung.RowEditorWindow', Ext.applyIf( this.rowEditorConfig, {grid: this}));
            }
        }
        if(this.rowEditor!=null) {
            this.subCmps.push(this.rowEditor);
        }
        if ( (undefined !== this.tooltip) && (undefined !== this.header) && ( undefined !== this.header.dom ) ) {
            Ext.QuickTips.register({
                target: this.header.dom,
                title: '',
                text: this.tooltip,
                enabled: true,
                showDelay: 20
            });
        }
        this.initialLoad();
    },
    initialLoad: function() {
        if(this.dataFn) {
            this.loadingData = true;
            this.dataFn(Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                this.getStore().getProxy().setData(this.formatData(result.list));
                this.getStore().load({
                    callback: function() {
                        this.getView().setLoading(false);
                    },
                    scope: this
                });
                this.loadingData = false;
            }, this));
        } else {
            this.getStore().load({
                callback: function() {
                    this.getView().setLoading(false);
                },
                scope: this
            });
        }
    },
    getTestRecord:function(index) {
        var rec= {};
        var property;
        for (var i=0; i<this.fields.length ; i++) {
            property = (this.fields[i].mapping != null)?this.fields[i].mapping:this.fields[i].name;
            rec[property]=
                (property=='id')?index+1:
                (property=='time_stamp')?{javaClass:"java.util.Date", time: (new Date(i*10000)).getTime()}:
            property+"_"+(i*(index+1))+"_"+Math.floor((Math.random()*10));
        }
        return rec;
    },
    formatData: function(data) {
        if(!data) {
            data=[];
        }
        if(testMode && data.length === 0) {
            if(this.testData) {
                data = data.concat(this.testData);
            } else if(data.length === 0) {
                var emptyRec={};
                var length = Math.floor((Math.random()*5));
                for(var t=0; t<length; t++) {
                    data.push(this.getTestRecord(t));
                }
            }
        }
        for(var i=0; i<data.length; i++) {
            data[i]["internalId"]=i+1;
            //prevent using ids from server
            if(!this.useServerIds) {
                delete data[i]["id"];
            }
        }
        return data;
    },
    buildData: function(handler) {
        if(this.dataFn) {
            this.dataFn(Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                this.getStore().getProxy().setData(this.formatData(result.list));
                if(handler) {
                    handler();
                }
            }, this));
        } else {
            var storeData = this.dataProperty? this.settingsCmp.settings[this.dataProperty].list:
                this.dataExpression? eval("this.settingsCmp."+this.dataExpression):
                this.storeData || [];
            
            this.getStore().getProxy().setData(this.formatData(Ext.clone(storeData)));
            if(handler) {
                handler();
            }
        }  
    },
    stopEditing: function() {
        if(this.inlineEditor) {
            this.inlineEditor.completeEdit();
        }
    },
    addHandler: function(button, e, rowData) {
        var record = Ext.create(Ext.getClassName(this.getStore().getProxy().getModel()), Ext.decode(Ext.encode(rowData || this.emptyRow)));
        record.set("internalId", this.genAddedId());
        this.stopEditing();
        if (this.rowEditor) {
            this.rowEditor.populate(record, true);
            this.rowEditor.show();
        } else {
            if (this.addAtTop)
                this.getStore().insert(0, [record]);
            else
                this.getStore().add([record]);
            this.updateChangedData(record, "added");
        }
    },
    editHandler: function(record) {
        this.stopEditing();
        // populate row editor
        this.rowEditor.populate(record);
        this.rowEditor.show();
    },
    deleteHandler: function(record) {
        this.stopEditing();
        this.updateChangedData(record, "deleted");
    },
    copyHandler: function(record) {
        var copyRecord = record.copy(null);
        copyRecord.set("internalId", this.genAddedId());
        var fields = copyRecord.getFields();
        for(var i = 0; i < fields.length; i++){
            if(fields[i].getName() == "readOnly"){
                copyRecord.set(fields[i].getName(), "false");
            }
            if(fields[i].getName() == this.copyField){
                copyRecord.set(fields[i].getName(), copyRecord.get(fields[i].getName()) + " ("+i18n._("copy") +")");
            }
        }
        this.stopEditing();
        this.getStore().add([copyRecord]);
        this.updateChangedData(copyRecord, "added");
    },
    importHandler: function() {
        if(!this.importSettingsWindow) {
            this.importSettingsWindow = Ext.create('Ung.ImportSettingsWindow',{
                grid: this
            });
            this.subCmps.push(this.importSettingsWindow);
        }
        this.stopEditing();
        this.importSettingsWindow.show();
    },
    onImport: function (importMode, importedRows) {
        this.stopEditing();
        Ext.Function.defer(this.onImportContinue, 1, this, [importMode, importedRows]);
    },
    onImportContinue: function (importMode, importedRows) {
        var invalidRecords=0;
        if(importedRows == null) {
            importedRows=[];
        }
        var records=[];
        for (var i = 0; i < importedRows.length; i++) {
            try {
                var record= Ext.create(Ext.getClassName(this.getStore().getProxy().getModel()), importedRows[i]);
                if(importedRows[i].javaClass == this.recordJavaClass) {
                    record.set("internalId", this.genAddedId());
                    records.push(record);
                } else {
                    invalidRecords++;
                }
            } catch(e) {
                invalidRecords++;
            }
        }
        var validRecords=records.length;
        if(validRecords > 0) {
            if(importMode=='replace' ) {
                this.deleteAllRecords();
                this.getStore().insert(0, records);
                this.updateChangedDataOnImport(records, "added");
            } else {
                if(importMode=='append') {
                    this.getStore().add(records);
                } else if(importMode=='prepend') { //replace or prepend mode
                    this.getStore().insert(0, records);
                }
                this.updateChangedDataOnImport(records, "added");
            }
        }
        if(validRecords > 0) {
            if(invalidRecords==0) {
                Ext.MessageBox.alert(i18n._('Import successful'), Ext.String.format(i18n._("Imported file contains {0} valid records."), validRecords));
            } else {
                Ext.MessageBox.alert(i18n._('Import successful'), Ext.String.format(i18n._("Imported file contains {0} valid records and {1} invalid records."), validRecords, invalidRecords));
            }
        } else {
            if(invalidRecords==0) {
                Ext.MessageBox.alert(i18n._('Warning'), i18n._("Import failed. Imported file has no records."));
            } else {
                Ext.MessageBox.alert(i18n._('Warning'), Ext.String.format(i18n._("Import failed. Imported file contains {0} invalid records and no valid records."), invalidRecords));
            }
        }
    },
    deleteAllRecords: function () {
        var records=this.getStore().getRange();
        this.updateChangedDataOnImport(records, "deleted");
    },
    exportHandler: function() {
        Ext.MessageBox.wait(i18n._("Exporting Settings..."), i18n._("Please wait"));
        var gridName=(this.name!=null)?this.name:this.recordJavaClass;
        gridName=gridName.trim().replace(/ /g,"_");
        var exportForm = document.getElementById('exportGridSettings');
        exportForm["gridName"].value=gridName;
        exportForm["gridData"].value="";
        exportForm["gridData"].value=Ext.encode(this.getList(true));
        exportForm.submit();
        Ext.MessageBox.hide();
    },
    genAddedId: function() {
        this.addedId--;
        return this.addedId;
    },
    beforeDestroy: function() {
        Ext.destroy(this.subCmps);
        this.callParent(arguments);
    },
    // when a page is rendered load the changedData for it
    updateFromChangedData: function(store, records) {
        for (var id in this.changedData) {
            var cd = this.changedData[id];
            if ("added" == cd.op) {
                var record = Ext.create(Ext.getClassName(store.getProxy().getModel()), cd.recData);
                store.insert(0, [record]);
            } else if ("modified" == cd.op) {
                var recIndex = store.findExact("internalId", parseInt(id, 10));
                if (recIndex >= 0) {
                    var rec = store.getAt(recIndex);
                    rec.data = cd.recData;
                    rec.commit();
                }
            }
        }
    },
    isDirty: function() {
        // Test if there are changed data
        return this.dirtyFlag || Ung.Util.hasData(this.changedData);
    },
    markDirty: function() {
        this.dirtyFlag=true;
    },
    clearDirty: function() {
        this.changedData = {};
        this.dirtyFlag=false;
        this.getView().setLoading(true);
        this.buildData(Ext.bind(function() {
            this.getStore().load({
                callback: function() {
                    this.enableSorting();
                    this.getView().setLoading(false);
                },
                scope: this
            });
        }, this));
    },
    reload: function(options) {
        if(options && options.data) {
            this.storeData = options.data;
        }
        this.clearDirty();
    },
    disableSorting: function () {
        if (!this.sortingDisabled) {
            var cmConfig = this.columns;
            for (var i in cmConfig) {
                cmConfig[i].initalSortable = cmConfig[i].sortable;
                cmConfig[i].sortable = false;
            }
            this.sortingDisabled=true;
        }
    },
    enableSorting: function () {
        if (this.sortingDisabled) {
            var cmConfig = this.columns;
            for (var i in cmConfig) {
                cmConfig[i].sortable=cmConfig[i].initalSortable;
            }
            this.sortingDisabled=false;
        }
    },
    // Update Changed data after an import
    updateChangedDataOnImport: function(records, currentOp) {
        this.disableSorting();
        var recLength=records.length;
        var i, record;
        if(currentOp == "added") {
            for (i=0; i < recLength; i++) {
                record=records[i];
                this.changedData[record.get("internalId")] = {
                    op: currentOp,
                    recData: record.data
                };
            }
        } else if (currentOp == "deleted") {
            for(i=0; i<recLength; i++) {
                this.getStore().suspendEvents();
                record=records[i];
                var id = record.get("internalId");
                var cd = this.changedData[id];
                if (cd == null) {
                    this.changedData[id] = {
                        op: currentOp,
                        recData: record.data
                    };
                } else {
                    if ("added" == cd.op) {
                        this.getStore().remove(record);
                        this.changedData[id] = null;
                        delete this.changedData[id];
                    } else {
                        this.changedData[id] = {
                            op: currentOp,
                            recData: record.data
                        };
                    }
                }
                this.getStore().resumeEvents();
            }
            if(records.length > 0) {
                this.getView().refresh(false);
            }
        }
    },
    // Update Changed data after an operation (modifyed, deleted, added)
    updateChangedData: function(record, currentOp) {
        this.disableSorting();
        var id = record.get("internalId");
        var cd = this.changedData[id];
        var index;
        if (cd == null) {
            this.changedData[id] = {
                op: currentOp,
                recData: record.data
            };
            if ("deleted" == currentOp) {
                index = this.getStore().indexOf(record);
                this.getView().refreshNode(index);
            }
        } else {
            if ("deleted" == currentOp) {
                if ("added" == cd.op) {
                    this.getStore().remove(record);
                    this.changedData[id] = null;
                    delete this.changedData[id];
                } else {
                    this.changedData[id] = {
                        op: currentOp,
                        recData: record.data
                    };
                    index = this.getStore().indexOf(record);
                    this.getView().refreshNode(index);
                }
            } else {
                if ("added" == cd.op) {
                    this.changedData[id].recData = record.data;
                } else {
                    this.changedData[id] = {
                        op: currentOp,
                        recData: record.data
                    };
                }
            }
        }
    },
    setRowEditor: function(rowEditor) {
        this.rowEditor = rowEditor;
        this.rowEditor.grid=this;
        this.subCmps.push(this.rowEditor);
    },
    getAddedDeletedModifiedLists: function() {
        var added = [];
        var deleted = [];
        var modified = [];
        for (var id in this.changedData) {
            var cd = this.changedData[id];
            if ("deleted" == cd.op) {
                if (id > 0) {
                    deleted.push(parseInt(id, 10));
                }
            } else {
                if (this.recordJavaClass != null) {
                    cd.recData["javaClass"] = this.recordJavaClass;
                }
                if (id < 0) {
                    added.push(cd.recData);
                } else {
                    modified.push(cd.recData);
                }
            }
        }
        return [{
            list: added,
            "javaClass": "java.util.ArrayList"
        }, {
            list: deleted,
            "javaClass": "java.util.ArrayList"
        }, {
            list: modified,
            "javaClass": "java.util.ArrayList"
        }];
    },
    getList: function(useId, useInternalId) {
        var list=[];
        if(this.loadingData) {
            //This code should never be called
            throw i18n._("Grid data loading is not yet completed.");
        }
        var records=this.getStore().getRange(), internalId, d;
        for(var i=0; i<records.length;i++) {
            internalId = records[i].get("internalId");
            if (internalId != null && internalId >= 0) {
                d = this.changedData[internalId];
                if (d && d.op == "deleted") {
                    continue;
                }
            }
            if (this.recordJavaClass != null) {
                records[i].data["javaClass"] = this.recordJavaClass;
            }
            var recData=Ext.decode(Ext.encode(records[i].data));
            if(!useInternalId) {
                delete recData["internalId"];
            }
            if(!useId) {
                delete recData["id"];
            } else if(!this.useServerIds) {
                recData["id"]=i+1;
            }

            list.push(recData);
        }
        return list;
    },
    getDeletedList: function() {
        var list=[];
        var records=this.getStore().getRange();
        for(var i=0; i<records.length;i++) {
            var id = records[i].get("internalId");
            if (id != null && id >= 0) {
                var d = this.changedData[id];
                if (d) {
                    if (d.op == "deleted") {
                        if (this.recordJavaClass != null) {
                            records[i].data["javaClass"] = this.recordJavaClass;
                        }
                        list.push(records[i].data);
                    }
                }
            }
        }
        return list;
    }
});

//Row editor window used by editor grid
Ext.define('Ung.RowEditorWindow', {
    extend:'Ung.EditWindow',
    // the editor grid
    grid: null,
    // input lines for standard input lines (text, checkbox, textarea, ..)
    inputLines: null,
    // label width for row editor input lines
    rowEditorLabelWidth: null,
    // the record currently edit
    record: null,
    // initial record data
    initialRecordData: null,
    sizeToRack: false,
    // size to grid on show
    sizeToGrid: false,
    //size to a given component
    sizeToComponent: null,
    sizeToParent: false,
    addMode: null,
    layout: "fit",
    initComponent: function() {
        if (!this.height && !this.width && !this.sizeToComponent) {
            this.sizeToGrid = true;
        }
        if (this.title == null) {
            this.title = i18n._('Edit');
        }
        if (this.rowEditorLabelWidth == null) {
            this.rowEditorLabelWidth = 100;
        }
        this.items = Ext.create('Ext.panel.Panel',{
            border: false,
            bodyStyle: 'padding:10px 10px 0px 10px;',
            autoScroll: true,
            defaults: {
                labelWidth: this.rowEditorLabelWidth
            },
            items: this.inputLines
        });
        this.callParent(arguments);
    },
    doSize: function() {
        if(!this.sizeToComponent) {
            if(this.sizeToParent) {
                this.sizeToComponent=this.grid.up("panel");
            }
            if(!this.sizeToComponent) {
                this.sizeToComponent=this.grid;
            }
        }
        var objPosition = this.sizeToComponent.getPosition();
        if (this.sizeToComponent || this.height==null || this.width==null) {
            var objSize = this.sizeToComponent.getSize();
            this.setSize(objSize);
            if (objPosition[1] + objSize.height > Ung.Main.viewport.getSize().height) {
                objPosition[1] = Math.max(Ung.Main.viewport.getSize().height - objSize.height, 0);
            }
        }
        this.setPosition(objPosition);
    },
    populate: function(record,addMode) {
        this.addMode=addMode;
        this.record = record;
        this.initialRecordData = Ext.encode(record.data);
        this.populateRecursive(this.items, record, 0);
        if(Ext.isFunction(this.syncComponents)) {
            this.syncComponents();
        }
        Ung.Util.clearDirty(this.items);
    },
    populateRecursive: function(component, record, depth) {
        if (component == null) {
            return;
        }
        if(depth>30) {
            console.log("Ung.RowEditorWindow.populateRecursive depth>30");
            return;
        }
        if (component.dataIndex != null) {
            if( Ext.isFunction(component.reset ) ){
                component.reset();
            }
            component.suspendEvents();
            component.setValue(record.get(component.dataIndex), record);
            component.resumeEvents();
            if(this.grid.hasReadOnly && (this.grid.changableFields || []).indexOf(component.dataIndex) == -1) {
                if(Ext.isFunction(component.setReadOnly)) {
                    component.setReadOnly(record.get("readOnly") === true);
                } else {
                    component.setDisabled(record.get("readOnly") === true);
                }
            }
            return;
        }
        if (component.items) {
            for (var i = 0; i < component.items.length; i++) {
                var item = Ext.isFunction(component.items.get)?component.items.get(i):component.items[i];
                this.populateRecursive( item, record, depth+1);
            }
        }
    },
    updateAction: function() {
        if (this.validate()!==true) {
            return false;
        }
        if (this.record !== null) {
            var data = {};
            this.updateActionRecursive(this.items, data, 0);
            this.record.set(data);
            if(this.addMode) {
                if (this.grid.addAtTop) {
                    this.grid.getStore().insert(0, [this.record]);
                } else {
                    this.grid.getStore().add([this.record]);
                }
                this.grid.updateChangedData(this.record, "added");
            }
        }
        this.hide();
        this.grid.reconfigure();
        return true;
    },
    updateActionRecursive: function(component, data, depth) {
        if (component == null) {
            return;
        }
        if(depth>30) {
            console.log("Ung.RowEditorWindow.updateActionRecursive depth>30");
            return;
        }
        if (component.dataIndex != null) {
            data[component.dataIndex]= component.getValue();
            return;
        }
        if (component.items) {
            for (var i = 0; i < component.items.length; i++) {
                var item = Ext.isFunction(component.items.get)?component.items.get(i):component.items[i];
                this.updateActionRecursive( item, data, depth+1);
            }
        }
    },
    // check if the form is valid;
    // this is the default functionality which can be overwritten
    validate: function() {
        var components = this.query("component[dataIndex]");
        return this.validateComponents(components);
    },
    isDirty: function() {
        return Ung.Util.isDirty(this.items);
    },
    closeWindow: function() {
        this.record.data = Ext.decode(this.initialRecordData);
        this.hide();
    }
});

//Import Settings window
Ext.define('Ung.ImportSettingsWindow', {
    extend:'Ung.EditWindow',
    // the editor grid
    grid: null,
    height: 230,
    width: 500,
    sizeToRack: false,
    // size to grid on show
    sizeToGrid: false,
    //importMode
    // 'replace' = 'Replace current settings'
    // 'prepend' = 'Prepend to current settings'
    // 'append' = 'Append to current settings'
    importMode: 'replace',
    initComponent: function() {
        if (!this.height && !this.width) {
            this.sizeToGrid = true;
        }
        if (this.title == null) {
            this.title = i18n._('Import Settings');
        }
        this.items = Ext.create('Ext.panel.Panel',{
            autoScroll: true,
            bodyStyle: 'padding:10px 10px 0px 10px;',
            items: [{
                xtype: 'radio',
                boxLabel: i18n._('Replace current settings'),
                hideLabel: true,
                name: 'importMode',
                checked: (this.importMode=='replace'),
                listeners: {
                    "change": {
                        fn: Ext.bind(function(elem, checked) {
                            if(checked) {
                                this.importMode = 'replace';
                            }
                        }, this)
                    }
                }
            }, {
                xtype: 'radio',
                boxLabel: i18n._('Prepend to current settings'),
                hideLabel: true,
                name: 'importMode',
                checked: (this.importMode=='prepend'),
                listeners: {
                    "change": {
                        fn: Ext.bind(function(elem, checked) {
                            if(checked) {
                                this.importMode = 'prepend';
                            }
                        }, this)
                    }
                }
            }, {
                xtype: 'radio',
                boxLabel: i18n._('Append to current settings'),
                hideLabel: true,
                name: 'importMode',
                checked: (this.importMode=='append'),
                listeners: {
                    "change": {
                        fn: Ext.bind(function(elem, checked) {
                            if(checked) {
                                this.importMode = 'append';
                            }
                        }, this)
                    }
                }
            }, {
                xtype: 'component',
                margin: '5 0 5 30',
                html: "<i>" + i18n._("with settings from")+ "</i>"
            }, {
                xtype: 'form',
                name: 'importSettingsForm',
                url: 'gridSettings',
                border: false,
                items: [{
                    xtype: 'filefield',
                    fieldLabel: i18n._('File'),
                    name: 'import_settings_textfield',
                    width: 450,
                    labelWidth: 50,
                    allowBlank: false,
                    validateOnBlur: false,
                    listeners: {
                        afterrender: function(field){
                            document.getElementById(field.getId()).addEventListener(
                                'change',
                                function(event){
                                    Ext.getCmp(this.id).eventFiles = event.target.files;
                                }, 
                                false
                            );
                        }
                    }
                },{
                    xtype: 'hidden',
                    name: 'type',
                    value: 'import'
                }]
            }]
        });
        this.callParent(arguments);
    },
    doSize: function() {
        var objPosition = this.grid.getPosition();
        if (this.sizeToGrid) {
            var objSize = this.grid.getSize();
            this.setSize(objSize);
            if (objPosition[1] + objSize.height > Ung.Main.viewport.getSize().height) {
                objPosition[1] = Math.max(Ung.Main.viewport.getSize().height - objSize.height,0);
            }
        }
        this.setPosition(objPosition);
    },
    updateAction: function() {
        this.down("form[name=importSettingsForm]").getForm().submit({
            waitMsg: i18n._('Please wait while the settings are uploaded...'),
            success: Ext.bind(this.importSettingsSuccess, this ),
            failure: Ext.bind(this.importSettingsFailure, this )
        });
    },
    importSettingsSuccess: function (form, action) {
        var result = action.result;
        Ext.MessageBox.wait(i18n._("Importing Settings..."), i18n._("Please wait"));
        if(!result) {
            Ext.MessageBox.alert(i18n._("Warning"), i18n._("Import failed."));
        } else if(!result.success) {
            Ext.MessageBox.alert(i18n._("Warning"), result.msg);
        } else {
            this.grid.onImport(this.importMode, result.msg);
            this.closeWindow();
        }
    },
    importSettingsFailure: function (form, action) {
        var result = action.result;
        if(!result) {
            Ext.MessageBox.alert(i18n._("Warning"), i18n._("Import failed. No file chosen."));
            } else {
                Ext.MessageBox.alert(i18n._("Warning"), action.result.msg);
            }
    },
    isDirty: function() {
        return false;
    },
    closeWindow: function() {
        this.hide();
    }
});
