Ext.define('TableConfig', {
    alternateClassName: 'TableConfig',
    singleton: true,

    getConfig: function(tableName) {
        return this.tableConfig[tableName];
    },

    checkHealth: function() {
        if(!rpc.reportsManager) {
            rpc.reportsManager = Ung.Main.getReportsManager();
        }
        var i, table, column, systemColumns, systemColumnsMap, tableConfigColumns, tableConfigColumnsMap;
        var systemTables = rpc.reportsManager.getTables();
        var systemTablesMap={};
        var missingTables = [];
        for(i=0; i<systemTables.length;i++) {
            systemTablesMap[systemTables[i]] = true;

            if(!this.tableConfig[systemTables[i]]) {

                // ignore 'totals' tables (from old reports and will be deprecated soon)
                if ( systemTables[i].indexOf('totals') !== -1 ) {
                    continue;
                }
                // ignore 'mail_msgs' table (will be deprecated soon)
                if ( systemTables[i].indexOf('mail_msgs') !== -1 ) {
                    continue;
                }
                missingTables.push(systemTables[i]);
            }
        }
        if(missingTables.length>0) {
            console.log('Warning: Missing tables: ' + missingTables.join(', '));
        }
        var extraTables = [];
        for (table in this.tableConfig) {
            if (this.tableConfig.hasOwnProperty(table)) {
                if(!systemTablesMap[table]) {
                    extraTables.push(table);
                }
            }
        }
        if (extraTables.length > 0) {
            console.log('Warning: Extra tables: ' + extraTables.join(', '));
        }

        for (table in this.tableConfig) {
            tableConfigColumns = this.tableConfig[table].columns;
            if(systemTablesMap[table]) {
                systemColumns = rpc.reportsManager.getColumnsForTable(table);
                systemColumnsMap = {};
                tableConfigColumnsMap = {};
                for(i=0;i<tableConfigColumns.length; i++) {
                    tableConfigColumnsMap[tableConfigColumns[i].dataIndex] = tableConfigColumns[i];
                }
                var missingColumns = [];
                for(i=0;i<systemColumns.length; i++) {
                    systemColumnsMap[systemColumns[i]] = true;
                    var columnConfig = tableConfigColumnsMap[systemColumns[i]];
                    if ( columnConfig === null ) {
                        missingColumns.push(systemColumns[i]);
                    } else {
                        if (! columnConfig.width ) {
                            console.log('Warning: Table "' + table + '" Columns: "' + columnConfig.dataIndex + '" missing width');
                        }
                    }
                }
                if (missingColumns.length > 0) {
                    console.log('Warning: Table "' + table + '" Missing columns: ' + missingColumns.join(', '));
                }

                var extraColumns = [];
                for (column in tableConfigColumnsMap) {
                    if (!systemColumnsMap[column]) {
                        extraColumns.push(column);
                    }
                }
                if (extraColumns.length > 0) {
                    console.log('Warning: Table "' + table + '" Extra columns: ' + extraColumns.join(', '));
                }

            }
        }

    },

    getColumnsForTable: function(table, store) {
        if(table !== null) {
            var tableConfig = this.getConfig(table);
            var columns = [], col;
            if(tableConfig !== null && Ext.isArray(tableConfig.columns)) {
                for(var i = 0; i<tableConfig.columns.length; i++) {
                    col = tableConfig.columns[i];
                    var name = col.header;
                    columns.push({
                        dataIndex: col.dataIndex,
                        header: name
                    });
                }
            }

            store.loadData(columns);
        }
    },

    getColumnHumanReadableName: function(columnName) {
        if(!this.columnsHumanReadableNames) {
            this.columnsHumanReadableNames = {};
            if(!this.tableConfig) {
                this.buildTableConfig();
            }
            var i, table, columns, dataIndex;
            for (table in this.tableConfig) {
                columns = this.tableConfig[table].columns;
                for(i=0; i<columns.length; i++) {
                    dataIndex = columns[i].dataIndex;
                    if(dataIndex && !this.columnsHumanReadableNames[dataIndex]) {
                        this.columnsHumanReadableNames[dataIndex] = columns[i].header;
                    }
                }
            }
        }
        if(!columnName) {
            columnName = '';
        }
        var readableName = this.columnsHumanReadableNames[columnName];
        return readableName !== null ? readableName : columnName.replace(/_/g,' ');
    },

    // new methods .........
    generate: function (table) {
        var checkboxes = [], comboItems = [];
        var tableConfig = this.tableConfig[table];

        if (!tableConfig) {
            console.log('Table not found!');
        }

        // generate checkboxes and menu
        Ext.Array.each(tableConfig.columns, function (column) {
            checkboxes.push({ boxLabel: column.header, inputValue: column.dataIndex, name: 'cbGroup' });
            comboItems.push({
                text: column.header,
                value: column.dataIndex
            });
        });
        tableConfig.checkboxes = checkboxes;
        tableConfig.comboItems = comboItems;

        return tableConfig;
    },

    // end new methods

    tableConfig: {
        sessions: {
            fields: [{
                name: 'session_id'
            }, {
                name: 'time_stamp',
                sortType: 'asTimestamp'
            }, {
                name: 'end_time',
                sortType: 'asTimestamp'
            }, {
                name: 'bypassed'
            }, {
                name: 'entitled'
            }, {
                name: 'protocol',
                convert: Converter.protocol
            }, {
                name: 'icmp_type',
                convert: Converter.icmp
            }, {
                name: 'hostname'
            }, {
                name: 'username'
            }, {
                name: 'tags'
            }, {
                name: 'policy_id',
                convert: Converter.policy
            }, {
                name: 'policy_rule_id'
            }, {
                name: 'c_client_addr',
                sortType: 'asIp'
            }, {
                name: 'c_client_port',
                sortType: 'asInt'
            }, {
                name: 'c_server_addr',
                sortType: 'asIp'
            }, {
                name: 'c_server_port',
                sortType: 'asInt'
            }, {
                name: 's_client_addr',
                sortType: 'asIp'
            }, {
                name: 's_client_port',
                sortType: 'asInt'
            }, {
                name: 's_server_addr',
                sortType: 'asIp'
            }, {
                name: 's_server_port',
                sortType: 'asInt'
            }, {
                name: 'client_intf',
                convert: Converter.interface
            }, {
                name: 'server_intf',
                convert: Converter.interface
            }, {
                name: 'client_country',
                convert: Converter.country
            }, {
                name: 'client_latitude'
            }, {
                name: 'client_longitude'
            }, {
                name: 'server_country',
                convert: Converter.country
            }, {
                name: 'server_latitude'
            }, {
                name: 'server_longitude'
            }, {
                name: 'c2p_bytes'
            }, {
                name: 'p2c_bytes'
            }, {
                name: 's2p_bytes'
            }, {
                name: 'p2s_bytes'
            }, {
                name: 'filter_prefix'
            }, {
                name: 'firewall_blocked'
            }, {
                name: 'firewall_flagged'
            }, {
                name: 'firewall_rule_index'
            }, {
                name: 'application_control_lite_blocked'
            }, {
                name: 'application_control_lite_protocol',
                type: 'string'
            }, {
                name: 'captive_portal_rule_index'
            }, {
                name: 'captive_portal_blocked'
            }, {
                name: 'application_control_application',
                type: 'string'
            }, {
                name: 'application_control_protochain',
                type: 'string'
            }, {
                name: 'application_control_category',
                type: 'string'
            }, {
                name: 'application_control_flagged'
            }, {
                name: 'application_control_blocked'
            }, {
                name: 'application_control_confidence'
            }, {
                name: 'application_control_detail'
            }, {
                name: 'application_control_ruleid'
            }, {
                name: 'bandwidth_control_priority'
            }, {
                name: 'bandwidth_control_rule',
                convert: Converter.bandwidthControlRule
            }, {
                name: 'ssl_inspector_status'
            }, {
                name: 'ssl_inspector_detail'
            }, {
                name: 'ssl_inspector_ruleid'
            }],
            columns: [{
                header: 'Session Id'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 'session_id'
            }, {
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp'
            }, {
                header: 'End Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'end_time',
                rtype: 'timestamp'
            }, {
                header: 'Bypassed'.t(),
                width: Renderer.booleanWidth,
                sortable: true,
                dataIndex: 'bypassed',
                filter: Renderer.booleanFilter,
                widgetField: {
                    xtype: 'combo',
                    store: [['true', 'True'.t()], ['false', 'False'.t()]],
                    value: 'true',
                    editable: false,
                    queryMode: 'local'
                }
            }, {
                header: 'Entitled'.t(),
                width: Renderer.booleanWidth,
                sortable: true,
                dataIndex: 'entitled',
                filter: Renderer.booleanFilter,
                widgetField: {
                    xtype: 'combo',
                    store: [['true', 'True'.t()], ['false', 'False'.t()]],
                    value: 'true',
                    editable: false,
                    queryMode: 'local'
                }
            }, {
                header: 'Protocol'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'protocol',
                // widgetField: {
                //     xtype: 'combo',
                //     store: ColumnRenderer.protocolStore(),
                //     editable: false,
                //     queryMode: 'local'
                // }
            }, {
                header: 'ICMP Type'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'icmp_type'
            }, {
                header: 'Policy Id'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'policy_id'
            }, {
                header: 'Policy Rule Id'.t(),
                width: Renderer.idWidth,
                sortable: true,
                dataIndex: 'policy_rule_id'
            }, {
                header: 'Client Interface'.t() ,
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'client_intf'
                // widgetField: {
                //     xtype: 'combo',
                //     // store: Util.getInterfaceListSystemDev(),
                //     editable: false,
                //     queryMode: 'local'
                // }
            }, {
                header: 'Server Interface'.t() ,
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'server_intf'
            }, {
                header: 'Client Country'.t() ,
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'client_country',
                widgetField: {
                    xtype: 'combo',
                    width: 300,
                    store: 'countries',
                    valueField: 'code',
                    displayField: 'name',
                    editable: false,
                    queryMode: 'local'
                }
            }, {
                header: 'Client Latitude'.t() ,
                width: Renderer.locationWidth,
                sortable: true,
                dataIndex: 'client_latitude'
            }, {
                header: 'Client Longitude'.t() ,
                width: Renderer.locationWidth,
                sortable: true,
                dataIndex: 'client_longitude'
            }, {
                header: 'Server Country'.t() ,
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'server_country',
                widgetField: {
                    xtype: 'combo',
                    width: 300,
                    store: 'countries',
                    valueField: 'code',
                    displayField: 'name',
                    editable: false,
                    queryMode: 'local'
                }
            }, {
                header: 'Server Latitude'.t() ,
                width: Renderer.locationWidth,
                sortable: true,
                dataIndex: 'server_latitude'
            }, {
                header: 'Server Longitude'.t() ,
                width: Renderer.locationWidth,
                sortable: true,
                dataIndex: 'server_longitude'
            }, {
                header: 'Username'.t(),
                width: Renderer.usernameWidth,
                sortable: true,
                dataIndex: 'username'
            }, {
                header: 'Hostname'.t(),
                width: Renderer.hostnameWidth,
                sortable: true,
                dataIndex: 'hostname'
            }, {
                header: 'Client'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 'c_client_addr'
            }, {
                header: 'Client Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'c_client_port',
                filter: Renderer.numericFilter,
                widgetField: {
                    xtype: 'numberfield',
                    value: 0
                }
            }, {
                header: 'New Client'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 's_client_addr'
            }, {
                header: 'New Client Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 's_client_port',
                filter: Renderer.numericFilter
            }, {
                header: 'Original Server'.t() ,
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 'c_server_addr'
            }, {
                header: 'Original Server Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'c_server_port',
                filter: Renderer.numericFilter
            }, {
                header: 'Server'.t() ,
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 's_server_addr'
            }, {
                header: 'Server Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 's_server_port',
                filter: Renderer.numericFilter
            }, {
                header: 'Tags'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'tags'
            }, {
                header: 'Filter Prefix'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'filter_prefix'
            }, {
                header: 'Priority'.t() + ' (Bandwidth Control)',
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'bandwidth_control_priority',
                rtype: 'priority'
            }, {
                header: 'Rule'.t() + ' (Bandwidth Control)',
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'bandwidth_control_rule',
            }, {
                header: 'Rule Id'.t() + ' (Application Control)',
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'application_control_ruleid',
                filter: Renderer.numericFilter
            }, {
                header: 'Application'.t() + ' (Application Control)',
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'application_control_application'
            }, {
                header: 'ProtoChain'.t() + ' (Application Control)',
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'application_control_protochain'
            }, {
                header: 'Category'.t() + ' (Application Control)',
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'application_control_category'
            }, {
                header: 'Blocked'.t() + ' (Application Control)',
                width: Renderer.booleanWidth,
                sortable: true,
                dataIndex: 'application_control_blocked',
                filter: Renderer.booleanFilter
            }, {
                header: 'Flagged'.t() + ' (Application Control)',
                width: Renderer.booleanWidth,
                sortable: true,
                dataIndex: 'application_control_flagged',
                filter: Renderer.booleanFilter
            }, {
                header: 'Confidence'.t() + ' (Application Control)',
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'application_control_confidence',
                filter: Renderer.numericFilter
            }, {
                header: 'Detail'.t() + ' (Application Control)',
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'application_control_detail'
            },{
                header: 'Protocol'.t() + ' (Application Control Lite)',
                width: Renderer.protocolWidth,
                sortable: true,
                dataIndex: 'application_control_lite_protocol',
                rtype: 'protocol'
            }, {
                header: 'Blocked'.t() + ' (Application Control Lite)',
                width: Renderer.booleanWidth,
                sortable: true,
                dataIndex: 'application_control_lite_blocked',
                filter: Renderer.booleanFilter
            }, {
                header: 'Rule Id'.t() + ' (SSL Inspector)',
                width: Renderer.idWidth,
                sortable: true,
                dataIndex: 'ssl_inspector_ruleid',
                filter: Renderer.numericFilter
            }, {
                header: 'Status'.t() + ' (SSL Inspector)',
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'ssl_inspector_status'
            }, {
                header: 'Detail'.t() + ' (SSL Inspector)',
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'ssl_inspector_detail'
            }, {
                header: 'Blocked'.t() + ' (Firewall)',
                width: Renderer.booleanWidth,
                sortable: true,
                dataIndex: 'firewall_blocked',
                filter: Renderer.booleanFilter
            }, {
                header: 'Flagged'.t() + ' (Firewall)',
                width: Renderer.booleanWidth,
                sortable: true,
                dataIndex: 'firewall_flagged',
                filter: Renderer.booleanFilter
            }, {
                header: 'Rule Id'.t() + ' (Firewall)',
                width: Renderer.idWidth,
                sortable: true,
                flex:1,
                dataIndex: 'firewall_rule_index',
                filter: Renderer.numericFilter
            }, {
                header: 'Captured'.t() + ' (Captive Portal)',
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'captive_portal_blocked',
                filter: Renderer.booleanFilter
            }, {
                header: 'Rule Id'.t() + ' (Captive Portal)',
                width: Renderer.idWidth,
                sortable: true,
                dataIndex: 'captive_portal_rule_index'
            }, {
                header: 'To-Server Bytes'.t(),
                width: Renderer.sizeWidth,
                sortable: true,
                dataIndex: 'p2s_bytes',
                rtype: 'datasize',
                filter: Renderer.numericFilter
            }, {
                header: 'From-Server Bytes'.t(),
                width: Renderer.sizeWidth,
                sortable: true,
                dataIndex: 's2p_bytes',
                rtype: 'datasize',
                filter: Renderer.numericFilter
            }, {
                header: 'To-Client Bytes'.t(),
                width: Renderer.sizeWidth,
                sortable: true,
                dataIndex: 'p2c_bytes',
                rtype: 'datasize',
                filter: Renderer.numericFilter
            }, {
                header: 'From-Client Bytes'.t(),
                width: Renderer.sizeWidth,
                sortable: true,
                dataIndex: 'c2p_bytes',
                rtype: 'datasize',
                filter: Renderer.numericFilter
            }]
        },
        session_minutes: {
            fields: [{
                name: 'session_id'
            }, {
                name: 'time_stamp',
                sortType: 'asTimestamp'
            }, {
                name: 'start_time',
                sortType: 'asTimestamp'
            }, {
                name: 'end_time',
                sortType: 'asTimestamp'
            }, {
                name: 'bypassed'
            }, {
                name: 'entitled'
            }, {
                name: 'protocol',
                convert: Converter.protocol
            }, {
                name: 'icmp_type',
                convert: Converter.icmp
            }, {
                name: 'hostname'
            }, {
                name: 'username'
            }, {
                name: 'tags'
            }, {
                name: 'policy_id',
                convert: Converter.policy
            }, {
                name: 'policy_rule_id'
            }, {
                name: 'c_client_addr',
                sortType: 'asIp'
            }, {
                name: 'c_client_port',
                sortType: 'asInt'
            }, {
                name: 'c_server_addr',
                sortType: 'asIp'
            }, {
                name: 'c_server_port',
                sortType: 'asInt'
            }, {
                name: 's_client_addr',
                sortType: 'asIp'
            }, {
                name: 's_client_port',
                sortType: 'asInt'
            }, {
                name: 's_server_addr',
                sortType: 'asIp'
            }, {
                name: 's_server_port',
                sortType: 'asInt'
            }, {
                name: 'client_intf',
                convert: Converter.interface
            }, {
                name: 'server_intf',
                convert: Converter.interface
            }, {
                name: 'client_country',
                convert: Converter.country
            }, {
                name: 'client_latitude'
            }, {
                name: 'client_longitude'
            }, {
                name: 'server_country',
                convert: Converter.country
            }, {
                name: 'server_latitude'
            }, {
                name: 'server_longitude'
            }, {
                name: 'c2p_bytes'
            }, {
                name: 'p2c_bytes'
            }, {
                name: 's2p_bytes'
            }, {
                name: 'p2s_bytes'
            }, {
                name: 'filter_prefix'
            }, {
                name: 'firewall_blocked'
            }, {
                name: 'firewall_flagged'
            }, {
                name: 'firewall_rule_index'
            }, {
                name: 'application_control_lite_blocked'
            }, {
                name: 'application_control_lite_protocol',
                type: 'string'
            }, {
                name: 'captive_portal_rule_index'
            }, {
                name: 'captive_portal_blocked'
            }, {
                name: 'application_control_application',
                type: 'string'
            }, {
                name: 'application_control_protochain',
                type: 'string'
            }, {
                name: 'application_control_category',
                type: 'string'
            }, {
                name: 'application_control_flagged'
            }, {
                name: 'application_control_blocked'
            }, {
                name: 'application_control_confidence'
            }, {
                name: 'application_control_detail'
            }, {
                name: 'application_control_ruleid'
            }, {
                name: 'bandwidth_control_priority'
            }, {
                name: 'bandwidth_control_rule',
                convert: Converter.bandwidthControlRule
            }, {
                name: 'ssl_inspector_status'
            }, {
                name: 'ssl_inspector_detail'
            }, {
                name: 'ssl_inspector_ruleid'
            }],
            columns: [{
                header: 'Session Id'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 'session_id'
            }, {
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp'
            }, {
                header: 'Start Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'start_time',
                rtype: 'timestamp'
            }, {
                header: 'End Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'end_time',
                rtype: 'timestamp'
            }, {
                header: 'Bypassed'.t(),
                width: Renderer.booleanWidth,
                sortable: true,
                dataIndex: 'bypassed',
                filter: Renderer.booleanFilter
            }, {
                header: 'Entitled'.t(),
                width: Renderer.booleanWidth,
                sortable: true,
                dataIndex: 'entitled',
                filter: Renderer.booleanFilter
            }, {
                header: 'Protocol'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'protocol',
            }, {
                header: 'ICMP Type'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'icmp_type'
            }, {
                header: 'Policy Id'.t(),
                width: 60,
                sortable: true,
                dataIndex: 'policy_id'
            }, {
                header: 'Policy Rule Id'.t(),
                width: 60,
                sortable: true,
                dataIndex: 'policy_rule_id'
            }, {
                header: 'Client Interface'.t() ,
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'client_intf'
            }, {
                header: 'Server Interface'.t() ,
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'server_intf'
            }, {
                header: 'Client Country'.t() ,
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'client_country',
            }, {
                header: 'Client Latitude'.t() ,
                width: Renderer.locationWidth,
                sortable: true,
                dataIndex: 'client_latitude'
            }, {
                header: 'Client Longitude'.t() ,
                width: Renderer.locationWidth,
                sortable: true,
                dataIndex: 'client_longitude'
            }, {
                header: 'Server Country'.t() ,
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'server_country',
            }, {
                header: 'Server Latitude'.t() ,
                width: Renderer.locationWidth,
                sortable: true,
                dataIndex: 'server_latitude'
            }, {
                header: 'Server Longitude'.t() ,
                width: Renderer.locationWidth,
                sortable: true,
                dataIndex: 'server_longitude'
            }, {
                header: 'Username'.t(),
                width: Renderer.usernameWidth,
                sortable: true,
                dataIndex: 'username'
            }, {
                header: 'Hostname'.t(),
                width: Renderer.hostnameWidth,
                sortable: true,
                dataIndex: 'hostname'
            }, {
                header: 'Client'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 'c_client_addr'
            }, {
                header: 'Client Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'c_client_port',
                filter: Renderer.numericFilter
            }, {
                header: 'New Client'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 's_client_addr'
            }, {
                header: 'New Client Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 's_client_port',
                filter: Renderer.numericFilter
            }, {
                header: 'Original Server'.t() ,
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 'c_server_addr'
            }, {
                header: 'Original Server Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'c_server_port',
                filter: Renderer.numericFilter
            }, {
                header: 'Server'.t() ,
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 's_server_addr'
            }, {
                header: 'Server Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 's_server_port',
                filter: Renderer.numericFilter
            }, {
                header: 'Tags'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'tags'
            }, {
                header: 'Filter Prefix'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'filter_prefix'
            }, {
                header: 'Rule Id'.t() + ' (Application Control)',
                width: Renderer.idWidth,
                sortable: true,
                dataIndex: 'application_control_ruleid',
                filter: Renderer.numericFilter
            }, {
                header: 'Priority'.t() + ' (Bandwidth Control)',
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'bandwidth_control_priority',
                rtype: 'priority'
            }, {
                header: 'Rule'.t() + ' (Bandwidth Control)',
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'bandwidth_control_rule'
            }, {
                header: 'Application'.t() + ' (Application Control)',
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'application_control_application'
            }, {
                header: 'ProtoChain'.t() + ' (Application Control)',
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'application_control_protochain'
            }, {
                header: 'Category'.t() + ' (Application Control)',
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'application_control_category'
            }, {
                header: 'Blocked'.t() + ' (Application Control)',
                width: Renderer.booleanWidth,
                sortable: true,
                dataIndex: 'application_control_blocked',
                filter: Renderer.booleanFilter
            }, {
                header: 'Flagged'.t() + ' (Application Control)',
                width: Renderer.booleanWidth,
                sortable: true,
                dataIndex: 'application_control_flagged',
                filter: Renderer.booleanFilter
            }, {
                header: 'Confidence'.t() + ' (Application Control)',
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'application_control_confidence',
                filter: Renderer.numericFilter
            }, {
                header: 'Detail'.t() + ' (Application Control)',
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'application_control_detail'
            },{
                header: 'Protocol'.t() + ' (Application Control Lite)',
                width: Renderer.protocolWidth,
                sortable: true,
                dataIndex: 'application_control_lite_protocol',
                rtype: 'protocol'
            }, {
                header: 'Blocked'.t() + ' (Application Control Lite)',
                width: Renderer.booleanWidth,
                sortable: true,
                dataIndex: 'application_control_lite_blocked',
                filter: Renderer.booleanFilter
            }, {
                header: 'Rule Id'.t() + ' (SSL Inspector)',
                width: Renderer.idWidth,
                sortable: true,
                dataIndex: 'ssl_inspector_ruleid',
                filter: Renderer.numericFilter
            }, {
                header: 'Status'.t() + ' (SSL Inspector)',
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'ssl_inspector_status'
            }, {
                header: 'Detail'.t() + ' (SSL Inspector)',
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'ssl_inspector_detail'
            }, {
                header: 'Blocked'.t() + ' (Firewall)',
                width: Renderer.booleanWidth,
                sortable: true,
                dataIndex: 'firewall_blocked',
                filter: Renderer.booleanFilter
            }, {
                header: 'Flagged'.t() + ' (Firewall)',
                width: Renderer.booleanWidth,
                sortable: true,
                dataIndex: 'firewall_flagged',
                filter: Renderer.booleanFilter
            }, {
                header: 'Rule Id'.t() + ' (Firewall)',
                width: Renderer.idWidth,
                sortable: true,
                flex:1,
                dataIndex: 'firewall_rule_index',
                filter: Renderer.numericFilter
            }, {
                header: 'Captured'.t() + ' (Captive Portal)',
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'captive_portal_blocked',
                filter: Renderer.booleanFilter
            }, {
                header: 'Rule Id'.t() + ' (Captive Portal)',
                width: Renderer.idWidth,
                sortable: true,
                dataIndex: 'captive_portal_rule_index'
            }, {
                header: 'From-Server Bytes'.t(),
                width: Renderer.sizeWidth,
                sortable: true,
                dataIndex: 's2c_bytes',
                rtype: 'datasize',
                filter: Renderer.numericFilter
            }, {
                header: 'From-Client Bytes'.t(),
                width: Renderer.sizeWidth,
                sortable: true,
                dataIndex: 'c2s_bytes',
                rtype: 'datasize',
                filter: Renderer.numericFilter
            }]
        },
        http_events: {
            fields: [{
                name: 'request_id',
                sortType: 'asInt'
            }, {
                name: 'policy_id',
                convert: Converter.policy
            }, {
                name: 'time_stamp',
                sortType: 'asTimestamp'
            }, {
                name: 'session_id',
                sortType: 'asInt'
            }, {
                name: 'client_intf',
                convert: Converter.interface
            }, {
                name: 'server_intf',
                convert: Converter.interface
            }, {
                name: 'c_client_addr',
                sortType: 'asIp'
            }, {
                name: 'c_client_port',
                sortType: 'asInt'
            }, {
                name: 'c_server_addr',
                sortType: 'asIp'
            }, {
                name: 'c_server_port',
                sortType: 'asInt'
            }, {
                name: 's_client_addr',
                sortType: 'asIp'
            }, {
                name: 's_client_port',
                sortType: 'asInt'
            }, {
                name: 's_server_addr',
                sortType: 'asIp'
            }, {
                name: 's_server_port',
                sortType: 'asInt'
            }, {
                name: 'username',
                type: 'string'
            }, {
                name: 'hostname',
                type: 'string'
            }, {
                name: 'method',
                type: 'string'
            }, {
                name: 'domain',
                type: 'string'
            }, {
                name: 'host',
                type: 'string'
            }, {
                name: 'uri',
                type: 'string'
            }, {
                name: 'referer',
                type: 'string'
            }, {
                name: 'c2s_content_length',
                sortType: 'asInt'
            }, {
                name: 's2c_content_length',
                sortType: 'asInt'
            }, {
                name: 's2c_content_type'
            }, {
                name: 'web_filter_blocked'
            }, {
                name: 'web_filter_flagged'
            }, {
                name: 'web_filter_category',
                type: 'string'
            }, {
                name: 'web_filter_reason',
                type: 'string',
                convert: Converter.httpReason
            }, {
                name: 'ad_blocker_action',
                type: 'string',
                convert: Converter.adBlockerAction
            }, {
                name: 'ad_blocker_cookie_ident',
                type: 'string'
            }, {
                name: 'virus_blocker_clean'
            }, {
                name: 'virus_blocker_name',
                type: 'string'
            }, {
                name: 'virus_blocker_lite_clean'
            }, {
                name: 'virus_blocker_lite_name',
                type: 'string'
            }],
            columns: [{
                header: 'Request Id'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'request_id'
            }, {
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp'
            }, {
                header: 'Policy Id'.t(),
                width: 60,
                sortable: true,
                dataIndex: 'policy_id'
            }, {
                header: 'Session Id'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'session_id'
            }, {
                header: 'Client Interface'.t() ,
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'client_intf'
            }, {
                header: 'Server Interface'.t() ,
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'server_intf'
            }, {
                header: 'Client'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 'c_client_addr'
            }, {
                header: 'Client Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'c_client_port',
                filter: {
                    type: 'numeric'
                }
            }, {
                header: 'New Client'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 's_client_addr'
            }, {
                header: 'New Client Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 's_client_port',
                filter: {
                    type: 'numeric'
                }
            }, {
                header: 'Original Server'.t() ,
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 'c_server_addr'
            }, {
                header: 'Original Server Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'c_server_port',
                filter: {
                    type: 'numeric'
                }
            }, {
                header: 'Server'.t() ,
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 's_server_addr'
            }, {
                header: 'Server Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 's_server_port',
                filter: {
                    type: 'numeric'
                }
            }, {
                header: 'Username'.t(),
                width: Renderer.usernameWidth,
                sortable: true,
                dataIndex: 'username'
            }, {
                header: 'Hostname'.t(),
                width: Renderer.hostnameWidth,
                sortable: true,
                dataIndex: 'hostname'
            }, {
                header: 'Domain'.t(),
                width: Renderer.hostnameWidth,
                sortable: true,
                dataIndex: 'domain'
            }, {
                header: 'Host'.t(),
                width: Renderer.hostnameWidth,
                sortable: true,
                dataIndex: 'host'
            }, {
                header: 'Uri'.t(),
                flex:1,
                width: Renderer.uriWidth,
                sortable: true,
                dataIndex: 'uri'
            }, {
                header: 'Method'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'method',
                renderer: function(value) {
                    // untranslated because these are HTTP methods
                    switch ( value ) {
                    case 'O': return 'OPTIONS' + ' (O)';
                    case 'G': return 'GET' + ' (G)';
                    case 'H': return 'HEAD' + ' (H)';
                    case 'P': return 'POST' + ' (P)';
                    case 'U': return 'PUT' + ' (U)';
                    case 'D': return 'DELETE' + ' (D)';
                    case 'T': return 'TRACE' + ' (T)';
                    case 'C': return 'CONNECT' + ' (C)';
                    case 'X': return 'NON-STANDARD' + ' (X)';
                    default: return value;
                    }
                }
            }, {
                header: 'Referer'.t(),
                width: Renderer.uriWidth,
                sortable: true,
                dataIndex: 'referer'
            }, {
                header: 'Download Content Length'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 's2c_content_length'
            }, {
                header: 'Upload Content Length'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'c2s_content_length'
            }, {
                header: 'Content Type'.t(),
                width: 150,
                sortable: true,
                dataIndex: 's2c_content_type'
            }, {
                header: 'Blocked'.t() + ' (Web Filter)',
                width: Renderer.booleanWidth,
                sortable: true,
                dataIndex: 'web_filter_blocked',
                filter: {
                    type: 'boolean',
                    yesText: 'true'.t(),
                    noText: 'false'.t()
                }
            }, {
                header: 'Flagged'.t() + ' (Web Filter)',
                width: Renderer.booleanWidth,
                sortable: true,
                dataIndex: 'web_filter_flagged',
                filter: {
                    type: 'boolean',
                    yesText: 'true'.t(),
                    noText: 'false'.t()
                }
            }, {
                header: 'Reason For Action'.t() +  ' (Web Filter)',
                width: 150,
                sortable: true,
                dataIndex: 'web_filter_reason'
            }, {
                header: 'Web Category'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'web_filter_category'
            }, {
                header: 'Action'.t() + ' (Ad Blocker)',
                width: Renderer.msgWidth,
                sortable: true,
                dataIndex: 'ad_blocker_action'
            }, {
                header: 'Blocked Cookie'.t() + ' (Ad Blocker)',
                width: Renderer.msgWidth,
                sortable: true,
                dataIndex: 'ad_blocker_cookie_ident'
            }, {
                header: 'Clean'.t() + ' (Virus Blocker Lite)',
                width: Renderer.booleanWidth,
                sortable: true,
                dataIndex: 'virus_blocker_lite_clean',
                filter: {
                    type: 'boolean',
                    yesText: 'true'.t(),
                    noText: 'false'.t()
                }
            }, {
                header: 'Virus Name'.t() + ' (Virus Blocker Lite)',
                width: 140,
                sortable: true,
                dataIndex: 'virus_blocker_lite_name'
            }, {
                header: 'Clean'.t() + ' (Virus Blocker)',
                width: Renderer.booleanWidth,
                sortable: true,
                dataIndex: 'virus_blocker_clean',
                filter: {
                    type: 'boolean',
                    yesText: 'true'.t(),
                    noText: 'false'.t()
                }
            }, {
                header: 'Virus Name'.t() + ' (Virus Blocker)',
                width: 140,
                sortable: true,
                dataIndex: 'virus_blocker_name'
            }]
        },
        http_query_events: {
            fields: [{
                name: 'event_id'
            }, {
                name: 'session_id'
            }, {
                name: 'policy_id',
                convert: Converter.policy
            }, {
                name: 'request_id'
            }, {
                name: 'time_stamp',
                sortType: 'asTimestamp'
            }, {
                name: 'client_intf',
                convert: Converter.interface
            }, {
                name: 'server_intf',
                convert: Converter.interface
            }, {
                name: 'c_client_addr',
                sortType: 'asIp'
            }, {
                name: 'c_client_port',
                sortType: 'asInt'
            }, {
                name: 'c_server_addr',
                sortType: 'asIp'
            }, {
                name: 'c_server_port',
                sortType: 'asInt'
            }, {
                name: 's_client_addr',
                sortType: 'asIp'
            }, {
                name: 's_client_port',
                sortType: 'asInt'
            }, {
                name: 's_server_addr',
                sortType: 'asIp'
            }, {
                name: 's_server_port',
                sortType: 'asInt'
            }, {
                name: 'username',
                type: 'string'
            }, {
                name: 'hostname',
                type: 'string'
            }, {
                name: 'c_server_addr',
                sortType: 'asIp'
            }, {
                name: 's_server_port',
                sortType: 'asInt'
            }, {
                name: 'host',
                type: 'string'
            }, {
                name: 'uri',
                type: 'string'
            }, {
                name: 'method',
                type: 'string'
            }, {
                name: 'c2s_content_length',
                sortType: 'asInt'
            }, {
                name: 's2c_content_length',
                sortType: 'asInt'
            }, {
                name: 's2c_content_type',
                type: 'string'
            }, {
                name: 'term'
            }],
            columns: [{
                header: 'Event Id'.t(),
                width: 60,
                sortable: true,
                dataIndex: 'event_id'
            }, {
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp'
            }, {
                header: 'Policy Id'.t(),
                width: 60,
                sortable: true,
                dataIndex: 'policy_id'
            }, {
                header: 'Request Id'.t(),
                width: 60,
                sortable: true,
                dataIndex: 'request_id'
            }, {
                header: 'Session Id'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'session_id'
            }, {
                header: 'Client Interface'.t() ,
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'client_intf'
            }, {
                header: 'Server Interface'.t() ,
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'server_intf'
            }, {
                header: 'Username'.t(),
                width: Renderer.usernameWidth,
                sortable: true,
                dataIndex: 'username'
            }, {
                header: 'Hostname'.t(),
                width: Renderer.hostnameWidth,
                sortable: true,
                dataIndex: 'hostname'
            }, {
                header: 'Client'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 'c_client_addr'
            }, {
                header: 'Client Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'c_client_port',
                filter: {
                    type: 'numeric'
                }
            }, {
                header: 'New Client'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 's_client_addr'
            }, {
                header: 'New Client Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 's_client_port',
                filter: {
                    type: 'numeric'
                }
            }, {
                header: 'Original Server'.t() ,
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 'c_server_addr'
            }, {
                header: 'Original Server Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'c_server_port',
                filter: {
                    type: 'numeric'
                }
            }, {
                header: 'Server'.t() ,
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 's_server_addr'
            }, {
                header: 'Server Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 's_server_port',
                filter: {
                    type: 'numeric'
                }
            }, {
                header: 'Host'.t(),
                width: Renderer.hostnameWidth,
                sortable: true,
                dataIndex: 'host'
            }, {
                header: 'Uri'.t(),
                flex:1,
                width: Renderer.uriWidth,
                sortable: true,
                dataIndex: 'uri'
            }, {
                header: 'Query Term'.t(),
                flex:1,
                width: Renderer.uriWidth,
                sortable: true,
                dataIndex: 'term'
            }, {
                header: 'Method'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'method'
            }, {
                header: 'Download Content Length'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 's2c_content_length'
            }, {
                header: 'Upload Content Length'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'c2s_content_length'
            }, {
                header: 'Content Type'.t(),
                width: 150,
                sortable: true,
                dataIndex: 's2c_content_type'
            }, {
                header: 'Server'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 'c_server_addr'
            }, {
                header: 'Server Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 's_server_port',
                filter: {
                    type: 'numeric'
                }
            }]
        },
        mail_addrs: {
            fields: [{
                name: 'event_id'
            }, {
                name: 'time_stamp',
                sortType: 'asTimestamp'
            }, {
                name: 'msg_id'
            }, {
                name: 'session_id'
            }, {
                name: 'policy_id',
                convert: Converter.policy
            }, {
                name: 'username'
            }, {
                name: 'hostname'
            }, {
                name: 'c_client_addr',
                sortType: 'asIp'
            }, {
                name: 'c_client_port',
                sortType: 'asInt'
            }, {
                name: 'c_server_addr',
                sortType: 'asIp'
            }, {
                name: 'c_server_port',
                sortType: 'asInt'
            }, {
                name: 's_client_addr',
                sortType: 'asIp'
            }, {
                name: 's_client_port',
                sortType: 'asInt'
            }, {
                name: 's_server_addr',
                sortType: 'asIp'
            }, {
                name: 's_server_port',
                sortType: 'asInt'
            }, {
                name: 'client_intf',
                convert: Converter.interface
            }, {
                name: 'server_intf',
                convert: Converter.interface
            }, {
                name: 'virus_blocker_name'
            }, {
                name: 'virus_blocker_clean'
            }, {
                name: 'virus_blocker_lite_name'
            }, {
                name: 'virus_blocker_lite_clean'
            }, {
                name: 'subject',
                type: 'string'
            }, {
                name: 'addr',
                type: 'string'
            }, {
                name: 'addr_name',
                type: 'string'
            }, {
                name: 'addr_kind',
                type: 'string'
            }, {
                name: 'sender',
                type: 'string'
            }, {
                name: 'vendor'
            }, {
                name:  'spam_blocker_lite_action',
                type: 'string',
                convert: Converter.emailAction
            }, {
                name: 'spam_blocker_lite_score'
            }, {
                name: 'spam_blocker_lite_is_spam'
            }, {
                name: 'spam_blocker_lite_tests_string'
            }, {
                name:  'spam_blocker_action',
                type: 'string',
                convert: Converter.emailAction
            }, {
                name: 'spam_blocker_score'
            }, {
                name: 'spam_blocker_is_spam'
            }, {
                name: 'spam_blocker_tests_string'
            }, {
                name:  'phish_blocker_action',
                type: 'string',
                convert: Converter.emailAction
            }, {
                name: 'phish_blocker_score'
            }, {
                name: 'phish_blocker_is_spam'
            }, {
                name: 'phish_blocker_tests_string'
            }],
            columns: [{
                header: 'Event Id'.t(),
                width: 60,
                sortable: true,
                dataIndex: 'event_id'
            }, {
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp'
            }, {
                header: 'Session Id'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'session_id'
            }, {
                header: 'Policy Id'.t(),
                width: 60,
                sortable: true,
                dataIndex: 'policy_id'
            }, {
                header: 'Message Id'.t(),
                width: 60,
                sortable: true,
                dataIndex: 'msg_id'
            }, {
                header: 'Client Interface'.t() ,
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'client_intf'
            }, {
                header: 'Server Interface'.t() ,
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'server_intf'
            }, {
                header: 'Username'.t(),
                width: Renderer.hostnameWidth,
                sortable: true,
                dataIndex: 'username'
            }, {
                header: 'Hostname'.t(),
                width: Renderer.hostnameWidth,
                sortable: true,
                dataIndex: 'hostname'
            }, {
                header: 'Client'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 'c_client_addr'
            }, {
                header: 'Client Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'c_client_port',
                filter: {
                    type: 'numeric'
                }
            }, {
                header: 'New Client'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 's_client_addr'
            }, {
                header: 'New Client Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 's_client_port',
                filter: {
                    type: 'numeric'
                }
            }, {
                header: 'Original Server'.t() ,
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 'c_server_addr'
            }, {
                header: 'Original Server Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'c_server_port',
                filter: {
                    type: 'numeric'
                }
            }, {
                header: 'Server'.t() ,
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 's_server_addr'
            }, {
                header: 'Server Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 's_server_port',
                filter: {
                    type: 'numeric'
                }
            }, {
                header: 'Receiver'.t(),
                width: Renderer.emailWidth,
                sortable: true,
                dataIndex: 'addr'
            }, {
                header: 'Address Name'.t(),
                width: Renderer.emailWidth,
                sortable: true,
                dataIndex: 'addr_name'
            }, {
                header: 'Address Kind'.t(),
                width: 60,
                sortable: true,
                dataIndex: 'addr_kind'
            }, {
                header: 'Sender'.t(),
                width: Renderer.emailWidth,
                sortable: true,
                dataIndex: 'sender'
            }, {
                header: 'Subject'.t(),
                flex:1,
                width: 150,
                sortable: true,
                dataIndex: 'subject'
            }, {
                header: 'Name'.t() + ' (Virus Blocker Lite)',
                width: 140,
                sortable: true,
                dataIndex: 'virus_blocker_lite_name'
            }, {
                header: 'Clean'.t() + ' (Virus Blocker Lite)',
                width: 140,
                sortable: true,
                dataIndex: 'virus_blocker_lite_clean'
            }, {
                header: 'Name'.t() + ' (Virus Blocker)',
                width: 140,
                sortable: true,
                dataIndex: 'virus_blocker_name'
            }, {
                header: 'Clean'.t() + ' (Virus Blocker)',
                width: 140,
                sortable: true,
                dataIndex: 'virus_blocker_clean'
            }, {
                header: 'Action'.t() + ' (Spam Blocker Lite)',
                width: 125,
                sortable: true,
                dataIndex: 'spam_blocker_lite_action'
            }, {
                header: 'Spam Score'.t() + ' (Spam Blocker Lite)',
                width: 70,
                sortable: true,
                dataIndex: 'spam_blocker_lite_score',
                filter: {
                    type: 'numeric'
                }
            }, {
                header: 'Is Spam'.t() + ' (Spam Blocker Lite)',
                width: 70,
                sortable: true,
                dataIndex: 'spam_blocker_lite_is_spam'
            }, {
                header: 'Detail'.t() + ' (Spam Blocker Lite)',
                width: 125,
                sortable: true,
                dataIndex: 'spam_blocker_lite_tests_string'
            }, {
                header: 'Action'.t() + ' (Spam Blocker)',
                width: 125,
                sortable: true,
                dataIndex: 'spam_blocker_action'
            }, {
                header: 'Spam Score'.t() + ' (Spam Blocker)',
                width: 70,
                sortable: true,
                dataIndex: 'spam_blocker_score',
                filter: {
                    type: 'numeric'
                }
            }, {
                header: 'Is Spam'.t() + ' (Spam Blocker)',
                width: 70,
                sortable: true,
                dataIndex: 'spam_blocker_is_spam'
            }, {
                header: 'Detail'.t() + ' (Spam Blocker)',
                width: 125,
                sortable: true,
                dataIndex: 'spam_blocker_tests_string'
            }, {
                header: 'Action'.t() + ' (Phish Blocker)',
                width: 125,
                sortable: true,
                dataIndex: 'phish_blocker_action'
            }, {
                header: 'Score'.t() + ' (Phish Blocker)',
                width: 70,
                sortable: true,
                dataIndex: 'phish_blocker_score'
            }, {
                header: 'Is Phish'.t() + ' (Phish Blocker)',
                width: 70,
                sortable: true,
                dataIndex: 'phish_blocker_is_spam'
            }, {
                header: 'Detail'.t() + ' (Phish Blocker)',
                width: 125,
                sortable: true,
                dataIndex: 'phish_blocker_tests_string'
            }]
        },
        directory_connector_login_events: {
            fields: [{
                name: 'id'
            }, {
                name: 'time_stamp',
                sortType: 'asTimestamp'
            }, {
                name: 'login_name'
            }, {
                name: 'domain'
            }, {
                name: 'type',
                convert: Converter.directoryConnectorAction
            }, {
                name: 'client_addr',
                sortType: 'asIp'
            }],
            columns: [{
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp'
            }, {
                header: 'Client'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 'client_addr'
            }, {
                header: 'Username'.t(),
                width: Renderer.usernameWidth,
                sortable: true,
                dataIndex: 'login_name'
            }, {
                header: 'Domain'.t(),
                width: Renderer.usernameWidth,
                sortable: true,
                dataIndex: 'domain'
            }, {
                header: 'Action'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'type'
            }]
        },
        admin_logins: {
            fields: [{
                name: 'time_stamp',
                sortType: 'asTimestamp'
            }, {
                name: 'login',
                type: 'string'
            }, {
                name: 'succeeded',
                type: 'string',
                convert: Converter.loginSuccess
            }, {
                name: 'local',
                type: 'string',
                convert: Converter.loginFrom
            }, {
                name: 'client_address',
                type: 'string'
            }, {
                name: 'reason',
                type: 'string',
                convert: Converter.loginFailureReason
            }],
            columns: [{
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp'
            }, {
                header: 'Login'.t(),
                width: Renderer.usernameWidth,
                sortable: true,
                dataIndex: 'login'
            }, {
                header: 'Success'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'succeeded'
            }, {
                header: 'Local'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'local'
            }, {
                header: 'Client Address'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 'client_addr'
            }, {
                header: 'Reason'.t(),
                flex: 1,
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'reason'
            }]
        },
        server_events: {
            fields: [{
                name: 'time_stamp',
                sortType: 'asTimestamp'
            }, {
                name: 'load_1'
            }, {
                name: 'load_5'
            }, {
                name: 'load_15'
            }, {
                name: 'cpu_user'
            }, {
                name: 'cpu_system'
            }, {
                name: 'mem_total',
                sortType: 'asInt'
            }, {
                name: 'mem_free',
                sortType: 'asInt'
            }, {
                name: 'disk_total',
                sortType: 'asInt'
            }, {
                name: 'disk_free',
                sortType: 'asInt'
            }, {
                name: 'swap_total',
                sortType: 'asInt'
            }, {
                name: 'swap_free',
                sortType: 'asInt'
            }, {
                name: 'active_hosts',
                sortType: 'asInt'
            }],
            columns: [{
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp'
            }, {
                header: 'Load (1-minute)'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'load_1',
                filter: {
                    type: 'numeric'
                }
            }, {
                header: 'Load (5-minute)'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'load_5',
                filter: {
                    type: 'numeric'
                }
            }, {
                header: 'Load (15-minute)'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'load_15',
                filter: {
                    type: 'numeric'
                }
            }, {
                header: 'CPU User Utilization'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'cpu_user',
                filter: {
                    type: 'numeric'
                }
            }, {
                header: 'CPU System Utilization'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'cpu_system',
                filter: {
                    type: 'numeric'
                }
            }, {
                header: 'Memory Total'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'mem_total',
                filter: {
                    type: 'numeric'
                },
                renderer: function(value) {
                    var meg = value/1024/1024;
                    return (Math.round( meg*10 )/10).toString() + ' MB';
                }

            }, {
                header: 'Memory Free'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'mem_free',
                filter: {
                    type: 'numeric'
                },
                renderer: function(value) {
                    var meg = value/1024/1024;
                    return (Math.round( meg*10 )/10).toString() + ' MB';
                }
            }, {
                header: 'Disk Total'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'disk_total',
                filter: {
                    type: 'numeric'
                },
                renderer: function(value) {
                    var gig = value/1024/1024/1024;
                    return (Math.round( gig*10 )/10).toString() + ' GB';
                }
            }, {
                header: 'Disk Free'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'disk_free',
                filter: {
                    type: 'numeric'
                },
                renderer: function(value) {
                    var gig = value/1024/1024/1024;
                    return (Math.round( gig*10 )/10).toString() + ' GB';
                }
            }, {
                header: 'Swap Total'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'swap_total',
                filter: {
                    type: 'numeric'
                },
                renderer: function(value) {
                    var meg = value/1024/1024;
                    return (Math.round( meg*10 )/10).toString() + ' MB';
                }
            }, {
                header: 'Swap Free'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'swap_free',
                filter: {
                    type: 'numeric'
                },
                renderer: function(value) {
                    var meg = value/1024/1024;
                    return (Math.round( meg*10 )/10).toString() + ' MB';
                }
            }, {
                header: 'Active Hosts'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'active_hosts',
                filter: {
                    type: 'numeric'
                }
            }]
        },
        host_table_updates: {
            fields: [{
                name: 'time_stamp',
                sortType: 'asTimestamp'
            }, {
                name: 'address',
                sortType: 'asIp'
            }, {
                name: 'key',
                type: 'string'
            }, {
                name: 'value',
                type: 'string'
            }, {
                name: 'old_value',
                type: 'string'
            }],
            columns: [{
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp'
            }, {
                header: 'Address'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 'address'
            }, {
                header: 'Key'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'key'
            }, {
                header: 'Value'.t(),
                width: Renderer.messageWidth,
                flex: 1,
                sortable: true,
                dataIndex: 'value'
            }, {
                header: 'Old Value'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'old_value'
            }]
        },
        device_table_updates: {
            fields: [{
                name: 'time_stamp',
                sortType: 'asTimestamp'
            }, {
                name: 'mac_address',
                type: 'string'
            }, {
                name: 'key',
                type: 'string'
            }, {
                name: 'value',
                type: 'string'
            }, {
                name: 'old_value',
                type: 'string'
            }],
            columns: [{
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp'
            }, {
                header: 'MAC Address'.t(),
                width: Renderer.macWidth,
                sortable: true,
                dataIndex: 'mac_address'
            }, {
                header: 'Key'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'key'
            }, {
                header: 'Value'.t(),
                width: Renderer.messageWidth,
                flex: 1,
                sortable: true,
                dataIndex: 'value'
            }, {
                header: 'Old Value'.t(),
                width: Renderer.messageWidth,
                flex: 1,
                sortable: true,
                dataIndex: 'old_value'
            }]
        },
        user_table_updates: {
            fields: [{
                name: 'time_stamp',
                sortType: 'asTimestamp'
            }, {
                name: 'username',
                type: 'string'
            }, {
                name: 'key',
                type: 'string'
            }, {
                name: 'value',
                type: 'string'
            }, {
                name: 'old_value',
                type: 'string'
            }],
            columns: [{
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp'
            }, {
                header: 'Username'.t(),
                width: Renderer.usernameWidth,
                sortable: true,
                dataIndex: 'username'
            }, {
                header: 'Key'.t(),
                width: 150,
                sortable: true,
                dataIndex: 'key'
            }, {
                header: 'Value'.t(),
                width: 150,
                flex: 1,
                sortable: true,
                dataIndex: 'value'
            }, {
                header: 'Old Value'.t(),
                width: 150,
                flex: 1,
                sortable: true,
                dataIndex: 'old_value'
            }]
        },
        configuration_backup_events: {
            fields: [{
                name: 'event_id'
            }, {
                name: 'time_stamp',
                sortType: 'asTimestamp'
            }, {
                name: 'success',
                type: 'string',
                convert: Ext.bind(function(value) {
                    return value ?  'success'.t(): 'failed'.t();
                }, this)
            }, {
                name: 'description',
                type: 'string'
            }, {
                name: 'destination',
                type: 'string'
            }],
            columns: [{
                header: 'Event Id'.t(),
                width: 100,
                sortable: true,
                dataIndex: 'event_id'
            },{
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp'
            }, {
                header: 'Result'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'success'
            }, {
                header: 'Destination'.t(),
                width: 100,
                sortable: true,
                dataIndex: 'destination'
            }, {
                header: 'Details'.t(),
                flex:1,
                width: 200,
                sortable: true,
                dataIndex: 'description'
            }]
        },
        wan_failover_test_events: {
            fields: [{
                name: 'event_id'
            },{
                name: 'time_stamp',
                sortType: 'asTimestamp'
            },{
                name: 'interface_id'
            },{
                name: 'name'
            },{
                name: 'success'
            },{
                name: 'description'
            }],
            columns: [{
                header: 'Event Id'.t(),
                width: 100,
                sortable: true,
                dataIndex: 'event_id'
            },{
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp'
            },{
                header: 'Interface Name'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'name'
            },{
                header: 'Interface Id'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'interface_id',
                rtype: 'interface'
            },{
                header: 'Success'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'success',
                filter: {
                    type: 'boolean',
                    yesText: 'true'.t(),
                    noText: 'false'.t()
                }
            },{
                header: 'Test Description'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'description',
                flex:1
            }]
        },
        wan_failover_action_events: {
            fields: [{
                name: 'event_id'
            },{
                name: 'time_stamp',
                sortType: 'asTimestamp'
            },{
                name: 'interface_id'
            },{
                name: 'name'
            },{
                name: 'os_name'
            },{
                name: 'action'
            }],
            columns: [{
                header: 'Event Id'.t(),
                width: 100,
                sortable: true,
                dataIndex: 'event_id'
            },{
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp'
            },{
                header: 'Interface Name'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'name'
            },{
                header: 'Interface Id'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'interface_id',
                rtype: 'interface'
            },{
                header: 'Interface OS'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'os_name'
            },{
                header: 'Action'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'action'
            }]
        },
        ipsec_user_events: {
            fields: [{
                name: 'time_stamp',
                sortType: 'asTimestamp'
            },{
                name: 'event_id'
            },{
                name: 'client_username'
            },{
                name: 'client_protocol'
            },{
                name: 'connect_stamp',
                sortType: 'asTimestamp'
            },{
                name: 'goodbye_stamp',
                sortType: 'asTimestamp'
            },{
                name: 'elapsed_time'
            },{
                name: 'client_address',
                sortType: 'asIp'
            },{
                name: 'net_interface'
            },{
                name: 'net_process'
            },{
                name: 'rx_bytes'
            },{
                name: 'tx_bytes'
            }],
            columns: [{
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp'
            },{
                header: 'Event Id'.t(),
                width: 100,
                sortable: true,
                dataIndex: 'event_id'
            },{
                header: 'Address'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 'client_address'
            },{
                header: 'Username'.t(),
                width: Renderer.usernameWidth,
                sortable: true,
                dataIndex: 'client_username'
            },{
                header: 'Protocol'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'client_protocol',
                // rtype: 'protocol',
            },{
                header: 'Login Time'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'connect_stamp',
                rtype: 'timestamp'
            },{
                header: 'Logout Time'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'goodbye_stamp',
                rtype: 'timestamp'
            },{
                header: 'Elapsed'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'elapsed_time'
            },{
                header: 'Interface'.t(),
                width: 80,
                sortable: true,
                dataIndex: 'net_interface',
                rtype: 'interface'
            },{
                header: 'RX Bytes'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'rx_bytes',
                renderer: function(value) {
                    if ((value === undefined) || (value === null) || (value === '')) {
                        return('');
                    }
                    var kb = value/1024;
                    return (Math.round( kb*10 )/10).toString() + ' KB';
                }
            },{
                header: 'TX Bytes'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'tx_bytes',
                renderer: function(value) {
                    if ((value === undefined) || (value === null) || (value === '')) {
                        return('');
                    }
                    var kb = value/1024;
                    return (Math.round( kb*10 )/10).toString() + ' KB';
                }
            },{
                header: 'Process'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'net_process'
            }]
        },
        ipsec_tunnel_stats: {
            fields: [{
                name: 'time_stamp',
                sortType: 'asTimestamp'
            }, {
                name: 'in_bytes',
                sortType: 'asInt'
            }, {
                name: 'out_bytes',
                sortType: 'asInt'
            }, {
                name: 'tunnel_name',
                type: 'string'
            }, {
                name: 'event_id',
                sortType: 'asInt'
            }],
            columns: [{
                header: 'Event Id'.t(),
                width: 100,
                sortable: true,
                dataIndex: 'event_id'
            }, {
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp'
            }, {
                header: 'Tunnel Name'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'tunnel_name'
            }, {
                header: 'In Bytes'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'in_bytes',
                renderer: function(value) {
                    var kb = value/1024;
                    return (Math.round( kb*10 )/10).toString() + ' KB';
                }
            }, {
                header: 'Out Bytes'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'out_bytes',
                renderer: function(value) {
                    var kb = value/1024;
                    return (Math.round( kb*10 )/10).toString() + ' KB';
                }
            }]
        },
        interface_stat_events: {
            fields: [{
                name: 'time_stamp',
                sortType: 'asTimestamp'
            }, {
                name: 'interface_id',
                sortType: 'asInt'
            }, {
                name: 'rx_rate'
            }, {
                name: 'tx_rate'
            }],
            columns: [{
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp'
            },{
                header: 'Interface Id'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'interface_id',
                rtype: 'interface'
            }, {
                header: 'RX Rate'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'rx_rate',
                filter: {
                    type: 'numeric'
                }
            }, {
                header: 'TX Rate'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'tx_rate',
                filter: {
                    type: 'numeric'
                }
            }]
        },
        smtp_tarpit_events: {
            fields: [{
                name: 'time_stamp',
                sortType: 'asTimestamp'
            }, {
                name: 'policy_id',
                convert: Converter.policy
            }, {
                name: 'event_id'
            }, {
                name: 'vendor_name'
            }, {
                name: 'ipaddr',
                convert: function(value) {
                    return value === null ? '': value;
                }
            }, {
                name: 'hostname'
            }],
            columns: [{
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp'
            }, {
                header: 'Policy Id'.t(),
                width: 60,
                sortable: true,
                dataIndex: 'policy_id'
            }, {
                header: 'Event Id'.t(),
                width: 60,
                sortable: true,
                dataIndex: 'event_id'
            }, {
                header: 'Vendor Name'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'vendor_name'
            }, {
                header: 'Sender'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'ipaddr'
            }, {
                header: 'DNSBL Server'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'hostname'
            }]
        },
        web_cache_stats: {
            fields: [{
                name: 'time_stamp',
                sortType: 'asTimestamp'
            }, {
                name: 'event_id'
            }, {
                name: 'hits'
            }, {
                name: 'misses'
            }, {
                name: 'bypasses'
            }, {
                name: 'systems'
            }, {
                name: 'hit_bytes'
            }, {
                name: 'miss_bytes'
            }],
            columns: [{
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp'
            }, {
                header: 'Event Id'.t(),
                width: 60,
                sortable: true,
                dataIndex: 'event_id'
            }, {
                header: 'Hit Count'.t(),
                width: 120,
                sortable: false,
                dataIndex: 'hits',
                filter: {
                    type: 'numeric'
                }
            }, {
                header: 'Miss Count'.t(),
                width: 120,
                sortable: false,
                dataIndex: 'misses',
                filter: {
                    type: 'numeric'
                }
            }, {
                header: 'Bypass Count'.t(),
                width: 120,
                sortable: false,
                dataIndex: 'bypasses',
                filter: {
                    type: 'numeric'
                }
            }, {
                header: 'System Count'.t(),
                width: 120,
                sortable: false,
                dataIndex: 'systems',
                filter: {
                    type: 'numeric'
                }
            }, {
                header: 'Hit Bytes'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'hit_bytes',
                filter: {
                    type: 'numeric'
                }
            }, {
                header: 'Miss Bytes'.t(),
                width: 120,
                sortable: true,
                dataIndex: 'miss_bytes',
                filter: {
                    type: 'numeric'
                }
            }]
        },
        captive_portal_user_events: {
            fields: [{
                name: 'time_stamp',
                sortType: 'asTimestamp'
            }, {
                name: 'policy_id',
                convert: Converter.policy
            }, {
                name: 'event_id'
            },{
                name: 'client_addr',
                sortType: 'asIp'
            },{
                name: 'login_name'
            },{
                name: 'auth_type',
                convert: Converter.authType
            },{
                name: 'event_info',
                convert: Converter.captivePortalEventInfo
            }],
            columns: [{
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp'
            }, {
                header: 'Policy Id'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'policy_id'
            }, {
                header: 'Event Id'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'event_id'
            }, {
                header: 'Client'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 'client_addr'
            }, {
                header: 'Username'.t(),
                width: Renderer.usernameWidth,
                sortable: true,
                dataIndex: 'login_name',
                flex:1
            }, {
                header: 'Action'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'event_info'
            }, {
                header: 'Authentication'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'auth_type'
            }]
        },
        intrusion_prevention_events: {
            fields: [{
                name: 'time_stamp',
                sortType: 'asTimestamp'
            }, {
                name: 'sig_id',
                sortType: 'asInt'
            }, {
                name: 'gen_id',
                sortType: 'asInt'
            }, {
                name: 'class_id',
                sortType: 'asInt'
            }, {
                name: 'source_addr',
                sortType: 'asIp'
            }, {
                name: 'source_port',
                sortType: 'asInt'
            }, {
                name: 'dest_addr',
                sortType: 'asIp'
            }, {
                name: 'dest_port',
                sortType: 'asInt'
            }, {
                name: 'protocol',
                convert: Converter.protocol
            }, {
                name: 'blocked'
            }, {
                name: 'category',
                type: 'string'
            }, {
                name: 'classtype',
                type: 'string'
            }, {
                name: 'msg',
                type: 'string'
            }],
            columns: [{
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp'
            }, {
                header: 'Sid'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'sig_id',
                filter: Renderer.numericFilter
            }, {
                header: 'Gid'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'gen_id',
                filter: Renderer.numericFilter
            }, {
                header: 'Cid'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'class_id',
                filter: Renderer.numericFilter
            }, {
                header: 'Source Address'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 'source_addr'
            }, {
                header: 'Source port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'source_port',
                filter: Renderer.numericFilter
            }, {
                header: 'Destination Address'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 'dest_addr'
            }, {
                header: 'Destination port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'dest_port',
                filter: Renderer.numericFilter
            }, {
                header: 'Protocol'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'protocol'
            }, {
                header: 'Blocked'.t(),
                width: Renderer.booleanWidth,
                sortable: true,
                dataIndex: 'blocked',
                filter: Renderer.booleanFilter
            }, {
                header: 'Category'.t(),
                width: Renderer.msgWidth,
                sortable: true,
                dataIndex: 'category'
            }, {
                header: 'Classtype'.t(),
                width: Renderer.msgWidth,
                sortable: true,
                dataIndex: 'classtype'
            }, {
                header: 'Msg'.t(),
                width: Renderer.msgWidth,
                flex: 1,
                sortable: true,
                dataIndex: 'msg'
            }]
        },
        openvpn_events: {
            fields: [{
                name: 'time_stamp',
                sortType: 'asTimestamp'
            }, {
                name: 'type'
            }, {
                name: 'client_name',
                type: 'string'
            }, {
                name: 'remote_address',
                sortType: 'asIp'
            }, {
                name: 'pool_address',
                sortType: 'asIp'
            }],
            columns: [{
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp'
            }, {
                header: 'Type'.t(),
                width: Renderer.messageWidth,
                flex: 1,
                sortable: true,
                dataIndex: 'type'
            }, {
                header: 'Client Name'.t(),
                width: Renderer.usernameWidth,
                sortable: true,
                dataIndex: 'client_name'
            }, {
                header: 'Client Address'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 'remote_address'
            }, {
                header: 'Pool Address'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 'pool_address'
            }]
        },
        openvpn_stats: {
            fields: [{
                name: 'time_stamp',
                sortType: 'asTimestamp'
            }, {
                name: 'start_time',
                sortType: 'asTimestamp'
            }, {
                name: 'end_time',
                sortType: 'asTimestamp'
            }, {
                name: 'rx_bytes',
                sortType: 'asInt'
            }, {
                name: 'tx_bytes',
                sortType: 'asInt'
            }, {
                name: 'remote_address',
                sortType: 'asIp'
            }, {
                name: 'pool_address',
                sortType: 'asIp'
            }, {
                name: 'remote_port',
                sortType: 'asInt'
            }, {
                name: 'client_name',
                type: 'string'
            }, {
                name: 'event_id',
                sortType: 'asInt'
            }],
            columns: [{
                header: 'Event Id'.t(),
                width: Renderer.idWidth,
                sortable: true,
                dataIndex: 'event_id'
            }, {
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp'
            }, {
                header: 'Start Time'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'start_time',
                rtype: 'timestamp'
            }, {
                header: 'End Time'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'end_time',
                rtype: 'timestamp'
            }, {
                header: 'Client Name'.t(),
                width: Renderer.usernameWidth,
                sortable: true,
                dataIndex: 'client_name'
            }, {
                header: 'Client Address'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 'remote_address'
            }, {
                header: 'Client Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'remote_port'
            }, {
                header: 'Pool Address'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 'pool_address'
            }, {
                header: 'RX Bytes'.t(),
                width: Renderer.sizeWidth,
                sortable: true,
                dataIndex: 'rx_bytes',
                rtype: 'datasize',
            }, {
                header: 'TX Bytes'.t(),
                width: Renderer.sizeWidth,
                sortable: true,
                dataIndex: 'tx_bytes',
                rtype: 'datasize',
            }]
        },
        alerts: {
            fields: [{
                name: 'time_stamp',
                sortType: 'asTimestamp'
            },{
                name: 'description'
            },{
                name: 'summary_text'
            },{
                name: 'json'
            }],
            columns: [{
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp'
            },{
                header: 'Description'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'description'
            },{
                header: 'Summary Text'.t(),
                sortable: true,
                dataIndex: 'summary_text'
            },{
                header: 'JSON'.t(),
                flex: 1,
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'json'
            }]
        },
        syslog: {
            fields: [{
                name: 'time_stamp',
                sortType: 'asTimestamp'
            },{
                name: 'description'
            },{
                name: 'summary_text'
            },{
                name: 'json'
            }],
            columns: [{
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp'
            },{
                header: 'Description'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'description'
            },{
                header: 'Summary Text'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'summary_text'
            },{
                header: 'JSON'.t(),
                flex: 1,
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'json'
            }]
        },
        ftp_events: {
            fields: [{
                name: 'event_id'
            }, {
                name: 'request_id'
            }, {
                name: 'session_id'
            }, {
                name: 'policy_id',
                convert: Converter.policy
            }, {
                name: 'time_stamp',
                sortType: 'asTimestamp'
            }, {
                name: 'method'
            }, {
                name: 'c_client_addr',
                sortType: 'asIp'
            }, {
                name: 'c_server_addr',
                sortType: 'asIp'
            }, {
                name: 's_client_addr',
                sortType: 'asIp'
            }, {
                name: 's_server_addr',
                sortType: 'asIp'
            }, {
                name: 'hostname'
            }, {
                name: 'username'
            }, {
                name: 'client_intf',
                convert: Converter.interface
            }, {
                name: 'server_intf',
                convert: Converter.interface
            }, {
                name: 'uri'
            }, {
                name: 'location'
            }, {
                name: 'virus_blocker_lite_name'
            }, {
                name: 'virus_blocker_lite_clean'
            }, {
                name: 'virus_blocker_name'
            }, {
                name: 'virus_blocker_clean'
            }],
            columns: [{
                header: 'Event Id'.t(),
                width: 100,
                sortable: true,
                dataIndex: 'event_id'
            }, {
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp'
            }, {
                header: 'Policy Id'.t(),
                width: 60,
                sortable: true,
                dataIndex: 'policy_id'
            }, {
                header: 'Session Id'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'session_id'
            }, {
                header: 'Request Id'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'request_id'
            }, {
                header: 'Method'.t(),
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'method'
            }, {
                header: 'Client Interface'.t() ,
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'client_intf'
            }, {
                header: 'Server Interface'.t() ,
                width: Renderer.portWidth,
                sortable: true,
                dataIndex: 'server_intf'
            }, {
                header: 'Client'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 'c_client_addr'
            }, {
                header: 'New Client'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 's_client_addr'
            }, {
                header: 'Original Server'.t() ,
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 'c_server_addr'
            }, {
                header: 'Server'.t() ,
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 's_server_addr'
            }, {
                header: 'Hostname'.t(),
                width: Renderer.hostnameWidth,
                sortable: true,
                dataIndex: 'hostname'
            }, {
                header: 'Username'.t(),
                width: Renderer.usernameWidth,
                sortable: true,
                dataIndex: 'username'
            }, {
                header: 'File Name'.t(),
                flex:1,
                width: Renderer.uriWidth,
                dataIndex: 'uri'
            }, {
                header: 'Virus Blocker Lite ' + 'Name'.t(),
                width: 140,
                sortable: true,
                dataIndex: 'virus_blocker_lite_name'
            }, {
                header: 'Virus Blocker Lite ' + 'clean'.t(),
                width: 140,
                sortable: true,
                dataIndex: 'virus_blocker_lite_clean'
            }, {
                header: 'Virus Blocker ' + 'Name'.t(),
                width: 140,
                sortable: true,
                dataIndex: 'virus_blocker_name'
            }, {
                header: 'Virus Blocker ' + 'Clean'.t(),
                width: 140,
                sortable: true,
                dataIndex: 'virus_blocker_clean'
            }, {
                header: 'Server'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 'c_server_addr'
            }]
        },
        quotas: {
            fields: [{
                name: 'time_stamp',
                sortType: 'asTimestamp'
            }, {
                name: 'address',
                sortType: 'asIp'
            }, {
                name: 'action',
                convert: Converter.quotaAction,
            }, {
                name: 'size',
                sortType: 'asInt'
            }, {
                name: 'reason'
            }],
            columns: [{
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp'
            }, {
                header: 'Address'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                dataIndex: 'address'
            }, {
                header: 'Action'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'action'
            }, {
                header: 'Size'.t(),
                width: Renderer.sizeWidth,
                sortable: true,
                dataIndex: 'size',
                filter: Renderer.numericFilter,
                rtype: 'datasize'
            }, {
                header: 'Reason'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                flex: 1,
                dataIndex: 'reason'
            }]
        },
        settings_changes: {
            fields: [{
                name: 'time_stamp',
                sortType: 'asTimestamp'
            }, {
                name: 'username'
            }, {
                name: 'hostname'
            }, {
                name: 'settings_file',
                convert: Converter.settingsFile
            }],
            columns: [{
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp'
            }, {
                header: 'Username'.t(),
                width: Renderer.usernameWidth,
                sortable: true,
                dataIndex: 'username'
            }, {
                header: 'Hostname'.t(),
                width: Renderer.hostnameWidth,
                sortable: true,
                dataIndex: 'hostname'
            }, {
                header: 'Settings File'.t(),
                flex:1,
                width: Renderer.uriWidth,
                dataIndex: 'settings_file'
            }]
        }
    }
});
