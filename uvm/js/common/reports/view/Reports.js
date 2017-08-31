Ext.define('Ung.view.reports.Reports', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.reports',
    itemId: 'reports',

    layout: 'border',

    controller: 'reports',

    viewModel: {
        data: {
            fetching: false
        }
    },

    // tbar: [{
    //     xtype: 'component',
    //     bind: {
    //         html: '{categoryName} | {categories.selection} | {reportName} | {report}'
    //     }
    // }],

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
            listeners: {
                selectionchange: function (el, node) {
                    if (!node.get('slug')) { return; }
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
