Ext.define('Ung.view.reports.Reports', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.reports',
    itemId: 'reports',

    layout: 'border',

    controller: 'reports',

    viewModel: {
        data: {
            fetching: false,
            selection: null
        }
    },

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        style: {
            zIndex: 9997
        },

        padding: 5,
        plugins: 'responsive',
        items: [{
            xtype: 'breadcrumb',
            reference: 'breadcrumb',
            store: 'reportstree',
            useSplitButtons: false,
            listeners: {
                selectionchange: function (el, node) {
                    if (!node.get('url')) { return; }
                    if (node) {
                        if (node.get('url')) {
                            Ung.app.redirectTo('#reports/' + node.get('url'));
                        } else {
                            Ung.app.redirectTo('#reports');
                        }
                    }
                }
            }
        }],
        responsiveConfig: {
            wide: { hidden: true },
            tall: { hidden: false }
        }
    }, {
        xtype: 'toolbar',
        itemId: 'actionsToolbar',
        ui: 'footer',
        dock: 'bottom',
        // border: true,
        style: {
            background: '#F5F5F5'
        },
        // hidden: true,
        // bind: {
        //     hidden: '{!entry}'
        // },
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
        }, '->', {
            xtype: 'component',
            html: '<i class="fa fa-spinner fa-spin fa-fw fa-lg"></i>',
            hidden: true,
            bind: {
                hidden: '{!fetching}'
            }
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
        }, {
            itemId: 'downloadBtn',
            text: 'Download'.t(),
            iconCls: 'fa fa-download',
            handler: 'downloadGraph',
            hidden: true,
            disabled: true,
            bind: {
                hidden: '{!isGraphEntry}',
                disabled: '{fetching}'
            }
        }, '-', {
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
        }]
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
        plugins: 'responsive',
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
        }, {
            xtype: 'toolbar',
            dock: 'bottom',
            ui: 'footer',
            layout: 'fit',
            items: [{
                xtype: 'button',
                iconCls: 'fa fa-magic',
                scale: 'medium',
                text: 'Create New Report'.t(),
                handler: 'newReportWizard'
            }]
        }],

        columns: [{
            xtype: 'treecolumn',
            flex: 1,
            dataIndex: 'text',
            // scope: 'controller',
            renderer: 'treeNavNodeRenderer'
        }],

        listeners: {
            beforeselect: 'beforeSelectReport'
        },

        responsiveConfig: {
            wide: { hidden: false },
            tall: { hidden: true }
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
        activeItem: 'graphreport',
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
                        hidden: '{selection}',
                        html: '<h1>{stats.categories.total}</h1>categories <p><span>{stats.categories.app} apps</span></p>'
                    }
                }, {
                    hidden: true,
                    bind: {
                        hidden: '{!selection}',
                        html: '<img src="{selection.icon}"><br/> {selection.text}'
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
                    html: 'select a report from {selection.text || "a category"}'
                }
            }]
        }, {
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
        }]
    }],
});
