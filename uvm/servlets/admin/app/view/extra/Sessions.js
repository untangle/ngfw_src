Ext.define('Ung.view.extra.Sessions', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.sessions',
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

        viewConfig: {
            stripeRows: true,
            enableTextSelection: true
        },

        plugins: ['gridfilters'],
        columnLines: true,

        features: [{
            ftype: 'grouping'
        }],

        fields: [{
            name: 'creationTime',
        }, {
            name: 'sessionId',
        }, {
            name: 'mark',
        }, {
            name: 'protocol',
            type: 'string',
            sortType: 'asUnString'
        }, {
            name: 'bypassed',
        }, {
            name: 'policy',
            type: 'string',
            sortType: 'asUnString'
        }, {
            name: 'hostname',
            type: 'string',
            sortType: 'asUnString'
        }, {
            name: 'natted',
        }, {
            name: 'portForwarded',
        }, {
            name: 'tags',
        }, {
            name: 'tagsString',
            type: 'string',
            sortType: 'asUnString'
        }, {
            name: "localAddr",
        },{
            name: "remoteAddr",
        },{
            name: "priority",
        },{
            name: "qosPriority",
        },{
            name: "pipeline",
        }, {
            name: 'clientIntf',
            type: 'string',
            sortType: 'asUnString'
        }, {
            name: 'preNatClient',
        }, {
            name: 'preNatClientPort',
        }, {
            name: 'postNatClient',
        }, {
            name: 'postNatClientPort',
        }, {
            name: 'clientCountry',
            type: 'string',
            sortType: 'asUnString'
        }, {
            name: 'clientLatitude',
        }, {
            name: 'clientLongitude',
        }, {
            name: 'serverIntf',
            type: 'string',
            sortType: 'asUnString'
        }, {
            name: 'preNatServer',
        }, {
            name: 'preNatServerPort',
        }, {
            name: 'postNatServer',
        }, {
            name: 'postNatServerPort',
        }, {
            name: 'serverCountry',
            type: 'string',
            sortType: 'asUnString'
        }, {
            name: 'serverLatitude',
        }, {
            name: 'serverLongitude',
        }, {
            name: 'clientKBps',
        }, {
            name: 'serverKBps',
        }, {
            name: 'totalKBps',
        }, {
            name: "application-control-lite-protocol",
            type: 'string',
            sortType: 'asUnString'
        },{
            name: "application-control-lite-category",
            type: 'string',
            sortType: 'asUnString'
        },{
            name: "application-control-lite-description",
            type: 'string',
            sortType: 'asUnString'
        },{
            name: "application-control-lite-matched",
        }, {
            name: "application-control-protochain",
        },{
            name: "application-control-application",
            type: 'string',
            sortType: 'asUnString'
        },{
            name: "application-control-category",
            type: 'string',
            sortType: 'asUnString'
        },{
            name: "application-control-detail",
            type: 'string',
            sortType: 'asUnString'
        },{
            name: "application-control-confidence",
        },{
            name: "application-control-productivity",
        },{
            name: "application-control-risk",
        }, {
            name: "web-filter-best-category-name",
            type: 'string',
            sortType: 'asUnString'
        },{
            name: "web-filter-best-category-description",
            type: 'string',
            sortType: 'asUnString'
        },{
            name: "web-filter-best-category-flagged",
        },{
            name: "web-filter-best-category-blocked",
        },{
            name: "web-filter-flagged",
        }, {
            name: "http-hostname",
            type: 'string',
            sortType: 'asUnString'
        },{
            name: "http-url",
            type: 'string',
            sortType: 'asUnString'
        },{
            name: "http-user-agent",
            type: 'string',
            sortType: 'asUnString'
        },{
            name: "http-uri",
            type: 'string',
            sortType: 'asUnString'
        },{
            name: "http-request-method",
            type: 'string',
            sortType: 'asUnString'
        },{
            name: "http-request-file-name",
            type: 'string',
            sortType: 'asUnString'
        },{
            name: "http-request-file-extension",
            type: 'string',
            sortType: 'asUnString'
        },{
            name: "http-request-file-path",
            type: 'string',
            sortType: 'asUnString'
        },{
            name: "http-response-file-name",
            type: 'string',
            sortType: 'asUnString'
        },{
            name: "http-response-file-extension",
            type: 'string',
            sortType: 'asUnString'
        },{
            name: "http-content-type",
            type: 'string',
            sortType: 'asUnString'
        },{
            name: "http-referer",
            type: 'string',
            sortType: 'asUnString'
        },{
            name: "http-content-length",
            type: 'string',
            sortType: 'asUnString'
        }, {
            name: "ssl-subject-dn",
            type: 'string',
            sortType: 'asUnString'
        },{
            name: "ssl-issuer-dn",
            type: 'string',
            sortType: 'asUnString'
        },{
            name: "ssl-session-inspect",
            type: 'string',
            sortType: 'asUnString'
        },{
            name: "ssl-sni-host",
            type: 'string',
            sortType: 'asUnString'
        }, {
            name: "ftp-file-name",
            type: 'string',
            sortType: 'asUnString'
        },{
            name: "ftp-data-session",
        }],

        columns: [{
            header: 'Creation Time'.t(),
            dataIndex: 'creationTime',
            hidden: true,
            rtype: 'timestamp',
            filter: { type: 'date' },
            width: Renderer.timestampWidth
        }, {
            header: 'Session ID'.t(),
            dataIndex: 'sessionId',
            hidden: true,
            filter: 'number',
            width: 60
        }, {
            header: 'Mark'.t(),
            dataIndex: 'mark',
            hidden: true,
            filter: 'number',
            width: 60,
            renderer: function(value) {
                if (value)
                    return "0x" + value.toString(16);
                else
                    return "";
            }
        }, {
            header: 'Protocol'.t(),
            dataIndex: 'protocol',
            width: 50,
            filter: {
                type: 'list',
                options: ['TCP', 'UDP']
            }
        }, {
            header: 'Bypassed'.t(),
            dataIndex: 'bypassed',
            width: Renderer.booleanWidth,
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
            },
            rtype: 'policy'
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
            width: Renderer.booleanWidth,
            hidden: true
        }, {
            header: 'Port Forwarded'.t(),
            dataIndex: 'portForwarded',
            filter: {
                type: 'boolean',
                yesText: 'true',
                noText: 'false'
            },
            width: Renderer.booleanWidth,
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
            filter: { type: 'string' },
            width: Renderer.ipWidth
        },{
            hidden: true,
            header: 'Remote Address'.t(),
            dataIndex: "remoteAddr",
            filter: { type: 'string' },
            width: Renderer.ipWidth
        },{
            hidden: true,
            header: 'Bandwidth Control ' + 'Priority'.t(),
            dataIndex: "priority",
            renderer: function(value) {
                if (Ext.isEmpty(value)) {
                    return '';
                }
                switch(value) {
                  case 0: return '';
                  case 1: return 'Very High'.t();
                  case 2: return 'High'.t();
                  case 3: return 'Medium'.t();
                  case 4: return 'Low'.t();
                  case 5: return 'Limited'.t();
                  case 6: return 'Limited More'.t();
                  case 7: return 'Limited Severely'.t();
                default: return Ext.String.format('Unknown Priority: {0}'.t(), value);
                }
            }
        },{
            hidden: true,
            header: 'QoS ' + 'Priority'.t(),
            dataIndex: "qosPriority",
            renderer: function(value) {
                if (Ext.isEmpty(value)) {
                    return '';
                }
                switch(value) {
                  case 0: return '';
                  case 1: return 'Very High'.t();
                  case 2: return 'High'.t();
                  case 3: return 'Medium'.t();
                  case 4: return 'Low'.t();
                  case 5: return 'Limited'.t();
                  case 6: return 'Limited More'.t();
                  case 7: return 'Limited Severely'.t();
                default: return Ext.String.format('Unknown Priority: {0}'.t(), value);
                }
            }
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
                header: 'Address'.t() + ' (' + 'Pre-NAT'.t() + ')',
                dataIndex: 'preNatClient',
                filter: { type: 'string' },
                width: Renderer.ipWidth
            }, {
                header: 'Port'.t() + ' (' + 'Pre-NAT'.t() + ')',
                dataIndex: 'preNatClientPort',
                filter: { type: 'numeric' },
                width: Renderer.portWidth
            }, {
                header: 'Address'.t() + ' (' + 'Post-NAT'.t() + ')',
                dataIndex: 'postNatClient',
                filter: { type: 'string' },
                width: Renderer.ipWidth,
                hidden: true
            }, {
                header: 'Port'.t() + ' (' + 'Post-NAT'.t() + ')',
                dataIndex: 'postNatClientPort',
                filter: { type: 'numeric' },
                width: Renderer.portWidth,
                hidden: true
            }, {
                header: 'Country'.t(),
                dataIndex: 'clientCountry',
                filter: { type: 'string' },
                width: Renderer.booleanWidth,
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
                header: 'Address'.t() + ' (' + 'Pre-NAT'.t() + ')',
                dataIndex: 'preNatServer',
                filter: { type: 'string' },
                width: Renderer.ipWidth,
                hidden: true
            }, {
                header: 'Port'.t() + ' (' + 'Pre-NAT'.t() + ')',
                dataIndex: 'preNatServerPort',
                filter: { type: 'numeric' },
                width: Renderer.portWidth,
                hidden: true
            }, {
                header: 'Address'.t() + ' (' + 'Post-NAT'.t() + ')',
                dataIndex: 'postNatServer',
                width: Renderer.ipWidth,
                filter: { type: 'string' }
            }, {
                header: 'Port'.t() + ' (' + 'Post-NAT'.t() + ')',
                dataIndex: 'postNatServerPort',
                filter: { type: 'numeric' },
                width: Renderer.portWidth
            }, {
                header: 'Country'.t(),
                dataIndex: 'serverCountry',
                filter: { type: 'string' },
                width: Renderer.booleanWidth
            }, {
                header: 'Latitude'.t(),
                dataIndex: 'serverLatitude',
                filter: { type: 'numeric' },
                hidden: true
            }, {
                header: 'Longitude'.t(),
                dataIndex: 'serverLongitude',
                filter: { type: 'numeric' },
                hidden: true
            }]
        }, {
            header: 'Speed (KB/s)'.t(),
            columns: [{
                header: 'Client'.t(),
                dataIndex: 'clientKBps',
                filter: 'number',
                align: 'right',
                width: 60
            }, {
                header: 'Server'.t(),
                dataIndex: 'serverKBps',
                filter: 'number',
                align: 'right',
                width: 60
            }, {
                header: 'Total'.t(),
                dataIndex: 'totalKBps',
                filter: 'number',
                align: 'right',
                width: 60
            }]
        }, {
            header: 'Application Control Lite',
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
            header: 'Application Control',
            columns: [{
                header: 'Protochain'.t(),
                dataIndex: "application-control-protochain",
                filter: { type: 'string' }
            },{
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
            header: 'Web Filter',
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
                header: 'Flagged'.t(),
                dataIndex: "web-filter-flagged",
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            }]
        }, {
            header: 'HTTP',
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
                header: 'Request File Extension'.t(),
                dataIndex: "http-request-file-extension",
                filter: { type: 'string'}
            },{
                hidden: true,
                header: 'Request File Path'.t(),
                dataIndex: "http-request-file-path",
                filter: { type: 'string'}
            },{
                hidden: true,
                header: 'Response File Name'.t(),
                dataIndex: "http-response-file-name",
                filter: { type: 'string'}
            },{
                hidden: true,
                header: 'Response File Extension'.t(),
                dataIndex: "http-response-file-extension",
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
        }, {
            header: 'SSL',
            hidden: true,
            columns: [{
                hidden: true,
                header: 'Subject DN'.t(),
                dataIndex: "ssl-subject-dn",
                filter: { type: 'string' }
            },{
                hidden: true,
                header: 'Issuer DN'.t(),
                dataIndex: "ssl-issuer-dn",
                filter: { type: 'string' }
            },{
                hidden: true,
                header: 'Inspected'.t(),
                dataIndex: "ssl-session-inspect",
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
            },{
                hidden: true,
                header: 'SNI Hostname'.t(),
                dataIndex: "ssl-sni-host",
                filter: { type: 'string'}
            }]
        }, {
            header: 'FTP',
            hidden: true,
            columns: [{
                hidden: true,
                header: 'Filename'.t(),
                dataIndex: "ftp-file-name",
                filter: { type: 'string' }
            },{
                hidden: true,
                header: 'Data Session'.t(),
                dataIndex: "ftp-data-session",
                filter: {
                    type: 'boolean',
                    yesText: 'true',
                    noText: 'false'
                }
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
        bind: {
            iconCls: '{autoRefresh ? "fa fa-check-square-o" : "fa fa-square-o"}'
        },
        enableToggle: true,
        toggleHandler: 'setAutoRefresh'
    }, {
        xtype: 'button',
        text: 'Reset View'.t(),
        iconCls: 'fa fa-refresh',
        itemId: 'resetBtn',
        handler: 'resetView',
    },
    '-',
    {
        xtype: 'ungridfilter'
    },{
        xtype: 'ungridstatus',
        tplFiltered: '{0} filtered, {1} total sessions'.t(),
        tplUnfiltered: '{0} sessions'.t()
    }]
});
