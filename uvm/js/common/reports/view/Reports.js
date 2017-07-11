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
