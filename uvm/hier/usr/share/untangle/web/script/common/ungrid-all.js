Ext.define('Ung.cmp.DataImporter', {
    extend: 'Ext.window.Window',
    width: 500,
    height: 200,

    alias: 'widget.dataimporter',

    title: 'Import Settings'.t(),
    // controller: 'recordeditor',

    viewModel: {
        data: {
            importMode: 'replace'
        }
    },
    actions: {
        import: {
            text: 'Import'.t(),
            formBind: true,
            iconCls: 'fa fa-check',
            handler: function (btn) {
                console.log(btn.up('form'));
                btn.up('form').submit({
                    waitMsg: 'Please wait while the settings are uploaded...'.t(),
                    success: function () {
                        console.log('success');
                    },
                    failure: function () {
                        console.log('failure');
                    }
                });
            }
            // handler: 'onApply'
        },
        cancel: {
            text: 'Cancel',
            iconCls: 'fa fa-check',
            // handler: 'onCancel'
        }
    },

    autoShow: true,
    modal: true,
    constrain: true,
    layout: 'fit',
    bodyStyle: {
        background: '#FFF'
    },

    items: [{
        xtype: 'form',
        bodyPadding: 10,
        bbar: ['->', '@cancel', '@import'],
        name: 'importSettingsForm',
        // url: 'gridSettings',
        url: 'http://localhost:8002/admin/gridSettings',
        border: false,
        items: [{
            xtype: 'radiogroup',
            // fieldLabel: 'Two Columns',
            // Arrange radio buttons into two columns, distributed vertically
            columns: 1,
            vertical: true,
            simpleValue: true,
            bind: '{importMode}',
            items: [
                { boxLabel: 'Replace current settings'.t(), name: 'importMode', inputValue: 'replace' },
                { boxLabel: 'Prepend to current settings'.t(), name: 'importMode', inputValue: 'prepend'},
                { boxLabel: 'Append to current settings'.t(), name: 'importMode', inputValue: 'append' }
            ]
        }, {
            xtype: 'filefield',
            width: '100%',
            fieldLabel: 'with settings from'.t(),
            labelAlign: 'top',
            // name: 'import_settings_textfield',
            // width: 450,
            // labelWidth: 50,
            allowBlank: false,
            validateOnBlur: false,
            // listeners: {
            //     afterrender: function(field){
            //         document.getElementById(field.getId()).addEventListener(
            //             'change',
            //             function(event){
            //                 Ext.getCmp(this.id).eventFiles = event.target.files;
            //             },
            //             false
            //         );
            //     }
            // }
        }, {
            xtype: 'hidden',
            name: 'type',
            value: 'import'
        }]
    }],
});

Ext.define('Ung.cmp.Grid', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.ungrid',

    controller: 'ungrid',

    actions: {
        add: { text: 'Add'.t(), iconCls: 'fa fa-plus-circle fa-lg', handler: 'addRecord' },
        addInline: { text: 'Add'.t(), iconCls: 'fa fa-plus-circle fa-lg', handler: 'addRecordInline' },
        import: { text: 'Import'.t(), iconCls: 'fa fa-arrow-down', handler: 'importData' },
        export: { text: 'Export'.t(), iconCls: 'fa fa-arrow-up', handler: 'exportData' },
        replace: { text: 'Import'.t(), iconCls: 'fa fa-arrow-down', handler: 'replaceData' },
        // moveUp: { iconCls: 'fa fa-chevron-up', tooltip: 'Move Up'.t(), direction: -1, handler: 'moveUp' },
        // moveDown: { iconCls: 'fa fa-chevron-down', tooltip: 'Move Down'.t(), direction: 1, handler: 'moveUp' }
    },

    /**
     * @cfg {Array} tbar
     * Contains the grid action buttons placed in the top toolbar
     * Possible values:
     * '@add' - opens up a popup form with an emptyRecord
     * '@addInline' - add a new emptyRecord directly to the grid (meaning that grid columns have an editor defined for inline cell editing)
     * '@import' - imports data from file
     * '@export' - exports data to file
     * '@replace' - imports data from file without prepend or append options
     */
    tbar: null,

    /**
     * @cfg {Array} recordActions
     * The action columns for the grid.
     * Possible values:
     * '@edit' - opens a popup form for editing record
     * '@delete' - marks record for deletions (is removed from grid upon save)
     * '@reorder' - enables records reordering by drag and drop
     */
    recordActions: null,

    /**
     * @cfg {String} listProperty
     * the string wich represents the object expression for the list
     * e.g. 'settings.portForwardRules.list'
     */
    listProperty: null,

    /**
     * @cfg {Array} conditions
     * Required for data containing conditions
     * Represents a list of conditions as defined in Ung.cmp.GridConditions, or custom conditions defined inline
     * e.g. [Condition.dstAddr, Condition.dstPort, Condition.protocol([['TCP','TCP'],['UDP','UDP']])]
     */
    conditions: null,

    /**
     * @cfg {Array} columns
     * The default columns configuration
     * Represents a list of columns as defined in Ung.cmp.GridColumns, or custom columns defined inline
     * e.g. [Column.ruleId, Column.enabled, Column.description, Column.conditions]
     */
    columns: null,

    /**
     * @cfg {Array} editorFields
     * The definition of fields which are used in the Popup record editor form
     * Represents a list of fields as defined in Ung.cmp.GridEditorFields, or custom field defined inline
     * e.g. [Field.description, Fields.conditions, Field.newDestination, Field.newPort]
     */
    editorFields: null,

    /**
     * @cfg {string} editorXtype
     * Override the default record editor xtype of 'ung.cmp.recordeditor'.
     * Almost certainly a defintion you'll extend from ung.cmp.recordeditor.
     */
    editorXtype: 'ung.cmp.recordeditor',

    /**
     * @cfg {Object} emptyRow
     * Required for adding new records
     * Represents an object used to create a new record for a specific grid
     * example:
     * {
     *     ruleId: -1,
     *     enabled: true,
     *     javaClass: 'com.untangle.uvm.network.PortForwardRule',
     *     conditions: {
     *         javaClass: 'java.util.LinkedList',
     *         list: []
     *     }
     * }
     */
    emptyRow: null,

    /**
     * @cfg {String} actionText
     * Used in grids with conditions.
     * Tells the actions which are taken if condition are met
     * e.g. 'Forward to the following location:'.t()
     */
    actionText: 'Perform the following action(s):'.t(),

    /**
     * @cfg {String} parentView
     * The itemId of the component used to get an extra controller with actioncolumn methods specific for that view purpose
     * e.g. '#users' which alloes accessing the UsersController and call actioncolumn methods from it
     */
    parentView: null,

    stateful: false,

    layout: 'fit',
    trackMouseOver: false,
    enableColumnHide: false,
    // columnLines: true,
    scrollable: true,
    selModel: {
        type: 'cellmodel'
    },

    plugins: [{
        ptype: 'cellediting',
        clicksToEdit: 1
    }, {
        ptype: 'responsive'
    }],

    initComponentColumn: function(column){
        if( this.stateful &&
            !column.stateId &&
            column.dataIndex){
            column.stateId = column.dataIndex;
        }

        if( column.xtype == 'checkcolumn' && column.checkAll){
            var columnDataIndex = column.dataIndex;

            if( this.tbar ){
                this.tbar.splice( this.tbarSeparatorIndex, 0, Ext.applyIf(column.checkAll, {
                    xtype: 'checkbox',
                    hidden: !rpc.isExpertMode,
                    hideLabel: true,
                    margin: '0 5px 0 5px',
                    boxLabel: Ext.String.format("{0} All".t(), column.header),
                    // scope: {columnDataIndex: columnDataIndex},
                    handler: function(checkbox, checked) {
                        var records=checkbox.up("grid").getStore().getRange();
                        for(var i=0; i<records.length; i++) {
                            records[i].set(this.colDataIndex, checked);
                        }
                    },
                }));
                this.tbarSeparatorIndex++;
            }
        }

        if( column.rtype ){
            column.renderer = 'columnRenderer';
        }
    },

    listeners: {
        beforeedit: 'beforeEdit'
    },

    initComponent: function () {
        /*
         * Treat viewConfig as an object that inline configuration can override on an
         * individual field level instead of the entire viewConfig object itself.
         */
        var viewConfig = {
            enableTextSelection: true,
            emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-lg"></i> No Data!</p>',
            stripeRows: false,
            getRowClass: function(record) {
                if (record.get('markedForDelete')) {
                    return 'mark-delete';
                }
                if (record.get('markedForNew')) {
                    return 'mark-new';
                }
                if (record.get('readOnly')) {
                    return 'mark-readonly';
                }
            }
        };
        if( this.viewConfig ){
            Ext.apply( viewConfig, this.viewConfig );
        }

        var columns = Ext.clone(this.columns), i;

        if( this.stateful &&
            ( this.itemId ||
              this.reference ) ) {
            this.stateId = "ungrid-" + this.itemId ? this.itemId : this.reference;
        }

        if (this.tbar == null) {
            this.tbar=[];
        }
        this.tbarSeparatorIndex = this.tbar.indexOf('->');
        if( this.tbarSeparatorIndex == -1 ){
            this.tbarSeparatorIndex = this.tbar.length;
        }

        if(columns){
            /*
             * Reports and others can set their columns manually.
             */
            columns.forEach( Ext.bind(function( column ){
                if( column.columns ){
                    /*
                     * Grouping
                     */
                    column.columns.forEach( Ext.bind( function( subColumn ){
                        this.initComponentColumn( subColumn );
                    }, this ) );
                }

                this.initComponentColumn( column );
            }, this ) );
        }

        if (this.recordActions) {
            for (i = 0; i < this.recordActions.length; i += 1) {
                var action = this.recordActions[i];
                if (action === 'changePassword') {
                    columns.push({
                        xtype: 'actioncolumn',
                        width: 120,
                        header: 'Change Password'.t(),
                        align: 'center',
                        resizable: false,
                        tdCls: 'action-cell',
                        iconCls: 'fa fa-lock',
                        menuDisabled: true,
                        hideable: false,
                        handler: 'changePassword'
                    });
                }
                if (action === 'edit') {
                    columns.push({
                        xtype: 'actioncolumn',
                        width: 60,
                        header: 'Edit'.t(),
                        align: 'center',
                        resizable: false,
                        tdCls: 'action-cell',
                        iconCls: 'fa fa-pencil',
                        handler: 'editRecord',
                        menuDisabled: true,
                        hideable: false,
                        isDisabled: function (table, rowIndex, colIndex, item, record) {
                            return record.get('readOnly') || false;
                        }
                    });
                }
                if (action === 'copy') {
                    columns.push({
                        xtype: 'actioncolumn',
                        width: 60,
                        header: 'Copy'.t(),
                        align: 'center',
                        resizable: false,
                        tdCls: 'action-cell',
                        iconCls: 'fa fa-files-o',
                        handler: 'copyRecord',
                        menuDisabled: true,
                        hideable: false,
                    });
                }
                if (action === 'delete') {
                    columns.push({
                        xtype: 'actioncolumn',
                        width: 60,
                        header: 'Delete'.t(),
                        align: 'center',
                        resizable: false,
                        tdCls: 'action-cell',
                        iconCls: 'fa fa-trash-o fa-red',
                        handler: 'deleteRecord',
                        menuDisabled: true,
                        hideable: false,
                        isDisabled: function (table, rowIndex, colIndex, item, record) {
                            return record.get('readOnly') || false;
                        }
                    });
                }

                if (action === 'reorder') {
                    this.sortableColumns = false;
                    Ext.apply( viewConfig, {
                        plugins: {
                            ptype: 'gridviewdragdrop',
                            dragText: 'Drag and drop to reorganize'.t(),
                            // allow drag only from drag column icons
                            dragZone: {
                                onBeforeDrag: function (data, e) {
                                    return Ext.get(e.target).hasCls('fa-arrows');
                                }
                            }
                        }
                    });
                    columns.unshift({
                        xtype: 'gridcolumn',
                        header: '<i class="fa fa-sort"></i>',
                        align: 'center',
                        width: 30,
                        resizable: false,
                        tdCls: 'action-cell',
                        menuDisabled: true,
                        hideable: false,
                        // iconCls: 'fa fa-arrows'
                        renderer: function() {
                            return '<i class="fa fa-arrows" style="cursor: move;"></i>';
                        },
                    });
                }
            }
        }

        Ext.apply(this, {
            columns: columns,
            viewConfig: viewConfig
        });
        this.callParent(arguments);
    }

});

Ext.define('Ung.cmp.GridColumns', {
    singleton: true,
    alternateClassName: 'Column',

    ruleId: {
        header: 'Rule Id'.t(),
        width: 70,
        align: 'right',
        resizable: false,
        dataIndex: 'ruleId',
        renderer: function(value) {
            return value < 0 ? 'new'.t() : value;
        }
    },

    enabled: {
        xtype: 'checkcolumn',
        header: 'Enable'.t(),
        dataIndex: 'enabled',
        resizable: false,
        width: 70
    },

    flagged: {
        xtype: 'checkcolumn',
        header: 'Flagged'.t(),
        dataIndex: 'flagged',
        resizable: false,
        width: 70
    },

    blocked: {
        xtype: 'checkcolumn',
        header: 'Blocked'.t(),
        dataIndex: 'blocked',
        resizable: false,
        width: 70
    },

    description: {
        header: 'Description',
        width: 200,
        dataIndex: 'description',
        renderer: function (value) {
            return value || '<em>no description<em>';
        }
    },

    conditions: {
        header: 'Conditions'.t(),
        flex: 1,
        dataIndex: 'conditions',
        renderer: 'conditionsRenderer'
    },

    bypass: {
        header: 'Bypass'.t(),
        xtype: 'checkcolumn',
        dataIndex: 'bypass',
        width: 70
    }
});


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
            newRecord = Ext.create('Ung.model.Rule', Ung.util.Util.activeClone(v.emptyRow));
        newRecord.set('markedForNew', true);
        if (v.topInsert) {
            v.getStore().insert(0, newRecord);
        } else {
            v.getStore().add(newRecord);
        }
    },

    regexId :  /^([^_]).*(id|Id)$/,
    copyRecord: function (view, rowIndex, colIndex, item, e, record) {
        var me = this,
            v = me.getView(),
            grid = v.down('grid'),
            newRecord = record.copy(null);
        newRecord.set('markedForNew', true);

        var id = null;
        for( var key in record.data){
            var idMatches = me.regexId.exec( key );
            if( idMatches ) {
                var value = record.get(key);
                if( isNaN(value) == false ){
                    newRecord.set(key, -1);
                }
                break;
            }
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
        this.editorWin(record);
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
            extraCtrl = view.up(view.parentView).getController();

        if (!extraCtrl) {
            console.log('Unable to get the extra controller');
            return;
        }

        // call the action from the extra controller in extra controller scope, and pass all the actioncolumn arguments
        if (item.action) {
            extraCtrl[item.action].apply(extraCtrl, arguments);
        } else {
            console.log('External action not defined!');
        }
    }

});

Ext.define('Ung.cmp.GridEditorFields', {
    singleton: true,
    alternateClassName: 'Field',

    conditions: {
        flex: 1,
        dataIndex: 'conditions',
        renderer: 'conditionsRenderer'
    },

    enableRule: function (label) {
        return {
            xtype: 'checkbox',
            fieldLabel: label || 'Enable'.t(),
            bind: '{record.enabled}',
        };
    },

    enableIpv6: {
        xtype: 'checkbox',
        fieldLabel: 'Enable IPv6 Support'.t(),
        bind: '{record.ipv6Enabled}',
    },

    blockedCombo: {
        xtype: 'combo',
        fieldLabel: 'Action'.t(),
        bind: '{record.blocked}',
        store: [[true, 'Block'.t()], [false, 'Pass'.t()]],
        queryMode: 'local',
        editable: false,
    },

    blocked: {
        xtype: 'checkbox',
        bind: '{record.blocked}',
        fieldLabel: 'Block'.t(),
        width: 100
    },

    flagged: {
        xtype: 'checkbox',
        bind: '{record.flagged}',
        fieldLabel: 'Flag'.t(),
        width: 100
    },

    allow: {
        xtype: 'combo',
        fieldLabel: 'Action'.t(),
        bind: '{record.allow}',
        store: [[false, 'Deny'.t()], [true, 'Allow'.t()]],
        queryMode: 'local',
        editable: false
    },

    bypass: {
        xtype: 'combo',
        fieldLabel: 'Action'.t(),
        bind: '{record.bypass}',
        store: [[true, 'Bypass'.t()], [false, 'Process'.t()]],
        queryMode: 'local',
        editable: false
    },

    macAddress: {
        xtype: 'textfield',
        fieldLabel: 'MAC Address'.t(),
        allowBlank: false,
        bind: '{record.macAddress}',
        emptyText: '[enter MAC name]'.t(),
        vtype: 'macAddress',
        maskRe: /[a-fA-F0-9:]/
    },

    ipAddress: {
        xtype: 'textfield',
        fieldLabel: 'Address'.t(),
        emptyText: '[enter address]'.t(),
        bind: '{record.address}',
        allowBlank: false,
        vtype: 'ipAddress',
    },

    network: {
        xtype: 'textfield',
        fieldLabel: 'Network'.t(),
        emptyText: '1.2.3.0'.t(),
        allowBlank: false,
        vtype: 'ipAddress',
        bind: '{record.network}',
    },

    netMask: {
        xtype: 'combo',
        fieldLabel: 'Netmask/Prefix'.t(),
        bind: '{record.prefix}',
        store: Util.getV4NetmaskList(false),
        queryMode: 'local',
        editable: false
    },

    natType: {
        xtype: 'combo',
        fieldLabel: 'NAT Type'.t(),
        bind: '{record.auto}',
        allowBlank: false,
        editable: false,
        store: [[true, 'Auto'.t()], [false, 'Custom'.t()]],
        queryMode: 'local',
    },
    natSource: {
        xtype: 'textfield',
        fieldLabel: 'New Source'.t(),
        width: 100,
        bind: {
            value: '{record.newSource}',
            disabled: '{record.auto}'
        },
        allowBlank: true,
        vtype: 'ipAddress'
    },
    description: {
        xtype: 'textfield',
        fieldLabel: 'Description'.t(),
        bind: '{record.description}',
        emptyText: '[no description]'.t(),
        allowBlank: false
    },

    newDestination: {
        xtype: 'textfield',
        fieldLabel: 'New Destination'.t(),
        bind: '{record.newDestination}',
        allowBlank: false,
        vtype: 'ipAddress'
    },

    newPort: {
        xtype: 'numberfield',
        fieldLabel: 'New Port'.t(),
        width: 100,
        bind: '{record.newPort}',
        allowBlank: true,
        minValue : 1,
        maxValue : 0xFFFF,
        vtype: 'port'
    },

    priority: {
        xtype: 'combo',
        fieldLabel: 'Priority'.t(),
        store: [
            [1, 'Very High'.t()],
            [2, 'High'.t()],
            [3, 'Medium'.t()],
            [4, 'Low'.t()],
            [5, 'Limited'.t()],
            [6, 'Limited More'.t()],
            [7, 'Limited Severely'.t()]
        ],
        bind: '{record.priority}',
        queryMode: 'local',
        editable: false
    },

    // string: {
    //     xtype: 'textfield',
    //     name: "Site",
    //     dataIndex: "string",
    //     fieldLabel: i18n._("Site"),
    //     emptyText: i18n._("[enter site]"),
    //     allowBlank: false,
    //     width: 400,
    // }

});


Ext.define('Ung.cmp.GridFilter', {
    extend: 'Ext.form.field.Text',
    alias: 'widget.ungridfilter',

    controller: 'ungridfilter',

    reference: 'filterfield',
    fieldLabel: 'Filter'.t(),
    emptyText: 'Filter data ...'.t(),
    labelWidth: 'auto',
    enableKeyEvents: true,

    triggers: {
        clear: {
            cls: 'x-form-clear-trigger',
            hidden: true,
            handler: function (field) {
                field.setValue('');
            }
        }
    },

    listeners: {
        change: 'filterEventList',
        buffer: 100
    }

});
Ext.define('Ung.cmp.GridFilterController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.ungridfilter',

    filterEventList: function (field, value) {
        var me = this,
            v = me.getView(),
            grid = v.up('panel').down('grid'),
            cols = grid.getVisibleColumns(),
            routeFilter = v.up('panel').routeFilter;

        grid.getStore().clearFilter();

        // add route filter
        if (routeFilter) {
            grid.getStore().getFilters().add(routeFilter);
        }

        if (!value) {
            field.getTrigger('clear').hide();
            return;
        }

        var regex = Ext.String.createRegex(value, false, false, true);

        grid.getStore().getFilters().add(function (item) {
            var str = [], filtered = false;

            Ext.Array.each(cols, function (col) {
                var val = item.get(col.dataIndex);
                if (!val) { return; }
                str.push(typeof val === 'object' ? Util.timestampFormat(val) : val.toString());
            });
            if (regex.test(str.join('|'))) { filtered = true; }

            // exclude if record does not meet route filter
            if (routeFilter) {
                if (item.get(routeFilter.property) !== routeFilter.value) {
                    filtered = false;
                }
            }
            return filtered;
        });

        field.getTrigger('clear').show();

        var gridStatus = v.up('panel').down('ungridstatus');
        if( gridStatus ){
            gridStatus.fireEvent('update');
        }
    }
});

Ext.define('Ung.cmp.GridStatus', {
    extend: 'Ext.toolbar.TextItem',
    alias: 'widget.ungridstatus',

    controller: 'ungridstatus',

    tplFiltered: '{0} filtered, {1} total entries'.t(),
    tplUnfiltered: '{0} entries'.t(),

    listeners: {
        update: 'onUpdateGridStatus'
    }

});

Ext.define('Ung.cmp.GridStatusController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.ungridstatus',

    onUpdateGridStatus: function(){
        var me = this,
            v = me.getView(),
            store = v.up('panel').down('grid').getStore();

        var count = store.getCount();
        if( store.getData().getSource() ){
            v.update( Ext.String.format( v.tplFiltered, count, store.getData().getSource().items.length ) );
        }else{
            v.update( Ext.String.format( v.tplUnfiltered, count ) );
        }
    }
});

Ext.define('Ung.cmp.PropertyGrid', {
    extend: 'Ext.grid.property.Grid',
    alias: 'widget.unpropertygrid',

    controller: 'unpropertygrid',

    editable: false,
    width: 400,
    split: true,
    collapsible: true,
    resizable: true,
    shadow: false,
    animCollapse: false,
    titleCollapse: true,
    collapsed: false,

    cls: 'prop-grid',

    viewConfig: {
        getRowClass: function(record) {
            if (record.get('value') === null || record.get('value') === '') {
                return 'empty';
            }
            return;
        }
    },

    nameColumnWidth: 200,

    // features: [{
    //     ftype: 'grouping',
    //     groupHeaderTpl: '{name}'
    // }],

    listeners: {
        beforeedit: function () { return false; },
        beforeexpand: 'onBeforeExpand',
        beforerender: 'onBeforeRender'
    }
});

Ext.define('Ung.cmp.PropertyGridController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.unpropertygrid',

    getBindRecordName: function(){
        var me = this,
            v = me.getView();

        var bindRecordName = 'propertyRecord';
        if( v.initialConfig.bind &&
            ( typeof( v.initialConfig.bind ) == 'object' ) &&
            v.initialConfig.bind.source ){
            bindRecordName = v.initialConfig.bind.source.substring( 1, v.initialConfig.bind.source.length - 1);
        }

        return bindRecordName;
    },

    /*
     * If property grid started collapsed it will have no data.
     * On expansion, force population based on first row in master grid
     */
    onBeforeExpand: function(){
        var me = this,
            v = me.getView(),
            vm = me.getViewModel();

        if( vm.get(me.getBindRecordName()) == null ){
            v.up().down('grid').getView().getSelectionModel().select(0);
        }
    },

    onBeforeRender: function(){
        var me = this,
            v = me.getView(),
            vm = me.getViewModel();

        /*
         * Build source list from accompanying grid's column definitions
         */
        var masterGrid = v.up().down('grid');
        var columns = masterGrid.getColumns();

        var sourceConfig = {};
        columns.forEach( function(column){
            var displayName = column.text;
            if( column.ownerCt.text ){
                displayName = column.ownerCt.text + ' ' + displayName;
                //displayName = column.ownerCt.text + ' &#151 ' + displayName;
            }
            var config = {
                displayName: displayName
            };

            if( column.renderer && 
                !column.rtype ){
                config.renderer = column.renderer;
            }else{
                if( column.rtype ){
                    config.rtype = column.rtype;
                    config.renderer = 'columnRenderer';
                }
            }

            var key = column.dataIndex;
            sourceConfig[key] = config;
        });

        v.sourceConfig = Ext.apply({}, sourceConfig);
        v.configure(sourceConfig);
        v.reconfigure();

        masterGrid.getView().on('select', 'masterGridSelect', me );

        // this.getStore().sort('group');
        // this.getStore().group('group');

        // this.getStore().on({
        //     datachanged: Ext.bind(function( store ){
        //         var columns = this.up().down('grid').getColumns();
        //         store.each(function(record){
        //             var groupName = '';
        //             var recordName = record.get('name');
        //             columns.find( function(column){
        //                 if( column.dataIndex == recordName ){
        //                     if( column.ownerCt.text ){
        //                         groupName = column.ownerCt.text;
        //                     }
        //                 }
        //             });
        //             record.set('group', groupName);
        //         });
        //     },this)
        // });
    },

    /*
     * When row selected by master grid, have the property grid properly massage
     * data suitable for property grid.
     *
     * So keep in mind that this is all in the contet of the "grid master" we're attached to,
     * not this property grid.
     */
    masterGridSelect: function (grid, record) {
        var me = this,
            v = me.getView(),
            vm = me.getViewModel(),
            propertyRecord = record.getData();

        // hide these attributes always
        delete propertyRecord._id;
        delete propertyRecord.javaClass;
        delete propertyRecord.state;
        delete propertyRecord.attachments;

        for( var k in propertyRecord ){
            if( propertyRecord[k] == null ){
                continue;
            }
            /* If object only contains a JavaClass and one other
            /* field, use that field as the new non-object value.
            /* It works for timestamp...it should work for others. */
            if( ( typeof( propertyRecord[k] ) == 'object' ) &&
                 ( Object.keys(propertyRecord[k]).length == 2 ) &&
                 ( 'javaClass' in propertyRecord[k] ) ){
                var value = '';
                Object.keys(propertyRecord[k]).forEach( function(key){
                    if( key != 'javaClass' ){
                        value = propertyRecord[k][key];
                    }
                });
                propertyRecord[k] = value;
            }
            /*
             * Encode objects and arrays for details
             */
            if( ( typeof( propertyRecord[k] ) == 'object' ) ||
                ( typeof( propertyRecord[k] ) == 'array' ) ){
                propertyRecord[k] = Ext.encode( propertyRecord[k] );
            }
        }

        vm.set( me.getBindRecordName(), propertyRecord );
    },

    columnRenderer: function(value, metaData, record, rowIndex, columnIndex, store, view){
        var rtype = view.grid.sourceConfig[record.id].rtype;
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
                return Renderer[rtype](value);
            }
        }
        return value;
    }
});

Ext.define('Ung.cmp.RecordEditor', {
    extend: 'Ext.window.Window',
    width: 800,
    minHeight: 400,
    maxHeight: Ext.getBody().getViewSize().height - 20,

    xtype: 'ung.cmp.recordeditor',

    controller: 'recordeditor',
    closeAction: 'destroy',
    closable: false,

    viewModel: true,

    disabled: true,
    bind: {
        title: '{windowTitle}',
        disabled: '{!record}'
    },

    actions: {
        apply: {
            // bind: {
            //     text: '{actionTitle}'
            // },
            text: 'Done'.t(),
            formBind: true,
            iconCls: 'fa fa-check',
            handler: 'onApply'
        },
        cancel: {
            text: 'Cancel',
            iconCls: 'fa fa-ban',
            handler: 'onCancel'
        },
        addCondition: {
            itemId: 'addConditionBtn',
            text: 'Add Condition'.t(),
            iconCls: 'fa fa-plus'
        }
    },

    bodyStyle: {
        // background: '#FFF'
    },

    autoShow: true,
    // shadow: false,

    // layout: 'border',

    modal: true,
    // layout: {
    //     type: 'vbox',
    //     align: 'stretch'
    // },
    // tbar: [{
    //     itemId: 'addConditionBtn',
    //     text: 'Add Condition'.t(),
    //     iconCls: 'fa fa-plus',
    //     // handler: 'onAdd'
    // }],


    // scrollable: true,

    layout: 'fit',

    items: [{
        xtype: 'form',
        // region: 'center',
        scrollable: 'y',
        bodyPadding: 10,
        border: false,
        layout: 'anchor',
        defaults: {
            anchor: '100%',
            labelWidth: 180,
            labelAlign : 'right',
        },
        items: [],
        buttons: ['@cancel', '@apply']
    }],

    // initComponent: function () {
    //     var items = this.items;
    //     var form = items[0];

    //     for (var i = 0; i < this.fields.length; i++) {
    //         console.log();
    //         if (this.fields[i].editor) {
    //             if (this.fields[i].getItemId() !== 'conditions') {
    //                 form.items.push(this.fields[i].editor);
    //             } else {
    //                 this.items.push({
    //                     xtype: 'component',
    //                     html: 'some panel'
    //                 });
    //             }
    //         }
    //     }

    //     this.callParent(arguments);

    // }
});

Ext.define('Ung.cmp.RecordEditorController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.recordeditor',

    control: {
        '#': {
            beforerender: 'onBeforeRender',
            afterrender: 'onAfterRender',
            afterlayout: 'onAfterLayout',
            // close: 'onDestroy'
            // beforerender: 'onBeforeRender',
            // close: 'onClose',
        },
        'grid': {
            afterrender: 'onConditionsRender'
        }
    },

    recordBind: null,
    actionBind: null,

    conditionsGrid: {
        xtype: 'grid',
        trackMouseOver: false,
        disableSelection: true,
        sortableColumns: false,
        enableColumnHide: false,
        padding: '10 0',
        tbar: ['@addCondition'],
        bind: {
            store: {
                model: 'Ung.model.Condition',
                data: '{record.conditions.list}'
            }
        },
        viewConfig: {
            emptyText: '<p style="text-align: center; margin: 0; line-height: 2"><i class="fa fa-exclamation-triangle fa-2x"></i> <br/>No Conditions! Add from the menu...</p>',
            stripeRows: false,
        },
        columns: [{
            header: 'Type'.t(),
            menuDisabled: true,
            dataIndex: 'conditionType',
            align: 'right',
            width: 200,
            renderer: 'conditionRenderer'
        }, {
            xtype: 'widgetcolumn',
            menuDisabled: true,
            width: 80,
            resizable: false,
            widget: {
                xtype: 'combo',
                // margin: 3,
                editable: false,
                bind: '{record.invert}',
                store: [[true, 'is NOT'.t()], [false, 'is'.t()]]
            }
        }, {
            header: 'Value'.t(),
            xtype: 'widgetcolumn',
            cellWrap: true,
            menuDisabled: true,
            sortable: false,
            flex: 1,
            widget: {
                xtype: 'container',
                padding: '1 3',
                layout: 'fit'
            },
            onWidgetAttach: 'onWidgetAttach'
        }, {
            xtype: 'actioncolumn',
            menuDisabled: true,
            sortable: false,
            width: 30,
            align: 'center',
            iconCls: 'fa fa-minus-circle fa-red',
            tdCls: 'action-cell-cond',
            handler: 'removeCondition'
        }]
    },


    onBeforeRender: function (v) {
        var vm = this.getViewModel();
        this.mainGrid = v.up('grid');

        if (!v.record) {
//            v.record = Ext.create('Ung.model.Rule', Ext.clone(this.mainGrid.emptyRow));
            v.record = Ext.create('Ung.model.Rule', Ung.util.Util.activeClone(this.mainGrid.emptyRow));
            v.record.set('markedForNew', true);
            this.action = 'add';
            vm.set({
                record: v.record,
                windowTitle: 'Add'.t()
            });
        } else {
            this.getViewModel().set({
                record: v.record.copy(null),
                windowTitle: 'Edit'.t()
            });
        }

        /**
         * if record has action object
         * hard to explain but needed to keep dirty state (show as modified)
         */
        if (v.record.get('action') && (typeof v.record.get('action') === 'object')) {
            this.actionBind = vm.bind({
                bindTo: '{_action}',
                deep: true
            }, function (actionObj) {
                // console.log(actionObj);
                // console.log(vm.get('record.action'));
                // console.log(Ext.Object.equals(Ext.clone(actionObj), vm.get('record.action')));
                // if (!Ext.Object.equals(actionObj, vm.get('record.action'))) {
                vm.set('record.action', Ext.clone(actionObj));
                // }
            });
            vm.set('_action', v.record.get('action'));
        }
    },

    onAfterRender: function (view) {
        var fields = this.mainGrid.editorFields, form = view.down('form');

        // if conditions are null, create empty conditions list so they can be edited
        var vm = this.getViewModel();
        if (!vm.get('record.conditions')) {
            vm.set('record.conditions', {
                javaClass: 'java.util.LinkedList',
                list: []
            });
        }

        // add editable column fields into the form
        for (var i = 0; i < fields.length; i++) {
            if (fields[i].dataIndex !== 'conditions') {
                form.add(fields[i]);
            } else {
                form.add({
                    xtype: 'component',
                    padding: '10 0 0 0',
                    html: '<strong>' + 'If all of the following conditions are met:'.t() + '</strong>'
                });
                form.add(this.conditionsGrid);
                form.add({
                    xtype: 'component',
                    padding: '0 0 10 0',
                    html: '<strong>' + this.mainGrid.actionText + '</strong>'
                });
            }
        }
        form.isValid();
        // setTimeout(view.center();
    },

    onAfterLayout: function( container, layout){
        var bodyWindowHeight = Ext.getBody().getViewSize().height;
        var windowY = container.getY();
        var windowHeight = container.getHeight();
        var windowBottom = windowY + windowHeight;
        if(windowBottom > bodyWindowHeight){
            container.setY(windowY - (windowBottom - bodyWindowHeight) );
        }
    },

    onApply: function () {
        var v = this.getView(),
            vm = this.getViewModel(),
            condStore, invalidConditionFields = [];

        // if conditions
        if (v.down('grid')) {

            // check for invalid conditions fields
            Ext.Array.each(v.down('grid').query('field'), function (field) {
                if (!field.isValid()) {
                    invalidConditionFields.push(
                        ( field.fieldLabel ? '<b>' + field.fieldLabel + '</b>: ' : '' ) +
                        ( field.activeErrors ? field.activeErrors.join(', ') : '' )
                    );
                }
            });

            if (invalidConditionFields.length > 0){
                Ext.MessageBox.alert(
                    'Warning'.t(),
                    'One or more fields contain invalid values. Settings cannot be saved until these problems are resolved.'.t() +
                    '<br><br>' +
                    invalidConditionFields.join('<br>')
                );
                return false;
            }

            condStore = v.down('grid').getStore();
            if (condStore.getModifiedRecords().length > 0 || condStore.getRemovedRecords().length > 0 || condStore.getNewRecords().length > 0) {
                v.record.set('conditions', {
                    javaClass: 'java.util.LinkedList',
                    list: Ext.Array.pluck(condStore.getRange(), 'data')
                });
            }
        }

        if (!this.action) {
            for (var field in vm.get('record').modified) {
                if (field !== 'conditions') {
                    v.record.set(field, vm.get('record').get(field));
                }
            }
        }
        if (this.action === 'add') {
            this.mainGrid.getStore().add(v.record);
        }
        v.close();
    },

    onCancel: function () {
        this.getView().close();
    },


    onConditionsRender: function (conditionsGrid) {
        var conds = this.mainGrid.conditions,
            menuConditions = [], i;

        // create and add conditions to the menu
        if( conds ){
            // when record is modified update conditions menu
            this.recordBind = this.getViewModel().bind({
                bindTo: '{record}',
            }, this.setMenuConditions);

            for (i = 0; i < conds.length; i += 1) {
                if (conds[i].visible) {
                    menuConditions.push({
                        text: conds[i].displayName,
                        conditionType: conds[i].name,
                        index: i
                    });
                }
            }

            conditionsGrid.down('#addConditionBtn').setMenu({
                showSeparator: false,
                plain: true,
                items: menuConditions,
                mouseLeaveDelay: 0,
                listeners: {
                    click: 'addCondition'
                }
            });
        }

    },

    /**
     * Updates the disabled/enabled status of the conditions in the menu
     */
    setMenuConditions: function () {
        var conditionsGrid = this.getView().down('grid'),
            menu = conditionsGrid.down('#addConditionBtn').getMenu(),
            store = conditionsGrid.getStore();
        menu.items.each(function (item) {
            item.setDisabled(store.findRecord('conditionType', item.conditionType) ? true : false);
        });
    },

    /**
     * Adds a new condition for the edited rule
     */
    addCondition: function (menu, item) {
        var newCond = {
            conditionType: item.conditionType,
            invert: false,
            javaClass: this.mainGrid.ruleJavaClass,
            value: ''
        };
        this.getView().down('grid').getStore().add(newCond);
        this.setMenuConditions();
    },

    /**
     * Removes a condition from the rule
     */
    removeCondition: function (view, rowIndex, colIndex, item, e, record) {
        // record.drop();
        this.getView().down('grid').getStore().remove(record);
        this.setMenuConditions();
    },

    /**
     * Renders the condition name in the grid
     */
    conditionRenderer: function (val) {
        return '<strong>' + this.mainGrid.conditionsMap[val].displayName + ':</strong>';
    },

    /**
     * Adds specific condition editor based on it's defined type
     */
    onWidgetAttach: function (column, container, record) {
        var me = this;
        container.removeAll(true);

        var condition = this.mainGrid.conditionsMap[record.get('conditionType')], i;

        switch (condition.type) {
        case 'boolean':
            container.add({
                xtype: 'component',
                padding: 3,
                html: 'True'.t()
            });
            break;
        case 'textfield':
            container.add({
                xtype: 'textfield',
                fieldLabel: condition.displayName,
                hideLabel: true,
                style: { margin: 0 },
                bind: {
                    value: '{record.value}'
                },
                vtype: condition.vtype,
                allowBlank: false
            });
            break;
        case 'numberfield':
            container.add({
                xtype: 'numberfield',
                fieldLabel: condition.displayName,
                hideLabel: true,
                style: { margin: 0 },
                bind: {
                    value: '{record.value}'
                },
                vtype: condition.vtype,
                allowBlank: false
            });
            break;
        case 'checkboxgroup':
            var ckItems = [];
            for (i = 0; i < condition.values.length; i += 1) {
                ckItems.push({
                    // name: 'ck',
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
            break;
        case 'countryfield':
            // container.layout = { type: 'hbox', align: 'stretch'};
            container.add({
                xtype: 'tagfield',
                flex: 1,
                emptyText: 'Select countries or specify a custom value ...',
                store: { type: 'countries' },
                filterPickList: true,
                forceSelection: false,
                // typeAhead: true,
                queryMode: 'local',
                selectOnFocus: false,
                // anyMatch: true,
                createNewOnEnter: true,
                createNewOnBlur: true,
                value: record.get('value'),
                displayField: 'name',
                valueField: 'code',
                listConfig: {
                    itemTpl: ['<div>{name} <strong>[{code}]</strong></div>']
                },
                listeners: {
                    change: function (field, newValue) {
                        // transform array into comma separated values string
                        if (newValue.length > 0) {
                            record.set('value', newValue.join(','));
                        } else {
                            record.set('value', '');
                        }

                    }
                }
            });
            break;
        case 'userfield':
            container.add({
                xtype: 'tagfield',
                flex: 1,
                emptyText: 'Select a user or specify a custom value ...',
                store: { data: [] },
                filterPickList: true,
                forceSelection: false,
                // typeAhead: true,
                queryMode: 'local',
                selectOnFocus: false,
                // anyMatch: true,
                createNewOnEnter: true,
                createNewOnBlur: true,
                // value: record.get('value'),
                displayField: 'uid',
                valueField: 'uid',
                listConfig: {
                    itemTpl: ['<div>{uid}</div>']
                },
                listeners: {
                    afterrender: function (field) {
                        var app, data = [];
                        try {
                            app = rpc.appManager.app('directory-connector');
                        } catch (e) {
                            Util.handleException(e);
                        }
                        if (app) {
                            data = app.getUserEntries().list;
                        } else {
                            data.push({ firstName: '', lastName: null, uid: '[any]', displayName: 'Any User'});
                            data.push({ firstName: '', lastName: null, uid: '[authenticated]', displayName: 'Any Authenticated User'});
                            data.push({ firstName: '', lastName: null, uid: '[unauthenticated]', displayName: 'Any Unauthenticated/Unidentified User'});
                        }
                        field.getStore().loadData(data);
                        field.setValue(record.get('value'));
                    },
                    change: function (field, newValue) {
                        if (newValue.length > 0) {
                            record.set('value', newValue.join(','));
                        } else {
                            record.set('value', '');
                        }
                    }
                }
            });
            break;
        case 'directorygroupfield':
            container.add({
                xtype: 'tagfield',
                flex: 1,
                emptyText: 'Select a group or specify a custom value ...',
                store: { data: [] },
                filterPickList: true,
                forceSelection: false,
                // typeAhead: true,
                queryMode: 'local',
                selectOnFocus: false,
                // anyMatch: true,
                createNewOnEnter: true,
                createNewOnBlur: true,
                // value: record.get('value'),
                displayField: 'CN',
                valueField: 'SAMAccountName',
                listConfig: {
                    itemTpl: ['<div>{CN} <strong>[{SAMAccountName}]</strong></div>']
                },
                listeners: {
                    afterrender: function (field) {
                        var app, data = [];
                        try {
                            app = rpc.appManager.app('directory-connector');
                        } catch (e) {
                            Util.handleException(e);
                        }
                        if (app) {
                            data = app.getGroupEntries().list;
                        }
                        data.push({ SAMAccountName: '*', CN: 'Any Group'});

                        field.getStore().loadData(data);
                        field.setValue(record.get('value'));
                    },
                    change: function (field, newValue) {
                        if (newValue.length > 0) {
                            record.set('value', newValue.join(','));
                        } else {
                            record.set('value', '');
                        }
                    }
                }
            });
            break;
        case 'timefield':
            container.add({
                xtype: 'container',
                layout: { type: 'hbox', align: 'middle' },
                hoursStore: (function () {
                    var arr = [];
                    for (var i = 0; i <= 23; i += 1) {
                        arr.push(i < 10 ? '0' + i : i.toString());
                    }
                    return arr;
                })(),
                minutesStore: (function () {
                    var arr = [];
                    for (var i = 0; i <= 59; i += 1) {
                        arr.push(i < 10 ? '0' + i : i.toString());
                    }
                    return arr;
                })(),
                defaults: {
                    xtype: 'combo', store: [], width: 40, editable: false, queryMode: 'local', listeners: {
                        change: function (combo, newValue) {
                            var view = combo.up('container'), period = '';
                            record.set('value', view.down('#hours1').getValue() + ':' + view.down('#minutes1').getValue() + '-' + view.down('#hours2').getValue() + ':' + view.down('#minutes2').getValue());
                        }
                    }
                },
                items: [
                    { itemId: 'hours1', },
                    { xtype: 'component', html: ' : ', width: 'auto', margin: '0 3' },
                    { itemId: 'minutes1' },
                    { xtype: 'component', html: 'to'.t(), width: 'auto', margin: '0 3' },
                    { itemId: 'hours2' },
                    { xtype: 'component', html: ' : ', width: 'auto', margin: '0 3' },
                    { itemId: 'minutes2' }
                ],
                listeners: {
                    afterrender: function (view) {
                        view.down('#hours1').setStore(view.hoursStore);
                        view.down('#minutes1').setStore(view.minutesStore);
                        view.down('#hours2').setStore(view.hoursStore);
                        view.down('#minutes2').setStore(view.minutesStore);
                        if (!record.get('value')) {
                            view.down('#hours1').setValue('12');
                            view.down('#minutes1').setValue('00');
                            view.down('#hours2').setValue('13');
                            view.down('#minutes2').setValue('30');
                        } else {
                            startTime = record.get('value').split('-')[0];
                            endTime = record.get('value').split('-')[1];
                            view.down('#hours1').setValue(startTime.split(':')[0]);
                            view.down('#minutes1').setValue(startTime.split(':')[1]);
                            view.down('#hours2').setValue(endTime.split(':')[0]);
                            view.down('#minutes2').setValue(endTime.split(':')[1]);
                        }
                    }
                }
            });
            break;
        }
    },

    onDestroy: function () {
        this.recordBind.destroy();
        this.recordBind = null;
        this.actionBind.destroy();
        this.actionBind = null;
    }
});
