Ext.define('Ung.view.reports.Entry', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.reports-entry',

    controller: 'reports-entry',

    viewModel: {
        type: 'reports-entry'
    },

    border: false,
    bodyBorder: false,

    layout: 'border',

    dockedItems: [{
        xtype: 'container',
        dock: 'top',
        cls: 'report-header',
        // height: 60,
        bind: {
            html: '{reportHeading}'
        }
    }],

    items: [{
        region: 'center',
        border: false,
        itemId: 'chartHolder',
        layout: 'fit',
        items: [], // here the chart will be added
        bbar: [{
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
                maxValue: '{_ed}'
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
                minValue: '{_sd}'
            },
            maxValue: new Date(Math.floor(rpc.systemManager.getMilliseconds()))
        }, '-' , {
            text: 'Refresh'.t(),
            iconCls: 'fa fa-refresh fa-lg',
            itemId: 'refreshBtn',
            handler: 'fetchReportData'
        }, '->', {
            itemId: 'downloadBtn',
            text: 'Download'.t(),
            iconCls: 'fa fa-download fa-lg',
        }, '-', {
            itemId: 'dashboardBtn',
            iconCls: 'fa fa-home fa-lg',
            bind: {
                // text: '{isWidget ? "Remove from Dashboard" : "Add to Dashboard"}'
            }

        }]
    }, {
        region: 'south',
        xtype: 'tabpanel',
        height: 300,
        // layout: 'fit',
        minHeight: 300,
        split: true,

        defaults: {
            border: false,
            bodyBorder: false
        },

        items: [{
            title: 'Style'.t(),
            // iconCls: 'fa fa-eyedropper fa-lg',
            layout: 'border',
            items: [{
                region: 'center',
                layout: {
                    type: 'vbox',
                    align: 'stretch'
                },
                defaults: {
                    border: false,
                    bodyBorder: false
                },
                bodyPadding: 10,
                items: [{
                    // ALL - report title
                    xtype: 'textfield',
                    fieldLabel: 'Title'.t(),
                    maxWidth: 400,
                    labelWidth: 150,
                    labelAlign: 'right',
                    allowBlank: false,
                    bind: '{report.title}'
                }, {
                    // ALL - report description
                    xtype: 'textfield',
                    fieldLabel: 'Description'.t(),
                    labelWidth: 150,
                    labelAlign: 'right',
                    bind: '{report.description}'
                }, {
                    xtype: 'checkbox',
                    fieldLabel: 'Enabled'.t(),
                    labelWidth: 150,
                    labelAlign: 'right',
                    bind: '{report.enabled}'
                }, {
                    // TIME_GRAPH - chart style
                    xtype: 'container',
                    layout: 'hbox',
                    margin: '0 0 5 0',
                    hidden: true,
                    bind: {
                        disabled: '{!isTimeGraph}',
                        hidden: '{!isTimeGraph}'
                    },
                    items: [{
                        xtype: 'label',
                        cls: 'x-form-item-label-default',
                        width: 155,
                        style: {
                            textAlign: 'right'
                        },
                        text: 'Time Chart Style'.t() + ':'
                    }, {
                        xtype: 'segmentedbutton',
                        itemId: 'chartStyleBtn',
                        bind: {
                            value: '{report.timeStyle}'
                        },
                        items: [
                            {text: 'Line'.t(), value: 'LINE', styleType: 'spline'},
                            {text: 'Area'.t(), value: 'AREA', styleType: 'areaspline'},
                            {text: 'Stacked Area'.t(), value: 'AREA_STACKED', styleType: 'areaspline', stacked: true},
                            {text: 'Column'.t(), value: 'BAR', styleType: 'column', grouped: true},
                            {text: 'Overlapped Columns'.t(), value: 'BAR_OVERLAPPED', styleType: 'column', overlapped: true},
                            {text: 'Stacked Columns'.t(), value: 'BAR_STACKED', styleType: 'column', stacked : true}
                        ],
                        listeners: {
                            toggle: 'fetchReportData'
                        }
                    }]
                }, {
                    // PIE_GRAPH - chart style
                    xtype: 'container',
                    layout: 'hbox',
                    margin: '0 0 5 0',
                    hidden: true,
                    bind: {
                        disabled: '{!isPieGraph}',
                        hidden: '{!isPieGraph}'
                    },
                    items: [{
                        xtype: 'label',
                        cls: 'x-form-item-label-default',
                        width: 155,
                        style: {
                            textAlign: 'right'
                        },
                        text: 'Style'.t() + ':'
                    }, {
                        xtype: 'segmentedbutton',
                        itemId: 'chartStyleBtn',
                        bind: {
                            value: '{report.pieStyle}'
                        },
                        items: [
                            {text: 'Pie'.t(), value: 'PIE', styleType: 'pie'},
                            //{text: 'Pie 3D', value: 'PIE_3D', styleType: 'pie'},
                            {text: 'Donut'.t(), value: 'DONUT', styleType: 'pie'},
                            //{text: 'Donut 3D', value: 'DONUT_3D', styleType: 'pie'},
                            {text: 'Column'.t(), value: 'COLUMN', styleType: 'column'},
                            // {text: 'Column 3D', value: 'COLUMN_3D', styleType: 'column'}
                        ]
                        // listeners: {
                        //     toggle: 'fetchReportData'
                        // }
                    }]
                }, {
                    // TIME_GRAPH - data interval
                    xtype: 'container',
                    layout: 'hbox',
                    margin: '0 0 5 0',
                    hidden: true,
                    bind: {
                        disabled: '{!isTimeGraph}',
                        hidden: '{!isTimeGraph}'
                    },
                    items: [{
                        xtype: 'label',
                        cls: 'x-form-item-label-default',
                        width: 155,
                        style: {
                            textAlign: 'right'
                        },
                        text: 'Time Data Interval'.t() + ':'
                    }, {
                        xtype: 'segmentedbutton',
                        itemId: 'timeIntervalBtn',
                        bind: {
                            value: '{report.timeDataInterval}'
                        },
                        items: [
                            {text: 'Auto'.t(), value: 'AUTO'},
                            {text: 'Second'.t(), value: 'SECOND', defaultTimeFrame: 60 },
                            {text: 'Minute'.t(), value: 'MINUTE', defaultTimeFrame: 60 },
                            {text: 'Hour'.t(), value: 'HOUR', defaultTimeFrame: 24 },
                            {text: 'Day'.t(), value: 'DAY', defaultTimeFrame: 7 },
                            {text: 'Week'.t(), value: 'WEEK', defaultTimeFrame: 12 },
                            {text: 'Month'.t(), value: 'MONTH', defaultTimeFrame: 6 }
                        ],
                        listeners: {
                            toggle: 'fetchReportData'
                        }
                    }]
                }, {
                    // PIE_GRAPH - number of pie slices
                    xtype: 'numberfield',
                    fieldLabel: 'Pie Slices Number'.t(),
                    labelWidth: 150,
                    maxWidth: 200,
                    labelAlign: 'right',
                    minValue: 1,
                    maxValue: 25,
                    allowBlank: false,
                    hidden: true,
                    bind: {
                        disabled: '{!isPieGraph}',
                        hidden: '{!isPieGraph}',
                        value: '{report.pieNumSlices}'
                    }
                }]
            }, {
                region: 'east',
                xtype: 'grid',
                width: 150,
                resizable: false,
                split: true,
                // plugins: {
                //     ptype: 'gridviewdragdrop',
                //     // dragZone: {
                //     //     onBeforeDrag: function (data, e) {
                //     //         return Ext.get(e.target).hasCls('fa-arrows');
                //     //     }
                //     // }
                // },
                bind: '{colors}',
                tbar: [{
                    text: 'Add Color',
                    iconCls: 'fa fa-plus-circle',
                    handler: 'addColor'
                }],
                columns: [{
                    xtype: 'widgetcolumn',
                    // dataIndex: 'color',
                    menuDisabled: true,
                    flex: 1,
                    resizable: false,
                    widget: {
                        xtype: 'colorfield',
                        bind: '{record.color}'
                    }
                }, {
                    xtype: 'actioncolumn',
                    iconCls: 'fa fa-minus-circle',
                    width: 30
                }],
            }]

        }, {
            title: 'Conditions'.t(),
            // iconCls: 'fa fa-filter fa-lg'
        }, {
            title: 'Advanced'.t(),
            // iconCls: 'fa fa-cog fa-lg',
            scrollable: true,
            layout: {
                type: 'vbox',
                align: 'stretch'
            },
            items: [{
                // TIME_GRAPH - table
                xtype: 'combobox',
                fieldLabel: 'Table'.t(),
                maxWidth: 400,
                labelWidth: 150,
                labelAlign: 'right',
                editable: false,
                hidden: true,
                bind: {
                    store: '{tableNames}',
                    disabled: '{!isTimeGraph}',
                    hidden: '{!isTimeGraph}',
                    value: '{report.table}'
                }
            }, {
                // TIME_GRAPH - units
                xtype: 'textfield',
                fieldLabel: 'Units'.t(),
                maxWidth: 305,
                labelWidth: 150,
                labelAlign: 'right',
                hidden: true,
                bind: {
                    disabled: '{!isTimeGraph}',
                    hidden: '{!isTimeGraph}',
                    value: '{report.units}'
                }
            }, {
                // TIME_GRAPH - time data columns
                xtype: 'textarea',
                fieldLabel: 'Time Data Columns'.t(),
                grow: true,
                maxWidth: 500,
                labelWidth: 150,
                labelAlign: 'right',
                hidden: true,
                bind: {
                    disabled: '{!isTimeGraph}',
                    hidden: '{!isTimeGraph}',
                    value: '{report.timeDataColumns}'
                }
            }, {
                // TIME_GRAPH - series renderer
                xtype: 'textfield',
                fieldLabel: 'Series Renderer'.t(),
                maxWidth: 305,
                labelWidth: 150,
                labelAlign: 'right',
                hidden: true,
                bind: {
                    disabled: '{!isTimeGraph}',
                    hidden: '{!isTimeGraph}',
                    value: '{report.seriesRenderer}'
                }
            }, {
                // ALL - column ordering
                xtype: 'container',
                layout: 'hbox',
                margin: '0 0 5 0',
                items: [{
                    xtype: 'label',
                    cls: 'x-form-item-label-default',
                    width: 155,
                    style: {
                        textAlign: 'right'
                    },
                    text: 'Order By Column'.t() + ':'
                }, {
                    xtype: 'textfield',
                    bind: {
                        value: '{report.orderByColumn}'
                    }
                }, {
                    xtype: 'segmentedbutton',
                    margin: '0 0 0 5',
                    bind: {
                        value: '{report.orderDesc}'
                    },
                    items: [
                        {text: 'Ascending'.t(), iconCls: 'fa fa-sort-amount-asc', value: true },
                        {text: 'Descending'.t(), iconCls: 'fa fa-sort-amount-desc' , value: false }
                    ]
                }]
            }, {
                // ALL - display order
                xtype: 'numberfield',
                fieldLabel: 'Display Order'.t(),
                maxWidth: 220,
                labelWidth: 150,
                labelAlign: 'right',
                bind: {
                    value: '{report.displayOrder}'
                }
            }]
        }],

        fbar: [{
            xtype: 'component',
            style: {
                fontSize: '11px',
                color: '#555'
            },
            html: '<i class="fa fa-info-circle"></i> ' + 'This is a default report and cannot be removed or modified!',
            hidden: true,
            bind: {
                hidden: '{!report.readOnly}'
            }
        }, '->', {
            text: 'Remove'.t(),
            iconCls: 'fa fa-minus-circle',
            hidden: true,
            bind: {
                hidden: '{report.readOnly}'
            }
        }, {
            text: 'Update'.t(),
            iconCls: 'fa fa-save',
            formBind: true,
            hidden: true,
            bind: {
                hidden: '{report.readOnly}'
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

});
