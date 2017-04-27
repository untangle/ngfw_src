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
                xtype: 'textfield',
                reference: 'filterfield',
                fieldLabel: 'Filter'.t(),
                emptyText: 'Filter data ...',
                labelWidth: 'auto',
                enableKeyEvents: true,
                triggers: {
                    clear: {
                        cls: 'x-form-clear-trigger',
                        hidden: true,
                        handler: 'onFilterEventClear'
                    }
                },
                listeners: {
                    change: 'filterEventList',
                    buffer: 100
                }
            }]
        }, {
            xtype: 'toolbar',
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
                xtype: 'label',
                margin: '0 5',
                text: 'From'.t() + ':'
            }, {
                xtype: 'datefield',
                format: 'date_fmt'.t(),
                editable: false,
                width: 100,
                bind: {
                    value: '{_sd}',
                    maxValue: '{_ed}'
                }
            }, {
                xtype: 'timefield',
                increment: 10,
                // format: 'date_fmt'.t(),
                editable: false,
                width: 80,
                bind: {
                    value: '{_st}',
                    // maxValue: '{_ed}'
                }
            }, {
                xtype: 'label',
                margin: '0 5',
                text: 'till'
            }, {
                xtype: 'checkbox',
                boxLabel: 'Present'.t(),
                bind: '{tillNow}'
            }, {
                xtype: 'datefield',
                format: 'date_fmt'.t(),
                editable: false,
                width: 100,
                hidden: true,
                bind: {
                    value: '{_ed}',
                    hidden: '{tillNow}',
                    minValue: '{_sd}'
                },
                maxValue: new Date(Math.floor(rpc.systemManager.getMilliseconds()))
            }, {
                xtype: 'timefield',
                increment: 10,
                // format: 'date_fmt'.t(),
                editable: false,
                width: 80,
                hidden: true,
                bind: {
                    value: '{_et}',
                    hidden: '{tillNow}',
                    // minValue: '{_sd}'
                },
                // maxValue: new Date(Math.floor(rpc.systemManager.getMilliseconds()))
            }, '->', {
                text: 'Refresh'.t(),
                iconCls: 'fa fa-refresh',
                itemId: 'refreshBtn',
                handler: 'refreshData'
            }, '-', {
                text: 'Reset View'.t(),
                iconCls: 'fa fa-refresh',
                itemId: 'resetBtn',
                handler: 'resetView',
                bind: {
                    hidden: '{entry.type !== "EVENT_LIST"}'
                }
            }, '-', {
                itemId: 'exportBtn',
                text: 'Export'.t(),
                iconCls: 'fa fa-sign-out',
                handler: 'exportEventsHandler',
                hidden: true,
                bind: {
                    hidden: '{entry.type !== "EVENT_LIST"}'
                }
            }, {
                itemId: 'downloadBtn',
                text: 'Download'.t(),
                iconCls: 'fa fa-download',
                handler: 'downloadGraph',
                hidden: true,
                bind: {
                    hidden: '{!isGraphEntry}'
                }
            }, '-', {
                itemId: 'dashboardBtn',
                bind: {
                    iconCls: 'fa {widget ? "fa-minus-circle" : "fa-plus-circle" }',
                    text: '{widget ? "Remove from " : "Add to "}' + ' Dashboard'
                },
                handler: 'dashboardAddRemove'
            }]
        }],
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

            items: [{
                xtype: 'component',
                padding: 10,
                margin: '0 0 10 0',
                style: { background: '#EEE' },
                html: '<i class="fa fa-info-circle fa-lg"></i> This is a default report. Any changes can be saved only by creating a new Report.',
                hidden: true,
                bind: {
                    hidden: '{!entry.readOnly}'
                }
            }, {
                xtype: 'textfield',
                fieldLabel: '<strong>' + 'Title'.t() + '</strong>',
                labelAlign: 'right',
                bind: '{entry.title}',
                anchor: '100%'
            }, {
                xtype: 'textarea',
                grow: true,
                fieldLabel: '<strong>' + 'Description'.t() + '</strong>',
                labelAlign: 'right',
                bind: '{entry.description}',
                anchor: '100%'
            }, {
                xtype: 'fieldset',
                title: '<i class="fa fa-paint-brush"></i> ' + 'Style'.t(),
                padding: 10,
                collapsible: true,
                defaults: {
                    labelWidth: 150,
                    labelAlign: 'right'
                },
                hidden: true,
                bind: {
                    hidden: '{!isGraphEntry}'
                },
                items: [{
                    // TIME_GRAPH - chart style
                    xtype: 'combo',
                    fieldLabel: 'Time Chart Style'.t(),
                    anchor: '100%',
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
                        value: '{entry.timeStyle}',
                        hidden: '{!(isTimeGraph || isTimeGraphDynamic)}'
                    },
                }, {
                    // TIME_GRAPH - data interval
                    xtype: 'combo',
                    fieldLabel: 'Time Data Interval'.t(),
                    anchor: '100%',
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
                        value: '{entry.timeDataInterval}',
                        hidden: '{!(isTimeGraph || isTimeGraphDynamic)}'
                    },
                }, {
                    // TIME_GRAPH - data grouping approximation
                    xtype: 'combo',
                    fieldLabel: 'Approximation'.t(),
                    anchor: '100%',
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
                        value: '{_approximation}',
                        hidden: '{!(isTimeGraph || isTimeGraphDynamic)}'
                    },
                }, {
                    // PIE_GRAPH - chart style
                    xtype: 'combo',
                    fieldLabel: 'Style'.t(),
                    anchor: '100%',
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
                        value: '{entry.pieStyle}',
                        disabled: '{!isPieGraph}',
                        hidden: '{!isPieGraph}'
                    },
                }, {
                    // PIE_GRAPH - number of pie slices
                    xtype: 'numberfield',
                    fieldLabel: 'Pie Slices Number'.t(),
                    labelWidth: 150,
                    width: 210,
                    labelAlign: 'right',
                    minValue: 1,
                    maxValue: 25,
                    allowBlank: false,
                    hidden: true,
                    bind: {
                        value: '{entry.pieNumSlices}',
                        disabled: '{!isPieGraph}',
                        hidden: '{!isPieGraph}'
                    }
                }, {
                    xtype: 'textarea',
                    anchor: '100%',
                    fieldLabel: 'Colors'.t() + ' (comma sep.)',
                    bind: '{_colorsStr}'
                }]
                // {
                //     xtype: 'checkbox',
                //     reference: 'defaultColors',
                //     fieldLabel: 'Colors'.t(),
                //     boxLabel: 'Default'.t(),
                //     bind: '{_defaultColors}'
                // }, {
                //     xtype: 'container',
                //     margin: '0 0 0 155',
                //     itemId: 'colors',
                //     hidden: true,
                //     bind: {
                //         hidden: '{defaultColors.checked}'
                //     }
                // }]
            }, {
                xtype: 'fieldset',
                title: '<i class="fa fa-sliders"></i> ' + 'Advanced'.t(),
                padding: 10,
                collapsible: true,
                collapsed: true,
                defaults: {
                    labelWidth: 150,
                    labelAlign: 'right'
                },
                items: [{
                    // ALL graphs
                    xtype: 'textfield',
                    fieldLabel: 'Units'.t(),
                    anchor: '100%',
                    hidden: true,
                    bind: {
                        value: '{entry.units}',
                        hidden: '{!isGraphEntry}'
                    }
                }, {
                    // ALL entries
                    xtype: 'combo',
                    fieldLabel: 'Table'.t(),
                    anchor: '100%',
                    bind: '{entry.table}',
                    editable: false,
                    queryMode: 'local'
                }, {
                    // TIME_GRAPH only
                    xtype: 'textarea',
                    anchor: '100%',
                    fieldLabel: 'Time Data Columns'.t(),
                    grow: true,
                    hidden: true,
                    bind: {
                        value: '{entry.timeDataColumns}',
                        hidden: '{!isTimeGraph}'
                    }
                }, {
                    xtype: 'component',
                    style: {
                        borderTop: '1px #CCC solid',
                        margin: '15px 0'
                    },
                    autoEl: { tag: 'hr' },
                    hidden: true,
                    bind: { hidden: '{!isTimeGraphDynamic}' }
                }, {
                    // TIME_GRAPH_DYNAMIC only
                    xtype: 'textfield',
                    anchor: '100%',
                    fieldLabel: 'Time Data Dynamic Value'.t(),
                    labelWidth: 200,
                    hidden: true,
                    bind: {
                        value: '{entry.timeDataDynamicValue}',
                        hidden: '{!isTimeGraphDynamic}'
                    }
                }, {
                    // TIME_GRAPH_DYNAMIC only
                    xtype: 'textfield',
                    anchor: '100%',
                    fieldLabel: 'Time Data Dynamic Column'.t(),
                    labelWidth: 200,
                    hidden: true,
                    bind: {
                        value: '{entry.timeDataDynamicColumn}',
                        hidden: '{!isTimeGraphDynamic}'
                    }
                }, {
                    // TIME_GRAPH_DYNAMIC only
                    xtype: 'numberfield',
                    anchor: '100%',
                    fieldLabel: 'Time Data Dynamic Limit'.t(),
                    labelWidth: 200,
                    hidden: true,
                    bind: {
                        value: '{entry.timeDataDynamicLimit}',
                        hidden: '{!isTimeGraphDynamic}'
                    }
                }, {
                    // TIME_GRAPH_DYNAMIC only
                    xtype: 'combo',
                    anchor: '100%',
                    fieldLabel: 'Time Data Aggregation Function'.t(),
                    labelWidth: 200,
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
                        value: '{entry.timeDataDynamicAggregationFunction}',
                        hidden: '{!isTimeGraphDynamic}'
                    }
                }, {
                    // TIME_GRAPH_DYNAMIC only
                    xtype: 'checkbox',
                    fieldLabel: 'Time Data Dynamic Allow Null'.t(),
                    labelWidth: 200,
                    hidden: true,
                    bind: {
                        value: '{entry.timeDataDynamicAllowNull}',
                        hidden: '{!isTimeGraphDynamic}'
                    }
                }, {
                    xtype: 'component',
                    style: {
                        borderTop: '1px #CCC solid',
                        margin: '15px 0'
                    },
                    autoEl: { tag: 'hr' },
                    hidden: true,
                    bind: { hidden: '{!isTimeGraphDynamic}' }
                }, {
                    // GRAPH ENTRY
                    xtype: 'textfield',
                    anchor: '100%',
                    fieldLabel: 'Series Renderer'.t(),
                    hidden: true,
                    bind: {
                        value: '{entry.seriesRenderer}',
                        hidden: '{!isGraphEntry}'
                    }
                }, {
                    // PIE_GRAPH only
                    xtype: 'textfield',
                    anchor: '100%',
                    fieldLabel: 'Pie Group Column'.t(),
                    hidden: true,
                    bind: {
                        value: '{entry.pieGroupColumn}',
                        hidden: '{!isPieGraph}'
                    }
                }, {
                    // PIE_GRAPH only
                    xtype: 'textfield',
                    anchor: '100%',
                    fieldLabel: 'Pie Sum Column'.t(),
                    hidden: true,
                    bind: {
                        value: '{entry.pieSumColumn}',
                        hidden: '{!isPieGraph}'
                    }
                }, {
                    // ALL graphs
                    xtype: 'textfield',
                    anchor: '100%',
                    fieldLabel: 'Order By Column'.t(),
                    hidden: true,
                    bind: {
                        value: '{entry.orderByColumn}',
                        hidden: '{!isGraphEntry}'
                    }
                }, {
                    // ALL graphs
                    xtype: 'segmentedbutton',
                    margin: '0 0 5 155',
                    items: [
                        { text: 'Ascending'.t(), iconCls: 'fa fa-sort-amount-asc', value: true },
                        { text: 'Descending'.t(), iconCls: 'fa fa-sort-amount-desc' , value: false }
                    ],
                    hidden: true,
                    bind: {
                        value: '{entry.orderDesc}',
                        hidden: '{!isGraphEntry}'
                    }
                }, {
                    // TEXT entries
                    xtype: 'textarea',
                    anchor: '100%',
                    fieldLabel: 'Text Columns'.t(),
                    grow: true,
                    hidden: true,
                    bind: {
                        value: '{entry.textColumns}',
                        hidden: '{!isTextEntry}'
                    }
                }, {
                    // TEXT entries
                    xtype: 'textfield',
                    anchor: '100%',
                    fieldLabel: 'Text String'.t(),
                    hidden: true,
                    bind: {
                        value: '{entry.textString}',
                        hidden: '{!isTextEntry}'
                    }
                }, {
                    // ALL entries - display order
                    xtype: 'numberfield',
                    fieldLabel: 'Display Order'.t(),
                    anchor: '70%',
                    bind: '{entry.displayOrder}'
                }]
            }, {
                // columns for EVENT_LIST
                xtype: 'fieldset',
                title: '<i class="fa fa-columns"></i> ' + 'Columns'.t(),
                maxHeight: 200,
                scrollable: 'y',
                collapsible: true,
                items: [{
                    xtype: 'checkboxgroup',
                    itemId: 'tableColumns',
                    columns: 1,
                    vertical: true,
                    items: [],
                    listeners: {
                        change: 'updateDefaultColumns'
                    }
                }],
                hidden: true,
                bind: {
                    hidden: '{entry.type !== "EVENT_LIST"}'
                }
            }, {
                // SQL CONDITIONS
                xtype: 'fieldset',
                bind: {
                    title: '{_sqlTitle}',
                    collapsed: '{!entry.conditions}'
                },
                padding: 10,
                collapsible: true,
                collapsed: true,
                items: [{
                    xtype: 'grid',
                    itemId: 'sqlConditions',
                    trackMouseOver: false,
                    sortableColumns: false,
                    enableColumnResize: false,
                    enableColumnMove: false,
                    enableColumnHide: false,
                    // hideHeaders: true,
                    disableSelection: true,
                    viewConfig: {
                        emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-lg"></i> No Conditions!</p>',
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
            }],
            bbar: ['->', {
                text: 'Remove'.t(),
                iconCls: 'fa fa-minus-circle',
                disabled: true,
                bind: {
                    disabled: '{entry.readOnly}'
                }
            }, {
                text: 'Save'.t(),
                iconCls: 'fa fa-save',
                // formBind: true,
                disabled: true,
                bind: {
                    disabled: '{entry.readOnly}'
                },
                handler: 'updateReport'
            }, {
                text: 'Save as New Report'.t(),
                iconCls: 'fa fa-plus-circle',
                itemId: 'saveNewBtn',
                handler: 'saveNewReport'
                // formBind: true
            }]
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

        trackMouseOver: false,
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
            emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-lg"></i> No Filters!</p>',
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
