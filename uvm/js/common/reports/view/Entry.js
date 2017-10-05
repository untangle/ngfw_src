Ext.define('Ung.view.reports.Entry', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.reports-entry',

    controller: 'reports-entry',

    viewModel: {
        type: 'reports-entry'
    },

    layout: 'border',

    items: [{
        region: 'center',
        border: false,
        bodyBorder: false,
        itemId: 'entryContainer',
        layout: 'card',
        bind: {
            activeItem: '{_reportCard}'
        },
        items: [{
            xtype: 'graphreport',
            itemId: 'graphreport',
            renderInReports: true
        }, {
            xtype: 'eventreport',
            itemId: 'eventreport',
            renderInReports: true
        }, {
            xtype: 'textreport',
            itemId: 'textreport',
            renderInReports: true
        }],
    }, {
        region: 'west',
        width: 200,
        split: true,
        html: 'settings'
    }, {
        region: 'east',
        // xtype: 'tabpanel',
        // title: 'Data & Settings'.t(),
        width: 400,
        minWidth: 400,
        split: true,
        // animCollapse: false,
        // floatable: true,
        // floating: true,
        // collapsible: true,
        // collapsed: false,
        // titleCollapse: true,
        // hidden: true,
        border: false,
        bind: {
            hidden: '{!(dataBtn.pressed || settingsBtn.pressed)}',
            activeItem: '{dataBtn.pressed ? 0 : 1}'
        },

        layout: 'card',

        defaults: {
            border: false
        },

        items: [{
            xtype: 'grid',
            itemId: 'currentData',
            // title: '<i class="fa fa-list"></i> ' + 'Current Data'.t(),
            // hidden: true,
            // emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-2x"></i> <br/>No Data!</p>',
            store: { data: [] },
            // bind: {
            //     store: {
            //         data: '{_currentData}'
            //     },
            //     // hidden: '{entry && entry.type === "EVENT_LIST"}'
            // },
            dockedItems: [{
                xtype: 'toolbar',
                border: false,
                dock: 'top',
                cls: 'report-header',
                height: 53,
                padding: '0 10',
                items: [{
                    xtype: 'component',
                    html: '<h2>' + 'Current Data'.t() + '</h2><p>&nbsp;</p>'
                }, '->', {
                    iconCls: 'fa fa-external-link-square',
                    text: 'Export'.t(),
                    handler: 'exportGraphData'
                }, {
                    iconCls: 'fa fa-close',
                    handler: 'closeSide'
                }]
            }],
        }, {
            xtype: 'form',
            // title: '<i class="fa fa-cog"></i> ' + 'Settings'.t(),
            scrollable: 'y',
            layout: 'anchor',
            bodyBorder: false,
            bodyPadding: 10,

            dockedItems: [{
                xtype: 'toolbar',
                border: false,
                dock: 'top',
                cls: 'report-header',
                height: 53,
                padding: '0 10',
                items: [{
                    xtype: 'component',
                    html: '<h2>' + 'Settings'.t() + '</h2><p>&nbsp;</p>'
                }, '->', {
                    iconCls: 'fa fa-close',
                    handler: 'closeSide'
                }]
            }],


        }]
    }, {
        region: 'south',
        xtype: 'grid',
        height: 280,
        title: Ext.String.format('Conditions: {0}'.t(), 0),
        itemId: 'sqlFilters',
        collapsible: true,
        collapsed: true,
        animCollapse: false,
        titleCollapse: true,
        split: true,
        hidden: true,

        sortableColumns: false,
        enableColumnResize: false,
        enableColumnMove: false,
        enableColumnHide: false,
        disableSelection: true,
        // hideHeaders: true,
        bind: {
            hidden: '{!entry}',
            store: { data: '{sqlFilterData}' }
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
                store: { data: [] },
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
            flex: 1,
            // align: 'center',
            iconCls: 'fa fa-minus-circle',
            handler: 'removeSqlFilter'
        }]
    }],

    dockedItems: [{
        xtype: 'toolbar',
        border: false,
        dock: 'top',
        cls: 'report-header',
        height: 53,
        padding: '0 10',
        items: [{
            xtype: 'component',
            bind: {
                html: '{reportHeading}'
            }
        }, '->', {
            text: 'Current Data'.t(),
            reference: 'dataBtn',
            enableToggle: true,
            toggleGroup: 'side',
            iconCls: 'fa fa-list',
            hidden: true,
            bind: {
                hidden: '{!entry || entry.type === "EVENT_LIST"}'
            }
        }, {
            itemId: 'exportBtn',
            text: 'Export'.t(),
            iconCls: 'fa fa-external-link-square',
            handler: 'exportEventsHandler',
            hidden: true,
            bind: {
                hidden: '{entry.type !== "EVENT_LIST"}'
            }
        }, {
            text: 'Settings'.t(),
            reference: 'settingsBtn',
            enableToggle: true,
            toggleGroup: 'side',
            iconCls: 'fa fa-cog',
            hidden: true,
            bind: {
                hidden: '{!entry}'
            }
        }]
    }, {
        xtype: 'toolbar',
        dock: 'top',
        border: false,
        hidden: true,
        bind: {
            hidden: '{!entry || entry.type !== "EVENT_LIST"}'
        },
        items: [{
            xtype: 'ungridfilter'
        },{
            xtype: 'ungridstatus'
        }]
    }]
});
