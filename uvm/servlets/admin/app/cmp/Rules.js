Ext.define('Ung.cmp.Rules', {
    extend: 'Ext.grid.Panel',
    xtype: 'ung.cmp.rules',

    requires: [
        'Ung.cmp.RulesController',
        'Ung.cmp.RuleEditor'
    ],

    controller: 'rules',

    selModel: {
        type: 'rowmodel'
    },

    viewConfig: {
        plugins: [{
            ptype: 'cellediting',
            clicksToEdit: 1
        }],
        stripeRows: false
        // getRowClass: function(record) {
        //     //console.log(record);
        //     if (record.markDelete) {
        //         return 'delete';
        //     }
        //     //if (record.phantom) {
        //     //    return 'added';
        //     //}
        //     if (record.dirty) {
        //         return 'dirty';
        //     }
        // }
    },

    config: {
        toolbarFeatures: null, // ['add', 'delete', 'revert', 'importexport'] add specific buttons to top toolbar
        columnFeatures: null, // ['delete', 'edit', 'reorder', 'select'] add specific actioncolumns to grid
        inlineEdit: null, // 'cell' or 'row',
        dataProperty: null, // the settings data property, e.g. settings.dataProperty.list

        conditions: null,
        conditionsMap: null
    },


    tbar: [{
        text: 'Add Rule'.t(),
        iconCls: 'fa fa-plus'
    }],

    // trackMouseOver: false,
    // disableSelection: true,
    // columnLines: true,

    initComponent: function () {
        // add any action columns
        var columnFeatures = this.getColumnFeatures(),
            actionColumns = [];

        // Edit column
        if (Ext.Array.contains(columnFeatures, 'edit')) {
            actionColumns.push({
                xtype: 'actioncolumn',
                text: 'Edit'.t(),
                align: 'center',
                width: 50,
                sortable: false,
                hideable: false,
                resizable: false,
                menuDisabled: true,
                // materialIcon: 'edit',
                // handler: 'editRecord',
                // editor: false,
                // type: 'edit',
                items: [{
                    iconCls: 'fa fa-pencil-square'
                }]
            });
        }

        // Delete column
        if (Ext.Array.contains(columnFeatures, 'delete')) {
            actionColumns.push({
                xtype: 'ung.actioncolumn',
                text: 'Delete'.t(),
                align: 'center',
                width: 50,
                //tdCls: 'stripe-col',
                sortable: false,
                hideable: false,
                resizable: false,
                menuDisabled: true,
                materialIcon: 'delete',
                handler: 'deleteRecord',
                type: 'delete'
            });
        }

        // Select column which add checkboxes for each row
        if (Ext.Array.contains(columnFeatures, 'select')) {
            this.selModel = {
                type: 'checkboxmodel'
            };
        }

        // Reorder column, allows sorting columns, overriding any other sorters
        if (Ext.Array.contains(columnFeatures, 'reorder')) {
            this.sortableColumns = false; // disable column sorting as it would affect drag sorting

            Ext.apply(this, {
                viewConfig: {
                    plugins: {
                        ptype: 'gridviewdragdrop',
                        dragText: 'Drag and drop to reorganize'.t(),
                        // allow drag only from drag column icons
                        dragZone: {
                            onBeforeDrag: function(data, e) {
                                console.log(Ext.get(e.target));
                                return Ext.get(e.target).hasCls('draggable');
                            }
                        }
                    }
                }
            });

            // add the droag/drop sorting column as the first column
            actionColumns.unshift({
                xtype: 'actioncolumn',
                align: 'center',
                width: 50,
                sortable: false,
                hideable: false,
                resizable: false,
                menuDisabled: true,
                tdCls: 'draggable',
                // tpl: '<i class="fa fa-arrows"></i>'
                // dragEnabled: true,
                iconCls: 'fa fa-ellipsis-v'
            });
        }

        // set action columns
        this.columns = this.columns.concat(actionColumns);

        this.callParent(arguments);
    }

});