Ext.define('Ung.cmp.Grid', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.ungrid',

    controller: 'ungrid',

    actions: {
        add: { text: 'Add'.t(), iconCls: 'fa fa-plus-circle fa-lg', handler: 'addRecord' },
        addInline: { text: 'Add'.t(), iconCls: 'fa fa-plus-circle fa-lg', handler: 'addRecordInline' },
        addSimple: { text: 'Add'.t(), iconCls: 'fa fa-plus-circle fa-lg', handler: 'addSimpleRecord' },
        import: { text: 'Import'.t(), iconCls: 'fa fa-arrow-down', handler: 'importData' },
        export: { text: 'Export'.t(), iconCls: 'fa fa-arrow-up', handler: 'exportData' },
        replace: { text: 'Import'.t(), iconCls: 'fa fa-arrow-down', handler: 'replaceData' },
        refresh: { text: 'Refresh'.t(), iconCls: 'fa fa-refresh', handler: 'refresh'},
        reset: { text: 'Reset View'.t(), iconCls: 'fa fa-undo', handler: 'resetView'}
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
     * @cfg {Boolean} defaultSortable
     *
     * If true, allow all columns to sort.
     * If the 'sortable' key on each column is defined, that value will be used instead.
     */
    defaultSortable: true,

    recordModel: 'Ung.model.Rule',

    stateful: false,

    layout: 'fit',
    enableColumnHide: false,
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
        if( column.sortable === undefined ){
            column.sortable = this.defaultSortable;
        }

        if( this.stateful &&
            !column.stateId &&
            column.dataIndex){
            column.stateId = column.dataIndex;
        }

        if( column.xtype == 'checkcolumn' && column.checkAll){
            if( this.tbar ){
                this.tbar.splice( this.tbarSeparatorIndex, 0, Ext.applyIf(column.checkAll, {
                    xtype: 'checkbox',
                    hidden: !Rpc.directData('rpc.isExpertMode'),
                    hideLabel: true,
                    margin: '0 5px 0 5px',
                    boxLabel: Ext.String.format("{0} All".t(), column.header),
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
    },

    listeners: {
        beforeedit: 'beforeEdit'
    },

    initComponent: function () {
        /*
         * Treat viewConfig as an object that inline configuration can override on an
         * individual field level instead of the entire viewConfig object itself.
         */
        var me = this;
        var viewConfig = {
            enableTextSelection: true,
            stripeRows: false,
            getRowClass: function(record) {
                if (record.get('markedForDelete')) {
                    return 'mark-delete';
                }
                if (record.get('markedForNew')) {
                    return 'mark-new';
                }
                if (this.up('grid').getController().isRecordRestricted(record) || record.get('readOnly')) {
                    return 'mark-readonly';
                }
                if (record.get('markedForMove')) {
                    return 'mark-moved';
                }
            }
        };
        if(this.emptyText){
            // If you want to override the styling, just add an emptyText member to the defined viewConfig
            viewConfig.emptyText = '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-2x"></i> <br/>' + this.emptyText + '</p>';
        }
        if( this.viewConfig ){
            Ext.apply( viewConfig, this.viewConfig );
        }

        var columns = [], i;
        columns = Ext.merge(columns, this.columns);

        if( this.stateful &&
            ( this.itemId ||
              this.reference ) ) {
            this.stateId = "ungrid-" + this.itemId ? this.itemId : this.reference;
        }

        if (this.tbar != null) {
            this.tbarSeparatorIndex = this.tbar.indexOf('->');
            if( this.tbarSeparatorIndex == -1 ){
                this.tbarSeparatorIndex = this.tbar.length;
            }
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

        var action = null;
        if (this.recordActions &&
            (me.initialConfig.actionColumnsAdded !== true) ){

            if(!me.initialConfig.columns){
                me.initialConfig.columns = me.columns;
            }
            var initialConfigColumns = Ext.clone(me.initialConfig.columns);
            var column;
            for (i = 0; i < this.recordActions.length; i += 1) {
                action = this.recordActions[i];
                if (action === 'changePassword') {
                    column = {
                        xtype: 'actioncolumn',
                        width: Renderer.actionWidth + 40,
                        header: 'Change Password'.t(),
                        align: 'center',
                        resizable: false,
                        tdCls: 'action-cell',
                        iconCls: 'fa fa-lock',
                        menuDisabled: true,
                        hideable: false,
                        handler: 'changePassword'
                    };
                    columns.push(column);
                    initialConfigColumns.push(column);
                }
                if (action === 'edit') {
                    column = {
                        xtype: 'actioncolumn',
                        width: Renderer.actionWidth,
                        header: 'Edit'.t(),
                        align: 'center',
                        resizable: false,
                        tdCls: 'action-cell',
                        iconCls: 'fa fa-pencil',
                        handler: 'editRecord',
                        menuDisabled: true,
                        hideable: false,
                        isDisabled: function (table, rowIndex, colIndex, item, record) {
                            return record.get('readOnly') || table.up('grid').getController().isRecordRestricted(record) || false;
                        }
                    };
                    columns.push(column);
                    initialConfigColumns.push(column);
                }
                if (action === 'copy') {
                    column = {
                        xtype: 'actioncolumn',
                        width: Renderer.actionWidth,
                        header: 'Copy'.t(),
                        align: 'center',
                        resizable: false,
                        tdCls: 'action-cell',
                        iconCls: 'fa fa-files-o',
                        handler: 'copyRecord',
                        menuDisabled: true,
                        hideable: false,
                    };
                    columns.push(column);
                    initialConfigColumns.push(column);
                }
                if (action === 'delete') {
                    column = {
                        xtype: 'actioncolumn',
                        width: Renderer.actionWidth,
                        header: 'Delete'.t(),
                        align: 'center',
                        resizable: false,
                        tdCls: 'action-cell',
                        iconCls: 'fa fa-trash-o fa-red',
                        handler: 'deleteRecord',
                        menuDisabled: true,
                        hideable: false,
                        isDisabled: function (table, rowIndex, colIndex, item, record) {
                            return record.get('readOnly') || table.up('grid').getController().isRecordRestricted(record) || false;
                        }
                    };
                    columns.push(column);
                    initialConfigColumns.push(column);
                }

                if (action === 'reorder') {
                    column = Column.reorder;
                    columns.unshift(column);
                    initialConfigColumns.unshift(column);
                }
            }

            Ext.apply(this.initialConfig, {
                columns: initialConfigColumns,
                actionColumnsAdded: true
            });
         }

        if (this.recordActions &&
            (me.initialConfig.actionColumnsAdded === true) ){
            for (i = 0; i < this.recordActions.length; i += 1) {
                action = this.recordActions[i];

                if (action === 'reorder') {
                    this.sortableColumns = false;
                    Ext.apply( viewConfig, {
                        enableTextSelection: false,
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
