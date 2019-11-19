Ext.define('Ung.common.threatprevention.references', {
    // alternateClassName: 'IpReputationReferences',
    singleton: true,

    reputations: Ext.create('Ext.data.ArrayStore', {
        fields: [ 'color', 'rangeBegin', 'rangeEnd', 'description', 'details' ],
        sorters: [{
            property: 'rangeBegin',
            direction: 'DESC'
        }],
        data: [
        [ '408740', 81, 100, 'Trustworthy'.t(), 'These are clean IPs that have not been tied to a security risk. There is a very low predictive risk that your infrastructure and endpoints will be exposed to attack.'.t() ],
        [ '68af68', 61, 80, 'Low Risk'.t(), 'These are benign IPs and rarely exhibit characteristics that expose your infrastructure and endpoints to security risks. There is a low predictive risk of attack.'.t() ],
        [ 'ff9a22', 41, 60, 'Moderate Risk'.t(), 'These are generally benign IPs, but have exhibited some potential risk characteristics. There is some predictive risk that these IPs will deliver attacks to our infrastructure and endpoints.'.t() ],
        [ 'e8580c', 21, 40, 'Suspicious'.t(), 'These are suspicious IPs. There is a higher than average predictive risk that these IPs will deliver attacks to your infrastructure and endpoints.'.t() ],
        [ 'e51313', 0,  20, 'High Risk'.t(), 'These are high risk IP addresses. There is a high predictive risk that these IPs will deliver attacks - such as malicious payloads, DoS attacks, or others - to your infrastructure and endpoints.'.t() ],
        ]
    }),

    reputationRenderer: function(value, meta, record){
        return Ext.String.format("{0} ({1})".t(), record.get('description'), record.get('details'));
    },

    reputatationConditionValues: function(){
        values = [];
        Ung.common.threatprevention.references.reputations.each(function(record){
            values.push([
                record.get('rangeBegin') + '-' + record.get('rangeEnd'),
                record.get('description'),
                record.get('details')
            ]);
        });
        return values;
    },

    // Move to renders
    threats: Ext.create('Ext.data.ArrayStore', {
        fields: [ 'bit', 'description', 'details' ],
        sorters: [{
            property: 'bit',
            direction: 'ASC'
        }],
        data: [
            // ["0", 'Any'.t(), 'Any threat'.t()],
            ["1", 'Spam Sources'.t(), 'IP addresses from rogue SMTP server connection attempts, anomalous mail activity detected on network sensors, and IPs correlated with known email and forum spammers.'.t() ],
            ["2", 'Windows Exploits'.t(), 'IP addresses participating in distribution of malware or malware attacks, protocol exploits, recorded login attempts, information stealing and other advanced or nefarious shell code, rootkits, worms or virus-related activity.'.t() ],
            ["3", 'Web Attacks'.t(), 'IP addresses using cross-site scripting, iFrame injection, SQL injection, cross-domain injection, domain password brute force attacks to target vulnerabilities on a web server, and malicious automated web activity.'.t()],
            ["4", 'Botnets'.t(), 'IP addresses acting as botnet Command and Control (C&C) centers, infected zombie machines controlled by the C&C servers, botnet hosts distributing malware, cobalt beacons, suspicious fast flux activity, and ransomware reported C&C.'.t() ],
            ["5", 'Scanners'.t(), 'IP addresses involved in unauthorized reconnaissance activities such as probing, host scanning, port scanning, and brute force login attempts, as well as repetitive and suspicious programmatic behavior observed in regular time intervals.'.t() ],
            ["6", 'Denial of Service'.t(), 'Scanners category includes all reconnaissance such as probes, host scan, domain scan and password brute force attack.'.t() ],
            ["7", 'Reputation'.t(), 'IP addresses with a low Reputation Index score. IP addresses in this category have a higher than average risk of hosting and/or distributing malicious content or files.'.t() ],
            ["8", 'Phishing'.t(), 'IP addresses that are hosting phishing sites or initiating suspicious entrapment activities through malicious ads, web email POST forms, and click fraud.'.t() ],
            ["9", 'Proxy'.t(), 'IP addresses offering anonymization services, as well as those known to have been associated with open web proxy, VPN, or suspicious proxy chains.'.t() ],
            ["12", 'Mobile Threats'.t(), 'IP addresses associated with malicious mobile application distribution or with attacks initiating from a mobile device.'.t() ],
            ["14", 'Tor Proxy'.t(), 'IP addresses associated with Tor Proxy activity such as exit nodes or suspicious IPs associated with Tor activity.'.t() ],
            ['17', 'Keyloggers'.t(), "Downloads and discussion of software agents that track a user's keystrokes or monitor their web surfing habits.".t()],
            ['18', 'Malware'.t(), "Malicious content including executables, drive-by infection sites, malicious scripts, viruses, trojans, and code.".t()],
            ['19', 'Spyware'.t(), "Spyware or Adware sites that provide or promote information gathering or tracking that is unknown to, or without the explicit consent of, the end user or the organization, also unsolicited advertising popups and programs that may be installed on a user's computer.".t()]
        ]
    }),

    threatsRenderer: function(value, meta, record){
        return record.get('description');
    },

    threatsConditionValues: function(){
        values = [];
        Ung.common.threatprevention.references.threats.each(function(record){
            if(record.get('bit') != 0){
                values.push([
                    record.get('bit'),
                    record.get('description'),
                    record.get('details')
                ]);
            }
        });
        return values;
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
});
