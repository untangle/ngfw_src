Ext.define('Ung.reports.cmp.GlobalConditions', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.globalconditions',

    controller: 'globalconditions',

    viewModel: {
        stores: {
            conditions: {
                data: [],
                listeners: {
                    datachanged: 'updateConditions'
                }
            }
        },
        formulas: {
            f_tableconfig: function (get) {
                return TableConfig.generate(get('entry.table'));
            }
        }
    },

    title: 'Global Conditions'.t() + ': ' + 'none'.t(),


    sortableColumns: false,
    enableColumnResize: false,
    enableColumnMove: false,
    enableColumnHide: false,
    disableSelection: true,
    // hideHeaders: true,
    bind: {
        hidden: '{!entry}',
        store: '{conditions}'
    },

    viewConfig: {
        emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-lg"></i> ' + 'No Conditions'.t() + '</p>',
        stripeRows: false,
    },

    dockedItems: [{
        xtype: 'toolbar',
        itemId: 'filtersToolbar',
        dock: 'top',
        layout: {
            type: 'hbox',
            align: 'stretch',
            pack: 'start'
        },
        items: [{
            xtype: 'button',
            text: 'Quick Add'.t(),
            iconCls: 'fa fa-plus-circle',
            menu: {
                plain: true,
                // mouseLeaveDelay: 0,
                items: [{
                    text: 'Loading ...'
                }],
            },
            listeners: {
                menushow: 'sqlFilterQuickItems'
            }
        }, '-',  {
            xtype: 'combo',
            emptyText: 'Select Column ...',
            labelWidth: 100,
            labelAlign: 'right',
            width: 450,
            itemId: 'sqlFilterCombo',
            reference: 'sqlFilterCombo',
            publishes: 'value',
            value: '',
            editable: false,
            queryMode: 'local',
            displayField: 'text',
            valueField: 'value',
            bind: {
                store: {
                    data: '{f_tableconfig.comboItems}'
                }
            },
            listConfig: {
                itemTpl: ['<div data-qtip="{value}"><strong>{text}</strong> <span style="float: right;">[{value}]</span></div>']
            },
            listeners: {
                change: 'onColumnChange'
            }
        }, {
            xtype: 'combo',
            width: 80,
            publishes: 'value',
            value: '=',
            itemId: 'sqlFilterOperator',
            store: ['=', '!=', '>', '<', '>=', '<=', 'like', 'not like', 'is', 'is not', 'in', 'not in'],
            editable: false,
            queryMode: 'local',
            hidden: true,
            bind: {
                hidden: '{!sqlFilterCombo.value}'
            }
        },
        // {
        //     xtype: 'textfield',
        //     itemId: 'sqlFilterValue',
        //     value: '',
        //     disabled: true,
        //     enableKeyEvents: true,
        //     bind: {
        //         disabled: '{sqlFilterCombo.value === ""}'
        //     },
        //     listeners: {
        //         keyup: 'onFilterKeyup'
        //     }
        // },
        {
            xtype: 'button',
            text: 'Add',
            iconCls: 'fa fa-plus-circle',
            disabled: true,
            bind: {
                disabled: '{!sqlFilterCombo.value}'
            },
            handler: 'addSqlFilter'
        }]
    }],

    columns: [{
        header: 'Column'.t(),
        width: 435,
        dataIndex: 'column',
        renderer: 'sqlColumnRenderer'
    }, {
        header: 'Operator'.t(),
        xtype: 'widgetcolumn',
        width: 82,
        align: 'center',
        widget: {
            xtype: 'combo',
            width: 80,
            bind: '{record.operator}',
            store: ['=', '!=', '>', '<', '>=', '<=', 'like', 'not like', 'is', 'is not', 'in', 'not in'],
            editable: false,
            queryMode: 'local'
        }

    }, {
        header: 'Value'.t(),
        xtype: 'widgetcolumn',
        width: 300,
        widget: {
            xtype: 'textfield',
            bind: '{record.value}'
        }
    }, {
        xtype: 'actioncolumn',
        width: 30,
        align: 'center',
        iconCls: 'fa fa-trash-o fa-red',
        handler: function (table, rowIndex, colIndex, item, e, record) {
            record.drop();
        }
    }, {
        flex: 1
    }]
});
