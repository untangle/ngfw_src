Ext.define('Ung.config.events.MainController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.config.events',

    control: {
        '#': {
            afterrender: 'loadEvents',
        },
    },

    loadEvents: function () {
        var v = this.getView(),
            vm = this.getViewModel();
        v.setLoading(true);

        Ext.Deferred.sequence([
            Rpc.asyncPromise('rpc.eventManager.getSettings'),
        ], this).then(function(result) {
            vm.set({
                settings: result[0],
            });
        }, function(ex) {
            console.error(ex);
            Util.exceptionToast(ex);
        }).always(function() {
            v.setLoading(false);
        });

        vm.set('conditions', {
    HostTableEvent: {
    description: "These events are created by the base system and inserted to the [[Database_Schema#host_table_updates|host_table_updates]] table when the host table is modified.",
    fields: [
        {
        name: "address",
        type: "InetAddress",
        description: "The address",
        },
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "key",
        type: "String",
        description: "The key",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
        {
        name: "value",
        type: "String",
        description: "The value",
        },
    ]
    },
    DeviceTableEvent: {
    description: "These events are created by the base system and inserted to the [[Database_Schema#device_table_updates|device_table_updates]] table when the device list is modified.",
    fields: [
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "device",
        type: "DeviceTableEntry",
        description: "The Device",
        },
        {
        name: "key",
        type: "String",
        description: "The key",
        },
        {
        name: "macAddress",
        type: "String",
        description: "The MAC address",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
        {
        name: "value",
        type: "String",
        description: "The value",
        },
    ]
    },
    UserTableEvent: {
    description: "These events are created by the base system and inserted to the [[Database_Schema#host_table_updates|host_table_updates]] table when the host table is modified.",
    fields: [
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "key",
        type: "String",
        description: "The key",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
        {
        name: "username",
        type: "String",
        description: "The username",
        },
        {
        name: "value",
        type: "String",
        description: "The value",
        },
    ]
    },
    SessionStatsEvent: {
    description: "These events are created by the base system and update the [[Database_Schema#sessions|sessions]] table when a session ends with the updated stats.",
    fields: [
        {
        name: "c2pBytes",
        type: "long",
        description: "The number of bytes sent from the client to Untangle",
        },
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "endTime",
        type: "long",
        description: "The end time/date",
        },
        {
        name: "p2cBytes",
        type: "long",
        description: "The number of bytes sent to the client from Untangle",
        },
        {
        name: "p2sBytes",
        type: "long",
        description: "The number of bytes sent to the server from Untangle",
        },
        {
        name: "s2pBytes",
        type: "long",
        description: "The number of bytes sent from the server to Untangle",
        },
        {
        name: "sessionId",
        type: "Long",
        description: "The session ID",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
    ]
    },
    SessionEvent: {
    description: "These events are created by the base system and update the [[Database_Schema#sessions|sessions]] table each time a session is created.",
    fields: [
        {
        name: "cClientAddr",
        type: "InetAddress",
        description: "The client-side (pre-NAT) client address",
        },
        {
        name: "cClientPort",
        type: "Integer",
        description: "The client-side (pre-NAT) client port",
        },
        {
        name: "cServerAddr",
        type: "InetAddress",
        description: "The client-side (pre-NAT) server address",
        },
        {
        name: "cServerPort",
        type: "Integer",
        description: "The client-side (pre-NAT) server port",
        },
        {
        name: "sClientAddr",
        type: "InetAddress",
        description: "The server-side (post-NAT) client address",
        },
        {
        name: "sClientPort",
        type: "Integer",
        description: "The server-side (post-NAT) client port",
        },
        {
        name: "sServerAddr",
        type: "InetAddress",
        description: "The server-side (post-NAT) server address",
        },
        {
        name: "sServerPort",
        type: "Integer",
        description: "The server-side (post-NAT) server port",
        },
        {
        name: "bypassed",
        type: "boolean",
        description: "True if bypassed, false otherwise",
        },
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "clientCountry",
        type: "String",
        description: "The client country",
        },
        {
        name: "clientIntf",
        type: "Integer",
        description: "The client interface ID",
        },
        {
        name: "clientLatitude",
        type: "Double",
        description: "The client latitude",
        },
        {
        name: "clientLongitude",
        type: "Double",
        description: "The client longitude",
        },
        {
        name: "entitled",
        type: "boolean",
        description: "The entitled status",
        },
        {
        name: "filterPrefix",
        type: "String",
        description: "The filter prefix if blocked by the filter rules",
        },
        {
        name: "hostname",
        type: "String",
        description: "The hostname",
        },
        {
        name: "icmpType",
        type: "Short",
        description: "The ICMP type",
        },
        {
        name: "localAddr",
        type: "InetAddress",
        description: "The local host address",
        },
        {
        name: "policyId",
        type: "Integer",
        description: "The policy ID",
        },
        {
        name: "policyRuleId",
        type: "Integer",
        description: "The policy rule ID",
        },
        {
        name: "protocol",
        type: "Short",
        description: "The protocol",
        },
        {
        name: "protocolName",
        type: "String",
        description: "The protocol name",
        },
        {
        name: "remoteAddr",
        type: "InetAddress",
        description: "The remote host address",
        },
        {
        name: "serverCountry",
        type: "String",
        description: "The server country",
        },
        {
        name: "serverIntf",
        type: "Integer",
        description: "The server interface ID",
        },
        {
        name: "serverLatitude",
        type: "Double",
        description: "The server latitude",
        },
        {
        name: "serverLongitude",
        type: "Double",
        description: "The server longitude",
        },
        {
        name: "sessionId",
        type: "Long",
        description: "The session ID",
        },
        {
        name: "tagsString",
        type: "String",
        description: "The string value of all tags",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
        {
        name: "username",
        type: "String",
        description: "The username",
        },
    ]
    },
    SessionMinuteEvent: {
    description: "These events are created by the base system and update the [[Database_Schema#sessions|session_minutes]] table each minute a session exists.",
    fields: [
        {
        name: "c2sBytes",
        type: "long",
        description: "The number of bytes sent from the client to the server",
        },
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "s2cBytes",
        type: "long",
        description: "The number of bytes sent from the server to the client",
        },
        {
        name: "sessionId",
        type: "long",
        description: "The session ID",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
    ]
    },
    SessionNatEvent: {
    description: "These events are created by the base system and update the [[Database_Schema#sessions|sessions]] table each time a session is NATd with the post-NAT information.",
    fields: [
        {
        name: "sClientAddr",
        type: "InetAddress",
        description: "The server-side (post-NAT) client address",
        },
        {
        name: "sClientPort",
        type: "Integer",
        description: "The server-side (post-NAT) client port",
        },
        {
        name: "sServerAddr",
        type: "InetAddress",
        description: "The server-side (post-NAT) server address",
        },
        {
        name: "sServerPort",
        type: "Integer",
        description: "The server-side (post-NAT) server port",
        },
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "serverIntf",
        type: "Integer",
        description: "The server interface ID",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
    ]
    },
    QuotaEvent: {
    description: "These events are created by the [[Bandwidth Control]] and inserted or update the [[Database_Schema#quotas|quotas]] table when quotas are given or exceeded.",
    fields: [
        {
        name: "action",
        type: "int",
        description: "The action (1=Quota Given, 2=Quota Exceeded)",
        },
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "entity",
        type: "String",
        description: "The entity",
        },
        {
        name: "quotaSize",
        type: "long",
        description: "The quota size",
        },
        {
        name: "reason",
        type: "String",
        description: "The reason",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
    ]
    },
    SettingsChangesEvent: {
    description: "These events are created by the base system and inserted to the [[Database_Schema#settings_changes|settings_changes]] table when settings are changed.",
    fields: [
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "hostname",
        type: "String",
        description: "The hostname",
        },
        {
        name: "settingsFile",
        type: "String",
        description: "The settings file",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
        {
        name: "username",
        type: "String",
        description: "The username",
        },
    ]
    },
    AlertEvent: {
    description: "These events are created by [[Reports]] and inserted to the [[Database_Schema#alerts|alerts]] table when an alert fires.",
    fields: [
        {
        name: "causalRule",
        type: "EventRule",
        description: "The causal rule",
        },
        {
        name: "cause",
        type: "LogEvent",
        description: "The cause",
        },
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "description:",
        type: "String",
        description: "The description:",
        },
        {
        name: "eventSent",
        type: "Boolean",
        description: "True if the event was sent, false otherwise",
        },
        {
        name: "json",
        type: "String",
        description: "The JSON string",
        },
        {
        name: "summaryText",
        type: "String",
        description: "The summary text",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
    ]
    },
    SyslogEvent: {
    description: "These events are created by [[Reports]] and inserted to the [[Database_Schema#syslog|syslog]] table when a syslog event occurs.",
    fields: [
        {
        name: "causalRule",
        type: "EventRule",
        description: "The causal rule",
        },
        {
        name: "cause",
        type: "LogEvent",
        description: "The cause",
        },
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "description:",
        type: "String",
        description: "The description:",
        },
        {
        name: "eventSent",
        type: "Boolean",
        description: "True if the event was sent, false otherwise",
        },
        {
        name: "json",
        type: "String",
        description: "The JSON string",
        },
        {
        name: "summaryText",
        type: "String",
        description: "The summary text",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
    ]
    },
    LogEvent: {
    description: "These base class for all events.",
    fields: [
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
    ]
    },
    InterfaceStatEvent: {
    description: "These events are created by the base system and inserted to the [[Database_Schema#settings_changes|interface_stat_events]] table periodically with interface stats.",
    fields: [
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "interfaceId",
        type: "int",
        description: "The interface ID",
        },
        {
        name: "rxRate",
        type: "double",
        description: "The RX rate in byte/s",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
        {
        name: "txRate",
        type: "double",
        description: "The TX rate in byte/s",
        },
    ]
    },
    SystemStatEvent: {
    description: "These events are created by the base system and inserted to the [[Database_Schema#server_events|server_events]] table periodically.",
    fields: [
        {
        name: "activeHosts",
        type: "int",
        description: "The active host count",
        },
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "cpuSystem",
        type: "float",
        description: "The system CPU utilization",
        },
        {
        name: "cpuUser",
        type: "float",
        description: "The user CPU utilization",
        },
        {
        name: "diskFree",
        type: "long",
        description: "The amount of disk free",
        },
        {
        name: "diskFreePercent",
        type: "float",
        description: "The percentage of disk free",
        },
        {
        name: "diskTotal",
        type: "long",
        description: "The total size of the disk",
        },
        {
        name: "diskUsed",
        type: "long",
        description: "The amount of disk used",
        },
        {
        name: "diskUsedPercent",
        type: "float",
        description: "The percentage of disk used",
        },
        {
        name: "load1",
        type: "float",
        description: "The 1-minute CPU load",
        },
        {
        name: "load15",
        type: "float",
        description: "The 15-minute CPU load",
        },
        {
        name: "load5",
        type: "float",
        description: "The 5-minute CPU load",
        },
        {
        name: "memBuffers",
        type: "long",
        description: "The amount of memory used by buffers",
        },
        {
        name: "memCache",
        type: "long",
        description: "The amount of memory used by cache",
        },
        {
        name: "memFree",
        type: "long",
        description: "The amount of free memory",
        },
        {
        name: "memFreePercent",
        type: "float",
        description: "The percentage of total memory that is free",
        },
        {
        name: "memTotal",
        type: "long",
        description: "The total amount of memory",
        },
        {
        name: "memUsed",
        type: "long",
        description: "The amount of used memory",
        },
        {
        name: "memUsedPercent",
        type: "float",
        description: "The percentage of total memory that is used",
        },
        {
        name: "swapFree",
        type: "long",
        description: "The amount of free swap",
        },
        {
        name: "swapFreePercent",
        type: "float",
        description: "The percentage of total swap that is free",
        },
        {
        name: "swapTotal",
        type: "long",
        description: "The total size of swap",
        },
        {
        name: "swapUsed",
        type: "long",
        description: "The amount of used swap",
        },
        {
        name: "swapUsedPercent",
        type: "float",
        description: "The percentage of total swap that is used",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
    ]
    },
    CaptivePortalUserEvent: {
    description: "These events are created by [[Captive Portal]] and inserted to the [[Database_Schema#captive_portal_user_events|captive_portal_user_events]] table when Captive Portal user takes an action.",
    fields: [
        {
        name: "authenticationType",
        type: "CaptivePortalSettings$AuthenticationType",
        description: "The authentication type",
        },
        {
        name: "authenticationTypeValue",
        type: "String",
        description: "The authentication type as a string",
        },
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "clientAddr",
        type: "String",
        description: "The client address",
        },
        {
        name: "event",
        type: "CaptivePortalUserEvent$EventType",
        description: "The event (LOGIN, FAILED, TIMEOUT, INACTIVE, USER_LOGOUT, ADMIN_LOGOUT)",
        },
        {
        name: "eventValue",
        type: "String",
        description: "The event value as a string (LOGIN, FAILED, TIMEOUT, INACTIVE, USER_LOGOUT, ADMIN_LOGOUT)",
        },
        {
        name: "loginName",
        type: "String",
        description: "The login name",
        },
        {
        name: "policyId",
        type: "Integer",
        description: "The policy ID",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
    ]
    },
    CaptureRuleEvent: {
    description: "These events are created by [[Captive Portal]] and update the [[Database_Schema#sessions|sessions]] table when Captive Portal processes a session.",
    fields: [
        {
        name: "captured",
        type: "boolean",
        description: "True if captured, false otherwise",
        },
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "ruleId",
        type: "Integer",
        description: "The rule ID",
        },
        {
        name: "sessionEvent",
        type: "SessionEvent",
        description: "The session event",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
    ]
    },
    TunnelStatusEvent: {
    description: "These events are created by [[IPsec VPN]] and inserted to the [[Database_Schema#ipsec_tunnel_stats|ipsec_tunnel_stats]] table periodically.",
    fields: [
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "inBytes",
        type: "long",
        description: "The number of bytes received from this tunnel",
        },
        {
        name: "outBytes",
        type: "long",
        description: "The number of bytes sent in this tunnel",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
        {
        name: "tunnelName",
        type: "String",
        description: "The name of this tunnel",
        },
    ]
    },
    VirtualUserEvent: {
    description: "These events are created by [[IPsec VPN]] and inserted to the [[Database_Schema#ipsec_user_events|ipsec_user_events]] table when a user event occurs.",
    fields: [
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "clientAddress",
        type: "InetAddress",
        description: "The client address",
        },
        {
        name: "clientProtocol",
        type: "String",
        description: "The client protocol",
        },
        {
        name: "clientUsername",
        type: "String",
        description: "The client username",
        },
        {
        name: "elapsedTime",
        type: "String",
        description: "The elapsed time",
        },
        {
        name: "eventId",
        type: "Long",
        description: "The event ID",
        },
        {
        name: "netInterface",
        type: "String",
        description: "The net interface",
        },
        {
        name: "netProcess",
        type: "String",
        description: "The net process",
        },
        {
        name: "netRXbytes",
        type: "Long",
        description: "The number of RX (received) bytes",
        },
        {
        name: "netTXbytes",
        type: "Long",
        description: "The number of TX (transmitted) bytes",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
    ]
    },
    ConfigurationBackupEvent: {
    description: "These events are created by [[Configuration Backup]] and inserted to the [[Database_Schema#configuratio_backup_events|configuratio_backup_events]] table when a backup occurs.",
    fields: [
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "destination",
        type: "String",
        description: "The destination",
        },
        {
        name: "detail",
        type: "String",
        description: "The details",
        },
        {
        name: "success",
        type: "boolean",
        description: "True if successful, false otherwise",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
    ]
    },
    IntrusionPreventionLogEvent: {
    description: "These events are created by [[Intrusion Prevention]] and inserted to the [[Database_Schema#intrusion_prevention_events|intrusion_prevention_events]] table when a rule matches.",
    fields: [
        {
        name: "blocked",
        type: "short",
        description: "1 if blocked, 0 otherwise",
        },
        {
        name: "category",
        type: "String",
        description: "The category",
        },
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "classificationId",
        type: "long",
        description: "The classification ID",
        },
        {
        name: "classtype",
        type: "String",
        description: "The classtype",
        },
        {
        name: "dportIcode",
        type: "int",
        description: "The dportIcode",
        },
        {
        name: "eventId",
        type: "long",
        description: "The event ID",
        },
        {
        name: "eventMicrosecond",
        type: "long",
        description: "The event microsecond",
        },
        {
        name: "eventSecond",
        type: "long",
        description: "The event second",
        },
        {
        name: "eventType",
        type: "long",
        description: "The event type",
        },
        {
        name: "generatorId",
        type: "long",
        description: "The generator ID",
        },
        {
        name: "impact",
        type: "short",
        description: "The impact",
        },
        {
        name: "impactFlag",
        type: "short",
        description: "The impact flag",
        },
        {
        name: "ipDestination",
        type: "InetAddress",
        description: "The IP address destination",
        },
        {
        name: "ipSource",
        type: "InetAddress",
        description: "The IP address source",
        },
        {
        name: "mplsLabel",
        type: "long",
        description: "The mplsLabel",
        },
        {
        name: "msg",
        type: "String",
        description: "The msg",
        },
        {
        name: "padding",
        type: "int",
        description: "The padding",
        },
        {
        name: "priorityId",
        type: "long",
        description: "The priority ID",
        },
        {
        name: "protocol",
        type: "short",
        description: "The protocol",
        },
        {
        name: "sensorId",
        type: "long",
        description: "The sensor ID",
        },
        {
        name: "signatureId",
        type: "long",
        description: "The signature ID",
        },
        {
        name: "signatureRevision",
        type: "long",
        description: "The signature revision",
        },
        {
        name: "sportItype",
        type: "int",
        description: "The sportItype",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
        {
        name: "vlanId",
        type: "int",
        description: "The VLAN Id",
        },
    ]
    },
    SslInspectorLogEvent: {
    description: "These events are created by [[SSL Inspector]] and update the [[Database_Schema#sessions|sessions]] table when a session is processed by SSL Inspector.",
    fields: [
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "detail",
        type: "String",
        description: "The details",
        },
        {
        name: "ruleId",
        type: "Integer",
        description: "The rule ID",
        },
        {
        name: "sessionEvent",
        type: "SessionEvent",
        description: "The session event",
        },
        {
        name: "status",
        type: "String",
        description: "The status",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
    ]
    },
    ApplicationControlLiteEvent: {
    description: "These events are created by [[Application Control Lite]] and update the [[Database_Schema#sessions|sessions]] table when application control lite identifies a session.",
    fields: [
        {
        name: "blocked",
        type: "boolean",
        description: "True if blocked, false otherwise",
        },
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "protocol",
        type: "String",
        description: "The protocol",
        },
        {
        name: "sessionId",
        type: "Long",
        description: "The session ID",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
    ]
    },
    ApplicationControlLogEvent: {
    description: "These events are created by [[Application Control]] and update the [[Database_Schema#sessions|sessions]] table when application control identifies a session.",
    fields: [
        {
        name: "application",
        type: "String",
        description: "The application",
        },
        {
        name: "blocked",
        type: "boolean",
        description: "True if blocked, false otherwise",
        },
        {
        name: "category",
        type: "String",
        description: "The category",
        },
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "confidence",
        type: "Integer",
        description: "The confidence (0-100)",
        },
        {
        name: "detail",
        type: "String",
        description: "The details",
        },
        {
        name: "flagged",
        type: "boolean",
        description: "True if flagged, false otherwise",
        },
        {
        name: "protochain",
        type: "String",
        description: "The protochain",
        },
        {
        name: "ruleId",
        type: "Integer",
        description: "The rule ID",
        },
        {
        name: "sessionEvent",
        type: "SessionEvent",
        description: "The session event",
        },
        {
        name: "state",
        type: "Integer",
        description: "The state",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
    ]
    },
    CookieEvent: {
    description: "These events are created by [[Ad Blocker]] and update the [[Database_Schema#http_events|http_events]] table when a cookie is blocked.",
    fields: [
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "identification",
        type: "String",
        description: "The identification string",
        },
        {
        name: "requestId",
        type: "Long",
        description: "The request ID",
        },
        {
        name: "sessionEvent",
        type: "SessionEvent",
        description: "The session event",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
    ]
    },
    AdBlockerEvent: {
    description: "These events are created by [[Ad Blocker]] and update the [[Database_Schema#http_events|http_events]] table when an ad is blocked.",
    fields: [
        {
        name: "action",
        type: "Action",
        description: "The action",
        },
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "reason",
        type: "String",
        description: "The reason",
        },
        {
        name: "requestId",
        type: "Long",
        description: "The request ID",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
    ]
    },
    WebFilterQueryEvent: {
    description: "These events are created by [[Web Filter]] and inserted to the [[Database_Schema#http_query_events|http_query_events]] table when web filter processes a search engine search.",
    fields: [
        {
        name: "appName",
        type: "String",
        description: "The name of the application",
        },
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "contentLength",
        type: "long",
        description: "The content length",
        },
        {
        name: "host",
        type: "String",
        description: "The host",
        },
        {
        name: "method",
        type: "HttpMethod",
        description: "The method",
        },
        {
        name: "requestId",
        type: "Long",
        description: "The request ID",
        },
        {
        name: "requestUri",
        type: "URI",
        description: "The request URI",
        },
        {
        name: "sessionEvent",
        type: "SessionEvent",
        description: "The session event",
        },
        {
        name: "term",
        type: "String",
        description: "The search term/phrase",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
    ]
    },
    WebFilterEvent: {
    description: "These events are created by [[Web Filter]] and update the [[Database_Schema#http_events|http_events]] table when web filter processes a web request.",
    fields: [
        {
        name: "appName",
        type: "String",
        description: "The name of the application",
        },
        {
        name: "blocked",
        type: "Boolean",
        description: "True if blocked, false otherwise",
        },
        {
        name: "category",
        type: "String",
        description: "The category",
        },
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "flagged",
        type: "Boolean",
        description: "True if flagged, false otherwise",
        },
        {
        name: "reason",
        type: "Reason",
        description: "The reason",
        },
        {
        name: "requestLine",
        type: "RequestLine",
        description: "The request line",
        },
        {
        name: "sessionEvent",
        type: "SessionEvent",
        description: "The session event",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
    ]
    },
    PrioritizeEvent: {
    description: "These events are created by the [[Bandwidth Control]] and update the [[Database_Schema#sessions|session]] table when a session is prioritized.",
    fields: [
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "priority",
        type: "int",
        description: "The priority",
        },
        {
        name: "ruleId",
        type: "int",
        description: "The rule ID",
        },
        {
        name: "sessionEvent",
        type: "SessionEvent",
        description: "The session event",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
    ]
    },
    WanFailoverTestEvent: {
    description: "These events are created by [[WAN Failover]] and inserted to the [[Database_Schema#wan_failover_test_events|wan_failover_test_events]] table when a test is run.",
    fields: [
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "description:",
        type: "String",
        description: "The description:",
        },
        {
        name: "interfaceId",
        type: "int",
        description: "The interface ID",
        },
        {
        name: "name",
        type: "String",
        description: "The test name",
        },
        {
        name: "osName",
        type: "String",
        description: "The O/S interface name",
        },
        {
        name: "success",
        type: "Boolean",
        description: "True if successful, false otherwise",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
    ]
    },
    WanFailoverEvent: {
    description: "These events are created by [[WAN Failover]] and inserted to the [[Database_Schema#wan_failover_action_events|wan_failover_action_events]] table when WAN Failover takes an action.",
    fields: [
        {
        name: "action",
        type: "WanFailoverEvent$Action",
        description: "The action",
        },
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "interfaceId",
        type: "int",
        description: "The interface ID",
        },
        {
        name: "name",
        type: "String",
        description: "The name",
        },
        {
        name: "osName",
        type: "String",
        description: "The O/S interface name",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
    ]
    },
    SpamSmtpTarpitEvent: {
    description: "These events are created by [[Spam Blocker]] and inserted to the [[Database_Schema#smtp_tarpit_events|smtp_tarpit_events]] table when a session is tarpitted.",
    fields: [
        {
        name: "iPAddr",
        type: "InetAddress",
        description: "The IP address",
        },
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "hostname",
        type: "String",
        description: "The hostname",
        },
        {
        name: "sessionEvent",
        type: "SessionEvent",
        description: "The session event",
        },
        {
        name: "sessionId",
        type: "Long",
        description: "The session ID",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
        {
        name: "vendorName",
        type: "String",
        description: "The application name",
        },
    ]
    },
    SpamLogEvent: {
    description: "These events are created by [[Spam Blocker]] and update the [[Database_Schema#mail_msgs|mail_msgs]] table when an email is scanned.",
    fields: [
        {
        name: "action",
        type: "SpamMessageAction",
        description: "The action",
        },
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "clientAddr",
        type: "InetAddress",
        description: "The client address",
        },
        {
        name: "clientPort",
        type: "int",
        description: "The client port",
        },
        {
        name: "messageId",
        type: "Long",
        description: "The message ID",
        },
        {
        name: "receiver",
        type: "String",
        description: "The receiver",
        },
        {
        name: "score",
        type: "float",
        description: "The score",
        },
        {
        name: "sender",
        type: "String",
        description: "The sender",
        },
        {
        name: "serverAddr",
        type: "InetAddress",
        description: "The server address",
        },
        {
        name: "serverPort",
        type: "int",
        description: "The server port",
        },
        {
        name: "smtpMessageEvent",
        type: "SmtpMessageEvent",
        description: "The parent SMTP message event",
        },
        {
        name: "isSpam",
        type: "boolean",
        description: "True if spam, false otherwise",
        },
        {
        name: "subject",
        type: "String",
        description: "The subject",
        },
        {
        name: "testsString",
        type: "String",
        description: "The tests string from the spam engine",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
        {
        name: "vendorName",
        type: "String",
        description: "The application name",
        },
    ]
    },
    FirewallEvent: {
    description: "These events are created by [[Firewall]] and update the [[Database_Schema#sessions|sessions]] table when a firewall rule matches a session.",
    fields: [
        {
        name: "blocked",
        type: "boolean",
        description: "True if blocked, false otherwise",
        },
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "flagged",
        type: "boolean",
        description: "True if flagged, false otherwise",
        },
        {
        name: "ruleId",
        type: "long",
        description: "The rule ID",
        },
        {
        name: "sessionId",
        type: "Long",
        description: "The session ID",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
    ]
    },
    LoginEvent: {
    description: "These events are created by [[Directory Connector]] and inserted to the [[Database_Schema#directory_connector_login_events|directory_connector_login_events]] table for each login.",
    fields: [
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "clientAddr",
        type: "InetAddress",
        description: "The client address",
        },
        {
        name: "domain",
        type: "String",
        description: "The domain",
        },
        {
        name: "event",
        type: "String",
        description: "The event",
        },
        {
        name: "loginName",
        type: "String",
        description: "The login name",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
    ]
    },
    SmtpMessageAddressEvent: {
    description: "These events are created by SMTP subsystem and inserted to the [[Database_Schema#mail_addrs|mail_addrs]] table for each address on each email.",
    fields: [
        {
        name: "addr",
        type: "String",
        description: "The address",
        },
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "kind",
        type: "AddressKind",
        description: "The type for this address (F=From, T=To, C=CC, G=Envelope From, B=Envelope To, X=Unknown)",
        },
        {
        name: "messageId",
        type: "Long",
        description: "The message ID",
        },
        {
        name: "personal",
        type: "String",
        description: "personal",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
    ]
    },
    SmtpMessageEvent: {
    description: "These events are created by SMTP subsystem and inserted to the [[Database_Schema#mail_msgs|mail_msgs]] table for each email.",
    fields: [
        {
        name: "addresses",
        type: "Set",
        description: "The addresses",
        },
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "envelopeFromAddress",
        type: "String",
        description: "The envelop FROM address",
        },
        {
        name: "envelopeToAddress",
        type: "String",
        description: "The envelope TO address",
        },
        {
        name: "messageId",
        type: "Long",
        description: "The message ID",
        },
        {
        name: "receiver",
        type: "String",
        description: "The receiver",
        },
        {
        name: "sender",
        type: "String",
        description: "The sender",
        },
        {
        name: "sessionEvent",
        type: "SessionEvent",
        description: "The session event",
        },
        {
        name: "sessionId",
        type: "Long",
        description: "The session ID",
        },
        {
        name: "subject",
        type: "String",
        description: "The subject",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
        {
        name: "tmpFile",
        type: "File",
        description: "The /tmp file",
        },
    ]
    },
    VirusSmtpEvent: {
    description: "These events are created by [[Virus Blocker]] and update the [[Database_Schema#mail_msgs|mail_msgs]] table when Virus Blocker scans an email.",
    fields: [
        {
        name: "action",
        type: "String",
        description: "The action",
        },
        {
        name: "appName",
        type: "String",
        description: "The name of the application",
        },
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "clean",
        type: "boolean",
        description: "True if clean, false otherwise",
        },
        {
        name: "messageId",
        type: "Long",
        description: "The message ID",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
        {
        name: "virusName",
        type: "String",
        description: "The virus name, if not clean",
        },
    ]
    },
    VirusFtpEvent: {
    description: "These events are created by [[Virus Blocker]] and update the [[Database_Schema#ftp_events|ftp_events]] table when Virus Blocker scans an FTP transfer.",
    fields: [
        {
        name: "appName",
        type: "String",
        description: "The name of the application",
        },
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "clean",
        type: "boolean",
        description: "True if clean, false otherwise",
        },
        {
        name: "sessionEvent",
        type: "SessionEvent",
        description: "The session event",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
        {
        name: "uri",
        type: "String",
        description: "The URI",
        },
        {
        name: "virusName",
        type: "String",
        description: "The virus name, if not clean",
        },
    ]
    },
    VirusHttpEvent: {
    description: "These events are created by [[Virus Blocker]] and update the [[Database_Schema#http_events|http_events]] table when Virus Blocker scans an HTTP transfer.",
    fields: [
        {
        name: "appName",
        type: "String",
        description: "The name of the application",
        },
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "clean",
        type: "boolean",
        description: "True if clean, false otherwise",
        },
        {
        name: "requestId",
        type: "Long",
        description: "The request ID",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
        {
        name: "virusName",
        type: "String",
        description: "The virus name, if not clean",
        },
    ]
    },
    // SpamSmtpTarpitEvent: {
    // description: "These events are created by [[Spam Blocker]] and inserted to the [[Database_Schema#smtp_tarpit_events|smtp_tarpit_events]] table when a session is tarpitted.",
    // fields: [
    //     {
    //     name: "iPAddr",
    //     type: "InetAddress",
    //     description: "The IP address",
    //     },
    //     {
    //     name: "class",
    //     type: "Class",
    //     description: "The class name",
    //     },
    //     {
    //     name: "hostname",
    //     type: "String",
    //     description: "The hostname",
    //     },
    //     {
    //     name: "sessionEvent",
    //     type: "SessionEvent",
    //     description: "The session event",
    //     },
    //     {
    //     name: "sessionId",
    //     type: "Long",
    //     description: "The session ID",
    //     },
    //     {
    //     name: "timeStamp",
    //     type: "Timestamp",
    //     description: "The timestamp",
    //     },
    //     {
    //     name: "vendorName",
    //     type: "String",
    //     description: "The application name",
    //     },
    // ]
    // },
    // SpamLogEvent: {
    // description: "These events are created by [[Spam Blocker]] and update the [[Database_Schema#mail_msgs|mail_msgs]] table when an email is scanned.",
    // fields: [
    //     {
    //     name: "action",
    //     type: "SpamMessageAction",
    //     description: "The action",
    //     },
    //     {
    //     name: "class",
    //     type: "Class",
    //     description: "The class name",
    //     },
    //     {
    //     name: "clientAddr",
    //     type: "InetAddress",
    //     description: "The client address",
    //     },
    //     {
    //     name: "clientPort",
    //     type: "int",
    //     description: "The client port",
    //     },
    //     {
    //     name: "messageId",
    //     type: "Long",
    //     description: "The message ID",
    //     },
    //     {
    //     name: "receiver",
    //     type: "String",
    //     description: "The receiver",
    //     },
    //     {
    //     name: "score",
    //     type: "float",
    //     description: "The score",
    //     },
    //     {
    //     name: "sender",
    //     type: "String",
    //     description: "The sender",
    //     },
    //     {
    //     name: "serverAddr",
    //     type: "InetAddress",
    //     description: "The server address",
    //     },
    //     {
    //     name: "serverPort",
    //     type: "int",
    //     description: "The server port",
    //     },
    //     {
    //     name: "smtpMessageEvent",
    //     type: "SmtpMessageEvent",
    //     description: "The parent SMTP message event",
    //     },
    //     {
    //     name: "isSpam",
    //     type: "boolean",
    //     description: "True if spam, false otherwise",
    //     },
    //     {
    //     name: "subject",
    //     type: "String",
    //     description: "The subject",
    //     },
    //     {
    //     name: "testsString",
    //     type: "String",
    //     description: "The tests string from the spam engine",
    //     },
    //     {
    //     name: "timeStamp",
    //     type: "Timestamp",
    //     description: "The timestamp",
    //     },
    //     {
    //     name: "vendorName",
    //     type: "String",
    //     description: "The application name",
    //     },
    // ]
    // },
    HttpResponseEvent: {
    description: "These events are created by HTTP subsystem and update the [[Database_Schema#http_events|http_events]] table when a web response happens.",
    fields: [
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "contentLength",
        type: "long",
        description: "The content length",
        },
        {
        name: "contentType",
        type: "String",
        description: "The content type",
        },
        {
        name: "httpRequestEvent",
        type: "HttpRequestEvent",
        description: "The corresponding HTTP request event",
        },
        {
        name: "requestLine",
        type: "RequestLine",
        description: "The request line",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
    ]
    },
    HttpRequestEvent: {
    description: "These events are created by HTTP subsystem and inserted to the [[Database_Schema#http_events|http_events]] table when a web request happens.",
    fields: [
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "contentLength",
        type: "long",
        description: "The content length",
        },
        {
        name: "domain",
        type: "String",
        description: "The domain",
        },
        {
        name: "host",
        type: "String",
        description: "The host",
        },
        {
        name: "method",
        type: "HttpMethod",
        description: "The HTTP method",
        },
        {
        name: "referer",
        type: "String",
        description: "The referer",
        },
        {
        name: "requestId",
        type: "Long",
        description: "The request ID",
        },
        {
        name: "requestUri",
        type: "URI",
        description: "The request URI",
        },
        {
        name: "sessionEvent",
        type: "SessionEvent",
        description: "The session event",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
    ]
    },
    OpenVpnEvent: {
    description: "These events are created by [[OpenVPN]] and update the [[Database_Schema#openvpn_events|openvpn_events]] table when OpenVPN processes a client action.",
    fields: [
        {
        name: "address",
        type: "InetAddress",
        description: "The address",
        },
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "clientName",
        type: "String",
        description: "The client name",
        },
        {
        name: "poolAddress",
        type: "InetAddress",
        description: "The pool address",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
        {
        name: "type",
        type: "OpenVpnEvent$EventType",
        description: "The type",
        },
    ]
    },
    OpenVpnStatusEvent: {
    description: "These events are created by [[OpenVPN]] and update the [[Database_Schema#openvpn_stats|openvpn_stats]] table periodically.",
    fields: [
        {
        name: "address",
        type: "InetAddress",
        description: "The address",
        },
        {
        name: "bytesRxDelta",
        type: "long",
        description: "The delta number of RX (received) bytes from the previous event",
        },
        {
        name: "bytesRxTotal",
        type: "long",
        description: "The total number of RX (received) bytes",
        },
        {
        name: "bytesTxDelta",
        type: "long",
        description: "The delta number of TX (transmitted) bytes from the previous event",
        },
        {
        name: "bytesTxTotal",
        type: "long",
        description: "The total number of TX (transmitted) bytes",
        },
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "clientName",
        type: "String",
        description: "The client name",
        },
        {
        name: "end",
        type: "Timestamp",
        description: "The end",
        },
        {
        name: "poolAddress",
        type: "InetAddress",
        description: "The pool address",
        },
        {
        name: "port",
        type: "int",
        description: "The port",
        },
        {
        name: "start",
        type: "Timestamp",
        description: "The start",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
    ]
    },
    WebCacheEvent: {
    description: "These events are created by [[Web Cache]] and inserted to the [[Database_Schema#web_cache_stats|web_cache_stats]] table periodically.",
    fields: [
        {
        name: "bypassCount",
        type: "long",
        description: "The number of bypasses",
        },
        {
        name: "class",
        type: "Class",
        description: "The class name",
        },
        {
        name: "hitBytes",
        type: "long",
        description: "The number of bytes worth of hits",
        },
        {
        name: "hitCount",
        type: "long",
        description: "The number of hits",
        },
        {
        name: "missBytes",
        type: "long",
        description: "The number of bytes worth of misses",
        },
        {
        name: "missCount",
        type: "long",
        description: "The number of misses",
        },
        {
        name: "policyId",
        type: "Long",
        description: "The policy ID",
        },
        {
        name: "systemCount",
        type: "long",
        description: "The number of system bypasses",
        },
        {
        name: "timeStamp",
        type: "Timestamp",
        description: "The timestamp",
        },
    ]
    },        }
        );

        
        var conditions = vm.get('conditions');
        var classesStoreData = [];
        for( var className in conditions ){
            // classesStoreData.push([ '*' + className + '*', className, conditions[className].description]);
            classesStoreData.push([className, conditions[className].description]);
        }

        vm.set('classes', Ext.create('Ext.data.ArrayStore', {
            fields: [ 'name', 'description' ],
            sorters: [{
                property: 'name',
                direction: 'ASC'
            }],
            data: classesStoreData
        }) );

    },

    saveSettings: function () {
        var me = this,
            view = this.getView(),
            vm = this.getViewModel();

        if (!Util.validateForms(view)) {
            return;
        }

        view.setLoading(true);

        view.query('ungrid').forEach(function (grid) {
            var store = grid.getStore();
            /**
             * Important!
             * update custom grids only if are modified records or it was reordered via drag/drop
             */
            if (store.getModifiedRecords().length > 0 || store.isReordered) {
                store.each(function (record) {
                    if (record.get('markedForDelete')) {
                        record.drop();
                    }
                });
                store.isReordered = undefined;
                vm.set(grid.listProperty, Ext.Array.pluck(store.getRange(), 'data'));
                // store.commitChanges();
            }
        });

        Ext.Deferred.sequence([
            // !!! NOT RIGHT YET
            Rpc.asyncPromise('rpc.eventManager.setSettings', vm.get('settings')),
        ], this).then(function() {
            me.loadEvents();
            Util.successToast('Events'.t() + ' settings saved!');
        }, function(ex) {
            console.error(ex);
            Util.exceptionToast(ex);
        }).always(function() {
            view.setLoading(false);
        });
    },

});

Ext.define('Ung.config.events.cmp.EventsRecordEditor', {
    extend: 'Ung.cmp.RecordEditor',
    xtype: 'ung.cmp.uneventsrecordeditor',

    controller: 'uneventsrecordeditorcontroller',

    actions: {
        apply: {
            text: 'Done'.t(),
            formBind: true,
            iconCls: 'fa fa-check',
            handler: 'onApply'
        },
        cancel: {
            text: 'Cancel',
            iconCls: 'fa fa-ban',
            handler: 'onCancel'
        },
        addCondition: {
            itemId: 'addConditionBtn',
            text: 'Add Field'.t(),
            iconCls: 'fa fa-plus'
        },
    },

    /// !!! For listerner debugging.
    items: [{
        xtype: 'form',
        // region: 'center',
        scrollable: 'y',
        bodyPadding: 10,
        border: false,
        layout: 'anchor',
        defaults: {
            anchor: '100%',
            labelWidth: 180,
            labelAlign : 'right',
        },
        items: [],
        buttons: ['@cancel', '@apply']
    }],    
});

Ext.define('Ung.config.events.cmp.EventsRecordEditorController', {
    extend: 'Ung.cmp.RecordEditorController',
    alias: 'controller.uneventsrecordeditorcontroller',

    control: {
        '#': {
            beforerender: 'onBeforeRender',
            afterrender: 'onAfterRender',
        },
        'grid': {
            afterrender: 'onConditionsRender'
        }
    },

    conditionsGrid: {
        xtype: 'grid',
        trackMouseOver: false,
        disableSelection: true,
        sortableColumns: false,
        enableColumnHide: false,
        padding: '10 0',
        tbar: ['@addCondition'],
        bind: {
            store: {
                model: 'Ung.model.EventCondition',
                data: '{record.conditions.list}'
            }
        },
        viewConfig: {
            emptyText: '<p style="text-align: center; margin: 0; line-height: 2"><i class="fa fa-exclamation-triangle fa-2x"></i> <br/>No Fields! Add from the menu...</p>',
            stripeRows: false,
        },
        columns: [{
            header: 'Field'.t(),
            name: 'field',
            xtype: 'widgetcolumn',
            menuDisabled: true,
            align: 'right',
            width: 150,
            // renderer: 'conditionRenderer'
            widget: {
                xtype: 'combo',
                editable: false,
                queryMode: 'local',
                bind: '{record.field}',
                valueField: 'name',
                displayField: 'name',
                forceSelection: true,
                listConfig:   { 
                    itemTpl: '<div data-qtip="{description}">{name}</div>'
                },
            },
            onWidgetAttach: 'fieldWidgetAttach'
        }, {
            header: 'Comparator'.t(),
            xtype: 'widgetcolumn',
            menuDisabled: true,
            width: 120,
            resizable: false,
            widget: {
                xtype: 'combo',
                editable: false,
                queryMode: 'local',
                bind: '{record.comparator}',
                valueField: 'name',
                displayField: 'description',
                forceSelection: true
            },
            onWidgetAttach: 'comparatorWidgetAttach'
        }, {
            header: 'Value'.t(),
            xtype: 'widgetcolumn',
            menuDisabled: true,
            sortable: false,
            flex: 1,
            widget: {
                xtype: 'container',
                padding: '0 3'
            },
            onWidgetAttach: 'fieldValueWidgetAttach'
        }, {
            xtype: 'actioncolumn',
            menuDisabled: true,
            sortable: false,
            width: 30,
            align: 'center',
            iconCls: 'fa fa-minus-circle fa-red',
            tdCls: 'action-cell-cond',
            handler: 'removeCondition'
        }]
    },

    massageRecordIn: function(record){
        // breakout class record
        var conditions = record.get('conditions');
        conditions.list.forEach( function(condition){
            if(condition.field == 'class'){
                record.set( 'class', condition.fieldValue.substring(1, condition.fieldValue.length - 1) );
                record.set( '_classCondition', conditions.list.splice(conditions.list.indexOf(condition), 1)[0] );
            }
        });
        record.set('conditions', conditions);
    },
    massageRecordOut: function(record, store){
        var conditionsList = Ext.Array.pluck( store.getRange(), 'data' );
        var classCondition = record.get('_classCondition');
        classCondition.fieldValue = '*' + record.get('class') + '*';
        conditionsList.unshift(classCondition);

        delete(record.data.class);
        delete(record.data._classCondition);

        record.set('conditions', {
            javaClass: 'java.util.LinkedList',
            list: conditionsList
        });
    },

    onBeforeRender: function (v) {
        var vm = this.getViewModel();

        this.mainGrid = v.up('grid');

        var windowTitle = '';
        if (!v.record) {
            v.record = Ext.create('Ung.model.Rule', Ext.clone(this.mainGrid.emptyRow));
            v.record.set('markedForNew', true);
            this.action = 'add';
            windowTitle = 'Add'.t();
        } else {
            windowTitle = 'Edit'.t();
        }

        this.massageRecordIn(v.record);

        this.getViewModel().set({
            record: ( this.action == 'add' ) ? v.record : v.record.copy(null),
            windowTitle: windowTitle
        });

        /**
         * if record has action object
         * hard to explain but needed to keep dirty state (show as modified)
         */
        if (v.record.get('action') && (typeof v.record.get('action') === 'object')) {
            this.actionBind = vm.bind({
                bindTo: '{_action}',
                deep: true
            }, function (actionObj) {
                vm.set('record.action', Ext.clone(actionObj));
            });
            vm.set('_action', v.record.get('action'));
        }

    },

    onAfterRender: function (view) {
        var fields = this.mainGrid.editorFields, 
            form = view.down('form');
        // add editable column fields into the form

        for (var i = 0; i < fields.length; i++) {
            if (fields[i].dataIndex !== 'conditions') {
                form.add(fields[i]);
            } else {
                form.add([{
                    xtype: 'component',
                    padding: '10 0 0 0',
                    html: '<strong>' + 'If all of the following field conditions are met:'.t() + '</strong>'
                },{
                    xtype: 'combo',
                    fieldLabel: 'Class'.t(),
                    editable: false,
                    queryMode: 'local',
                    bind:{
                        value: '{record.class}',
                        store: '{classes}',
                    },
                    valueField: 'name',
                    displayField: 'name',
                    forceSelection: true,
                    listConfig:   { 
                        itemTpl: '<div data-qtip="{description}">{name}</div>'
                    },
                    listeners: {
                        change: 'classChange'
                    }
                },
                this.conditionsGrid
                ]);
            }
        }
        form.isValid();
    },

    onApply: function () {
        var v = this.getView(),
            vm = this.getViewModel(),
            condStore;

        if (v.down('grid')) {
            condStore = v.down('grid').getStore();
            /* Regardless of what changeed, we need to re-integrate the record. */
            this.massageRecordOut(v.record, condStore);
        }

        if (!this.action) {
            for (var field in vm.get('record').modified) {
                if (field !== 'conditions') {
                    v.record.set(field, vm.get('record').get(field));
                }
            }
        }
        if (this.action === 'add') {
            this.mainGrid.getStore().add(v.record);
        }
        v.close();
    },

    onCancel: function () {
        var v = this.getView();
        if (v.down('grid')) {
            condStore = v.down('grid').getStore();
            this.massageRecordOut(v.record, condStore);
        }
        this.getView().close();
    },

    onConditionsRender: function () {
        var vm = this.getViewModel();

        // when record is modified update conditions menu
        this.recordBind = this.getViewModel().bind({
            bindTo: '{record}',
        }, this.setMenuConditions, this);

        var menu = [];
        vm.get('conditions')[vm.get('record.class')].fields.forEach(function(fieldCondition){
            if(fieldCondition.name == 'class' || fieldCondition.name == 'timeStamp' ){
                return;
            }
            menu.push({
                text: fieldCondition.name,
                tooltip: fieldCondition.description,
            });
        });

        var conditionsGrid = this.getView().down('grid');
        conditionsGrid.down('#addConditionBtn').setMenu({
            showSeparator: false,
            plain: true,
            items: menu,
            mouseLeaveDelay: 0,
            listeners: {
                click: 'addCondition'
            }
        });
    },
    /**
     * Updates the disabled/enabled status of the conditions in the menu
     */
    setMenuConditions: function () {
        var conditionsGrid = this.getView().down('grid'),
            menu = conditionsGrid.down('#addConditionBtn').getMenu(),
            store = conditionsGrid.getStore();

        menu.items.each(function (item) {
            item.setDisabled(store.findRecord('field', item.text) ? true : false);
        });
    },

    /**
     * Adds a new condition for the edited rule
     */
    addCondition: function (menu, item) {
        if(item){
            var condition = {
                field: item.text,
                comparator: '=',
                javaClass: this.mainGrid.ruleJavaClass,
                // ?? default values for enums and such?
                fieldValue: ''
            };

            this.getView().down('grid').getStore().add( condition );
            this.setMenuConditions();
        }
    },

    /**
     * Removes a condition from the rule
     */
    removeCondition: function (view, rowIndex, colIndex, item, e, record) {
        // record.drop();
        this.getView().down('grid').getStore().remove(record);
        this.setMenuConditions();
    },

    classChange: function( combo, newValue, oldValue ){
        if(oldValue == null){
            combo.resetOriginalValue();
            return;
        }
        if(newValue == combo.originalValue){
            return;
        }
        if( newValue != oldValue ){
            var record = this.getViewModel().get('record');
            Ext.MessageBox.confirm(
                    'Change Class'.t(), 
                    'If you change the class, fields may be removed. Are you sure?'.t(), 
                    Ext.bind(function(button){
                        if (button == 'no') {
                            record.set('class', oldValue );
                        } else {
                            this.classChangeFields();
                        }
                    },this)
            );
        }
    },

    classChangeFields: function(){
        var vm = this.getViewModel();

        // Re-render add field menu
        this.onConditionsRender();

        // Remove fields if they do not exist in new class.
        var newFields = vm.get('conditions')[vm.get('record.class')].fields;

        var store = this.getView().down('grid').getStore();
        var match;
        store.each(function(record){
            match = false;
            newFields.forEach(function(field){
                if(record.get('field') == field.name){
                    match = true;
                }
            });
            if(match == false){
                store.remove(record);
            }
        });

        // Modify all grid fields to match store
        console.log('look for grid field columns=');
        console.log(this.getView().down('grid').down('[name=field]'));
        var v = this.getView();

        // if conditions
        if (v.down('grid')) {
            console.log('store range');
            console.log(v.down('grid').getStore().getRange());
            console.log(v.down('grid').getView().getRow(0) );
            console.log(v.down('grid').getView() );
        }

        // Change value fields especially if fixed

        // Remove field from Add field

    },

    classFieldStores: {},
    buildClassFieldStore: function( className ){

        if( !(className in this.classFieldStores) ){
            var vm = this.getViewModel();

            var fields = [];
            var conditions = vm.get('conditions');
            for( var conditionsClassName in  conditions ){
                if( conditionsClassName == className ){
                    conditions[className].fields.forEach( function(field){
                        if(field.name == 'class' || field.name == 'timeStamp'){
                            return;
                        }
                        fields.push( [field.name, field.description] ) ;
                    });
                }
            }

            this.classFieldStores[className] = Ext.create('Ext.data.ArrayStore', {
                fields: [ 'name', 'description' ],
                sorters: [{
                    property: 'name',
                    direction: 'ASC'
                }],
                data: fields
            });
        }
        return this.classFieldStores[className];
    },

    /*
     * Change the store for the widget to the appropriate class fieldset
     */
    fieldWidgetAttach: function (column, container, record) {
        var vm = this.getViewModel();
        container.setStore( this.buildClassFieldStore( vm.get('record').get('class') ) );
    },

    comparatorFieldStores: {},
    buildComperatorFieldStore: function( type ){
        var storeName;
        switch(type.toLowerCase()){
            case 'string':
            // case 'class':
                storeName = 'string';
                break;

            case 'boolean':
                storeName = 'boolean';
                break;

            default:
                storeName = 'numeric';
        }
        if( !(storeName in this.comparatorFieldStores) ){
            var vm = this.getViewModel();

            var fields = [];
            switch(storeName){
                case 'string':
                    fields.push([ '=', 'Equals (=)'.t() ]);
                    fields.push([ '!=', 'Does not equal (!=)'.t() ]);
                    break;
                case 'boolean':
                    fields.push([ '=', 'Is'.t() ]);
                    fields.push([ '!=', 'Is not'.t() ]);
                    break;
                default:
                    fields.push([ '>', 'Less (<)'.t() ]);
                    fields.push([ '>=', 'Less or equal (<=)'.t() ]);
                    fields.push([ '=', 'Equals (=)'.t() ]);
                    fields.push([ '<=', 'Greater or equal (>=)'.t() ]);
                    fields.push([ '<', 'Greater (>)'.t() ]);
                    fields.push([ '!=', 'Does not equal (!=)'.t() ]);
            }

            this.classFieldStores[storeName] = Ext.create('Ext.data.ArrayStore', {
                fields: [ 'name', 'description' ],
                data: fields
            });
        }
        return this.classFieldStores[storeName];
    },
    comparatorWidgetAttach: function (column, container, record) {
        var vm = this.getViewModel();
        var className = vm.get('record').get('class');
        var conditions = vm.get('conditions');

        var type = null;
        for( var conditionsClassName in  conditions ){
            if( conditionsClassName == className ){
                conditions[conditionsClassName].fields.forEach( function(field){
                    if(field.name == record.get('field')){
                        type = field.type;
                    }
                });
            }
        }

        container.setStore( this.buildComperatorFieldStore( type ) );
    },

    /**
     * Adds specific condition editor based on it's defined type
     */
    fieldValueWidgetAttach: function (column, container, record) {
        var vm = this.getViewModel();
        var className = vm.get('record').get('class');
        var conditions = vm.get('conditions');

        var type = null;
        for( var conditionsClassName in  conditions ){
            if( conditionsClassName == className ){
                conditions[conditionsClassName].fields.forEach( function(field){
                    if(field.name == record.get('field')){
                        type = field.type;
                    }
                });
            }
        }

        container.removeAll(true);
        switch(type.toLowerCase()){
            case 'string':
                container.add({
                    xtype: 'textfield',
                    style: { margin: 0 },
                    bind: {
                        value: '{record.fieldValue}'
                    },
                    // vtype: condition.vtype
                });
                break;

            case 'class':
                container.add({
                    xtype: 'component',
                    padding: 3,
                    html: className
                    // mouseover?
                });
                break;

            case 'boolean':
                container.add({
                    xtype: 'component',
                    padding: 3,
                    html: 'True'.t()
                });
                break;

            default:
                var allowDecimals = (type == 'float' ? true : false);
                container.add({
                    xtype: 'numberfield',
                    style: { margin: 0 },
                    allowDecimals: allowDecimals,
                    bind: {
                        value: '{record.fieldValue}'
                    },
                    // vtype: condition.vtype
                });
        }

        // determine appropriate type
        // build combo based on type:
        // type: "boolean",
        // type: "Boolean",
            // true

        // type: "Class",

        // type: "double",
        // type: "Double",
        // type: "int",
        // type: "Integer",
        // type: "long",
        // type: "Long",
        // type: "short",
        // type: "Short",
        //  numeric with java limits

        // type: "float",
        //  numeric with decimal vtype

        // type: "InetAddress",
        //  string with IP vtype

        // type: "String",
        //  string

        // type: "Timestamp",
        //  ??

        // type: "URI",
        //  string with url vtype

        // type: "Action",
        // type: "AddressKind",
        // type: "CaptivePortalSettings$AuthenticationType",
        // type: "CaptivePortalUserEvent$EventType",
        // type: "DeviceTableEntry",
        // type: "EventRule",
        // type: "File",
        // type: "HttpMethod",
        // type: "HttpRequestEvent",
        // type: "LogEvent",
        // type: "OpenVpnEvent$EventType",
        // type: "Reason",
        // type: "RequestLine",
        // type: "SessionEvent",
        // type: "Set",
        // type: "SmtpMessageEvent",
        // type: "SpamMessageAction",
        // type: "WanFailoverEvent$Action",


        // container.removeAll(true);

        // var condition = this.mainGrid.conditionsMap[record.get('conditionType')], i, ckItems = [];

        // switch (condition.type) {
        // case 'boolean':
        //     container.add({
        //         xtype: 'component',
        //         padding: 3,
        //         html: 'True'.t()
        //     });
        //     break;
        // case 'textfield':
        //     container.add({
        //         xtype: 'textfield',
        //         style: { margin: 0 },
        //         bind: {
        //             value: '{record.value}'
        //         },
        //         vtype: condition.vtype
        //     });
        //     break;
        // case 'numberfield':
        //     container.add({
        //         xtype: 'numberfield',
        //         style: { margin: 0 },
        //         bind: {
        //             value: '{record.value}'
        //         },
        //         vtype: condition.vtype
        //     });
        //     break;
        // case 'checkboxgroup':
        //     // console.log(condition.values);
        //     // var values_arr = (cond.value !== null && cond.value.length > 0) ? cond.value.split(',') : [], i, ckItems = [];
        //     for (i = 0; i < condition.values.length; i += 1) {
        //         ckItems.push({
        //             inputValue: condition.values[i][0],
        //             boxLabel: condition.values[i][1]
        //         });
        //     }
        //     container.add({
        //         xtype: 'checkboxgroup',
        //         bind: {
        //             value: '{record.value}'
        //         },
        //         columns: 4,
        //         vertical: true,
        //         defaults: {
        //             padding: '0 10 0 0'
        //         },
        //         items: ckItems
        //     });
        // }
    },
});

Ext.define('Ung.config.events.cmp.EventGridController', {
    extend: 'Ung.cmp.GridController',

    alias: 'controller.uneventsgrid',

    onBeforeRender: function (view) {
        // // create conditionsMap for later use
        // if (view.conditions) {
        //     view.conditionsMap = Ext.Array.toValueMap(view.conditions, 'name');
        // }
    },

    editorWin: function (record) {
        this.dialog = this.getView().add({
            xtype: 'ung.cmp.uneventsrecordeditor',
            record: record
        });
        this.dialog.show();
    },

    conditionsRenderer: function (value) {
        var view = this.getView(),
            conds = value.list,
            resp = [], i, valueRenderer = [];

        console.log('conditionsRenderer');


        // for (i = 0; i < conds.length; i += 1) {
        //     valueRenderer = [];

        //     switch (conds[i].conditionType) {
        //     case 'SRC_INTF':
        //     case 'DST_INTF':
        //         conds[i].value.toString().split(',').forEach(function (intfff) {
        //             valueRenderer.push(Util.interfacesListNamesMap()[intfff]);
        //         });
        //         break;
        //     case 'DST_LOCAL':
        //     case 'WEB_FILTER_FLAGGED':
        //         valueRenderer.push('true'.t());
        //         break;
        //     default:
        //         valueRenderer = conds[i].value.toString().split(',');
        //     }
        //     resp.push(view.conditionsMap[conds[i].conditionType].displayName + '<strong>' + (conds[i].invert ? ' &nrArr; ' : ' &rArr; ') + '<span class="cond-val ' + (conds[i].invert ? 'invert' : '') + '">' + valueRenderer.join(', ') + '</span>' + '</strong>');
        // }
        // return resp.length > 0 ? resp.join(' &nbsp;&bull;&nbsp; ') : '<em>' + 'No conditions' + '</em>';
    },
    conditionRenderer: function (val) {
        // return this.getView().conditionsMap[val].displayName;
        return 'conditionRenderer';
    },

});

Ext.define('Ung.config.events.cmp.Grid', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.uneventgrid',

    setConditions: function( conditions ){
        this.conditions = conditions;
    },
    getConditions: function(){
        return this.conditions;
    }

});

Ext.define ('Ung.model.EventCondition', {
    extend: 'Ext.data.Model' ,
    fields: [
        // { name: 'conditionType', type: 'string' },
        // { name: 'invert', type: 'boolean', defaultValue: false },
        { name: 'field', type: 'string', defaultValue: '' },
        { name: 'comparator', type: 'string', defaultValue: '=' },
        { name: 'fieldValue', type: 'auto', defaultValue: '' },
        { name: 'javaClass', type: 'string' },
    ],
    proxy: {
        autoLoad: true,
        type: 'memory',
        reader: {
            type: 'json'
        }
    }
});