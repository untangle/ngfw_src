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
            ["app-detect", 'This category contains signatures that look for, and control, the traffic of certain applications that generate network activity. This category will be used to control various aspects of how an application behaves.'.t() ],
            ["activex", 'These are designed to catch exploits in the ActiveX framework.'.t() ],
            ["attack-response", 'These are designed to catch the results of a successful attack. Things like \'id=root\', or error messages that indicate a compromise may have happened. Note: Trojan and virus post-infection activity is included generally in the VIRUS signature set, not here.'.t() ],
            ["blacklist", 'This category contains URI, USER-AGENT, DNS, and IP address signatures that have been determined to be indicators of malicious activity. These signatures are based on activity from the Talos virus sandboxes, public list of malicious URLs, and other data sources.'.t() ],
            ["botcc", 'These are autogenerated from several sources of known and confirmed active Botnet and other Command and Control hosts. Updated daily, primary data source is Shadowserver.org.' .t() ],
            ["browser-chrome", 'This category contains detection for vulnerabilities present in the Chrome browser. (This is separate from the browser-webkit category, as Chrome has enough vulnerabilities to be broken out into it\'s own, and while it uses the Webkit rendering engine, there\'s a lot of other features to Chrome.)'.t() ],
            ["browser-firefox", 'This category contains detection for vulnerabilities present in the Firefox browser, or products that have the Gecko engine. (Thunderbird email client, etc)'.t() ],
            ["browser-ie", 'This category contains detection for vulnerabilities present in the Internet Explorer browser (Trident or Tasman engines)'.t() ],
            ["browser-webkit", 'This category contains detection of vulnerabilities present in the Webkit browser engine (aside from Chrome) this includes Apple\'s Safari, RIM\'s mobile browser, Nokia, KDE, Webkit itself, and Palm.'.t() ],
            ["browser-other", 'This category contains detection for vulnerabilities in other browsers not listed above.'.t() ],
            ["browser-plugins", 'This category contains detection for vulnerabilities in browsers that deal with plugins to the browser. (Example: Active-x)'.t() ],
            ["chat", 'These are designed to catch exploits from various chat applications.'.t() ],
            ["ciarmy", 'These are designed to catch exploits identified by Collective Intellegence Network Security.'.t() ],
            ["community", 'General community shared signatures.'.t() ],
            ["compromised", 'Signatures to block known hostile or compromised hosts.'.t() ],
            ["content-replace", 'This category contains any signature that utilizes the replace functionality inside of Snort.'.t() ],
            ["current-events", 'Signatures identified by various alert agencies.'.t() ],
            ["data", 'Sensitive data such as credit cards, social security numbers, etc.'.t() ],
            ["decoder", 'Detect various TCP/UDP/IP level protocol anomolies that typically indicate exploit attempts.'.t() ],
            ["deleted", 'When a signature has been deprecated or replaced it is moved to this categories. Signatures are never totally removed from the signature set, they are moved here.'.t() ],
            ["dns", 'This category is for signatures that may indicate the presence of the DNS protocol or vulnerabilities in the DNS protocol on the network.'.t() ],
            ["dos", 'Intended to catch inbound DOS activity, and outbound indications.'.t() ],
            ["drop", 'This is a daily updated list of the Spamhaus DROP (Don\'t Route or Peer) list. Primarily known professional spammers.'.t() ],
            ["dshield", 'Daily updated list of the DShield top attackers list.'.t() ],
            ["exploit", 'This is an older category which will be deprecated soon. This category looks for exploits against software in a generic form.'.t() ],
            ["exploit-kit", 'This category contains signatures that are specifically tailored to detect exploit kit activity. This does not include post-compromise signatures (as those would be in indicator-compromise). Files that are dropped as result of visiting an exploit kit would be in their respective file category.'.t() ],
            ["file-executable", 'This category contains signatures for vulnerabilities that are found or are delivered through executable files, regardless of platform.'.t() ],
            ["file-flash", 'This category contains signatures for vulnerabilities that are found or are delivered through flash files. Either compressed or uncompressed, regardless of delivery method platform being attacked.'.t() ],
            ["file-image", 'This category contains signatures for vulnerabilities that are found inside of images files. Regardless of delivery method, software being attacked, or type of image. (Examples include: jpg, png, gif, bmp, etc)'.t() ],
            ["file-identify", 'This category is to identify files through file extension, the content in the file (file magic), or header found in the traffic. This information is usually used to then set a flowbit to be used in a different signature.'.t() ],
            ["file-multimedia", 'This category contains signatures for vulnerabilities present inside of multimedia files (mp3, movies, wmv)'.t() ],
            ["file-office", 'This category contains signatures for vulnerabilities present inside of files belonging to the Microsoft Office suite of software. (Excel, PowerPoint, Word, Visio, Access, Outlook, etc)'.t() ],
            ["file-pdf", 'This category contains signatures for vulnerabilities found inside of PDF files. Regardless of method of creation, delivery method, or which piece of software the PDF affects (for example, both Adobe Reader and FoxIt Reader)'.t() ],
            ["file-other", 'This category contains signatures for vulnerabilities present inside a file, that doesn\'t fit into the other categories above.'.t() ],
            ["ftp", 'This category is for signatures that may indicate the presence of the ftp protocol or vulnerabilities in the ftp protocol on the network.'.t() ],
            ["games", 'This category is for signatures that may indicate the presence of gaming protocols or vulnerabilities in the gaming protocols on the network.'.t() ],
            ["icmp", 'This category is for signatures that may indicate the presence of icmp traffic or vulnerabilities in icmp on the network.'.t() ],
            ["icmp-info", 'Attempts to determine target hardware and software using typical ICMP behavior.'.t() ],
            ["imap", 'This category is for signatures that may indicate the presence of the imap protocol or vulnerabilities in the imap protocol on the network.'.t() ],
            ["inappropriate", 'Porn, Kiddy porn, sites you shouldn\'t visit at work, etc. Warning: These are generally quite Regex heavy and thus high load and frequent false positives. Only run these if you\'re really interested.'.t() ],
            ["indicator-compromise", 'This category contains signatures that are clearly to be used only for the detection of a positively compromised system, false positives may occur.'.t() ],
            ["indicator-obfuscation", 'This category contains signatures that are clearly used only for the detection of obfuscated content. Like encoded JavaScript signatures.'.t() ],
            ["indicator-shellcode", 'This category contains signatures that are simply looking for simple identification markers of shellcode in traffic. This replaces the old shellcode.signatures.'.t() ],
            ["info", 'Signatures that attempt to send informaton to commonly known malware sites.'.t() ],
            ["malware-backdoor", 'This category contains signatures for the detection of traffic destined to known listening backdoor command channels. If a piece of malicious soft are opens a port and waits for incoming commands for its control functions, this type of detection will be here. A simple example would be the detection for BackOrifice as it listens on a specific port and then executes the commands sent.'.t() ],
            ["malware-cnc", 'This category contains known malicious command and control activity for identified botnet traffic. This includes call home, downloading of dropped files, and ex-filtration of data. Actual commands issued from Master to Zombie type stuff will also be here.'.t() ],
            ["malware-tools", 'This category contains signatures that deal with tools that can be considered malicious in nature. For example, LOIC.'.t() ],
            ["malware-other", 'This category contains signatures that are malware related, but don\'t fit into one of the other malware categories.'.t() ],
            ["misc", 'Various, otherwise uncategorizable exploit attempts.'.t() ],
            ["mobile-malware", 'This category contains signatures for the detection of traffic on mobile devices.'.t() ],
            ["netbios", 'This category is for signatures that may indicate the presence of NetBIOS traffic or vulnerabilities in NetBIOS on the network.'.t() ],
            ["os-linux", 'This category contains signatures that are looking for vulnerabilities in Linux based OSes. Not for browsers or any other software on it, but simply against the OS itself.'.t() ],
            ["os-solaris", 'This category contains signatures that are looking for vulnerabilities in Solaris based OSes. Not for any browsers or any other software on top of the OS.'.t() ],
            ["os-windows", 'This category contains signatures that are looking for vulnerabilities in Windows based OSes. Not for any browsers or any other software on top of the OS.'.t() ],
            ["os-other", 'This category contains signatures that are looking for vulnerabilities in an OS that is not listed above.'.t() ],
            ["p2p", 'This category deals with pua or Potentially Unwanted Applications that deal with p2p.'.t() ],
            ["pop3", 'This category is for signatures that may indicate the presence of the pop3 protocol or vulnerabilities in the pop3 protocol on the network.'.t() ],
            ["preprocessor_portscan", 'This category contains signatures that are looking for vulnerabilities via portscans.'.t() ],
            ["policy-multimedia", 'This category contains signatures that detect potential violations of policy for multimedia. Examples like the detection of the use of iTunes on the network. This is not for vulnerabilities found within multimedia files, as that would be in file-multimedia.'.t() ],
            ["policy-social", 'This category contains signatures for the detection potential violations of policy on corporate networks for the use of social media. (p2p, chat, etc)'.t() ],
            ["policy-other", 'This category is for signatures that may violate the end-users corporate policy bud do not fall into any of the other policy categories first.'.t() ],
            ["policy-spam", 'This category is for signatures that may indicate the presence of spam on the network.'.t() ],
            ["protocol-finger", 'This category is for signatures that may indicate the presence of the finger protocol or vulnerabilities in the finger protocol on the network.'.t() ],
            ["protocol-ftp", 'This category is for signatures that may indicate the presence of the ftp protocol or vulnerabilities in the ftp protocol on the network.'.t() ],
            ["protocol-icmp", 'This category is for signatures that may indicate the presence of icmp traffic or vulnerabilities in icmp on the network.'.t() ],
            ["protocol-imap", 'This category is for signatures that may indicate the presence of the imap protocol or vulnerabilities in the imap protocol on the network.'.t() ],
            ["protocol-pop", 'This category is for signatures that may indicate the presence of the pop protocol or vulnerabilities in the pop protocol on the network.'.t() ],
            ["protocol-services", 'This category is for signatures that may indicate the presence of the rservices protocol or vulnerabilities in the rservices protocols on the network.'.t() ],
            ["protocol-voip", 'This category is for signatures that may indicate the presence of voip services or vulnerabilities in the voip protocol on the network.'.t() ],
            ["pua-adware", 'This category deals with pua or Potentially Unwanted Applications that deal with adware or spyware.'.t() ],
            ["pua-p2p", 'This category deals with pua or Potentially Unwanted Applications that deal with p2p.'.t() ],
            ["pua-toolbars", 'This category deals with pua or Potentially Unwanted Applications that deal with toolbars installed on the client system. (Google Toolbar, Yahoo Toolbar, Hotbar, etc)'.t() ],
            ["pua-other", 'This category deals with pua or Potentially Unwanted Applications that don\'t fit into one of the categories shown above.'.t() ],
            ["rpc", 'Remote Proceudre Call exploits.'.t() ],
            ["scada", 'SCADA sourced exploits.'.t() ],
            ["scan", 'Scanning exploits.'.t() ],
            ["server-apache", 'This category deals with vulnerabilities in or attacks against the Apache Web Server.'.t() ],
            ["server-iis", 'This category deals with vulnerabilities in or attacks against the Microsoft IIS Web server.'.t() ],
            ["server-mssql", 'This category deals with vulnerabilities in or attacks against the Microsoft SQL Server.'.t() ],
            ["server-mysql", 'This category deals with vulnerabilities in or attacks against Oracle\'s MySQL server.'.t() ],
            ["server-oracle", 'This category deals with vulnerabilities in or attacks against Oracle\'s Oracle DB Server.'.t() ],
            ["server-webapp", 'This category deals with vulnerabilities in or attacks against Web based applications on servers.'.t() ],
            ["server-mail", 'This category contains signatures that detect vulnerabilities in mail servers. (Exchange, Courier). These are separate from the protocol categories, as those deal with the traffic going to the mail servers itself.'.t() ],
            ["server-other", 'This category contains signatures that detect vulnerabilities in or attacks against servers that are not detailed in the above list.'.t() ],
            ["shellcode", 'Exploits that attempt to excute shell commands on various operating systems.'.t() ],
            ["smtp", 'This category is for signatures that may indicate the presence of the SMTP protocol or vulnerabilities in the SMTP protocol on the network.'.t() ],
            ["snmp", 'This category is for signatures that may indicate the presence of the SNMP protocol or vulnerabilities in the SNMP protocol on the network.'.t() ],
            ["sql", 'SQL exploits.'.t() ],
            ["tftp", 'This category is for signatures that may indicate the presence of the TFTP protocol or vulnerabilities in the TFTP protocol on the network.'.t() ],
            ["tor", 'TOR exploits.'.t() ],
            ["trojan", 'Trojan Horse exploits.'.t() ],
            ["user-agents", 'This category deals with vulnerabilities in or attacks against common application clients.'.t() ],
            ["voip", 'This category deals with vulnerabilities in or attacks against Voice Over IP applications.'.t() ],
            ["web-client", 'This category deals with vulnerabilities in or attacks against Web clients.'.t() ],
            ["web-server", 'This category deals with vulnerabilities in or attacks against Web servers.'.t() ],
            ["web-specific-apps", 'This category deals with vulnerabilities in or attacks against Web based applications on servers.'.t() ],
            ["worm", 'Worm exploits.'.t() ],
            ]
        }),

        categoryRenderer: function(value, meta, record){
            return record.get('description');
        },
    }
});
