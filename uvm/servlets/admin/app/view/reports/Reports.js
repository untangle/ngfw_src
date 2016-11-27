Ext.define('Ung.view.reports.Reports', {
    extend: 'Ext.container.Container',
    xtype: 'ung.reports',
    layout: 'border',

    requires: [
        'Ung.view.reports.ReportsController',
        'Ung.view.reports.ReportsModel',
        'Ung.model.Category'
    ],

    controller: 'reports',
    viewModel: {
        type: 'reports'
    },

    config: {
        // initial category used for reports inside nodes
        initCategory: null
    },

    items: [{
        region: 'west',
        xtype: 'grid',
        itemId: 'categoriesGrid',
        title: 'Select Category'.t(),
        width: 200,
        hideHeaders: true,
        shadow: false,
        collapsible: true,
        floatable: false,
        titleCollapse: true,
        animCollapse: false,
        //allowDeselect: true,
        viewConfig: {
            stripeRows: false
            /*
            getRowClass: function(record) {
                if (record.dirty) {
                    return 'dirty';
                }
            }
            */
        },
        hidden: true,
        bind: {
            hidden: '{areCategoriesHidden}',
            store: '{categories}'
        },
        columns: [{
            dataIndex: 'icon',
            width: 20,
            renderer: function (value, meta) {
                meta.tdCls = 'app-icon';
                return '<img src="' + value + '"/>';
            }
        }, {
            dataIndex: 'displayName',
            flex: 1
            /*
            renderer: function (value, meta) {
                //meta.tdCls = 'app-icon';
                return '<span style="font-weight: 600;">' + value + '</span>';
            }
            */
        }]
    }, {
        region: 'center',
        layout: 'border',
        border: false,
        items: [{
            region: 'west',
            xtype: 'grid',
            itemId: 'reportsGrid',
            title: 'Select Report'.t(),
            width: 250,
            hideHeaders: true,
            shadow: false,
            collapsible: true,
            layout: 'fit',
            animCollapse: false,
            floatable: false,
            titleCollapse: true,
            viewConfig: {
                stripeRows: false
            },
            bind: {
                //hidden: '{!report}',
                hidden: '{!isCategorySelected}',
                store: '{reports}'
            },
            columns: [{
                dataIndex: 'title',
                width: 20,
                renderer: function (value, meta, record) {
                    meta.tdCls = 'app-icon';
                    return Ung.Util.iconReportTitle(record);
                }
            }, {
                dataIndex: 'title',
                flex: 1,
                renderer: function (value, meta, record) {
                    return record.get('readOnly') ? value.t() : value;
                }
            }, {
                dataIndex: 'readOnly',
                width: 20,
                align: 'center',
                renderer: function (value, meta) {
                    meta.tdCls = 'app-icon';
                    return !value ? '<i class="material-icons" style="font-size: 14px; color: #999;">brush</i>' : '';
                }
            }, {
                dataIndex: 'uniqueId',
                width: 20,
                align: 'center',
                renderer: function (value, meta) {
                    meta.tdCls = 'app-icon';
                    if (Ext.getStore('widgets').findRecord('entryId', value)) {
                        return '<i class="material-icons" style="font-size: 14px; color: #999;">home</i>';
                    }
                    return '';
                }
            }]
        }, {
            region: 'center',
            border: false,
            layout: 'card',
            bind: {
                activeItem: '{activeCard}'
            },
            defaults: {
                border: false
            },
            items: [{
                // initial view which displays all available categories / apps
                itemId: 'allCategoriesCard',
                scrollable: true,
                layout: {
                    type: 'vbox',
                    align: 'stretch'
                    //pack: 'center'
                },
                items: [{
                    xtype: 'component',
                    cls: 'headline',
                    margin: '50 0',
                    html: 'Please select a category first!'
                }, {
                    xtype: 'component',
                    itemId: 'categoriesLoader',
                    margin: '50 0',
                    cls: 'loader',
                    html: '<div class="spinner"><div class="bounce1"></div><div class="bounce2"></div><div class="bounce3"></div></div>'
                }, {
                    xtype: 'container',
                    itemId: 'allCategoriesList',
                    //layout: 'fit',
                    //maxWidth: 600,
                    style: {
                        textAlign: 'center'
                    }
                }]
            }, {
                // view which displays all reports from a specific category
                itemId: 'categoryCard',
                scrollable: true,
                items: [{
                    xtype: 'component',
                    cls: 'headline',
                    margin: '50 0',
                    bind: {
                        html: '<img src="{category.icon}" style="width: 80px; height: 80px;"/><br/>{category.displayName}'
                    }
                }, {
                    xtype: 'container',
                    style: {
                        textAlign: 'center'
                    },
                    itemId: 'categoryReportsList'
                }]
            }, {
                // report display
                layout: 'border',
                itemId: 'reportCard',
                defaults: {
                    border: false,
                    bodyBorder: false
                },
                items: [{
                    // report heading
                    region: 'north',
                    height: 60,
                    items: [{
                        xtype: 'component',
                        cls: 'report-header',
                        bind: {
                            html: '{reportHeading}'
                        }
                    }]
                }, {
                    // report chart/event grid
                    region: 'center',
                    itemId: 'report',
                    //height: 40,
                    layout: 'fit',
                    items: [],
                    bbar: [{
                        xtype: 'component',
                        margin: '0 5 0 5',
                        html: Ung.Util.iconTitle('', 'date_range-16')
                    }, {
                        xtype: 'button',
                        bind: {
                            text: '{startDate}'
                        },
                        menu: {
                            itemId: 'startDateTimeMenu',
                            plain: true,
                            showSeparator: false,
                            shadow: false,
                            //xtype: 'form',
                            layout: {
                                type: 'vbox',
                                align: 'stretch'
                            },
                            items: [{
                                xtype: 'datepicker',
                                maxDate: new Date(),
                                itemId: 'startDate'
                            }, {
                                xtype: 'timefield',
                                itemId: 'startTime',
                                format: 'h:i a',
                                increment: 30,
                                margin: '5',
                                fieldLabel: 'Time'.t(),
                                labelAlign: 'right',
                                labelWidth: 60,
                                width: 160,
                                plain: true,
                                editable: false,
                                value: '12:00 am',
                                bind: {
                                    maxValue: '{startTimeMax}'
                                }
                            }, {
                                xtype: 'button',
                                itemId: 'startDateTimeBtn',
                                text: Ung.Util.iconTitle('OK'.t(), 'check-16')
                            }]
                        }
                    }, {
                        xtype: 'button',
                        bind: {
                            text: '{endDate}'
                        },
                        menu: {
                            itemId: 'endDateTimeMenu',
                            plain: true,
                            showSeparator: false,
                            shadow: false,
                            //xtype: 'form',
                            layout: {
                                type: 'vbox',
                                align: 'stretch'
                            },
                            items: [{
                                xtype: 'datepicker',
                                maxDate: new Date(),
                                itemId: 'endDate',
                                bind: {
                                    minDate: '{startDateTime}'
                                }
                            }, {
                                xtype: 'timefield',
                                itemId: 'endTime',
                                format: 'h:i a',
                                increment: 30,
                                margin: '5',
                                fieldLabel: 'Time'.t(),
                                labelAlign: 'right',
                                labelWidth: 60,
                                width: 160,
                                plain: true,
                                editable: false,
                                value: '12:00 am',
                                bind: {
                                    maxValue: '{endTimeMax}'
                                }
                            }, {
                                xtype: 'button',
                                itemId: 'endDateTimeBtn',
                                text: Ung.Util.iconTitle('OK'.t(), 'check-16')
                            }]
                        }
                    }, '-' , {
                        text: Ung.Util.iconTitle('Refresh'.t(), 'update-16'),
                        itemId: 'refreshBtn'
                    }, '->', {
                        itemId: 'downloadBtn',
                        text: Ung.Util.iconTitle('Download'.t(), 'file_download-16')
                    }, '-', {
                        itemId: 'dashboardBtn',
                        bind: {
                            text: '{dashboardBtnLabel}'
                        }

                    }]
                }, {
                    // report customization
                    region: 'south',
                    xtype: 'form',
                    layout: 'fit',
                    minHeight: 300,
                    shadow: false,
                    split: true,
                    collapsible: true,
                    collapsed: false,
                    floatable: false,
                    titleCollapse: true,
                    animCollapse: false,
                    border: false,
                    bodyBorder: false,

                    bind: {
                        title: '{customizeTitle}'
                    },

                    items: [{
                        xtype: 'tabpanel',
                        itemId: 'customization',
                        border: false,
                        defaults: {
                            border: false,
                            bodyBorder: false,
                            bodyPadding: 5
                        },
                        items: [{
                            title: Ung.Util.iconTitle('Style'.t(), 'color_lens-16'),
                            layout: {
                                type: 'vbox',
                                align: 'stretch'
                            },
                            items: [{
                                // ALL - report title
                                xtype: 'textfield',
                                fieldLabel: 'Title'.t(),
                                maxWidth: 400,
                                labelWidth: 150,
                                labelAlign: 'right',
                                allowBlank: false,
                                bind: {
                                    value: '{report.title}'
                                }
                            }, {
                                // ALL - report description
                                xtype: 'textfield',
                                fieldLabel: 'Description'.t(),
                                labelWidth: 150,
                                labelAlign: 'right',
                                bind: {
                                    value: '{report.description}'
                                }
                            }, {
                                // ALL - report enabled
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
                                    text: 'Enabled'.t() + ':'
                                }, {
                                    xtype: 'segmentedbutton',
                                    bind: {
                                        value: '{report.enabled}'
                                    },
                                    items: [
                                        {text: 'YES'.t(), value: true },
                                        {text: 'NO'.t(), value: false }
                                    ]
                                }]
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
                                        value: '{report.pieStyle}'
                                    },
                                    items: [
                                        {text: 'Pie', value: 'PIE', styleType: 'pie'},
                                        //{text: 'Pie 3D', value: 'PIE_3D', styleType: 'pie'},
                                        {text: 'Donut', value: 'DONUT', styleType: 'pie'},
                                        //{text: 'Donut 3D', value: 'DONUT_3D', styleType: 'pie'},
                                        {text: 'Column', value: 'COLUMN', styleType: 'column'}
                                        //{text: 'Column 3D', value: 'COLUMN_3D', styleType: 'column'}
                                    ]
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
                                    value: '{report.pieNumSlices}'
                                }
                            }]
                        }, {
                            title: Ung.Util.iconTitle('Conditions'.t(), 'find_in_page-16')
                        }, {
                            title: Ung.Util.iconTitle('Advanced'.t(), 'settings-16'),
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
                                        {text: Ung.Util.iconTitle('Ascending'.t(), 'arrow_upward-16'), value: true },
                                        {text: Ung.Util.iconTitle('Descending'.t(), 'arrow_downward-16'), value: false }
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
                        }]
                    }],
                    fbar: [/*{
                        text: Ung.Util.iconTitle('Preview'.t(), 'rotate_left-16'),
                        itemId: 'applyBtn',
                        formBind: true
                    },*/ {
                        text: Ung.Util.iconTitle('Remove'.t(), 'delete-16'),
                        itemId: 'removeBtn',
                        bind: {
                            hidden: '{report.readOnly}'
                        }
                    }, {
                        text: Ung.Util.iconTitle('Update'.t(), 'save-16'),
                        itemId: 'updateBtn',
                        formBind: true,
                        bind: {
                            hidden: '{report.readOnly}'
                        }
                    }, {
                        text: Ung.Util.iconTitle('Save as New Report'.t(), 'add-16'),
                        itemId: 'saveNewBtn',
                        formBind: true
                    }]
                }]
            }]

        }]
    }]
});