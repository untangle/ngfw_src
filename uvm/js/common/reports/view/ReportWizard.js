Ext.define('Ung.view.reports.ReportWizard', {
    extend: 'Ext.window.Window',
    alias: 'widget.reportwizard',

    title: 'Create New Report'.t(),
    modal: true,
    width: 950,
    height: 650,

    layout: 'fit',

    // bodyStyle: { background: '#FFF' },

    controller: 'newreport',
    viewModel: 'newreport',

    items: [{
        xtype: 'form',
        scrollable: true,
        border: false,
        layout: 'border',
        bodyBorder: false,
        items: [{
            // title: 'General',
            region: 'west',
            width: 250,
            weight: 20,
            split: true,
            layout: {
                type: 'vbox',
                align: 'stretch'
            },
            border: false,
            defaults: {
                // border: false,
                bodyBorder: false,
                bodyPadding: 10
            },
            items: [{
                // generic report properties
                xtype: 'panel',
                // bodyStyle: { background: '#DDD' },
                border: false,
                defaults: {
                    labelAlign: 'top'
                },
                layout: 'anchor',
                items: [{
                    xtype: 'textfield',
                    fieldLabel: '<strong>' + 'Title'.t() + '</strong>',
                    bind: '{newEntry.title}',
                    anchor: '100%'
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
                    queryMode: 'local'
                }, {
                    xtype: 'textarea',
                    grow: true,
                    fieldLabel: '<strong>' + 'Description'.t() + '</strong>',
                    bind: '{newEntry.description}',
                    anchor: '100%'
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
                    bind: {
                        value: '{newEntry.type}'
                    }
                    // xtype: 'radiogroup',
                    // simpleValue: true,
                    // fieldLabel: '<strong>' + 'Report Type'.t() + '</strong>',
                    // layout: {
                    //     type: 'vbox',
                    //     // align: 'stretch'
                    // },
                    // items: [
                    //     { boxLabel: '<i class="fa fa-align-left"></i> <strong>' + 'Text'.t() + '</strong>', name: 'rt', inputValue: 'TEXT' },
                    //     { boxLabel: '<i class="fa fa-pie-chart"></i> <strong>'  + 'Pie Graph'.t() + '</strong>', name: 'rt', inputValue: 'PIE_GRAPH' },
                    //     { boxLabel: '<i class="fa fa-line-chart"></i> <strong>' + 'Time Graph'.t() + '</strong>', name: 'rt', inputValue: 'TIME_GRAPH' },
                    //     { boxLabel: '<i class="fa fa-line-chart"></i> <strong>' + 'Time Graph Dynamic'.t() + '</strong>', name: 'rt', inputValue: 'TIME_GRAPH_DYNAMIC' },
                    //     { boxLabel: '<i class="fa fa-list-ul"></i> <strong>'    + 'Event List'.t()  + '</strong>', name: 'rt', inputValue: 'EVENT_LIST' }
                    // ],
                    // bind: {
                    //     value: '{newEntry.type}'
                    // }
                }]
            }, {
                // style properties
                xtype: 'panel',
                title: '<i class="fa fa-paint-brush"></i> ' + 'Graph/View Options'.t(),
                flex: 1,
                layout: 'anchor',
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
                        value: '{newEntry.pieStyle}',
                        hidden: '{newEntry.type !== "PIE_GRAPH"}'
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
                        value: '{newEntry.pieNumSlices}',
                        hidden: '{newEntry.type !== "PIE_GRAPH"}'
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
                        value: '{newEntry.timeStyle}',
                        hidden: '{newEntry.type !== "TIME_GRAPH" && newEntry.type !== "TIME_GRAPH_DYNAMIC"}'
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
                        value: '{newEntry.timeDataInterval}',
                        hidden: '{newEntry.type !== "TIME_GRAPH" && newEntry.type !== "TIME_GRAPH_DYNAMIC"}'
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
                        value: '{newEntry.approximation}',
                        hidden: '{newEntry.type !== "TIME_GRAPH" && newEntry.type !== "TIME_GRAPH_DYNAMIC"}'
                    },
                }, {
                    // PIE_GRAPH, TIME_GRAPH, TIME_GRAPH_DYNAMIC
                    xtype: 'textarea',
                    fieldLabel: 'Colors'.t() + ' (comma sep.)',
                    hidden: true,
                    bind: {
                        value: '{_colorsStr}',
                        hidden: '{newEntry.type !== "PIE_GRAPH" && newEntry.type !== "TIME_GRAPH" && newEntry.type !== "TIME_GRAPH_DYNAMIC"}'
                    },
                }]
            }],
            bbar: [{
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
                    bind: '{newEntry.displayOrder}'
                }, {
                    xtype: 'component',
                    padding: '0 5',
                    html: '<i class="fa fa-info-circle fa-gray" data-qtip="The order to display this report entry (relative to others)"></i>'
                }]
            }]
        }, {
            // graph
            region: 'north',
            height: 250,
            split: true,
            html: 'graph'
        }, {
            title: '<i class="fa fa-database"></i> ' + 'Data Source'.t(),
            region: 'center',
            minHeight: 200,
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
                        value: '{newEntry.table}',
                        store: '{tables}'
                    },
                    editable: false,
                    queryMode: 'local'
                }, {
                    xtype: 'button',
                    text: 'View Columns'.t()
                }]
            }, {
                // ALL GRAPHS
                xtype: 'toolbar',
                dock: 'bottom',
                ui: 'footer',
                hidden: true,
                bind: {
                    hidden: '{newEntry.type !== "TIME_GRAPH" && newEntry.type !== "TIME_GRAPH_DYNAMIC" && newEntry.type !== "PIE_GRAPH"}'
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
                        value: '{newEntry.orderByColumn}'
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
                        value: '{newEntry.orderDesc}',
                    }
                }]
            }],
            items: [{
                // TEXT
                xtype: 'grid',
                itemId: 'newEntryTextDataColumnsGrid',
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
                store: {
                    data: [],
                    proxy: {
                        type: 'memory',
                        reader: { type: 'json' }
                    }
                },
                bind: {
                    hidden: '{newEntry.type !== "TEXT"}'
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
                    value: '{newEntry.textString}',
                    hidden: '{newEntry.type !== "TEXT"}'
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
                bind: {
                    store: { data: '{tableColumns}' },
                    value: '{newEntry.pieGroupColumn}',
                    hidden: '{newEntry.type !== "PIE_GRAPH"}'
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
                        value: '{newEntry.pieSumColumn}'
                    }
                }, {
                    xtype: 'component',
                    padding: '0 5',
                    html: '<i class="fa fa-info-circle fa-gray" data-qtip="e.g. count(*)"></i>'
                }],
                hidden: true,
                bind: {
                    hidden: '{newEntry.type !== "PIE_GRAPH"}'
                }
            }, {
                // TIME_GRAPH
                xtype: 'grid',
                itemId: 'newEntryTimeDataColumnsGrid',
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
                store: {
                    data: [],
                    proxy: {
                        type: 'memory',
                        reader: { type: 'json' }
                    }
                },
                bind: {
                    hidden: '{newEntry.type !== "TIME_GRAPH"}'
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
                bind: {
                    store: { data: '{tableColumns}' },
                    value: '{newEntry.timeDataDynamicColumn}',
                    hidden: '{newEntry.type !== "TIME_GRAPH_DYNAMIC"}'
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
                bind: {
                    store: { data: '{tableColumns}' },
                    value: '{newEntry.timeDataDynamicValue}',
                    hidden: '{newEntry.type !== "TIME_GRAPH_DYNAMIC"}'
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
                        value: '{newEntry.timeDataDynamicLimit}'
                    }
                }, {
                    xtype: 'component',
                    padding: '0 5',
                    html: '<i class="fa fa-info-circle fa-gray" data-qtip="e.g. 10"></i>'
                }],
                hidden: true,
                bind: {
                    hidden: '{newEntry.type !== "TIME_GRAPH_DYNAMIC"}'
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
                    value: '{newEntry.timeDataDynamicAggregationFunction}',
                    hidden: '{newEntry.type !== "TIME_GRAPH_DYNAMIC"}'
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
                        value: '{newEntry.units}'
                    }
                }, {
                    xtype: 'component',
                    padding: '0 5',
                    html: '<i class="fa fa-info-circle fa-gray" data-qtip="e.g. sessions, bytes/s"></i>'
                }],
                hidden: true,
                bind: {
                    hidden: '{newEntry.type !== "TIME_GRAPH" && newEntry.type !== "TIME_GRAPH_DYNAMIC" && newEntry.type !== "PIE_GRAPH"}'
                }
            }, {
                // ALL GRAPHS
                xtype: 'textfield',
                anchor: '60%',
                fieldLabel: 'Series Renderer'.t(),
                hidden: true,
                bind: {
                    value: '{newEntry.seriesRenderer}',
                    hidden: '{newEntry.type !== "TIME_GRAPH" && newEntry.type !== "TIME_GRAPH_DYNAMIC" && newEntry.type !== "PIE_GRAPH"}'
                }
            }]
        }, {
            title: '<i class="fa fa-filter"></i> ' + 'SQL Conditions'.t(),
            region: 'south',
            split: true,
            collapsible: true,
            collapsed: true,
            titleCollapse: true,
            animCollapse: false,
            height: 200
        }],

        buttons: [
            { text: 'Preview'.t(), iconCls: 'fa fa-eye' },
            { text: 'Save New Report'.t(), iconCls: 'fa fa-floppy-o' }
        ]
    }]
});
