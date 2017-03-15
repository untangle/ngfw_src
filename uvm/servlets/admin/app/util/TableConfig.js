Ext.define('Ung.util.tableConfig', {
    alternateClassName: 'TabelConfig',
    singleton: true,

    //Field width constants
    timestampFieldWidth: 135,
    ipFieldWidth: 100,
    macFieldWidth: 100,
    portFieldWidth: 70,
    hostnameFieldWidth: 120,
    uriFieldWidth: 200,
    usernameFieldWidth: 120,
    booleanFieldWidth: 60,
    emailFieldWidth: 150,

    getConfig: function(tableName) {
        if(!this.tableConfig) {
            this.buildTableConfig();
        }
        return this.tableConfig[tableName];
    },
    checkHealth: function() {
        if(!rpc.reportsManager) {
            rpc.reportsManager = Ung.Main.getReportsManager();
        }
        if(!this.tableConfig) {
            this.buildTableConfig();
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
    httpEventConvertReason: function(value) {
        if(Ext.isEmpty(value)) {
            return null;
        }
        switch ( value ) {
        case 'D': return 'in Categories Block list'.t() + ' (D)';
        case 'U': return 'in Site Block list'.t() + ' (U)';
        case 'E': return 'in File Block list'.t() + ' (E)';
        case 'M': return 'in MIME Types Block list'.t() + ' (M)';
        case 'H': return 'hostname is an IP address'.t() + ' (H)';
        case 'I': return 'in Site Pass list'.t() + ' (I)';
        case 'R': return 'referer in Site Pass list'.t() + ' (R)';
        case 'C': return 'in Clients Pass list'.t() + ' (C)';
        case 'B': return 'in Unblocked list'.t() + ' (B)';
        case 'F': return 'in Rules list'.t() + ' (F)';
        case 'N': return 'no rule applied'.t() + ' (N)';
        default:  return 'no rule applied'.t();
        }
    },
    mailEventConvertAction: function(value) {
        if(Ext.isEmpty(value)) {
            return '';
        }
        switch (value) {
        case 'P': return 'pass message'.t();
        case 'M': return 'mark message'.t();
        case 'D': return 'drop message'.t();
        case 'B': return 'block message'.t();
        case 'Q': return 'quarantine message'.t();
        case 'S': return 'pass safelist message'.t();
        case 'Z': return 'pass oversize message'.t();
        case 'O': return 'pass outbound message'.t();
        case 'F': return 'block message (scan failure)'.t();
        case 'G': return 'pass message (scan failure)'.t();
        case 'Y': return 'block message (greylist)'.t();
        default:  return 'unknown action'.t();
        }
    },
    buildTableConfig: function() {
        this.tableConfig = {
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
                    name: 'protocol'
                }, {
                    name: 'icmp_type'
                }, {
                    name: 'hostname'
                }, {
                    name: 'username'
                }, {
                    name: 'tags'
                }, {
                    name: 'policy_id'
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
                    name: 'client_intf'
                }, {
                    name: 'server_intf'
                }, {
                    name: 'client_country'
                }, {
                    name: 'client_latitude'
                }, {
                    name: 'client_longitude'
                }, {
                    name: 'server_country'
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
                    name: 'bandwidth_control_rule'
                }, {
                    name: 'ssl_inspector_status'
                }, {
                    name: 'ssl_inspector_detail'
                }, {
                    name: 'ssl_inspector_ruleid'
                }],
                columns: [{
                    header: 'Session Id'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'session_id'
                }, {
                    header: 'Timestamp'.t(),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        // return i18n.timestampFormat(value);
                    }
                }, {
                    header: 'End Timestamp'.t(),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'end_time',
                    renderer: function(value) {
                        // return i18n.timestampFormat(value);
                    }
                }, {
                    header: 'Bypassed'.t(),
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'bypassed',
                    filter: {
                        type: 'boolean',
                        yesText: 'true'.t(),
                        noText: 'false'.t()
                    }
                }, {
                    header: 'Entitled'.t(),
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'entitled',
                    filter: {
                        type: 'boolean',
                        yesText: 'true'.t(),
                        noText: 'false'.t()
                    }
                }, {
                    header: 'Protocol'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'protocol',
                    renderer: Ung.panel.Reports.getColumnRenderer('protocol')
                }, {
                    header: 'ICMP Type'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'icmp_type'
                }, {
                    header: 'Policy Id'.t(),
                    width: 60,
                    sortable: true,
                    dataIndex: 'policy_id',
                    renderer: Ung.panel.Reports.getColumnRenderer('policy_id')
                }, {
                    header: 'Policy Rule Id'.t(),
                    width: 60,
                    sortable: true,
                    dataIndex: 'policy_rule_id'
                }, {
                    header: 'Client Interface'.t() ,
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'client_intf'
                }, {
                    header: 'Server Interface'.t() ,
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'server_intf'
                }, {
                    header: 'Client Country'.t() ,
                    width: 80,
                    sortable: true,
                    dataIndex: 'client_country',
                    renderer: function(value) { return Ung.Main.getCountryName(value); }
                }, {
                    header: 'Client Latitude'.t() ,
                    width: 80,
                    sortable: true,
                    dataIndex: 'client_latitude'
                }, {
                    header: 'Client Longitude'.t() ,
                    width: 80,
                    sortable: true,
                    dataIndex: 'client_longitude'
                }, {
                    header: 'Server Country'.t() ,
                    width: 80,
                    sortable: true,
                    dataIndex: 'server_country',
                    renderer: function(value) { return Ung.Main.getCountryName(value); }
                }, {
                    header: 'Server Latitude'.t() ,
                    width: 80,
                    sortable: true,
                    dataIndex: 'server_latitude'
                }, {
                    header: 'Server Longitude'.t() ,
                    width: 80,
                    sortable: true,
                    dataIndex: 'server_longitude'
                }, {
                    header: 'Username'.t(),
                    width: Ung.TableConfig.usernameFieldWidth,
                    sortable: true,
                    dataIndex: 'username'
                }, {
                    header: 'Hostname'.t(),
                    width: Ung.TableConfig.hostnameFieldWidth,
                    sortable: true,
                    dataIndex: 'hostname'
                }, {
                    header: 'Client'.t(),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'c_client_addr'
                }, {
                    header: 'Client Port'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'c_client_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'New Client'.t(),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 's_client_addr'
                }, {
                    header: 'New Client Port'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 's_client_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'Original Server'.t() ,
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'c_server_addr'
                }, {
                    header: 'Original Server Port'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'c_server_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'Server'.t() ,
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 's_server_addr'
                }, {
                    header: 'Server Port'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 's_server_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'Tags'.t(),
                    width: 150,
                    sortable: true,
                    dataIndex: 'tags'
                }, {
                    header: 'Filter Prefix'.t(),
                    width: 120,
                    sortable: true,
                    dataIndex: 'filter_prefix'
                }, {
                    header: 'Rule Id'.t() + ' (Application Control)',
                    width: 70,
                    sortable: true,
                    dataIndex: 'application_control_ruleid',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'Priority'.t() + ' (Bandwidth Control)',
                    width: 120,
                    sortable: true,
                    dataIndex: 'bandwidth_control_priority',
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
                }, {
                    header: 'Rule'.t() + ' (Bandwidth Control)',
                    width: 120,
                    sortable: true,
                    dataIndex: 'bandwidth_control_rule',
                    renderer: function(value) {
                        return Ext.isEmpty(value) ? 'none'.t() : value;
                    }
                }, {
                    header: 'Application'.t() + ' (Application Control)',
                    width: 120,
                    sortable: true,
                    dataIndex: 'application_control_application'
                }, {
                    header: 'ProtoChain'.t() + ' (Application Control)',
                    width: 180,
                    sortable: true,
                    dataIndex: 'application_control_protochain'
                }, {
                    header: 'Category'.t() + ' (Application Control)',
                    width: 80,
                    sortable: true,
                    dataIndex: 'application_control_category'
                }, {
                    header: 'Blocked'.t() + ' (Application Control)',
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'application_control_blocked',
                    filter: {
                        type: 'boolean',
                        yesText: 'true'.t(),
                        noText: 'false'.t()
                    }
                }, {
                    header: 'Flagged'.t() + ' (Application Control)',
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'application_control_flagged',
                    filter: {
                        type: 'boolean',
                        yesText: 'true'.t(),
                        noText: 'false'.t()
                    }
                }, {
                    header: 'Confidence'.t() + ' (Application Control)',
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'application_control_confidence',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'Detail'.t() + ' (Application Control)',
                    width: 200,
                    sortable: true,
                    dataIndex: 'application_control_detail'
                },{
                    header: 'Protocol'.t() + ' (Application Control Lite)',
                    width: 120,
                    sortable: true,
                    dataIndex: 'application_control_lite_protocol'
                }, {
                    header: 'Blocked'.t() + ' (Application Control Lite)',
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'application_control_lite_blocked',
                    filter: {
                        type: 'boolean',
                        yesText: 'true'.t(),
                        noText: 'false'.t()
                    }
                }, {
                    header: 'Rule Id'.t() + ' (SSL Inspector)',
                    width: 70,
                    sortable: true,
                    dataIndex: 'ssl_inspector_ruleid',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'Status'.t() + ' (SSL Inspector)',
                    width: 100,
                    sortable: true,
                    dataIndex: 'ssl_inspector_status'
                }, {
                    header: 'Detail'.t() + ' (SSL Inspector)',
                    width: 250,
                    sortable: true,
                    dataIndex: 'ssl_inspector_detail'
                }, {
                    header: 'Blocked'.t() + ' (Firewall)',
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'firewall_blocked',
                    filter: {
                        type: 'boolean',
                        yesText: 'true'.t(),
                        noText: 'false'.t()
                    }
                }, {
                    header: 'Flagged'.t() + ' (Firewall)',
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'firewall_flagged',
                    filter: {
                        type: 'boolean',
                        yesText: 'true'.t(),
                        noText: 'false'.t()
                    }
                }, {
                    header: 'Rule Id'.t() + ' (Firewall)',
                    width: 60,
                    sortable: true,
                    flex:1,
                    dataIndex: 'firewall_rule_index',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'Captured'.t() + ' (Captive Portal)',
                    width: 100,
                    sortable: true,
                    dataIndex: 'captive_portal_blocked',
                    filter: {
                        type: 'boolean',
                        yesText: 'true'.t(),
                        noText: 'false'.t()
                    }
                }, {
                    header: 'Rule Id'.t() + ' (Captive Portal)',
                    width: 60,
                    sortable: true,
                    dataIndex: 'captive_portal_rule_index'
                }, {
                    header: 'To-Server Bytes'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'p2s_bytes',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'From-Server Bytes'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 's2p_bytes',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'To-Client Bytes'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'p2c_bytes',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'From-Client Bytes'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'c2p_bytes',
                    filter: {
                        type: 'numeric'
                    }
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
                    name: 'protocol'
                }, {
                    name: 'icmp_type'
                }, {
                    name: 'hostname'
                }, {
                    name: 'username'
                }, {
                    name: 'tags'
                }, {
                    name: 'policy_id'
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
                    name: 'client_intf'
                }, {
                    name: 'server_intf'
                }, {
                    name: 'client_country'
                }, {
                    name: 'client_latitude'
                }, {
                    name: 'client_longitude'
                }, {
                    name: 'server_country'
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
                    name: 'bandwidth_control_rule'
                }, {
                    name: 'ssl_inspector_status'
                }, {
                    name: 'ssl_inspector_detail'
                }, {
                    name: 'ssl_inspector_ruleid'
                }],
                columns: [{
                    header: 'Session Id'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'session_id'
                }, {
                    header: 'Timestamp'.t(),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        // return Util.timestampFormat(value);
                    }
                }, {
                    header: 'Start Timestamp'.t(),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'start_time',
                    renderer: function(value) {
                        // return i18n.timestampFormat(value);
                    }
                }, {
                    header: 'End Timestamp'.t(),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'end_time',
                    renderer: function(value) {
                        // return i18n.timestampFormat(value);
                    }
                }, {
                    header: 'Bypassed'.t(),
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'bypassed',
                    filter: {
                        type: 'boolean',
                        yesText: 'true'.t(),
                        noText: 'false'.t()
                    }
                }, {
                    header: 'Entitled'.t(),
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'entitled',
                    filter: {
                        type: 'boolean',
                        yesText: 'true'.t(),
                        noText: 'false'.t()
                    }
                }, {
                    header: 'Protocol'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'protocol',
                    renderer: Ung.panel.Reports.getColumnRenderer('protocol')
                }, {
                    header: 'ICMP Type'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'icmp_type'
                }, {
                    header: 'Policy Id'.t(),
                    width: 60,
                    sortable: true,
                    dataIndex: 'policy_id',
                    renderer: Ung.panel.Reports.getColumnRenderer('policy_id')
                }, {
                    header: 'Policy Rule Id'.t(),
                    width: 60,
                    sortable: true,
                    dataIndex: 'policy_rule_id'
                }, {
                    header: 'Client Interface'.t() ,
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'client_intf'
                }, {
                    header: 'Server Interface'.t() ,
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'server_intf'
                }, {
                    header: 'Client Country'.t() ,
                    width: 80,
                    sortable: true,
                    dataIndex: 'client_country',
                    renderer: function(value) { return Ung.Main.getCountryName(value); }
                }, {
                    header: 'Client Latitude'.t() ,
                    width: 80,
                    sortable: true,
                    dataIndex: 'client_latitude'
                }, {
                    header: 'Client Longitude'.t() ,
                    width: 80,
                    sortable: true,
                    dataIndex: 'client_longitude'
                }, {
                    header: 'Server Country'.t() ,
                    width: 80,
                    sortable: true,
                    dataIndex: 'server_country',
                    renderer: function(value) { return Ung.Main.getCountryName(value); }
                }, {
                    header: 'Server Latitude'.t() ,
                    width: 80,
                    sortable: true,
                    dataIndex: 'server_latitude'
                }, {
                    header: 'Server Longitude'.t() ,
                    width: 80,
                    sortable: true,
                    dataIndex: 'server_longitude'
                }, {
                    header: 'Username'.t(),
                    width: Ung.TableConfig.usernameFieldWidth,
                    sortable: true,
                    dataIndex: 'username'
                }, {
                    header: 'Hostname'.t(),
                    width: Ung.TableConfig.hostnameFieldWidth,
                    sortable: true,
                    dataIndex: 'hostname'
                }, {
                    header: 'Client'.t(),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'c_client_addr'
                }, {
                    header: 'Client Port'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'c_client_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'New Client'.t(),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 's_client_addr'
                }, {
                    header: 'New Client Port'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 's_client_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'Original Server'.t() ,
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'c_server_addr'
                }, {
                    header: 'Original Server Port'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'c_server_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'Server'.t() ,
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 's_server_addr'
                }, {
                    header: 'Server Port'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 's_server_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'Tags'.t(),
                    width: 150,
                    sortable: true,
                    dataIndex: 'tags'
                }, {
                    header: 'Filter Prefix'.t(),
                    width: 120,
                    sortable: true,
                    dataIndex: 'filter_prefix'
                }, {
                    header: 'Rule Id'.t() + ' (Application Control)',
                    width: 70,
                    sortable: true,
                    dataIndex: 'application_control_ruleid',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'Priority'.t() + ' (Bandwidth Control)',
                    width: 120,
                    sortable: true,
                    dataIndex: 'bandwidth_control_priority',
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
                }, {
                    header: 'Rule'.t() + ' (Bandwidth Control)',
                    width: 120,
                    sortable: true,
                    dataIndex: 'bandwidth_control_rule',
                    renderer: function(value) {
                        return Ext.isEmpty(value) ? 'none'.t() : value;
                    }
                }, {
                    header: 'Application'.t() + ' (Application Control)',
                    width: 120,
                    sortable: true,
                    dataIndex: 'application_control_application'
                }, {
                    header: 'ProtoChain'.t() + ' (Application Control)',
                    width: 180,
                    sortable: true,
                    dataIndex: 'application_control_protochain'
                }, {
                    header: 'Category'.t() + ' (Application Control)',
                    width: 80,
                    sortable: true,
                    dataIndex: 'application_control_category'
                }, {
                    header: 'Blocked'.t() + ' (Application Control)',
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'application_control_blocked',
                    filter: {
                        type: 'boolean',
                        yesText: 'true'.t(),
                        noText: 'false'.t()
                    }
                }, {
                    header: 'Flagged'.t() + ' (Application Control)',
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'application_control_flagged',
                    filter: {
                        type: 'boolean',
                        yesText: 'true'.t(),
                        noText: 'false'.t()
                    }
                }, {
                    header: 'Confidence'.t() + ' (Application Control)',
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'application_control_confidence',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'Detail'.t() + ' (Application Control)',
                    width: 200,
                    sortable: true,
                    dataIndex: 'application_control_detail'
                },{
                    header: 'Protocol'.t() + ' (Application Control Lite)',
                    width: 120,
                    sortable: true,
                    dataIndex: 'application_control_lite_protocol'
                }, {
                    header: 'Blocked'.t() + ' (Application Control Lite)',
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'application_control_lite_blocked',
                    filter: {
                        type: 'boolean',
                        yesText: 'true'.t(),
                        noText: 'false'.t()
                    }
                }, {
                    header: 'Rule Id'.t() + ' (SSL Inspector)',
                    width: 70,
                    sortable: true,
                    dataIndex: 'ssl_inspector_ruleid',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'Status'.t() + ' (SSL Inspector)',
                    width: 100,
                    sortable: true,
                    dataIndex: 'ssl_inspector_status'
                }, {
                    header: 'Detail'.t() + ' (SSL Inspector)',
                    width: 250,
                    sortable: true,
                    dataIndex: 'ssl_inspector_detail'
                }, {
                    header: 'Blocked'.t() + ' (Firewall)',
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'firewall_blocked',
                    filter: {
                        type: 'boolean',
                        yesText: 'true'.t(),
                        noText: 'false'.t()
                    }
                }, {
                    header: 'Flagged'.t() + ' (Firewall)',
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'firewall_flagged',
                    filter: {
                        type: 'boolean',
                        yesText: 'true'.t(),
                        noText: 'false'.t()
                    }
                }, {
                    header: 'Rule Id'.t() + ' (Firewall)',
                    width: 60,
                    sortable: true,
                    flex:1,
                    dataIndex: 'firewall_rule_index',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'Captured'.t() + ' (Captive Portal)',
                    width: 100,
                    sortable: true,
                    dataIndex: 'captive_portal_blocked',
                    filter: {
                        type: 'boolean',
                        yesText: 'true'.t(),
                        noText: 'false'.t()
                    }
                }, {
                    header: 'Rule Id'.t() + ' (Captive Portal)',
                    width: 60,
                    sortable: true,
                    dataIndex: 'captive_portal_rule_index'
                }, {
                    header: 'From-Server Bytes'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 's2c_bytes',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'From-Client Bytes'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'c2s_bytes',
                    filter: {
                        type: 'numeric'
                    }
                }]
            },
            http_events: {
                fields: [{
                    name: 'request_id',
                    sortType: 'asInt'
                }, {
                    name: 'policy_id',
                    sortType: 'asInt'
                }, {
                    name: 'time_stamp',
                    sortType: 'asTimestamp'
                }, {
                    name: 'session_id',
                    sortType: 'asInt'
                }, {
                    name: 'client_intf',
                    sortType: 'asInt'
                }, {
                    name: 'server_intf',
                    sortType: 'asInt'
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
                    convert: Ung.TableConfig.httpEventConvertReason
                }, {
                    name: 'ad_blocker_action',
                    type: 'string',
                    convert: function(value) {
                        return (value === 'B')?'block'.t() : 'pass'.t();
                    }
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
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'request_id'
                }, {
                    header: 'Timestamp'.t(),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        // return i18n.timestampFormat(value);
                    }
                }, {
                    header: 'Policy Id'.t(),
                    width: 60,
                    sortable: true,
                    dataIndex: 'policy_id',
                    renderer: Ung.panel.Reports.getColumnRenderer('policy_id')
                }, {
                    header: 'Session Id'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'session_id'
                }, {
                    header: 'Client Interface'.t() ,
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'client_intf'
                }, {
                    header: 'Server Interface'.t() ,
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'server_intf'
                }, {
                    header: 'Client'.t(),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'c_client_addr'
                }, {
                    header: 'Client Port'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'c_client_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'New Client'.t(),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 's_client_addr'
                }, {
                    header: 'New Client Port'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 's_client_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'Original Server'.t() ,
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'c_server_addr'
                }, {
                    header: 'Original Server Port'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'c_server_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'Server'.t() ,
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 's_server_addr'
                }, {
                    header: 'Server Port'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 's_server_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'Username'.t(),
                    width: Ung.TableConfig.usernameFieldWidth,
                    sortable: true,
                    dataIndex: 'username'
                }, {
                    header: 'Hostname'.t(),
                    width: Ung.TableConfig.hostnameFieldWidth,
                    sortable: true,
                    dataIndex: 'hostname'
                }, {
                    header: 'Domain'.t(),
                    width: Ung.TableConfig.hostnameFieldWidth,
                    sortable: true,
                    dataIndex: 'domain'
                }, {
                    header: 'Host'.t(),
                    width: Ung.TableConfig.hostnameFieldWidth,
                    sortable: true,
                    dataIndex: 'host'
                }, {
                    header: 'Uri'.t(),
                    flex:1,
                    width: Ung.TableConfig.uriFieldWidth,
                    sortable: true,
                    dataIndex: 'uri'
                }, {
                    header: 'Method'.t(),
                    width: Ung.TableConfig.portFieldWidth,
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
                    width: Ung.TableConfig.uriFieldWidth,
                    sortable: true,
                    dataIndex: 'referer'
                }, {
                    header: 'Download Content Length'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 's2c_content_length'
                }, {
                    header: 'Upload Content Length'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'c2s_content_length'
                }, {
                    header: 'Content Type'.t(),
                    width: 150,
                    sortable: true,
                    dataIndex: 's2c_content_type'
                }, {
                    header: 'Blocked'.t() + ' (Web Filter)',
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'web_filter_blocked',
                    filter: {
                        type: 'boolean',
                        yesText: 'true'.t(),
                        noText: 'false'.t()
                    }
                }, {
                    header: 'Flagged'.t() + ' (Web Filter)',
                    width: Ung.TableConfig.booleanFieldWidth,
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
                    width: 120,
                    sortable: true,
                    dataIndex: 'ad_blocker_action'
                }, {
                    header: 'Blocked Cookie'.t() + ' (Ad Blocker)',
                    width: 100,
                    sortable: true,
                    dataIndex: 'ad_blocker_cookie_ident'
                }, {
                    header: 'Clean'.t() + ' (Virus Blocker Lite)',
                    width: Ung.TableConfig.booleanFieldWidth,
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
                    width: Ung.TableConfig.booleanFieldWidth,
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
                    name: 'policy_id'
                }, {
                    name: 'request_id'
                }, {
                    name: 'time_stamp',
                    sortType: 'asTimestamp'
                }, {
                    name: 'client_intf'
                }, {
                    name: 'server_intf'
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
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        // return i18n.timestampFormat(value);
                    }
                }, {
                    header: 'Policy Id'.t(),
                    width: 60,
                    sortable: true,
                    dataIndex: 'policy_id',
                    renderer: Ung.panel.Reports.getColumnRenderer('policy_id')
                }, {
                    header: 'Request Id'.t(),
                    width: 60,
                    sortable: true,
                    dataIndex: 'request_id'
                }, {
                    header: 'Session Id'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'session_id'
                }, {
                    header: 'Client Interface'.t() ,
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'client_intf'
                }, {
                    header: 'Server Interface'.t() ,
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'server_intf'
                }, {
                    header: 'Username'.t(),
                    width: Ung.TableConfig.usernameFieldWidth,
                    sortable: true,
                    dataIndex: 'username'
                }, {
                    header: 'Hostname'.t(),
                    width: Ung.TableConfig.hostnameFieldWidth,
                    sortable: true,
                    dataIndex: 'hostname'
                }, {
                    header: 'Client'.t(),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'c_client_addr'
                }, {
                    header: 'Client Port'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'c_client_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'New Client'.t(),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 's_client_addr'
                }, {
                    header: 'New Client Port'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 's_client_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'Original Server'.t() ,
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'c_server_addr'
                }, {
                    header: 'Original Server Port'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'c_server_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'Server'.t() ,
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 's_server_addr'
                }, {
                    header: 'Server Port'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 's_server_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'Host'.t(),
                    width: Ung.TableConfig.hostnameFieldWidth,
                    sortable: true,
                    dataIndex: 'host'
                }, {
                    header: 'Uri'.t(),
                    flex:1,
                    width: Ung.TableConfig.uriFieldWidth,
                    sortable: true,
                    dataIndex: 'uri'
                }, {
                    header: 'Query Term'.t(),
                    flex:1,
                    width: Ung.TableConfig.uriFieldWidth,
                    sortable: true,
                    dataIndex: 'term'
                }, {
                    header: 'Method'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'method'
                }, {
                    header: 'Download Content Length'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 's2c_content_length'
                }, {
                    header: 'Upload Content Length'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'c2s_content_length'
                }, {
                    header: 'Content Type'.t(),
                    width: 150,
                    sortable: true,
                    dataIndex: 's2c_content_type'
                }, {
                    header: 'Server'.t(),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'c_server_addr'
                }, {
                    header: 'Server Port'.t(),
                    width: Ung.TableConfig.portFieldWidth,
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
                    name: 'policy_id'
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
                    name: 'client_intf'
                }, {
                    name: 'server_intf'
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
                    convert: Ung.TableConfig.mailEventConvertAction
                }, {
                    name: 'spam_blocker_lite_score'
                }, {
                    name: 'spam_blocker_lite_is_spam'
                }, {
                    name: 'spam_blocker_lite_tests_string'
                }, {
                    name:  'spam_blocker_action',
                    type: 'string',
                    convert: Ung.TableConfig.mailEventConvertAction
                }, {
                    name: 'spam_blocker_score'
                }, {
                    name: 'spam_blocker_is_spam'
                }, {
                    name: 'spam_blocker_tests_string'
                }, {
                    name:  'phish_blocker_action',
                    type: 'string',
                    convert: Ung.TableConfig.mailEventConvertAction
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
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        // return i18n.timestampFormat(value);
                    }
                }, {
                    header: 'Session Id'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'session_id'
                }, {
                    header: 'Policy Id'.t(),
                    width: 60,
                    sortable: true,
                    dataIndex: 'policy_id',
                    renderer: Ung.panel.Reports.getColumnRenderer('policy_id')
                }, {
                    header: 'Message Id'.t(),
                    width: 60,
                    sortable: true,
                    dataIndex: 'msg_id'
                }, {
                    header: 'Client Interface'.t() ,
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'client_intf'
                }, {
                    header: 'Server Interface'.t() ,
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'server_intf'
                }, {
                    header: 'Username'.t(),
                    width: Ung.TableConfig.hostnameFieldWidth,
                    sortable: true,
                    dataIndex: 'username'
                }, {
                    header: 'Hostname'.t(),
                    width: Ung.TableConfig.hostnameFieldWidth,
                    sortable: true,
                    dataIndex: 'hostname'
                }, {
                    header: 'Client'.t(),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'c_client_addr'
                }, {
                    header: 'Client Port'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'c_client_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'New Client'.t(),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 's_client_addr'
                }, {
                    header: 'New Client Port'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 's_client_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'Original Server'.t() ,
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'c_server_addr'
                }, {
                    header: 'Original Server Port'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'c_server_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'Server'.t() ,
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 's_server_addr'
                }, {
                    header: 'Server Port'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 's_server_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'Receiver'.t(),
                    width: Ung.TableConfig.emailFieldWidth,
                    sortable: true,
                    dataIndex: 'addr'
                }, {
                    header: 'Address Name'.t(),
                    width: Ung.TableConfig.emailFieldWidth,
                    sortable: true,
                    dataIndex: 'addr_name'
                }, {
                    header: 'Address Kind'.t(),
                    width: 60,
                    sortable: true,
                    dataIndex: 'addr_kind'
                }, {
                    header: 'Sender'.t(),
                    width: Ung.TableConfig.emailFieldWidth,
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
                    name: 'type'
                }, {
                    name: 'client_addr',
                    sortType: 'asIp'
                }],
                columns: [{
                    header: 'Timestamp'.t(),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        // return i18n.timestampFormat(value);
                    }
                }, {
                    header: 'Client'.t(),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'client_addr'
                }, {
                    header: 'Username'.t(),
                    width: Ung.TableConfig.usernameFieldWidth,
                    sortable: true,
                    dataIndex: 'login_name'
                }, {
                    header: 'Domain'.t(),
                    width: Ung.TableConfig.usernameFieldWidth,
                    sortable: true,
                    dataIndex: 'domain'
                }, {
                    header: 'Action'.t(),
                    width: 100,
                    sortable: true,
                    dataIndex: 'type',
                    renderer: Ext.bind(function(value) {
                        switch(value) {
                        case 'I': return 'login'.t();
                        case 'U': return 'update'.t();
                        case 'O': return 'logout'.t();
                        default: return 'unknown'.t();
                        }
                    }, this)
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
                    convert: Ext.bind(function(value) {
                        return value ?  'success'.t(): 'failed'.t();
                    }, this)
                }, {
                    name: 'local',
                    type: 'string',
                    convert: Ext.bind(function(value) {
                        return value ?  'local'.t(): 'remote'.t();
                    }, this)
                }, {
                    name: 'client_address',
                    type: 'string'
                }, {
                    name: 'reason',
                    type: 'string'
                }],
                columns: [{
                    header: 'Timestamp'.t(),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        // return i18n.timestampFormat(value);
                    }
                }, {
                    header: 'Login'.t(),
                    width: 120,
                    sortable: true,
                    dataIndex: 'login'
                }, {
                    header: 'Success'.t(),
                    width: 120,
                    sortable: true,
                    dataIndex: 'succeeded'
                }, {
                    header: 'Local'.t(),
                    width: 120,
                    sortable: true,
                    dataIndex: 'local'
                }, {
                    header: 'Client Address'.t(),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'client_addr'
                }, {
                    header: 'Reason'.t(),
                    width: 200,
                    sortable: true,
                    dataIndex: 'reason',
                    renderer: Ext.bind(function(value) {
                        switch(value) {
                        case 'U': return 'invalid username'.t();
                        case 'P': return 'invalid password'.t();
                        default: return '';
                        }
                    }, this)
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
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        // return i18n.timestampFormat(value);
                    }
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
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        // return i18n.timestampFormat(value);
                    }
                }, {
                    header: 'Address'.t(),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'address'
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
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        // return i18n.timestampFormat(value);
                    }
                }, {
                    header: 'MAC Address'.t(),
                    width: Ung.TableConfig.macFieldWidth,
                    sortable: true,
                    dataIndex: 'mac_address'
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
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        // return i18n.timestampFormat(value);
                    }
                }, {
                    header: 'Username'.t(),
                    width: Ung.TableConfig.usernameFieldWidth,
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
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        // return i18n.timestampFormat(value);
                    }
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
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        // return i18n.timestampFormat(value);
                    }
                },{
                    header: 'Interface Name'.t(),
                    width: 120,
                    sortable: true,
                    dataIndex: 'name'
                },{
                    header: 'Interface Id'.t(),
                    width: 120,
                    sortable: true,
                    dataIndex: 'interface_id'
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
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        // return i18n.timestampFormat(value);
                    }
                },{
                    header: 'Interface Name'.t(),
                    width: 120,
                    sortable: true,
                    dataIndex: 'name'
                },{
                    header: 'Interface Id'.t(),
                    width: 120,
                    sortable: true,
                    dataIndex: 'interface_id'
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
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        // return i18n.timestampFormat(value);
                    }
                },{
                    header: 'Event Id'.t(),
                    width: 100,
                    sortable: true,
                    dataIndex: 'event_id'
                },{
                    header: 'Address'.t(),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'client_address'
                },{
                    header: 'Username'.t(),
                    width: Ung.TableConfig.usernameFieldWidth,
                    sortable: true,
                    dataIndex: 'client_username'
                },{
                    header: 'Protocol'.t(),
                    width: 120,
                    sortable: true,
                    dataIndex: 'client_protocol'
                },{
                    header: 'Login Time'.t(),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'connect_stamp',
                    renderer: function(value) {
                        // return i18n.timestampFormat(value);
                    }
                },{
                    header: 'Logout Time'.t(),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'goodbye_stamp',
                    renderer: function(value) {
                        // return (value ==='' ? '' : i18n.timestampFormat(value));
                    }
                },{
                    header: 'Elapsed'.t(),
                    width: 120,
                    sortable: true,
                    dataIndex: 'elapsed_time'
                },{
                    header: 'Interface'.t(),
                    width: 80,
                    sortable: true,
                    dataIndex: 'net_interface'
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
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: Ext.bind(function(value) {
                        // return i18n.timestampFormat(value);
                    }, this )
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
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: Ext.bind(function(value) {
                        // return i18n.timestampFormat(value);
                    }, this )
                },{
                    header: 'Interface Id'.t(),
                    width: 120,
                    sortable: true,
                    dataIndex: 'interface_id'
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
                    name: 'policy_id'
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
                // the list of columns
                columns: [{
                    header: 'Timestamp'.t(),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        // return i18n.timestampFormat(value);
                    }
                }, {
                    header: 'Policy Id'.t(),
                    width: 60,
                    sortable: true,
                    dataIndex: 'policy_id',
                    renderer: Ung.panel.Reports.getColumnRenderer('policy_id')
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
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        // return i18n.timestampFormat(value);
                    }
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
                    name: 'policy_id'
                }, {
                    name: 'event_id'
                },{
                    name: 'client_addr',
                    sortType: 'asIp'
                },{
                    name: 'login_name'
                },{
                    name: 'auth_type'
                },{
                    name: 'event_info'
                }],
                columns: [{
                    header: 'Timestamp'.t(),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        // return i18n.timestampFormat(value);
                    }
                }, {
                    header: 'Policy Id'.t(),
                    width: 60,
                    sortable: true,
                    dataIndex: 'policy_id',
                    renderer: Ung.panel.Reports.getColumnRenderer('policy_id')
                }, {
                    header: 'Event Id'.t(),
                    width: 60,
                    sortable: true,
                    dataIndex: 'event_id'
                }, {
                    header: 'Client'.t(),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'client_addr'
                }, {
                    header: 'Username'.t(),
                    width: Ung.TableConfig.usernameFieldWidth,
                    sortable: true,
                    dataIndex: 'login_name',
                    flex:1
                }, {
                    header: 'Action'.t(),
                    width: 165,
                    sortable: true,
                    dataIndex: 'event_info',
                    renderer: Ext.bind(function( value ) {
                        switch ( value ) {
                        case 'LOGIN':
                            return  'Login Success'.t();
                        case 'FAILED':
                            return  'Login Failure'.t();
                        case 'TIMEOUT':
                            return  'Session Timeout'.t();
                        case 'INACTIVE':
                            return  'Idle Timeout'.t();
                        case 'USER_LOGOUT':
                            return  'User Logout'.t();
                        case 'ADMIN_LOGOUT':
                            return  'Admin Logout'.t();
                        }
                        return '';
                    }, this )
                }, {
                    header: 'Authentication'.t(),
                    width: 165,
                    sortable: true,
                    dataIndex: 'auth_type',
                    renderer: Ext.bind(function( value ) {
                        switch ( value ) {
                        case 'NONE':
                            return  'None'.t();
                        case 'LOCAL_DIRECTORY':
                            return  'Local Directory'.t();
                        case 'ACTIVE_DIRECTORY':
                            return  'Active Directory'.t();
                        case 'RADIUS':
                            return  'RADIUS'.t();
                        case 'CUSTOM':
                            return  'Custom'.t();
                        }
                        return '';
                    }, this )
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
                    sortType: 'asInt'
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
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        // return i18n.timestampFormat(value);
                    }
                }, {
                    header: 'Sid'.t(),
                    width: 70,
                    sortable: true,
                    dataIndex: 'sig_id',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'Gid'.t(),
                    width: 70,
                    sortable: true,
                    dataIndex: 'gen_id',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'Cid'.t(),
                    width: 70,
                    sortable: true,
                    dataIndex: 'class_id',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: 'Source Address'.t(),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'source_addr'
                }, {
                    header: 'Source port'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'source_port',
                    filter: {
                        type: 'numeric'
                    },
                    renderer: function(value, metaData, record, row, col, store, gridView){
                        if( record.get('protocol') === 1 ){
                            return '';
                        }
                        return value;
                    }
                }, {
                    header: 'Destination Address'.t(),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'dest_addr'
                }, {
                    header: 'Destination port'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'dest_port',
                    filter: {
                        type: 'numeric'
                    },
                    renderer: function(value, metaData, record, row, col, store, gridView){
                        if( record.get('protocol') === 1 ) {
                            return '';
                        }
                        return value;
                    }
                }, {
                    header: 'Protocol'.t(),
                    width: 70,
                    sortable: true,
                    dataIndex: 'protocol',
                    renderer: function(value, metaData, record, row, col, store, gridView){
                        switch(value){
                        case 1:
                            return 'ICMP'.t();
                        case 17:
                            return 'UDP'.t();
                        case 6:
                            return 'TCP'.t();
                        }
                        return value;
                    }
                }, {
                    header: 'Blocked'.t(),
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'blocked',
                    filter: {
                        type: 'boolean',
                        yesText: 'true'.t(),
                        noText: 'false'.t()
                    }
                }, {
                    header: 'Category'.t(),
                    width: 200,
                    sortable: true,
                    dataIndex: 'category'
                }, {
                    header: 'Classtype'.t(),
                    width: 200,
                    sortable: true,
                    dataIndex: 'classtype'
                }, {
                    header: 'Msg'.t(),
                    width: 200,
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
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: Ext.bind(function(value) {
                        // return i18n.timestampFormat(value);
                    }, this )
                }, {
                    header: 'Type'.t(),
                    width: 120,
                    sortable: true,
                    dataIndex: 'type'
                }, {
                    header: 'Client Name'.t(),
                    width: Ung.TableConfig.usernameFieldWidth,
                    sortable: true,
                    dataIndex: 'client_name'
                }, {
                    header: 'Client Address'.t(),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'remote_address'
                }, {
                    header: 'Pool Address'.t(),
                    width: Ung.TableConfig.ipFieldWidth,
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
                    width: 100,
                    sortable: true,
                    dataIndex: 'event_id'
                }, {
                    header: 'Timestamp'.t(),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: Ext.bind(function(value) {
                        // return i18n.timestampFormat(value);
                    }, this )
                }, {
                    header: 'Start Time'.t(),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'start_time',
                    renderer: Ext.bind(function(value) {
                        // return i18n.timestampFormat(value);
                    }, this )
                }, {
                    header: 'End Time'.t(),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'end_time',
                    renderer: Ext.bind(function(value) {
                        // return i18n.timestampFormat(value);
                    }, this )
                }, {
                    header: 'Client Name'.t(),
                    width: 120,
                    sortable: true,
                    dataIndex: 'client_name'
                }, {
                    header: 'Client Address'.t(),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'remote_address'
                }, {
                    header: 'Client Port'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'remote_port'
                }, {
                    header: 'Pool Address'.t(),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'pool_address'
                }, {
                    header: 'RX Bytes'.t(),
                    width: 120,
                    sortable: true,
                    dataIndex: 'rx_bytes',
                    renderer: function(value) {
                        var kb = value/1024;
                        return (Math.round( kb*10 )/10).toString() + ' KB';
                    }
                }, {
                    header: 'TX Bytes'.t(),
                    width: 120,
                    sortable: true,
                    dataIndex: 'tx_bytes',
                    renderer: function(value) {
                        var kb = value/1024;
                        return (Math.round( kb*10 )/10).toString() + ' KB';
                    }
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
                // the list of columns
                columns: [{
                    header: 'Timestamp'.t(),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        // return i18n.timestampFormat(value);
                    }
                },{
                    header: 'Description'.t(),
                    width: 200,
                    sortable: true,
                    dataIndex: 'description'
                },{
                    header: 'Summary Text'.t(),
                    flex: 1,
                    width: 500,
                    sortable: true,
                    dataIndex: 'summary_text'
                },{
                    header: 'JSON'.t(),
                    width: 500,
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
                // the list of columns
                columns: [{
                    header: 'Timestamp'.t(),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        // return i18n.timestampFormat(value);
                    }
                },{
                    header: 'Description'.t(),
                    width: 200,
                    sortable: true,
                    dataIndex: 'description'
                },{
                    header: 'Summary Text'.t(),
                    flex: 1,
                    width: 500,
                    sortable: true,
                    dataIndex: 'summary_text'
                },{
                    header: 'JSON'.t(),
                    width: 500,
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
                    name: 'policy_id'
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
                    name: 'client_intf'
                }, {
                    name: 'server_intf'
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
                // the list of columns
                columns: [{
                    header: 'Event Id'.t(),
                    width: 100,
                    sortable: true,
                    dataIndex: 'event_id'
                }, {
                    header: 'Timestamp'.t(),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        // return i18n.timestampFormat(value);
                    }
                }, {
                    header: 'Policy Id'.t(),
                    width: 60,
                    sortable: true,
                    dataIndex: 'policy_id',
                    renderer: Ung.panel.Reports.getColumnRenderer('policy_id')
                }, {
                    header: 'Session Id'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'session_id'
                }, {
                    header: 'Request Id'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'request_id'
                }, {
                    header: 'Method'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'method'
                }, {
                    header: 'Client Interface'.t() ,
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'client_intf'
                }, {
                    header: 'Server Interface'.t() ,
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'server_intf'
                }, {
                    header: 'Client'.t(),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'c_client_addr'
                }, {
                    header: 'New Client'.t(),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 's_client_addr'
                }, {
                    header: 'Original Server'.t() ,
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'c_server_addr'
                }, {
                    header: 'Server'.t() ,
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 's_server_addr'
                }, {
                    header: 'Hostname'.t(),
                    width: Ung.TableConfig.hostnameFieldWidth,
                    sortable: true,
                    dataIndex: 'hostname'
                }, {
                    header: 'Username'.t(),
                    width: Ung.TableConfig.usernameFieldWidth,
                    sortable: true,
                    dataIndex: 'username'
                }, {
                    header: 'File Name'.t(),
                    flex:1,
                    width: Ung.TableConfig.uriFieldWidth,
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
                    width: Ung.TableConfig.ipFieldWidth,
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
                    name: 'action'
                }, {
                    name: 'size',
                    sortType: 'asInt'
                }, {
                    name: 'reason'
                }],
                columns: [{
                    header: 'Timestamp'.t(),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        // return i18n.timestampFormat(value);
                    }
                }, {
                    header: 'Address'.t(),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'address'
                }, {
                    header: 'Action'.t(),
                    width: 120,
                    sortable: true,
                    dataIndex: 'action',
                    renderer: function(value) {
                        if ( value === 1 ) {
                            return '1 (' + 'Given'.t() + ')';
                        }
                        if ( value === 2 ) {
                            return '2 (' + 'Exceeded'.t() + ')';
                        }
                        return value;
                    }
                }, {
                    header: 'Size'.t(),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'size'
                }, {
                    header: 'Reason'.t(),
                    width: 200,
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
                    name: 'settings_file'
                }],
                // the list of columns
                columns: [{
                    header: 'Timestamp'.t(),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        // return i18n.timestampFormat(value);
                    }
                }, {
                    header: 'Username'.t(),
                    width: Ung.TableConfig.usernameFieldWidth,
                    sortable: true,
                    dataIndex: 'username'
                }, {
                    header: 'Hostname'.t(),
                    width: Ung.TableConfig.hostnameFieldWidth,
                    sortable: true,
                    dataIndex: 'hostname'
                }, {
                    header: 'Settings File'.t(),
                    flex:1,
                    width: Ung.TableConfig.uriFieldWidth,
                    dataIndex: 'settings_file',
                    renderer: function( value ){
                        value = value.replace( /^.*\/settings\//, '' );
                        return value;
                    }
                }]
            }
        };
        if(Ung.Main.webuiMode) {
            this.tableConfig.settings_changes.columns.push({
                header: 'Differences'.t(),
                xtype: 'actioncolumn',
                align: 'center',
                dataIndex: 'settings_file',
                width: 100,
                items: [{
                    icon: '/skins/default/images/admin/icons/icon_detail.png',
                    tooltip: 'Show difference between previous version'.t(),
                    handler: function(view, rowIndex, colIndex, item, e, record) {
                        if( !this.diffWindow ) {
                            var columnRenderer = function(value, meta, record) {
                                var action = record.get('action');
                                if( action === 3){
                                    meta.style = 'background-color:#ffff99';
                                }else if(action === 2) {
                                    meta.style = 'background-color:#ffdfd9';
                                }else if(action === 1) {
                                    meta.style = 'background-color:#d9f5cb';
                                }
                                return value;
                            };
                            this.diffWindow = Ext.create('Ung.Window',{
                                name: 'diffWindow',
                                title: 'Settings Difference'.t(),
                                closeAction: 'hide',
                                items: [Ext.create('Ung.grid.Panel', {
                                    name: 'gridDiffs',
                                    width: 200,
                                    height: 200,
                                    hasAdd: false,
                                    hasEdit: false,
                                    hasDelete: false,
                                    initialLoad: function() {},
                                    cls: 'diff-grid',
                                    dataFn: function(handler) {
                                        this.getStore().getProxy().setData([]);
                                        this.getStore().load();
                                        rpc.settingsManager.getDiff(Ext.bind(function(result,exception) {
                                            var diffWindow = this.up('window[name=diffWindow]');
                                            if (diffWindow === null || !diffWindow.isVisible()) {
                                                return;
                                            }
                                            if(exception) {
                                                this.getView().setLoading(false);
                                                handler(result, exception);
                                                return;
                                            }
                                            var diffData = [];
                                            var diffLines = result.split('\n');
                                            var action;
                                            var previousAction, previousLine, currentAction, currentLine;
                                            for( var i = 0; i < diffLines.length; i++) {
                                                previousAction = diffLines[i].substr(0,1);
                                                previousLine = diffLines[i].substr(1,510);
                                                currentAction = diffLines[i].substr(511,1);
                                                currentLine = diffLines[i].substr(512);

                                                if( previousAction !== '<' && previousAction !== '>') {
                                                    previousLine = previousAction + previousLine;
                                                    previousAction = -1;
                                                }
                                                if( currentAction !== '<' && currentAction !== '>' && currentAction !== '|'){
                                                    currentLine = currentAction + currentLine;
                                                    currentAction = -1;
                                                }

                                                if( currentAction === '|' ) {
                                                    action = 3;
                                                } else if(currentAction === '<') {
                                                    action = 2;
                                                } else if(currentAction === '>') {
                                                    action = 1;
                                                } else {
                                                    action = 0;
                                                }

                                                diffData.push({
                                                    line: (i + 1),
                                                    previous: previousLine.replace(/\s+$/,'').replace(/\s/g, '&nbsp;'),
                                                    current: currentLine.replace(/\s+$/,'').replace(/\s/g, '&nbsp;'),
                                                    action: action
                                                });
                                            }
                                            handler({javaClass:'java.util.LinkedList', list: diffData});
                                        },this), this.fileName);
                                    },
                                    fields: [{
                                        name: 'line'
                                    }, {
                                        name: 'previous'
                                    }, {
                                        name: 'current'
                                    }, {
                                        name: 'action'
                                    }],
                                    columnsDefaultSortable: false,
                                    columns:[{
                                        text: 'Line'.t(),
                                        dataIndex: 'line',
                                        renderer: columnRenderer
                                    },{
                                        text: 'Previous'.t(),
                                        flex: 1,
                                        dataIndex: 'previous',
                                        renderer: columnRenderer
                                    },{
                                        text: 'Current'.t(),
                                        flex: 1,
                                        dataIndex: 'current',
                                        renderer: columnRenderer
                                    }]
                                })],
                                buttons: [{
                                    text: 'Close'.t(),
                                    handler: Ext.bind(function() {
                                        this.diffWindow.hide();
                                    }, this)
                                }],
                                update: function(fileName) {
                                    var grid = this.down('grid[name=gridDiffs]');
                                    grid.fileName = fileName;
                                    grid.reload();

                                },
                                doSize : function() {
                                    this.maximize();
                                }
                            });
                            this.on('beforedestroy', Ext.bind(function() {
                                if(this.diffWindow) {
                                    Ext.destroy(this.diffWindow);
                                    this.diffWindow = null;
                                }
                            }, this));
                        }
                        this.diffWindow.show();
                        this.diffWindow.update(record.get('settings_file'));
                    }
                }]
            });
        }
        var key, columns, i;
        for(key in this.tableConfig) {
            columns = this.tableConfig[key].columns;
            for(i=0; i<columns.length; i++) {
                if(columns[i].dataIndex && !columns[i].renderer && !columns[i].filter) {
                    columns[i].filter = { type: 'string' };
                }
            }
        }

    }
});
