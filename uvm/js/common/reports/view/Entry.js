Ext.define('Ung.view.reports.Entry', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.reports-entry',

    controller: 'reports-entry',

    viewModel: {
        type: 'reports-entry'
    },

    layout: 'border',

    dockedItems: [{
        xtype: 'toolbar',
        border: false,
        dock: 'top',
        cls: 'report-header',
        padding: 5,
        style: {
            background: '#FFF'
        },
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        items: [{
            xtype: 'component',
            bind: {
                html: '<h1><span>{entry.category} /</span> {entry.title}</h1>'
            }
        }]
    }, {
        xtype: 'toolbar',
        dock: 'top',
        ui: 'footer',
        items: [{
            xtype: 'component',
            bind: {
                html: '{entry.description}'
            }
        }, '->', {
            xtype: 'component',
            html: '<i class="fa fa-spinner fa-spin fa-fw fa-lg"></i>',
            hidden: true,
            bind: {
                hidden: '{!fetching}'
            }
        }, {
            xtype: 'checkbox',
            boxLabel: 'Data View'.t(),
            reference: 'dataBtn',
            hidden: true,
            disabled: true,
            bind: {
                hidden: '{!entry || entry.type === "EVENT_LIST"}',
                disabled: '{fetching}'
            }
        }, {
            text: 'Reset View'.t(),
            iconCls: 'fa fa-refresh',
            itemId: 'resetBtn',
            handler: 'resetView',
            disabled: true,
            bind: {
                hidden: '{entry.type !== "EVENT_LIST"}',
                disabled: '{fetching}'
            }
        }, '-', {
            itemId: 'downloadBtn',
            text: 'Download Graph'.t(),
            iconCls: 'fa fa-download',
            handler: 'downloadGraph',
            hidden: true,
            disabled: true,
            bind: {
                hidden: '{!isGraphEntry}',
                disabled: '{fetching}'
            }
        }, {
            itemId: 'dashboardBtn',
            hidden: true,
            disabled: true,
            bind: {
                iconCls: 'fa {widget ? "fa-minus-circle" : "fa-plus-circle" }',
                text: '{widget ? "Remove from " : "Add to "}' + ' Dashboard',
                hidden: '{context !== "admin"}',
                disabled: '{fetching}'
            },
            handler: 'dashboardAddRemove'
        }, {
            itemId: 'exportBtn',
            text: 'Export'.t(),
            iconCls: 'fa fa-external-link-square',
            handler: 'exportEventsHandler',
            hidden: true,
            bind: {
                hidden: '{entry.type !== "EVENT_LIST"}'
            }
        }, '-', {
            text: 'Settings'.t(),
            reference: 'settingsBtn',
            // enableToggle: true,
            iconCls: 'fa fa-cog',
            hidden: true,
            bind: {
                hidden: '{!entry}'
            },
            handler: 'editEntry'
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
    }],

    items: [{
        /**
         * region containing the actual report (text, graph, events)
         */
        region: 'center',
        // border: false,
        // bodyBorder: false,
        // itemId: 'entryContainer',
        layout: 'card',
        bind: {
            activeItem: '{f_activeReportType}'
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

        dockedItems: [{
            xtype: 'toolbar',
            itemId: 'actionsToolbar',
            ui: 'footer',
            dock: 'bottom',
            // border: true,
            style: {
                background: '#F5F5F5'
            },
            hidden: true,
            bind: {
                hidden: '{!entry}'
            },
            items: [{
                xtype: 'combo',
                itemId: 'eventsLimitSelector',
                hidden: true,
                disabled: true,
                bind: {
                    hidden: '{entry.type !== "EVENT_LIST"}',
                    disabled: '{fetching}'
                },
                editable: false,
                value: 1000,
                store: [
                    [1000, '1000 ' + 'Events'.t()],
                    [10000, '10000 ' + 'Events'.t()],
                    [50000, '50000 ' + 'Events'.t()]
                ],
                queryMode: 'local',
                listeners: {
                    change: 'refreshData'
                }
            }, {
                xtype: 'label',
                margin: '0 5',
                text: 'From'.t() + ':'
            }, {
                xtype: 'datefield',
                format: 'date_fmt'.t(),
                editable: false,
                width: 100,
                disabled: true,
                bind: {
                    value: '{_sd}',
                    disabled: '{fetching}'
                }
            }, {
                xtype: 'timefield',
                increment: 10,
                // format: 'date_fmt'.t(),
                editable: false,
                width: 80,
                disabled: true,
                bind: {
                    value: '{_st}',
                    disabled: '{fetching}'
                }
            }, {
                xtype: 'label',
                margin: '0 5',
                text: 'till'
            }, {
                xtype: 'checkbox',
                boxLabel: 'Present'.t(),
                disabled: true,
                bind: {
                    value: '{tillNow}',
                    disabled: '{fetching}'
                }
            }, {
                xtype: 'datefield',
                format: 'date_fmt'.t(),
                editable: false,
                width: 100,
                hidden: true,
                disabled: true,
                bind: {
                    value: '{_ed}',
                    hidden: '{tillNow}',
                    disabled: '{fetching}'
                },
                maxValue: new Date(Math.floor(rpc.systemManager.getMilliseconds()))
            }, {
                xtype: 'timefield',
                increment: 10,
                // format: 'date_fmt'.t(),
                editable: false,
                width: 80,
                hidden: true,
                disabled: true,
                bind: {
                    value: '{_et}',
                    hidden: '{tillNow}',
                    disabled: '{fetching}'
                },
                // maxValue: new Date(Math.floor(rpc.systemManager.getMilliseconds()))
            }, {
                xtype: 'component',
                width: 50
            }, {
                xtype: 'checkbox',
                boxLabel: 'Auto Refresh'.t(),
                disabled: true,
                bind: {
                    value: '{autoRefresh}',
                    disabled: '{!autoRefresh && fetching}'
                },
                handler: 'setAutoRefresh'
            }, {
                text: 'Refresh'.t(),
                iconCls: 'fa fa-refresh',
                itemId: 'refreshBtn',
                handler: 'refreshData',
                bind: {
                    disabled: '{autoRefresh || fetching}'
                }
            }]
        }]

    }, {
        region: 'east',
        width: 350,
        weight: 20,
        split: true,
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        hidden: true,
        bind: {
            hidden: '{!eEntry}'
        },
        // border: false,
        defaults: {
            // border: false,
            bodyBorder: false,
            bodyPadding: 10
        },
        items: [{
            // generic report properties
            xtype: 'panel',
            // bodyStyle: { background: '#DDD' },
            title: 'General'.t(),
            border: false,
            defaults: {
                labelAlign: 'top'
            },
            layout: 'anchor',
            items: [{
                xtype: 'textfield',
                fieldLabel: '<strong>' + 'Title'.t() + '</strong>',
                anchor: '100%',
                hidden: true,
                bind: {
                    value: '{eEntry.title}',
                    hidden: '{!eEntry}'
                }
            }, {
                xtype: 'combo',
                itemId: 'categoryCombo',
                editable: false,
                fieldLabel: '<strong>' + 'Category'.t() + '</strong>',
                anchor: '100%',
                displayField: 'displayName',
                tpl: '<ul class="x-list-plain"><tpl for=".">' +
                        '<li role="option" class="x-boundlist-item"><img src="{icon}" style="width: 16px; height: 16px; vertical-align: middle;"> {displayName}</li>' +
                     '</tpl></ul>',
                store: 'categories',
                queryMode: 'local',
                hidden: true,
                bind: {
                    value: '{eEntry.category}',
                    hidden: '{!eEntry}'
                }
            }, {
                xtype: 'textarea',
                grow: true,
                fieldLabel: '<strong>' + 'Description'.t() + '</strong>',
                anchor: '100%',
                hidden: true,
                bind: {
                    value: '{eEntry.description}',
                    hidden: '{!eEntry}'
                }
            }, {
                xtype: 'combo',
                fieldLabel: '<strong>' + 'Report Type'.t() + '</strong>',
                anchor: '100%',
                publishes: 'value',
                editable: false,
                queryMode: 'local',
                displayField: 'name',
                valueField: 'value',
                store: {
                    data: [
                        { name: 'Text'.t(), value: 'TEXT' },
                        { name: 'Pie Graph'.t(), value: 'PIE_GRAPH' },
                        { name: 'Time Graph'.t(), value: 'TIME_GRAPH' },
                        { name: 'Time Graph Dynamic'.t(), value: 'TIME_GRAPH_DYNAMIC' },
                        { name: 'Event List'.t(), value: 'EVENT_LIST' },
                    ]
                },
                hidden: true,
                bind: {
                    value: '{eEntry.type}',
                    hidden: '{!eEntry}'
                }
            }]
        }, {
            // style properties
            xtype: 'panel',
            title: '<i class="fa fa-paint-brush"></i> ' + 'Graph/View Options'.t(),
            flex: 1,
            layout: 'anchor',
            border: false,
            defaults: {
                labelAlign: 'top',
                anchor: '100%'
            },
            items: [{
                // PIE_GRAPH
                xtype: 'combo',
                fieldLabel: 'Style'.t(),
                editable: false,
                store: [
                    ['PIE', 'Pie'.t()],
                    ['PIE_3D', 'Pie 3D'.t()],
                    ['DONUT', 'Donut'.t()],
                    ['DONUT_3D', 'Donut 3D'.t()],
                    ['COLUMN', 'Column'.t()],
                    ['COLUMN_3D', 'Column 3D'.t()]
                ],
                queryMode: 'local',
                hidden: true,
                bind: {
                    value: '{eEntry.pieStyle}',
                    hidden: '{eEntry.type !== "PIE_GRAPH"}'
                }
            }, {
                // PIE_GRAPH
                xtype: 'numberfield',
                fieldLabel: 'Pie Slices Number'.t(),
                // anchor: '30%',
                minValue: 1,
                maxValue: 25,
                allowBlank: false,
                hidden: true,
                bind: {
                    value: '{eEntry.pieNumSlices}',
                    hidden: '{eEntry.type !== "PIE_GRAPH"}'
                }
            }, {
                // TIME_GRAPH, TIME_GRAPH_DYNAMIC
                xtype: 'combo',
                fieldLabel: 'Time Chart Style'.t(),
                editable: false,
                store: [
                    ['LINE', 'Line'.t()],
                    ['AREA', 'Area'.t()],
                    ['AREA_STACKED', 'Stacked Area'.t()],
                    ['BAR', 'Column'.t()],
                    ['BAR_OVERLAPPED', 'Overlapped Columns'.t()],
                    ['BAR_STACKED', 'Stacked Columns'.t()]
                ],
                queryMode: 'local',
                hidden: true,
                bind: {
                    value: '{eEntry.timeStyle}',
                    hidden: '{eEntry.type !== "TIME_GRAPH" && eEntry.type !== "TIME_GRAPH_DYNAMIC"}'
                },
            }, {
                // TIME_GRAPH, TIME_GRAPH_DYNAMIC
                xtype: 'combo',
                fieldLabel: 'Time Data Interval'.t(),
                editable: false,
                store: [
                    ['AUTO', 'Auto'.t()],
                    ['SECOND', 'Second'.t()],
                    ['MINUTE', 'Minute'.t()],
                    ['HOUR', 'Hour'.t()],
                    ['DAY', 'Day'.t()],
                    ['WEEK', 'Week'.t()],
                    ['MONTH', 'Month'.t()]
                ],
                queryMode: 'local',
                hidden: true,
                bind: {
                    value: '{eEntry.timeDataInterval}',
                    hidden: '{eEntry.type !== "TIME_GRAPH" && eEntry.type !== "TIME_GRAPH_DYNAMIC"}'
                },
            }, {
                // TIME_GRAPH, TIME_GRAPH_DYNAMIC
                xtype: 'combo',
                fieldLabel: 'Approximation'.t(),
                editable: false,
                store: [
                    ['average', 'Average'.t()],
                    ['high', 'High'.t()],
                    ['low', 'Low'.t()],
                    ['sum', 'Sum'.t() + ' (' + 'default'.t() + ')'] // default
                ],
                queryMode: 'local',
                hidden: true,
                bind: {
                    value: '{f_approximation}',
                    hidden: '{eEntry.type !== "TIME_GRAPH" && eEntry.type !== "TIME_GRAPH_DYNAMIC"}'
                },
            }, {
                // PIE_GRAPH, TIME_GRAPH, TIME_GRAPH_DYNAMIC
                xtype: 'textarea',
                fieldLabel: 'Colors'.t() + ' (comma sep.)',
                hidden: true,
                bind: {
                    value: '{_colorsStr}',
                    hidden: '{eEntry.type !== "PIE_GRAPH" && eEntry.type !== "TIME_GRAPH" && eEntry.type !== "TIME_GRAPH_DYNAMIC"}'
                },
            }, {
                xtype: 'fieldcontainer',
                // fieldLabel: 'Display Order'.t(),
                // labelAlign: 'right',
                layout: { type: 'hbox', align: 'middle' },
                padding: '0 10',
                items: [{
                    xtype: 'label',
                    margin: '0 5 0 0',
                    html: 'Display Order'.t() + ':'
                }, {
                    xtype: 'numberfield',
                    width: 70,
                    bind: '{eEntry.displayOrder}'
                }, {
                    xtype: 'component',
                    padding: '0 5',
                    html: '<i class="fa fa-info-circle fa-gray" data-qtip="The order to display this report entry (relative to others)"></i>'
                }],
                hidden: true,
                bind: {
                    hidden: '{!eEntry}'
                }
            }]
        }],
        dockedItems: [{
            xtype: 'toolbar',
            dock: 'bottom',
            ui: 'footer',
            layout: { type: 'vbox', align: 'stretch' },
            items: [{
                xtype: 'button',
                text: 'Preview/Refresh'.t(),
                iconCls: 'fa fa-refresh fa-lg',
                scale: 'medium'
            }, {
                xtype: 'component',
                height: 5
            }, {
                xtype: 'segmentedbutton',
                allowToggle: false,
                flex: 1,
                defaults: {
                    scale: 'medium'
                },
                items: [{
                    text: 'Cancel'.t(),
                    iconCls: 'fa fa-ban fa-lg',
                    handler: 'cancelEdit'
                }, {
                    text: 'Save'.t(),
                    iconCls: 'fa fa-floppy-o fa-lg',
                }, {
                    text: 'Save as New'.t(),
                    iconCls: 'fa fa-floppy-o fa-lg',
                }]
            }]
        }]
    }, {
        region: 'south',
        height: 300,
        split: true,
        layout: 'border',
        hidden: true,
        bind: {
            hidden: '{!eEntry}'
        },
        items: [{
            region: 'west',
            title: '<i class="fa fa-database"></i> ' + 'Data Source'.t(),
            width: '50%',
            split: true,
            layout: {
                type: 'anchor'
            },
            bodyPadding: 10,
            scrollable: true,
            defaults: {
                labelWidth: 200,
                labelAlign: 'right',
                // labelStyle: 'font-weight: 600'
            },
            dockedItems: [{
                // ALL
                xtype: 'toolbar',
                dock: 'top',
                ui: 'footer',
                border: '1 1 0 1',
                items: [{
                    xtype: 'combo',
                    flex: 1,
                    fieldLabel: 'Data Table'.t(),
                    labelAlign: 'right',
                    bind: {
                        value: '{eEntry.table}',
                        store: '{tables}'
                    },
                    editable: false,
                    queryMode: 'local'
                }]
            }, {
                // ALL GRAPHS
                xtype: 'toolbar',
                dock: 'bottom',
                ui: 'footer',
                hidden: true,
                bind: {
                    hidden: '{eEntry.type !== "TIME_GRAPH" && eEntry.type !== "TIME_GRAPH_DYNAMIC" && eEntry.type !== "PIE_GRAPH"}'
                },
                items: [{
                    xtype: 'combo',
                    fieldLabel: 'Order By Column'.t(),
                    // emptyText: 'No selection. Select a Column ...',
                    flex: 1,
                    publishes: 'value',
                    value: '',
                    editable: false,
                    queryMode: 'local',
                    displayField: 'value',
                    valueField: 'value',
                    bind: {
                        store: { data: '{tableColumns}' },
                        value: '{eEntry.orderByColumn}'
                    },
                    displayTpl: '<tpl for=".">{text} [{value}]</tpl>',
                    listConfig: {
                        itemTpl: ['<div data-qtip="{value}"><strong>{text}</strong> <span style="float: right;">[{value}]</span></div>']
                    },
                }, {
                    xtype: 'segmentedbutton',
                    items: [
                        { text: '', iconCls: 'fa fa-arrow-up', value: true, tooltip: 'Ascending'.t() },
                        { text: '', iconCls: 'fa fa-arrow-down' , value: false, tooltip: 'Descending'.t() }
                    ],
                    bind: {
                        value: '{eEntry.orderDesc}',
                    }
                }]
            }],
            items: [{
                // TEXT
                xtype: 'grid',
                itemId: 'textDataColumnsGrid',
                sortableColumns: false,
                enableColumnResize: false,
                enableColumnMove: false,
                enableColumnHide: false,
                disableSelection: true,
                minHeight: 80,
                margin: '0 0 10 0',
                viewConfig: {
                    emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-lg"></i> ' + 'No Columns'.t() + '</p>',
                },
                tbar: [{
                    xtype: 'component',
                    html: 'Text Data Columns'.t(),
                    padding: '0 0 0 5'
                }, '->', {
                    xtype: 'button',
                    text: 'Add'.t(),
                    iconCls: 'fa fa-plus-circle',
                    handler: function (btn) {
                        btn.up('grid').getStore().add({ str: '' });
                    }
                }],
                plugins: [{
                    ptype: 'cellediting',
                    clicksToEdit: 1
                }],
                hidden: true,
                bind: {
                    store: {
                        data: '{textDataColumns}'
                    },
                    hidden: '{eEntry.type !== "TEXT"}'
                },
                columns: [{
                    dataIndex: 'str',
                    flex: 1,
                    editor: 'textfield',
                    renderer: function (val) {
                        return val || '<em>Click to insert column value ...</em>';
                    }
                }, {
                    xtype: 'actioncolumn',
                    width: 40,
                    align: 'center',
                    resizable: false,
                    tdCls: 'action-cell',
                    iconCls: 'fa fa-times',
                    handler: function (view, rowIndex, colIndex, item, e, record) {
                        record.drop();
                    },
                    menuDisabled: true,
                    hideable: false
                }]
            }, {
                // TEXT
                xtype: 'textfield',
                anchor: '100%',
                fieldLabel: 'Text String'.t(),
                hidden: true,
                bind: {
                    value: '{eEntry.textString}',
                    hidden: '{eEntry.type !== "TEXT"}'
                }
            }, {
                // PIE_GRAPH
                xtype: 'combo',
                fieldLabel: 'Pie Group Column'.t(),
                anchor: '100%',
                publishes: 'value',
                value: '',
                editable: false,
                queryMode: 'local',
                displayField: 'value',
                valueField: 'value',
                hidden: true,
                bind: {
                    store: { data: '{tableColumns}' },
                    value: '{eEntry.pieGroupColumn}',
                    hidden: '{eEntry.type !== "PIE_GRAPH"}'
                },
                displayTpl: '<tpl for=".">{text} [{value}]</tpl>',
                listConfig: {
                    itemTpl: ['<div data-qtip="{value}"><strong>{text}</strong> <span style="float: right;">[{value}]</span></div>']
                },
            }, {
                // PIE_GRAPH
                xtype: 'fieldcontainer',
                fieldLabel: 'Pie Sum Column'.t(),
                layout: { type: 'hbox', align: 'middle' },
                items: [{
                    xtype: 'textfield',
                    anchor: '60%',
                    bind: {
                        value: '{eEntry.pieSumColumn}'
                    }
                }, {
                    xtype: 'component',
                    padding: '0 5',
                    html: '<i class="fa fa-info-circle fa-gray" data-qtip="e.g. count(*)"></i>'
                }],
                hidden: true,
                bind: {
                    hidden: '{eEntry.type !== "PIE_GRAPH"}'
                }
            }, {
                // TIME_GRAPH
                xtype: 'grid',
                itemId: 'timeDataColumnsGrid',
                sortableColumns: false,
                enableColumnResize: false,
                enableColumnMove: false,
                enableColumnHide: false,
                disableSelection: true,
                minHeight: 80,
                margin: '0 0 10 0',
                viewConfig: {
                    emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-lg"></i> ' + 'No Columns'.t() + '</p>',
                },
                tbar: [{
                    xtype: 'component',
                    html: 'Time Data Columns'.t(),
                    padding: '0 0 0 5'
                }, '->', {
                    xtype: 'button',
                    text: 'Add'.t(),
                    iconCls: 'fa fa-plus-circle',
                    handler: function (btn) {
                        btn.up('grid').getStore().add({ str: '' });
                    }
                }],
                plugins: [{
                    ptype: 'cellediting',
                    clicksToEdit: 1
                }],
                hidden: true,
                bind: {
                    store: {
                        data: '{timeDataColumns}'
                    },
                    hidden: '{eEntry.type !== "TIME_GRAPH"}'
                },
                columns: [{
                    dataIndex: 'str',
                    flex: 1,
                    editor: 'textfield',
                    renderer: function (val) {
                        return val || '<em>Click to insert column value ...</em>';
                    }
                }, {
                    xtype: 'actioncolumn',
                    width: 40,
                    align: 'center',
                    resizable: false,
                    tdCls: 'action-cell',
                    iconCls: 'fa fa-times',
                    handler: function (view, rowIndex, colIndex, item, e, record) {
                        record.drop();
                    },
                    menuDisabled: true,
                    hideable: false
                }]
            }, {
                // TIME_GRAPH_DYNAMIC
                xtype: 'combo',
                fieldLabel: 'Time Data Dynamic Column'.t(),
                anchor: '100%',
                publishes: 'value',
                value: '',
                editable: false,
                queryMode: 'local',
                displayField: 'value',
                valueField: 'value',
                hidden: true,
                bind: {
                    store: { data: '{tableColumns}' },
                    value: '{eEntry.timeDataDynamicColumn}',
                    hidden: '{eEntry.type !== "TIME_GRAPH_DYNAMIC"}'
                },
                displayTpl: '<tpl for=".">{text} [{value}]</tpl>',
                listConfig: {
                    itemTpl: ['<div data-qtip="{value}"><strong>{text}</strong> <span style="float: right;">[{value}]</span></div>']
                }
            }, {
                // TIME_GRAPH_DYNAMIC
                xtype: 'combo',
                fieldLabel: 'Time Data Dynamic Value'.t(),
                anchor: '100%',
                publishes: 'value',
                value: '',
                editable: false,
                queryMode: 'local',
                displayField: 'value',
                valueField: 'value',
                hidden: true,
                bind: {
                    store: { data: '{tableColumns}' },
                    value: '{eEntry.timeDataDynamicValue}',
                    hidden: '{eEntry.type !== "TIME_GRAPH_DYNAMIC"}'
                },
                displayTpl: '<tpl for=".">{text} [{value}]</tpl>',
                listConfig: {
                    itemTpl: ['<div data-qtip="{value}"><strong>{text}</strong> <span style="float: right;">[{value}]</span></div>']
                }
            }, {
                // TIME_GRAPH_DYNAMIC
                xtype: 'fieldcontainer',
                fieldLabel: 'Time Data Dynamic Limit'.t(),
                layout: { type: 'hbox', align: 'middle' },
                items: [{
                    xtype: 'numberfield',
                    width: 50,
                    bind: {
                        value: '{eEntry.timeDataDynamicLimit}'
                    }
                }, {
                    xtype: 'component',
                    padding: '0 5',
                    html: '<i class="fa fa-info-circle fa-gray" data-qtip="e.g. 10"></i>'
                }],
                hidden: true,
                bind: {
                    hidden: '{eEntry.type !== "TIME_GRAPH_DYNAMIC"}'
                }
            }, {
                // TIME_GRAPH_DYNAMIC
                xtype: 'combo',
                fieldLabel: 'Time Data Aggregation Function'.t(),
                store: [
                    ['avg', 'Average'.t()],
                    ['sum', 'Sum'.t()],
                    ['min', 'Min'.t()],
                    ['max', 'Max'.t()]
                ],
                editable: false,
                queryMode: 'local',
                hidden: true,
                bind: {
                    value: '{eEntry.timeDataDynamicAggregationFunction}',
                    hidden: '{eEntry.type !== "TIME_GRAPH_DYNAMIC"}'
                }
            }, {
                // ALL GRAPHS
                xtype: 'fieldcontainer',
                fieldLabel: 'Units'.t(),
                layout: { type: 'hbox', align: 'middle' },
                items: [{
                    xtype: 'textfield',
                    anchor: '60%',
                    bind: {
                        value: '{eEntry.units}'
                    }
                }, {
                    xtype: 'component',
                    padding: '0 5',
                    html: '<i class="fa fa-info-circle fa-gray" data-qtip="e.g. sessions, bytes/s"></i>'
                }],
                hidden: true,
                bind: {
                    hidden: '{eEntry.type !== "TIME_GRAPH" && eEntry.type !== "TIME_GRAPH_DYNAMIC" && eEntry.type !== "PIE_GRAPH"}'
                }
            }, {
                // ALL GRAPHS
                xtype: 'textfield',
                anchor: '60%',
                fieldLabel: 'Series Renderer'.t(),
                hidden: true,
                bind: {
                    value: '{eEntry.seriesRenderer}',
                    hidden: '{eEntry.type !== "TIME_GRAPH" && eEntry.type !== "TIME_GRAPH_DYNAMIC" && eEntry.type !== "PIE_GRAPH"}'
                }
            }]
        }, {
            region: 'center',
            title: '<i class="fa fa-filter"></i> ' + 'SQL Conditions'.t(),
            xtype: 'grid',
            itemId: 'sqlConditions',
            sortableColumns: false,
            enableColumnResize: false,
            enableColumnMove: false,
            enableColumnHide: false,
            // hideHeaders: true,
            disableSelection: true,
            viewConfig: {
                emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-lg"></i> ' + 'No Conditions'.t() + '</p>',
                stripeRows: false,
            },
            fields: ['column', 'operator', 'value'],
            bind: {
                store: {
                    data: '{_sqlConditions}'
                }
            },
            dockedItems: [{
                xtype: 'toolbar',
                dock: 'top',
                layout: {
                    type: 'hbox',
                    align: 'stretch'
                },
                items: [{
                    xtype: 'combo',
                    emptyText: 'Select Column ...',
                    flex: 1,
                    itemId: 'sqlConditionsCombo',
                    reference: 'sqlConditionsCombo',
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
                }, {
                    xtype: 'button',
                    text: 'Add',
                    iconCls: 'fa fa-plus-circle',
                    disabled: true,
                    bind: {
                        disabled: '{!sqlConditionsCombo.value}'
                    },
                    handler: 'addSqlCondition'
                }]
            }],
            columns: [{
                header: 'Column'.t(),
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
                flex: 1,
                widget: {
                    xtype: 'textfield',
                    bind: '{record.value}'
                }
            }, {
                xtype: 'actioncolumn',
                width: 22,
                align: 'center',
                iconCls: 'fa fa-minus-circle',
                handler: 'removeSqlCondition'
                // xtype: 'widgetcolumn',
                // width: 22,
                // align: 'center',
                // widget: {
                //     xtype: 'button',
                //     width: 20,
                //     iconCls: 'fa fa-minus-circle',
                //     handler: 'removeSqlCondition'
                // }
            }]
        }]
    }, {
        region: 'east',
        xtype: 'grid',
        itemId: 'currentData',
        width: 400,
        minWidth: 400,
        split: true,
        store: { data: [] },
        bind: {
            hidden: '{!dataBtn.checked}',
        },
        bbar: ['->', {
            itemId: 'exportGraphData',
            text: 'Export Data'.t(),
            iconCls: 'fa fa-external-link-square',
            handler: 'exportGraphData'
        }]
    }, {
        /**
         * Global conditions which apply to all reports
         */
        region: 'south',
        weight: 25,
        xtype: 'grid',
        height: 280,
        title: Ext.String.format('Global Conditions: {0}'.t(), 0),
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
    }]

});
