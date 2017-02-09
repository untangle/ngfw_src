Ext.define('Ung.cmp.Rules', {
    extend: 'Ext.grid.Panel',
    // xtype: 'ung.cmp.rules',
    alias: 'widget.rules',

    layout: 'fit',

    requires: [
        'Ung.cmp.RulesController',
        // 'Ung.cmp.RulesModel',
        // 'Ung.cmp.RuleEditor',
        'Ung.cmp.RecordEditor'
    ],

    // viewModel: 'rules',

    controller: 'rules',

    config: {
        // toolbarFeatures: null, // ['add', 'delete', 'revert', 'importexport'] add specific buttons to top toolbar
        // columnFeatures: null, // ['delete', 'edit', 'reorder', 'select'] add specific actioncolumns to grid
        // inlineEdit: null, // 'cell' or 'row',
        // dataProperty: null, // the settings data property, e.g. settings.dataProperty.list

        // recordActions: null,

        // conditions: null,
        // conditionsMap: null
    },

    tbar: [{
        text: 'Add'.t(),
        // iconCls: 'fa fa-plus',
        handler: 'addRecord'
    }],


    selModel: {
        // type: 'rowmodel'
    },

    sortableColumns: false,
    enableColumnHide: false,
    // border: false,
    // plugins: [{
    //     ptype: 'cellediting',
    //     clicksToEdit: 1
    // }],

    actions: {
        edit: {
            iconCls: 'fa fa-pencil',
            tooltip: 'Edit record'.t(),
            handler: 'editRecord'
        },
        delete: {
            iconCls: 'fa fa-trash-o',
            tooltip: 'Delete record'.t(),
            handler: 'deleteRecord'
        },
        moveUp: {
            iconCls: 'fa fa-chevron-up',
            tooltip: 'Move Up'.t(),
            direction: -1,
            handler: 'moveUp'
        },
        moveDown: {
            iconCls: 'fa fa-chevron-down',
            tooltip: 'Move Down'.t(),
            direction: 1,
            handler: 'moveUp'
        }
    },

    viewConfig: {
        stripeRows: false,
        getRowClass: function(record) {
            if (record.get('markedForDelete')) {
                return 'mark-delete';
            }
            //if (record.phantom) {
            //    return 'added';
            //}
            // if (record.dirty) {
            //     return 'dirty';
            // }
        }
    },

    // trackMouseOver: false,
    // disableSelection: true,
    // columnLines: true,

    initComponent: function () {

        // Ext.apply(this.config, this.cfg);

        // var columnFeatures = this.getColumnFeatures();

        if (this.columns) {
            this.columns.push({
                xtype: 'actioncolumn',
                header: 'Actions'.t(),
                align: 'center',
                items: this.recordActions
            });
        }
        // Ext.apply(this, Ext.apply(this.initialConfig, this.config));
        // Ext.apply(this.initialConfig, config);

        // Edit column
        // if (Ext.Array.contains(columnFeatures, 'edit')) {
        //     actionColumns.push({
        //         xtype: 'actioncolumn',
        //         text: 'Edit'.t(),
        //         align: 'center',
        //         width: 50,
        //         sortable: false,
        //         hideable: false,
        //         resizable: false,
        //         menuDisabled: true,
        //         // materialIcon: 'edit',
        //         // handler: 'editRecord',
        //         // editor: false,
        //         // type: 'edit',
        //         items: [{
        //             iconCls: 'fa fa-pencil-square'
        //         }]
        //     });
        // }

        // Delete column
        // if (Ext.Array.contains(columnFeatures, 'delete')) {
        //     actionColumns.push({
        //         xtype: 'actioncolumn',
        //         text: 'Delete'.t(),
        //         align: 'center',
        //         width: 70,
        //         //tdCls: 'stripe-col',
        //         sortable: false,
        //         hideable: false,
        //         resizable: false,
        //         menuDisabled: true,
        //         iconCls: 'fa fa-trash',
        //         handler: 'deleteRecord'
        //     });
        // }

        // Select column which add checkboxes for each row
        // if (Ext.Array.contains(columnFeatures, 'select')) {
        //     this.selModel = {
        //         type: 'checkboxmodel'
        //     };
        // }

        // Reorder column, allows sorting columns, overriding any other sorters
        // if (Ext.Array.contains(columnFeatures, 'reorder')) {
        //     // this.sortableColumns = false; // disable column sorting as it would affect drag sorting

        //     Ext.apply(this.items[0].viewConfig, {
        //         plugins: {
        //             ptype: 'gridviewdragdrop',
        //             dragText: 'Drag and drop to reorganize'.t(),
        //             // allow drag only from drag column icons
        //             dragZone: {
        //                 onBeforeDrag: function(data, e) {
        //                     console.log(Ext.get(e.target));
        //                     return Ext.get(e.target).hasCls('fa-arrows');
        //                 }
        //             }
        //         }
        //     });

        //     this.items[0].columns.unshift({
        //         xtype: 'actioncolumn',
        //         header: 'Reorder'.t(),
        //         width: 70,
        //         align: 'center',
        //         items: ['@moveUp', '@moveDown']
        //     });


        //     // add the droag/drop sorting column as the first column
        //     this.items[0].columns.unshift({
        //         xtype: 'gridcolumn',
        //         text: 'Reorder'.t(),
        //         align: 'center',
        //         width: 70,
        //         sortable: false,
        //         hideable: false,
        //         resizable: false,
        //         menuDisabled: true,
        //         tdCls: 'draggable',
        //         renderer: function() {
        //             return '<i class="fa fa-arrows" style="cursor: move;"></i>';
        //         },
        //         // dragEnabled: true,
        //     });
        // }

        // set action columns
        // this.columns = this.columns.concat(actionColumns);

        this.callParent(arguments);
    }

});