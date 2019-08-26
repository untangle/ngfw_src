Ext.define('Ung.apps.ipreputation.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-ip-reputation',
    controller: 'app-ip-reputation',

    viewModel: {
        stores: {
            passRules: { data: '{settings.passRules.list}' },
        }
    },

    items: [
        { xtype: 'app-ip-reputation-status' },
        { xtype: 'app-ip-reputation-reputation' },
        { xtype: 'app-ip-reputation-pass' }
    ],

    statics: {
        threatLevels: Ext.create('Ext.data.ArrayStore', {
            fields: [ 'color', 'rangeBegin', 'rangeEnd', 'description', 'details' ],
            sorters: [{
                property: 'rangeBegin',
                direction: 'ASC'
            }],
            data: [
            ]
        }),

        threaLevelsRenderer: function(value, meta, record){
            return Ext.String.format("{0} ({1})".t(), record.get('description'), record.get('details'));
        },

        threats: Ext.create('Ext.data.ArrayStore', {
            fields: [ 'bit', 'description', 'details' ],
            sorters: [{
                property: 'bit',
                direction: 'ASC'
            }],
            data: [
                ["0", 'Any'.t(), 'Any threat'.t()],
                ["1", 'Spam Sources'.t(), 'IP addresses from rogue SMTP server connection attempts, anomalous mail activity detected on network sensors, and IPs correlated with known email and forum spammers.'.t() ],
                ["2", 'Windows Exploits'.t(), 'IP addresses participating in distribution of malware or malware attacks, protocol exploits, recorded login attempts, information stealing and other advanced or nefarious shell code, rootkits, worms or virus-related activity.'.t() ],
                ["3", 'Web Attacks'.t(), 'IP addresses using cross-site scripting, iFrame injection, SQL injection, cross-domain injection, domain password brute force attacks to target vulnerabilities on a web server, and malicious automated web activity.'.t()],
                ["4", 'Botnets'.t(), 'IP addresses acting as botnet Command and Control (C&C) centers, infected zombie machines controlled by the C&C servers, botnet hosts distributing malware, cobalt beacons, suspicious fast flux activity, and ransomware reported C&C.'.t() ],
                ["5", 'Scanners'.t(), 'IP addresses involved in unauthorized reconnaissance activities such as probing, host scanning, port scanning, and brute force login attempts, as well as repetitive and suspicious programmatic behavior observed in regular time intervals.'.t() ],
                ["6", 'Denal of Service'.t(), 'Scanners category includes all reconnaissance such as probes, host scan, domain scan and password brute force attack.'.t() ],
                ["7", 'Reputation'.t(), 'IP addresses with a low Webroot® Reputation Index score. IP addresses in this category have a higher than average risk of hosting and/or distributing malicious content or files.'.t() ],
                ["8", 'Phishing'.t(), 'IP addresses that are hosting phishing sites or initiating suspicious entrapment activities through malicious ads, web email POST forms, and click fraud.'.t() ],
                ["9", 'Proxy'.t(), 'IP addresses offering anonymization services, as well as those known to have been associated with open web proxy, VPN, or suspicious proxy chains.'.t() ],
                ["12", 'Mobile Threats'.t(), 'S IP addresses associated with malicious mobile application distribution or with attacks initiating from a mobile device.'.t() ],
                ["14", 'Tor Proxy'.t(), 'IP addresses associated with Tor Proxy activity such as exit nodes or suspicious IPs associated with Tor activity.'.t() ],
            ]
        }),

        threatsRenderer: function(value, meta, record){
            return record.get('description');
        },

        actions: Ext.create('Ext.data.ArrayStore', {
            fields: [ 'value', 'description'],
            sorters: [{
                property: 'value',
                direction: 'ASC'
            }],
            data: [
                ["block", 'Block'.t()],
                ["pass", 'Pass'.t()]
            ]
        }),
    }

});
