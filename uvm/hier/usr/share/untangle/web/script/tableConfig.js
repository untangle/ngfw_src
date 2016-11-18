Ext.define('Ung.TableConfig', {
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
        var i, j, table, column, systemColumns, systemColumnsMap, tableConfigColumns, tableConfigColumnsMap;
        var systemTables = rpc.reportsManager.getTables();
        var systemTablesMap={};
        var missingTables = [];
        for(i=0; i<systemTables.length;i++) {
            systemTablesMap[systemTables[i]] = true;

            if(!this.tableConfig[systemTables[i]]) {

                // ignore "totals" tables (from old reports and will be deprecated soon)
                if ( systemTables[i].indexOf("totals") != -1 )
                    continue;
                // ignore "mail_msgs" table (will be deprecated soon)
                if ( systemTables[i].indexOf("mail_msgs") != -1 )
                    continue;

                missingTables.push(systemTables[i]);
            }
        }
        if(missingTables.length>0) {
            console.log("Warning: Missing tables: "+missingTables.join(", "));
        }
        var extraTables = [];
        for(table in this.tableConfig) {
            if(!systemTablesMap[table]) {
                extraTables.push(table);
            }
        }
        if(extraTables.length>0) {
            console.log("Warning: Extra tables: "+extraTables.join(", "));
        }
        for(table in this.tableConfig) {
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
                    if ( columnConfig == null ) {
                        missingColumns.push(systemColumns[i]);
                    } else {
                        if (! columnConfig.width )
                            console.log("Warning: Table '"+table+"' Columns: '"+columnConfig.dataIndex+"' missing width");
                    }
                }
                if(missingColumns.length>0) {
                    console.log("Warning: Table '"+table+"' Missing columns: "+missingColumns.join(", "));
                }

                var extraColumns = [];
                for(column in tableConfigColumnsMap) {
                    if(!systemColumnsMap[column]) {
                        extraColumns.push(column);
                    }
                }
                if(extraColumns.length>0) {
                    console.log("Warning: Table '"+table+"' Extra columns: "+extraColumns.join(", "));
                }

            }
        }

    },
    getColumnsForTable: function(table, store) {
        if(table!=null) {
            var tableConfig = this.getConfig(table);
            var columns = [], col;
            if(tableConfig!=null && Ext.isArray(tableConfig.columns)) {
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
            for(table in this.tableConfig) {
                columns = this.tableConfig[table].columns;
                for(i=0; i<columns.length; i++) {
                    dataIndex = columns[i].dataIndex;
                    if(dataIndex && !this.columnsHumanReadableNames[dataIndex]) {
                        this.columnsHumanReadableNames[dataIndex] = columns[i].header;
                    }
                }
            }
        }
        if(!columnName) columnName="";
        var readableName = this.columnsHumanReadableNames[columnName];
        return readableName!=null ? readableName : columnName.replace(/_/g," ");
    },
    httpEventConvertReason: function(value) {
        if(Ext.isEmpty(value)) {
            return null;
        }
        switch ( value ) {
          case 'D': return i18n._("in Categories Block list") + " (D)";
          case 'U': return i18n._("in Site Block list") + " (U)";
          case 'E': return i18n._("in File Block list") + " (E)";
          case 'M': return i18n._("in MIME Types Block list") + " (M)";
          case 'H': return i18n._("hostname is an IP address") + " (H)";
          case 'I': return i18n._("in Site Pass list") + " (I)";
          case 'R': return i18n._("referer in Site Pass list") + " (R)";
          case 'C': return i18n._("in Clients Pass list") + " (C)";
          case 'B': return i18n._("in Unblocked list") + " (B)";
          case 'F': return i18n._("in Rules list") + " (F)";
          case 'N': return i18n._("no rule applied") + " (N)";
        default: return i18n._("no rule applied");
        }
    },
    mailEventConvertAction: function(value) {
        if(Ext.isEmpty(value)) {
            return "";
        }
        switch (value) {
            case 'P': return i18n._("pass message");
            case 'M': return i18n._("mark message");
            case 'D': return i18n._("drop message");
            case 'B': return i18n._("block message");
            case 'Q': return i18n._("quarantine message");
            case 'S': return i18n._("pass safelist message");
            case 'Z': return i18n._("pass oversize message");
            case 'O': return i18n._("pass outbound message");
            case 'F': return i18n._("block message (scan failure)");
            case 'G': return i18n._("pass message (scan failure)");
            case 'Y': return i18n._("block message (greylist)");
            default: return i18n._("unknown action");
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
                    name: "c2p_bytes"
                }, {
                    name: "p2c_bytes"
                }, {
                    name: "s2p_bytes"
                }, {
                    name: "p2s_bytes"
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
                    header: i18n._("Session Id"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'session_id'
                }, {
                    header: i18n._("Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: i18n._("End Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'end_time',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: i18n._('Bypassed'),
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'bypassed',
                    filter: {
                        type: 'boolean',
                        yesText: i18n._('true'),
                        noText: i18n._('false')
                    }
                }, {
                    header: i18n._('Entitled'),
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'entitled',
                    filter: {
                        type: 'boolean',
                        yesText: i18n._('true'),
                        noText: i18n._('false')
                    }
                }, {
                    header: i18n._("Protocol"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'protocol',
                    renderer: Ung.panel.Reports.getColumnRenderer('protocol')
                }, {
                    header: i18n._("ICMP Type"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'icmp_type'
                }, {
                    header: i18n._('Policy Id'),
                    width: 60,
                    sortable: true,
                    dataIndex: 'policy_id',
                    renderer: Ung.panel.Reports.getColumnRenderer('policy_id')
                }, {
                    header: i18n._('Policy Rule Id'),
                    width: 60,
                    sortable: true,
                    dataIndex: 'policy_rule_id'
                }, {
                    header: i18n._("Client Interface") ,
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'client_intf'
                }, {
                    header: i18n._("Server Interface") ,
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'server_intf'
                }, {
                    header: i18n._("Client Country") ,
                    width: 80,
                    sortable: true,
                    dataIndex: 'client_country',
                    renderer: function(value) { return Ung.Main.getCountryName(value); }
                }, {
                    header: i18n._("Client Latitude") ,
                    width: 80,
                    sortable: true,
                    dataIndex: 'client_latitude'
                }, {
                    header: i18n._("Client Longitude") ,
                    width: 80,
                    sortable: true,
                    dataIndex: 'client_longitude'
                }, {
                    header: i18n._("Server Country") ,
                    width: 80,
                    sortable: true,
                    dataIndex: 'server_country',
                    renderer: function(value) { return Ung.Main.getCountryName(value); }
                }, {
                    header: i18n._("Server Latitude") ,
                    width: 80,
                    sortable: true,
                    dataIndex: 'server_latitude'
                }, {
                    header: i18n._("Server Longitude") ,
                    width: 80,
                    sortable: true,
                    dataIndex: 'server_longitude'
                }, {
                    header: i18n._("Username"),
                    width: Ung.TableConfig.usernameFieldWidth,
                    sortable: true,
                    dataIndex: 'username'
                }, {
                    header: i18n._("Hostname"),
                    width: Ung.TableConfig.hostnameFieldWidth,
                    sortable: true,
                    dataIndex: 'hostname'
                }, {
                    header: i18n._("Client"),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'c_client_addr'
                }, {
                    header: i18n._("Client Port"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'c_client_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("New Client"),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 's_client_addr'
                }, {
                    header: i18n._("New Client Port"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 's_client_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("Original Server") ,
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'c_server_addr'
                }, {
                    header: i18n._("Original Server Port"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'c_server_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("Server") ,
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 's_server_addr'
                }, {
                    header: i18n._("Server Port"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 's_server_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._('Filter Prefix'),
                    width: 120,
                    sortable: true,
                    dataIndex: 'filter_prefix'
                }, {
                    header: i18n._('Rule Id') + ' (Application Control)',
                    width: 70,
                    sortable: true,
                    dataIndex: 'application_control_ruleid',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._('Priority') + ' (Bandwidth Control)',
                    width: 120,
                    sortable: true,
                    dataIndex: 'bandwidth_control_priority',
                    renderer: function(value) {
                        if (Ext.isEmpty(value)) {
                            return "";
                        }
                        switch(value) {
                            case 0: return "";
                            case 1: return i18n._("Very High");
                            case 2: return i18n._("High");
                            case 3: return i18n._("Medium");
                            case 4: return i18n._("Low");
                            case 5: return i18n._("Limited");
                            case 6: return i18n._("Limited More");
                            case 7: return i18n._("Limited Severely");
                            default: return Ext.String.format(i18n._("Unknown Priority: {0}"), value);
                        }
                    }
                }, {
                    header: i18n._('Rule') + ' (Bandwidth Control)',
                    width: 120,
                    sortable: true,
                    dataIndex: 'bandwidth_control_rule',
                    renderer: function(value) {
                        return Ext.isEmpty(value) ? i18n._("none") : value;
                    }
                }, {
                    header: i18n._('Application') + ' (Application Control)',
                    width: 120,
                    sortable: true,
                    dataIndex: 'application_control_application'
                }, {
                    header: i18n._('ProtoChain') + ' (Application Control)',
                    width: 180,
                    sortable: true,
                    dataIndex: 'application_control_protochain'
                }, {
                    header: i18n._('Category') + ' (Application Control)',
                    width: 80,
                    sortable: true,
                    dataIndex: 'application_control_category'
                }, {
                    header: i18n._('Blocked') + ' (Application Control)',
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'application_control_blocked',
                    filter: {
                        type: 'boolean',
                        yesText: i18n._('true'),
                        noText: i18n._('false')
                    }
                }, {
                    header: i18n._('Flagged') + ' (Application Control)',
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'application_control_flagged',
                    filter: {
                        type: 'boolean',
                        yesText: i18n._('true'),
                        noText: i18n._('false')
                    }
                }, {
                    header: i18n._('Confidence') + ' (Application Control)',
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'application_control_confidence',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._('Detail') + ' (Application Control)',
                    width: 200,
                    sortable: true,
                    dataIndex: 'application_control_detail'
                },{
                    header: i18n._('Protocol') + ' (Application Control Lite)',
                    width: 120,
                    sortable: true,
                    dataIndex: 'application_control_lite_protocol'
                }, {
                    header: i18n._('Blocked') + ' (Application Control Lite)',
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'application_control_lite_blocked',
                    filter: {
                        type: 'boolean',
                        yesText: i18n._('true'),
                        noText: i18n._('false')
                    }
                }, {
                    header: i18n._('Rule Id') + ' (SSL Inspector)',
                    width: 70,
                    sortable: true,
                    dataIndex: 'ssl_inspector_ruleid',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._('Status') + ' (SSL Inspector)',
                    width: 100,
                    sortable: true,
                    dataIndex: 'ssl_inspector_status'
                }, {
                    header: i18n._('Detail') + ' (SSL Inspector)',
                    width: 250,
                    sortable: true,
                    dataIndex: 'ssl_inspector_detail'
                }, {
                    header: i18n._('Blocked') + ' (Firewall)',
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'firewall_blocked',
                    filter: {
                        type: 'boolean',
                        yesText: i18n._('true'),
                        noText: i18n._('false')
                    }
                }, {
                    header: i18n._('Flagged') + ' (Firewall)',
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'firewall_flagged',
                    filter: {
                        type: 'boolean',
                        yesText: i18n._('true'),
                        noText: i18n._('false')
                    }
                }, {
                    header: i18n._('Rule Id') + ' (Firewall)',
                    width: 60,
                    sortable: true,
                    flex:1,
                    dataIndex: 'firewall_rule_index',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._('Captured') + ' (Captive Portal)',
                    width: 100,
                    sortable: true,
                    dataIndex: "captive_portal_blocked",
                    filter: {
                        type: 'boolean',
                        yesText: i18n._('true'),
                        noText: i18n._('false')
                    }
                }, {
                    header: i18n._('Rule Id') + ' (Captive Portal)',
                    width: 60,
                    sortable: true,
                    dataIndex: "captive_portal_rule_index"
                }, {
                    header: i18n._('To-Server Bytes'),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'p2s_bytes',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._('From-Server Bytes'),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 's2p_bytes',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._('To-Client Bytes'),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'p2c_bytes',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._('From-Client Bytes'),
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
                    name: "c2p_bytes"
                }, {
                    name: "p2c_bytes"
                }, {
                    name: "s2p_bytes"
                }, {
                    name: "p2s_bytes"
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
                    header: i18n._("Session Id"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'session_id'
                }, {
                    header: i18n._("Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: i18n._("Start Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'start_time',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: i18n._("End Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'end_time',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: i18n._('Bypassed'),
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'bypassed',
                    filter: {
                        type: 'boolean',
                        yesText: i18n._('true'),
                        noText: i18n._('false')
                    }
                }, {
                    header: i18n._('Entitled'),
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'entitled',
                    filter: {
                        type: 'boolean',
                        yesText: i18n._('true'),
                        noText: i18n._('false')
                    }
                }, {
                    header: i18n._("Protocol"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'protocol',
                    renderer: Ung.panel.Reports.getColumnRenderer('protocol')
                }, {
                    header: i18n._("ICMP Type"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'icmp_type'
                }, {
                    header: i18n._('Policy Id'),
                    width: 60,
                    sortable: true,
                    dataIndex: 'policy_id',
                    renderer: Ung.panel.Reports.getColumnRenderer('policy_id')
                }, {
                    header: i18n._('Policy Rule Id'),
                    width: 60,
                    sortable: true,
                    dataIndex: 'policy_rule_id'
                }, {
                    header: i18n._("Client Interface") ,
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'client_intf'
                }, {
                    header: i18n._("Server Interface") ,
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'server_intf'
                }, {
                    header: i18n._("Client Country") ,
                    width: 80,
                    sortable: true,
                    dataIndex: 'client_country',
                    renderer: function(value) { return Ung.Main.getCountryName(value); }
                }, {
                    header: i18n._("Client Latitude") ,
                    width: 80,
                    sortable: true,
                    dataIndex: 'client_latitude'
                }, {
                    header: i18n._("Client Longitude") ,
                    width: 80,
                    sortable: true,
                    dataIndex: 'client_longitude'
                }, {
                    header: i18n._("Server Country") ,
                    width: 80,
                    sortable: true,
                    dataIndex: 'server_country',
                    renderer: function(value) { return Ung.Main.getCountryName(value); }
                }, {
                    header: i18n._("Server Latitude") ,
                    width: 80,
                    sortable: true,
                    dataIndex: 'server_latitude'
                }, {
                    header: i18n._("Server Longitude") ,
                    width: 80,
                    sortable: true,
                    dataIndex: 'server_longitude'
                }, {
                    header: i18n._("Username"),
                    width: Ung.TableConfig.usernameFieldWidth,
                    sortable: true,
                    dataIndex: 'username'
                }, {
                    header: i18n._("Hostname"),
                    width: Ung.TableConfig.hostnameFieldWidth,
                    sortable: true,
                    dataIndex: 'hostname'
                }, {
                    header: i18n._("Client"),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'c_client_addr'
                }, {
                    header: i18n._("Client Port"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'c_client_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("New Client"),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 's_client_addr'
                }, {
                    header: i18n._("New Client Port"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 's_client_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("Original Server") ,
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'c_server_addr'
                }, {
                    header: i18n._("Original Server Port"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'c_server_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("Server") ,
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 's_server_addr'
                }, {
                    header: i18n._("Server Port"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 's_server_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._('Filter Prefix'),
                    width: 120,
                    sortable: true,
                    dataIndex: 'filter_prefix'
                }, {
                    header: i18n._('Rule Id') + ' (Application Control)',
                    width: 70,
                    sortable: true,
                    dataIndex: 'application_control_ruleid',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._('Priority') + ' (Bandwidth Control)',
                    width: 120,
                    sortable: true,
                    dataIndex: 'bandwidth_control_priority',
                    renderer: function(value) {
                        if (Ext.isEmpty(value)) {
                            return "";
                        }
                        switch(value) {
                            case 0: return "";
                            case 1: return i18n._("Very High");
                            case 2: return i18n._("High");
                            case 3: return i18n._("Medium");
                            case 4: return i18n._("Low");
                            case 5: return i18n._("Limited");
                            case 6: return i18n._("Limited More");
                            case 7: return i18n._("Limited Severely");
                            default: return Ext.String.format(i18n._("Unknown Priority: {0}"), value);
                        }
                    }
                }, {
                    header: i18n._('Rule') + ' (Bandwidth Control)',
                    width: 120,
                    sortable: true,
                    dataIndex: 'bandwidth_control_rule',
                    renderer: function(value) {
                        return Ext.isEmpty(value) ? i18n._("none") : value;
                    }
                }, {
                    header: i18n._('Application') + ' (Application Control)',
                    width: 120,
                    sortable: true,
                    dataIndex: 'application_control_application'
                }, {
                    header: i18n._('ProtoChain') + ' (Application Control)',
                    width: 180,
                    sortable: true,
                    dataIndex: 'application_control_protochain'
                }, {
                    header: i18n._('Category') + ' (Application Control)',
                    width: 80,
                    sortable: true,
                    dataIndex: 'application_control_category'
                }, {
                    header: i18n._('Blocked') + ' (Application Control)',
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'application_control_blocked',
                    filter: {
                        type: 'boolean',
                        yesText: i18n._('true'),
                        noText: i18n._('false')
                    }
                }, {
                    header: i18n._('Flagged') + ' (Application Control)',
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'application_control_flagged',
                    filter: {
                        type: 'boolean',
                        yesText: i18n._('true'),
                        noText: i18n._('false')
                    }
                }, {
                    header: i18n._('Confidence') + ' (Application Control)',
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'application_control_confidence',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._('Detail') + ' (Application Control)',
                    width: 200,
                    sortable: true,
                    dataIndex: 'application_control_detail'
                },{
                    header: i18n._('Protocol') + ' (Application Control Lite)',
                    width: 120,
                    sortable: true,
                    dataIndex: 'application_control_lite_protocol'
                }, {
                    header: i18n._('Blocked') + ' (Application Control Lite)',
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'application_control_lite_blocked',
                    filter: {
                        type: 'boolean',
                        yesText: i18n._('true'),
                        noText: i18n._('false')
                    }
                }, {
                    header: i18n._('Rule Id') + ' (SSL Inspector)',
                    width: 70,
                    sortable: true,
                    dataIndex: 'ssl_inspector_ruleid',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._('Status') + ' (SSL Inspector)',
                    width: 100,
                    sortable: true,
                    dataIndex: 'ssl_inspector_status'
                }, {
                    header: i18n._('Detail') + ' (SSL Inspector)',
                    width: 250,
                    sortable: true,
                    dataIndex: 'ssl_inspector_detail'
                }, {
                    header: i18n._('Blocked') + ' (Firewall)',
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'firewall_blocked',
                    filter: {
                        type: 'boolean',
                        yesText: i18n._('true'),
                        noText: i18n._('false')
                    }
                }, {
                    header: i18n._('Flagged') + ' (Firewall)',
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'firewall_flagged',
                    filter: {
                        type: 'boolean',
                        yesText: i18n._('true'),
                        noText: i18n._('false')
                    }
                }, {
                    header: i18n._('Rule Id') + ' (Firewall)',
                    width: 60,
                    sortable: true,
                    flex:1,
                    dataIndex: 'firewall_rule_index',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._('Captured') + ' (Captive Portal)',
                    width: 100,
                    sortable: true,
                    dataIndex: "captive_portal_blocked",
                    filter: {
                        type: 'boolean',
                        yesText: i18n._('true'),
                        noText: i18n._('false')
                    }
                }, {
                    header: i18n._('Rule Id') + ' (Captive Portal)',
                    width: 60,
                    sortable: true,
                    dataIndex: "captive_portal_rule_index"
                }, {
                    header: i18n._('From-Server Bytes'),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 's2c_bytes',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._('From-Client Bytes'),
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
                    name: 'web_filter_lite_blocked'
                }, {
                    name: 'web_filter_blocked'
                }, {
                    name: 'web_filter_lite_flagged'
                }, {
                    name: 'web_filter_flagged'
                }, {
                    name: 'web_filter_lite_category',
                    type: 'string'
                }, {
                    name: 'web_filter_category',
                    type: 'string'
                }, {
                    name: 'web_filter_lite_reason',
                    type: 'string',
                    convert: Ung.TableConfig.httpEventConvertReason
                }, {
                    name: 'web_filter_reason',
                    type: 'string',
                    convert: Ung.TableConfig.httpEventConvertReason
                }, {
                    name: 'ad_blocker_action',
                    type: 'string',
                    convert: function(value) {
                        return (value == 'B')?i18n._("block") : i18n._("pass");
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
                    header: i18n._("Request Id"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'request_id'
                }, {
                    header: i18n._("Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: i18n._('Policy Id'),
                    width: 60,
                    sortable: true,
                    dataIndex: 'policy_id',
                    renderer: Ung.panel.Reports.getColumnRenderer('policy_id')
                }, {
                    header: i18n._("Session Id"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'session_id'
                }, {
                    header: i18n._("Client Interface") ,
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'client_intf'
                }, {
                    header: i18n._("Server Interface") ,
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'server_intf'
                }, {
                    header: i18n._("Client"),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'c_client_addr'
                }, {
                    header: i18n._("Client Port"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'c_client_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("New Client"),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 's_client_addr'
                }, {
                    header: i18n._("New Client Port"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 's_client_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("Original Server") ,
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'c_server_addr'
                }, {
                    header: i18n._("Original Server Port"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'c_server_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("Server") ,
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 's_server_addr'
                }, {
                    header: i18n._("Server Port"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 's_server_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("Username"),
                    width: Ung.TableConfig.usernameFieldWidth,
                    sortable: true,
                    dataIndex: 'username'
                }, {
                    header: i18n._("Hostname"),
                    width: Ung.TableConfig.hostnameFieldWidth,
                    sortable: true,
                    dataIndex: 'hostname'
                }, {
                    header: i18n._("Domain"),
                    width: Ung.TableConfig.hostnameFieldWidth,
                    sortable: true,
                    dataIndex: 'domain'
                }, {
                    header: i18n._("Host"),
                    width: Ung.TableConfig.hostnameFieldWidth,
                    sortable: true,
                    dataIndex: 'host'
                }, {
                    header: i18n._("Uri"),
                    flex:1,
                    width: Ung.TableConfig.uriFieldWidth,
                    sortable: true,
                    dataIndex: 'uri'
                }, {
                    header: i18n._("Method"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'method',
                    renderer: function(value) {
                        // untranslated because these are HTTP methods
                        switch ( value ) {
                            case 'O': return "OPTIONS" + " (O)";
                            case 'G': return "GET" + " (G)";
                            case 'H': return "HEAD" + " (H)";
                            case 'P': return "POST" + " (P)";
                            case 'U': return "PUT" + " (U)";
                            case 'D': return "DELETE" + " (D)";
                            case 'T': return "TRACE" + " (T)";
                            case 'C': return "CONNECT" + " (C)";
                            case 'X': return "NON-STANDARD" + " (X)";
                            default: return value;
                        }
                    }
                }, {
                    header: i18n._("Referer"),
                    width: Ung.TableConfig.uriFieldWidth,
                    sortable: true,
                    dataIndex: 'referer'
                }, {
                    header: i18n._("Download Content Length"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 's2c_content_length'
                }, {
                    header: i18n._("Upload Content Length"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'c2s_content_length'
                }, {
                    header: i18n._("Content Type"),
                    width: 150,
                    sortable: true,
                    dataIndex: 's2c_content_type'
                }, {
                    header: i18n._("Blocked") + " (Web Filter Lite)",
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'web_filter_lite_blocked',
                    filter: {
                        type: 'boolean',
                        yesText: i18n._('true'),
                        noText: i18n._('false')
                    }
                }, {
                    header: i18n._("Flagged") + " (Web Filter Lite)",
                    width: Ung.TableConfig.booleanFieldWidth,
                    dataIndex: 'web_filter_lite_flagged',
                    filter: {
                        type: 'boolean',
                        yesText: i18n._('true'),
                        noText: i18n._('false')
                    }
                }, {
                    header: i18n._("Reason For Action") + " (Web Filter Lite)",
                    width: 150,
                    sortable: true,
                    dataIndex: 'web_filter_lite_reason'
                }, {
                    header: i18n._("Category") + " (Web Filter Lite)",
                    width: 120,
                    sortable: true,
                    dataIndex: 'web_filter_lite_category'
                }, {
                    header: i18n._("Blocked") + " (Web Filter)",
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'web_filter_blocked',
                    filter: {
                        type: 'boolean',
                        yesText: i18n._('true'),
                        noText: i18n._('false')
                    }
                }, {
                    header: i18n._("Flagged") + " (Web Filter)",
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'web_filter_flagged',
                    filter: {
                        type: 'boolean',
                        yesText: i18n._('true'),
                        noText: i18n._('false')
                    }
                }, {
                    header: i18n._("Reason For Action") +  " (Web Filter)",
                    width: 150,
                    sortable: true,
                    dataIndex: 'web_filter_reason'
                }, {
                    header: i18n._("Category") + " (Web Filter)",
                    width: 120,
                    sortable: true,
                    dataIndex: 'web_filter_category'
                }, {
                    header: i18n._("Action") + " (Ad Blocker)",
                    width: 120,
                    sortable: true,
                    dataIndex: 'ad_blocker_action'
                }, {
                    header: i18n._("Blocked Cookie") + " (Ad Blocker)",
                    width: 100,
                    sortable: true,
                    dataIndex: 'ad_blocker_cookie_ident'
                }, {
                    header: i18n._('Clean') + ' (Virus Blocker Lite)',
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'virus_blocker_lite_clean',
                    filter: {
                        type: 'boolean',
                        yesText: i18n._('true'),
                        noText: i18n._('false')
                    }
                }, {
                    header: i18n._('Virus Name') + ' (Virus Blocker Lite)',
                    width: 140,
                    sortable: true,
                    dataIndex: 'virus_blocker_lite_name'
                }, {
                    header: i18n._('Clean') + ' (Virus Blocker)',
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'virus_blocker_clean',
                    filter: {
                        type: 'boolean',
                        yesText: i18n._('true'),
                        noText: i18n._('false')
                    }
                }, {
                    header: i18n._('Virus Name') + ' (Virus Blocker)',
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
                    header: i18n._("Event Id"),
                    width: 60,
                    sortable: true,
                    dataIndex: "event_id"
                }, {
                    header: i18n._("Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: i18n._('Policy Id'),
                    width: 60,
                    sortable: true,
                    dataIndex: 'policy_id',
                    renderer: Ung.panel.Reports.getColumnRenderer('policy_id')
                }, {
                    header: i18n._('Request Id'),
                    width: 60,
                    sortable: true,
                    dataIndex: 'request_id'
                }, {
                    header: i18n._("Session Id"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'session_id'
                }, {
                    header: i18n._("Client Interface") ,
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'client_intf'
                }, {
                    header: i18n._("Server Interface") ,
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'server_intf'
                }, {
                    header: i18n._("Username"),
                    width: Ung.TableConfig.usernameFieldWidth,
                    sortable: true,
                    dataIndex: 'username'
                }, {
                    header: i18n._("Hostname"),
                    width: Ung.TableConfig.hostnameFieldWidth,
                    sortable: true,
                    dataIndex: 'hostname'
                }, {
                    header: i18n._("Client"),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'c_client_addr'
                }, {
                    header: i18n._("Client Port"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'c_client_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("New Client"),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 's_client_addr'
                }, {
                    header: i18n._("New Client Port"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 's_client_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("Original Server") ,
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'c_server_addr'
                }, {
                    header: i18n._("Original Server Port"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'c_server_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("Server") ,
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 's_server_addr'
                }, {
                    header: i18n._("Server Port"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 's_server_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("Host"),
                    width: Ung.TableConfig.hostnameFieldWidth,
                    sortable: true,
                    dataIndex: 'host'
                }, {
                    header: i18n._("Uri"),
                    flex:1,
                    width: Ung.TableConfig.uriFieldWidth,
                    sortable: true,
                    dataIndex: 'uri'
                }, {
                    header: i18n._("Query Term"),
                    flex:1,
                    width: Ung.TableConfig.uriFieldWidth,
                    sortable: true,
                    dataIndex: 'term'
                }, {
                    header: i18n._("Method"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'method'
                }, {
                    header: i18n._("Download Content Length"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 's2c_content_length'
                }, {
                    header: i18n._("Upload Content Length"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'c2s_content_length'
                }, {
                    header: i18n._("Content Type"),
                    width: 150,
                    sortable: true,
                    dataIndex: 's2c_content_type'
                }, {
                    header: i18n._("Server"),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'c_server_addr'
                }, {
                    header: i18n._("Server Port"),
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
                    header: i18n._("Event Id"),
                    width: 60,
                    sortable: true,
                    dataIndex: "event_id"
                }, {
                    header: i18n._("Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: i18n._("Session Id"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'session_id'
                }, {
                    header: i18n._('Policy Id'),
                    width: 60,
                    sortable: true,
                    dataIndex: 'policy_id',
                    renderer: Ung.panel.Reports.getColumnRenderer('policy_id')
                }, {
                    header: i18n._("Message Id"),
                    width: 60,
                    sortable: true,
                    dataIndex: "msg_id"
                }, {
                    header: i18n._("Client Interface") ,
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'client_intf'
                }, {
                    header: i18n._("Server Interface") ,
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'server_intf'
                }, {
                    header: i18n._("Username"),
                    width: Ung.TableConfig.hostnameFieldWidth,
                    sortable: true,
                    dataIndex: 'username'
                }, {
                    header: i18n._("Hostname"),
                    width: Ung.TableConfig.hostnameFieldWidth,
                    sortable: true,
                    dataIndex: 'hostname'
                }, {
                    header: i18n._("Client"),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'c_client_addr'
                }, {
                    header: i18n._("Client Port"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'c_client_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("New Client"),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 's_client_addr'
                }, {
                    header: i18n._("New Client Port"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 's_client_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("Original Server") ,
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'c_server_addr'
                }, {
                    header: i18n._("Original Server Port"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'c_server_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("Server") ,
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 's_server_addr'
                }, {
                    header: i18n._("Server Port"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 's_server_port',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("Receiver"),
                    width: Ung.TableConfig.emailFieldWidth,
                    sortable: true,
                    dataIndex: 'addr'
                }, {
                    header: i18n._("Address Name"),
                    width: Ung.TableConfig.emailFieldWidth,
                    sortable: true,
                    dataIndex: 'addr_name'
                }, {
                    header: i18n._("Address Kind"),
                    width: 60,
                    sortable: true,
                    dataIndex: 'addr_kind'
                }, {
                    header: i18n._("Sender"),
                    width: Ung.TableConfig.emailFieldWidth,
                    sortable: true,
                    dataIndex: 'sender'
                }, {
                    header: i18n._("Subject"),
                    flex:1,
                    width: 150,
                    sortable: true,
                    dataIndex: 'subject'
                }, {
                    header: i18n._('Name') + ' (Virus Blocker Lite)',
                    width: 140,
                    sortable: true,
                    dataIndex: 'virus_blocker_lite_name'
                }, {
                    header: i18n._('Clean') + ' (Virus Blocker Lite)',
                    width: 140,
                    sortable: true,
                    dataIndex: 'virus_blocker_lite_clean'
                }, {
                    header: i18n._('Name') + ' (Virus Blocker)',
                    width: 140,
                    sortable: true,
                    dataIndex: 'virus_blocker_name'
                }, {
                    header: i18n._('Clean') + ' (Virus Blocker)',
                    width: 140,
                    sortable: true,
                    dataIndex: 'virus_blocker_clean'
                }, {
                    header: i18n._("Action") + " (Spam Blocker Lite)",
                    width: 125,
                    sortable: true,
                    dataIndex: 'spam_blocker_lite_action'
                }, {
                    header: i18n._("Spam Score") + " (Spam Blocker Lite)",
                    width: 70,
                    sortable: true,
                    dataIndex: 'spam_blocker_lite_score',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("Is Spam") + " (Spam Blocker Lite)",
                    width: 70,
                    sortable: true,
                    dataIndex: 'spam_blocker_lite_is_spam'
                }, {
                    header: i18n._("Detail") + " (Spam Blocker Lite)",
                    width: 125,
                    sortable: true,
                    dataIndex: 'spam_blocker_lite_tests_string'
                }, {
                    header: i18n._("Action") + " (Spam Blocker)",
                    width: 125,
                    sortable: true,
                    dataIndex: 'spam_blocker_action'
                }, {
                    header: i18n._("Spam Score") + " (Spam Blocker)",
                    width: 70,
                    sortable: true,
                    dataIndex: 'spam_blocker_score',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("Is Spam") + " (Spam Blocker)",
                    width: 70,
                    sortable: true,
                    dataIndex: 'spam_blocker_is_spam'
                }, {
                    header: i18n._("Detail") + " (Spam Blocker)",
                    width: 125,
                    sortable: true,
                    dataIndex: 'spam_blocker_tests_string'
                }, {
                    header: i18n._("Action") + " (Phish Blocker)",
                    width: 125,
                    sortable: true,
                    dataIndex: 'phish_blocker_action'
                }, {
                    header: i18n._("Score") + " (Phish Blocker)",
                    width: 70,
                    sortable: true,
                    dataIndex: 'phish_blocker_score'
                }, {
                    header: i18n._("Is Phish") + " (Phish Blocker)",
                    width: 70,
                    sortable: true,
                    dataIndex: 'phish_blocker_is_spam'
                }, {
                    header: i18n._("Detail") + " (Phish Blocker)",
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
                    header: i18n._("Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: i18n._("Client"),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'client_addr'
                }, {
                    header: i18n._("Username"),
                    width: Ung.TableConfig.usernameFieldWidth,
                    sortable: true,
                    dataIndex: 'login_name'
                }, {
                    header: i18n._("Domain"),
                    width: Ung.TableConfig.usernameFieldWidth,
                    sortable: true,
                    dataIndex: 'domain'
                }, {
                    header: i18n._('Action'),
                    width: 100,
                    sortable: true,
                    dataIndex: 'type',
                    renderer: Ext.bind(function(value) {
                        switch(value) {
                            case "I": return i18n._("login");
                            case "U": return i18n._("update");
                            case "O": return i18n._("logout");
                            default: return i18n._("unknown");
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
                        return value ?  i18n._("success"): i18n._("failed");
                    }, this)
                }, {
                    name: 'local',
                    type: 'string',
                    convert: Ext.bind(function(value) {
                        return value ?  i18n._("local"): i18n._("remote");
                    }, this)
                }, {
                    name: 'client_address',
                    type: 'string'
                }, {
                    name: 'reason',
                    type: 'string'
                }],
                columns: [{
                    header: i18n._("Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: i18n._("Login"),
                    width: 120,
                    sortable: true,
                    dataIndex: 'login'
                }, {
                    header: i18n._("Success"),
                    width: 120,
                    sortable: true,
                    dataIndex: 'succeeded'
                }, {
                    header: i18n._("Local"),
                    width: 120,
                    sortable: true,
                    dataIndex: 'local'
                }, {
                    header: i18n._("Client Address"),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'client_addr'
                }, {
                    header: i18n._('Reason'),
                    width: 200,
                    sortable: true,
                    dataIndex: 'reason',
                    renderer: Ext.bind(function(value) {
                        switch(value) {
                            case "U": return i18n._("invalid username");
                            case "P": return i18n._("invalid password");
                            default: return "";
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
                    header: i18n._("Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: i18n._("Load (1-minute)"),
                    width: 120,
                    sortable: true,
                    dataIndex: 'load_1',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("Load (5-minute)"),
                    width: 120,
                    sortable: true,
                    dataIndex: 'load_5',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("Load (15-minute)"),
                    width: 120,
                    sortable: true,
                    dataIndex: 'load_15',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("CPU User Utilization"),
                    width: 120,
                    sortable: true,
                    dataIndex: 'cpu_user',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("CPU System Utilization"),
                    width: 120,
                    sortable: true,
                    dataIndex: 'cpu_system',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("Memory Total"),
                    width: 120,
                    sortable: true,
                    dataIndex: 'mem_total',
                    filter: {
                        type: 'numeric'
                    },
                    renderer: function(value) {
                        var meg = value/1024/1024;
                        return (Math.round( meg*10 )/10).toString() + " MB";
                    }

                }, {
                    header: i18n._("Memory Free"),
                    width: 120,
                    sortable: true,
                    dataIndex: 'mem_free',
                    filter: {
                        type: 'numeric'
                    },
                    renderer: function(value) {
                        var meg = value/1024/1024;
                        return (Math.round( meg*10 )/10).toString() + " MB";
                    }
                }, {
                    header: i18n._("Disk Total"),
                    width: 120,
                    sortable: true,
                    dataIndex: 'disk_total',
                    filter: {
                        type: 'numeric'
                    },
                    renderer: function(value) {
                        var gig = value/1024/1024/1024;
                        return (Math.round( gig*10 )/10).toString() + " GB";
                    }
                }, {
                    header: i18n._("Disk Free"),
                    width: 120,
                    sortable: true,
                    dataIndex: 'disk_free',
                    filter: {
                        type: 'numeric'
                    },
                    renderer: function(value) {
                        var gig = value/1024/1024/1024;
                        return (Math.round( gig*10 )/10).toString() + " GB";
                    }
                }, {
                    header: i18n._("Swap Total"),
                    width: 120,
                    sortable: true,
                    dataIndex: 'swap_total',
                    filter: {
                        type: 'numeric'
                    },
                    renderer: function(value) {
                        var meg = value/1024/1024;
                        return (Math.round( meg*10 )/10).toString() + " MB";
                    }
                }, {
                    header: i18n._("Swap Free"),
                    width: 120,
                    sortable: true,
                    dataIndex: 'swap_free',
                    filter: {
                        type: 'numeric'
                    },
                    renderer: function(value) {
                        var meg = value/1024/1024;
                        return (Math.round( meg*10 )/10).toString() + " MB";
                    }
                }, {
                    header: i18n._("Active Hosts"),
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
                }],
                columns: [{
                    header: i18n._("Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: i18n._("Address"),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'address'
                }, {
                    header: i18n._("Key"),
                    width: 150,
                    sortable: true,
                    dataIndex: 'key'
                }, {
                    header: i18n._("Value"),
                    width: 150,
                    flex: 1,
                    sortable: true,
                    dataIndex: 'value'
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
                }],
                columns: [{
                    header: i18n._("Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: i18n._("MAC Address"),
                    width: Ung.TableConfig.macFieldWidth,
                    sortable: true,
                    dataIndex: 'mac_address'
                }, {
                    header: i18n._("Key"),
                    width: 150,
                    sortable: true,
                    dataIndex: 'key'
                }, {
                    header: i18n._("Value"),
                    width: 150,
                    flex: 1,
                    sortable: true,
                    dataIndex: 'value'
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
                        return value ?  i18n._("success"): i18n._("failed");
                    }, this)
                }, {
                    name: 'description',
                    type: 'string'
                }, {
                    name: 'destination',
                    type: 'string'
                }],
                columns: [{
                    header: i18n._("Event Id"),
                    width: 100,
                    sortable: true,
                    dataIndex: "event_id"
                },{
                    header: i18n._("Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: i18n._("Result"),
                    width: 120,
                    sortable: true,
                    dataIndex: 'success'
                }, {
                    header: i18n._("Destination"),
                    width: 100,
                    sortable: true,
                    dataIndex: 'destination'
                }, {
                    header: i18n._("Details"),
                    flex:1,
                    width: 200,
                    sortable: true,
                    dataIndex: 'description'
                }]
            },
            wan_failover_test_events: {
                fields: [{
                    name: "event_id"
                },{
                    name: "time_stamp",
                    sortType: 'asTimestamp'
                },{
                    name: "interface_id"
                },{
                    name: "name"
                },{
                    name: "success"
                },{
                    name: "description"
                }],
                columns: [{
                    header: i18n._("Event Id"),
                    width: 100,
                    sortable: true,
                    dataIndex: "event_id"
                },{
                    header: i18n._("Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                },{
                    header: i18n._("Interface Name"),
                    width: 120,
                    sortable: true,
                    dataIndex: "name"
                },{
                    header: i18n._("Interface Id"),
                    width: 120,
                    sortable: true,
                    dataIndex: "interface_id"
                },{
                    header: i18n._("Success"),
                    width: 120,
                    sortable: true,
                    dataIndex: "success",
                    filter: {
                        type: 'boolean',
                        yesText: i18n._('true'),
                        noText: i18n._('false')
                    }
                },{
                    header: i18n._("Test Description"),
                    width: 120,
                    sortable: true,
                    dataIndex: "description",
                    flex:1
                }]
            },
            wan_failover_action_events: {
                fields: [{
                    name: "event_id"
                },{
                    name: "time_stamp",
                    sortType: 'asTimestamp'
                },{
                    name: "interface_id"
                },{
                    name: "name"
                },{
                    name: "os_name"
                },{
                    name: "action"
                }],
                columns: [{
                    header: i18n._("Event Id"),
                    width: 100,
                    sortable: true,
                    dataIndex: "event_id"
                },{
                    header: i18n._("Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                },{
                    header: i18n._("Interface Name"),
                    width: 120,
                    sortable: true,
                    dataIndex: "name"
                },{
                    header: i18n._("Interface Id"),
                    width: 120,
                    sortable: true,
                    dataIndex: "interface_id"
                },{
                    header: i18n._("Interface OS"),
                    width: 120,
                    sortable: true,
                    dataIndex: "os_name"
                },{
                    header: i18n._("Action"),
                    width: 120,
                    sortable: true,
                    dataIndex: "action"
                }]
            },
            ipsec_user_events: {
                fields: [{
                    name: "time_stamp",
                    sortType: 'asTimestamp'
                },{
                    name: "event_id"
                },{
                    name: "client_username"
                },{
                    name: "client_protocol"
                },{
                    name: "connect_stamp",
                    sortType: 'asTimestamp'
                },{
                    name: "goodbye_stamp",
                    sortType: 'asTimestamp'
                },{
                    name: "elapsed_time"
                },{
                    name: "client_address",
                    sortType: 'asIp'
                },{
                    name: "net_interface"
                },{
                    name: "net_process"
                },{
                    name: "rx_bytes"
                },{
                    name: "tx_bytes"
                }],
                columns: [{
                    header: i18n._("Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                },{
                    header: i18n._("Event Id"),
                    width: 100,
                    sortable: true,
                    dataIndex: "event_id"
                },{
                    header: i18n._("Address"),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: "client_address"
                },{
                    header: i18n._("Username"),
                    width: Ung.TableConfig.usernameFieldWidth,
                    sortable: true,
                    dataIndex: "client_username"
                },{
                    header: i18n._("Protocol"),
                    width: 120,
                    sortable: true,
                    dataIndex: "client_protocol"
                },{
                    header: i18n._("Login Time"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: "connect_stamp",
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                },{
                    header: i18n._("Logout Time"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: "goodbye_stamp",
                    renderer: function(value) {
                        return (value ==="" ? "" : i18n.timestampFormat(value));
                    }
                },{
                    header: i18n._("Elapsed"),
                    width: 120,
                    sortable: true,
                    dataIndex: "elapsed_time"
                },{
                    header: i18n._("Interface"),
                    width: 80,
                    sortable: true,
                    dataIndex: "net_interface"
                },{
                    header: i18n._("RX Bytes"),
                    width: 120,
                    sortable: true,
                    dataIndex: "rx_bytes",
                    renderer: function(value) {
                        if ((value === undefined) || (value === null) || (value === "")) return("");
                        var kb = value/1024;
                        return (Math.round( kb*10 )/10).toString() + " KB";
                    }
                },{
                    header: i18n._("TX Bytes"),
                    width: 120,
                    sortable: true,
                    dataIndex: "tx_bytes",
                    renderer: function(value) {
                        if ((value === undefined) || (value === null) || (value === "")) return("");
                        var kb = value/1024;
                        return (Math.round( kb*10 )/10).toString() + " KB";
                    }
                },{
                    header: i18n._("Process"),
                    width: 120,
                    sortable: true,
                    dataIndex: "net_process"
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
                    header: i18n._("Event Id"),
                    width: 100,
                    sortable: true,
                    dataIndex: "event_id"
                }, {
                    header: i18n._("Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: Ext.bind(function(value) {
                        return i18n.timestampFormat(value);
                    }, this )
                }, {
                    header: i18n._("Tunnel Name"),
                    width: 120,
                    sortable: true,
                    dataIndex: 'tunnel_name'
                }, {
                    header: i18n._("In Bytes"),
                    width: 120,
                    sortable: true,
                    dataIndex: "in_bytes",
                    renderer: function(value) {
                        var kb = value/1024;
                        return (Math.round( kb*10 )/10).toString() + " KB";
                    }
                }, {
                    header: i18n._("Out Bytes"),
                    width: 120,
                    sortable: true,
                    dataIndex: "out_bytes",
                    renderer: function(value) {
                        var kb = value/1024;
                        return (Math.round( kb*10 )/10).toString() + " KB";
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
                    header: i18n._("Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: Ext.bind(function(value) {
                        return i18n.timestampFormat(value);
                    }, this )
                },{
                    header: i18n._("Interface Id"),
                    width: 120,
                    sortable: true,
                    dataIndex: "interface_id"
                }, {
                    header: i18n._("RX Rate"),
                    width: 120,
                    sortable: true,
                    dataIndex: 'rx_rate',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("TX Rate"),
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
                        return value == null ? "": value;
                    }
                }, {
                    name: 'hostname'
                }],
                // the list of columns
                columns: [{
                    header: i18n._("Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: i18n._('Policy Id'),
                    width: 60,
                    sortable: true,
                    dataIndex: 'policy_id',
                    renderer: Ung.panel.Reports.getColumnRenderer('policy_id')
                }, {
                    header: i18n._('Event Id'),
                    width: 60,
                    sortable: true,
                    dataIndex: 'event_id'
                }, {
                    header: i18n._('Vendor Name'),
                    width: 120,
                    sortable: true,
                    dataIndex: 'vendor_name'
                }, {
                    header: i18n._("Sender"),
                    width: 120,
                    sortable: true,
                    dataIndex: 'ipaddr'
                }, {
                    header: i18n._("DNSBL Server"),
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
                    header: i18n._("Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: i18n._('Event Id'),
                    width: 60,
                    sortable: true,
                    dataIndex: 'event_id'
                }, {
                    header: i18n._("Hit Count"),
                    width: 120,
                    sortable: false,
                    dataIndex: 'hits',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("Miss Count"),
                    width: 120,
                    sortable: false,
                    dataIndex: 'misses',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("Bypass Count"),
                    width: 120,
                    sortable: false,
                    dataIndex: 'bypasses',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("System Count"),
                    width: 120,
                    sortable: false,
                    dataIndex: 'systems',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("Hit Bytes"),
                    width: 120,
                    sortable: true,
                    dataIndex: 'hit_bytes',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("Miss Bytes"),
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
                    name: "time_stamp",
                    sortType: 'asTimestamp'
                }, {
                    name: 'policy_id'
                }, {
                    name: 'event_id'
                },{
                    name: "client_addr",
                    sortType: 'asIp'
                },{
                    name: "login_name"
                },{
                    name: "auth_type"
                },{
                    name: "event_info"
                }],
                columns: [{
                    header: i18n._("Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: "time_stamp",
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: i18n._('Policy Id'),
                    width: 60,
                    sortable: true,
                    dataIndex: 'policy_id',
                    renderer: Ung.panel.Reports.getColumnRenderer('policy_id')
                }, {
                    header: i18n._('Event Id'),
                    width: 60,
                    sortable: true,
                    dataIndex: 'event_id'
                }, {
                    header: i18n._("Client"),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: "client_addr"
                }, {
                    header: i18n._("Username"),
                    width: Ung.TableConfig.usernameFieldWidth,
                    sortable: true,
                    dataIndex: "login_name",
                    flex:1
                }, {
                    header: i18n._("Action"),
                    width: 165,
                    sortable: true,
                    dataIndex: "event_info",
                    renderer: Ext.bind(function( value ) {
                        switch ( value ) {
                            case "LOGIN":
                                return i18n._( "Login Success" );
                            case "FAILED":
                                return i18n._( "Login Failure" );
                            case "TIMEOUT":
                                return i18n._( "Session Timeout" );
                            case "INACTIVE":
                                return i18n._( "Idle Timeout" );
                            case "USER_LOGOUT":
                                return i18n._( "User Logout" );
                            case "ADMIN_LOGOUT":
                                return i18n._( "Admin Logout" );
                        }
                        return "";
                    }, this )
                }, {
                    header: i18n._("Authentication"),
                    width: 165,
                    sortable: true,
                    dataIndex: "auth_type",
                    renderer: Ext.bind(function( value ) {
                        switch ( value ) {
                            case "NONE":
                                return i18n._( "None" );
                            case "LOCAL_DIRECTORY":
                                return i18n._( "Local Directory" );
                            case "ACTIVE_DIRECTORY":
                                return i18n._( "Active Directory" );
                            case "RADIUS":
                                return i18n._( "RADIUS" );
                            case "CUSTOM":
                                return i18n._( "Custom" );
                        }
                        return "";
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
                    header: i18n._("Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: i18n._("Sid"),
                    width: 70,
                    sortable: true,
                    dataIndex: 'sig_id',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("Gid"),
                    width: 70,
                    sortable: true,
                    dataIndex: 'gen_id',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("Cid"),
                    width: 70,
                    sortable: true,
                    dataIndex: 'class_id',
                    filter: {
                        type: 'numeric'
                    }
                }, {
                    header: i18n._("Source Address"),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'source_addr'
                }, {
                    header: i18n._("Source port"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'source_port',
                    filter: {
                        type: 'numeric'
                    },
                    renderer: function(value, metaData, record, row, col, store, gridView){
                        if( record.get("protocol") == 1 ){
                            return "";
                        }
                        return value;
                    }
                }, {
                    header: i18n._("Destination Address"),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'dest_addr'
                }, {
                    header: i18n._("Destination port"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'dest_port',
                    filter: {
                        type: 'numeric'
                    },
                    renderer: function(value, metaData, record, row, col, store, gridView){
                        if( record.get("protocol") == 1 ){
                            return "";
                        }
                        return value;
                    }
                }, {
                    header: i18n._("Protocol"),
                    width: 70,
                    sortable: true,
                    dataIndex: 'protocol',
                    renderer: function(value, metaData, record, row, col, store, gridView){
                        switch(value){
                            case 1:
                                return i18n._("ICMP");
                            case 17:
                                return i18n._("UDP");
                            case 6:
                                return i18n._("TCP");
                        }
                        return value;
                    }
                }, {
                    header: i18n._("Blocked"),
                    width: Ung.TableConfig.booleanFieldWidth,
                    sortable: true,
                    dataIndex: 'blocked',
                    filter: {
                        type: 'boolean',
                        yesText: i18n._('true'),
                        noText: i18n._('false')
                    }
                }, {
                    header: i18n._("Category"),
                    width: 200,
                    sortable: true,
                    dataIndex: 'category'
                }, {
                    header: i18n._("Classtype"),
                    width: 200,
                    sortable: true,
                    dataIndex: 'classtype'
                }, {
                    header: i18n._("Msg"),
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
                    header: i18n._("Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: Ext.bind(function(value) {
                        return i18n.timestampFormat(value);
                    }, this )
                }, {
                    header: i18n._("Type"),
                    width: 120,
                    sortable: true,
                    dataIndex: 'type'
                }, {
                    header: i18n._("Client Name"),
                    width: Ung.TableConfig.usernameFieldWidth,
                    sortable: true,
                    dataIndex: 'client_name'
                }, {
                    header: i18n._("Client Address"),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'remote_address'
                }, {
                    header: i18n._("Pool Address"),
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
                    header: i18n._("Event Id"),
                    width: 100,
                    sortable: true,
                    dataIndex: "event_id"
                }, {
                    header: i18n._("Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: Ext.bind(function(value) {
                        return i18n.timestampFormat(value);
                    }, this )
                }, {
                    header: i18n._("Start Time"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'start_time',
                    renderer: Ext.bind(function(value) {
                        return i18n.timestampFormat(value);
                    }, this )
                }, {
                    header: i18n._("End Time"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'end_time',
                    renderer: Ext.bind(function(value) {
                        return i18n.timestampFormat(value);
                    }, this )
                }, {
                    header: i18n._("Client Name"),
                    width: 120,
                    sortable: true,
                    dataIndex: 'client_name'
                }, {
                    header: i18n._("Client Address"),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'remote_address'
                }, {
                    header: i18n._("Client Port"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: "remote_port"
                }, {
                    header: i18n._("Pool Address"),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'pool_address'
                }, {
                    header: i18n._("RX Bytes"),
                    width: 120,
                    sortable: true,
                    dataIndex: "rx_bytes",
                    renderer: function(value) {
                        var kb = value/1024;
                        return (Math.round( kb*10 )/10).toString() + " KB";
                    }
                }, {
                    header: i18n._("TX Bytes"),
                    width: 120,
                    sortable: true,
                    dataIndex: "tx_bytes",
                    renderer: function(value) {
                        var kb = value/1024;
                        return (Math.round( kb*10 )/10).toString() + " KB";
                    }
                }]
            },
            alerts: {
                fields: [{
                    name: "time_stamp",
                    sortType: 'asTimestamp'
                },{
                    name: "description"
                },{
                    name: "summary_text"
                },{
                    name: "json"
                }],
                // the list of columns
                columns: [{
                    header: i18n._("Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                },{
                    header: i18n._("Description"),
                    width: 200,
                    sortable: true,
                    dataIndex: "description"
                },{
                    header: i18n._("Summary Text"),
                    flex: 1,
                    width: 500,
                    sortable: true,
                    dataIndex: "summary_text"
                },{
                    header: i18n._("JSON"),
                    width: 500,
                    sortable: true,
                    dataIndex: "json"
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
                    header: i18n._("Event Id"),
                    width: 100,
                    sortable: true,
                    dataIndex: "event_id"
                }, {
                    header: i18n._("Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: i18n._('Policy Id'),
                    width: 60,
                    sortable: true,
                    dataIndex: 'policy_id',
                    renderer: Ung.panel.Reports.getColumnRenderer('policy_id')
                }, {
                    header: i18n._("Session Id"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'session_id'
                }, {
                    header: i18n._("Request Id"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'request_id'
                }, {
                    header: i18n._("Method"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'method'
                }, {
                    header: i18n._("Client Interface") ,
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'client_intf'
                }, {
                    header: i18n._("Server Interface") ,
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'server_intf'
                }, {
                    header: i18n._("Client"),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'c_client_addr'
                }, {
                    header: i18n._("New Client"),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 's_client_addr'
                }, {
                    header: i18n._("Original Server") ,
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'c_server_addr'
                }, {
                    header: i18n._("Server") ,
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 's_server_addr'
                }, {
                    header: i18n._("Hostname"),
                    width: Ung.TableConfig.hostnameFieldWidth,
                    sortable: true,
                    dataIndex: 'hostname'
                }, {
                    header: i18n._("Username"),
                    width: Ung.TableConfig.usernameFieldWidth,
                    sortable: true,
                    dataIndex: 'username'
                }, {
                    header: i18n._("File Name"),
                    flex:1,
                    width: Ung.TableConfig.uriFieldWidth,
                    dataIndex: 'uri'
                }, {
                    header: 'Virus Blocker Lite ' + i18n._('Name'),
                    width: 140,
                    sortable: true,
                    dataIndex: 'virus_blocker_lite_name'
                }, {
                    header: 'Virus Blocker Lite ' + i18n._('clean'),
                    width: 140,
                    sortable: true,
                    dataIndex: 'virus_blocker_lite_clean'
                }, {
                    header: 'Virus Blocker ' + i18n._('Name'),
                    width: 140,
                    sortable: true,
                    dataIndex: 'virus_blocker_name'
                }, {
                    header: 'Virus Blocker ' + i18n._('Clean'),
                    width: 140,
                    sortable: true,
                    dataIndex: 'virus_blocker_clean'
                }, {
                    header: i18n._("Server"),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'c_server_addr'
                }]
            },
            penaltybox: {
                fields: [{
                    name: 'time_stamp',
                    sortType: 'asTimestamp'
                }, {
                    name: 'address',
                    sortType: 'asIp'
                }, {
                    name: 'reason'
                }, {
                    name: 'start_time',
                    sortType: 'asTimestamp'
                }, {
                    name: 'end_time',
                    sortType: 'asTimestamp'
                }],
                columns: [{
                    header: i18n._("Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: i18n._("Address"),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'address'
                }, {
                    header: i18n._("Reason"),
                    width: 120,
                    sortable: true,
                    flex: 1,
                    dataIndex: 'reason'
                }, {
                    header: i18n._("Start Time"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'start_time',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: i18n._("End Time"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'end_time',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
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
                    header: i18n._("Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: i18n._("Address"),
                    width: Ung.TableConfig.ipFieldWidth,
                    sortable: true,
                    dataIndex: 'address'
                }, {
                    header: i18n._("Action"),
                    width: 120,
                    sortable: true,
                    dataIndex: 'action',
                    renderer: function(value) {
                        if ( value == 1 )
                            return "1 (" + i18n._("Given") + ")";
                        if ( value == 2 )
                            return "2 (" + i18n._("Exceeded") + ")";
                        return value;
                    }
                }, {
                    header: i18n._("Size"),
                    width: Ung.TableConfig.portFieldWidth,
                    sortable: true,
                    dataIndex: 'size'
                }, {
                    header: i18n._("Reason"),
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
                    header: i18n._("Timestamp"),
                    width: Ung.TableConfig.timestampFieldWidth,
                    sortable: true,
                    dataIndex: 'time_stamp',
                    renderer: function(value) {
                        return i18n.timestampFormat(value);
                    }
                }, {
                    header: i18n._("Username"),
                    width: Ung.TableConfig.usernameFieldWidth,
                    sortable: true,
                    dataIndex: 'username'
                }, {
                    header: i18n._("Hostname"),
                    width: Ung.TableConfig.hostnameFieldWidth,
                    sortable: true,
                    dataIndex: 'hostname'
                }, {
                    header: i18n._("Settings File"),
                    flex:1,
                    width: Ung.TableConfig.uriFieldWidth,
                    dataIndex: 'settings_file',
                    renderer: function( value ){
                        value = value.replace( /^.*\/settings\//, "" );
                        return value;
                    }
                }]
            }
        };
        if(Ung.Main.webuiMode) {
            this.tableConfig.settings_changes.columns.push({
                header: i18n._("Differences"),
                xtype: 'actioncolumn',
                align: 'center',
                dataIndex: 'settings_file',
                width: 100,
                items: [{
                    icon: '/skins/default/images/admin/icons/icon_detail.png',
                    tooltip: i18n._("Show difference between previous version"),
                    handler: function(view, rowIndex, colIndex, item, e, record) {
                        if( !this.diffWindow ) {
                            var columnRenderer = function(value, meta, record) {
                                var action = record.get("action");
                                if( action == 3){
                                    meta.style = "background-color:#ffff99";
                                }else if(action == 2) {
                                    meta.style = "background-color:#ffdfd9";
                                }else if(action == 1) {
                                    meta.style = "background-color:#d9f5cb";
                                }
                                return value;
                            };
                            this.diffWindow = Ext.create('Ung.Window',{
                                name: 'diffWindow',
                                title: i18n._('Settings Difference'),
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
                                            var diffWindow = this.up("window[name=diffWindow]");
                                            if (diffWindow ==null || !diffWindow.isVisible()) {
                                                return;
                                            }
                                            if(exception) {
                                                this.getView().setLoading(false);
                                                handler(result, exception);
                                                return;
                                            }
                                            var diffData = [];
                                            var diffLines = result.split("\n");
                                            var action;
                                            for( var i = 0; i < diffLines.length; i++) {
                                                previousAction = diffLines[i].substr(0,1);
                                                previousLine = diffLines[i].substr(1,510);
                                                currentAction = diffLines[i].substr(511,1);
                                                currentLine = diffLines[i].substr(512);

                                                if( previousAction != "<" && previousAction != ">") {
                                                    previousLine = previousAction + previousLine;
                                                    previousAction = -1;
                                                }
                                                if( currentAction != "<" && currentAction != ">" && currentAction != "|"){
                                                    currentLine = currentAction + currentLine;
                                                    currentAction = -1;
                                                }

                                                if( currentAction == "|" ) {
                                                    action = 3;
                                                } else if(currentAction == "<") {
                                                    action = 2;
                                                } else if(currentAction == ">") {
                                                    action = 1;
                                                } else {
                                                    action = 0;
                                                }

                                                diffData.push({
                                                    line: (i + 1),
                                                    previous: previousLine.replace(/\s+$/,"").replace(/\s/g, "&nbsp;"),
                                                    current: currentLine.replace(/\s+$/,"").replace(/\s/g, "&nbsp;"),
                                                    action: action
                                                });
                                            }
                                            handler({javaClass:"java.util.LinkedList", list: diffData});
                                        },this), this.fileName);
                                    },
                                    fields: [{
                                        name: "line"
                                    }, {
                                        name: "previous"
                                    }, {
                                        name: "current"
                                    }, {
                                        name: "action"
                                    }],
                                    columnsDefaultSortable: false,
                                    columns:[{
                                        text: i18n._("Line"),
                                        dataIndex: "line",
                                        renderer: columnRenderer
                                    },{
                                        text: i18n._("Previous"),
                                        flex: 1,
                                        dataIndex: "previous",
                                        renderer: columnRenderer
                                    },{
                                        text: i18n._("Current"),
                                        flex: 1,
                                        dataIndex: "current",
                                        renderer: columnRenderer
                                    }]
                                })],
                                buttons: [{
                                    text: i18n._("Close"),
                                    handler: Ext.bind(function() {
                                        this.diffWindow.hide();
                                    }, this)
                                }],
                                update: function(fileName) {
                                    var grid = this.down("grid[name=gridDiffs]");
                                    grid.fileName = fileName;
                                    grid.reload();

                                },
                                doSize : function() {
                                    this.maximize();
                                }
                            });
                            this.on("beforedestroy", Ext.bind(function() {
                                if(this.diffWindow) {
                                    Ext.destroy(this.diffWindow);
                                    this.diffWindow = null;
                                }
                            }, this));
                        }
                        this.diffWindow.show();
                        this.diffWindow.update(record.get("settings_file"));
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
