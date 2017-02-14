Ext.define('Ung.cmp.Rules', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.rules',

    requires: [
        'Ung.cmp.RulesController',
        'Ung.cmp.RecordEditor',
        'Ung.cmp.DataImporter',
        'Ung.model.Condition'
    ],

    controller: 'rules',

    actions: {
        add: { text: 'Add'.t(), iconCls: 'fa fa-plus', handler: 'addRecord' },
        import: { text: 'Import'.t(), handler: 'importData' },
        export: { text: 'Export'.t(), handler: 'exportData' },
        edit: {
            iconCls: 'fa fa-pencil',
            tooltip: 'Edit record'.t(),
            handler: 'editRecord',
            isDisabled: function (table, rowIndex, colIndex, item, record) {
                return record.get('readOnly') || false;
            }
        },
        delete: {
            iconCls: 'fa fa-trash-o',
            tooltip: 'Delete record'.t(),
            handler: 'deleteRecord',
            isDisabled: function (table, rowIndex, colIndex, item, record) {
                return record.get('readOnly') || false;
            }
        },
        moveUp: { iconCls: 'fa fa-chevron-up', tooltip: 'Move Up'.t(), direction: -1, handler: 'moveUp' },
        moveDown: { iconCls: 'fa fa-chevron-down', tooltip: 'Move Down'.t(), direction: 1, handler: 'moveUp' }
    },

    layout: 'fit',
    trackMouseOver: false,
    sortableColumns: false,
    enableColumnHide: false,
    // forceFit: true,
    // columnLines: true,

    selModel: {
        type: 'cellmodel'
    },
    viewConfig: {
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
    },

    // bbar: [{
    //     xtype: 'form',
    //     itemId: 'exportForm',
    //     url: 'http://localhost:8002/webui/gridSettings',
    //     defaults: {
    //         xtype: 'textfield'
    //     },
    //     items: [{
    //         name: 'gridName',
    //     }, {
    //         name: 'gridData',
    //     }, {
    //         name: 'type',
    //         value: 'export'
    //     }]
    // }],

    config: {
        // toolbarFeatures: null, // ['add', 'delete', 'revert', 'importexport'] add specific buttons to top toolbar
        // columnFeatures: null, // ['delete', 'edit', 'reorder', 'select'] add specific actioncolumns to grid
        // inlineEdit: null, // 'cell' or 'row',
        // dataProperty: null, // the settings data property, e.g. settings.dataProperty.list

        // recordActions: null,

        // conditions: null,
        // conditionsMap: null
    },

    initComponent: function () {
        // to revisit the way columns are attached
        var columns = Ext.clone(this.columns), i;

        if (this.recordActions) {
            for (i = 0; i < this.recordActions.length; i += 1) {
                var action = this.recordActions[i];
                if (action === '@edit' || action === '@delete') {
                    columns.push({
                        xtype: 'actioncolumn',
                        width: 60,
                        header: action === '@edit' ? 'Edit'.t() : 'Delete'.t(),
                        align: 'center',
                        tdCls: 'action-cell',
                        items: [action]
                    });
                }
                if (action === '@reorder') {
                    Ext.apply(this.viewConfig, {
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
                        tdCls: 'action-cell',
                        // iconCls: 'fa fa-arrows'
                        renderer: function() {
                            return '<i class="fa fa-arrows" style="cursor: move;"></i>';
                        },
                    });
                }
            }
            Ext.apply(this, {
                columns: columns
            });
        }
        this.callParent(arguments);
    }

});