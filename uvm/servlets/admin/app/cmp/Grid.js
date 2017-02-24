Ext.define('Ung.cmp.Grid', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.ungrid',

    requires: [
        'Ung.cmp.GridController',
        'Ung.cmp.RecordEditor',
        'Ung.cmp.DataImporter',
        'Ung.model.Condition'
    ],

    controller: 'ungrid',

    /**
     * @cfg {Array} tbar
     * Contains the grid action buttons placed in the top toolbar
     * Possible values:
     * '@add' - opens up a popup form with an emptyRecord
     * '@addInline' - add a new emptyRecord directly to the grid (meaning that grid columns have an editor defined for inline cell editing)
     * '@import' - imports data from file (not implemented)
     * '@export' - exports data to file (not implemented)
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








    actions: {
        add: { text: 'Add'.t(), iconCls: 'fa fa-plus-circle fa-lg', handler: 'addRecord' },
        addInline: { text: 'Add'.t(), iconCls: 'fa fa-plus-circle fa-lg', handler: 'addRecordInline' },
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
            iconCls: 'fa fa-trash-o fa-red',
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
    forceFit: true,
    // columnLines: true,

    selModel: {
        type: 'cellmodel'
    },
    viewConfig: {
        emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-exclamation-triangle fa-2x"></i> <br/>No Data! Add from the menu...</p>',
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

    plugins: {
        ptype: 'cellediting',
        clicksToEdit: 1
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
                        resizable: false,
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
                        resizable: false,
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
