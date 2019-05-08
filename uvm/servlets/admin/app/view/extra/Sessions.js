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
        defaultSortable: true,

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
            sortType: 'asUnString'
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
            type: 'string',
            sortType: 'asUnString'
        },{
            name: "remoteAddr",
            type: 'string',
            sortType: 'asUnString'
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
            type: 'string',
            sortType: 'asIp'
        }, {
            name: 'preNatClientPort',
        }, {
            name: 'postNatClient',
            type: 'string',
            sortType: 'asIp'
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
            type: 'string',
            sortType: 'asIp'
        }, {
            name: 'preNatServerPort',
        }, {
            name: 'postNatServer',
            type: 'string',
            sortType: 'asIp'
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
            convert: Converter.sessionSpeed
        }, {
            name: 'serverKBps',
            convert: Converter.sessionSpeed
        }, {
            name: 'totalKBps',
            convert: function(value, record){
                if ( record.data.serverKBps == null ||
                     record.data.clientKBps == null ){
                        return null;
                }
                return Converter.sessionSpeed(value);
            }
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
            type: 'string',
            sortType: 'asUnString'
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
            type: 'string',
            sortType: 'asUnString'
        },{
            name: "application-control-risk",
            type: 'string',
            sortType: 'asUnString'
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
            type: 'string',
            sortType: 'asUnString'
        },{
            name: "web-filter-best-category-blocked",
            type: 'string',
            sortType: 'asUnString'
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
            renderer: Renderer.timestamp,
            filter: Renderer.timestampFilter
        }, {
            header: 'Session ID'.t(),
            dataIndex: 'sessionId',
            width: Renderer.messageWidth,
            hidden: true,
            filter: Renderer.numericFilter
        }, {
            header: 'Mark'.t(),
            dataIndex: 'mark',
            width: Renderer.idWidth,
            hidden: true,
            filter: Renderer.numericFilter,
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
            renderer: Renderer.boolean
        }, {
            header: 'Policy'.t(),
            dataIndex: 'policy',
            width: Renderer.messageWidth,
            filter: Renderer.stringFilter,
            renderer: Renderer.policy
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
            width: Renderer.messageWidth,
            filter: Renderer.stringFilter
        }, {
            header: 'Client'.t(),
            columns: [{
                header: 'Interface'.t(),
                dataIndex: 'clientIntf',
                width: Renderer.messageWidth,
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
                width: Renderer.booleanWidth,
                filter: Renderer.stringFilter,
                hidden: true
            }, {
                header: 'Latitude'.t(),
                dataIndex: 'clientLatitude',
                width: Renderer.locationWidth,
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
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter
            }, {
                header: 'Address'.t() + ' (' + 'Pre-NAT'.t() + ')',
                dataIndex: 'preNatServer',
                width: Renderer.ipWidth,
                filter: Renderer.stringFilter,
                hidden: true
            }, {
                header: 'Port'.t() + ' (' + 'Pre-NAT'.t() + ')',
                dataIndex: 'preNatServerPort',
                width: Renderer.portWidth,
                filter: Renderer.numericFilter,
                hidden: true
            }, {
                header: 'Address'.t() + ' (' + 'Post-NAT'.t() + ')',
                width: Renderer.ipWidth,
                dataIndex: 'postNatServer',
                filter: Renderer.stringFilter
            }, {
                header: 'Port'.t() + ' (' + 'Post-NAT'.t() + ')',
                dataIndex: 'postNatServerPort',
                width: Renderer.portWidth,
                filter: Renderer.numericFilter
            }, {
                header: 'Country'.t(),
                dataIndex: 'serverCountry',
                width: Renderer.booleanWidth,
                filter: Renderer.stringFilter
            }, {
                header: 'Latitude'.t(),
                dataIndex: 'serverLatitude',
                width: Renderer.locationWidth,
                filter: Renderer.numericFilter,
                hidden: true
            }, {
                header: 'Longitude'.t(),
                dataIndex: 'serverLongitude',
                width: Renderer.locationWidth,
                filter: Renderer.numericFilter,
                hidden: true
            }]
        }, {
            header: 'Speed (KB/s)'.t(),
            columns: [{
                header: 'Client'.t(),
                dataIndex: 'clientKBps',
                width: Renderer.sizeWidth,
                filter: Renderer.numericFilter,
                align: 'right'
            }, {
                header: 'Server'.t(),
                dataIndex: 'serverKBps',
                width: Renderer.sizeWidth,
                filter: Renderer.numericFilter,
                align: 'right'
            }, {
                header: 'Total'.t(),
                dataIndex: 'totalKBps',
                width: Renderer.sizeWidth,
                filter: Renderer.numericFilter,
                align: 'right'
            }]
        }, {
            header: 'Application Control Lite',
            hidden: true,
            columns: [{
                hidden: true,
                header: 'Protocol'.t(),
                dataIndex: "application-control-lite-protocol",
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Category'.t(),
                dataIndex: "application-control-lite-category",
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Description'.t(),
                dataIndex: "application-control-lite-description",
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Matched?'.t(),
                dataIndex: "application-control-lite-matched",
                width: Renderer.messageWidth,
                filter: Renderer.booleanFilter
            }]
        }, {
            header: 'Application Control',
            columns: [{
                header: 'Protochain'.t(),
                dataIndex: "application-control-protochain",
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter
            },{
                header: 'Application'.t(),
                dataIndex: "application-control-application",
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Category'.t(),
                dataIndex: "application-control-category",
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Detail'.t(),
                dataIndex: "application-control-detail",
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Confidence'.t(),
                dataIndex: "application-control-confidence",
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Productivity'.t(),
                dataIndex: "application-control-productivity",
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Risk'.t(),
                dataIndex: "application-control-risk",
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter
            }]
        }, {
            header: 'Web Filter',
            hidden: true,
            columns: [{
                hidden: true,
                header: 'Category Name'.t(),
                dataIndex: "web-filter-best-category-name",
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Category Description'.t(),
                dataIndex: "web-filter-best-category-description",
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Category Flagged'.t(),
                dataIndex: "web-filter-best-category-flagged",
                width: Renderer.messageWidth,
                filter: Renderer.booleanFilter
            },{
                hidden: true,
                header: 'Category Blocked'.t(),
                dataIndex: "web-filter-best-category-blocked",
                width: Renderer.booleanWidth,
                filter: Renderer.booleanFilter,
            },{
                hidden: true,
                header: 'Flagged'.t(),
                dataIndex: "web-filter-flagged",
                width: Renderer.booleanWidth,
                filter: Renderer.booleanFilter,
            }]
        }, {
            header: 'HTTP',
            hidden: true,
            columns: [{
                hidden: true,
                header: 'Hostname'.t(),
                dataIndex: "http-hostname",
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'URL'.t(),
                dataIndex: "http-url",
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'User Agent'.t(),
                dataIndex: "http-user-agent",
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'URI'.t(),
                dataIndex: "http-uri",
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Request Method'.t(),
                dataIndex: "http-request-method",
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Request File Name'.t(),
                dataIndex: "http-request-file-name",
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Request File Extension'.t(),
                dataIndex: "http-request-file-extension",
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Request File Path'.t(),
                dataIndex: "http-request-file-path",
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Response File Name'.t(),
                dataIndex: "http-response-file-name",
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Response File Extension'.t(),
                dataIndex: "http-response-file-extension",
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Content Type'.t(),
                dataIndex: "http-content-type",
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Referer'.t(),
                dataIndex: "http-referer",
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Content Length'.t(),
                dataIndex: "http-content-length",
                width: Renderer.sizeWidth,
                filter: Renderer.numericFilter
            }]
        }, {
            header: 'SSL',
            hidden: true,
            columns: [{
                hidden: true,
                header: 'Subject DN'.t(),
                dataIndex: "ssl-subject-dn",
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Issuer DN'.t(),
                dataIndex: "ssl-issuer-dn",
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Inspected'.t(),
                dataIndex: "ssl-session-inspect",
                width: Renderer.booleanWidth,
                filter: Renderer.booleanFilter
            },{
                hidden: true,
                header: 'SNI Hostname'.t(),
                dataIndex: "ssl-sni-host",
                width: Renderer.hostnameWidth,
                filter: Renderer.stringFilter
            }]
        }, {
            header: 'FTP',
            hidden: true,
            columns: [{
                hidden: true,
                header: 'Filename'.t(),
                dataIndex: "ftp-file-name",
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter
            },{
                hidden: true,
                header: 'Data Session'.t(),
                dataIndex: "ftp-data-session",
                width: Renderer.booleanWidth,
                filter: Renderer.booleanFilter,
            }]
        }, {
            header: 'Tags'.t(),
            dataIndex: 'tags',
            width: Renderer.tagsWidth,
            renderer: Renderer.tags
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
    }]
});
