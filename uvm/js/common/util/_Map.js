/**
 * contains all the necessary mappings
 * _ (underscore) naming in front of the files required when minified to be concatenated last
 *
 */
Ext.define('Ung.util.Map', {
    singleton: true,
    alternateClassName: 'Map',

    policies: {
        1: 'Default'
    },

    init: function () {
        this.buildInterfacesMap();
        this.buildPoliciesMap();
    },

    buildInterfacesMap: function () {
        var interfaces, map = {};
        if (rpc.reportsManager) {
            interfaces = Rpc.directData('rpc.reportsManager.getInterfacesInfo').list;
            Ext.Array.each(interfaces, function (intf) {
                map[intf.interfaceId] = intf.name;
            });
        }
        this.interfaces = map;
    },

    buildPoliciesMap: function () {
        var policies,
            map = { 1: 'Default' };

        if (rpc.reportsManager) {
            policies = Rpc.directData('rpc.reportsManager.getPoliciesInfo');
            if (policies && policies['list']) {
                Ext.Array.each(policies['list'], function (policy) {
                    map[policy.policyId] = policy.name;
                });
            }
        }
        this.policies = map;
    },

    /**
     * all possible fields matching all possible db tables fields
     * having a grid column definition (col) and a store field definition (fld)
     * field_id: {
     *      col: {
     *          text: 'Column header text'.t(),
     *          filter: Rndr.filters.numeric, // the filter type e.g. 'numeric'/ 'boolean', defaults to 'string'
     *          width: 100, // the column width
     *          renderer: Rndr.boolean // the renderer method if needed, see Rndr class,
     *          dataIndex: // matching field_id and set in grid setup
     *          hidden: // true or false depending if found in report defaultColumns (not set here)
     *      },
     *      fld: {
     *          type: 'string', // model field type
     *          convert: Converter.icmp // the converter method if needed, see Converter class,
     *          sortType: 'asIp', // if requires a specific sort type
     *          name: // matching field_id and set in grid setup
     *      }
     * }
     *
     * This definitions are used when setting up grid for a specific report
     * applying a store model matching report entry db table reference
     * and setting corresponding grid columns based on that
     */
    fields: {
        action: {
            col: { text: 'Action'.t(), width: 100 },
            fld: { type: 'string' }
        },
        action_quotas: {
            col: { text: 'Action'.t(), dataIndex: 'action', width: 100 },
            fld: { name: 'action', type: 'string', convert: Converter.quotaAction }
        },
        active_hosts: {
            col: { text: 'Active Hosts'.t(), filter: Rndr.filters.numeric, width: 100 },
            fld: { type: 'integer' }
        },
        addr: {
            col: { text: 'Receiver'.t(), width: 120 },
            fld: { type: 'string' }
        },
        addr_kind: {
            col: { text: 'Address Kind'.t(), width: 120 },
            fld: { type: 'string' }
        },
        addr_name: {
            col: { text: 'Address Name'.t(), width: 120 },
            fld: { type: 'string' }
        },
        address: {
            col: { text: 'Address'.t(), width: 120 },
            fld: { type: 'string', sortType: 'asIp' }
        },
        auth_type: {
            col: { text: 'Auth Type'.t(), width: 120 },
            fld: { type: 'string', convert: Converter.authType }
        },
        blocked: {
            col: { text: 'Blocked'.t(), filter: Rndr.filters.boolean, width: Rndr.colW.boolean,  renderer: Rndr.boolean },
            fld: { type: 'boolean' }
        },
        bypassed: {
            col: { text: 'Bypassed'.t(), filter: Rndr.filters.boolean, width: Rndr.colW.boolean, renderer: Rndr.boolean },
            fld: { type: 'boolean' }
        },
        bypasses: {
            col: { text: 'Bypass Count'.t(), filter: Rndr.filters.numeric, width: 80 },
            fld: { type: 'integer' }
        },
        c2p_bytes: {
            col: { text: 'From-Client Bytes'.t(), filter: Rndr.filters.numeric, width: 80, align: 'right', renderer: Renderer.datasize },
            fld: { type: 'number' }
        },
        c2s_bytes: {
            col: { text: 'From-Client Bytes'.t(), filter: Rndr.filters.numeric, width: 80, align: 'right', renderer: Renderer.datasize },
            fld: { type: 'number' }
        },
        c2s_content_length: {
            col: { text: 'Upload Content Length'.t(), filter: Rndr.filters.numeric, width: 120 },
            fld: { type: 'number' }
        },
        c_client_addr: {
            col: { text: 'Client'.t(), filter: Rndr.filters.string, width: 120 },
            fld: { type: 'string', sortType: 'asIp' }
        },
        c_client_port: {
            col: { text: 'Client Port'.t(), filter: Rndr.filters.numeric, width: 120 },
            fld: { type: 'integer' }
        },
        c_server_addr: {
            col: { text: 'Original Server'.t(), filter: Rndr.filters.string, width: 120 },
            fld: { type: 'string', sortType: 'asIp' }
        },
        c_server_port: {
            col: { text: 'Original Server Port'.t(), filter: Rndr.filters.numeric, width: 120 },
            fld: { type: 'integer' }
        },
        category: {
            col: { text: 'Category'.t(), width: 100 },
            fld: { type: 'string' }
        },
        classtype: {
            col: { text: 'Classtype'.t(), width: 100 },
            fld: { type: 'string' }
        },
        class_id: {
            col: { text: 'Cid'.t(), filter: Rndr.filters.numeric, width: 100 },
            fld: { type: 'integer' }
        },
        client_addr: {
            col: { text: 'Client Address'.t(), width: 120 },
            fld: { type: 'string', sortType: 'asIp' }
        },
        client_address: {
            col: { text: 'Client Address'.t(), width: 120 },
            fld: { type: 'string', sortType: 'asIp' }
        },
        client_country: {
            col: { text: 'Client Country'.t(), width: 120 }, // converter
            fld: { type: 'string', convert: Converter.country }
        },
        client_intf: {
            col: { text: 'Client Interface'.t(), width: 100 }, // converter
            fld: { type: 'integer', convert: Converter.interface }
        },
        client_latitude: {
            col: { text: 'Client Latitude'.t(), filter: Rndr.filters.numeric, width: 120 },
            fld: { type: 'number' }
        },
        client_longitude: {
            col: { text: 'Client Longitude'.t(), filter: Rndr.filters.numeric, width: 120 },
            fld: { type: 'number' }
        },
        client_name: {
            col: { text: 'Client Name'.t(), width: 120 },
            fld: { type: 'string' }
        },
        client_protocol: {
            col: { text: 'Client Protocol'.t(), width: 120 },
            fld: { type: 'string' }
        },
        client_username: {
            col: { text: 'Client Username'.t(), width: 120 },
            fld: { type: 'string' }
        },
        connect_stamp: {
            col: { text: 'Login Time'.t(), filter: Rndr.filters.date, width: 120 },
            fld: { type: 'integer', convert: Converter.timestamp }
        },
        cpu_system: {
            col: { text: 'CPU System Utilization'.t(), filter: Rndr.filters.numeric, width: 100 },
            fld: { type: 'number' }
        },
        cpu_user: {
            col: { text: 'CPU User Utilization'.t(), filter: Rndr.filters.numeric, width: 100 },
            fld: { type: 'number' }
        },
        description: {
            col: { text: 'Description'.t(), width: 200 }, // multiple column names!!!
            fld: { type: 'string' }
        },
        destination: {
            col: { text: 'Destination'.t(), width: 200 },
            fld: { type: 'string' }
        },
        dest_addr: {
            col: { text: 'Destination Address'.t(), width: 200 },
            fld: { type: 'string', sortType: 'asIp' }
        },
        dest_port: {
            col: { text: 'Destination Port'.t(), filter: Rndr.filters.numeric, width: 200 },
            fld: { type: 'integer' }
        },
        disk_free: {
            col: { text: 'Disk Free'.t(), filter: Rndr.filters.numeric, width: 100, align: 'right', renderer: Rndr.disk },
            fld: { type: 'number' }
        },
        disk_total: {
            col: { text: 'Disk Total'.t(), filter: Rndr.filters.numeric, width: 100, align: 'right', renderer: Rndr.disk },
            fld: { type: 'number' }
        },
        domain: {
            col: { text: 'Domain'.t(), width: 120 },
            fld: { type: 'string' }
        },
        elapsed_time: {
            col: { text: 'Elapsed'.t(), filter: Rndr.filters.numeric, width: 160 }, // converter
            fld: { type: 'integer', convert: Converter.timestamp }
        },
        end_time: {
            col: { text: 'End Time'.t(), filter: Rndr.filters.date, width: 160 }, // converter
            fld: { type: 'integer', convert: Converter.timestamp }
        },
        entitled: {
            col: { text: 'Entitled'.t(), filter: Rndr.filters.boolean, width: Rndr.colW.boolean, renderer: Rndr.boolean },
            fld: { type: 'boolean' }
        },
        entity: {
            col: { text: 'Address'.t(), width: 80 },
            fld: { type: 'string' }
        },
        event_id: {
            col: { text: 'Event Id'.t(), filter: Rndr.filters.numeric, width: 120 },
            fld: { type: 'integer' }
        },
        event_info: {
            col: { text: 'Event Info'.t(), width: 120 },
            fld: { type: 'string', convert: Converter.captivePortalEventInfo }
        },
        event_type: {
            col: { text: 'Type'.t(), width: 120 },
            fld: { type: 'string' }
        },
        filter_prefix: {
            col: { text: 'Filter Prefix'.t(), width: 80 },
            fld: { type: 'string' }
        },
        flagged: {
            col: { text: 'Flagged'.t(), filter: Rndr.filters.boolean, width: Rndr.colW.boolean, renderer: Rndr.boolean },
            fld: { type: 'boolean' }
        },
        gen_id: {
            col: { text: 'Gid'.t(), filter: Rndr.filters.numeric, width: 100 },
            fld: { type: 'integer' }
        },
        goodbye_stamp: {
            col: { text: 'Logout Time'.t(), filter: Rndr.filters.date, width: 100 },
            fld: { type: 'integer', convert: Converter.timestamp }
        },
        hits: {
            col: { text: 'Hit Count'.t(), filter: Rndr.filters.numeric, width: 250 },
            fld: { type: 'integer' }
        },
        hit_bytes: {
            col: { text: 'Hit Bytes'.t(), filter: Rndr.filters.numeric, width: 250 },
            fld: { type: 'number' }
        },
        host: {
            col: { text: 'Host'.t(), width: 250 },
            fld: { type: 'string' }
        },
        hostname: {
            col: { text: 'Hostname'.t(), width: 120 },
            fld: { type: 'string' }
        },
        icmp_type: {
            col: { text: 'ICMP Type'.t(), width: 150 },
            fld: { type: 'integer', convert: Converter.icmp }
        },
        in_bytes: {
            col: { text: 'In Bytes'.t(), filter: Rndr.filters.numeric, width: 200 },
            fld: { type: 'number' }
        },
        interface_id: {
            col: { text: 'Interface'.t(), width: 100 },
            fld: { type: 'integer', convert: Converter.interface }
        },
        ipaddr: {
            col: { text: 'Sender'.t(), width: 200 },
            fld: { type: 'string' }
        },
        json: {
            col: { text: 'JSON'.t(), flex: 1 },
            fld: { type: 'string' }
        },
        key: {
            col: { text: 'Key'.t(), width: 120 },
            fld: { type: 'string' }
        },
        load_1: {
            col: { text: 'Load (1-minute)'.t(), filter: Rndr.filters.numeric, width: 100, align: 'right' },
            fld: { type: 'number' }
        },
        load_5: {
            col: { text: 'Load (5-minute)'.t(), filter: Rndr.filters.numeric, width: 80, align: 'right' },
            fld: { type: 'number' }
        },
        load_15: {
            col: { text: 'Load (15-minute)'.t(), filter: Rndr.filters.numeric, width: 80, align: 'right' },
            fld: { type: 'number' }
        },
        local: {
            col: { text: 'Local'.t(), filter: Rndr.filters.boolean, width: 100, renderer: Rndr.localLogin },
            fld: { type: 'boolean' }
        },
        local_addr: {
            col: { text: 'Local Address'.t(), width: 120 },
            fld: { type: 'string', sortType: 'asIp' }
        },
        local_address: {
            col: { text: 'Local Address'.t(), width: 120 },
            fld: { type: 'string', sortType: 'asIp' }
        },
        login: {
            col: { text: 'Login'.t(), width: 100},
            fld: { type: 'string' }
        },
        login_name: {
            col: { text: 'Login'.t(), width: 100},
            fld: { type: 'string' }
        },
        mac_address: {
            col: { text: 'MAC Address'.t(), width: 120},
            fld: { type: 'string' }
        },
        mem_free: {
            col: { text: 'Memory Free'.t(), filter: Rndr.filters.numeric, width: 100, align: 'right', renderer: Rndr.memory },
            fld: { type: 'number' }
        },
        mem_total: {
            col: { text: 'Memory Total'.t(), filter: Rndr.filters.numeric, width: 100, align: 'right', renderer: Rndr.memory },
            fld: { type: 'number' }
        },
        method: {
            col: { text: 'Method'.t(), width: 120 }, // converter
            fld: { type: 'string', convert: Converter.httpMethod }
        },
        misses: {
            col: { text: 'Miss Count'.t(), filter: Rndr.filters.numeric, width: 100 },
            fld: { type: 'number' }
        },
        miss_bytes: {
            col: { text: 'Miss Bytes'.t(), filter: Rndr.filters.numeric, width: 120 },
            fld: { type: 'number' }
        },
        msg: {
            col: { text: 'Msg'.t(), width: 120 },
            fld: { type: 'string' }
        },
        msg_id: {
            col: { text: 'Message Id'.t(), width: 120 },
            fld: { type: 'string' }
        },
        name: {
            col: { text: 'Interface Name'.t(), width: 120 },
            fld: { type: 'string' }
        },
        net_interface: {
            col: { text: 'Interface'.t(), width: 120 },
            fld: { type: 'string' }
        },
        net_process: {
            col: { text: 'Process'.t(), width: 120 },
            fld: { type: 'string' }
        },
        old_value: {
            col: { text: 'Old Value'.t(), width: 120 },
            fld: { type: 'string' }
        },
        os_name: {
            col: { text: 'Interface OS'.t(), width: 120 },
            fld: { type: 'string' }
        },
        out_bytes: {
            col: { text: 'Out Bytes'.t(), filter: Rndr.filters.numeric, width: 100 },
            fld: { type: 'number' }
        },
        p2c_bytes: {
            col: { text: 'To-Client Bytes'.t(), filter: Rndr.filters.numeric, width: 80, align: 'right', renderer: Renderer.datasize },
            fld: { type: 'number' }
        },
        p2s_bytes: {
            col: { text: 'To-Server Bytes'.t(), filter: Rndr.filters.numeric, width: 80, align: 'right', renderer: Renderer.datasize },
            fld: { type: 'number' }
        },
        policy_id: {
            col: { text: 'Policy Id'.t(), width: 120},
            fld: { type: 'integer', convert: Converter.policy }
        },
        policy_rule_id: {
            col: { text: 'Policy Rule'.t(), width: 100},
            fld: { type: 'integer' }
        },
        pool_address: {
            col: { text: 'Pool Address'.t(), width: 100},
            fld: { type: 'string', sortType: 'asIp' }
        },
        protocol: {
            col: { text: 'Protocol'.t(), filter: Rndr.filters.numeric, width: 80 },
            fld: { type: 'integer', convert: Converter.protocol }
        },
        reason: {
            col: { text: 'Reason'.t(), width: 200 },
            fld: { type: 'string' }
        },
        reason_admin_logins: {
            col: { text: 'Reason'.t(), dataIndex: 'reason', width: 100 },
            fld: { name: 'reason', type: 'string', convert: Converter.loginFailureReason }
        },
        receiver: {
            col: { text: 'Receiver'.t(), width: 100 },
            fld: { type: 'string' }
        },
        referer: {
            col: { text: 'Referer'.t(), width: 120 },
            fld: { type: 'string' }
        },
        remote_addr: {
            col: { text: 'Remote Address'.t(), width: 120 },
            fld: { type: 'string', sortType: 'asIp' }
        },
        remote_address: {
            col: { text: 'Remote Address'.t(), width: 120 },
            fld: { type: 'string', sortType: 'asIp' }
        },
        remote_port: {
            col: { text: 'Remote Port'.t(), filter: Rndr.filters.numeric, width: 120 },
            fld: { type: 'integer' }
        },
        request_id: {
            col: { text: 'Request Id'.t(), filter: Rndr.filters.numeric, width: 120 },
            fld: { type: 'integer' }
        },
        rid: {
            col: { text: 'Rid'.t(), width: 120 },
            fld: { type: 'string' }
        },
        rule_id: {
            col: { text: 'Rule Id'.t(), width: 120 },
            fld: { type: 'string' }
        },
        rx_bytes: {
            col: { text: 'RX Bytes'.t(), filter: Rndr.filters.numeric, width: 120 },
            fld: { type: 'number' }
        },
        rx_rate: {
            col: { text: 'RX Rate'.t(), filter: Rndr.filters.numeric, width: 120 },
            fld: { type: 'number' }
        },
        s2c_bytes: {
            col: { text: 'From-Server Bytes'.t(), filter: Rndr.filters.numeric, width: 80, align: 'right', renderer: Renderer.datasize },
            fld: { type: 'number' }
        },
        s2c_content_filename: {
            col: { text: 'Content Filename'.t(), width: 160 },
            fld: { type: 'string' }
        },
        s2c_content_length: {
            col: { text: 'Download Content Length'.t(), filter: Rndr.filters.numeric, width: 120 },
            fld: { type: 'number' }
        },
        s2c_content_type: {
            col: { text: 'Content Type'.t(), width: 120 },
            fld: { type: 'string' }
        },
        s2p_bytes: {
            col: { text: 'From-Server Bytes'.t(), filter: Rndr.filters.numeric, width: 80, align: 'right', renderer: Renderer.datasize },
            fld: { type: 'number' }
        },
        s_client_addr: {
            col: { text: 'New Client'.t(), filter: Rndr.filters.string, width: 120 },
            fld: { type: 'string', sortType: 'asIp' }
        },
        s_client_port: {
            col: { text: 'New Client Port'.t(), filter: Rndr.filters.numeric, width: 120 },
            fld: { type: 'integer' }
        },
        s_server_addr: {
            col: { text: 'Server'.t(), filter: Rndr.filters.string, width: 120 },
            fld: { type: 'string', sortType: 'asIp' }
        },
        s_server_port: {
            col: { text: 'Server Port'.t(), filter: Rndr.filters.numeric, width: 120 },
            fld: { type: 'integer' }
        },
        sender: {
            col: { text: 'Sender', width: 100 },
            fld: { type: 'string' }
        },
        server_address: {
            col: { text: 'Server Address'.t(), width: 120 },
            fld: { type: 'string', sortType: 'asIp' }
        },
        server_country: {
            col: { text: 'Server Country'.t(), width: 120 }, // converter
            fld: { type: 'string', convert: Converter.country }
        },
        server_intf: {
            col: { text: 'Server Interface'.t(), width: 100 }, // converter
            fld: { type: 'integer', convert: Converter.interface }
        },
        server_latitude: {
            col: { text: 'Server Latitude'.t(), filter: Rndr.filters.numeric, width: 120 },
            fld: { type: 'number' }
        },
        server_longitude: {
            col: { text: 'Server Longitude'.t(), filter: Rndr.filters.numeric, width: 120},
            fld: { type: 'number' }
        },
        session_id: {
            col: { text: 'Session Id'.t(), filter: Rndr.filters.numeric, width: 120 },
            fld: { type: 'integer' }
        },
        settings_file: {
            col: { text: 'Settings File'.t(), width: 200, renderer: Rndr.settingsFile },
            fld: { type: 'string' }
        },
        sig_id: {
            col: { text: 'Sid', filter: Rndr.filters.numeric, width: 100 },
            fld: { type: 'integer' }
        },
        size: {
            col: { text: 'Size', filter: Rndr.filters.numeric, width: 100 },
            fld: { type: 'number' }
        },
        source_addr: {
            col: { text: 'Source Address'.t(), width: 120 },
            fld: { type: 'string', sortType: 'asIp' }
        },
        source_port: {
            col: { text: 'Source Port'.t(), filter: Rndr.filters.numeric, width: 120 },
            fld: { type: 'integer' }
        },
        start_time: {
            col: { text: 'Start Time'.t(), filter: Rndr.filters.date, width: 160 }, // converter
            fld: { type: 'integer', convert: Converter.timestamp }
        },
        subject: {
            col: { text: 'Subject'.t(), width: 120 },
            fld: { type: 'string' }
        },
        succeeded: {
            col: { text: 'Succeeded'.t(), filter: Rndr.filters.boolean, width: Rndr.colW.boolean, renderer: Rndr.successLogin },
            fld: { type: 'boolean' }
        },
        success: {
            col: { text: 'Success'.t(), filter: Rndr.filters.boolean, width: Rndr.colW.boolean, renderer: Rndr.boolean },
            fld: { type: 'boolean' }
        },
        summary_text: {
            col: { text: 'Summary'.t(), width: 200 },
            fld: { type: 'string' }
        },
        swap_free: {
            col: { text: 'Swap Free'.t(), filter: Rndr.filters.numeric, width: 100, align: 'right', renderer: Rndr.memory },
            fld: { type: 'number' }
        },
        swap_total: {
            col: { text: 'Swap Total'.t(), filter: Rndr.filters.numeric, width: 100, align: 'right', renderer: Rndr.memory },
            fld: { type: 'number' }
        },
        systems: {
            col: { text: 'System Count'.t(), filter: Rndr.filters.numeric, width: 160 },
            fld: { type: 'integer' }
        },
        tags: {
            col: { text: 'Tags'.t(), width: 160 },
            fld: { type: 'string' }
        },
        term: {
            col: { text: 'Query Term'.t(), width: 160 },
            fld: { type: 'string' }
        },
        time_stamp: {
            col: { text: 'Timestamp'.t(), filter: Rndr.filters.date, width: 160 }, // converter
            fld: { type: 'auto', convert: Converter.timestamp }
        },
        tunnel_description: {
            col: { text: 'Tunnel Description'.t(), width: 160 },
            fld: { type: 'string' }
        },
        tunnel_name: {
            col: { text: 'Tunnel Name'.t(), width: 160 },
            fld: { type: 'string' }
        },
        tx_bytes: {
            col: { text: 'TX Bytes'.t(), filter: Rndr.filters.numeric, width: 80 },
            fld: { type: 'number' }
        },
        tx_rate: {
            col: { text: 'TX Rate'.t(), filter: Rndr.filters.numeric, width: 80 },
            fld: { type: 'number' }
        },
        type: {
            col: { text: 'Type'.t(), width: 160 },
            fld: { type: 'string' }
        },
        type_directory_connector: {
            col: { text: 'Action'.t(), dataIndex: 'type', width: 160 },
            fld: { name: 'type', type: 'string', convert: Converter.directoryConnectorAction }
        },
        uri: {
            col: { text: 'URI'.t(), width: 120 },
            fld: { type: 'string' }
        },
        username: {
            col: { text: 'Username'.t(), width: 120 },
            fld: { type: 'string' }
        },
        value: {
            col: { text: 'Value'.t(), width: 120 },
            fld: { type: 'string' }
        },
        vendor_name: {
            col: { text: 'Vendor Name'.t(), width: 200 },
            fld: { type: 'string' }
        },

        // applications related columns
        ad_blocker_action: {
            col: { text: 'Action'.t() +  ' (Ad Blocker)', width: 80, renderer: Rndr.adBlockerAction },
            fld: { type: 'string' }
        },
        ad_blocker_cookie_ident: {
            col: { text: 'Blocked Cookie'.t() + ' (Ad Blocker)', width: 120 },
            fld: { type: 'string' }
        },
        application_control_application: {
            col: { text: 'Application'.t() + ' (Application Control)', width: 120 },
            fld: { type: 'string' }
        },
        application_control_blocked: {
            col: { text: 'Blocked'.t() + ' (Application Control)', filter: Rndr.filters.boolean, width: Rndr.colW.boolean, renderer: Rndr.boolean },
            fld: { type: 'boolean' }
        },
        application_control_category: {
            col: { text: 'Category'.t() + ' (Application Control)', width: 120 },
            fld: { type: 'string' }
        },
        application_control_confidence: {
            col: { text: 'Confidence'.t() + ' (Application Control)', filter: Rndr.filters.numeric, width: 120 },
            fld: { type: 'integer' }
        },
        application_control_detail: {
            col: { text: 'Detail'.t() + ' (Application Control)', width: 120 },
            fld: { type: 'string' }
        },
        application_control_flagged: {
            col: { text: 'Flagged'.t() + ' (Application Control)', filter: Rndr.filters.boolean, width: Rndr.colW.boolean },
            fld: { type: 'boolean' }
        },
        application_control_protochain: {
            col: { text: 'Protochain'.t() + ' (Application Control)', width: 120 },
            fld: { type: 'string' }
        },
        application_control_ruleid: {
            col: { text: 'Rule'.t() + ' (Application Control)', filter: Rndr.filters.numeric, width: 120 },
            fld: { type: 'integer' }
        },
        application_control_lite_blocked: {
            col: { text: 'Blocked'.t() + ' (Application Control Lite)', filter: Rndr.filters.boolean, width: Rndr.colW.boolean, renderer: Rndr.boolean },
            fld: { type: 'boolean' }
        },
        application_control_lite_protocol: {
            col: { text: 'Protocol'.t() + ' (Application Control Lite)', width: 120 },
            fld: { type: 'string' } // ??? why text/string
        },
        bandwidth_control_priority: {
            col: { text: 'Priority'.t() + ' (Bandwidth Control)', width: 120, renderer: Rndr.priority },
            fld: { type: 'integer' }
        },
        bandwidth_control_rule: {
            col: { text: 'Rule'.t() + ' (Bandwidth Control)', width: 120, renderer: Rndr.bandwidthControlRule },
            fld: { type: 'integer' }
        },
        captive_portal_blocked: {
            col: { text: 'Blocked'.t() + ' (Captive Portal)', filter: Rndr.filters.boolean, width: Rndr.colW.boolean, renderer: Rndr.boolean },
            fld: { type: 'boolean' }
        },
        captive_portal_rule_index: {
            col: { text: 'Rule Id'.t() + ' (Captive Portal)', filter: Rndr.filters.numeric, width: 120 },
            fld: { type: 'integer' }
        },
        firewall_blocked: {
            col: { text: 'Blocked'.t() + ' (Firewall)', filter: Rndr.filters.boolean, width: Rndr.colW.boolean, renderer: Rndr.boolean },
            fld: { type: 'boolean' }
        },
        firewall_flagged: {
            col: { text: 'Flagged'.t() + ' (Firewall)', filter: Rndr.filters.boolean, width: Rndr.colW.boolean, renderer: Rndr.boolean },
            fld: { type: 'boolean' }
        },
        firewall_rule_index: {
            col: { text: 'Rule'.t() + ' (Firewall)', filter: Rndr.filters.numeric, width: 120 },
            fld: { type: 'integer' }
        },
        phish_blocker_action: {
            col: { text: 'Action'.t() + ' (Phish Blocker)', width: 120 }, // converter
            fld: { type: 'string', convert: Converter.emailAction }
        },
        phish_blocker_is_spam: {
            col: { text: 'Is Spam'.t() + ' (Phish Blocker)', filter: Rndr.filters.boolean, width: Rndr.colW.boolean, renderer: Rndr.boolean },
            fld: { type: 'boolean' }
        },
        phish_blocker_score: {
            col: { text: 'Score'.t() + ' (Phish Blocker)', filter: Rndr.filters.numeric, width: 120 },
            fld: { type: 'number' }
        },
        phish_blocker_tests_string: {
            col: { text: 'Detail'.t() + ' (Phish Blocker)', width: 120 },
            fld: { type: 'string' }
        },
        spam_blocker_action: {
            col: { text: 'Action'.t() + ' (Spam Blocker)', width: 120 },
            fld: { type: 'string', convert: Converter.emailAction }
        },
        spam_blocker_is_spam: {
            col: { text: 'Is Spam'.t() + ' (Spam Blocker)', filter: Rndr.filters.boolean, width: Rndr.colW.boolean, renderer: Rndr.boolean },
            fld: { type: 'boolean' }
        },
        spam_blocker_score: {
            col: { text: 'Spam Score'.t() + ' (Spam Blocker)', filter: Rndr.filters.numeric, width: 120 },
            fld: { type: 'number' }
        },
        spam_blocker_tests_string: {
            col: { text: 'Detail'.t() + ' (Spam Blocker)', width: 120 },
            fld: { type: 'string' }
        },
        spam_blocker_lite_action: {
            col: { text: 'Action'.t() + ' (Spam Blocker Lite)', width: 120},
            fld: { type: 'string', convert: Converter.emailAction }
        },
        spam_blocker_lite_is_spam: {
            col: { text: 'Is Spam'.t() + ' (Spam Blocker Lite)', filter: Rndr.filters.boolean, width: Rndr.colW.boolean, renderer: Rndr.boolean },
            fld: { type: 'boolean' }
        },
        spam_blocker_lite_score: {
            col: { text: 'Spam Score'.t() + ' (Spam Blocker Lite)', filter: Rndr.filters.numeric, width: 120 },
            fld: { type: 'number' }
        },
        spam_blocker_lite_tests_string: {
            col: { text: 'Detail'.t() + ' (Spam Blocker Lite)', width: 120 },
            fld: { type: 'string' }
        },
        ssl_inspector_detail: {
            col: { text: 'Detail'.t() + ' (SSL Inspector)', width: 120 },
            fld: { type: 'string' }
        },
        ssl_inspector_ruleid: {
            col: { text: 'Rule Id'.t() + ' (SSL Inspector)', filter: Rndr.filters.numeric, width: 120 },
            fld: { type: 'integer' }
        },
        ssl_inspector_status: {
            col: { text: 'Status'.t() + ' (SSL Inspector)', width: 120 },
            fld: { type: 'string' }
        },
        // threat_prevention_blocked: {
        //     col: { text: 'Blocked'.t() + ' (Threat Prevention)', filter: Rndr.filters.boolean, width: Rndr.colW.boolean, renderer: Rndr.boolean },
        //     fld: { type: 'boolean' }
        // },
        // threat_prevention_categories: {
        //     col: { text: 'Categories'.t() + ' (Threat Prevention)', width: 120 },
        //     fld: { type: 'integer' }
        // },
        // threat_prevention_client_categories: {
        //     col: { text: 'Client Categories'.t() + ' (Threat Prevention)', width: 100 },
        //     fld: { type: 'integer' }
        // },
        // threat_prevention_client_reputation: {
        //     col: { text: 'Client Reputation'.t() + ' (Threat Prevention)', width: 100 },
        //     fld: { type: 'integer' }
        // },
        // threat_prevention_flagged: {
        //     col: { text: 'Flagged'.t() + ' (Threat Prevention)', filter: Rndr.filters.boolean, width: Rndr.colW.boolean, renderer: Rndr.boolean },
        //     fld: { type: 'boolean' }
        // },
        // threat_prevention_reason: {
        //     col: { text: 'Reason'.t() + ' (Threat Prevention)', width: 100 },
        //     fld: { type: 'string' }
        // },
        // threat_prevention_reputation: {
        //     col: { text: 'Reputation'.t() + ' (Threat Prevention)', width: 120 },
        //     fld: { type: 'integer' }
        // },
        // threat_prevention_rule_id: {
        //     col: { text: 'Rule Id'.t() + ' (Threat Prevention)', width: 120 },
        //     fld: { type: 'integer' }
        // },
        // threat_prevention_server_categories: {
        //     col: { text: 'Server Categories'.t() + ' (Threat Prevention)', width: 100 },
        //     fld: { type: 'integer' }
        // },
        // threat_prevention_server_reputation: {
        //     col: { text: 'Server Reputation'.t() + ' (Threat Prevention)', width: 100 },
        //     fld: { type: 'integer' }
        // },
        virus_blocker_clean: {
            col: { text: 'Clean'.t() + ' (Virus Blocker)', filter: Rndr.filters.boolean, width: Rndr.colW.boolean, renderer: Rndr.boolean },
            fld: { type: 'boolean' }
        },
        virus_blocker_name: {
            col: { text: 'Name'.t() + ' (Virus Blocker)', width: 120 },
            fld: { type: 'string' }
        },
        virus_blocker_lite_clean: {
            col: { text: 'Clean'.t() + ' (Virus Blocker Lite)', filter: Rndr.filters.boolean, width: Rndr.colW.boolean, renderer: Rndr.boolean },
            fld: { type: 'boolean' }
        },
        virus_blocker_lite_name: {
            col: { text: 'Name'.t() + ' (Virus Blocker Lite)', width: 120 },
            fld: { type: 'string' }
        },
        web_filter_blocked: {
            col: { text: 'Blocked'.t() + ' (Web Filter)', filter: Rndr.filters.boolean, width: Rndr.colW.boolean, renderer: Rndr.boolean },
            fld: { type: 'boolean' }
        },
        web_filter_category_id: {
            col: { text: 'Web Category'.t() + ' (Web Filter)', width: 250 }, // converter
            fld: { type: 'integer', convert: Converter.webCategory }
        },
        web_filter_flagged: {
            col: { text: 'Flagged'.t() + ' (Web Filter)', filter: Rndr.filters.boolean, width: Rndr.colW.boolean, renderer: Rndr.boolean },
            fld: { type: 'boolean' }
        },
        web_filter_reason: {
            col: { text: 'Reason For Action'.t() + ' (Web Filter)', width: 120 },
            fld: { type: 'string', convert: Converter.webReason }
        },
        web_filter_rule_id: {
            col: { text: 'Web Rule'.t() + ' (Web Filter)', width: 120, renderer: Renderer.webRule }, // converter not implemented
            fld: { type: 'integer' }
        },

        /**
         * non table specific column
         * used in Settings Changes Events listing
         */
        differences: {
            col: {
                text: "Differences".t(),
                dataIndex: 'settings_file',
                width: Renderer.actionWidth,
                xtype: 'actioncolumn',
                align: 'center',
                tdCls: 'action-cell',
                hideable: false,
                sortable: false,
                hidden: false,
                menuDisabled: true,
                iconCls: 'fa fa-search fa-black',
                tooltip: "Show difference between previous version".t(),
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
                        this.diffWindow = Ext.create('Ext.window.Window',{
                            name: 'diffWindow',
                            title: 'Settings Difference'.t(),
                            closeAction: 'hide',
                            width: Ext.getBody().getViewSize().width - 20,
                            height:Ext.getBody().getViewSize().height - 20,
                            layout: 'fit',
                            items: [{
                                xtype: 'ungrid',
                                name: 'gridDiffs',
                                initialLoad: function() {},
                                cls: 'diff-grid',
                                reload: function(handler) {
                                    this.getStore().getProxy().setData([]);
                                    this.getStore().load();
                                    rpc.settingsManager.getDiff(Ext.bind(function(result,exception) {
                                        var diffWindow = this.up("window[name=diffWindow]");
                                        if (diffWindow ==null || !diffWindow.isVisible()) {
                                            return;
                                        }
                                        if(exception) {
                                            this.getView().setLoading(false);
                                            Util.handleException(exception);
                                            return;
                                        }
                                        var diffData = [];
                                        var diffLines = result.split("\n");
                                        var action;
                                        for( var i = 0; i < diffLines.length; i++) {
                                            var previousAction = diffLines[i].substr(0,1);
                                            var previousLine = diffLines[i].substr(1,510);
                                            var currentAction = diffLines[i].substr(511,1);
                                            var currentLine = diffLines[i].substr(512);

                                            if( previousAction != "<" && previousAction != ">") {
                                                previousLine = previousAction + previousLine;
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
                                        this.getStore().loadRawData(diffData);
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
                                    text: "Line".t(),
                                    dataIndex: "line",
                                    renderer: columnRenderer
                                },{
                                    text: "Previous".t(),
                                    flex: 1,
                                    dataIndex: "previous",
                                    renderer: columnRenderer
                                },{
                                    text: "Current".t(),
                                    flex: 1,
                                    dataIndex: "current",
                                    renderer: columnRenderer
                                }]
                            }],
                            buttons: [{
                                text: "Close".t(),
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
            },
            fld: { type: 'auto' }
        }

        /**
         * NOT USED, kept just for future references/improvement needs
         * merged sessions related fields definition which are not defined above
         * this are "-" dash separated instead of "_"
         * this are used in the record detauls view so they do not require but human readable text
         */
        // 'application-control-application': {
        //     col: { text: 'Application'.t() }
        // }

    },

    /**
     * all tables with corresponding fields as defined in db
     * the fields are used to determine the store model field and the grid column defined above
     * if there are tables having same field name but with different meaning,
     * than use a different mapping for each and not the exact field name
     *
     * !!! Note, all the below columns for table can be fetched using
     * rpc.reportsManager.getColumnsForTable(<table_name>)
     */
    tables: {
        admin_logins: [
            'time_stamp',
            'login',
            'local',
            'client_addr',
            'succeeded',
            'reason_admin_logins' // original: reason
        ],
        alerts: [
            'time_stamp',
            'description',
            'summary_text',
            'json'
        ],
        captive_portal_user_events: [
            'time_stamp',
            'policy_id',
            'event_id',
            'login_name',
            'event_info',
            'auth_type',
            'client_addr'
        ],
        configuration_backup_events: [
            'time_stamp',
            'success',
            'description',
            'destination',
            'event_id'
        ],
        device_table_updates: [
            'time_stamp',
            'mac_address',
            'key',
            'value',
            'old_value'
        ],
        directory_connector_login_events: [
            'time_stamp',
            'login_name',
            'domain',
            'type_directory_connector', // original = type
            'client_addr'
        ],
        ftp_events: [
            'time_stamp',
            'event_id',
            'session_id',
            'client_intf',
            'server_intf',
            'c_client_addr',
            's_client_addr',
            'c_server_addr',
            's_server_addr',
            'policy_id',
            'username',
            'hostname',
            'request_id',
            'method',
            'uri'
        ],
        host_table_updates: [
            'time_stamp',
            'address',
            'key',
            'value',
            'old_value'
        ],
        interface_stat_events: [
            'time_stamp',
            'interface_id',
            'rx_rate',
            'rx_bytes',
            'tx_rate',
            'tx_bytes'
        ],
        ipsec_user_events: [
            'event_id',
            'time_stamp',
            'connect_stamp',
            'goodbye_stamp',
            'client_address',
            'client_protocol',
            'client_username',
            'net_process',
            'net_interface',
            'elapsed_time',
            'rx_bytes',
            'tx_bytes'
        ],
        ipsec_vpn_events: [
            'event_id',
            'time_stamp',
            'local_address',
            'remote_address',
            'tunnel_description',
            'event_type'
        ],
        ipsec_tunnel_stats: [
            'time_stamp',
            'tunnel_name',
            'in_bytes',
            'out_bytes',
            'event_id'
        ],
        wireguard_vpn_stats: [
            'time_stamp',
            'tunnel_name',
            'peer_address',
            'in_bytes',
            'out_bytes',
            'event_id'
        ],
        wireguard_vpn_events: [
            'event_id',
            'time_stamp',
            'tunnel_name',
            'event_type'
        ],
        intrusion_prevention_events: [
            'time_stamp',
            'sig_id',
            'gen_id',
            'class_id',
            'source_addr',
            'source_port',
            'dest_addr',
            'dest_port',
            'protocol',
            'blocked',
            'category',
            'classtype',
            'msg',
            'rid',
            'rule_id'
        ],
        openvpn_events: [
            'time_stamp',
            'remote_address',
            'pool_address',
            'client_name',
            'type'
        ],
        openvpn_stats: [
            'time_stamp',
            'start_time',
            'end_time',
            'rx_bytes',
            'tx_bytes',
            'remote_address',
            'pool_address',
            'remote_port',
            'client_name',
            'event_id'
        ],
        quotas: [
            'time_stamp',
            'entity',
            'action_quotas', // original: action
            'size',
            'reason'
        ],
        server_events: [
            'time_stamp',
            'load_1',
            'load_5',
            'load_15',
            'cpu_user',
            'cpu_system',
            'mem_total',
            'mem_free',
            'disk_total',
            'disk_free',
            'swap_total',
            'swap_free',
            'active_hosts'
        ],
        settings_changes: [
            'time_stamp',
            'settings_file',
            'username',
            'hostname',
            'differences' // custom non sql table field
        ],
        smtp_tarpit_events: [
            'time_stamp',
            'ipaddr',
            'hostname',
            'policy_id',
            'vendor_name',
            'event_id'
        ],
        tunnel_vpn_events: [
            'event_id',
            'time_stamp',
            'tunnel_name',
            'server_address',
            'local_address',
            'event_type'
        ],
        tunnel_vpn_stats: [
            'time_stamp',
            'tunnel_name',
            'in_bytes',
            'out_bytes',
            'event_id'
        ],
        user_table_updates: [
            'time_stamp',
            'username',
            'key',
            'value',
            'old_value'
        ],
        wan_failover_action_events: [
            'time_stamp',
            'interface_id',
            'action',
            'os_name',
            'name',
            'event_id'
        ],
        wan_failover_test_events: [
            'time_stamp',
            'interface_id',
            'name',
            'description',
            'success',
            'event_id'
        ],
        web_cache_stats: [
            'time_stamp',
            'hits',
            'misses',
            'bypasses',
            'systems',
            'hit_bytes',
            'miss_bytes',
            'event_id'
        ],
        http_events: [
            'request_id',
            'time_stamp',
            'session_id',
            'client_intf',
            'server_intf',
            'c_client_addr',
            's_client_addr',
            'c_server_addr',
            's_server_addr',
            'c_client_port',
            's_client_port',
            'c_server_port',
            's_server_port',
            'client_country',
            'client_latitude',
            'client_longitude',
            'server_country',
            'server_latitude',
            'server_longitude',
            'policy_id',
            'username',
            'hostname',
            'method',
            'uri',
            'host',
            'domain',
            'referer',
            'c2s_content_length',
            's2c_content_length',
            's2c_content_type',
            's2c_content_filename',
            'ad_blocker_cookie_ident',
            'ad_blocker_action',
            'web_filter_reason',
            'web_filter_category_id',
            'web_filter_rule_id',
            'web_filter_blocked',
            'web_filter_flagged',
            'virus_blocker_lite_clean',
            'virus_blocker_lite_name',
            'virus_blocker_clean',
            'virus_blocker_name'
            // 'threat_prevention_blocked',
            // 'threat_prevention_flagged',
            // 'threat_prevention_rule_id',
            // 'threat_prevention_reputation',
            // 'threat_prevention_categories'
        ],
        http_query_events: [
            'event_id',
            'time_stamp',
            'session_id',
            'client_intf',
            'server_intf',
            'c_client_addr',
            's_client_addr',
            'c_server_addr',
            's_server_addr',
            'c_client_port',
            's_client_port',
            'c_server_port',
            's_server_port',
            'policy_id',
            'username',
            'hostname',
            'request_id',
            'method',
            'uri',
            'term',
            'host',
            'c2s_content_length',
            's2c_content_length',
            's2c_content_type',
            'blocked',
            'flagged',
            'web_filter_reason'
        ],
        mail_addrs: [
            'time_stamp',
            'session_id',
            'server_intf',
            'c_client_addr',
            'c_server_addr',
            'c_client_port',
            'c_server_port',
            's_client_addr',
            's_server_addr',
            's_client_port',
            's_server_port',
            'policy_id',
            'username',
            'msg_id',
            'subject',
            'addr',
            'addr_name',
            'addr_kind',
            'hostname',
            'event_id',
            'sender',
            'virus_blocker_lite_clean',
            'virus_blocker_lite_name',
            'virus_blocker_clean',
            'virus_blocker_name',
            'spam_blocker_lite_score',
            'spam_blocker_lite_is_spam',
            'spam_blocker_lite_action',
            'spam_blocker_lite_tests_string',
            'spam_blocker_score',
            'spam_blocker_is_spam',
            'spam_blocker_action',
            'spam_blocker_tests_string',
            'phish_blocker_score',
            'phish_blocker_is_spam',
            'phish_blocker_tests_string',
            'phish_blocker_action'
        ],
        mail_msgs: [
            'time_stamp',
            'session_id',
            'server_intf',
            'c_client_addr',
            's_server_addr',
            'c_client_port',
            's_server_port',
            'policy_id',
            'username',
            'msg_id',
            'subject',
            'hostname',
            'event_id',
            'sender',
            'receiver',
            'virus_blocker_lite_clean',
            'virus_blocker_lite_name',
            'virus_blocker_clean',
            'virus_blocker_name',
            'spam_blocker_lite_score',
            'spam_blocker_lite_is_spam',
            'spam_blocker_lite_tests_string',
            'spam_blocker_lite_action',
            'spam_blocker_score',
            'spam_blocker_is_spam',
            'spam_blocker_tests_string',
            'spam_blocker_action',
            'phish_blocker_score',
            'phish_blocker_is_spam',
            'phish_blocker_tests_string',
            'phish_blocker_action',
        ],
        session_minutes: [
            'session_id',
            'time_stamp',
            'c2s_bytes',
            's2c_bytes',
            'start_time',
            'end_time',
            'bypassed',
            'entitled',
            'protocol',
            'icmp_type',
            'hostname',
            'username',
            'policy_id',
            'policy_rule_id',
            'local_addr',
            'remote_addr',
            'c_client_addr',
            'c_server_addr',
            'c_client_port',
            'c_server_port',
            's_client_addr',
            's_server_addr',
            's_client_port',
            's_server_port',
            'client_intf',
            'server_intf',
            'client_country',
            'client_latitude',
            'client_longitude',
            'server_country',
            'server_latitude',
            'server_longitude',
            'filter_prefix',
            'firewall_blocked',
            'firewall_flagged',
            'firewall_rule_index',
            // 'threat_prevention_blocked',
            // 'threat_prevention_flagged',
            // 'threat_prevention_reason',
            // 'threat_prevention_rule_id',
            // 'threat_prevention_client_reputation',
            // 'threat_prevention_client_categories',
            // 'threat_prevention_server_reputation',
            // 'threat_prevention_server_categories',
            'application_control_lite_protocol',
            'application_control_lite_blocked',
            'captive_portal_blocked',
            'captive_portal_rule_index',
            'application_control_application',
            'application_control_protochain',
            'application_control_category',
            'application_control_blocked',
            'application_control_flagged',
            'application_control_confidence',
            'application_control_ruleid',
            'application_control_detail',
            'bandwidth_control_priority',
            'bandwidth_control_rule',
            'ssl_inspector_ruleid',
            'ssl_inspector_status',
            'ssl_inspector_detail',
            'tags'
        ],
        sessions: [
            'session_id',
            'time_stamp',
            'end_time',
            'bypassed',
            'entitled',
            'protocol',
            'icmp_type',
            'hostname',
            'username',
            'policy_id',
            'policy_rule_id',
            'local_addr',
            'remote_addr',
            'c_client_addr',
            'c_server_addr',
            'c_client_port',
            'c_server_port',
            's_client_addr',
            's_server_addr',
            's_client_port',
            's_server_port',
            'client_intf',
            'server_intf',
            'client_country',
            'client_latitude',
            'client_longitude',
            'server_country',
            'server_latitude',
            'server_longitude',
            'c2p_bytes',
            'p2c_bytes',
            's2p_bytes',
            'p2s_bytes',
            'filter_prefix',
            'firewall_blocked',
            'firewall_flagged',
            'firewall_rule_index',
            // 'threat_prevention_blocked',
            // 'threat_prevention_flagged',
            // 'threat_prevention_reason',
            // 'threat_prevention_rule_id',
            // 'threat_prevention_client_reputation',
            // 'threat_prevention_client_categories',
            // 'threat_prevention_server_reputation',
            // 'threat_prevention_server_categories',
            'application_control_lite_protocol',
            'application_control_lite_blocked',
            'captive_portal_blocked',
            'captive_portal_rule_index',
            'application_control_application',
            'application_control_protochain',
            'application_control_category',
            'application_control_blocked',
            'application_control_flagged',
            'application_control_confidence',
            'application_control_ruleid',
            'application_control_detail',
            'bandwidth_control_priority',
            'bandwidth_control_rule',
            'ssl_inspector_ruleid',
            'ssl_inspector_status',
            'ssl_inspector_detail',
            'tags'
        ]
    },

    listeners: {
        sessions: {},
        http_events: {}
    },


    webReasons: {
        D: 'in Categories Block list'.t(),
        U: 'in Site Block list'.t(),
        T: 'in Search Term list'.t(),
        E: 'in File Block list'.t(),
        M: 'in MIME Types Block list'.t(),
        H: 'hostname is an IP address'.t(),
        I: 'in Site Pass list'.t(),
        R: 'referer in Site Pass list'.t(),
        C: 'in Clients Pass list'.t(),
        B: 'in Temporary Unblocked list'.t(),
        F: 'in Rules list'.t(),
        K: 'Kid-friendly redirect'.t(),
        default: 'no rule applied'.t()
    },

    webCategories: {
        0: 'Uncategorized',
        1: 'Real Estate',
        2: 'Computer and Internet Security',
        3: 'Financial Services',
        4: 'Business and Economy',
        5: 'Computer and Internet Info',
        6: 'Auctions',
        7: 'Shopping',
        8: 'Cult and Occult',
        9: 'Travel',
        10: 'Abused Drugs',
        11: 'Adult and Pornography',
        12: 'Home and Garden',
        13: 'Military',
        14: 'Social Networking',
        15: 'Dead Sites',
        16: 'Individual Stock Advice and Tools',
        17: 'Training and Tools',
        18: 'Dating',
        19: 'Sex Education',
        20: 'Religion',
        21: 'Entertainment and Arts',
        22: 'Personal sites and Blogs',
        23: 'Legal',
        24: 'Local Information',
        25: 'Streaming Media',
        26: 'Job Search',
        27: 'Gambling',
        28: 'Translation',
        29: 'Reference and Research',
        30: 'Shareware and Freeware',
        31: 'Peer to Peer',
        32: 'Marijuana',
        33: 'Hacking',
        34: 'Games',
        35: 'Philosophy and Political Advocacy',
        36: 'Weapons',
        37: 'Pay to Surf',
        38: 'Hunting and Fishing',
        39: 'Society',
        40: 'Educational Institutions',
        41: 'Online Greeting Cards',
        42: 'Sports',
        43: 'Swimsuits and Intimate Apparel',
        44: 'Questionable',
        45: 'Kids',
        46: 'Hate and Racism',
        47: 'Personal Storage',
        48: 'Violence',
        49: 'Keyloggers and Monitoring',
        50: 'Search Engines',
        51: 'Internet Portals',
        52: 'Web Advertisements',
        53: 'Cheating',
        54: 'Gross',
        55: 'Web-based Email',
        56: 'Malware Sites',
        57: 'Phishing and Other Frauds',
        58: 'Proxy Avoidance and Anonymizers',
        59: 'Spyware and Adware',
        60: 'Music',
        61: 'Government',
        62: 'Nudity',
        63: 'News and Media',
        64: 'Illegal',
        65: 'Content Delivery Networks',
        66: 'Internet Communications',
        67: 'Bot Nets',
        68: 'Abortion',
        69: 'Health and Medicine',
        71: 'SPAM URLs',
        74: 'Dynamically Generated Content',
        75: 'Parked Domains',
        76: 'Alcohol and Tobacco',
        78: 'Image and Video Search',
        79: 'Fashion and Beauty',
        80: 'Recreation and Hobbies',
        81: 'Motor Vehicles',
        82: 'Web Hosting'
    },

    protocols: {
        0: 'HOPOPT [0]',
        1: 'ICMP [1]',
        2: 'IGMP [2]',
        3: 'GGP [3]',
        4: 'IP-in-IP [4]',
        5: 'ST [5]',
        6: 'TCP [6]',
        7: 'CBT [7]',
        8: 'EGP [8]',
        9: 'IGP [9]',
        10: 'BBN-RCC-MON [10]',
        11: 'NVP-II [11]',
        12: 'PUP [12]',
        13: 'ARGUS [13]',
        14: 'EMCON [14]',
        15: 'XNET [15]',
        16: 'CHAOS [16]',
        17: 'UDP [17]',
        18: 'MUX [18]',
        19: 'DCN-MEAS [19]',
        20: 'HMP [20]',
        21: 'PRM [21]',
        22: 'XNS-IDP [22]',
        23: 'TRUNK-1 [23]',
        24: 'TRUNK-2 [24]',
        25: 'LEAF-1 [25]',
        26: 'LEAF-2 [26]',
        27: 'RDP [27]',
        28: 'IRTP [28]',
        29: 'ISO-TP4 [29]',
        30: 'NETBLT [30]',
        31: 'MFE-NSP [31]',
        32: 'MERIT-INP [32]',
        33: 'DCCP [33]',
        34: '3PC [34]',
        35: 'IDPR [35]',
        36: 'XTP [36]',
        37: 'DDP [37]',
        38: 'IDPR-CMTP [38]',
        39: 'TP++ [39]',
        40: 'IL [40]',
        41: 'IPv6 [41]',
        42: 'SDRP [42]',
        43: 'IPv6-Route [43]',
        44: 'IPv6-Frag [44]',
        45: 'IDRP [45]',
        46: 'RSVP [46]',
        47: 'GRE [47]',
        48: 'MHRP [48]',
        49: 'BNA [49]',
        50: 'ESP [50]',
        51: 'AH [51]',
        52: 'I-NLSP [52]',
        53: 'SWIPE [53]',
        54: 'NARP [54]',
        55: 'MOBILE [55]',
        56: 'TLSP [56]',
        57: 'SKIP [57]',
        58: 'IPv6-ICMP [58]',
        59: 'IPv6-NoNxt [59]',
        60: 'IPv6-Opts [60]',
        62: 'CFTP [62]',
        64: 'SAT-EXPAK [64]',
        65: 'KRYPTOLAN [65]',
        66: 'RVD [66]',
        67: 'IPPC [67]',
        69: 'SAT-MON [69]',
        70: 'VISA [70]',
        71: 'IPCU [71]',
        72: 'CPNX [72]',
        73: 'CPHB [73]',
        74: 'WSN [74]',
        75: 'PVP [75]',
        76: 'BR-SAT-MON [76]',
        77: 'SUN-ND [77]',
        78: 'WB-MON [78]',
        79: 'WB-EXPAK [79]',
        80: 'ISO-IP [80]',
        81: 'VMTP [81]',
        82: 'SECURE-VMTP [82]',
        83: 'VINES [83]',
        84: 'TTP [84]',
        85: 'NSFNET-IGP [85]',
        86: 'DGP [86]',
        87: 'TCF [87]',
        88: 'EIGRP [88]',
        89: 'OSPF [89]',
        90: 'Sprite-RPC [90]',
        91: 'LARP [91]',
        92: 'MTP [92]',
        93: 'AX.25 [93]',
        94: 'IPIP [94]',
        95: 'MICP [95]',
        96: 'SCC-SP [96]',
        97: 'ETHERIP [97]',
        98: 'ENCAP [98]',
        100: 'GMTP [100]',
        101: 'IFMP [101]',
        102: 'PNNI [102]',
        103: 'PIM [103]',
        104: 'ARIS [104]',
        105: 'SCPS [105]',
        106: 'QNX [106]',
        107: 'A/N [107]',
        108: 'IPComp [108]',
        109: 'SNP [109]',
        110: 'Compaq-Peer [110]',
        111: 'IPX-in-IP [111]',
        112: 'VRRP [112]',
        113: 'PGM [113]',
        115: 'L2TP [115]',
        116: 'DDX [116]',
        117: 'IATP [117]',
        118: 'STP [118]',
        119: 'SRP [119]',
        120: 'UTI [120]',
        121: 'SMP [121]',
        122: 'SM [122]',
        123: 'PTP [123]',
        124: 'IS-IS [124]',
        125: 'FIRE [125]',
        126: 'CRTP [126]',
        127: 'CRUDP [127]',
        128: 'SSCOPMCE [128]',
        129: 'IPLT [129]',
        130: 'SPS [130]',
        131: 'PIPE [131]',
        132: 'SCTP [132]',
        133: 'FC [133]',
        134: 'RSVP-E2E-IGNORE [134]',
        135: 'Mobility [135]',
        136: 'UDPLite [136]',
        137: 'MPLS-in-IP [137]',
        138: 'manet [138]',
        139: 'HIP [139]',
        140: 'Shim6 [140]',
        141: 'WESP [141]',
        142: 'ROHC [142]',
        default: 'Unknown'.t()
    },

    httpMethods: {
        'O': 'OPTIONS (O)',
        'G': 'GET (G)',
        'H': 'HEAD (H)',
        'P': 'POST (P)',
        'U': 'PUT (U)',
        'D': 'DELETE (D)',
        'T': 'TRACE (T)',
        'C': 'CONNECT (C)',
        'X': 'NON-STANDARD (X)'
    },

    emailActions: {
        'P': 'pass message'.t(),
        'M': 'mark message'.t(),
        'D': 'drop message'.t(),
        'B': 'block message'.t(),
        'Q': 'quarantine message'.t(),
        'S': 'pass safelist message'.t(),
        'Z': 'pass oversize message'.t(),
        'O': 'pass outbound message'.t(),
        'F': 'block message (scan failure)'.t(),
        'G': 'pass message (scan failure)'.t(),
        'Y': 'block message (greylist)'.t()
    },

    priorities: {
        0: '',
        1: 'Very High'.t(),
        2: 'High'.t(),
        3: 'Medium'.t(),
        4: 'Low'.t(),
        5: 'Limited'.t(),
        6: 'Limited More'.t(),
        7: 'Limited Severely'.t()
    },

    loginFailureReasons: {
        U: 'invalid username'.t(),
        P: 'invalid password'.t()
    },

    icmps: {
        0: 'Echo Reply'.t(),
        1: 'Unassigned'.t(),
        2: 'Unassigned'.t(),
        3: 'Destination Unreachable'.t(),
        4: 'Source Quench (Deprecated)'.t(),
        5: 'Redirect'.t(),
        6: 'Alternate Host Address (Deprecated)'.t(),
        7: 'Unassigned'.t(),
        8: 'Echo'.t(),
        9: 'Router Advertisement'.t(),
        10: 'Router Solicitation'.t(),
        11: 'Time Exceeded'.t(),
        12: 'Parameter Problem'.t(),
        13: 'Timestamp'.t(),
        14: 'Timestamp Reply'.t(),
        15: 'Information Request (Deprecated)'.t(),
        16: 'Information Reply (Deprecated)'.t(),
        17: 'Address Mask Request (Deprecated)'.t(),
        18: 'Address Mask Reply (Deprecated)'.t(),
        19: 'Reserved (for Security)'.t(),
        20: 'Reserved (for Robustness Experiment)'.t(),
        21: 'Reserved (for Robustness Experiment)'.t(),
        22: 'Reserved (for Robustness Experiment)'.t(),
        23: 'Reserved (for Robustness Experiment)'.t(),
        24: 'Reserved (for Robustness Experiment)'.t(),
        25: 'Reserved (for Robustness Experiment)'.t(),
        26: 'Reserved (for Robustness Experiment)'.t(),
        27: 'Reserved (for Robustness Experiment)'.t(),
        28: 'Reserved (for Robustness Experiment)'.t(),
        29: 'Reserved (for Robustness Experiment)'.t(),
        30: 'Traceroute (Deprecated)'.t(),
        31: 'Datagram Conversion Error (Deprecated)'.t(),
        32: 'Mobile Host Redirect (Deprecated)'.t(),
        33: 'IPv6 Where-Are-You (Deprecated)'.t(),
        34: 'IPv6 I-Am-Here (Deprecated)'.t(),
        35: 'Mobile Registration Request (Deprecated)'.t(),
        36: 'Mobile Registration Reply (Deprecated)'.t(),
        37: 'Domain Name Request (Deprecated)'.t(),
        38: 'Domain Name Reply (Deprecated)'.t(),
        39: 'SKIP (Deprecated)'.t(),
        40: 'Photuris'.t(),
        41:  'ICMP messages utilized by experimental mobility protocols'.t(),
        253: 'RFC3692-style Experiment 1'.t(),
        254: 'RFC3692-style Experiment 2'.t(),
        255: 'Reserved'.t()
    },

    countries: {
        XU: 'Unknown'.t(),
        XL: 'Local'.t(),
        AF: 'Afghanistan'.t(),
        AX: 'Aland Islands'.t(),
        AL: 'Albania'.t(),
        DZ: 'Algeria'.t(),
        AS: 'American Samoa'.t(),
        AD: 'Andorra'.t(),
        AO: 'Angola'.t(),
        AI: 'Anguilla'.t(),
        AQ: 'Antarctica'.t(),
        AG: 'Antigua and Barbuda'.t(),
        AR: 'Argentina'.t(),
        AM: 'Armenia'.t(),
        AW: 'Aruba'.t(),
        AU: 'Australia'.t(),
        AT: 'Austria'.t(),
        AZ: 'Azerbaijan'.t(),
        BS: 'Bahamas'.t(),
        BH: 'Bahrain'.t(),
        BD: 'Bangladesh'.t(),
        BB: 'Barbados'.t(),
        BY: 'Belarus'.t(),
        BE: 'Belgium'.t(),
        BZ: 'Belize'.t(),
        BJ: 'Benin'.t(),
        BM: 'Bermuda'.t(),
        BT: 'Bhutan'.t(),
        BO: 'Bolivia, Plurinational State of'.t(),
        BQ: 'Bonaire, Sint Eustatius and Saba'.t(),
        BA: 'Bosnia and Herzegovina'.t(),
        BW: 'Botswana'.t(),
        BV: 'Bouvet Island'.t(),
        BR: 'Brazil'.t(),
        IO: 'British Indian Ocean Territory'.t(),
        BN: 'Brunei Darussalam'.t(),
        BG: 'Bulgaria'.t(),
        BF: 'Burkina Faso'.t(),
        BI: 'Burundi'.t(),
        KH: 'Cambodia'.t(),
        CM: 'Cameroon'.t(),
        CA: 'Canada'.t(),
        CV: 'Cape Verde'.t(),
        KY: 'Cayman Islands'.t(),
        CF: 'Central African Republic'.t(),
        TD: 'Chad'.t(),
        CL: 'Chile'.t(),
        CN: 'China'.t(),
        CX: 'Christmas Island'.t(),
        CC: 'Cocos (Keeling) Islands'.t(),
        CO: 'Colombia'.t(),
        KM: 'Comoros'.t(),
        CG: 'Congo'.t(),
        CD: 'Congo, the Democratic Republic of the'.t(),
        CK: 'Cook Islands'.t(),
        CR: 'Costa Rica'.t(),
        CI: "Cote d'Ivoire".t(),
        HR: 'Croatia'.t(),
        CU: 'Cuba'.t(),
        CW: 'Curacao'.t(),
        CY: 'Cyprus'.t(),
        CZ: 'Czech Republic'.t(),
        DK: 'Denmark'.t(),
        DJ: 'Djibouti'.t(),
        DM: 'Dominica'.t(),
        DO: 'Dominican Republic'.t(),
        EC: 'Ecuador'.t(),
        EG: 'Egypt'.t(),
        SV: 'El Salvador'.t(),
        GQ: 'Equatorial Guinea'.t(),
        ER: 'Eritrea'.t(),
        EE: 'Estonia'.t(),
        ET: 'Ethiopia'.t(),
        FK: 'Falkland Islands (Malvinas)'.t(),
        FO: 'Faroe Islands'.t(),
        FJ: 'Fiji'.t(),
        FI: 'Finland'.t(),
        FR: 'France'.t(),
        GF: 'French Guiana'.t(),
        PF: 'French Polynesia'.t(),
        TF: 'French Southern Territories'.t(),
        GA: 'Gabon'.t(),
        GM: 'Gambia'.t(),
        GE: 'Georgia'.t(),
        DE: 'Germany'.t(),
        GH: 'Ghana'.t(),
        GI: 'Gibraltar'.t(),
        GR: 'Greece'.t(),
        GL: 'Greenland'.t(),
        GD: 'Grenada'.t(),
        GP: 'Guadeloupe'.t(),
        GU: 'Guam'.t(),
        GT: 'Guatemala'.t(),
        GG: 'Guernsey'.t(),
        GN: 'Guinea'.t(),
        GW: 'Guinea-Bissau'.t(),
        GY: 'Guyana'.t(),
        HT: 'Haiti'.t(),
        HM: 'Heard Island and McDonald Islands'.t(),
        VA: 'Holy See (Vatican City State)'.t(),
        HN: 'Honduras'.t(),
        HK: 'Hong Kong'.t(),
        HU: 'Hungary'.t(),
        IS: 'Iceland'.t(),
        IN: 'India'.t(),
        ID: 'Indonesia'.t(),
        IR: 'Iran, Islamic Republic of'.t(),
        IQ: 'Iraq'.t(),
        IE: 'Ireland'.t(),
        IM: 'Isle of Man'.t(),
        IL: 'Israel'.t(),
        IT: 'Italy'.t(),
        JM: 'Jamaica'.t(),
        JP: 'Japan'.t(),
        JE: 'Jersey'.t(),
        JO: 'Jordan'.t(),
        KZ: 'Kazakhstan'.t(),
        KE: 'Kenya'.t(),
        KI: 'Kiribati'.t(),
        KP: "Korea, Democratic People's Republic of".t(),
        KR: 'Korea, Republic of'.t(),
        KW: 'Kuwait'.t(),
        KG: 'Kyrgyzstan'.t(),
        LA: "Lao People's Democratic Republic".t(),
        LV: 'Latvia'.t(),
        LB: 'Lebanon'.t(),
        LS: 'Lesotho'.t(),
        LR: 'Liberia'.t(),
        LY: 'Libya'.t(),
        LI: 'Liechtenstein'.t(),
        LT: 'Lithuania'.t(),
        LU: 'Luxembourg'.t(),
        MO: 'Macao'.t(),
        MK: 'Macedonia, the Former Yugoslav Republic of'.t(),
        MG: 'Madagascar'.t(),
        MW: 'Malawi'.t(),
        MY: 'Malaysia'.t(),
        MV: 'Maldives'.t(),
        ML: 'Mali'.t(),
        MT: 'Malta'.t(),
        MH: 'Marshall Islands'.t(),
        MQ: 'Martinique'.t(),
        MR: 'Mauritania'.t(),
        MU: 'Mauritius'.t(),
        YT: 'Mayotte'.t(),
        MX: 'Mexico'.t(),
        FM: 'Micronesia, Federated States of'.t(),
        MD: 'Moldova, Republic of'.t(),
        MC: 'Monaco'.t(),
        MN: 'Mongolia'.t(),
        ME: 'Montenegro'.t(),
        MS: 'Montserrat'.t(),
        MA: 'Morocco'.t(),
        MZ: 'Mozambique'.t(),
        MM: 'Myanmar'.t(),
        NA: 'Namibia'.t(),
        NR: 'Nauru'.t(),
        NP: 'Nepal'.t(),
        NL: 'Netherlands'.t(),
        NC: 'New Caledonia'.t(),
        NZ: 'New Zealand'.t(),
        NI: 'Nicaragua'.t(),
        NE: 'Niger'.t(),
        NG: 'Nigeria'.t(),
        NU: 'Niue'.t(),
        NF: 'Norfolk Island'.t(),
        MP: 'Northern Mariana Islands'.t(),
        NO: 'Norway'.t(),
        OM: 'Oman'.t(),
        PK: 'Pakistan'.t(),
        PW: 'Palau'.t(),
        PS: 'Palestine, State of'.t(),
        PA: 'Panama'.t(),
        PG: 'Papua New Guinea'.t(),
        PY: 'Paraguay'.t(),
        PE: 'Peru'.t(),
        PH: 'Philippines'.t(),
        PN: 'Pitcairn'.t(),
        PL: 'Poland'.t(),
        PT: 'Portugal'.t(),
        PR: 'Puerto Rico'.t(),
        QA: 'Qatar'.t(),
        RE: 'Reunion'.t(),
        RO: 'Romania'.t(),
        RU: 'Russian Federation'.t(),
        RW: 'Rwanda'.t(),
        BL: 'Saint Barthelemy'.t(),
        SH: 'Saint Helena, Ascension and Tristan da Cunha'.t(),
        KN: 'Saint Kitts and Nevis'.t(),
        LC: 'Saint Lucia'.t(),
        MF: 'Saint Martin (French part)'.t(),
        PM: 'Saint Pierre and Miquelon'.t(),
        VC: 'Saint Vincent and the Grenadines'.t(),
        WS: 'Samoa'.t(),
        SM: 'San Marino'.t(),
        ST: 'Sao Tome and Principe'.t(),
        SA: 'Saudi Arabia'.t(),
        SN: 'Senegal'.t(),
        RS: 'Serbia'.t(),
        SC: 'Seychelles'.t(),
        SL: 'Sierra Leone'.t(),
        SG: 'Singapore'.t(),
        SX: 'Sint Maarten (Dutch part)'.t(),
        SK: 'Slovakia'.t(),
        SI: 'Slovenia'.t(),
        SB: 'Solomon Islands'.t(),
        SO: 'Somalia'.t(),
        ZA: 'South Africa'.t(),
        GS: 'South Georgia and the South Sandwich Islands'.t(),
        SS: 'South Sudan'.t(),
        ES: 'Spain'.t(),
        LK: 'Sri Lanka'.t(),
        SD: 'Sudan'.t(),
        SR: 'Suriname'.t(),
        SJ: 'Svalbard and Jan Mayen'.t(),
        SZ: 'Swaziland'.t(),
        SE: 'Sweden'.t(),
        CH: 'Switzerland'.t(),
        SY: 'Syrian Arab Republic'.t(),
        TW: 'Taiwan, Province of China'.t(),
        TJ: 'Tajikistan'.t(),
        TZ: 'Tanzania, United Republic of'.t(),
        TH: 'Thailand'.t(),
        TL: 'Timor-Leste'.t(),
        TG: 'Togo'.t(),
        TK: 'Tokelau'.t(),
        TO: 'Tonga'.t(),
        TT: 'Trinidad and Tobago'.t(),
        TN: 'Tunisia'.t(),
        TR: 'Turkey'.t(),
        TM: 'Turkmenistan'.t(),
        TC: 'Turks and Caicos Islands'.t(),
        TV: 'Tuvalu'.t(),
        UG: 'Uganda'.t(),
        UA: 'Ukraine'.t(),
        AE: 'United Arab Emirates'.t(),
        GB: 'United Kingdom'.t(),
        US: 'United States'.t(),
        UM: 'United States Minor Outlying Islands'.t(),
        UY: 'Uruguay'.t(),
        UZ: 'Uzbekistan'.t(),
        VU: 'Vanuatu'.t(),
        VE: 'Venezuela, Bolivarian Republic of'.t(),
        VN: 'Viet Nam'.t(),
        VG: 'Virgin Islands, British'.t(),
        VI: 'Virgin Islands, U.S.'.t(),
        WF: 'Wallis and Futuna'.t(),
        EH: 'Western Sahara'.t(),
        YE: 'Yemen'.t(),
        ZM: 'Zambia'.t(),
        ZW: 'Zimbabwe'.t(),
    },
});
