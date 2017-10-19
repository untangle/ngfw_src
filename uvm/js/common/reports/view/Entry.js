Ext.define('Ung.view.reports.Entry', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.entry',

    controller: 'entry',
    viewModel: {
        type: 'entry'
    },

    layout: 'fit',

    items: [{
        /**
         * using a formpanel to help validate when editing entry settings
         */
        xtype: 'form',
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
            // using 2 headings, 1 for selected entry, other for editing entry
            items: [{
                xtype: 'component',
                hidden: true,
                bind: {
                    html: '<h1><span>{entry.category} /</span> {entry.title}</h1>',
                    hidden: '{eEntry}'
                }
            }, {
                xtype: 'component',
                hidden: true,
                bind: {
                    html: '<h1><span>{eEntry.category || "category"} /</span> {eEntry.title || "title"}</h1>',
                    hidden: '{!eEntry}'
                }
            }]
        }, {
            /**
             * top toolbar containing report entry available actions
             */
            xtype: 'toolbar',
            dock: 'top',
            ui: 'footer',
            items: [{
                xtype: 'component',
                hidden: true,
                bind: {
                    html: '{entry.description}',
                    hidden: '{eEntry}'
                }
            }, {
                xtype: 'component',
                hidden: true,
                bind: {
                    html: '{eEntry.description}',
                    hidden: '{!eEntry}'
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
                reference: 'dataCk',
                hidden: true,
                disabled: true,
                bind: {
                    hidden: '{!entry || entry.type === "EVENT_LIST" || eEntry}',
                    disabled: '{fetching}'
                }
            }, {
                text: 'Reset View'.t(),
                iconCls: 'fa fa-refresh',
                itemId: 'resetBtn',
                handler: 'resetView',
                hidden: true,
                disabled: true,
                bind: {
                    hidden: '{!entry || entry.type !== "EVENT_LIST" || eEntry}',
                    disabled: '{fetching}'
                }
            }, {
                itemId: 'downloadBtn',
                text: 'Download Graph'.t(),
                iconCls: 'fa fa-download',
                handler: 'downloadGraph',
                hidden: true,
                disabled: true,
                bind: {
                    hidden: '{!isGraphEntry || eEntry}',
                    disabled: '{fetching}'
                }
            }, {
                itemId: 'dashboardBtn',
                hidden: true,
                disabled: true,
                bind: {
                    iconCls: 'fa {widget ? "fa-minus-circle" : "fa-plus-circle" }',
                    text: '{widget ? "Remove from " : "Add to "}' + ' Dashboard',
                    hidden: '{context !== "admin" || eEntry}',
                    disabled: '{fetching}'
                },
                handler: 'dashboardAddRemove'
            }, {
                itemId: 'exportBtn',
                text: 'Export'.t(),
                iconCls: 'fa fa-external-link-square',
                handler: 'exportEventsHandler',
                hidden: true,
                disabled: true,
                bind: {
                    hidden: '{entry.type !== "EVENT_LIST" || eEntry}',
                    disabled: '{fetching}'
                }
            }, {
                text: 'Settings'.t(),
                reference: 'settingsBtn',
                // enableToggle: true,
                iconCls: 'fa fa-cog',
                hidden: true,
                disabled: true,
                bind: {
                    hidden: '{!entry || eEntry}',
                    disabled: '{fetching}'
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
             * cards region containing the actual report (text, graph, events, invalid report message)
             */
            region: 'center',
            border: false,
            itemId: 'reportCard',
            layout: 'card',
            bind: {
                activeItem: '{activeReportCard}'
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
            }, {
                itemId: 'invalidreport',
                border: false,
                layout: 'center',
                items: [{
                    xtype: 'component',
                    html: '<div style="text-align: center; font-size: 14px;"><i class="fa fa-exclamation-triangle fa-2x fa-orange"></i><p>Fill all the required settings then click "Preview/Refresh"!</p></div>'
                }]
            }],

            dockedItems: [{
                /**
                 * Notification for readonly or custom report
                 */
                xtype: 'component',
                dock: 'top',
                padding: '10',
                hidden: true,
                style: {
                    background: '#F9FFA8'
                },
                bind: {
                    hidden: '{!eEntry.readOnly}'
                },
                html: '<i class="fa fa-info-circle fa-lg"></i> ' + 'This is a default <strong>read-only</strong> report. Any changes can be saved as a New Report with a different title.'
            }, {
                /**
                 * Date Range / Refresh toolbar
                 * visible only when viewing reports but not when editing them
                 */
                xtype: 'toolbar',
                itemId: 'actionsToolbar',
                ui: 'footer',
                dock: 'bottom',
                style: {
                    background: '#F5F5F5'
                },
                hidden: true,
                bind: {
                    hidden: '{!entry || eEntry}'
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
                        [100, '100 ' + 'Events'.t()],
                        [500, '500 ' + 'Events'.t()],
                        [1000, '1000 ' + 'Events'.t()],
                        [5000, '5000 ' + 'Events'.t()],
                        [10000, '10000 ' + 'Events'.t()],
                        [50000, '50000 ' + 'Events'.t()],
                    ],
                    queryMode: 'local',
                    listeners: {
                        change: 'refreshData'
                    }
                }, {
                    xtype: 'component',
                    html: 'Since'.t() + ':'
                }, {
                    // range till now in hours
                    xtype: 'combo',
                    reference: 'sinceDate',
                    publishes: 'value',
                    // fieldLabel: 'Since'.t(),
                    // labelWidth: 'auto',
                    width: 100,
                    margin: '0 10 0 0',
                    value: 24,
                    editable: false,
                    store: [
                        [1, '1 hour'.t()],
                        [3, '3 hours'.t()],
                        [6, '6 hours'.t()],
                        [12, '12 hours'.t()],
                        [24, '1 day'.t()],
                        [24 * 3, '3 days'.t()],
                        [24 * 7, '1 week'.t()],
                        [24 * 14, '2 weeks'.t()],
                        [24 * 30, '1 month'.t()]
                    ],
                    queryMode: 'local',
                    disabled: true,
                    bind: {
                        disabled: '{customRange.value || fetching}'
                    }
                }, {
                    xtype: 'checkbox',
                    reference: 'customRange',
                    publishes: 'value',
                    boxLabel: 'Date Range'.t(),
                    disabled: true,
                    bind: {
                        disabled: '{fetching}'
                    }
                }, {
                    xtype: 'daterange',
                    hidden: true,
                    disabled: true,
                    bind: {
                        startDate: '{f_startdate}',
                        endDate: '{f_enddate}',
                        hidden: '{!customRange.value}',
                        disabled: '{fetching}'
                    }
                }, {
                    xtype: 'segmentedbutton',
                    allowToggle: false,
                    items: [{
                        text: 'Refresh'.t(),
                        iconCls: 'fa fa-refresh',
                        itemId: 'refreshBtn',
                        handler: 'refreshData',
                        disabled: true,
                        bind: {
                            disabled: '{autoRefresh || fetching}'
                        }
                    }, {
                        text: 'Auto'.t(),
                        enableToggle: true,
                        bind: {
                            iconCls: '{autoRefresh ? "fa fa-check-square-o" : "fa fa-square-o"}',
                        },
                        handler: 'setAutoRefresh'
                    }],
                    hidden: true,
                    bind: {
                        hidden: '{eEntry}'
                    }
                }]
            }]
        }, {
            /**
             * east border ragion with entry editing fields
             */
            region: 'east',
            width: 350,
            weight: 20,
            split: true,
            layout: {
                type: 'vbox',
                align: 'stretch'
            },
            scrollable: true,
            hidden: true,
            bind: {
                hidden: '{!eEntry}'
            },
            defaults: {
                bodyBorder: false,
                bodyPadding: 10
            },
            items: [{
                // generic report properties
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
                    },
                    valuePublishEvent: 'blur', // update binding on blur only
                    allowBlank: false,
                    emptyText: 'Enter Report Title ...'.t(),
                    validator: function (title) {
                        var vm = this.up('entry').getViewModel();
                        if (vm.get('eEntry.readOnly')) {
                            if (Ext.getStore('reports').find('title', title.trim(), 0, false, false, true) > 0) {
                                return 'Choose a unique report title!'.t();
                            }
                            return true;
                        }
                        return true;
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
                    allowBlank: false,
                    emptyText: 'Select a Category ...'.t(),
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
                    // allowBlank: false,
                    emptyText: 'Add a description ...'.t(),
                    valuePublishEvent: 'blur',
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
                    allowBlank: false,
                    emptyText: 'Select Report Type ...'.t(),
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
                }, {
                    xtype: 'numberfield',
                    fieldLabel: 'Display Order'.t(),
                    width: 100,
                    bind: '{eEntry.displayOrder}',
                    hidden: false,
                    disabled: false
                }]
            }, {
                // style properties
                xtype: 'panel',
                title: '<i class="fa fa-paint-brush"></i> ' + 'Graph/View Options'.t(),
                // flex: 1,
                layout: 'anchor',
                border: false,
                defaults: {
                    labelAlign: 'top',
                    anchor: '100%',
                    hidden: true,
                    disabled: true
                },
                hidden: true,
                bind: {
                    hidden: '{eEntry.type === "TEXT" || eEntry.type === "EVENT_LIST"}'
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
                    bind: {
                        value: '{eEntry.pieStyle}',
                        hidden: '{eEntry.type !== "PIE_GRAPH"}',
                        disabled: '{eEntry.type !== "PIE_GRAPH"}'
                    },
                    allowBlank: false,
                    emptyText: 'Select Style ...'.t()
                }, {
                    // PIE_GRAPH
                    xtype: 'fieldcontainer',
                    fieldLabel: 'Pie Slices Number'.t(),
                    labelAlign: 'left',
                    layout: {
                        type: 'hbox',
                        align: 'middle'
                    },
                    items: [{
                        xtype: 'slider',
                        flex: 1,
                        reference: 'pieNumSlices',
                        minValue: 1,
                        maxValue: 25,
                        increment: 1,
                        publishes: 'value',
                        publishOnComplete: false,
                        bind: {
                            value: '{eEntry.pieNumSlices}',
                        },
                        tipText: function (thumb) {
                            return String(thumb.value) + ' ' + 'slices'.t();
                        }
                    }, {
                        xtype: 'component',
                        width: 100,
                        margin: '0 0 0 10',
                        bind: {
                            html: '<strong>{pieNumSlices.value} ' + 'slices'.t() + '</strong>'
                        }
                    }],
                    bind: {
                        hidden: '{eEntry.type !== "PIE_GRAPH"}',
                        disabled: '{eEntry.type !== "PIE_GRAPH"}'
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
                    bind: {
                        value: '{eEntry.timeStyle}',
                        hidden: '{eEntry.type !== "TIME_GRAPH" && eEntry.type !== "TIME_GRAPH_DYNAMIC"}',
                        disabled: '{eEntry.type !== "TIME_GRAPH" && eEntry.type !== "TIME_GRAPH_DYNAMIC"}'
                    },
                    allowBlank: false,
                    emptyText: 'Select Style ...'.t()
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
                    bind: {
                        value: '{eEntry.timeDataInterval}',
                        hidden: '{eEntry.type !== "TIME_GRAPH" && eEntry.type !== "TIME_GRAPH_DYNAMIC"}',
                        disabled: '{eEntry.type !== "TIME_GRAPH" && eEntry.type !== "TIME_GRAPH_DYNAMIC"}'
                    },
                    allowBlank: false,
                    emptyText: 'Select the Time Interval ...'.t()
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
                    bind: {
                        value: '{f_approximation}',
                        hidden: '{eEntry.type !== "TIME_GRAPH" && eEntry.type !== "TIME_GRAPH_DYNAMIC"}',
                        disabled: '{eEntry.type !== "TIME_GRAPH" && eEntry.type !== "TIME_GRAPH_DYNAMIC"}'
                    },
                    allowBlank: false,
                    emptyText: 'Select Approximation ...'.t()
                }, {
                    // PIE_GRAPH, TIME_GRAPH, TIME_GRAPH_DYNAMIC
                    xtype: 'textarea',
                    fieldLabel: 'Custom Colors'.t() + ' (comma separated)',
                    bind: {
                        value: '{_colorsStr}',
                        hidden: '{eEntry.type !== "PIE_GRAPH" && eEntry.type !== "TIME_GRAPH" && eEntry.type !== "TIME_GRAPH_DYNAMIC"}',
                        disabled: '{eEntry.type !== "PIE_GRAPH" && eEntry.type !== "TIME_GRAPH" && eEntry.type !== "TIME_GRAPH_DYNAMIC"}'
                    },
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
                    scale: 'medium',
                    handler: 'previewReport',
                    formBind: true
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
                        formBind: true,
                        hidden: true,
                        bind: {
                            hidden: '{eEntry.readOnly}'
                        }
                    }, {
                        text: 'Save as New'.t(),
                        iconCls: 'fa fa-floppy-o fa-lg',
                        formBind: true,
                        hidden: true,
                        bind: {
                            hidden: '{!eEntry.uniqueId}' // hide if creating New Report
                        }
                    }]
                }]
            }]
        }, {
            /**
             * south border region width entry data source and sql conditions etc...
             */
            region: 'south',
            height: 300,
            split: true,
            layout: 'border',
            hidden: true,
            bind: {
                hidden: '{!eEntry}'
            },
            defaults: {
                border: false
            },
            items: [{
                region: 'west',
                itemId: 'tableColumns',
                xtype: 'grid',
                title: '<i class="fa fa-database"></i> ' + 'Data Source'.t(),
                width: '40%',
                split: true,
                sortableColumns: false,
                disableSelection: true,
                enableColumnHide: false,
                columnLines: true,
                viewConfig: {
                    enableTextSelection: true
                },
                // selType: 'checkboxmodel',
                bind: {
                    store: {
                        data: '{tableColumns}',
                        listeners: {
                            update: function (grid, column) {
                                Ext.fireEvent('defaultcolumnschange', column);
                            }
                        }
                    }
                },
                columns: [{
                    xtype: 'checkcolumn',
                    header: '',
                    dataIndex: 'isDefault',
                    width: 25,
                    hidden: true,
                    bind: {
                        hidden: '{eEntry.type !== "EVENT_LIST"}'
                    }
                }, {
                    header: 'Column Name'.t(),
                    dataIndex: 'text',
                    width: 250
                }, {
                    header: 'Column Id'.t(),
                    dataIndex: 'value',
                    flex: 1,
                    renderer: function (val) { return '<strong>' + val + '</strong>'; }
                }],
                dockedItems: [{
                    // ALL
                    xtype: 'toolbar',
                    dock: 'top',
                    padding: 5,
                    // ui: 'footer',
                    border: '1 1 0 1',
                    items: [{
                        xtype: 'combo',
                        flex: 1,
                        fieldLabel: 'Data Table'.t(),
                        fieldStyle: 'font-weight: bold',
                        labelAlign: 'right',
                        bind: {
                            value: '{eEntry.table}',
                            store: '{tables}'
                        },
                        editable: false,
                        queryMode: 'local',
                        allowBlank: false,
                        emptyText: 'Select a Table for Report data ...'.t()
                    }]
                }, {
                    xtype: 'toolbar',
                    dock: 'bottom',
                    padding: 5,
                    items: [{
                        xtype: 'component',
                        html: ' <i class="fa fa-level-down fa-rotate-180 fa-lg"></i> ' + 'Select the default columns to show in report'.t()
                    }],
                    hidden: true,
                    bind: {
                        hidden: '{eEntry.type !== "EVENT_LIST"}'
                    }
                }]
            }, {
                region: 'center',
                xtype: 'tabpanel',
                items: [{
                    title: '<i class="fa fa-cogs"></i> ' + 'Data Settings'.t(),
                    layout: {
                        type: 'anchor'
                    },
                    scrollable: true,
                    defaults: {
                        labelWidth: 200,
                        labelAlign: 'right',
                        disabled: true,
                        hidden: true
                    },
                    items: [{
                        // used for validating text columns
                        xtype: 'textfield',
                        bind: {
                            value: '{textColumnsCount}',
                            disabled: '{eEntry.type !== "TEXT"}'
                        },
                        validator: function(val) {
                            return val > 0;
                        }
                    }, {
                        // TEXT
                        xtype: 'grid',
                        itemId: 'textColumnsGrid',
                        sortableColumns: false,
                        enableColumnResize: false,
                        enableColumnMove: false,
                        enableColumnHide: false,
                        disableSelection: true,
                        // minHeight: 120,
                        margin: '0 0 20 0',
                        hideHeaders: true,
                        border: false,
                        viewConfig: {
                            emptyText: '<p style="text-align: center; margin: 0; line-height: 2; font-size: 12px; color: red;"><i class="fa fa-exclamation-triangle fa-lg fa-orange"></i><br/>' + 'Text Columns are required for the report!'.t() + '</p>',
                        },
                        tbar: [{
                            xtype: 'component',
                            html: '<strong>' + 'Text Data Columns'.t() + '</strong>',
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
                        disabled: false,
                        bind: {
                            store: '{textColumnsStore}',
                            hidden: '{eEntry.type !== "TEXT"}'
                        },
                        columns: [{
                            xtype: 'rownumberer',
                            // title: 'Value Mask'.t(),
                            width: 40,
                            renderer: function (value, metaData, record, rowIdx) { return '{' + rowIdx + '}'; }
                        }, {
                            dataIndex: 'str',
                            flex: 1,
                            editor: 'textfield',
                            renderer: function (val) {
                                return val ? '<strong>' + val + '</strong>' : '<em>Click to insert column value ...</em>';
                            }
                        }, {
                            xtype: 'actioncolumn',
                            width: 40,
                            align: 'center',
                            resizable: false,
                            tdCls: 'action-cell',
                            iconCls: 'fa fa-times',
                            // handler: function (view, rowIndex, colIndex, item, e, record) {
                            //     record.drop();
                            // },
                            handler: 'removeTextColumn',
                            menuDisabled: true,
                            hideable: false
                        }]
                    }, {
                        // TEXT
                        xtype: 'textarea',
                        anchor: '100%',
                        fieldLabel: 'Text String'.t(),
                        labelAlign: 'top',
                        margin: 10,
                        bind: {
                            value: '{eEntry.textString}',
                            hidden: '{eEntry.type !== "TEXT"}',
                            disabled: '{eEntry.type !== "TEXT"}'
                        },
                        allowBlank: false
                    }, {
                        // PIE_GRAPH
                        xtype: 'combo',
                        fieldLabel: 'Pie Group Column'.t(),
                        margin: '5 0',
                        typeAhead: true,
                        hideTrigger: true,
                        anchor: '70%',
                        displayField: 'value',
                        valueField: 'value',
                        bind: {
                            store: { data: '{tableColumns}' },
                            value: '{eEntry.pieGroupColumn}',
                            hidden: '{eEntry.type !== "PIE_GRAPH"}',
                            disabled: '{eEntry.type !== "PIE_GRAPH"}'
                        },
                        queryMode: 'local',
                        allowBlank: false,
                        emptyText: 'Enter a Column Id or a custom value ...'
                    }, {
                        // PIE_GRAPH
                        xtype: 'combo',
                        fieldLabel: 'Pie Sum Column'.t(),
                        margin: '5 0',
                        typeAhead: true,
                        hideTrigger: true,
                        anchor: '70%',
                        displayField: 'value',
                        valueField: 'value',
                        bind: {
                            store: { data: '{tableColumns}' },
                            value: '{eEntry.pieSumColumn}',
                            hidden: '{eEntry.type !== "PIE_GRAPH"}',
                            disabled: '{eEntry.type !== "PIE_GRAPH"}'
                        },
                        queryMode: 'local',
                        allowBlank: false,
                        emptyText: 'Enter a Column Id or a custom value ( e.g. count(*)) ...'
                    }, {
                        xtype: 'textfield',
                        bind: {
                            value: '{timeDataColumnsCount}',
                            disabled: '{eEntry.type !== "TIME_GRAPH"}'
                        },
                        validator: function(val) {
                            return val > 0;
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
                        // minHeight: 120,
                        margin: '0 0 20 0',
                        border: false,
                        viewConfig: {
                            emptyText: '<p style="text-align: center; margin: 0; line-height: 2; font-size: 12px; color: red;"><i class="fa fa-exclamation-triangle fa-lg fa-orange"></i><br/>' + 'Time Data Columns are required for the graph!'.t() + '</p>',
                        },
                        tbar: [{
                            xtype: 'component',
                            html: '<strong>' + 'Time Data Columns'.t() + '</strong>',
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
                        disabled: false,
                        bind: {
                            store: '{timeDataColumnsStore}',
                            hidden: '{eEntry.type !== "TIME_GRAPH"}'
                        },
                        columns: [{
                            dataIndex: 'str',
                            flex: 1,
                            editor: 'textfield',
                            renderer: function (val) {
                                return val ? '<strong>' + val + '</strong>' : '<em>Click to insert column value ...</em>';
                            }
                        }, {
                            xtype: 'actioncolumn',
                            width: 40,
                            align: 'center',
                            resizable: false,
                            tdCls: 'action-cell',
                            iconCls: 'fa fa-times',
                            handler: 'removeTimeDataColumn',
                            // handler: function (view, rowIndex, colIndex, item, e, record) {
                            //     record.drop();
                            //     record.commit();
                            // },
                            menuDisabled: true,
                            hideable: false
                        }]
                    }, {
                        // TIME_GRAPH_DYNAMIC
                        xtype: 'combo',
                        fieldLabel: 'Time Data Dynamic Column'.t(),
                        margin: '5 0',
                        anchor: '70%',
                        publishes: 'value',
                        allowBlank: false,
                        emptyText: 'Select Column ...'.t(),
                        editable: false,
                        queryMode: 'local',
                        displayField: 'value',
                        valueField: 'value',
                        bind: {
                            store: { data: '{tableColumns}' },
                            value: '{eEntry.timeDataDynamicColumn}',
                            hidden: '{eEntry.type !== "TIME_GRAPH_DYNAMIC"}',
                            disabled: '{eEntry.type !== "TIME_GRAPH_DYNAMIC"}'
                        },
                        displayTpl: '<tpl for=".">{text} [{value}]</tpl>',
                        listConfig: {
                            itemTpl: ['<div data-qtip="{value}"><strong>{text}</strong> <span style="float: right;">[{value}]</span></div>']
                        }
                    }, {
                        // TIME_GRAPH_DYNAMIC
                        xtype: 'combo',
                        fieldLabel: 'Time Data Dynamic Value'.t(),
                        margin: '5 0',
                        typeAhead: true,
                        hideTrigger: true,
                        anchor: '70%',
                        displayField: 'value',
                        valueField: 'value',
                        bind: {
                            store: { data: '{tableColumns}' },
                            value: '{eEntry.timeDataDynamicValue}',
                            hidden: '{eEntry.type !== "TIME_GRAPH_DYNAMIC"}',
                            disabled: '{eEntry.type !== "TIME_GRAPH_DYNAMIC"}'
                        },
                        queryMode: 'local',
                        allowBlank: false,
                        emptyText: 'Enter a Column Id or a custom value ...'
                    }, {
                        // TIME_GRAPH_DYNAMIC
                        xtype: 'fieldcontainer',
                        fieldLabel: 'Time Data Dynamic Limit'.t(),
                        layout: { type: 'hbox', align: 'middle' },
                        items: [{
                            xtype: 'numberfield',
                            width: 70,
                            allowBlank: false,
                            disabled: true,
                            bind: {
                                value: '{eEntry.timeDataDynamicLimit}',
                                disabled: '{eEntry.type !== "TIME_GRAPH_DYNAMIC"}'
                            }
                        }, {
                            xtype: 'component',
                            padding: '0 5',
                            html: '<i class="fa fa-info-circle fa-gray" data-qtip="e.g. 10"></i>'
                        }],
                        disabled: false,
                        bind: {
                            hidden: '{eEntry.type !== "TIME_GRAPH_DYNAMIC"}'
                        }
                    }, {
                        // TIME_GRAPH_DYNAMIC
                        xtype: 'combo',
                        fieldLabel: 'Time Data Aggregation Function'.t(),
                        anchor: '70%',
                        store: [
                            ['avg', 'Average'.t()],
                            ['sum', 'Sum'.t()],
                            ['min', 'Min'.t()],
                            ['max', 'Max'.t()]
                        ],
                        editable: false,
                        allowBlank: false,
                        emptyText: 'Select Aggregation Method ...'.t(),
                        queryMode: 'local',
                        bind: {
                            value: '{eEntry.timeDataDynamicAggregationFunction}',
                            hidden: '{eEntry.type !== "TIME_GRAPH_DYNAMIC"}',
                            disabled: '{eEntry.type !== "TIME_GRAPH_DYNAMIC"}'
                        }
                    }, {
                        // ALL GRAPHS
                        xtype: 'textfield',
                        fieldLabel: 'Units'.t(),
                        anchor: '70%',
                        allowBlank: false,
                        emptyText: 'Enter Units ... (e.g. sessions, bytes/s)'.t(),
                        bind: {
                            value: '{eEntry.units}',
                            hidden: '{eEntry.type !== "TIME_GRAPH" && eEntry.type !== "TIME_GRAPH_DYNAMIC" && eEntry.type !== "PIE_GRAPH"}',
                            disabled: '{eEntry.type !== "TIME_GRAPH" && eEntry.type !== "TIME_GRAPH_DYNAMIC" && eEntry.type !== "PIE_GRAPH"}'
                        }
                    }, {
                        // PIE_GRAPH
                        xtype: 'fieldcontainer',
                        fieldLabel: 'Order By Column'.t(),
                        anchor: '70%',
                        allowBlank: false,
                        bind: {
                            hidden: '{eEntry.type !== "PIE_GRAPH"}',
                            disabled: '{eEntry.type !== "PIE_GRAPH"}'
                        },
                        layout: { type: 'hbox', align: 'middle' },
                        items: [{
                            xtype: 'textfield',
                            flex: 1,
                            emptyText: 'Enter a Column Id or a custom value ...',
                            bind: {
                                value: '{eEntry.orderByColumn}'
                            }
                        }, {
                            xtype: 'segmentedbutton',
                            margin: '0 0 0 5',
                            items: [
                                { text: 'Asc'.t(), iconCls: 'fa fa-arrow-up', value: true },
                                { text: 'Desc'.t(), iconCls: 'fa fa-arrow-down' , value: false }
                            ],
                            bind: {
                                value: '{eEntry.orderDesc}'
                            }
                        }]
                    }, {
                        // ALL GRAPHS
                        xtype: 'combo',
                        anchor: '70%',
                        fieldLabel: 'Series Renderer'.t(),
                        store: Renderer.forReports,
                        editable: false,
                        queryMode: 'local',
                        bind: {
                            value: '{eEntry.seriesRenderer}',
                            hidden: '{eEntry.type !== "TIME_GRAPH" && eEntry.type !== "TIME_GRAPH_DYNAMIC" && eEntry.type !== "PIE_GRAPH"}',
                            disabled: '{eEntry.type !== "TIME_GRAPH" && eEntry.type !== "TIME_GRAPH_DYNAMIC" && eEntry.type !== "PIE_GRAPH"}'
                        }
                    }, {
                        // EVENT_LIST
                        xtype: 'component',
                        html: '<h1 style="text-align: center;">No Settings!</h1>',
                        hidden: true,
                        bind: {
                            hidden: '{eEntry.type !== "EVENT_LIST"}'
                        }
                    }]
                }, {
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
                    disabled: true,
                    bind: {
                        title: '{_sqlTitle}',
                        disabled: '{!tableColumns}',
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
                            fieldStyle: 'font-weight: bold',
                            bind: {
                                store: {
                                    data: '{tableColumns}'
                                }
                            },
                            displayTpl: '<tpl for=".">{text} [{value}]</tpl>',
                            listConfig: {
                                itemTpl: ['<div data-qtip="{value}">{text} <span style="font-weight: bold; float: right;">[{value}]</span></div>']
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
                        renderer: 'sqlColumnRenderer',
                        width: 300
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
                    }]
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
                hidden: '{!dataCk.checked || eEntry}',
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

    }]
});
