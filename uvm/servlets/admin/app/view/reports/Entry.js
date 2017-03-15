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
        // border: false,
        itemId: 'chartContainer',
        layout: 'fit',
        items: [], // here the chart will be added

        dockedItems: [{
            xtype: 'container',
            dock: 'top',
            cls: 'report-header',
            // height: 60,
            bind: {
                html: '{reportHeading}'
            }
        }],

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
            handler: 'refreshData'
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
        region: 'east',
        width: 400,
        minWidth: 400,
        split: true,
        title: 'Settings'.t(),
        scrollable: 'y',
        xtype: 'form',
        layout: 'anchor',
        // floatable: true,
        // floating: true,
        collapsible: true,
        collapsed: false,
        bodyPadding: 10,
        items: [{
            xtype: 'textfield',
            fieldLabel: 'Title'.t(),
            bind: '{entry.title}',
            anchor: '100%'
        }, {
            xtype: 'textarea',
            grow: true,
            fieldLabel: 'Description'.t(),
            bind: '{entry.description}',
            anchor: '100%'
        }, {
            xtype: 'checkbox',
            fieldLabel: 'Enabled'.t(),
            bind: '{entry.enabled}'
        }, {
            xtype: 'fieldset',
            title: '<i class="fa fa-paint-brush"></i> ' + 'Style'.t(),
            padding: 10,
            defaults: {
                labelWidth: 150,
                labelAlign: 'right'
            },
            items: [{
                xtype: 'combo',
                fieldLabel: 'Time Chart Style'.t(),
                anchor: '100%',
                bind: '{entry.timeStyle}',
                editable: false,
                store: [
                    ['LINE', 'Line'.t()],
                    ['AREA', 'Area'.t()],
                    ['AREA_STACKED', 'Stacked Area'.t()],
                    ['BAR', 'Column'.t()],
                    ['BAR_OVERLAPPED', 'Overlapped Columns'.t()],
                    ['BAR_STACKED', 'Stacked Columns'.t()]
                ],
                queryMode: 'local'
            }, {
                xtype: 'combo',
                fieldLabel: 'Time Data Interval'.t(),
                anchor: '100%',
                bind: '{entry.timeDataInterval}',
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
                queryMode: 'local'
            }, {
                xtype: 'checkbox',
                reference: 'defaultColors',
                fieldLabel: 'Colors'.t(),
                boxLabel: 'Default'.t(),
                bind: '{_defaultColors}'
            }, {
                xtype: 'container',
                margin: '0 0 0 155',
                itemId: 'colors',
                // layout: 'hbox',
                bind: {
                    hidden: '{defaultColors.checked}'
                }
            }]
        }, {
            xtype: 'fieldset',
            title: '<i class="fa fa-sliders"></i> ' + 'Advanced'.t(),
            padding: 10,
            defaults: {
                labelWidth: 150,
                labelAlign: 'right'
            },
            items: [{
                xtype: 'textfield',
                fieldLabel: 'Units'.t(),
                anchor: '100%',
                bind: '{entry.units}'
            }, {
                xtype: 'combo',
                fieldLabel: 'Table'.t(),
                anchor: '100%',
                bind: '{entry.table}',
                editable: false,
                queryMode: 'local'
            }, {
                xtype: 'textarea',
                anchor: '100%',
                fieldLabel: 'Time Data Columns'.t(),
                grow: true,
                bind: '{entry.timeDataColumns}'
            }, {
                xtype: 'textfield',
                anchor: '100%',
                fieldLabel: 'Series Renderer'.t(),
                bind: '{entry.seriesRenderer}'
            }, {
                xtype: 'textfield',
                anchor: '100%',
                fieldLabel: 'Order By Column'.t(),
                bind: '{entry.orderByColumn}'
            }, {
                xtype: 'segmentedbutton',
                margin: '0 0 5 155',
                bind: '{entry.orderDesc}',
                items: [
                    { text: 'Ascending'.t(), iconCls: 'fa fa-sort-amount-asc', value: true },
                    { text: 'Descending'.t(), iconCls: 'fa fa-sort-amount-desc' , value: false }
                ]
            }, {
                // ALL - display order
                xtype: 'numberfield',
                fieldLabel: 'Display Order'.t(),
                anchor: '70%',
                bind: '{entry.displayOrder}'
            }]
        }, {
            xtype: 'fieldset',
            title: '<i class="fa fa-database"></i> ' + 'Sql Conditions:'.t(),
            padding: 10,

            items: [{
                xtype: 'button',
                menu: {
                    items: [{
                        text: 'aaaa',
                        menu: {
                            items: [{
                                text: 'cccc',
                                menu: {
                                    items: [{
                                        xtype: 'textfield'
                                    }]
                                }
                            }]
                        }
                    }, {
                        text: 'bbb'
                    }]
                }
            }]
        }]



    }, {
        region: 'south',
        xtype: 'tabpanel',
        height: 280,
        title: 'Customize'.t(),
        collapsible: true,
        collapsed: true,
        animCollapse: false,
        titleCollapse: true,
        // layout: 'fit',
        // minHeight: 'auto',
        split: true,
        hidden: true,
        bind: {
            hidden: '{!entry}'
        },
        items: [{
            title: 'Style'.t(),
            // iconCls: 'fa fa-eyedropper fa-lg',
            layout: {
                type: 'vbox',
                align: 'stretch'
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
                msgTarget: 'side',
                bind: '{entry.title}',
                validator: function (val) {
                    var category = this.up('reports-entry').getViewModel().get('category'),
                        entry = this.up('reports-entry').getViewModel().get('entry'),
                        reports = Ext.getStore('reports').queryBy(function (report) {
                            return report.get('category') === category.get('categoryName') &&
                                report.get('uniqueId') !== entry.get('uniqueId') &&
                                report.get('title').replace(/[^0-9a-z\s]/gi, '').replace(/\s+/g, '-').toLowerCase() === val.replace(/[^0-9a-z\s]/gi, '').replace(/\s+/g, '-').toLowerCase();
                        });
                    if (reports.length > 0) {
                        return 'This title already exists in this category!'.t();
                    }
                    return true;
                }
            }, {
                // ALL - report description
                xtype: 'textfield',
                fieldLabel: 'Description'.t(),
                labelWidth: 150,
                labelAlign: 'right',
                bind: '{entry.description}'
            }, {
                xtype: 'checkbox',
                fieldLabel: 'Enabled'.t(),
                labelWidth: 150,
                labelAlign: 'right',
                bind: '{entry.enabled}'
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
                        value: '{entry.timeStyle}'
                    },
                    items: [
                        {text: 'Line'.t(), value: 'LINE', styleType: 'spline'},
                        {text: 'Area'.t(), value: 'AREA', styleType: 'areaspline'},
                        {text: 'Stacked Area'.t(), value: 'AREA_STACKED', styleType: 'areaspline', stacked: true},
                        {text: 'Column'.t(), value: 'BAR', styleType: 'column', grouped: true},
                        {text: 'Bar 3D'.t(), value: 'BAR_3D', styleType: 'column', grouped: true},
                        {text: 'Bar 3D Overlapped'.t(), value: 'BAR_3D_OVERLAPPED', styleType: 'column', grouped: true},
                        {text: 'Overlapped Columns'.t(), value: 'BAR_OVERLAPPED', styleType: 'column', overlapped: true},
                        {text: 'Stacked Columns'.t(), value: 'BAR_STACKED', styleType: 'column', stacked : true}
                    ]
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
                        value: '{entry.pieStyle}'
                    },
                    items: [
                        {text: 'Pie'.t(), value: 'PIE', styleType: 'pie'},
                        {text: 'Pie 3D'.t(), value: 'PIE_3D', styleType: 'pie'},
                        {text: 'Donut'.t(), value: 'DONUT', styleType: 'pie'},
                        {text: 'Donut 3D'.t(), value: 'DONUT_3D', styleType: 'pie'},
                        {text: 'Column'.t(), value: 'COLUMN', styleType: 'column'},
                        {text: 'Column 3D'.t(), value: 'COLUMN_3D', styleType: 'column'}
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
                        value: '{entry.timeDataInterval}'
                    },
                    items: [
                        {text: 'Auto'.t(), value: 'AUTO'},
                        {text: 'Second'.t(), value: 'SECOND', defaultTimeFrame: 60 },
                        {text: 'Minute'.t(), value: 'MINUTE', defaultTimeFrame: 60 },
                        {text: 'Hour'.t(), value: 'HOUR', defaultTimeFrame: 24 },
                        {text: 'Day'.t(), value: 'DAY', defaultTimeFrame: 7 },
                        {text: 'Week'.t(), value: 'WEEK', defaultTimeFrame: 12 },
                        {text: 'Month'.t(), value: 'MONTH', defaultTimeFrame: 6 }
                    ]
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
                    value: '{entry.pieNumSlices}'
                }
            },
            {
                // xtype: 'container',
                // layout: 'hbox',
                // margin: '0 0 5 0',
                // items: [{
                //     xtype: 'label',
                //     cls: 'x-form-item-label-default',
                //     width: 155,
                //     style: {
                //         textAlign: 'right'
                //     },
                //     text: 'Colors'.t() + ':'
                // }, {
                //     xtype: 'checkbox',
                //     reference: 'defaultColors',
                //     margin: '0 10 0 0',
                //     boxLabel: 'Default'.t(),
                //     bind: '{_defaultColors}'
                // }, {
                //     itemId: 'colors',
                //     xtype: 'container',
                //     layout: 'hbox',
                //     bind: {
                //         hidden: '{defaultColors.checked}'
                //     }
                // }]
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
                    value: '{entry.table}'
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
                    value: '{entry.units}'
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
                    value: '{entry.timeDataColumns}'
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
                    value: '{entry.seriesRenderer}'
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
                        value: '{entry.orderByColumn}'
                    }
                }, {
                    xtype: 'segmentedbutton',
                    margin: '0 0 0 5',
                    bind: {
                        value: '{entry.orderDesc}'
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
                    value: '{entry.displayOrder}'
                }
            }]
        }],

        fbar: [{
            xtype: 'component',
            style: {
                fontSize: '11px',
                color: '#555'
            },
            html: '<i class="fa fa-info-circle"></i> ' + 'This is a default report. Any changes can be saved only by creating a new Report!',
            hidden: true,
            bind: {
                hidden: '{!entry.readOnly}'
            }
        }, '->', {
            text: 'Remove'.t(),
            iconCls: 'fa fa-minus-circle',
            hidden: true,
            bind: {
                hidden: '{entry.readOnly}'
            }
        }, {
            text: 'Update'.t(),
            iconCls: 'fa fa-save',
            formBind: true,
            hidden: true,
            bind: {
                hidden: '{entry.readOnly}'
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
