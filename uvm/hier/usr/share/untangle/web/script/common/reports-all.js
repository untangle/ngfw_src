Ext.define ('Ung.model.Category', {
    extend: 'Ext.data.Model' ,
    fields: [
        { name: 'categoryName', type: 'string' },
        { name: 'displayName', type: 'string' },
        { name: 'icon', type: 'string' },
        {
            name: 'slug',
            calculate: function (cat) {
                return cat.categoryName.replace(/ /g, '-').toLowerCase();
            }
        },
    ],
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    }
});

Ext.define ('Ung.model.Report', {
    extend: 'Ext.data.Model' ,
    fields: [
        'category', 'colors', 'conditions', 'defaultColumns', 'description',
        'displayOrder', 'enabled',
        'javaClass',
        'orderByColumn',
        'orderDesc',

        'pieGroupColumn',
        'pieNumSlices',
        'pieStyle',
        'pieSumColumn',

        'readOnly',
        'seriesRenderer',
        'table',
        'textColumns',
        'textString',

        'timeDataColumns',
        'timeDataDynamicAggregationFunction',
        'timeDataDynamicAllowNull',
        'timeDataDynamicColumn',
        'timeDataDynamicLimit',
        'timeDataDynamicValue',
        'timeDataInterval',
        'timeStyle',
        'title',
        'type',
        'uniqueId',
        'units',

        {
            name: 'localizedTitle',
            calculate: function (entry) {
                return entry.readOnly ? entry.title : entry.title;
            }
        },
        {
            name: 'localizedDescription',
            calculate: function (entry) {
                return entry.readOnly ? entry.description : entry.description;
            }
        },
        {
            name: 'slug',
            calculate: function (entry) {
                if (entry.title) {
                    return entry.title.replace(/[^0-9a-z\s]/gi, '').replace(/\s+/g, '-').toLowerCase();
                }
                return '';
            }
        },
        {
            name: 'url',
            calculate: function (entry) {
                // return entry.category.replace(/ /g, '').toLowerCase() + '/' + entry.uniqueId;
                return entry.category.replace(/ /g, '-').toLowerCase() + '/' + entry.slug;
            }
        },
        {
            name: 'icon',
            calculate: function (entry) {
                var icon;
                switch (entry.type) {
                case 'TEXT':
                    icon = 'fa-align-left';
                    break;
                case 'EVENT_LIST':
                    icon = 'fa-list-ul';
                    break;
                case 'PIE_GRAPH':
                    icon = 'fa-pie-chart';
                    if (entry.pieStyle === 'COLUMN' || entry.pieStyle === 'COLUMN_3D') {
                        icon = 'fa-bar-chart';
                    } else {
                        if (entry.pieStyle === 'DONUT' || entry.pieStyle === 'DONUT_3D') {
                            icon = 'fa-pie-chart';
                        }
                    }
                    break;
                case 'TIME_GRAPH':
                case 'TIME_GRAPH_DYNAMIC':
                    icon = 'fa-line-chart';
                    if (entry.timeStyle.indexOf('BAR') >= 0) {
                        icon = 'fa-bar-chart';
                    } else {
                        if (entry.timeStyle.indexOf('AREA') >= 0) {
                            icon = 'fa-area-chart';
                        }
                    }
                    break;
                default:
                    icon = 'fa-align-left';
                }
                return icon;
            }
        }
    ],
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
            //rootProperty: 'list'
        }
    }
});

Ext.define('Ung.store.Categories', {
    extend: 'Ext.data.Store',
    storeId: 'categories',

    fields: [{
        name: 'displayName', type: 'string'
    }, {
        name: 'name', type: 'string'
    }, {
        name: 'type', type: 'string', defaultValue: 'app'
    }, {
        name: 'icon', type: 'string',
        calculate: function (cat) {
            if (cat.type === 'system') {
                return '/skins/modern-rack/images/admin/config/icon_config_' + cat.name + '.png';
            }
            return '/skins/modern-rack/images/admin/apps/' + cat.name + '_80x80.png';
        }
    }, {
        name: 'slug', type: 'string',
        calculate: function (cat) { return cat.name; }
    }],
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
        }
    }
});

Ext.define('Ung.store.Reports', {
    extend: 'Ext.data.Store',
    alias: 'store.reports',
    storeId: 'reports',
    model: 'Ung.model.Report',
    groupField: 'category',
    sorters: [{
        property: 'displayOrder',
        direction: 'ASC'
    }]
});

Ext.define('Ung.store.ReportsTree', {
    extend: 'Ext.data.TreeStore',
    alias: 'store.reportstree',
    storeId: 'reportstree',
    filterer: 'bottomup',

    build: function () {
        var me = this, nodes = [], storeCat, category;
        Ext.Array.each(Ext.getStore('reports').getGroups().items, function (group) {
            storeCat = Ext.getStore('categories').findRecord('displayName', group._groupKey);

            if (!storeCat) { return; }

            // create category node
            category = {
                text: group._groupKey,
                slug: storeCat.get('slug'),
                type: storeCat.get('type'), // app or system
                icon: storeCat.get('icon'),
                cls: 'x-tree-category',
                url: storeCat.get('slug'),
                children: [],
                // expanded: group._groupKey === vm.get('category.categoryName')
            };
            // add reports to each category
            Ext.Array.each(group.items, function (entry) {
                category.children.push({
                    text: entry.get('localizedTitle'),
                    slug: entry.get('slug'),
                    url: entry.get('url'),
                    uniqueId: entry.get('uniqueId'),
                    type: entry.get('type'),
                    readOnly: entry.get('readOnly'),
                    iconCls: 'fa ' + entry.get('icon'),
                    cls: 'x-tree-report',
                    leaf: true
                    // selected: uniqueId === vm.get('entry.uniqueId')
                });
            });
            nodes.push(category);
        });

        me.setRoot({
            text: 'All reports',
            slug: 'reports',
            expanded: true,
            children: nodes
        });
    }

});

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
                xtype: 'combo',
                itemId: 'eventsLimitSelector',
                hidden: true,
                bind: {
                    hidden: '{entry.type !== "EVENT_LIST"}'
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
                handler: 'refreshData',
                bind: {
                    disabled: '{autoRefresh}'
                }
            }, {
                xtype: 'button',
                text: 'Auto Refresh'.t(),
                enableToggle: true,
                toggleHandler: 'setAutoRefresh',
                bind: {
                    iconCls: '{autoRefresh ? "fa fa-check-square-o" : "fa fa-square-o"}',
                    pressed: '{autoRefresh}'
                }
            }, {
                text: 'Reset View'.t(),
                iconCls: 'fa fa-refresh',
                itemId: 'resetBtn',
                handler: 'resetView',
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
            }],
            bbar: ['->', {
                text: 'Delete'.t(),
                iconCls: 'fa fa-minus-circle',
                disabled: true,
                bind: {
                    disabled: '{entry.readOnly}'
                },
                handler: 'removeReport'
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

Ext.define('Ung.view.reports.EntryController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.reports-entry',

    control: {
        '#': {
            afterrender: 'onAfterRender',
        }
    },

    refreshInterval: null,

    // colorPalette: [
    //     // red
    //     'B71C1C', 'C62828', 'D32F2F', 'E53935', 'F44336', 'EF5350', 'E57373', 'EF9A9A',
    //     // pink
    //     '880E4F', 'AD1547', 'C2185B', 'D81B60', 'E91E63', 'EC407A', 'F06292', 'F48FB1',
    //     // purple
    //     '4A148C', '6A1B9A', '7B1FA2', '8E24AA', '9C27B0', 'AB47BC', 'BA68C8', 'CE93D8',
    //     // blue
    //     '0D47A1', '1565C0', '1976D2', '1E88E5', '2196F3', '42A5F5', '64B5F6', '90CAF9',
    //     // teal`
    //     '004D40', '00695C', '00796B', '00897B', '009688', '26A69A', '4DB6AC', '80CBC4',
    //     // green
    //     '1B5E20', '2E7D32', '388E3C', '43A047', '4CAF50', '66BB6A', '81C784', 'A5D6A7',
    //     // limE
    //     '827717', '9E9D24', 'AFB42B', 'C0CA33', 'CDDC39', 'D4E157', 'DCE775', 'E6EE9C',
    //     // yellow
    //     'F57F17', 'F9A825', 'FBC02D', 'FDD835', 'FFEB3B', 'FFEE58', 'FFF176', 'FFF59D',
    //     // orange
    //     'E65100', 'EF6C00', 'F57C00', 'FB8C00', 'FF9800', 'FFA726', 'FFB74D', 'FFCC80',
    //     // brown
    //     '3E2723', '4E342E', '5D4037', '6D4C41', '795548', '8D6E63', 'A1887F', 'BCAAA4',
    //     // grey
    //     '212121', '424242', '616161', '757575', '9E9E9E', 'BDBDBD', 'E0E0E0', 'EEEEEE',
    // ],

    onAfterRender: function () {
        var me = this, vm = this.getViewModel(), widget,
            entryContainer = me.getView().down('#entryContainer'),
            dataGrid = this.getView().down('#currentData');

        vm.bind('{entry}', function (entry) {
            vm.set('_currentData', []);
            vm.set('autoRefresh', false); // reset auto refresh

            dataGrid.setColumns([]);
            dataGrid.setLoading(true);

            me.tableConfig = TableConfig.generate(entry.get('table'));

            if (entry.get('type') === 'EVENT_LIST') {
                me.lookupReference('dataBtn').setPressed(false);
                me.getView().down('#tableColumns').removeAll();
                me.getView().down('#tableColumns').add(me.tableConfig.checkboxes);
                me.getView().down('#tableColumns').setValue(entry.get('defaultColumns') ? entry.get('defaultColumns').join() : '');
                // me.lookup('filterfield').fireEvent('change');
                me.lookup('filterfield').setValue('');
            } else {
                me.getView().down('#tableColumns').removeAll();
                me.getView().down('#tableColumns').setValue({});
            }

            // check if widget in admin context
            if (Ung.app.servletContext === 'admin') {
                widget = Ext.getStore('widgets').findRecord('entryId', entry.get('uniqueId')) || null;
                vm.set('widget', Ext.getStore('widgets').findRecord('entryId', entry.get('uniqueId')));
            }




            // set the _sqlConditions data as for the sql conditions grid store
            vm.set('_sqlConditions', entry.get('conditions') || []);
            // set combo store conditions
            me.getView().down('#sqlConditionsCombo').getStore().setData(me.tableConfig.comboItems);
            me.getView().down('#sqlConditionsCombo').setValue(null);

            me.getView().down('#sqlFilterCombo').getStore().setData(me.tableConfig.comboItems);
            me.getView().down('#sqlFilterCombo').setValue(null);

        });

        // vm.bind('{_defaultColors}', function (val) {
        //     console.log('colors');
        //     var colors, colorBtns = [];

        //     if (val) {
        //         vm.set('entry.colors', null);
        //     } else {
        //         colors = vm.get('entry.colors') || Util.defaultColors;
        //         me.getView().down('#colors').removeAll();
        //         colors.forEach(function (color, i) {
        //             colorBtns.push({
        //                 xtype: 'button',
        //                 margin: '0 1',
        //                 idx: i,
        //                 arrowVisible: false,
        //                 menu: {
        //                     plain: true,
        //                     xtype: 'colormenu',
        //                     colors: me.colorPalette,
        //                     height: 200,
        //                     listeners: {
        //                         select: 'updateColor'
        //                     },
        //                     dockedItems: [{
        //                         xtype: 'toolbar',
        //                         dock: 'bottom',
        //                         // ui: 'footer',
        //                         items: [{
        //                             // text: 'Remove'.t(),
        //                             iconCls: 'fa fa-ban',
        //                             tooltip: 'Remove'.t()
        //                         }, {
        //                             text: 'OK'.t(),
        //                             iconCls: 'fa fa-check',
        //                             listeners: {
        //                                 click: function (btn) {
        //                                     btn.up('button').hideMenu();
        //                                 }
        //                             }
        //                         }]

        //                     }]
        //                 },
        //                 text: '<i class="fa fa-square" style="color: ' + color + '"></i>',
        //             });
        //         });
        //         me.getView().down('#colors').add(colorBtns);
        //     }
        // });

    },

    closeSide: function () {
        this.lookupReference('dataBtn').setPressed(false);
        this.lookupReference('settingsBtn').setPressed(false);
    },

    formatTimeData: function (data) {
        var entry = this.getViewModel().get('entry'),
            vm = this.getViewModel(),
            dataGrid = this.getView().down('#currentData'), i, column;

        dataGrid.setLoading(false);

        // var storeFields = [{
        //     name: 'time_trunc'
        // }];

        var reportDataColumns = [{
            dataIndex: 'time_trunc',
            header: 'Timestamp'.t(),
            width: 130,
            flex: 1,
            renderer: function (val) {
                return (!val) ? 0 : Util.timestampFormat(val);
            }
        }];
        var seriesRenderer = null, title;

        for (i = 0; i < entry.get('timeDataColumns').length; i += 1) {
            column = entry.get('timeDataColumns')[i].split(' ').splice(-1)[0];
            title = column;
            reportDataColumns.push({
                dataIndex: column,
                header: title,
                width: entry.get('timeDataColumns').length > 2 ? 60 : 90,
                renderer: function (val) {
                    return val !== undefined ? val : '-';
                }
            });
        }

        dataGrid.setColumns(reportDataColumns);
        dataGrid.getStore().loadData(data);
        // vm.set('_currentData', data);
    },

    formatTimeDynamicData: function (data) {
        var vm = this.getViewModel(),
            entry = vm.get('entry'),
            timeDataColumns = [],
            dataGrid = this.getView().down('#currentData'), i, column;

        dataGrid.setLoading(false);

        for (i = 0; i < data.length; i += 1) {
            for (var _column in data[i]) {
                if (data[i].hasOwnProperty(_column) && _column !== 'time_trunc' && _column !== 'time' && timeDataColumns.indexOf(_column) < 0) {
                    timeDataColumns.push(_column);
                }
            }
        }

        var reportDataColumns = [{
            dataIndex: 'time_trunc',
            header: 'Timestamp'.t(),
            width: 130,
            flex: 1,
            renderer: function (val) {
                return (!val) ? 0 : Util.timestampFormat(val);
            }
        }];
        var seriesRenderer = null, title;
        if (!Ext.isEmpty(entry.get('seriesRenderer'))) {
            seriesRenderer = Renderer[entry.get('seriesRenderer')];
        }

        for (i = 0; i < timeDataColumns.length; i += 1) {
            column = timeDataColumns[i];
            title = seriesRenderer ? seriesRenderer(column) + ' [' + column + ']' : column;
            // storeFields.push({name: timeDataColumns[i], type: 'integer'});
            reportDataColumns.push({
                dataIndex: column,
                header: title,
                width: timeDataColumns.length > 2 ? 60 : 90
            });
        }

        dataGrid.setColumns(reportDataColumns);
        dataGrid.getStore().loadData(data);
        // vm.set('_currentData', data);
    },

    formatPieData: function (data) {
        var me = this, entry = me.getViewModel().get('entry'),
            vm = me.getViewModel(),
            dataGrid = me.getView().down('#currentData');

        dataGrid.setLoading(false);

        dataGrid.setColumns([{
            dataIndex: entry.get('pieGroupColumn'),
            header: me.sqlColumnRenderer(entry.get('pieGroupColumn')),
            flex: 1
        }, {
            dataIndex: 'value',
            header: 'value'.t(),
            width: 200
        }, {
            xtype: 'actioncolumn',
            menuDisabled: true,
            width: 30,
            align: 'center',
            items: [{
                iconCls: 'fa fa-filter',
                tooltip: 'Add Condition'.t(),
                handler: 'addPieFilter'
            }]
        }]);
        dataGrid.getStore().loadData(data);
        // vm.set('_currentData', data);

    },

    addPieFilter: function (view, rowIndex, colIndex, item, e, record) {
        var me = this, vm = me.getViewModel(),
            gridFilters =  me.getView().down('#sqlFilters'),
            col = vm.get('entry.pieGroupColumn');

        if (col) {
            vm.get('sqlFilterData').push({
                column: col,
                operator: '=',
                value: record.get(col),
                javaClass: 'com.untangle.app.reports.SqlCondition'
            });
        } else {
            console.log('Issue with pie column!');
            return;
        }

        gridFilters.setCollapsed(false);
        gridFilters.setTitle(Ext.String.format('Conditions: {0}'.t(), vm.get('sqlFilterData').length));
        gridFilters.getStore().reload();
        me.refreshData();
    },

    formatTextData: function (data) {
        var entry = this.getViewModel().get('entry'),
            vm = this.getViewModel(),
            dataGrid = this.getView().down('#currentData');
        dataGrid.setLoading(false);

        dataGrid.setColumns([{
            dataIndex: 'data',
            header: 'data'.t(),
            flex: 1
        }, {
            dataIndex: 'value',
            header: 'value'.t(),
            width: 200
        }]);

        var reportData = [], value;
        if (data.length > 0 && entry.get('textColumns') !== null) {
            for (i = 0; i < entry.get('textColumns').length; i += 1) {
                column = entry.get('textColumns')[i].split(' ').splice(-1)[0];
                value = Ext.isEmpty(data[0][column]) ? 0 : data[0][column];
                reportData.push({data: column, value: value});
            }
        }
        // vm.set('_currentData', reportData);
        dataGrid.getStore().loadData(reportData);
    },

    filterData: function (min, max) {
        // aply filtering only on timeseries
        if (this.getViewModel().get('entry.type').indexOf('TIME_GRAPH') >= 0) {
            this.getView().down('#currentData').getStore().clearFilter();
            this.getView().down('#currentData').getStore().filterBy(function (point) {
                var t = point.get('time_trunc').time;
                return t >= min && t <= max ;
            });
        }
    },



    updateColor: function (menu, color) {
        var vm = this.getViewModel(),
            newColors = vm.get('entry.colors') ? Ext.clone(vm.get('entry.colors')) : Ext.clone(Util.defaultColors);

        menu.up('button').setText('<i class="fa fa-square" style="color: #' + color + ';"></i>');
        newColors[menu.up('button').idx] = '#' + color;
        vm.set('entry.colors', newColors);
        return false;
    },

    // addColor: function (btn) {
    //     btn.up('grid').getStore().add({color: 'FF0000'});
    //     // var vm = this.getViewModel();
    //     // var colors = vm.get('report.colors');
    //     // colors.push('#FF0000');
    //     // vm.set('report.colors', colors);
    // },

    refreshData: function () {
        var vm = this.getViewModel();
        switch(vm.get('entry.type')) {
            case 'TEXT': this.getView().down('textreport').getController().fetchData(); break;
            case 'EVENT_LIST': this.getView().down('eventreport').getController().fetchData(); break;
            default: this.getView().down('graphreport').getController().fetchData();
        }
    },

    resetView: function(){
        var grid = this.getView().down('grid');
        Ext.state.Manager.clear(grid.stateId);
        grid.reconfigure(null, grid.tableConfig.columns);
    },


    // TABLE COLUMNS / CONDITIONS
    updateDefaultColumns: function (el, value) {
        this.getViewModel().set('entry.defaultColumns', value.split(','));
    },

    addSqlCondition: function (btn) {
        var me = this, vm = me.getViewModel(),
            conds = vm.get('_sqlConditions') || [];

        conds.push({
            autoFormatValue: false,
            column: me.getView().down('#sqlConditionsCombo').getValue(),
            javaClass: 'com.untangle.app.reports.SqlCondition',
            operator: '=',
            value: ''
        });

        me.getView().down('#sqlConditionsCombo').setValue(null);

        vm.set('_sqlConditions', conds);
        me.getView().down('#sqlConditions').getStore().reload();
    },

    removeSqlCondition: function (table, rowIndex) {
        var me = this, vm = me.getViewModel(),
            conds = vm.get('_sqlConditions');
        Ext.Array.removeAt(conds, rowIndex);
        vm.set('_sqlConditions', conds);
        me.getView().down('#sqlConditions').getStore().reload();
    },

    sqlColumnRenderer: function (val) {
        return '<strong>' + TableConfig.getColumnHumanReadableName(val) + '</strong> <span style="float: right;">[' + val + ']</span>';
    },
    // TABLE COLUMNS / CONDITIONS END


    // FILTERS
    addSqlFilter: function () {
        var me = this, vm = me.getViewModel(),
            _filterComboCmp = me.getView().down('#sqlFilterCombo'),
            _operatorCmp = me.getView().down('#sqlFilterOperator'),
            _filterValueCmp = me.getView().down('#sqlFilterValue');

        vm.get('sqlFilterData').push({
            column: _filterComboCmp.getValue(),
            operator: _operatorCmp.getValue(),
            value: _filterValueCmp.getValue(),
            javaClass: 'com.untangle.app.reports.SqlCondition'
        });

        _filterComboCmp.setValue(null);
        _operatorCmp.setValue('=');

        me.getView().down('#filtersToolbar').remove('sqlFilterValue');

        me.getView().down('#sqlFilters').setTitle(Ext.String.format('Conditions: {0}'.t(), vm.get('sqlFilterData').length));
        me.getView().down('#sqlFilters').getStore().reload();
        me.refreshData();
    },

    removeSqlFilter: function (table, rowIndex) {
        var me = this, vm = me.getViewModel();
        Ext.Array.removeAt(vm.get('sqlFilterData'), rowIndex);

        me.getView().down('#filtersToolbar').remove('sqlFilterValue');

        me.getView().down('#sqlFilters').setTitle(Ext.String.format('Conditions: {0}'.t(), vm.get('sqlFilterData').length));
        me.getView().down('#sqlFilters').getStore().reload();
        me.refreshData();
    },

    onColumnChange: function (cmp, newValue) {
        var me = this;

        cmp.up('toolbar').remove('sqlFilterValue');

        if (!newValue) { return; }
        var column = Ext.Array.findBy(me.tableConfig.columns, function (column) {
            return column.dataIndex === newValue;
        });

        if (column.widgetField) {
            column.widgetField.itemId = 'sqlFilterValue';
            cmp.up('toolbar').insert(4, column.widgetField);
        } else {
            cmp.up('toolbar').insert(4, {
                xtype: 'textfield',
                itemId: 'sqlFilterValue',
                value: ''
            });
        }
    },

    onFilterKeyup: function (cmp, e) {
        if (e.keyCode === 13) {
            this.addSqlFilter();
        }
    },

    sqlFilterQuickItems: function (btn) {
        var me = this, menuItem, menuItems = [], col;
        Rpc.asyncData('rpc.reportsManager.getConditionQuickAddHints').then(function (result) {
            Ext.Object.each(result, function (key, vals) {
                menuItem = {
                    text: TableConfig.getColumnHumanReadableName(key),
                    disabled: vals.length === 0
                };
                if (vals.length > 0) {
                    menuItem.menu = {
                        plain: true,
                        items: Ext.Array.map(vals, function (val) {
                            return {
                                text: val,
                                column: key
                            };
                        }),
                        listeners: {
                            click: 'selectQuickFilter'
                        }
                    };
                }
                menuItems.push(menuItem);


            });
            btn.getMenu().removeAll();
            btn.getMenu().add(menuItems);
        });
    },

    selectQuickFilter: function (menu, item) {
        var me = this, vm = this.getViewModel(),
            _filterComboCmp = me.getView().down('#sqlFilterCombo'),
            _operatorCmp = me.getView().down('#sqlFilterOperator');

        vm.get('sqlFilterData').push({
            column: item.column,
            operator: '=',
            value: item.text,
            javaClass: 'com.untangle.app.reports.SqlCondition'
        });

        _filterComboCmp.setValue(null);
        _operatorCmp.setValue('=');

        me.getView().down('#filtersToolbar').remove('sqlFilterValue');

        me.getView().down('#sqlFilters').setTitle(Ext.String.format('Conditions: {0}'.t(), vm.get('sqlFilterData').length));
        me.getView().down('#sqlFilters').getStore().reload();
        me.refreshData();

    },

    // END FILTERS


    // // DASHBOARD ACTION
    dashboardAddRemove: function (btn) {
        var vm = this.getViewModel(), widget = vm.get('widget'), entry = vm.get('entry'), action;

        if (!widget) {
            action = 'add';
            widget = Ext.create('Ung.model.Widget', {
                displayColumns: entry.get('displayColumns'),
                enabled: true,
                entryId: entry.get('uniqueId'),
                javaClass: 'com.untangle.uvm.DashboardWidgetSettings',
                refreshIntervalSec: 60,
                timeframe: '',
                type: 'ReportEntry'
            });
        } else {
            action = 'remove';
        }

        Ext.fireEvent('widgetaction', action, widget, entry, function (wg) {
            vm.set('widget', wg);
            Util.successToast('<span style="color: yellow; font-weight: 600;">' + vm.get('entry.title') + '</span> ' + (action === 'add' ? 'added to' : 'removed from') + ' Dashboard!');
        });
    },



    updateReport: function () {
        var me = this,
            v = this.getView(),
            vm = this.getViewModel(),
            entry = vm.get('entry');

        v.setLoading(true);
        Rpc.asyncData('rpc.reportsManager.saveReportEntry', entry.getData())
            .then(function(result) {
                v.setLoading(false);
                vm.get('report').copyFrom(entry);
                vm.get('report').commit();
                Util.successToast('<span style="color: yellow; font-weight: 600;">' + vm.get('entry.title') + '</span> report updated!');
                Ung.app.redirectTo('#reports/' + entry.get('category').replace(/ /g, '-').toLowerCase() + '/' + entry.get('title').replace(/[^0-9a-z\s]/gi, '').replace(/\s+/g, '-').toLowerCase());

                Ext.getStore('reportstree').build(); // rebuild tree after save new
                me.refreshData();
            });
    },

    saveNewReport: function () {
        var v = this.getView(),
            vm = this.getViewModel(),
            entry = vm.get('entry');

        entry.set('uniqueId', 'report-' + Math.random().toString(36).substr(2));
        entry.set('readOnly', false);

        v.setLoading(true);
        Rpc.asyncData('rpc.reportsManager.saveReportEntry', entry.getData())
            .then(function(result) {
                v.setLoading(false);
                Ext.getStore('reports').add(entry);
                entry.commit();
                Util.successToast('<span style="color: yellow; font-weight: 600;">' + entry.get('title') + ' report added!');
                Ung.app.redirectTo('#reports/' + entry.get('category').replace(/ /g, '-').toLowerCase() + '/' + entry.get('title').replace(/[^0-9a-z\s]/gi, '').replace(/\s+/g, '-').toLowerCase());

                Ext.getStore('reportstree').build(); // rebuild tree after save new
            });
    },

    removeReport: function () {
        var me = this, v = this.getView(),
            vm = this.getViewModel(),
            entry = vm.get('entry');

        if (vm.get('widget')) {
            Ext.MessageBox.confirm('Warning'.t(),
                'Deleting this report will remove also the Widget from Dashboard!'.t() + '<br/><br/>' +
                'Do you want to continue?'.t(),
                function (btn) {
                    if (btn === 'yes') {
                        // remove it from dashboard first
                        Ext.fireEvent('widgetaction', 'remove', vm.get('widget'), entry, function (wg) {
                            vm.set('widget', wg);
                            me.removeReportAction(entry.getData());
                        });
                    }
                });
        } else {
            me.removeReportAction(entry.getData());
        }

    },

    removeReportAction: function (entry) {
        Rpc.asyncData('rpc.reportsManager.removeReportEntry', entry)
            .then(function (result) {
                Ung.app.redirectTo('#reports/' + entry.category.replace(/ /g, '-').toLowerCase());
                Util.successToast(entry.title + ' ' + 'deleted successfully'.t());

                var removableRec = Ext.getStore('reports').findRecord('uniqueId', entry.uniqueId);
                if (removableRec) {
                    Ext.getStore('reports').remove(removableRec); // remove record
                    Ext.getStore('reportstree').build(); // rebuild tree after save new
                }
            }, function (ex) {
                Util.handleException(ex);
            });
    },

    downloadGraph: function () {
        var view = this.getView(), vm = this.getViewModel(), now = new Date();
        try {
            this.getView().down('#graphreport').getController().chart.exportChart({
                filename: (vm.get('entry.category') + '-' + vm.get('entry.title') + '-' + Ext.Date.format(now, 'd.m.Y-Hi')).replace(/ /g, '_'),
                type: 'image/png'
            });
        } catch (ex) {
            console.log(ex);
            Util.handleException('Unable to download!');
        }
    },

    exportEventsHandler: function () {
        var me = this, vm = me.getViewModel(), entry = vm.get('entry').getData(), columns = [];
        if (!entry) { return; }

        var grid = me.getView().down('eventreport > ungrid');

        if (!grid) {
            console.log('Grid not found');
            return;
        }

        Ext.Array.each(grid.getColumns(), function (col) {
            if (col.dataIndex && !col.hidden) {
                columns.push(col.dataIndex);
            }
        });

        var conditions = [];
        Ext.Array.each(Ext.clone(vm.get('sqlFilterData')), function (cnd) {
            delete cnd._id;
            conditions.push(cnd);
        });

        Ext.MessageBox.wait('Exporting Events...'.t(), 'Please wait'.t());
        var downloadForm = document.getElementById('downloadForm');
        downloadForm['type'].value = 'eventLogExport';
        downloadForm['arg1'].value = (entry.category + '-' + entry.title + '-' + Ext.Date.format(new Date(), 'd.m.Y-Hi')).replace(/ /g, '_');
        downloadForm['arg2'].value = Ext.encode(entry);
        downloadForm['arg3'].value = conditions.length > 0 ? Ext.encode(conditions) : '';
        downloadForm['arg4'].value = columns.join(',');
        downloadForm['arg5'].value = vm.get('_sd') ? vm.get('_sd').getTime() : -1;
        downloadForm['arg6'].value = vm.get('_ed') ? vm.get('_ed').getTime() : -1;
        downloadForm.submit();
        Ext.MessageBox.hide();
    },

    exportGraphData: function (btn) {
        var me = this, vm = me.getViewModel(), entry = vm.get('entry').getData(), columns = [], headers = [];
        if (!entry) { return; }

        var grid = btn.up('grid'), csv = [];

        if (!grid) {
            console.log('Grid not found');
            return;
        }

        var processRow = function (row) {
            var data = [], j, innerValue;
            for (j = 0; j < row.length; j += 1) {
                innerValue = !row[j] ? '' : row[j].toString();
                data.push('"' + innerValue.replace(/"/g, '""') + '"');
            }
            return data.join(',') + '\r\n';
        };

        Ext.Array.each(grid.getColumns(), function (col) {
            if (col.dataIndex && !col.hidden) {
                columns.push(col.dataIndex);
                headers.push(col.text);
            }
        });
        csv.push(processRow(headers));

        grid.getStore().each(function (row, idx) {
            var r = [];
            for (j = 0; j < columns.length; j += 1) {
                if (columns[j] === 'time_trunc') {
                    r.push(Util.timestampFormat(row.get('time_trunc')));
                } else {
                    r.push(row.get(columns[j]));
                }
            }
            csv.push(processRow(r));
        });

        me.download(csv.join(''), (entry.category + '-' + entry.title + '-' + Ext.Date.format(new Date(), 'd.m.Y-Hi')).replace(/ /g, '_') + '.csv', 'text/csv');

    },

    download: function(content, fileName, mimeType) {
        var a = document.createElement('a');
        mimeType = mimeType || 'application/octet-stream';

        if (navigator.msSaveBlob) { // IE10
            return navigator.msSaveBlob(new Blob([ content ], {
                type : mimeType
            }), fileName);
        } else if ('download' in a) { // html5 A[download]
            a.href = 'data:' + mimeType + ',' + encodeURIComponent(content);
            a.setAttribute('download', fileName);
            document.body.appendChild(a);
            setTimeout(function() {
                a.click();
                document.body.removeChild(a);
            }, 100);
            return true;
        } else { //do iframe dataURL download (old ch+FF):
            var f = document.createElement('iframe');
            document.body.appendChild(f);
            f.src = 'data:' + mimeType + ',' + encodeURIComponent(content);
            setTimeout(function() {
                document.body.removeChild(f);
            }, 400);
            return true;
        }
    },

    setAutoRefresh: function (btn) {
        var me = this,
            vm = this.getViewModel();
        vm.set('autoRefresh', btn.pressed);

        if (btn.pressed) {
            me.refreshData();
            this.refreshInterval = setInterval(function () {
                me.refreshData();
            }, 5000);
        } else {
            clearInterval(this.refreshInterval);
        }

    },

});

Ext.define('Ung.view.reports.EntryModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.reports-entry',


    data: {
        startDate: new Date(Math.floor(rpc.systemManager.getMilliseconds()/1800000) * 1800000  - 3600 * 24 * 1000),
        endDate: new Date(Math.floor(rpc.systemManager.getMilliseconds()/1800000) * 1800000),
        tillNow: true,
        _currentData: [],
        sqlFilterData: [],
        autoRefresh: false
    },

    // stores: {
    //     _sqlConditionsStore: {
    //         data: '{_sqlConditions}',
    //         proxy: {
    //             type: 'memory',
    //             reader: {
    //                 type: 'json'
    //             }
    //         }
    //     }
    // },

    formulas: {
        _reportCard: function (get) {
            if (get('entry.type') === 'TEXT') { return 'textreport'; }
            if (get('entry.type') === 'EVENT_LIST') { return 'eventreport'; }
            return 'graphreport';
        },


        _approximation: {
            get: function (get) {
                return get('entry.approximation') || 'sum';
            },
            set: function (value) {
                this.set('entry.approximation', value !== 'sum' ? value : null);
            }
        },

        _sqlConditions: {
            get: function (get) {
               return get('entry.conditions') || [];
            },
            set: function (value) {
                this.set('entry.conditions', value);
                this.set('_sqlTitle', '<i class="fa fa-filter"></i> ' + 'Sql Conditions:'.t() + ' (' + value.length + ')');
               // return get('entry.conditions') || [];
            },
        },

        _props: function (get) {
            return get('entry').getData();
        },

        _colorsStr: {
            get: function (get) {
                if (get('entry.colors')) {
                    return get('entry.colors').join(',');
                } else {
                    return '';
                }
            },
            set: function (value) {
                var str = value.replace(/ /g, '');
                if (value.length > 0) {
                    this.set('entry.colors', value.split(','));
                } else {
                    this.set('entry.colors', null);
                }
            }
        },

        // _colors: {
        //     get: function (get) {
        //         return get('report.colors');
        //     },
        //     set: function (value) {
        //         console.log(this.get('report.colors'));
        //     }
        // },


        _sd: {
            get: function (get) {
                return get('startDate');
            },
            set: function (value) {
                var sd = new Date(this.get('startDate'));
                sd.setDate(value.getDate());
                sd.setMonth(value.getMonth());
                sd.setFullYear(value.getFullYear());
                this.set('startDate', sd);
            }
        },
        _st: {
            get: function (get) {
                return get('startDate');
            },
            set: function (value) {
                var sd = new Date(this.get('startDate'));
                sd.setHours(value.getHours());
                sd.setMinutes(value.getMinutes());
                this.set('startDate', sd);
            }
        },
        _ed: {
            get: function (get) {
                return get('endDate');
            },
            set: function (value) {
                var ed = new Date(this.get('endDate'));
                ed.setDate(value.getDate());
                ed.setMonth(value.getMonth());
                ed.setFullYear(value.getFullYear());
                this.set('endDate', ed);
            }
        },
        _et: {
            get: function (get) {
                return get('endDate');
            },
            set: function (value) {
                var ed = new Date(this.get('endDate'));
                ed.setHours(value.getHours());
                ed.setMinutes(value.getMinutes());
                this.set('endDate', ed);
            }
        },


        reportHeading: function (get) {
            if (get('entry.readOnly')) {
                return '<h2>' + get('entry.title').t() + '</h2><p>' + get('entry.description').t() + '</p>';
            }
            return '<h2>' + get('entry.title') + '</h2><p>' + get('entry.description') + '</p>';
        },
        // enableIcon: function (get) {
        //     return get('entry.enabled') ? 'fa-green' : 'fa-flip-horizontal fa-grey';
        // },

        isTimeGraph: function (get) {
            if (!get('entry.type')) {
                return false;
            }
            return get('entry.type') === 'TIME_GRAPH';
        },
        isTimeGraphDynamic: function (get) {
            if (!get('entry.type')) {
                return false;
            }
            return get('entry.type') === 'TIME_GRAPH_DYNAMIC';
        },
        isPieGraph: function (get) {
            if (!get('entry.type')) {
                return false;
            }
            return get('entry.type') === 'PIE_GRAPH';
        },

        isGraphEntry: function (get) {
            return get('isTimeGraph') || get('isTimeGraphDynamic') || get('isPieGraph');
        },

        isTextEntry: function (get)  {
            return get('entry.type') === 'TEXT';
        }
    }

});

Ext.define('Ung.view.reports.EventReport', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.eventreport',

    viewModel: {
        stores: {
            events: {
                data: '{eventsData}',
                listeners: {
                    datachanged: 'onDataChanged'
                }
            },
            props: {
                data: '{propsData}'
            }
        }
    },
    controller: 'eventreport',

    layout: 'border',

    border: false,
    bodyBorder: false,

    defaults: {
        border: false
    },

    items: [{
        xtype: 'ungrid',
        stateful: true,
        itemId: 'eventsGrid',
        reference: 'eventsGrid',
        region: 'center',
        bind: '{events}',
        plugins: ['gridfilters'],
        emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-2x"></i> <br/>No Records!</p>',
        enableColumnHide: true,
        listeners: {
            select: 'onEventSelect'
        }
    }, {
        xtype: 'unpropertygrid',
        itemId: 'eventsProperties',
        reference: 'eventsProperties',
        region: 'east',
        title: 'Details'.t(),
        collapsed: true,

        bind: {
            source: '{eventProperty}',
        }
    }]
});

Ext.define('Ung.view.reports.EventReportController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.eventreport',

    control: {
        '#': {
            afterrender: 'onAfterRender',
            deactivate: 'onDeactivate'
        }
    },

    onAfterRender: function () {
        var me = this, vm = this.getViewModel(), i;

        me.modFields = { uniqueId: null };

        // remove property grid if in dashboard
        if (me.getView().up('#dashboard')) {
            me.getView().down('unpropertygrid').hide();
        }

        vm.bind('{entry}', function (entry) {

            if (entry.get('type') !== 'EVENT_LIST') { return; }

            if (me.modFields.uniqueId !== entry.get('uniqueId')) {
                me.modFields = {
                    uniqueId: entry.get('uniqueId'),
                    defaultColumns: entry.get('defaultColumns')
                };

                var grid = me.getView().down('grid');
                var identifier = 'eventsGrid-' + entry.get('uniqueId');
                grid.itemId = identifier;
                grid.stateId = identifier;

                me.tableConfig = Ext.clone(TableConfig.getConfig(entry.get('table')));
                me.defaultColumns = vm.get('widget.displayColumns') || entry.get('defaultColumns'); // widget or report columns

                var visibleColumns = Ext.clone(me.defaultColumns);
                var currentStorage = Ext.state.Manager.provider.get(identifier);
                if( currentStorage ){
                    currentStorage.columns.forEach( function( column ){
                        if( ( column.hidden !== undefined ) &&
                            ( column.hidden === false ) ){
                            visibleColumns.push(column.id);
                        }
                    });
                }

                me.tableConfig.columns.forEach( function(column){
                    if( column.columns ){
                        /*
                         * Grouping
                         */
                        column.columns.forEach( Ext.bind( function( subColumn ){
                            grid.initComponentColumn( subColumn );
                        }, this ) );
                    }
                    grid.initComponentColumn( column );
                });
                me.tableConfig.fields.forEach( function(field){
                    if( !field.sortType ){
                        field.sortType = 'asUnString';
                    }
                });

                grid.tableConfig = me.tableConfig;
                grid.setColumns(me.tableConfig.columns);

                grid.getColumns().forEach( function(column){
                    if( column.xtype == 'actioncolumn'){
                        return;
                    }
                    column.setHidden( Ext.Array.indexOf(visibleColumns, column.dataIndex) < 0 );
                    if( column.columns ){
                        column.columns.forEach( Ext.bind( function( subColumn ){
                            subColumn.setHidden( Ext.Array.indexOf(visibleColumns, column.dataIndex) < 0 );
                        }, this ) );
                    }
                });
                // Force state processing for this renamed grid
                grid.mixins.state.constructor.call(grid);

                var propertygrid = me.getView().down('#eventsProperties');
                vm.set( 'eventProperty', null );
                propertygrid.fireEvent('beforerender');
                propertygrid.fireEvent('beforeexpand');

                if (!me.getView().up('reportwidget')) {
                    me.fetchData();
                } else {
                    me.isWidget = true;
                }
                return;
            }

        }, me, { deep: true });

        // clear grid selection (hide event side data) when settings are open
        vm.bind('{settingsBtn.pressed}', function (pressed) {
            if (pressed) {
                me.getView().down('grid').getSelectionModel().deselectAll();
            }
        });
    },

    onDeactivate: function () {
        this.modFields = { uniqueId: null };
        this.getViewModel().set('eventsData', []);
        this.getView().down('grid').getSelectionModel().deselectAll();
    },

    fetchData: function (reset, cb) {
        var me = this,
            v = me.getView(),
            vm = me.getViewModel();

        var treeNav;
        if (me.getView().up('#reports')) {
            treeNav = me.getView().up('#reports').down('treepanel');
        }

        var limit = 1000;
        if( me.getView().up('reports-entry') ){
            limit = me.getView().up('reports-entry').down('#eventsLimitSelector').getValue();
        }
        me.entry = vm.get('entry');

        var startDate = vm.get('startDate');
        var endDate = vm.get('tillNow') ? null : vm.get('endDate');
        if (!me.getView().renderInReports) { // if not rendered in reports than treat as widget
            startDate = new Date(rpc.systemManager.getMilliseconds() - (vm.get('widget.timeframe') || 3600 * 24) * 1000);
            endDate = new Date(rpc.systemManager.getMilliseconds());
        }

        var grid = v.down('grid');

        me.getViewModel().set('eventsData', []);
        if (treeNav) { treeNav.setDisabled(true); } // disable reports tree while data is fetched
        me.getView().setLoading(true);
        Rpc.asyncData('rpc.reportsManager.getEventsForDateRangeResultSet',
                        vm.get('entry').getData(), // entry
                        vm.get('sqlFilterData'), // etra conditions
                        limit,
                        startDate, // start date
                        endDate) // end date
            .then(function(result) {
                if (me.getView().up('reports-entry')) {
                    me.getView().up('reports-entry').down('#currentData').setLoading(false);
                }
                me.getView().setLoading(false);
                if (treeNav) { treeNav.setDisabled(false); }

                me.loadResultSet(result);

                if (cb) { cb(); }
            });
    },

    loadResultSet: function (reader) {
        var me = this,
            v = me.getView(),
            vm = me.getViewModel(),
            grid = me.getView().down('grid');

        this.getView().setLoading(true);
        grid.getStore().setFields( grid.tableConfig.fields );
        var eventData = [];
        var result = [];
        while( true ){
            result = reader.getNextChunk(1000);
            if(result && result.list && result.list.length){
                result.list.forEach(function(value){
                    eventData.push(value);
                });
                continue;
            }
            break;
        }
        reader.closeConnection();
        vm.set('eventsData', eventData);
        this.getView().setLoading(false);
    },

    onEventSelect: function (el, record) {
        var me = this, vm = this.getViewModel(), propsData = [];

        if (me.isWidget) { return; }

        Ext.Array.each(me.tableConfig.columns, function (column) {
            propsData.push({
                name: column.header,
                value: record.get(column.dataIndex)
            });
        });

        vm.set('propsData', propsData);
        // when selecting an event hide Settings if open
        me.getView().up('reports-entry').lookupReference('settingsBtn').setPressed(false);

    },

    onDataChanged: function(){
        var me = this,
            v = me.getView(),
            vm = me.getViewModel();

        if( vm.get('eventProperty') == null ){
            v.down('grid').getSelectionModel().select(0);
        }

        if( v.up().down('ungridstatus') ){
            v.up().down('ungridstatus').fireEvent('update');
        }
    }

});

Ext.define('Ung.view.reports.GraphReport', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.graphreport',

    controller: 'graphreport',
    viewModel: true,

    border: false,
    bodyBorder: false,

    items: [{
        xtype: 'component',
        reference: 'graph',
        cls: 'chart'
    }]
});

Ext.define('Ung.view.reports.GraphReportController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.graphreport',

    control: {
        '#': {
            afterrender: 'onAfterRender',
            resize: 'onResize'
        }
    },

    /**
     * initializes graph report
     */
    onAfterRender: function () {
        var me = this, vm = this.getViewModel(),
            modFields = { uniqueId: null }; // keeps some of the entry modified fields needed for dynamic styling

        // build the empty chart
        me.buildChart();


        if (!me.getView().up('reportwidget')) {
            // whatch when report entry is changed or modified

            vm.bind('{entry}', function (entry) {
                // if it's not a graph report, do nothing
                if (entry.get('type').indexOf('GRAPH') < 0) { return; }

                if (modFields.uniqueId !== entry.get('uniqueId')) {
                    modFields = {
                        uniqueId: entry.get('uniqueId'),
                        timeDataInterval: entry.get('timeDataInterval'),
                        pieNumSlices: entry.get('pieNumSlices'),
                        timeStyle: entry.get('timeStyle'),
                        pieStyle: entry.get('pieStyle'),
                        approximation: entry.get('approximation'),
                        colors: entry.get('colors')
                    };
                    // fetch report data first time
                    me.fetchData(true);
                    return;
                }

                // based on which fields are modified do some specific actions
                Ext.Object.each(modFields, function (key, value) {
                    if (key === 'uniqueId') { return; }
                    if (value !== entry.get(key)) {
                        modFields[key] = entry.get(key);
                        if (key === 'timeDataInterval') { me.fetchData(false); }
                        if (key === 'pieNumSlices') { me.setPieSeries(); }
                        if (Ext.Array.indexOf(['timeStyle', 'pieStyle', 'approximation'], key) >= 0) { me.setStyles(); }
                    }
                });
            }, me, {
                deep: true
            });
        } else {
            me.isWidget = true;
            // DashboardQueue.add(me.getView());
        }
    },

    /**
     * when container is resized the chart needs to adapt to the new size
     */
    onResize: function () {
        if (this.chart) {
            this.chart.reflow();
        }
    },

    /**
     * builds an empty chart (no data) and adds it to the container (this is done once)
     */
    buildChart: function () {
        var me = this, entry = me.getViewModel().get('entry'), widgetDisplay = me.getView().widgetDisplay;

        me.chart = new Highcharts.StockChart({
            chart: {
                type: 'spline',
                renderTo: me.getView().lookupReference('graph').getEl().dom,
                animation: false,
                spacing: widgetDisplay ? [5, 5, 10, 5] : [10, 10, 15, 10],
                style: { fontFamily: 'Source Sans Pro', fontSize: '12px' }
            },
            exporting: {
                enabled: false
            },
            navigator: { enabled: false },
            rangeSelector : { enabled: false },
            scrollbar: { enabled: false },
            credits: { enabled: false },
            title: {
                text: null
            },

            lang: { noData: '<i class="fa fa-info-circle fa-lg"></i><br/>No data!' },
            noData: {
                position: {
                    verticalAlign: 'top',
                    y: 20
                },
                style: {
                    fontFamily: 'Source Sans Pro',
                    padding: 0,
                    fontSize: '14px',
                    fontWeight: 'normal',
                    color: '#999',
                    textAlign: 'center'
                },
                useHTML: true
            },

            // colors: (me.entry.get('colors') !== null && me.entry.get('colors') > 0) ? me.entry.get('colors') : me.defaultColors,

            xAxis: {
                alternateGridColor: 'rgba(220, 220, 220, 0.1)',
                lineColor: '#C0D0E0',
                lineWidth: 1,
                tickLength: 3,
                gridLineWidth: 1,
                gridLineDashStyle: 'dash',
                gridLineColor: '#EEE',
                labels: {
                    style: {
                        color: '#333',
                        fontSize: '11px',
                        fontWeight: 600
                    },
                    y: 12
                },
                maxPadding: 0,
                minPadding: 0,
                events: {
                    // afterSetExtremes: function () {
                    //     // filters the current data grid based on the zoom range
                    //     if (me.getView().up('reports-entry')) {
                    //         me.getView().up('reports-entry').getController().filterData(this.getExtremes().min, this.getExtremes().max);
                    //     }
                    // }
                }
            },
            yAxis: {
                allowDecimals: true,
                min: 0,
                lineColor: '#C0D0E0',
                lineWidth: 1,
                gridLineWidth: 1,
                gridLineDashStyle: 'dash',
                gridLineColor: '#EEE',
                //tickPixelInterval: 50,
                tickLength: 5,
                tickWidth: 1,
                showFirstLabel: false,
                showLastLabel: true,
                endOnTick: true,
                // tickInterval: entry.get('units') === 'percent' ? 20 : undefined,
                maxPadding: 0,
                opposite: false,
                labels: {
                    align: 'right',
                    useHTML: true,
                    padding: 0,
                    style: {
                        color: '#333',
                        fontSize: '11px',
                        fontWeight: 600
                    },
                    x: -10,
                    y: 4
                },
                title: {
                    align: 'high',
                    offset: -10,
                    y: 3,
                    rotation: 0,
                    textAlign: 'left',
                    style: {
                        color: '#555',
                        fontSize: '12px',
                        fontWeight: 600
                    }
                }
            },
            tooltip: {
                enabled: true,
                animation: false,
                shared: true,
                // distance: 30,
                padding: 5,
                hideDelay: 0,
            },
            plotOptions: {
                column: {
                    depth: 25,
                    edgeWidth: 1,
                    edgeColor: '#FFF'
                },
                areaspline: {
                    lineWidth: 1,
                    tooltip: {
                        // split: true
                    }
                },
                spline: {
                    lineWidth: 2
                },
                pie: {
                    allowPointSelect: true,
                    cursor: 'pointer',
                    center: ['50%', '50%'],
                    showInLegend: true,
                    colorByPoint: true,

                    depth: 35,
                    minSize: 150,
                    borderWidth: 1,
                    borderColor: '#FFF',
                    dataLabels: {
                        enabled: true,
                        distance: 5,
                        padding: 0,
                        reserveSpace: false,
                        style: {
                            fontSize: '12px',
                            color: '#333',
                            // fontFamily: 'Source Sans Pro',
                            fontWeight: 600
                        },
                        formatter: function () {
                            if (this.point.percentage < 2) {
                                return null;
                            }
                            if (this.point.name.length > 25) {
                                return this.point.name.substring(0, 25) + '...';
                            }
                            return this.point.name + ' (' + this.point.percentage.toFixed(2) + '%)';
                        }
                    }
                },
                series: {
                    animation: false,
                    states: {
                        hover: {
                            lineWidthPlus: 0
                        }
                    },
                    marker: {
                        radius: 2,
                    }
                }
            },
            legend: {
                margin: 0,
                y: widgetDisplay ? 5 : 0,
                useHTML: true,
                lineHeight: 12,
                itemDistance: 10,
                itemStyle: {
                    fontSize: '11px',
                    fontWeight: 600,
                    width: '120px',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis'
                },
                symbolHeight: 8,
                symbolWidth: 8,
                symbolRadius: 4
            },
            series: []
        });
    },

    /**
     * fetches the report data
     */
    fetchData: function (reset, cb) {
        var me = this,
            vm = this.getViewModel(),
            entryType = vm.get('entry.type');

        var treeNav;
        if (me.getView().up('#reports')) {
            treeNav = me.getView().up('#reports').down('treepanel');
        }

        if (reset) {
            // if report entry changed, reset the chart first
            while(me.chart.series.length > 0) {
                me.chart.series[0].remove(true);
            }
            me.chart.update({
                xAxis: { visible: false },
                yAxis: { visible: false }
            });
            me.chart.redraw();
            me.chart.zoomOut();
        }

        if (treeNav) { treeNav.setDisabled(true); } // disable reports tree while data is fetched
        me.chart.showLoading('<i class="fa fa-spinner fa-spin fa-2x fa-fw"></i>');

        if (!me.getView().renderInReports) { // if not rendered in reports than treat as widget
            vm.set('startDate', new Date(rpc.systemManager.getMilliseconds() - (vm.get('widget.timeframe') || 3600) * 1000));
            vm.set('endDate', new Date(rpc.systemManager.getMilliseconds()));
        }

        Rpc.asyncData('rpc.reportsManager.getDataForReportEntry',
            vm.get('entry').getData(), // entry
            vm.get('startDate'), // start date
            vm.get('tillNow') ? null : vm.get('endDate'), // end date
            vm.get('sqlFilterData'), -1) // sql filters
            .then(function (result) {
                if (treeNav) { treeNav.setDisabled(false); }
                me.chart.hideLoading();
                me.data = result.list;

                // after data is fetched, generate chart series based on it's type
                if (entryType === 'TIME_GRAPH' || entryType === 'TIME_GRAPH_DYNAMIC') {
                    me.setTimeSeries();
                }
                if (entryType === 'PIE_GRAPH') {
                    me.setPieSeries();
                }

                if (cb) { cb(); }
                // if graph rendered inside reports, format and add data in current data grid
                if (!me.isWidget && me.getView().up('reports-entry')) {
                    // vm.set('_currentData', []);
                    var ctrl = me.getView().up('reports-entry').getController();
                    switch (entryType) {
                        case 'TIME_GRAPH':         ctrl.formatTimeData(me.data); break;
                        case 'TIME_GRAPH_DYNAMIC': ctrl.formatTimeDynamicData(me.data); break;
                        case 'PIE_GRAPH':          ctrl.formatPieData(me.data); break;
                    }
                } else {
                    // is widget
                    // DashboardQueue.next();
                    // console.log(me);
                    // Ext.defer(function () {
                    //     DashboardQueue.add(me);
                    // }, me.refreshIntervalSec * 1000);

                }
            });
    },

    /**
     * set chart series for the timeseries
     */
    setTimeSeries: function () {
        var me = this, vm = this.getViewModel(),
            timeDataColumns = Ext.clone(vm.get('entry.timeDataColumns')),
            colors = (vm.get('entry.colors') && vm.get('entry.colors').length > 0) ? vm.get('entry.colors') : Util.defaultColors,
            i, j, seriesData, series = [], seriesRenderer = null, column,
            units = vm.get('entry.units');

        if (!me.data) { return; }

        // get or generate series names based on time data columns
        if (!timeDataColumns) {
            timeDataColumns = [];
            for (i = 0; i < me.data.length; i += 1) {
                for (var _column in me.data[i]) {
                    if (me.data[i].hasOwnProperty(_column) && _column !== 'time_trunc' && _column !== 'time' && timeDataColumns.indexOf(_column) < 0) {
                        timeDataColumns.push(_column);
                    }
                }
            }

            if (!Ext.isEmpty(vm.get('entry.seriesRenderer'))) {
                seriesRenderer = Renderer[vm.get('entry.seriesRenderer')];
            }

        } else {
            for (i = 0; i < timeDataColumns.length; i += 1) {
                timeDataColumns[i] = timeDataColumns[i].split(' ').splice(-1)[0];
            }
        }

        // create series
        for (i = 0; i < timeDataColumns.length; i += 1) {
            column = timeDataColumns[i];
            seriesData = [];
            for (j = 0; j < me.data.length; j += 1) {
                seriesData.push([
                    me.data[j].time_trunc.time || me.data[j].time_trunc, // for sqlite is time_trunc, for postgres is time_trunc.time
                    me.data[j][column] || 0
                ]);
            }
            var renderedName = column;
            if( seriesRenderer ){
                renderedName = seriesRenderer(column);
                if(renderedName.substr(-1) != ']'){
                    renderedName += " [" + column + "]";
                }
            }
            series.push({
                name: renderedName,
                data: seriesData,
                fillColor: {
                    linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1 },
                    stops: [
                        [0, Highcharts.Color(colors[i]).setOpacity(0.7).get('rgba')],
                        [1, Highcharts.Color(colors[i]).setOpacity(0.1).get('rgba')]
                    ]
                },
                tooltip: {
                    pointFormatter: function () {
                        var str = '<span style="color: ' + this.color + '; font-weight: bold;">' + this.series.name + '</span>';
                        if (units === "bytes" || units === "bytes/s") {
                            str += ': <b>' + Util.bytesRenderer(this.y) + '</b>';
                        } else {
                            str += ': <b>' + this.y + '</b> ' + units;
                        }
                        return str + '<br/>';
                    }
                }
            });
        }

        // remove existing series
        while (this.chart.series.length > 0) {
            this.chart.series[0].remove(false);
        }

        // add series
        series.forEach(function (serie) {
            me.chart.addSeries(serie, false, false);
        });
        me.setStyles();
        me.chart.redraw();
    },

    /**
     * set serie fro the pie chart
     */
    setPieSeries: function () {
        var me = this, vm = this.getViewModel(), seriesName,
            slicesData = [], restValue = 0, seriesRenderer = null, i;

        if (!me.data) { return; }

        if (!Ext.isEmpty(vm.get('entry.seriesRenderer'))) {
            seriesRenderer = Renderer[vm.get('entry.seriesRenderer')];
        }

        for (i = 0; i < me.data.length; i += 1) {
            if (!seriesRenderer) {
                seriesName = me.data[i][vm.get('entry.pieGroupColumn')] !== undefined ? me.data[i][vm.get('entry.pieGroupColumn')] : 'None'.t();
            } else {
                seriesName = seriesRenderer(me.data[i][vm.get('entry.seriesRenderer')]);
            }

            if (i < vm.get('entry.pieNumSlices')) {
                slicesData.push({
                    name: seriesName,
                    y: me.data[i].value,
                });
            } else {
                restValue += me.data[i].value;
            }
        }

        if (restValue > 0) {
            slicesData.push({
                name: 'Others'.t(),
                color: '#DDD',
                y: restValue
            });
        }

        while(this.chart.series.length > 0) {
            this.chart.series[0].remove(false);
        }

        me.chart.addSeries({
            name: vm.get('entry.units').t(),
            data: slicesData,
            tooltip: {
                pointFormatter: function () {
                    var str = '<span style="color: ' + this.color + '; font-weight: bold;">' + this.series.name + '</span>';
                    if (vm.get('entry.units') === "bytes" || vm.get('entry.units') === "bytes/s") {
                        str += ': <b>' + Util.bytesRenderer(this.y) + '</b>';
                    } else {
                        str += ': <b>' + this.y + '</b>';
                    }
                    return str + '<br/>';
                }
            }
        }, false, false);

        me.setStyles();
        me.chart.redraw();
    },

    /**
     * returns the chart type (e.g. line, areaspline, column etc...) based on entry
     */
    setChartType: function (entry) {
        var type;

        if (entry.get('type') === 'TIME_GRAPH' || entry.get('type') === 'TIME_GRAPH_DYNAMIC') {
            switch (entry.get('timeStyle')) {
            case 'LINE':
                type = 'spline';
                break;
            case 'AREA':
            case 'AREA_STACKED':
                type = 'areaspline';
                break;
            case 'BAR':
            case 'BAR_3D':
            case 'BAR_OVERLAPPED':
            case 'BAR_3D_OVERLAPPED':
            case 'BAR_STACKED':
                type = 'column';
                break;
            default:
                type = 'areaspline';
            }
        }
        if (entry.get('type') === 'PIE_GRAPH') {
            if (entry.get('pieStyle').indexOf('COLUMN') >= 0) {
                type = 'column';
            } else {
                type = 'pie';
            }
        }
        return type;
    },

    /**
     * sets/updates the chart styles based on entry and data
     */
    setStyles: function () {
        var me = this, entry = this.getViewModel().get('entry'),
        widgetDisplay = me.getView().widgetDisplay,

        isTimeColumn = false, isColumnStacked = false, isColumnOverlapped = false,
        isPieColumn = false, isDonut = false, isPie = false, is3d = false;

        var isPieGraph = entry.get('type') === 'PIE_GRAPH';
        var isTimeGraph = entry.get('type').indexOf('TIME_GRAPH') >= 0;

        if (entry.get('timeStyle')) {
            isTimeColumn = entry.get('timeStyle').indexOf('BAR') >= 0;
            isColumnStacked = entry.get('timeStyle').indexOf('STACKED') >= 0;
            isColumnOverlapped = entry.get('timeStyle').indexOf('OVERLAPPED') >= 0;
        }

        if (entry.get('pieStyle')) {
            isPieColumn = entry.get('pieStyle').indexOf('COLUMN') >= 0;
            isPie = entry.get('pieStyle').indexOf('COLUMN') < 0;
            isDonut = entry.get('pieStyle').indexOf('DONUT') >= 0;
            is3d = entry.get('pieStyle').indexOf('3D') >= 0;
        }

        var colors = Ext.clone(entry.get('colors')) || Ext.clone(Util.defaultColors);

        if (colors) {
            for (var i = 0; i < colors.length; i += 1) {
                colors[i] = isTimeGraph ? ( isColumnOverlapped ? new Highcharts.Color(colors[i]).setOpacity(0.5).get('rgba') : new Highcharts.Color(colors[i]).setOpacity(0.7).get('rgba')) : colors[i];
            }
            // add gradient
            if ((isPie || isDonut) && !is3d) {
                colors = Highcharts.map( colors, function (color) {
                    return {
                        radialGradient: {
                            cx: 0.5,
                            cy: 0.5,
                            r: 0.7
                        },
                        stops: [
                            [0, Highcharts.Color(color).setOpacity(0.4).get('rgba')],
                            [1, Highcharts.Color(color).setOpacity(0.8).get('rgba')]
                        ]
                    };
                });
            }

        }

        me.chart.update({
            chart: {
                type: me.setChartType(entry),
                zoomType: isTimeGraph ? 'x' : undefined,
                panning: isTimeGraph,
                panKey: 'ctrl',
                options3d: {
                    enabled: is3d,
                    alpha: isPieColumn ? 30 : 50,
                    beta: isPieColumn ? 5 : 0
                }
            },
            colors: colors,
            // scrollbar: {
            //     enabled: isTimeGraph
            // },
            plotOptions: {
                series: {
                    stacking: isColumnStacked ? 'normal' : undefined,
                    dataGrouping: isTimeGraph ? { approximation: entry.get('approximation') || 'sum' } : undefined
                },
                // pie graphs
                pie: {
                    innerSize: isDonut ? '40%' : 0,
                    //borderColor: '#666666'
                },
                // time graphs
                spline: {
                    shadow: true,
                    dataGrouping: {
                        groupPixelWidth: 8
                    },
                },
                // time graphs
                areaspline: {
                    // shadow: true,
                    // fillOpacity: 0.3,
                    dataGrouping: {
                        groupPixelWidth: 8
                    },
                },
                column: {
                    borderWidth: isColumnOverlapped ? 1 : 0,
                    pointPlacement: isTimeGraph ? 'on' : undefined, // time
                    colorByPoint: isPieColumn, // pie
                    grouping: !isColumnOverlapped,
                    groupPadding: 0.20,
                    // shadow: !isColumnOverlapped,
                    shadow: false,
                    dataGrouping: isTimeGraph ? { groupPixelWidth: isColumnStacked ? 50 : 80 } : undefined
                }
            },
            xAxis: {
                visible: !isPie,
                type: isTimeGraph ? 'datetime' : 'category',
                crosshair: (isTimeGraph && !isTimeColumn) ? {
                    width: 1,
                    dashStyle: 'ShortDot',
                    color: 'rgba(100, 100, 100, 0.5)'
                } : false
                // crosshair: {
                //     width: 1,
                //     dashStyle: 'ShortDot',
                //     color: 'rgba(100, 100, 100, 0.5)'
                // },
            },
            yAxis: {
                visible: !isPie,
                minRange: entry.get('units') === 'percent' ? 100 : 1,
                maxRange: entry.get('units') === 'percent' ? 100 : undefined,
                labels: {
                    formatter: function() {
                        var finalVal = this.value;

                        if (entry.get('units') === 'bytes/s') {
                            finalVal = Util.bytesToHumanReadable(this.value, true);
                            /*
                            if (this.isLast) {
                                return '<span style="color: #555; font-size: 12px;"><strong>' + finalVal + '</strong> (per second)</span>';
                            }
                            */
                        } else {
                            /*
                            if (this.isLast) {
                                return '<span style="color: #555; font-size: 12px;"><strong>' + this.value + '</strong> (' + entry.get('units') + ')</span>';
                            }
                            */
                        }
                        return finalVal;
                    }
                },
                title: {
                    text: entry.get('units')
                }
            },
            legend: {
                enabled: !(widgetDisplay && isPie),
                layout: isPie ? 'vertical' : 'horizontal',
                align: isPie ? 'left' : 'center',
                verticalAlign: isPie ? 'top' : 'bottom'
            }
            // tooltip: {
            //     split: isTimeGraph && !isTimeColumn
            // }
        });

        if (entry.get('timeStyle') === 'BAR_OVERLAPPED' || entry.get('timeStyle') === 'BAR_3D_OVERLAPPED') {
            Ext.Array.each(me.chart.series, function (serie, idx) {
                serie.update({
                    pointPadding: (me.chart.series.length <= 3 ? 0.1 : 0.075) * idx
                }, false);
            });
            me.chart.redraw();
        } else {
            Ext.Array.each(me.chart.series, function (serie, idx) {
                serie.update({
                    pointPadding: 0.1
                }, false);
            });
            me.chart.redraw();
        }
    }

});

Ext.define('Ung.view.reports.Reports', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.reports',
    itemId: 'reports',

    layout: 'border',

    controller: 'reports',

    viewModel: true,

    // tbar: [{
    //     xtype: 'component',
    //     bind: {
    //         html: '{categoryName} | {categories.selection} | {reportName} | {report}'
    //     }
    // }],

    dockedItems: [{
        xtype: 'toolbar',
        ui: 'navigation',
        dock: 'top',
        border: false,
        hidden: true,
        bind: {
            hidden: '{ servlet !== "ADMIN" }'
        },
        style: {
            background: '#333435',
            zIndex: 9997
        },
        defaults: {
            xtype: 'button',
            border: false,
            hrefTarget: '_self'
        },
        items: Ext.clone(Util.subNav)

        // to do investigate breadcrumbs
        // items: Ext.Array.insert(Ext.clone(Util.subNav), 0, [{
        //     xtype: 'breadcrumb',
        //     reference: 'breadcrumb',
        //     defaults: {
        //         border: false
        //     },
        //     listeners: {
        //         selectionchange: function (el, node) {
        //             if (node) {
        //                 if (node.get('url')) {
        //                     Ung.app.redirectTo('#reports/' + node.get('url'));
        //                 } else {
        //                     Ung.app.redirectTo('#reports');
        //                 }
        //             }
        //         }
        //     }
        // }])
    }],

    items: [{
        xtype: 'treepanel',
        reference: 'tree',
        width: 250,
        minWidth: 200,
        region: 'west',
        split: true,
        border: false,
        // singleExpand: true,
        useArrows: true,
        rootVisible: false,

        store: 'reportstree',

        viewConfig: {
            selectionModel: {
                type: 'treemodel',
                pruneRemoved: false
            }
        },

        dockedItems: [{
            xtype: 'toolbar',
            border: false,
            dock: 'top',
            cls: 'report-header',
            height: 53,
            padding: '0 10',
            items: [{
                xtype: 'component',
                html: '<h2>Select Report</h2><p>Find or select a report</p>'
            }]
        }, {
            xtype: 'textfield',
            margin: '1',
            emptyText: 'Filter reports ...',
            enableKeyEvents: true,
            flex: 1,
            triggers: {
                clear: {
                    cls: 'x-form-clear-trigger',
                    hidden: true,
                    handler: 'onTreeFilterClear'
                }
            },
            listeners: {
                change: 'filterTree',
                buffer: 100
            }
        }],

        columns: [{
            xtype: 'treecolumn',
            flex: 1,
            dataIndex: 'text',
            // scope: 'controller',
            renderer: 'treeNavNodeRenderer'
        }],

        listeners: {
            select: function (el, node) {
                if (Ung.app.servletContext === 'reports') {
                    Ung.app.redirectTo('#' + node.get('url'));
                } else {
                    Ung.app.redirectTo('#reports/' + node.get('url'));
                }
            }
        }
    }, {
        region: 'center',
        itemId: 'cards',
        reference: 'cards',
        border: false,
        layout: 'card',
        cls: 'reports-all',
        defaults: {
            border: false,
            bodyBorder: false
        },
        items: [{
            itemId: 'category',
            layout: { type: 'vbox', align: 'stretch' },
            items: [{
                xtype: 'component',
                padding: '20px 0',
                cls: 'charts-bar',
                html: '<i class="fa fa-area-chart fa-2x"></i>' +
                    '<i class="fa fa-line-chart fa-2x"></i>' +
                    '<i class="fa fa-pie-chart fa-2x"></i>' +
                    '<i class="fa fa-bar-chart fa-2x"></i>' +
                    '<i class="fa fa-list-ul fa-2x"></i>' +
                    '<i class="fa fa-align-left fa-2x"></i>'
            }, {
                xtype: 'container',
                cls: 'stats',
                layout: { type: 'hbox', pack: 'center' },
                defaults: {
                    xtype: 'component',
                    cls: 'stat'
                },
                items: [{
                    hidden: true,
                    bind: {
                        hidden: '{tree.selection}',
                        html: '<h1>{stats.categories.total}</h1>categories <p><span>{stats.categories.app} apps</span></p>'
                    }
                }, {
                    hidden: true,
                    bind: {
                        hidden: '{!tree.selection}',
                        html: '<img src="{tree.selection.icon}"><br/> {tree.selection.text}'
                    }
                }, {
                    bind: {
                        html: '<h1>{stats.reports.total}</h1>reports' +
                            '<p><span>{stats.reports.chart} charts</span><br/>' +
                            '<span>{stats.reports.event} event lists</span><br/>' +
                            '<span>{stats.reports.info} summaries</span><br/>' +
                            '<span>{stats.reports.custom} custom reports</span></p>'

                    }
                }]
            }, {
                xtype: 'component',
                cls: 'pls',
                html: 'select a report from a category',
                bind: {
                    html: 'select a report from {tree.selection.text || "a category"}'
                }
            }]
        }, {
            xtype: 'reports-entry',
            itemId: 'report',
            bind: {
                html: '<h3>{selectedReport.localizedTitle}</h3>',
            }
        }]
    }]
});

Ext.define('Ung.view.reports.ReportsController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.reports',

    control: {
        '#': { afterrender: 'onAfterRender', deactivate: 'resetView' }
    },

    listen: {
        global: {
            init: 'onInit'
        }
    },

    onAfterRender: function () {
        this.getView().setLoading(true);
    },

    onInit: function () {
        var me = this, vm = me.getViewModel(), path = '';
        me.getView().setLoading(false);

        vm.bind('{hash}', function (hash) {
            if (!hash) { me.resetView(); return; }

            if (Ung.app.servletContext === 'reports') {
                path = '/reports/' + window.location.hash.replace('#', '');
            } else {
                path = window.location.hash.replace('#', '');
            }
            me.lookup('tree').collapseAll();
            me.lookup('tree').selectPath(path, 'slug', '/', me.selectPath, me);
        });
        me.buildStats();
    },

    /**
     * selects a tree node (report or category) based on location hash
     * and updates viewmodel with the report entry
     */
    selectPath: function (success, node) {
        var me = this, vm = me.getViewModel(), record;

        if (!success) { console.log('error'); return; }

        if (node.isLeaf()) {
            // report node
            record = Ext.getStore('reports').findRecord('url', node.get('url'));
            if (record) {
                vm.set({
                    report: record, // main reference from the store
                    entry: record.copy(null) // report reference copy on which modifications are made
                });
            }
            me.lookup('cards').setActiveItem('report');
        } else {
            me.lookup('cards').setActiveItem('category');
            me.buildStats();
            node.expand();
        }

        // me.lookup('breadcrumb').setSelection(node);
    },

    /**
     * the tree item renderer used after filtering tree
     */
    treeNavNodeRenderer: function(value, meta, record) {
        // if (!record.isLeaf()) {
        //     meta.tdCls = 'x-tree-category';
        // }
        // if (!record.get('readOnly') && record.get('uniqueId')) {
        //     meta.tdCls = 'x-tree-custom-report';
        // }
        return this.rendererRegExp ? value.replace(this.rendererRegExp, '<span style="font-weight: bold; background: #EEE; color: #000; border-bottom: 1px #000 solid;">$1</span>') : value;
    },

    /**
     * filters reports tree
     */
    filterTree: function (field, value) {
        var me = this, tree = me.lookup('tree');
        me.rendererRegExp = new RegExp('(' + value + ')', 'gi');

        if (!value) {
            tree.getStore().clearFilter();
            tree.collapseAll();
            field.getTrigger('clear').hide();
            return;
        }

        tree.getStore().getFilters().replaceAll({
            property: 'text',
            value: new RegExp(Ext.String.escape(value), 'i')
        });
        tree.expandAll();
        field.getTrigger('clear').show();
    },

    onTreeFilterClear: function () {
        this.lookup('tree').down('textfield').setValue();
    },

    /**
     * resets the view to an initial state
     */
    resetView: function () {
        var me = this, tree = me.lookup('tree');
        tree.collapseAll();
        tree.getSelectionModel().deselectAll();
        tree.getStore().clearFilter();
        tree.down('textfield').setValue('');

        me.buildStats();
        me.lookup('cards').setActiveItem('category');
        me.getViewModel().set('hash', null);
    },

    /**
     * builds statistics for categories
     */
    buildStats: function () {
        var me = this, vm = me.getViewModel(), tree = me.lookup('tree'), selection,
        stats = {
            set: false,
            reports: {
                total: 0,
                custom: 0,
                chart: 0,
                event: 0,
                info: 0
            },
            categories: {
                total: 0,
                app: 0
            }
        };

        if (tree.getSelection().length === 0) {
            selection = tree.getRootNode();
        } else {
            selection = tree.getSelection()[0];
        }

        selection.cascadeBy(function (node) {
            if (node.isRoot()) { return; }
            if (node.isLeaf()) {
                stats.reports.total += 1;
                if (!node.get('readOnly')) { stats.reports.custom += 1; }
                switch(node.get('type')) {
                    case 'TIME_GRAPH':
                    case 'TIME_GRAPH_DYNAMIC':
                    case 'PIE_GRAPH':
                        stats.reports.chart += 1; break;
                    case 'EVENT_LIST':
                        stats.reports.event += 1; break;
                    case 'TEXT':
                        stats.reports.info += 1; break;
                }
            } else {
                stats.categories.total += 1;
                if (node.get('type') === 'app') {
                    stats.categories.app += 1;
                }
            }
        });
        vm.set('stats', stats);
        // vm.notify();
    },

    // breadcrumbSelection: function (el, node) {
    //     console.log(node);
    // }
});

Ext.define('Ung.view.reports.TextReport', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.textreport',

    viewModel: true,
    controller: 'textreport',

    border: false,
    bodyBorder: false,

    padding: 10,

    style: {
        fontSize: '14px'
    }
});

Ext.define('Ung.view.reports.TextReportController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.textreport',

    control: {
        '#': {
            beforerender: 'onBeforeRender',
            deactivate: 'onDeactivate'
        }
    },

    onBeforeRender: function () {
        var me = this, vm = this.getViewModel();

        vm.bind('{entry}', function (entry) {
            if (entry.get('type') !== 'TEXT') {
                return;
            }
            if (!me.getView().up('reportwidget')) {
                me.fetchData();
            } else {
                me.isWidget = true;
            }
        });
    },

    onDeactivate: function () {
        this.getView().setHtml('');
    },

    fetchData: function (reset, cb) {
        var me = this, vm = this.getViewModel();
        me.entry = vm.get('entry');

        var treeNav;
        if (me.getView().up('#reports')) {
            treeNav = me.getView().up('#reports').down('treepanel');
        }

        if (!me.getView().renderInReports) { // if not rendered in reports than treat as widget
            vm.set('startDate', new Date(rpc.systemManager.getMilliseconds() - (vm.get('widget.timeframe') || 3600 * 24) * 1000));
            vm.set('endDate', new Date(rpc.systemManager.getMilliseconds()));
        }

        if (treeNav) { treeNav.setDisabled(true); } // disable reports tree while data is fetched
        me.getView().setLoading(true);
        Rpc.asyncData('rpc.reportsManager.getDataForReportEntry',
            vm.get('entry').getData(), // entry
            vm.get('startDate'), // start date
            vm.get('tillNow') ? null : vm.get('endDate'), // end date
            vm.get('sqlFilterData'), -1) // sql filters
            .then(function(result) {
                me.getView().setLoading(false);
                if (treeNav) { treeNav.setDisabled(false); }
                me.processData(result.list);
                if (me.getView().up('reports-entry')) {
                    me.getView().up('reports-entry').getController().formatTextData(result.list);
                }

                if (cb) { cb(); }

            });
    },

    processData: function (data) {

        var v = this.getView(),
            vm = this.getViewModel(),
            textColumns = vm.get('entry.textColumns'), i, columnName, values = [];

        if (data.length > 0 && textColumns && textColumns.length > 0) {
            Ext.Array.each(textColumns, function (column) {
                columnName = column.split(' ').splice(-1)[0];
                values.push(data[0][columnName] || 0);
            });

            v.setHtml(Ext.String.format.apply(Ext.String.format, [vm.get('entry.textString')].concat(values)));
            // todo: send data to the datagrid for TEXT report
        }
    }
});
