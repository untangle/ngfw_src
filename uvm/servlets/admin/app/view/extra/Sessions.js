Ext.define('Ung.view.extra.Sessions', {
    extend: 'Ext.panel.Panel',
    // extend: 'Ext.grid.Panel',
    xtype: 'ung.sessions',
    // layout: 'border',
    /* requires-start */
    requires: [
        'Ung.view.extra.SessionsController'
    ],
    /* requires-end */
    controller: 'sessions',

    viewModel: {
        data: {
            autoRefresh: false
        }
    },

    layout: 'border',

    dockedItems: [{
        xtype: 'toolbar',
        ui: 'navigation',
        dock: 'top',
        border: false,
        style: {
            background: '#333435',
            zIndex: 9997
        },
        defaults: {
            xtype: 'button',
            border: false,
            hrefTarget: '_self'
        },
        items: Ext.Array.insert(Ext.clone(Util.subNav), 0, [{
            xtype: 'component',
            margin: '0 0 0 10',
            style: {
                color: '#CCC'
            },
            html: 'Current Sessions'.t()
        }])
    }],

    defaults: {
        border: false
    },

    // title: 'Current Sessions'.t(),

    items: [{
        region: 'center',
        xtype: 'ungrid',
        itemId: 'sessionsgrid',
        reference: 'sessionsgrid',
        store: 'sessions',
        stateful: true,

        enableColumnHide: true,
        forceFit: false,
        viewConfig: {
            stripeRows: true,
            enableTextSelection: true
        },

        plugins: ['gridfilters'],
        columnLines: true,

        features: [{
            ftype: 'grouping'
        }],

        columns: [{
            header: 'Creation Time'.t(),
            dataIndex: 'creationTime',
            hidden: true,
            renderer: function(value) {
                return value ? Util.timestampFormat(value) : '';
            }
        }, {
            header: 'Protocol'.t(),
            dataIndex: 'protocol',
            width: 70,
            filter: {
                type: 'list',
                options: ['TCP', 'UDP']
            }
        }, {
            header: 'Bypassed'.t(),
            dataIndex: 'bypassed',
            width: 70,
            filter: {
                type: 'boolean',
                yesText: 'true',
                noText: 'false'
            },
            rtype: 'boolean'
        }, {
            header: 'Policy'.t(),
            dataIndex: 'policy',
            hidden: true,
            filter: {
                type: 'string' // should be list
            }
        }, {
            header: 'Hostname'.t(),
            dataIndex: 'hostname',
            width: 100,
            filter: { type: 'string' }
        }, {
            header: 'NATd'.t(),
            dataIndex: 'natted',
            filter: {
                type: 'boolean',
                yesText: 'true',
                noText: 'false'
            },
            hidden: true
        }, {
            header: 'Port Forwarded'.t(),
            dataIndex: 'portForwarded',
            filter: {
                type: 'boolean',
                yesText: 'true',
                noText: 'false'
            },
            hidden: true
        }, {
            header: 'Hostname'.t(),
            dataIndex: 'platform-hostname',
            hidden: true
        }, {
            header: 'Username'.t(),
            dataIndex: 'platform-username',
            hidden: true
        }, {
            header: 'Tags'.t(),
            dataIndex: 'tags',
            rtype: 'tags'
        }, {
            header: 'Tags String'.t(),
            dataIndex: 'tagsString',
            filter: { type: 'string' },
            hidden: true
        }, {
            hidden: true,
            header: 'Local Address'.t(),
            dataIndex: "localAddr",
            filter: { type: 'string' }
        },{
            hidden: true,
            header: 'Remote Address'.t(),
            dataIndex: "remoteAddr",
            filter: { type: 'string' }
        },{
            hidden: true,
            header: 'Priority'.t() + " " + "(Bandwidth Control)",
            dataIndex: "priority",
            // renderer: function(value) {
            //     return (value < 1 || value > 7)?i18n._("None"):priorityList[value-1];
            // },
            // filter: {
            //     type: 'list',
            //     store: priorityOptionsStore
            // }
        },{
            hidden: true,
            header: 'Priority'.t() + " (QoS)",
            dataIndex: "qosPriority",
            // renderer: function(value) {
            //     return (value < 1 || value > 7)?i18n._("None"):priorityList[value-1];
            // },
            // filter: {
            //     type: 'list',
            //     store: priorityOptionsStore
            // }
        },{
            hidden: true,
            header: 'Pipeline'.t(),
            dataIndex: "pipeline",
            filter: { type: 'string' }
        }, {
            header: 'Client'.t(),
            columns: [{
                header: 'Interface'.t(),
                dataIndex: 'clientIntf',
                filter: { type: 'string' },
                rtype: 'interface'
            }, {
                header: 'Pre-NAT'.t(),
                dataIndex: 'preNatClient',
                filter: { type: 'string' }
            }, {
                header: 'Port (Pre-NAT)'.t(),
                dataIndex: 'preNatClientPort',
                filter: { type: 'numeric' }
            }, {
                header: 'Post-NAT'.t(),
                dataIndex: 'postNatClient',
                filter: { type: 'string' },
                hidden: true
            }, {
                header: 'Port (Post-NAT)'.t(),
                dataIndex: 'postNatClientPort',
                filter: { type: 'numeric' },
                hidden: true
            }, {
                header: 'Country'.t(),
                dataIndex: 'clientCountry',
                filter: { type: 'string' },
                hidden: true
            }, {
                header: 'Latitude'.t(),
                dataIndex: 'clientLatitude',
                filter: { type: 'numeric' },
                hidden: true
            }, {
                header: 'Longitude'.t(),
                dataIndex: 'clientLongitude',
                filter: { type: 'numeric' },
                hidden: true
            }]
        }, {
            header: 'Server'.t(),
            columns: [{
                header: 'Interface'.t(),
                dataIndex: 'serverIntf',
                filter: { type: 'string' },
                rtype: 'interface'
            }, {
                header: 'Pre-NAT'.t(),
                dataIndex: 'preNatServer',
                filter: { type: 'string' },
                hidden: true
            }, {
                header: 'Port (Pre-NAT)'.t(),
                dataIndex: 'preNatServerPort',
                filter: { type: 'numeric' },
                hidden: true
            }, {
                header: 'Post-NAT'.t(),
                dataIndex: 'postNatServer',
                filter: { type: 'string' }
            }, {
                header: 'Port (Post-NAT)'.t(),
                dataIndex: 'postNatServerPort',
                filter: { type: 'numeric' }
            }, {
                header: 'Country'.t(),
                dataIndex: 'serverCountry',
                filter: { type: 'string' },
                hidden: true
            }, {
                header: 'Latitude'.t(),
                dataIndex: 'serverLatitude',
                filter: { type: 'numeric' },
                hidden: true
            }, {
                header: 'Longitude'.t(),
                dataIndex: 'serverLlongitude',
                filter: { type: 'numeric' },
                hidden: true
            }]
        }, {
            header: 'Speed (KB/s)'.t(),
            columns: [{
                header: 'Client'.t(),
                dataIndex: 'clientKBps',
                filter: 'number',
                align: 'right'
            }, {
                header: 'Server'.t(),
                dataIndex: 'serverKBps',
                filter: 'number',
                align: 'right'
            }, {
                header: 'Total'.t(),
                dataIndex: 'totalKBps',
                filter: 'number',
                align: 'right'
            }]
        }, {
            header: 'Application Control Lite'.t(),
            hidden: true,
            columns: [{
                hidden: true,
                header: 'Protocol'.t(),
                dataIndex: "application-control-lite-protocol",
                filter: { type: 'string' }
            },{
                hidden: true,
                header: 'Category'.t(),
                dataIndex: "application-control-lite-category",
                filter: { type: 'string' }
            },{
                hidden: true,
                header: 'Description'.t(),
                dataIndex: "application-control-lite-description",
                filter: { type: 'string' }
            },{
                hidden: true,
                header: 'Matched?'.t(),
                dataIndex: "application-control-lite-matched",
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            }]
        }, {
            header: 'Application Control'.t(),
            columns: [{
                hidden: true,
                header: 'Protochain'.t(),
                dataIndex: "application-control-protochain",
                filter: { type: 'string' }
            },{
                hidden: true,
                header: 'Application'.t(),
                dataIndex: "application-control-application",
                filter: { type: 'string' }
            },{
                hidden: true,
                header: 'Category'.t(),
                dataIndex: "application-control-category",
                filter: { type: 'string' }
            },{
                hidden: true,
                header: 'Detail'.t(),
                dataIndex: "application-control-detail",
                filter: { type: 'string' }
            },{
                hidden: true,
                header: 'Confidence'.t(),
                dataIndex: "application-control-confidence",
                filter: { type: 'string' }
            },{
                hidden: true,
                header: 'Productivity'.t(),
                dataIndex: "application-control-productivity",
                filter: { type: 'string' }
            },{
                hidden: true,
                header: 'Risk'.t(),
                dataIndex: "application-control-risk",
                filter: { type: 'string' }
            }]
        }, {
            header: 'Web Filter'.t(),
            hidden: true,
            columns: [{
                hidden: true,
                header: 'Category Name'.t(),
                dataIndex: "web-filter-best-category-name",
                filter: { type: 'string' }
            },{
                hidden: true,
                header: 'Category Description'.t(),
                dataIndex: "web-filter-best-category-description",
                filter: { type: 'string' }
            },{
                hidden: true,
                header: 'Category Flagged'.t(),
                dataIndex: "web-filter-best-category-flagged",
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            },{
                hidden: true,
                header: 'Category Blocked'.t(),
                dataIndex: "web-filter-best-category-blocked",
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            },{
                hidden: true,
                header: 'Content Type'.t(),
                dataIndex: "web-filter-content-type",
                filter: { type: 'string' }
            },{
                hidden: true,
                header: 'Flagged'.t(),
                dataIndex: "web-filter-flagged",
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            }]
        }, {
            header: 'HTTP'.t(),
            hidden: true,
            columns: [{
                hidden: true,
                header: 'Hostname'.t(),
                dataIndex: "http-hostname",
                filter: { type: 'string' }
            },{
                hidden: true,
                header: 'URL'.t(),
                dataIndex: "http-url",
                filter: { type: 'string' }
            },{
                hidden: true,
                header: 'User Agent'.t(),
                dataIndex: "http-user-agent",
                filter: { type: 'string' }
            },{
                hidden: true,
                header: 'URI'.t(),
                dataIndex: "http-uri",
                filter: { type: 'string'}
            },{
                hidden: true,
                header: 'Request Method'.t(),
                dataIndex: "http-request-method",
                filter: { type: 'string'}
            },{
                hidden: true,
                header: 'Request File Name'.t(),
                dataIndex: "http-request-file-name",
                filter: { type: 'string'}
            },{
                hidden: true,
                header: 'Request File Path'.t(),
                dataIndex: "http-request-file-path",
                filter: { type: 'string'}
            },{
                hidden: true,
                header: 'Content Type'.t(),
                dataIndex: "http-content-type",
                filter: { type: 'string'}
            },{
                hidden: true,
                header: 'Referer'.t(),
                dataIndex: "http-referer",
                filter: { type: 'string'}
            },{
                hidden: true,
                header: 'Content Length'.t(),
                dataIndex: "http-content-length",
                filter: 'number'
            }]
        }]
    }, {
        region: 'east',
        xtype: 'unpropertygrid',
        title: 'Session Details'.t(),
        itemId: 'details',

        bind: {
            source: '{selectedSession}'
        }
    }],
    tbar: [{
        xtype: 'button',
        text: 'Refresh'.t(),
        iconCls: 'fa fa-repeat',
        handler: 'getSessions',
        bind: {
            disabled: '{autoRefresh}'
        }
    }, {
        xtype: 'button',
        text: 'Auto Refresh'.t(),
        iconCls: 'fa fa-refresh',
        enableToggle: true,
        toggleHandler: 'setAutoRefresh'
    }, {
        xtype: 'button',
        text: 'Reset View'.t(),
        iconCls: 'fa fa-refresh',
        itemId: 'resetBtn',
        handler: 'resetView',
    }, '-', 'Filter:'.t(), {
        xtype: 'textfield',
        checkChangeBuffer: 200
    }]
});
