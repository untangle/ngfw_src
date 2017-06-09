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
            sortType: 'asTimestamp'
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
            sortType: 'asUnString',
            convert: Converter.policy
        }, {
            name: 'hostname',
            type: 'string',
            sortType: 'asUnString'
        }, {
            name: 'username',
            type: 'string',
            sortType: 'asUnString'
        }, {
            name: 'natted',
        }, {
            name: 'portForwarded',
        }, {
            name: 'tags',
        }, {
            name: "localAddr",
        },{
            name: "remoteAddr",
        },{
            name: "priority",
            convert: Converter.priority
        },{
            name: "qosPriority",
            convert: Converter.priority
        },{
            name: "pipeline",
        }, {
            name: 'clientIntf',
            type: 'string',
            sortType: 'asUnString',
            convert: Converter.interface
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
            sortType: 'asUnString',
            convert: Converter.interface
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
            width: Renderer.timestampWidth,
            hidden: true,
            rtype: 'timestamp',
            filter: Renderer.timestampFilter
        }, {
            header: 'Session ID'.t(),
            dataIndex: 'sessionId',
            width: Renderer.idWidth,
            hidden: true,
            filter: Renderer.numeric
        }, {
            header: 'Mark'.t(),
            dataIndex: 'mark',
            width: Renderer.idWidth,
            hidden: true,
            filter: Renderer.numeric,
            renderer: function(value) {
                if (value)
                    return "0x" + value.toString(16);
                else
                    return "";
            }
        }, {
            header: 'Protocol'.t(),
            dataIndex: 'protocol',
            width: Renderer.portWidth,
            filter: Renderer.stringFilter
        }, {
            header: 'Bypassed'.t(),
            dataIndex: 'bypassed',
            width: Renderer.booleanWidth,
            filter: Renderer.booleanFilter,
            rtype: 'boolean'
        }, {
            header: 'Policy'.t(),
            dataIndex: 'policy',
            // Look into list on policy
            filter: Renderer.stringFilter
        }, {
            header: 'Hostname'.t(),
            dataIndex: 'hostname',
            width: Renderer.hostnameWidth,
            filter: Renderer.stringFilter
        }, {
            header: 'Username'.t(),
            dataIndex: 'username',
            width: Renderer.usernameWidth,
            filter: Renderer.stringFilter
        }, {
            header: 'NATd'.t(),
            dataIndex: 'natted',
            width: Renderer.booleanWidth,
            filter: Renderer.booleanFilter,
            hidden: true
        }, {
            header: 'Port Forwarded'.t(),
            dataIndex: 'portForwarded',
            width: Renderer.booleanWidth,
            filter: Renderer.booleanFilter,
            hidden: true
        }, {
            header: 'Tags'.t(),
            dataIndex: 'tags',
            rtype: 'tags'
        }, {
            hidden: true,
            header: 'Local Address'.t(),
            dataIndex: "localAddr",
            width: Renderer.ipWidth,
            filter: Renderer.stringFilter
        },{
            hidden: true,
            header: 'Remote Address'.t(),
            dataIndex: "remoteAddr",
            width: Renderer.ipWidth,
            filter: Renderer.stringFilter
        },{
            hidden: true,
            header: 'Bandwidth Control ' + 'Priority'.t(),
            dataIndex: "priority",
            width: Renderer.messageWidth,
            filter: Renderer.stringFilter
        },{
            hidden: true,
            header: 'QoS ' + 'Priority'.t(),
            dataIndex: "qosPriority",
            width: Renderer.messageWidth,
            filter: Renderer.stringFilter
        },{
            hidden: true,
            header: 'Pipeline'.t(),
            dataIndex: "pipeline",
            filter: Renderer.stringFilter
        }, {
            header: 'Client'.t(),
            columns: [{
                header: 'Interface'.t(),
                dataIndex: 'clientIntf',
                filter: Renderer.stringFilter
            }, {
                header: 'Address'.t() + ' (' + 'Pre-NAT'.t() + ')',
                dataIndex: 'preNatClient',
                width: Renderer.ipWidth,
                filter: Renderer.stringFilter,
            }, {
                header: 'Port'.t() + ' (' + 'Pre-NAT'.t() + ')',
                dataIndex: 'preNatClientPort',
                width: Renderer.portWidth,
                filter: Renderer.numericFilter
            }, {
                header: 'Address'.t() + ' (' + 'Post-NAT'.t() + ')',
                dataIndex: 'postNatClient',
                width: Renderer.ipWidth,
                filter: Renderer.stringFilter,
                hidden: true
            }, {
                header: 'Port'.t() + ' (' + 'Post-NAT'.t() + ')',
                dataIndex: 'postNatClientPort',
                width: Renderer.portWidth,
                filter: Renderer.numericFilter,
                hidden: true
            }, {
                header: 'Country'.t(),
                dataIndex: 'clientCountry',
                filter: Renderer.stringFilter,
                width: Renderer.booleanWidth,
                hidden: true
            }, {
                header: 'Latitude'.t(),
                dataIndex: 'clientLatitude',
                filter: Renderer.numericFilter,
                hidden: true
            }, {
                header: 'Longitude'.t(),
                dataIndex: 'clientLongitude',
                filter: Renderer.numericFilter,
                hidden: true
            }]
        }, {
            header: 'Server'.t(),
            columns: [{
                header: 'Interface'.t(),
                dataIndex: 'serverIntf',
                filter: { type: 'string' },
            }, {
                header: 'Address'.t() + ' (' + 'Pre-NAT'.t() + ')',
                dataIndex: 'preNatServer',
                filter: Renderer.stringFilter,
                width: Renderer.ipWidth,
                hidden: true
            }, {
                header: 'Port'.t() + ' (' + 'Pre-NAT'.t() + ')',
                dataIndex: 'preNatServerPort',
                filter: Renderer.numericFilter,
                width: Renderer.portWidth,
                hidden: true
            }, {
                header: 'Address'.t() + ' (' + 'Post-NAT'.t() + ')',
                dataIndex: 'postNatServer',
                width: Renderer.ipWidth,
                filter: Renderer.stringFilter
            }, {
                header: 'Port'.t() + ' (' + 'Post-NAT'.t() + ')',
                dataIndex: 'postNatServerPort',
                filter: Renderer.numericFilter,
                width: Renderer.portWidth
            }, {
                header: 'Country'.t(),
                dataIndex: 'serverCountry',
                filter: Renderer.stringFilter,
                width: Renderer.booleanWidth
            }, {
                header: 'Latitude'.t(),
                dataIndex: 'serverLatitude',
                filter: Renderer.numericFilter,
                hidden: true
            }, {
                header: 'Longitude'.t(),
                dataIndex: 'serverLongitude',
                filter: Renderer.numericFilter,
                hidden: true
            }]
        }, {
            header: 'Speed (KB/s)'.t(),
            columns: [{
                header: 'Client'.t(),
                dataIndex: 'clientKBps',
                filter: Renderer.numericFilter,
                align: 'right',
                width: 60
            }, {
                header: 'Server'.t(),
                dataIndex: 'serverKBps',
                filter: Renderer.numericFilter,
                align: 'right',
                width: 60
            }, {
                header: 'Total'.t(),
                dataIndex: 'totalKBps',
                filter: Renderer.numericFilter,
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
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Category'.t(),
                dataIndex: "application-control-lite-category",
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Description'.t(),
                dataIndex: "application-control-lite-description",
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Matched?'.t(),
                dataIndex: "application-control-lite-matched",
                filter: Renderer.booleanFilter
            }]
        }, {
            header: 'Application Control',
            columns: [{
                header: 'Protochain'.t(),
                dataIndex: "application-control-protochain",
                filter: Renderer.stringFilter
            },{
                header: 'Application'.t(),
                dataIndex: "application-control-application",
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Category'.t(),
                dataIndex: "application-control-category",
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Detail'.t(),
                dataIndex: "application-control-detail",
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Confidence'.t(),
                dataIndex: "application-control-confidence",
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Productivity'.t(),
                dataIndex: "application-control-productivity",
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Risk'.t(),
                dataIndex: "application-control-risk",
                filter: Renderer.stringFilter
            }]
        }, {
            header: 'Web Filter',
            hidden: true,
            columns: [{
                hidden: true,
                header: 'Category Name'.t(),
                dataIndex: "web-filter-best-category-name",
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Category Description'.t(),
                dataIndex: "web-filter-best-category-description",
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Category Flagged'.t(),
                dataIndex: "web-filter-best-category-flagged",
                filter: Renderer.booleanFilter
            },{
                hidden: true,
                header: 'Category Blocked'.t(),
                dataIndex: "web-filter-best-category-blocked",
                filter: Renderer.booleanFilter,
            },{
                hidden: true,
                header: 'Flagged'.t(),
                dataIndex: "web-filter-flagged",
                filter: Renderer.booleanFilter,
            }]
        }, {
            header: 'HTTP',
            hidden: true,
            columns: [{
                hidden: true,
                header: 'Hostname'.t(),
                dataIndex: "http-hostname",
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'URL'.t(),
                dataIndex: "http-url",
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'User Agent'.t(),
                dataIndex: "http-user-agent",
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'URI'.t(),
                dataIndex: "http-uri",
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Request Method'.t(),
                dataIndex: "http-request-method",
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Request File Name'.t(),
                dataIndex: "http-request-file-name",
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Request File Extension'.t(),
                dataIndex: "http-request-file-extension",
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Request File Path'.t(),
                dataIndex: "http-request-file-path",
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Response File Name'.t(),
                dataIndex: "http-response-file-name",
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Response File Extension'.t(),
                dataIndex: "http-response-file-extension",
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Content Type'.t(),
                dataIndex: "http-content-type",
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Referer'.t(),
                dataIndex: "http-referer",
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Content Length'.t(),
                dataIndex: "http-content-length",
                filter: Renderer.numericFilter
            }]
        }, {
            header: 'SSL',
            hidden: true,
            columns: [{
                hidden: true,
                header: 'Subject DN'.t(),
                dataIndex: "ssl-subject-dn",
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Issuer DN'.t(),
                dataIndex: "ssl-issuer-dn",
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Inspected'.t(),
                dataIndex: "ssl-session-inspect",
                filter: Renderer.booleanFilter
            },{
                hidden: true,
                header: 'SNI Hostname'.t(),
                dataIndex: "ssl-sni-host",
                filter: Renderer.stringFilter
            }]
        }, {
            header: 'FTP',
            hidden: true,
            columns: [{
                hidden: true,
                header: 'Filename'.t(),
                dataIndex: "ftp-file-name",
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Data Session'.t(),
                dataIndex: "ftp-data-session",
                filter: Renderer.booleanFilter,
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
