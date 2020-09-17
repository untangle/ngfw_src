Ext.define('Ung.apps.intrusionprevention.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-intrusion-prevention',
    controller: 'app-intrusion-prevention',

    viewModel: {
        data: {
            signatureStatusTotal: 0,
            signatureStatusLog: 0,
            signatureStatusBlock: 0,
            signatureStatusDisable: 0,
            lastUpdateCheck: '',
            lastUpdate: ''
        },
        stores: {
            rules: {
                storeId: 'rulesStore',
                model: 'Ung.model.intrusionprevention.rule',
                data: '{settings.rules.list}',
                listeners:{
                    update: 'rulesChanged',
                    datachanged: 'rulesChanged'
                }
            },
            signatures: {
                storeId: 'signaturesStore',
                model: 'Ung.model.intrusionprevention.signature',
                data: '{signaturesList}',
                groupField: 'classtype',
                sorters: [{
                    property: 'sid',
                    direction: 'ASC'
                }],
                listeners:{
                    datachanged: 'signaturesChanged'
                }
            },
            variables: {
                storeId: 'variablesStore',
                fields: [{
                    name: 'variable',
                },{
                    name: 'definition'
                },{
                    name: 'description'
                }],
                data: '{settings.variables.list}',
                sorters: [{
                    property: 'variable',
                    direction: 'ASC'
                }],
                listeners:{
                    update: 'variablesChanged',
                    datachanged: 'variablesChanged'
                }
            },
            networkVariables: {
                fields: [{
                    name: 'value'
                },{
                    name: 'description'
                }],
                data: '{networkVariablesList}'
            },
            bypassRules: {
                data: '{settings.bypassRules.list}'
            },
            updateSignatureSchedule: {
                data: '{settings.updateSignatureSchedule.list}'
            }
        }
    },

    items: [
        { xtype: 'app-intrusion-prevention-status' },
        { xtype: 'app-intrusion-prevention-rules' },
        { xtype: 'app-intrusion-prevention-signatures' },
        { xtype: 'app-intrusion-prevention-variables' },
        { xtype: 'app-intrusion-prevention-bypass' },
        { xtype: 'app-intrusion-prevention-advanced',
            tabConfig:{
                hidden: true,
                bind: {
                    hidden: '{!isExpertMode}'
                }
            }
        }
    ],

    statics: {
        updateSignatureFrequency: Ext.create('Ext.data.ArrayStore', {
            fields: [ 'name', 'value' ],
            data: [
                [ 'None'.t(), 'None' ],
                [ 'Daily'.t(), 'Daily' ],
                [ 'Weekly'.t(), 'Weekly' ]
            ]
        }),

        updateSignatureHour: Ext.create('Ext.data.ArrayStore', {
            fields: [ 'stringTime', 'intTime' ],
            data: [
                [ 'Default'.t(), -1],
                [ '1', 1 ],
                [ '2', 2 ],
                [ '3', 3 ],
                [ '4', 4 ],
                [ '5', 5 ],
                [ '6', 6 ],
                [ '7', 7 ],
                [ '8', 8 ],
                [ '9', 9 ],
                [ '10', 10 ],
                [ '11', 11 ],
                [ '12', 12 ]
            ]
        }),

        updateSignatureMinute: Ext.create('Ext.data.ArrayStore', {
            fields: [ 'stringTime', 'intTime' ],
            data: [
                [ 'Default'.t(), -1]
            ]
        }),

        getAllMinutes: function() {
            var store = Ung.apps.intrusionprevention.Main.updateSignatureMinute;
            var i;
            for (i = 0; i < 60; i++) {
                if (i < 10)
                    store.add({'stringTime': '0'+i, 'intTime': i});
                else
                    store.add({'stringTime': i, 'intTime': i});
            }
            return store;
        },

        updateSignatureDays: Ext.create('Ext.data.ArrayStore', {
            fields: [ 'displayValue', 'value' ],
            data: [
                [ ' '.t(), "None" ],
                [ 'Sunday'.t(), "Sunday" ],
                [ 'Monday'.t(), "Monday" ],
                [ 'Tuesday'.t(), "Tuesday" ],
                [ 'Wednesday'.t(), "Wednesday" ],
                [ 'Thursday'.t(), "Thursday" ],
                [ 'Friday'.t(), "Friday" ],
                [ 'Saturday'.t(), "Saturday" ]
            ]
        }),

        updateSignatureIsAm: Ext.create('Ext.data.ArrayStore', {
            fields: [ 'displayValue', 'value' ],
            data: [
                [ 'AM', true ],
                [ 'PM', false ]
            ]
        }),

        updateSignatureDaily: function() {
            return {
                xtype: 'grid',
    
                hideHeaders: true,
                bodyBorder: false,
                border: false,
                hidden: true,

                bind: {
                    store: '{updateSignatureSchedule}',
                    hidden: '{settings.updateSignatureFrequency != \'Daily\'}'
                },
                columns: [{
                    xtype: 'widgetcolumn',
                    dataIndex: 'enabled',
                    width: 30,
                    widget: {
                        xtype: 'checkbox',
                        bind: '{record.enabled}',
                    }
                }, {
                    dataIndex: 'day',
                    width: 80,
                }, {
                    dataIndex: 'hour',
                    xtype: 'widgetcolumn',
                    widget: {
                        xtype: 'combo',
                        editable: false,
                        queryMode: 'local',
                        bind: '{record.hour}',
                        store: Ung.apps.intrusionprevention.Main.updateSignatureHour,
                        displayField: 'stringTime',
                        valueField: 'intTime'
                    }
                }, {
                    dataIndex: 'colon',
                    width: 20
                }, {
                    dataIndex: 'minute',
                    xtype: 'widgetcolumn',
                    widget: {
                        xtype: 'combo',
                        editable: false,
                        queryMode: 'local',
                        bind: '{record.minute}',
                        store: Ung.apps.intrusionprevention.Main.updateSignatureMinute,
                        displayField: 'stringTime',
                        valueField: 'intTime'
                    }
                }, {
                    dataIndex: 'isAm',
                    xtype: 'widgetcolumn',
                    width: 70,
                    widget: {
                        xtype: 'combo',
                        editable: false,
                        width: 60,
                        padding: '0 0 0 10',
                        queryMode: 'local',
                        bind: '{record.isAm}',
                        store: Ung.apps.intrusionprevention.Main.updateSignatureIsAm,
                        displayField: 'displayValue',
                        valueField: 'value'
                    }
                }]
            };
            
        },

        updateSignatureWeekly: function() {
            return {
                xtype: 'fieldset',

                layout: {
                    type: 'hbox'
                },
    
                border: false,
                hidden: true,
                padding: '0 0 0 0',

                bind: {
                    hidden: '{settings.updateSignatureFrequency != \'Weekly\'}'
                },
                    
                items: [{
                    xtype: 'combo',
                    editable: false,
                    queryMode: 'local',
                    bind: '{settings.updateSignatureWeekly.day}',
                    store: Ung.apps.intrusionprevention.Main.updateSignatureDays,
                    displayField: 'displayValue',
                    valueField: 'value',
                    width: 100
                }, {
                    xtype: 'combo',
                    editable: false,
                    queryMode: 'local',
                    bind: '{settings.updateSignatureWeekly.hour}',
                    store: Ung.apps.intrusionprevention.Main.updateSignatureHour,
                    displayField: 'stringTime',
                    valueField: 'intTime',
                    padding: '0 0 0 20',
                    width: 105
                }, {
                    xtype: 'component',
                    html: ':',
                    width: 20,
                    padding: '0 0 0 8'
                }, {
                    xtype: 'combo',
                    editable: false,
                    queryMode: 'local',
                    bind: '{settings.updateSignatureWeekly.minute}',
                    store: Ung.apps.intrusionprevention.Main.getAllMinutes(),
                    displayField: 'stringTime',
                    valueField: 'intTime',
                    width: 105
                }, {
                    xtype: 'combo',
                    editable: false,
                    queryMode: 'local',
                    bind: '{settings.updateSignatureWeekly.isAm}',
                    store: Ung.apps.intrusionprevention.Main.updateSignatureIsAm,
                    displayField: 'displayValue',
                    valueField: 'value',
                    padding: '0 0 0 10',
                    width: 60
                }]
            };
            
        },

        updateSignatureButton: {
            xtype: 'button',
            text: 'Update Now'.t(),
            handler: 'updateSignatureManual'
        },


        processingStage: Ext.create('Ext.data.ArrayStore', {
            fields: [ 'value', 'description', 'detail'],
            data: [
                ['pre', 'Before other network processing'.t(), 'More detections in this mode which may not be very useful in a gateway environment.'.t()],
                ['post', 'After other network processing'.t(), 'Less detections in this mode which is typically more appropiate for a gateway environment.'.t()]
            ]
        }),

        signatureActions: Ext.create('Ext.data.ArrayStore', {
            fields: [ 'value', 'description'],
            data: [
                [ 'log', 'Log'.t() ],
                [ 'block', 'Block'.t() ],
                [ 'disable', 'Disable'.t() ]
            ]
        }),

        ruleActions: Ext.create('Ext.data.ArrayStore', {
            fields: [ 'value', 'description'],
            data: [
                [ 'default', 'Recommended'.t()],
                [ 'log', 'Enable Log'.t() ],
                [ 'blocklog', 'Enable Block if Recommended is Enabled'.t() ],
                [ 'block', 'Enable Block'.t()],
                [ 'disable', 'Disable'.t() ],
                [ 'whitelist', 'Whitelist'.t() ]
            ]
        }),

        actionNetworks: [{
            label: 'Match source networks'.t(),
            key: 'sourceNetworks' 
        },{
            label: 'Match destination networks'.t(),
            key: 'destinationNetworks'
        }],
        actionRenderer: function(value, meta, record){
            return record.get('description');
        },

        classtypes: Ext.create('Ext.data.ArrayStore', {
            fields: [ 'value', 'description', 'priority' ],
            sorters: [{
                property: 'value',
                direction: 'ASC'
            }],
            data: [
            [ "attempted-admin", "Attempted Administrator Privilege Gain".t(), "high"],
            [ "attempted-user", "Attempted User Privilege Gain".t(), "high" ],
            [ "policy-violation", "Potential Corporate Privacy Violation".t(), "high" ],
            [ "shellcode-detect", "Executable code was detected".t(), "high" ],
            [ "successful-admin", "Successful Administrator Privilege Gain".t(), "high" ],
            [ "successful-user", "Successful User Privilege Gain".t(), "high" ],
            [ "trojan-activity", "A Network Trojan was detected".t(), "high" ],
            [ "unsuccessful-user", "Unsuccessful User Privilege Gain".t(), "high" ],
            [ "web-application-attack", "Web Application Attack".t(), "high" ],

            [ "attempted-dos", "Attempted Denial of Service".t(), "medium" ],
            [ "attempted-recon", "Attempted Information Leak".t(), "medium" ],
            [ "bad-unknown", "Potentially Bad Traffic".t(), "medium" ],
            [ "default-login-attempt", "Attempt to login by a default username and password".t(), "medium" ],
            [ "denial-of-service", "Detection of a Denial of Service Attack".t(), "medium" ],
            [ "misc-attack", "Misc Attack".t(), "medium" ],
            [ "non-standard-protocol", "Detection of a non-standard protocol or event".t(), "medium" ],
            [ "rpc-portmap-decode", "Decode of an RPC Query".t(), "medium" ],
            [ "successful-dos", "Denial of Service".t(), "medium" ],
            [ "successful-recon-largescale", "Large Scale Information Leak".t(), "medium" ],
            [ "successful-recon-limited", "Information Leak".t(), "medium" ],
            [ "suspicious-filename-detect", "A suspicious filename was detected".t(), "medium" ],
            [ "suspicious-login", "An attempted login using a suspicious username was detected".t(), "medium" ],
            [ "system-call-detect", "A system call was detected".t(), "medium" ],
            [ "unusual-client-port-connection", "A client was using an unusual port".t(), "medium" ],
            [ "web-application-activity", "Access to a potentially vulnerable web application".t(), "medium" ],

            [ "general", "General traffic".t(), "low" ],
            [ "misc-activity", "Misc activity".t(), "low" ],
            [ "network-scan", "Detection of a Network Scan".t(), "low" ],
            [ "not-suspicious", "Not Suspicious Traffic".t(), "low" ],
            [ "protocol-command-decode", "Generic Protocol Command Decode".t(), "low" ],
            [ "string-detect", "A suspicious string was detected".t(), "low" ],
            [ "unknown", "Unknown Traffic".t(), "low" ]
            ]
        }),

        classtypeRenderer: function(value, meta, record){
            return Ext.String.format("{0} ({1})".t(), record.get('description'), record.get('priority'));
        },

        categories: Ext.create('Ext.data.ArrayStore', {
            fields: [ 'value', 'description' ],
            sorters: [{
                property: 'value',
                direction: 'ASC'
            }],
            data: [
            ["activex", 'These are designed to catch exploits in the ActiveX framework.'.t() ],
            ["app-layer-events", 'These rules log events related to the application layer.'.t() ],
            ["attack_response", 'These are designed to catch the results of a successful attack. Things like \'id=root\', or error messages that indicate a compromise may have happened. Note: Trojan and virus post-infection activity is included generally in the VIRUS signature set, not here.'.t() ],
            ["botcc", 'These are autogenerated from several sources of known and confirmed active Botnet and other Command and Control hosts. Updated daily, primary data source is Shadowserver.org.' .t() ],
            ["botcc.portgrouped", 'Same as botcc but grouped by destination port.'.t() ],
            ["chat", 'These are designed to catch exploits from various chat applications.'.t() ],
            ["ciarmy", 'These are designed to catch exploits identified by Collective Intellegence Network Security.'.t() ],
            ["compromised", 'Signatures to block known hostile or compromised hosts.'.t() ],
            ["current_events", 'Signatures identified by various alert agencies.'.t() ],
            ["decoder-events", 'These suricata-specific rules log normalization events related to decoding.'.t() ],
            ["deleted", 'When a signature has been deprecated or replaced it is moved to this categories. Signatures are never totally removed from the signature set, they are moved here.'.t() ],
            ["dns", 'This category is for signatures that may indicate the presence of the DNS protocol or vulnerabilities in the DNS protocol on the network.'.t() ],
            ["dns-events", 'These rules log events related to DNS.'.t() ],
            ["dos", 'Intended to catch inbound DOS activity, and outbound indications.'.t() ],
            ["drop", 'This is a daily updated list of the Spamhaus DROP (Don\'t Route or Peer) list. Primarily known professional spammers.'.t() ],
            ["dshield", 'Daily updated list of the DShield top attackers list.'.t() ],
            ["exploit", 'This is an older category which will be deprecated soon. This category looks for exploits against software in a generic form.'.t() ],
            ["files", 'Example rules for using the file handling and extraction functionality in Suricata.'.t() ],
            ["ftp", 'This category is for signatures that may indicate the presence of the ftp protocol or vulnerabilities in the ftp protocol on the network.'.t() ],
            ["games", 'This category is for signatures that may indicate the presence of gaming protocols or vulnerabilities in the gaming protocols on the network.'.t() ],
            ["http-events", 'These rules are rules to log HTTP protocol specific events, typically normal operation.'.t() ],
            ["icmp", 'This category is for signatures that may indicate the presence of icmp traffic or vulnerabilities in icmp on the network.'.t() ],
            ["icmp_info", 'Attempts to determine target hardware and software using typical ICMP behavior.'.t() ],
            ["imap", 'This category is for signatures that may indicate the presence of the imap protocol or vulnerabilities in the imap protocol on the network.'.t() ],
            ["inappropriate", 'Porn, Kiddy porn, sites you shouldn\'t visit at work, etc. Warning: These are generally quite Regex heavy and thus high load and frequent false positives. Only run these if you\'re really interested.'.t() ],
            ["info", 'Signatures that attempt to send informaton to commonly known malware sites.'.t() ],
            ["ipsec-events", 'These rules log events related to insecure IPsec.'.t() ],
            ["kerberos-events", 'These rules log events related to insecure and wrongly configured Kerberos.'.t() ],
            ["malware", 'This category is malware and spyware related with no criminal intent. The threshold for inclusion in this set is typically some form of tracking that stops short of obvious criminal activity.'.t() ],
            ["misc", 'Various, otherwise uncategorizable exploit attempts.'.t() ],
            ["mobile_malware", 'This category contains signatures for the detection of traffic on mobile devices.'.t() ],
            ["modbus-events", 'These rules log events related to modbus.'.t() ],
            ["netbios", 'This category is for signatures that may indicate the presence of NetBIOS traffic or vulnerabilities in NetBIOS on the network.'.t() ],
            ["nfs-events", 'These rules log events related to malformed NFS requests and responses.'.t() ],
            ["ntp-events", 'These rules log events related to malformed NTP requests and responses.'.t() ],
            ["p2p", 'This category deals with pua or Potentially Unwanted Applications that deal with p2p.'.t() ],
            ["policy", "This category is for the rules for things that are often disallowed by company or organizational policy. Myspace, Ebay, that kind of thing. ".t() ],
            ["pop3", 'This category is for signatures that may indicate the presence of the pop3 protocol or vulnerabilities in the pop3 protocol on the network.'.t() ],
            ["rpc", 'Remote Proceudre Call exploits.'.t() ],
            ["scada", 'SCADA sourced exploits.'.t() ],
            ["scan", 'Scanning exploits.'.t() ],
            ["shellcode", 'Exploits that attempt to excute shell commands on various operating systems.'.t() ],
            ["smb-events", 'These rules log events related to SMB internal parser errors and malformed requests and responses for SMB.'.t() ],
            ["smtp", 'This category is for signatures that may indicate the presence of the SMTP protocol or vulnerabilities in the SMTP protocol on the network.'.t() ],
            ["smtp-events", 'These rules log events related to SMTP i.e. incorrect configuration, malformed requests.'.t() ],
            ["snmp", 'This category is for signatures that may indicate the presence of the SNMP protocol or vulnerabilities in the SNMP protocol on the network.'.t() ],
            ["sql", 'SQL exploits.'.t() ],
            ["stream-events", 'These rules log events related to streams.'.t() ],
            ["telnet", 'These rules are related to telnet vulnerabilities and presence of telnet.'.t() ],            
            ["tftp", 'This category is for signatures that may indicate the presence of the TFTP protocol or vulnerabilities in the TFTP protocol on the network.'.t() ],
            ["tls-events", 'These rules log events related to insecure and wrong TLS.'.t() ],
            ["tor", 'TOR exploits.'.t() ],
            ["trojan", 'Trojan Horse exploits.'.t() ],
            ["user_agents", 'This category deals with vulnerabilities in or attacks against common application clients.'.t() ],
            ["voip", 'This category deals with vulnerabilities in or attacks against Voice Over IP applications.'.t() ],
            ["web_client", 'This category deals with vulnerabilities in or attacks against Web clients.'.t() ],
            ["web_server", 'This category deals with vulnerabilities in or attacks against Web servers.'.t() ],
            ["web_specific_apps", 'This category deals with vulnerabilities in or attacks against Web based applications on servers.'.t() ],
            ["worm", 'Worm exploits.'.t() ],
            ]
        }),

        categoryRenderer: function(value, meta, record){
            return record.get('description');
        },
    }
});
