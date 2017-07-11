Ext.define('Ung.util.Converter', {
    singleton: true,
    alternateClassName: 'Converter',

    mapValueFormat: '{0} [{1}]'.t(),

    countryMap: {
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
    country: function( value ) {
        if(Ext.isEmpty(value)) {
            return '';
        }
        return Ext.String.format(
                Converter.mapValueFormat,
                ( value in Converter.countryMap ) ? Converter.countryMap[value] : Converter.countryMap['default'],
                value
        );
    },

    httpReasonMap: {
        D: 'in Categories Block list'.t(),
        U: 'in Site Block list'.t(),
        E: 'in File Block list'.t(),
        M: 'in MIME Types Block list'.t(),
        H: 'hostname is an IP address'.t(),
        I: 'in Site Pass list'.t(),
        R: 'referer in Site Pass list'.t(),
        C: 'in Clients Pass list'.t(),
        B: 'in Unblocked list'.t(),
        F: 'in Rules list'.t(),
        N: 'no rule applied'.t(),
        default: 'no rule applied'.t()
    },
    httpReason: function( value ) {
        if(Ext.isEmpty(value)) {
            return '';
        }
        return Ext.String.format(
                Converter.mapValueFormat,
                ( value in Converter.httpReasonMap ) ? Converter.httpReasonMap[value] : Converter.httpReasonMap['default'],
                value
        );
    },

    emailActionMap: {
        P: 'pass message'.t(),
        M: 'mark message'.t(),
        D: 'drop message'.t(),
        B: 'block message'.t(),
        Q: 'quarantine message'.t(),
        S: 'pass safelist message'.t(),
        Z: 'pass oversize message'.t(),
        O: 'pass outbound message'.t(),
        F: 'block message (scan failure)'.t(),
        G: 'pass message (scan failure)'.t(),
        Y: 'block message (greylist)'.t(),
        default:  'unknown action'.t()
    },
    emailAction: function(value){
        if(Ext.isEmpty(value)) {
            return '';
        }
        return Ext.String.format(
                Converter.mapValueFormat,
                ( value in Converter.emailActionMap ) ? Converter.emailActionMap[value] : Converter.emailActionMap['default'],
                value
        );
    },

    protocolMap: {
        0: 'HOPOPT',
        1: 'ICMP',
        2: 'IGMP',
        3: 'GGP',
        4: 'IP-in-IP',
        5: 'ST',
        6: 'TCP',
        7: 'CBT',
        8: 'EGP',
        9: 'IGP',
        10: 'BBN-RCC-MON',
        11: 'NVP-II',
        12: 'PUP',
        13: 'ARGUS',
        14: 'EMCON',
        15: 'XNET',
        16: 'CHAOS',
        17: 'UDP',
        18: 'MUX',
        19: 'DCN-MEAS',
        20: 'HMP',
        21: 'PRM',
        22: 'XNS-IDP',
        23: 'TRUNK-1',
        24: 'TRUNK-2',
        25: 'LEAF-1',
        26: 'LEAF-2',
        27: 'RDP',
        28: 'IRTP',
        29: 'ISO-TP4',
        30: 'NETBLT',
        31: 'MFE-NSP',
        32: 'MERIT-INP',
        33: 'DCCP',
        34: '3PC',
        35: 'IDPR',
        36: 'XTP',
        37: 'DDP',
        38: 'IDPR-CMTP',
        39: 'TP++',
        40: 'IL',
        41: 'IPv6',
        42: 'SDRP',
        43: 'IPv6-Route',
        44: 'IPv6-Frag',
        45: 'IDRP',
        46: 'RSVP',
        47: 'GRE',
        48: 'MHRP',
        49: 'BNA',
        50: 'ESP',
        51: 'AH',
        52: 'I-NLSP',
        53: 'SWIPE',
        54: 'NARP',
        55: 'MOBILE',
        56: 'TLSP',
        57: 'SKIP',
        58: 'IPv6-ICMP',
        59: 'IPv6-NoNxt',
        60: 'IPv6-Opts',
        62: 'CFTP',
        64: 'SAT-EXPAK',
        65: 'KRYPTOLAN',
        66: 'RVD',
        67: 'IPPC',
        69: 'SAT-MON',
        70: 'VISA',
        71: 'IPCU',
        72: 'CPNX',
        73: 'CPHB',
        74: 'WSN',
        75: 'PVP',
        76: 'BR-SAT-MON',
        77: 'SUN-ND',
        78: 'WB-MON',
        79: 'WB-EXPAK',
        80: 'ISO-IP',
        81: 'VMTP',
        82: 'SECURE-VMTP',
        83: 'VINES',
        84: 'TTP',
        85: 'NSFNET-IGP',
        86: 'DGP',
        87: 'TCF',
        88: 'EIGRP',
        89: 'OSPF',
        90: 'Sprite-RPC',
        91: 'LARP',
        92: 'MTP',
        93: 'AX.25',
        94: 'IPIP',
        95: 'MICP',
        96: 'SCC-SP',
        97: 'ETHERIP',
        98: 'ENCAP',
        100: 'GMTP',
        101: 'IFMP',
        102: 'PNNI',
        103: 'PIM',
        104: 'ARIS',
        105: 'SCPS',
        106: 'QNX',
        107: 'A/N',
        108: 'IPComp',
        109: 'SNP',
        110: 'Compaq-Peer',
        111: 'IPX-in-IP',
        112: 'VRRP',
        113: 'PGM',
        115: 'L2TP',
        116: 'DDX',
        117: 'IATP',
        118: 'STP',
        119: 'SRP',
        120: 'UTI',
        121: 'SMP',
        122: 'SM',
        123: 'PTP',
        124: 'IS-IS',
        125: 'FIRE',
        126: 'CRTP',
        127: 'CRUDP',
        128: 'SSCOPMCE',
        129: 'IPLT',
        130: 'SPS',
        131: 'PIPE',
        132: 'SCTP',
        133: 'FC',
        134: 'RSVP-E2E-IGNORE',
        135: 'Mobility',
        136: 'UDPLite',
        137: 'MPLS-in-IP',
        138: 'manet',
        139: 'HIP',
        140: 'Shim6',
        141: 'WESP',
        142: 'ROHC',
        default: 'Unknown'.t()
    },
    protocol: function( value ){
        return Ext.String.format(
                Converter.mapValueFormat,
                ( value in Converter.protocolMap ) ? Converter.protocolMap[value] : Converter.protocolMap['default'],
                value
        );
    },

    icmpMap: {
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
        default: 'Unassigned'.t(),
        253: 'RFC3692-style Experiment 1'.t(),
        254: 'RFC3692-style Experiment 2'.t(),
        255: 'Reserved'.t()
    },
    icmp: function( value ){
        if(Ext.isEmpty(value)) {
            return '';
        }
        return Ext.String.format(
                Converter.mapValueFormat,
                ( value in Converter.icmpMap ) ? Converter.icmpMap[value] : Converter.icmpMap['default'],
                value
        );
    },

    interface: function (value) {
        if (!this.interfaceMap) {
            // this.buildInterfaceMap();
            var interfacesList = [], i;
            try {
                interfacesList = rpc.reportsManager.getInterfacesInfo().list;
            } catch (ex) {
                console.log(ex);
            }

            this.interfaceMap = {};
            for (i = 0; i < interfacesList.length; i += 1) {
                this.interfaceMap[interfacesList[i].interfaceId] = Ext.String.format(
                    Converter.mapValueFormat,
                    interfacesList[i].name,
                    interfacesList[i].interfaceId);
            }
        }
        return ( value && value != -1 ) ? this.interfaceMap[value] || value.toString() : '';
    },

    policy: function ( value ) {
        if (Ext.getStore('policiestree')) { // existing in ADMIN servlet only
            var policy = Ext.getStore('policiestree').findRecord('policyId', value);
            return policy ? policy.get('name') + ' [' + value + ']' : '';
        } else {
            return '';
        }
    },

    loginSuccess: function( value ){
        return value ?  'success'.t() : 'failed'.t();
    },

    loginFrom: function( value ){
        return value ?  'local'.t() : 'remote'.t();
    },

    loginFailureReasonMap : {
        U:'invalid username'.t(),
        P: 'invalid password'.t(),
        default: ''

    },
    loginFailureReason: function( value ){
        if(Ext.isEmpty(value)) {
            return '';
        }
        return ( value in Converter.loginFailureReasonMap ) ? Converter.loginFailureReasonMap[value] : Converter.loginFailureReasonMap['default'];
    },

    quotaActionMap: {
        1: 'Given'.t(),
        2: 'Exceeded'.t(),
        default: 'Unknown'.t()
    },
    quotaAction: function( value ){
        if(Ext.isEmpty(value)) {
            return '';
        }
        return Ext.String.format(
                Converter.mapValueFormat,
                ( value in Converter.quotaActionMap ) ? Converter.quotaActionMap[value] : Converter.quotaActionMap['default'],
                value
        );
    },

    captivePortalEventInfoMap: {
        LOGIN: 'Login Success'.t(),
        FAILED: 'Login Failure'.t(),
        TIMEOUT: 'Session Timeout'.t(),
        INACTIVE: 'Idle Timeout'.t(),
        USER_LOGOUT: 'User Logout'.t(),
        ADMIN_LOGOUT: 'Admin Logout'.t(),
        default: 'Unknown'.t()
    },
    captivePortalEventInfo: function( value ){
        if(Ext.isEmpty(value)) {
            return '';
        }
        return ( value in Converter.captivePortalEventInfoMap ) ? Converter.captivePortalEventInfoMap[value] : Converter.captivePortalEventInfoMap['default'];
    },

    authTypeMap: {
        NONE: 'None'.t(),
        LOCAL_DIRECTORY: 'Local Directory'.t(),
        ACTIVE_DIRECTORY: 'Active Directory'.t(),
        RADIUS: 'RADIUS'.t(),
        CUSTOM: 'Custom'.t(),
        default: 'Unknown'.t()
    },
    authType: function( value ){
        if(Ext.isEmpty(value)) {
            return '';
        }
        return ( value in Converter.authTypeMap ) ? Converter.authTypeMap[value] : Converter.authTypeMap['default'];
    },

    directoryConnectorActionMap: {
        I: 'login'.t(),
        U: 'update'.t(),
        O: 'logout'.t(),
        default: 'unknown'.t()

    },
    directoryConnectorAction: function( value ){
        if(Ext.isEmpty(value)) {
            return '';
        }
        return ( value in Converter.directoryConnectorActionMap ) ? Converter.directoryConnectorActionMap[value] : Converter.directoryConnectorActionMap['default'];
    },

    bandwidthControlRule: function( value ){
        return Ext.isEmpty(value) ? 'none'.t() : value;
    },

    adBlockerActionMap:{
        B: 'block'.t(),
        default: 'pass'.t()
    },
    adBlockerAction: function( value ){
        if(Ext.isEmpty(value)) {
            return '';
        }
        return ( value in Converter.adBlockerActionMap ) ? Converter.adBlockerActionMap[value] : Converter.adBlockerActionMap['default'];
    },

    configurationBackupSuccessMap:{
        true: 'success'.t(),
        default: 'failed'.t()
    },
    configurationBackupSuccess: function( value ){
        return ( value in Converter.configurationBackupSuccessMap ) ? Converter.configurationBackupSuccessMap[value] : Converter.configurationBackupSuccessMap['default'];
    },

    priorityMap: {
        0: '',
        1: 'Very High'.t(),
        2: 'High'.t(),
        3: 'Medium'.t(),
        4: 'Low'.t(),
        5: 'Limited'.t(),
        6: 'Limited More'.t(),
        7: 'Limited Severely'.t()
    },
    priority: function( value ){
        if (Ext.isEmpty(value)) {
            value = 0;
        }
        return ( value in Converter.priorityMap ) ? Converter.priorityMap[value] : Converter.priorityMap['default'];
    },

});

Ext.define('Ung.util.Metrics', {
    alternateClassName: 'Metrics',
    singleton: true,
    frequency: 10000,
    interval: null,
    running: false,

    start: function () {
        var me = this;
        Ext.getStore('stats').loadRawData({});
        Ext.getStore('metrics').loadData({});
        me.stop();
        me.run();
        me.interval = window.setInterval(function () {
            me.run();
        }, me.frequency);
    },

    stop: function () {
        if (this.interval !== null) {
            window.clearInterval(this.interval);
        }
    },

    run: function () {
        var data = [];
        rpc.metricManager.getMetricsAndStats(Ext.bind(function(result, ex) {
            if (ex) { Util.handleException(ex); return; }

            data = [];

            Ext.getStore('stats').loadRawData(result.systemStats);

            for (var appId in result.metrics) {
                if (result.metrics.hasOwnProperty(appId)) {
                    data.push({
                        appId: appId,
                        metrics: result.metrics[appId]
                    });
                }
            }

            Ext.getStore('metrics').loadData(data);
        }));
    }

});

Ext.define('Ung.util.Renderer', {
    singleton: true,
    alternateClassName: 'Renderer',

    /*
     * Common column widths
     */
    // Action
    actionWidth: 80,
    // Boolean
    booleanWidth: 40,
    // Counter
    counterWidth: 80,
    // Email address
    emailWidth: 150,
    // Hostname
    hostnameWidth: 120,
    // Numeric identifier
    idWidth: 75,
    // IP Address
    ipWidth: 100,
    // Load measurement
    loadWidth: 50,
    // Latitude/longtitude
    locationWidth: 50,
    // MAC address
    macWidth: 110,
    // General purpose
    messageWidth: 120,
    // Port
    portWidth: 70,
    // Data size
    sizeWidth: 90,
    // Tags
    tagsWidth: 200,
    // Timestamp
    timestampWidth: 135,
    // URI
    uriWidth: 200,
    // Username
    usernameWidth: 120,

    /*
     * Grid filters
     */
    booleanFilter: {
        type: 'boolean',
        yesText: 'true'.t(),
        noText: 'false'.t()
    },

    numericFilter: {
        type: 'numeric'
    },

    stringFilter: {
        type: 'string'
    },

    timestampFilter: {
        type: 'date'
    },

    boolean: function( value ){
        return ( value == true ) ? 'true' : 'false';
    },

    timestampOffset: (new Date().getTimezoneOffset() * 60000) + rpc.timeZoneOffset,
    timestamp: function( value ){
        if( !value ){
            return null;
        }
        if( ( typeof( value ) == 'object' ) &&
            value.time ){
            value = value.time;
        }
        var date = new Date( value );
        date.setTime( value + this.timestampOffset);
        return Ext.util.Format.date( date, 'timestamp_fmt'.t());
    },

    interface: function (value) {
        if (!Ung.util.Renderer.interfaceMap) {
            // this.buildInterfaceMap();
            var interfacesList = [], i;
            try {
                interfacesList = rpc.reportsManager.getInterfacesInfo().list;
            } catch (ex) {
                console.log(ex);
            }

            Ung.util.Renderer.interfaceMap = {};
            for (i = 0; i < interfacesList.length; i += 1) {
                Ung.util.Renderer.interfaceMap[interfacesList[i].interfaceId] = interfacesList[i].name + " [" + interfacesList[i].interfaceId + "]";
            }
        }
        return value ? Ung.util.Renderer.interfaceMap[value] || value.toString() : '';
    },

    tags: function (value, metaData) {
        if( value != null &&
            value != "" ){
            if( typeof(value) == 'string' ){
                value = Ext.decode( value );
            }
            if( value && value.list ) {
                var str = [], tip = [];
                Ext.Array.each(value.list, function (tag) {
                    str.push('<div class="tag-item">' + tag.name + '</div>');
                    if ( tag.expirationTime == 0 )
                        tip.push('<strong>' + tag.name + '</strong> - ' + 'Never'.t());
                    else
                        tip.push('<strong>' + tag.name + '</strong> - ' + Ext.Date.format(new Date(tag.expirationTime), 'timestamp_fmt'.t()));
                });
                if (metaData) {
                    metaData.tdAttr = 'data-qtip="' + tip.join('<br/>') + '"';
                    metaData.tdCls = 'tag-cell';
                }
                return '<div class="tagpicker">' + str.join('') + '</div>';
            }
        }
        return '';
    },

    // policy: function ( value ) {
    //     var policy = Ext.getStore('policiestree').findRecord('policyId', value);
    //     if (policy) {
    //         return policy.get('name') + ' [' + value + ']';
    //     }else{
    //         return 'None'.t();
    //     }
    // },

    datasizeMap: [
        [ 1125899906842624, 'PB'.t() ],
        [ 1099511627776, 'TB'.t() ],
        [ 1073741824, 'GB'.t() ],
        [ 1048576, 'MB'.t() ] ,
        [ 1024, 'KB'.t() ],
        [ 1, 'B'.t() ]
    ],
    datasize: function( value ){
        // walk map looking at key.  If larger then divide and use units
        value = parseInt( value, 10 );
        var size;
        for( var i = 0; i < Ung.util.Renderer.datasizeMap.length; i++){
            size = Ung.util.Renderer.datasizeMap[i];
            if( value >= size[0] ){
                break;
            }
        }
        if( ( value == 0 ) ||
            ( size[0] == 1 ) ){
            return value + ' ' + size[1];
        }else{
            return ( value / size[0] ).toFixed(2) + ' ' + size[1];
        }
    },

    datasizeoptional: function(value){
        if( value === 0 || value === '' ){
            return '';
        }
        return Renderer.datasize(value);
    },

    timeIntervalMap: {
        86400: 'Daily'.t(),
        604800: 'Weekly'.t(),
        1: 'Week to Date'.t(),
        2419200: 'Monthly'.t(),
        2: 'Month to Date'.t()
    },
    timeInterval: function ( value ){
        if( value in Ung.util.Renderer.timeIntervalMap ){
            return Ung.util.Renderer.timeIntervalMap[value];
        }
        return value;
    },

    dayOfWeekMap: {
        0: 'Sunday'.t(),
        1: 'Monday'.t(),
        2: 'Tuesday'.t(),
        3: 'Wednesday'.t(),
        4: 'Thursday'.t(),
        5: 'Friday'.t(),
        6: 'Saturday'.t()
    },
    dayOfWeek: function( value ){
        if( value in Ung.util.Renderer.dayOfWeeklMap ){
            return Ung.util.Renderer.dayOfWeekMap[value];
        }
        return value;
    },

    priorityMap: {
        0: '',
        1: 'Very High'.t(),
        2: 'High'.t(),
        3: 'Medium'.t(),
        4: 'Low'.t(),
        5: 'Limited'.t(),
        6: 'Limited More'.t(),
        7: 'Limited Severely'.t()
    },
    priority: function( value ){
        if (Ext.isEmpty(value)) {
            value = 0;
        }
        if( value in Ung.util.Renderer.priorityMap ){
            return Ung.util.Renderer.priorityMap[value];
        }
        return Ext.String.format('Unknown Priority: {0}'.t(), value);
    },

    mark: function( value ){
        if (value){
            return "0x" + value.toString(16);
        }
        return '';
    },

    protocolsMap: {
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
        142: 'ROHC [142]'
    },
    protocol: function (value) {
        return value ? Ung.util.Renderer.protocolsMap[value] || value.toString() : '';
    },

    settingsFile: function( value ){
        value = value.replace( /^.*\/settings\//, '' );
        value = value.replace( /^.*\/conf\//, '' );
        return value;
    },


});

/**
 * rpc connectivity brain
 */
Ext.define('Ung.util.Rpc', {
    alternateClassName: 'Rpc',
    singleton: true,

    evalExp: function(ns, context, expression) {
        if ( ns == null || context == null || expression == null ) {
            console.error('Error: Invalid RPC expression: \'' + expression + '\'.');
            Util.handleException('Invalid RPC expression: \'' + expression + '\'.');
            return null;
        }

        var lastPart = null;
        var len = ns.length;

        for (var i = 0; i < len ; i++) {
            if (context == null )
                break;
            var part = ns[i];
            context = context[part];
            lastPart = part;
        }
        if (context == null ) {
            console.error('Error: Invalid RPC expression: \'' + expression + '\'. Attribute \'' + lastPart + '\' is null');
            Util.handleException('Invalid RPC expression: \'' + expression + '\'. Attribute \'' + lastPart + '\' is null');
            return null;
        }

        return context;
    },

    asyncData: function(expression /*, args */) {
        var args = [].slice.call(arguments).splice(1),
            ns = expression.split('.'),
            method = ns.pop(),
            context = window,
            dfrd = new Ext.Deferred();

        context = Ung.util.Rpc.evalExp(ns, context, expression);
        if (!context) return null;

        if (!context.hasOwnProperty(method) || !Ext.isFunction(context[method])) {
            console.error('Error: No such RPC method: \'' + expression + '\'');
            Util.handleException('No such RPC method: \'' + expression + '\'');
            return null;
        }

        args.unshift(function (result, ex) {
            if (ex) {
                console.error('Error: ' + ex);
                Util.handleException(ex);
                dfrd.reject(ex);
            }
            // console.info(expression + ' (async data) ... OK');
            dfrd.resolve(result);
        });

        context[method].apply(null, args);
        return dfrd.promise;
    },

    directData: function(expression /*, args */) {
        var ns = expression.split('.'),
            method = ns.pop(),
            context = window,
            lastPart = null;

        context = Ung.util.Rpc.evalExp(ns, context, expression);
        if (!context) return null;

        if (!context.hasOwnProperty(method) || !Ext.isFunction(context[method])) {
            console.error('Error: No such RPC method: \'' + expression + '\' on attribute \'' + lastPart + '\'');
            Util.handleException('No such RPC method: \'' + expression + '\' on attribute \'' + lastPart + '\'');
            return null;
        }

        try {
            return context[method].call();
        } catch (ex) {
            Util.handleException(ex);
        }
        return null;
    },

    asyncPromise: function(expression /*, args */) {
        var args = [].slice.call(arguments).splice(1),
            ns = expression.split('.'),
            method = ns.pop(),
            context = window;

        context = Ung.util.Rpc.evalExp(ns, context, expression);
        if (!context) return null;

        if (!context.hasOwnProperty(method) || !Ext.isFunction(context[method])) {
            console.error('Error: No such RPC method: \'' + expression + '\'');
            Util.handleException('No such RPC method: \'' + expression + '\'');
            return null;
        }

        return function() {
            var dfrd = new Ext.Deferred();
            args.unshift(function (result, ex) {
                if (ex) { dfrd.reject(ex); }
                // console.info(expression + ' (async promise) ... OK');
                dfrd.resolve(result);
            });
            context[method].apply(null, args);
            return dfrd.promise;
        };
    },

    directPromise: function(expression /*, args */) {
        var ns = expression.split('.'),
            method = ns.pop(),
            context = window;

        context = Ung.util.Rpc.evalExp(ns, context, expression);
        if (!context) return null;

        if (!context.hasOwnProperty(method) || !Ext.isFunction(context[method])) {
            console.error('Error: No such RPC method: \'' + expression + '\'');
            Util.handleException('No such RPC method: \'' + expression + '\'');
            return null;
        }

        return function() {
            var dfrd = new Ext.Deferred();
            try {
                // console.info(expression + ' (direct promise) ... OK');
                dfrd.resolve(context[method].call());
            } catch (ex) {
                dfrd.reject(ex);
            }
            return dfrd.promise;
        };
    },

});

Ext.define('TableConfig', {
    alternateClassName: 'TableConfig',
    singleton: true,

    getConfig: function(tableName) {
        if(TableConfig.validated == false){
            TableConfig.validate();
        }
        return TableConfig.tableConfig[tableName];
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

    validated: false,
    validate: function(){
        for(var table in TableConfig.tableConfig){
            if(table == 'syslog'){
                continue;
            }
            TableConfig.tableConfig[table].fields.forEach( function( field ){
                // if(!field.type &&
                //     ( !field.sortType ||
                //       field.sortType != 'asTimestamp' ) ){
                //     console.log(table + ": field=" + field.name + ", missing type" );
                // }
            });
            TableConfig.tableConfig[table].columns.forEach( function( column ){
                if(column.width === undefined){
                    console.log(table + ":" + column.header + ", no width");
                }
                if(!column.filter &&
                    ( !column.xtype || column.xtype != "actioncolumn") ){
                    console.log(table + ": column=" + column.header + ", no filter");
                }
            });
        }
        TableConfig.validated = true;
    },

    // end new methods

    tableConfig: {
        sessions: {
            fields: [{
                name: 'session_id',
            }, {
                name: 'time_stamp',
                sortType: 'asTimestamp'
            }, {
                name: 'end_time',
                sortType: 'asTimestamp'
            }, {
                name: 'bypassed',
                type: 'boolean'
            }, {
                name: 'entitled',
                type: 'boolean'
            }, {
                name: 'protocol',
                convert: Converter.protocol
            }, {
                name: 'icmp_type',
                convert: Converter.icmp
            }, {
                name: 'hostname',
                type: 'string'
            }, {
                name: 'username',
                type: 'string',
                sortType: 'asUnString'
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
                name: 'filter_prefix',
                type: 'string',
                sortType: 'asUnString'
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
                type: 'string',
                sortType: 'asUnString'
            }, {
                name: 'captive_portal_rule_index'
            }, {
                name: 'captive_portal_blocked'
            }, {
                name: 'application_control_application',
                type: 'string',
                sortType: 'asUnString'
            }, {
                name: 'application_control_protochain',
                type: 'string',
                sortType: 'asUnString'
            }, {
                name: 'application_control_category',
                type: 'string',
                sortType: 'asUnString'
            }, {
                name: 'application_control_flagged'
            }, {
                name: 'application_control_blocked'
            }, {
                name: 'application_control_confidence'
            }, {
                name: 'application_control_detail',
                type: 'string',
                sortType: 'asUnString'
            }, {
                name: 'application_control_ruleid'
            }, {
                name: 'bandwidth_control_priority'
            }, {
                name: 'bandwidth_control_rule',
                convert: Converter.bandwidthControlRule
            }, {
                name: 'ssl_inspector_status',
                type: 'string',
                sortType: 'asUnString'
            }, {
                name: 'ssl_inspector_detail',
                type: 'string',
                sortType: 'asUnString'
            }, {
                name: 'ssl_inspector_ruleid'
            }],
            columns: [{
                header: 'Session Id'.t(),
                dataIndex: 'session_id',
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.numericFilter
            }, {
                header: 'Timestamp'.t(),
                dataIndex: 'time_stamp',
                width: Renderer.timestampWidth,
                sortable: true,
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            }, {
                header: 'End Timestamp'.t(),
                dataIndex: 'end_time',
                width: Renderer.timestampWidth,
                sortable: true,
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            }, {
                header: 'Bypassed'.t(),
                dataIndex: 'bypassed',
                width: Renderer.booleanWidth,
                sortable: true,
                rtype: 'boolean',
                filter: Renderer.booleanFilter
            }, {
                header: 'Entitled'.t(),
                dataIndex: 'entitled',
                width: Renderer.booleanWidth,
                sortable: true,
                rtype: 'boolean',
                filter: Renderer.booleanFilter,
            }, {
                header: 'Protocol'.t(),
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'protocol'
            }, {
                header: 'ICMP Type'.t(),
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'icmp_type'
            }, {
                header: 'Policy Id'.t(),
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'policy_id'
            }, {
                header: 'Policy Rule Id'.t(),
                width: Renderer.idWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'policy_rule_id'
            }, {
                header: 'Client Interface'.t() ,
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'client_intf'
            }, {
                header: 'Server Interface'.t() ,
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'server_intf'
            }, {
                header: 'Client Country'.t() ,
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'client_country'
            }, {
                header: 'Client Latitude'.t() ,
                width: Renderer.locationWidth,
                sortable: true,
                dataIndex: 'client_latitude',
                filter: Renderer.numericFilter
            }, {
                header: 'Client Longitude'.t() ,
                width: Renderer.locationWidth,
                sortable: true,
                dataIndex: 'client_longitude',
                filter: Renderer.numericFilter
            }, {
                header: 'Server Country'.t() ,
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'server_country'
            }, {
                header: 'Server Latitude'.t() ,
                width: Renderer.locationWidth,
                sortable: true,
                dataIndex: 'server_latitude',
                filter: Renderer.numericFilter
            }, {
                header: 'Server Longitude'.t() ,
                width: Renderer.locationWidth,
                sortable: true,
                dataIndex: 'server_longitude',
                filter: Renderer.numericFilter
            }, {
                header: 'Username'.t(),
                width: Renderer.usernameWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'username'
            }, {
                header: 'Hostname'.t(),
                width: Renderer.hostnameWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'hostname'
            }, {
                header: 'Client'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                filter: Renderer.stringFilter,
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
                filter: Renderer.stringFilter,
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
                filter: Renderer.stringFilter,
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
                filter: Renderer.stringFilter,
                dataIndex: 's_server_addr'
            }, {
                header: 'Server Port'.t(),
                width: Renderer.portWidth,
                // Ugh.  Don't like this...
                flex: 1,
                sortable: true,
                dataIndex: 's_server_port',
                filter: Renderer.numericFilter
            }, {
                header: 'Tags'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'tags'
            }, {
                header: 'Filter Prefix'.t(),
                width: Renderer.messageWidth,
                flex: 1,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'filter_prefix'
            }, {
                header: 'Priority'.t() + ' (Bandwidth Control)',
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'bandwidth_control_priority',
                rtype: 'priority'
            }, {
                header: 'Rule'.t() + ' (Bandwidth Control)',
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                flex: 1,
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
                filter: Renderer.stringFilter,
                dataIndex: 'application_control_application'
            }, {
                header: 'ProtoChain'.t() + ' (Application Control)',
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'application_control_protochain'
            }, {
                header: 'Category'.t() + ' (Application Control)',
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
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
                filter: Renderer.stringFilter,
                flex: 1,
                dataIndex: 'application_control_detail'
            },{
                header: 'Protocol'.t() + ' (Application Control Lite)',
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'application_control_lite_protocol',
                rtype: 'protocol'
            }, {
                header: 'Blocked'.t() + ' (Application Control Lite)',
                width: Renderer.booleanWidth,
                sortable: true,
                dataIndex: 'application_control_lite_blocked',
                flex: 1,
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
                filter: Renderer.stringFilter,
                dataIndex: 'ssl_inspector_status'
            }, {
                header: 'Detail'.t() + ' (SSL Inspector)',
                width: Renderer.messageWidth,
                flex: 1,
                sortable: true,
                filter: Renderer.stringFilter,
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
                filter: Renderer.stringFilter,
                flex: 1,
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
                width: Renderer.idWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'session_id'
            }, {
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            }, {
                header: 'Start Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'start_time',
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            }, {
                header: 'End Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'end_time',
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
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
                filter: Renderer.stringFilter,
                dataIndex: 'protocol',
            }, {
                header: 'ICMP Type'.t(),
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'icmp_type'
            }, {
                header: 'Policy Id'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'policy_id'
            }, {
                header: 'Policy Rule Id'.t(),
                width: Renderer.idWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'policy_rule_id'
            }, {
                header: 'Client Interface'.t() ,
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'client_intf'
            }, {
                header: 'Server Interface'.t() ,
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'server_intf'
            }, {
                header: 'Client Country'.t() ,
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'client_country',
            }, {
                header: 'Client Latitude'.t() ,
                width: Renderer.locationWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'client_latitude'
            }, {
                header: 'Client Longitude'.t() ,
                width: Renderer.locationWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'client_longitude'
            }, {
                header: 'Server Country'.t() ,
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'server_country',
            }, {
                header: 'Server Latitude'.t() ,
                width: Renderer.locationWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'server_latitude'
            }, {
                header: 'Server Longitude'.t() ,
                width: Renderer.locationWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'server_longitude'
            }, {
                header: 'Username'.t(),
                width: Renderer.usernameWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'username'
            }, {
                header: 'Hostname'.t(),
                width: Renderer.hostnameWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'hostname'
            }, {
                header: 'Client'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                filter: Renderer.stringFilter,
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
                filter: Renderer.stringFilter,
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
                filter: Renderer.stringFilter,
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
                filter: Renderer.stringFilter,
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
                filter: Renderer.stringFilter,
                dataIndex: 'tags'
            }, {
                header: 'Filter Prefix'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
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
                filter: Renderer.stringFilter,
                dataIndex: 'bandwidth_control_priority',
                rtype: 'priority'
            }, {
                header: 'Rule'.t() + ' (Bandwidth Control)',
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'bandwidth_control_rule'
            }, {
                header: 'Application'.t() + ' (Application Control)',
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'application_control_application'
            }, {
                header: 'ProtoChain'.t() + ' (Application Control)',
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'application_control_protochain'
            }, {
                header: 'Category'.t() + ' (Application Control)',
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
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
                filter: Renderer.stringFilter,
                dataIndex: 'application_control_detail'
            },{
                header: 'Protocol'.t() + ' (Application Control Lite)',
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.stringFilter,
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
                filter: Renderer.stringFilter,
                dataIndex: 'ssl_inspector_status'
            }, {
                header: 'Detail'.t() + ' (SSL Inspector)',
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
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
                filter: Renderer.stringFilter,
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
                width: Renderer.idWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'request_id'
            }, {
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            }, {
                header: 'Policy Id'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'policy_id'
            }, {
                header: 'Session Id'.t(),
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'session_id'
            }, {
                header: 'Client Interface'.t() ,
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'client_intf'
            }, {
                header: 'Server Interface'.t() ,
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'server_intf'
            }, {
                header: 'Client'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'c_client_addr'
            }, {
                header: 'Client Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'c_client_port'
            }, {
                header: 'New Client'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 's_client_addr'
            }, {
                header: 'New Client Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 's_client_port'
            }, {
                header: 'Original Server'.t() ,
                width: Renderer.ipWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'c_server_addr'
            }, {
                header: 'Original Server Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'c_server_port'
            }, {
                header: 'Server'.t() ,
                width: Renderer.ipWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 's_server_addr'
            }, {
                header: 'Server Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 's_server_port'
            }, {
                header: 'Username'.t(),
                width: Renderer.usernameWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'username'
            }, {
                header: 'Hostname'.t(),
                width: Renderer.hostnameWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'hostname'
            }, {
                header: 'Domain'.t(),
                width: Renderer.hostnameWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'domain'
            }, {
                header: 'Host'.t(),
                width: Renderer.hostnameWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'host'
            }, {
                header: 'Uri'.t(),
                flex:1,
                width: Renderer.uriWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'uri'
            }, {
                header: 'Method'.t(),
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.stringFilter,
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
                filter: Renderer.stringFilter,
                dataIndex: 'referer'
            }, {
                header: 'Download Content Length'.t(),
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 's2c_content_length'
            }, {
                header: 'Upload Content Length'.t(),
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'c2s_content_length'
            }, {
                header: 'Content Type'.t(),
                width: Renderer.messageWidth,
                flex: 1,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 's2c_content_type'
            }, {
                header: 'Blocked'.t() + ' (Web Filter)',
                width: Renderer.booleanWidth,
                sortable: true,
                dataIndex: 'web_filter_blocked',
                filter: Renderer.booleanFilter
            }, {
                header: 'Flagged'.t() + ' (Web Filter)',
                width: Renderer.booleanWidth,
                sortable: true,
                dataIndex: 'web_filter_flagged',
                filter: Renderer.booleanFilter
            }, {
                header: 'Reason For Action'.t() +  ' (Web Filter)',
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'web_filter_reason'
            }, {
                header: 'Web Category'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'web_filter_category'
            }, {
                header: 'Action'.t() + ' (Ad Blocker)',
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'ad_blocker_action'
            }, {
                header: 'Blocked Cookie'.t() + ' (Ad Blocker)',
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'ad_blocker_cookie_ident'
            }, {
                header: 'Clean'.t() + ' (Virus Blocker Lite)',
                width: Renderer.booleanWidth,
                sortable: true,
                dataIndex: 'virus_blocker_lite_clean',
                filter: Renderer.booleanFilter
            }, {
                header: 'Virus Name'.t() + ' (Virus Blocker Lite)',
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'virus_blocker_lite_name'
            }, {
                header: 'Clean'.t() + ' (Virus Blocker)',
                width: Renderer.booleanWidth,
                sortable: true,
                dataIndex: 'virus_blocker_clean',
                filter: Renderer.booleanFilter
            }, {
                header: 'Virus Name'.t() + ' (Virus Blocker)',
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
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
                width: Renderer.idWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'event_id'
            }, {
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            }, {
                header: 'Policy Id'.t(),
                width: Renderer.idWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'policy_id'
            }, {
                header: 'Request Id'.t(),
                width: Renderer.idWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'request_id'
            }, {
                header: 'Session Id'.t(),
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'session_id'
            }, {
                header: 'Client Interface'.t() ,
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'client_intf'
            }, {
                header: 'Server Interface'.t() ,
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'server_intf'
            }, {
                header: 'Username'.t(),
                width: Renderer.usernameWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'username'
            }, {
                header: 'Hostname'.t(),
                width: Renderer.hostnameWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'hostname'
            }, {
                header: 'Client'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'c_client_addr'
            }, {
                header: 'Client Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'c_client_port'
            }, {
                header: 'New Client'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 's_client_addr'
            }, {
                header: 'New Client Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 's_client_port'
            }, {
                header: 'Original Server'.t() ,
                width: Renderer.ipWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'c_server_addr'
            }, {
                header: 'Original Server Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'c_server_port'
            }, {
                header: 'Server'.t() ,
                width: Renderer.ipWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 's_server_addr'
            }, {
                header: 'Server Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 's_server_port'
            }, {
                header: 'Host'.t(),
                width: Renderer.hostnameWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'host'
            }, {
                header: 'Uri'.t(),
                flex:1,
                width: Renderer.uriWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'uri'
            }, {
                header: 'Query Term'.t(),
                flex:1,
                width: Renderer.uriWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'term'
            }, {
                header: 'Method'.t(),
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'method'
            }, {
                header: 'Download Content Length'.t(),
                width: Renderer.sizeWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 's2c_content_length'
            }, {
                header: 'Upload Content Length'.t(),
                width: Renderer.sizeWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'c2s_content_length'
            }, {
                header: 'Content Type'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 's2c_content_type'
            }, {
                header: 'Server'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'c_server_addr'
            }, {
                header: 'Server Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 's_server_port'
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
                width: Renderer.idWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'event_id'
            }, {
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            }, {
                header: 'Session Id'.t(),
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'session_id'
            }, {
                header: 'Policy Id'.t(),
                width: Renderer.idWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'policy_id'
            }, {
                header: 'Message Id'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'msg_id'
            }, {
                header: 'Client Interface'.t() ,
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'client_intf'
            }, {
                header: 'Server Interface'.t() ,
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'server_intf'
            }, {
                header: 'Username'.t(),
                width: Renderer.hostnameWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'username'
            }, {
                header: 'Hostname'.t(),
                width: Renderer.hostnameWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'hostname'
            }, {
                header: 'Client'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'c_client_addr'
            }, {
                header: 'Client Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'c_client_port'
            }, {
                header: 'New Client'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 's_client_addr'
            }, {
                header: 'New Client Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 's_client_port'
            }, {
                header: 'Original Server'.t() ,
                width: Renderer.ipWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'c_server_addr'
            }, {
                header: 'Original Server Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'c_server_port'
            }, {
                header: 'Server'.t() ,
                width: Renderer.ipWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 's_server_addr'
            }, {
                header: 'Server Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 's_server_port'
            }, {
                header: 'Receiver'.t(),
                width: Renderer.emailWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'addr'
            }, {
                header: 'Address Name'.t(),
                width: Renderer.emailWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'addr_name'
            }, {
                header: 'Address Kind'.t(),
                width: Renderer.idWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'addr_kind'
            }, {
                header: 'Sender'.t(),
                width: Renderer.emailWidth,
                flex:1,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'sender'
            }, {
                header: 'Subject'.t(),
                flex:1,
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'subject'
            }, {
                header: 'Name'.t() + ' (Virus Blocker Lite)',
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'virus_blocker_lite_name'
            }, {
                header: 'Clean'.t() + ' (Virus Blocker Lite)',
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'virus_blocker_lite_clean'
            }, {
                header: 'Name'.t() + ' (Virus Blocker)',
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'virus_blocker_name'
            }, {
                header: 'Clean'.t() + ' (Virus Blocker)',
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'virus_blocker_clean'
            }, {
                header: 'Action'.t() + ' (Spam Blocker Lite)',
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'spam_blocker_lite_action'
            }, {
                header: 'Spam Score'.t() + ' (Spam Blocker Lite)',
                width: Renderer.sizeWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'spam_blocker_lite_score'
            }, {
                header: 'Is Spam'.t() + ' (Spam Blocker Lite)',
                width: Renderer.booleanWidth,
                sortable: true,
                filter: Renderer.booleanFilter,
                dataIndex: 'spam_blocker_lite_is_spam'
            }, {
                header: 'Detail'.t() + ' (Spam Blocker Lite)',
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                flex: 1,
                dataIndex: 'spam_blocker_lite_tests_string'
            }, {
                header: 'Action'.t() + ' (Spam Blocker)',
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'spam_blocker_action'
            }, {
                header: 'Spam Score'.t() + ' (Spam Blocker)',
                width: Renderer.sizeWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'spam_blocker_score'
            }, {
                header: 'Is Spam'.t() + ' (Spam Blocker)',
                width: Renderer.booleanWidth,
                sortable: true,
                filter: Renderer.booleanFilter,
                dataIndex: 'spam_blocker_is_spam'
            }, {
                header: 'Detail'.t() + ' (Spam Blocker)',
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                flex: 1,
                dataIndex: 'spam_blocker_tests_string'
            }, {
                header: 'Action'.t() + ' (Phish Blocker)',
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'phish_blocker_action'
            }, {
                header: 'Score'.t() + ' (Phish Blocker)',
                width: Renderer.sizeWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'phish_blocker_score'
            }, {
                header: 'Is Phish'.t() + ' (Phish Blocker)',
                width: Renderer.booleanWidth,
                sortable: true,
                filter: Renderer.booleanFilter,
                dataIndex: 'phish_blocker_is_spam'
            }, {
                header: 'Detail'.t() + ' (Phish Blocker)',
                width: Renderer.messageWidth,
                flex: 1,
                sortable: true,
                filter: Renderer.stringFilter,
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
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            }, {
                header: 'Client'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'client_addr'
            }, {
                header: 'Username'.t(),
                width: Renderer.usernameWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'login_name'
            }, {
                header: 'Domain'.t(),
                width: Renderer.usernameWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'domain'
            }, {
                header: 'Action'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'type',
                flex: 1
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
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            }, {
                header: 'Login'.t(),
                width: Renderer.usernameWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'login'
            }, {
                header: 'Success'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'succeeded'
            }, {
                header: 'Local'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'local'
            }, {
                header: 'Client Address'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'client_addr'
            }, {
                header: 'Reason'.t(),
                flex: 1,
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
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
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            }, {
                header: 'Load (1-minute)'.t(),
                width: Renderer.loadWidth,
                sortable: true,
                dataIndex: 'load_1',
                filter: Renderer.numericFilter
            }, {
                header: 'Load (5-minute)'.t(),
                width: Renderer.loadWidth,
                sortable: true,
                dataIndex: 'load_5',
                filter: Renderer.numericFilter
            }, {
                header: 'Load (15-minute)'.t(),
                width: Renderer.loadWidth,
                sortable: true,
                dataIndex: 'load_15',
                filter: Renderer.numericFilter
            }, {
                header: 'CPU User Utilization'.t(),
                width: Renderer.loadWidth,
                sortable: true,
                dataIndex: 'cpu_user',
                filter: Renderer.numericFilter
            }, {
                header: 'CPU System Utilization'.t(),
                width: Renderer.loadWidth,
                sortable: true,
                dataIndex: 'cpu_system',
                filter: Renderer.numericFilter
            }, {
                header: 'Memory Total'.t(),
                width: Renderer.sizeWidth,
                sortable: true,
                dataIndex: 'mem_total',
                filter: Renderer.numericFilter,
                renderer: function(value) {
                    var meg = value/1024/1024;
                    return (Math.round( meg*10 )/10).toString() + ' MB';
                }

            }, {
                header: 'Memory Free'.t(),
                width: Renderer.sizeWidth,
                sortable: true,
                dataIndex: 'mem_free',
                filter: Renderer.numericFilter,
                renderer: function(value) {
                    var meg = value/1024/1024;
                    return (Math.round( meg*10 )/10).toString() + ' MB';
                }
            }, {
                header: 'Disk Total'.t(),
                width: Renderer.sizeWidth,
                sortable: true,
                dataIndex: 'disk_total',
                filter: Renderer.numericFilter,
                renderer: function(value) {
                    var gig = value/1024/1024/1024;
                    return (Math.round( gig*10 )/10).toString() + ' GB';
                }
            }, {
                header: 'Disk Free'.t(),
                width: Renderer.sizeWidth,
                flex: 1,
                sortable: true,
                dataIndex: 'disk_free',
                filter: Renderer.numericFilter,
                renderer: function(value) {
                    var gig = value/1024/1024/1024;
                    return (Math.round( gig*10 )/10).toString() + ' GB';
                }
            }, {
                header: 'Swap Total'.t(),
                width: Renderer.sizeWidth,
                sortable: true,
                dataIndex: 'swap_total',
                filter: Renderer.numericFilter,
                renderer: function(value) {
                    var meg = value/1024/1024;
                    return (Math.round( meg*10 )/10).toString() + ' MB';
                }
            }, {
                header: 'Swap Free'.t(),
                width: Renderer.sizeWidth,
                sortable: true,
                dataIndex: 'swap_free',
                filter: Renderer.numericFilter,
                renderer: function(value) {
                    var meg = value/1024/1024;
                    return (Math.round( meg*10 )/10).toString() + ' MB';
                }
            }, {
                header: 'Active Hosts'.t(),
                width: Renderer.sizeWidth,
                sortable: true,
                dataIndex: 'active_hosts',
                filter: Renderer.numericFilter
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
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            }, {
                header: 'Address'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'address'
            }, {
                header: 'Key'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'key'
            }, {
                header: 'Old Value'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'old_value'
            }, {
                header: 'Value'.t(),
                width: Renderer.messageWidth,
                flex: 1,
                sortable: true,
                filter: Renderer.stringFilter,
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
            }, {
                name: 'old_value',
                type: 'string'
            }],
            columns: [{
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            }, {
                header: 'MAC Address'.t(),
                width: Renderer.macWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'mac_address'
            }, {
                header: 'Key'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'key'
            }, {
                header: 'Old Value'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'old_value'
            }, {
                header: 'Value'.t(),
                width: Renderer.messageWidth,
                flex: 1,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'value'
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
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            }, {
                header: 'Username'.t(),
                width: Renderer.usernameWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'username'
            }, {
                header: 'Key'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'key'
            }, {
                header: 'Old Value'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'old_value'
            }, {
                header: 'Value'.t(),
                width: Renderer.messageWidth,
                flex: 1,
                sortable: true,
                filter: Renderer.stringFilter,
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
                convert: Converter.configurationBackupSuccess
            }, {
                name: 'description',
                type: 'string'
            }, {
                name: 'destination',
                type: 'string'
            }],
            columns: [{
                header: 'Event Id'.t(),
                width: Renderer.idWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'event_id'
            },{
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            }, {
                header: 'Result'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'success'
            }, {
                header: 'Destination'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'destination'
            }, {
                header: 'Details'.t(),
                flex:1,
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
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
                width: Renderer.idWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'event_id'
            },{
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            },{
                header: 'Interface Name'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'name'
            },{
                header: 'Interface Id'.t(),
                width: Renderer.idWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'interface_id',
                rtype: 'interface'
            },{
                header: 'Success'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                dataIndex: 'success',
                filter: Renderer.booleanFilter
            },{
                header: 'Test Description'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
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
                width: Renderer.idWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'event_id'
            },{
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            },{
                header: 'Interface Name'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'name'
            },{
                header: 'Interface Id'.t(),
                width: Renderer.idWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'interface_id',
                rtype: 'interface'
            },{
                header: 'Interface OS'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'os_name'
            },{
                header: 'Action'.t(),
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter,
                flex: 1,
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
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            },{
                header: 'Event Id'.t(),
                width: Renderer.idWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'event_id'
            },{
                header: 'Address'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'client_address'
            },{
                header: 'Username'.t(),
                width: Renderer.usernameWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'client_username'
            },{
                header: 'Protocol'.t(),
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'client_protocol',
                // rtype: 'protocol',
            },{
                header: 'Login Time'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'connect_stamp',
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            },{
                header: 'Logout Time'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'goodbye_stamp',
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            },{
                header: 'Elapsed'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'elapsed_time'
            },{
                header: 'Interface'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'net_interface',
                rtype: 'interface'
            },{
                header: 'RX Bytes'.t(),
                width: Renderer.sizeWidth,
                sortable: true,
                filter: Renderer.numericFilter,
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
                width: Renderer.sizeWidth,
                sortable: true,
                dataIndex: 'tx_bytes',
                filter: Renderer.numericFilter,
                renderer: function(value) {
                    if ((value === undefined) || (value === null) || (value === '')) {
                        return('');
                    }
                    var kb = value/1024;
                    return (Math.round( kb*10 )/10).toString() + ' KB';
                }
            },{
                header: 'Process'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
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
                width: Renderer.idWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'event_id'
            }, {
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            }, {
                header: 'Tunnel Name'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                flex: 1,
                filter: Renderer.stringFilter,
                dataIndex: 'tunnel_name'
            }, {
                header: 'In Bytes'.t(),
                width: Renderer.sizeWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'in_bytes',
                renderer: function(value) {
                    var kb = value/1024;
                    return (Math.round( kb*10 )/10).toString() + ' KB';
                }
            }, {
                header: 'Out Bytes'.t(),
                width: Renderer.sizeWidth,
                sortable: true,
                filter: Renderer.numericFilter,
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
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            },{
                header: 'Interface Id'.t(),
                width: Renderer.idWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'interface_id',
                rtype: 'interface'
            }, {
                header: 'RX Rate'.t(),
                width: Renderer.sizeWidth,
                sortable: true,
                dataIndex: 'rx_rate',
                filter: Renderer.numericFilter
            }, {
                header: 'TX Rate'.t(),
                width: Renderer.sizeWidth,
                sortable: true,
                dataIndex: 'tx_rate',
                filter: Renderer.numericFilter
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
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            }, {
                header: 'Policy Id'.t(),
                width: Renderer.idWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'policy_id'
            }, {
                header: 'Event Id'.t(),
                width: Renderer.idWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'event_id'
            }, {
                header: 'Vendor Name'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'vendor_name'
            }, {
                header: 'Sender'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'ipaddr'
            }, {
                header: 'DNSBL Server'.t(),
                width: Renderer.messageWidth,
                flex: 1,
                sortable: true,
                filter: Renderer.stringFilter,
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
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            }, {
                header: 'Event Id'.t(),
                width: Renderer.idWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'event_id'
            }, {
                header: 'Hit Count'.t(),
                width: Renderer.counterWidth,
                sortable: false,
                dataIndex: 'hits',
                filter: Renderer.numericFilter
            }, {
                header: 'Miss Count'.t(),
                width: Renderer.counterWidth,
                sortable: false,
                dataIndex: 'misses',
                filter: Renderer.numericFilter
            }, {
                header: 'Bypass Count'.t(),
                width: Renderer.counterWidth,
                sortable: false,
                dataIndex: 'bypasses',
                filter: Renderer.numericFilter
            }, {
                header: 'System Count'.t(),
                width: Renderer.counterWidth,
                sortable: false,
                dataIndex: 'systems',
                filter: Renderer.numericFilter
            }, {
                header: 'Hit Bytes'.t(),
                width: Renderer.sizeWidth,
                sortable: true,
                dataIndex: 'hit_bytes',
                filter: Renderer.numericFilter
            }, {
                header: 'Miss Bytes'.t(),
                width: Renderer.sizeWidth,
                flex: 1,
                sortable: true,
                dataIndex: 'miss_bytes',
                filter: Renderer.numericFilter
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
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            }, {
                header: 'Policy Id'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'policy_id'
            }, {
                header: 'Event Id'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'event_id'
            }, {
                header: 'Client'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'client_addr'
            }, {
                header: 'Username'.t(),
                width: Renderer.usernameWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'login_name'
            }, {
                header: 'Action'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'event_info'
            }, {
                header: 'Authentication'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'auth_type',
                flex: 1
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
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
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
                filter: Renderer.stringFilter,
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
                filter: Renderer.stringFilter,
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
                filter: Renderer.stringFilter,
                dataIndex: 'protocol'
            }, {
                header: 'Blocked'.t(),
                width: Renderer.booleanWidth,
                sortable: true,
                dataIndex: 'blocked',
                filter: Renderer.booleanFilter
            }, {
                header: 'Category'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'category'
            }, {
                header: 'Classtype'.t(),
                width: Renderer.messageWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'classtype'
            }, {
                header: 'Msg'.t(),
                width: Renderer.messageWidth,
                flex: 1,
                sortable: true,
                filter: Renderer.stringFilter,
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
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            }, {
                header: 'Type'.t(),
                width: Renderer.messageWidth,
                flex: 1,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'type'
            }, {
                header: 'Client Name'.t(),
                width: Renderer.usernameWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'client_name'
            }, {
                header: 'Client Address'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'remote_address'
            }, {
                header: 'Pool Address'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                filter: Renderer.stringFilter,
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
                filter: Renderer.numericFilter,
                dataIndex: 'event_id'
            }, {
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            }, {
                header: 'Start Time'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'start_time',
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            }, {
                header: 'End Time'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'end_time',
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            }, {
                header: 'Client Name'.t(),
                width: Renderer.usernameWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'client_name'
            }, {
                header: 'Client Address'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'remote_address'
            }, {
                header: 'Client Port'.t(),
                width: Renderer.portWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'remote_port'
            }, {
                header: 'Pool Address'.t(),
                width: Renderer.ipWidth,
                sortable: true,
                filter: Renderer.stringFilter,
                dataIndex: 'pool_address'
            }, {
                header: 'RX Bytes'.t(),
                width: Renderer.sizeWidth,
                sortable: true,
                filter: Renderer.numericFilter,
                dataIndex: 'rx_bytes',
                rtype: 'datasize',
            }, {
                header: 'TX Bytes'.t(),
                width: Renderer.sizeWidth,
                flex: 1,
                sortable: true,
                filter: Renderer.numericFilter,
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
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            },{
                header: 'Description'.t(),
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter,
                sortable: true,
                dataIndex: 'description'
            },{
                header: 'Summary Text'.t(),
                sortable: true,
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter,
                dataIndex: 'summary_text'
            },{
                header: 'JSON'.t(),
                flex: 1,
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter,
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
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
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
                width: Renderer.idWidth,
                filter: Renderer.numericFilter,
                sortable: true,
                dataIndex: 'event_id'
            }, {
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            }, {
                header: 'Policy Id'.t(),
                width: Renderer.idWidth,
                filter: Renderer.stringFilter,
                sortable: true,
                dataIndex: 'policy_id'
            }, {
                header: 'Session Id'.t(),
                width: Renderer.messageWidth,
                filter: Renderer.numericFilter,
                sortable: true,
                dataIndex: 'session_id'
            }, {
                header: 'Request Id'.t(),
                width: Renderer.portWidth,
                filter: Renderer.numericFilter,
                sortable: true,
                dataIndex: 'request_id'
            }, {
                header: 'Method'.t(),
                width: Renderer.portWidth,
                filter: Renderer.stringFilter,
                sortable: true,
                dataIndex: 'method'
            }, {
                header: 'Client Interface'.t() ,
                width: Renderer.portWidth,
                filter: Renderer.stringFilter,
                sortable: true,
                dataIndex: 'client_intf'
            }, {
                header: 'Server Interface'.t() ,
                width: Renderer.portWidth,
                filter: Renderer.stringFilter,
                sortable: true,
                dataIndex: 'server_intf'
            }, {
                header: 'Client'.t(),
                width: Renderer.ipWidth,
                filter: Renderer.stringFilter,
                sortable: true,
                dataIndex: 'c_client_addr'
            }, {
                header: 'New Client'.t(),
                width: Renderer.ipWidth,
                filter: Renderer.stringFilter,
                sortable: true,
                dataIndex: 's_client_addr'
            }, {
                header: 'Original Server'.t() ,
                width: Renderer.ipWidth,
                filter: Renderer.stringFilter,
                sortable: true,
                dataIndex: 'c_server_addr'
            }, {
                header: 'Server'.t() ,
                width: Renderer.ipWidth,
                filter: Renderer.stringFilter,
                sortable: true,
                dataIndex: 's_server_addr'
            }, {
                header: 'Hostname'.t(),
                width: Renderer.hostnameWidth,
                filter: Renderer.stringFilter,
                sortable: true,
                dataIndex: 'hostname'
            }, {
                header: 'Username'.t(),
                width: Renderer.usernameWidth,
                filter: Renderer.stringFilter,
                sortable: true,
                dataIndex: 'username'
            }, {
                header: 'File Name'.t(),
                flex:1,
                width: Renderer.uriWidth,
                filter: Renderer.stringFilter,
                dataIndex: 'uri'
            }, {
                header: 'Virus Blocker Lite ' + 'Name'.t(),
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter,
                sortable: true,
                dataIndex: 'virus_blocker_lite_name'
            }, {
                header: 'Virus Blocker Lite ' + 'clean'.t(),
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter,
                sortable: true,
                dataIndex: 'virus_blocker_lite_clean'
            }, {
                header: 'Virus Blocker ' + 'Name'.t(),
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter,
                sortable: true,
                dataIndex: 'virus_blocker_name'
            }, {
                header: 'Virus Blocker ' + 'Clean'.t(),
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter,
                sortable: true,
                dataIndex: 'virus_blocker_clean'
            }, {
                header: 'Server'.t(),
                width: Renderer.ipWidth,
                filter: Renderer.stringFilter,
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
                convert: Converter.quotaAction
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
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            }, {
                header: 'Address'.t(),
                width: Renderer.ipWidth,
                filter: Renderer.stringFilter,
                sortable: true,
                dataIndex: 'address'
            }, {
                header: 'Action'.t(),
                width: Renderer.messageWidth,
                filter: Renderer.stringFilter,
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
                filter: Renderer.stringFilter,
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
                name: 'username',
                type: 'string'
            }, {
                name: 'hostname',
                type: 'string'
            }, {
                name: 'settings_file',
                type: 'string'
            }],
            columns: [{
                header: 'Timestamp'.t(),
                width: Renderer.timestampWidth,
                sortable: true,
                dataIndex: 'time_stamp',
                rtype: 'timestamp',
                filter: Renderer.timestampFilter
            }, {
                header: 'Username'.t(),
                width: Renderer.usernameWidth,
                filter: Renderer.stringFilter,
                sortable: true,
                dataIndex: 'username'
            }, {
                header: 'Hostname'.t(),
                width: Renderer.hostnameWidth,
                filter: Renderer.stringFilter,
                sortable: true,
                dataIndex: 'hostname'
            }, {
                header: 'Settings File'.t(),
                flex:1,
                filter: Renderer.stringFilter,
                width: Renderer.uriWidth,
                dataIndex: 'settings_file',
                rtype: 'settingsFile'
            },{
                header: "Differences".t(),
                width: Renderer.actionWidth,
                xtype: 'actioncolumn',
                align: 'center',
                tdCls: 'action-cell',
                hideable: false,
                hidden: false,
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
            }]
        }
    }
});

Ext.define('Ung.util.Util', {
    alternateClassName: 'Util',
    singleton: true,
    ignoreExceptions: false,

    // defaultColors: ['#7cb5ec', '#434348', '#90ed7d', '#f7a35c', '#8085e9', '#f15c80', '#e4d354', '#2b908f', '#f45b5b', '#91e8e1'],
    defaultColors: ['#00b000', '#3030ff', '#009090', '#00ffff', '#707070', '#b000b0', '#fff000', '#b00000', '#ff0000', '#ff6347', '#c0c0c0'], // from old UI

    subNav: [
        '->',
        { text: 'Sessions'.t(), iconCls: 'fa fa-list', href: '#sessions', hrefTarget: '_self', bind: { userCls: '{activeItem === "sessions" ? "pressed" : ""}' } },
        { text: 'Hosts'.t(), iconCls: 'fa fa-th-list', href: '#hosts', hrefTarget: '_self', bind: { userCls: '{activeItem === "hosts" ? "pressed" : ""}' } },
        { text: 'Devices'.t(), iconCls: 'fa fa-desktop', href: '#devices', hrefTarget: '_self', bind: { userCls: '{activeItem === "devices" ? "pressed" : ""}' } },
        { text: 'Users'.t(), iconCls: 'fa fa-users', href: '#users', hrefTarget: '_self', bind: { userCls: '{activeItem === "users" ? "pressed" : ""}' } }
    ],

    baseCategories: [
        { name: 'hosts', type: 'system', displayName: 'Hosts' },
        { name: 'devices', type: 'system', displayName: 'Devices' },
        { name: 'network', type: 'system', displayName: 'Network' },
        { name: 'administration', type: 'system', displayName: 'Administration' },
        { name: 'system', type: 'system', displayName: 'System' },
        { name: 'events', type: 'system', displayName: 'Events' },
        { name: 'shield', type: 'system', displayName: 'Shield' },
        { name: 'users', type: 'system', displayName: 'Users' }
    ],

    appStorage: { },

    appDescription: {
        'web-filter': 'Web Filter scans and categorizes web traffic to monitor and enforce network usage policies.'.t(),
        'web-monitor': 'Web monitor scans and categorizes web traffic to monitor and enforce network usage policies.'.t(),
        'virus-blocker': 'Virus Blocker detects and blocks malware before it reaches users desktops or mailboxes.'.t(),
        'virus-blocker-lite': 'Virus Blocker Lite detects and blocks malware before it reaches users desktops or mailboxes.'.t(),
        'spam-blocker': 'Spam Blocker detects, blocks, and quarantines spam before it reaches users mailboxes.'.t(),
        'spam-blocker-lite': 'Spam Blocker Lite detects, blocks, and quarantines spam before it reaches users mailboxes.'.t(),
        'phish-blocker': 'Phish Blocker detects and blocks phishing emails using signatures.'.t(),
        'web-cache': 'Web Cache stores and serves web content from local cache for increased speed and reduced bandwidth usage.'.t(),
        'bandwidth-control': 'Bandwidth Control monitors, manages, and shapes bandwidth usage on the network'.t(),
        'ssl-inspector': 'SSL Inspector allows for full decryption of HTTPS and SMTPS so that other applications can process the encrytped streams.'.t(),
        'application-control': 'Application Control scans sessions and identifies the associated applications allowing each to be flagged and/or blocked.'.t(),
        'application-control-lite': 'Application Control Lite identifies, logs, and blocks sessions based on the session content using custom signatures.'.t(),
        'captive-portal': 'Captive Portal allows administrators to require network users to complete a defined process, such as logging in or accepting a network usage policy, before accessing the internet.'.t(),
        'firewall': 'Firewall is a simple application that flags and blocks sessions based on rules.'.t(),
        'ad-blocker': 'Ad Blocker blocks advertising content and tracking cookies for scanned web traffic.'.t(),
        'reports': 'Reports records network events to provide administrators the visibility and data necessary to investigate network activity.'.t(),
        'policy-manager': 'Policy Manager enables administrators to create different policies and handle different sessions with different policies based on rules.'.t(),
        'directory-connector': 'Directory Connector allows integration with external directories and services, such as Active Directory, RADIUS, or Google.'.t(),
        'wan-failover': 'WAN Failover detects WAN outages and re-routes traffic to any other available WANs to maximize network uptime.'.t(),
        'wan-balancer': 'WAN Balancer spreads network traffic across multiple internet connections for better performance.'.t(),
        'ipsec-vpn': 'IPsec VPN provides secure network access and tunneling to remote users and sites using IPsec, GRE, L2TP, Xauth, and IKEv2 protocols.'.t(),
        'openvpn': 'OpenVPN provides secure network access and tunneling to remote users and sites using the OpenVPN protocol.'.t(),
        'tunnel-vpn': 'Tunnel VPN provides secure and private internet through encrypted tunnel to remote secure internet providers.'.t(),
        'intrusion-prevention': 'Intrusion Prevention blocks scans, detects, and blocks attacks and suspicious traffic using signatures.'.t(),
        'configuration-backup': 'Configuration Backup automatically creates backups of settings uploads them to My Account and Google Drive.'.t(),
        'branding-manager': 'The Branding Settings are used to set the logo and contact information that will be seen by users (e.g. reports).'.t(),
        'live-support': 'Live Support provides on-demand help for any technical issues.'.t()
    },

    bytesToHumanReadable: function (bytes, si) {
        var thresh = si ? 1000 : 1024;
        if(Math.abs(bytes) < thresh) {
            return bytes + ' B';
        }
        var units = si ? ['kB','MB','GB','TB','PB','EB','ZB','YB'] : ['KiB','MiB','GiB','TiB','PiB','EiB','ZiB','YiB'];
        var u = -1;
        do {
            bytes /= thresh;
            ++u;
        } while(Math.abs(bytes) >= thresh && u < units.length - 1);
        return bytes.toFixed(1)+' '+units[u];
    },

    formatBytes: function (bytes, decimals) {
        if (bytes === 0) {
            return '0';
        }
        //bytes = bytes * 1000;
        var k = 1000, // or 1024 for binary
            dm = decimals || 3,
            sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'],
            i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
    },

    bytesRenderer: function(bytes, perSecond) {
        var units = (!perSecond) ? ['bytes'.t(), 'Kbytes'.t(), 'Mbytes'.t(), 'Gbytes'.t()] :
            ['bytes/s'.t(), 'Kbytes/s'.t(), 'Mbytes/s'.t(), 'Gbytes/s'.t()];
        var units_itr = 0;
        while ((bytes >= 1000 || bytes <= -1000) && units_itr < 3) {
            bytes = bytes/1000;
            units_itr++;
        }
        bytes = Math.round(bytes*100)/100;
        return bytes + ' ' + units[units_itr];
    },
    bytesRendererCompact: function(bytes) {
        var units = ['', 'K', 'M', 'G'];
        var units_itr = 0;
        while ((bytes >= 1000 || bytes <= -1000) && units_itr < 3) {
            bytes = bytes/1000;
            units_itr++;
        }
        bytes = Math.round(bytes*100)/100;
        return bytes + ' ' + units[units_itr];
    },

    successToast: function (message) {
        Ext.toast({
            html: '<i class="fa fa-check fa-lg"></i> ' + message,
            // minWidth: 200,
            bodyPadding: '12 12 12 40',
            baseCls: 'toast',
            border: false,
            bodyBorder: false,
            // align: 'b',
            align: 'br',
            autoCloseDelay: 5000,
            slideInAnimation: 'easeOut',
            slideInDuration: 300,
            hideDuration: 0,
            paddingX: 10,
            paddingY: 50
        });
    },

    showWarningMessage:function(message, details, errorHandler) {
        var wnd = Ext.create('Ext.window.Window', {
            title: 'Warning'.t(),
            modal:true,
            closable:false,
            layout: "fit",
            setSizeToRack: function () {
                if(Ung.Main && Ung.Main.viewport) {
                    var objSize = Ung.Main.viewport.getSize();
                    objSize.height = objSize.height - 66;
                    this.setPosition(0, 66);
                    this.setSize(objSize);
                } else {
                    this.maximize();
                }
            },
            doSize: function() {
                var detailsComp = this.down('fieldset[name="details"]');
                if(!detailsComp.isHidden()) {
                    this.setSizeToRack();
                } else {
                    this.center();
                }
            },
            items: {
                xtype: "panel",
                minWidth: 350,
                autoScroll: true,
                defaults: {
                    border: false
                },
                items: [{
                    xtype: "fieldset",
                    padding: 10,
                    items: [{
                        xtype: "label",
                        html: message,
                    }]
                }, {
                    xtype: "fieldset",
                    hidden: (typeof(interactiveMode) != "undefined" && interactiveMode == false),
                    items: [{
                        xtype: "button",
                        name: "details_button",
                        text: "Show details".t(),
                        hidden: details==null,
                        handler: function() {
                            var detailsComp = wnd.down('fieldset[name="details"]');
                            var detailsButton = wnd.down('button[name="details_button"]');
                            if(detailsComp.isHidden()) {
                                wnd.initialHeight = wnd.getHeight();
                                wnd.initialWidth = wnd.getWidth();
                                detailsComp.show();
                                detailsButton.setText('Hide details'.t());
                                wnd.setSizeToRack();
                            } else {
                                detailsComp.hide();
                                detailsButton.setText('Show details'.t());
                                wnd.restore();
                                wnd.setHeight(wnd.initialHeight);
                                wnd.setWidth(wnd.initialWidth);
                                wnd.center();
                            }
                        },
                        scope : this
                    }]
                }, {
                    xtype: "fieldset",
                    name: "details",
                    hidden: true,
                    html: details!=null ? details : ''
                }]
            },
            buttons: [{
                text: 'OK'.t(),
                hidden: (typeof(interactiveMode) != "undefined" && interactiveMode == false),
                handler: function() {
                    if ( errorHandler) {
                        errorHandler();
                    } else {
                        wnd.close();
                    }
                }
            }]
        });
        wnd.show();
        if(Ext.MessageBox.rendered) {
            Ext.MessageBox.hide();
        }
    },

    goToStartPage: function () {
        Ext.MessageBox.wait("Redirecting to the start page...".t(), "Please wait".t());
        location.reload();
    },

    handleException: function (exception) {
        if (Util.ignoreExceptions)
            return;

        var message = null;
        var details = "";

        if ( !exception ) {
            console.error("Null Exception!");
            return;
        } else {
            console.error(exception);
        }

        if ( exception.javaStack )
            exception.name = exception.javaStack.split('\n')[0]; //override poor jsonrpc.js naming
        if ( exception.name )
            details += "<b>" + "Exception name".t() +":</b> " + exception.name + "<br/><br/>";
        if ( exception.code )
            details += "<b>" + "Exception code".t() +":</b> " + exception.code + "<br/><br/>";
        if ( exception.message )
            details += "<b>" + "Exception message".t() + ":</b> " + exception.message.replace(/\n/g, '<br/>') + "<br/><br/>";
        if ( exception.javaStack )
            details += "<b>" + "Exception java stack".t() +":</b> " + exception.javaStack.replace(/\n/g, '<br/>') + "<br/><br/>";
        if ( exception.stack )
            details += "<b>" + "Exception js stack".t() +":</b> " + exception.stack.replace(/\n/g, '<br/>') + "<br/><br/>";
        if ( rpc.fullVersionAndRevision != null )
            details += "<b>" + "Build".t() +":&nbsp;</b>" + rpc.fullVersionAndRevision + "<br/><br/>";
        details +="<b>" + "Timestamp".t() +":&nbsp;</b>" + (new Date()).toString() + "<br/><br/>";
        if ( exception.response )
            details += "<b>" + "Exception response".t() +":</b> " + Ext.util.Format.stripTags(exception.response).replace(/\s+/g,'<br/>') + "<br/><br/>";

        /* handle authorization lost */
        if( exception.response && exception.response.includes("loginPage") ) {
            message  = "Session timed out.".t() + "<br/>";
            message += "Press OK to return to the login page.".t() + "<br/>";
            Util.ignoreExceptions = true;
            Util.showWarningMessage(message, details, Util.goToStartPage);
            return;
        }

        /* handle connection lost */
        if( exception.code==550 || exception.code == 12029 || exception.code == 12019 || exception.code == 0 ||
            /* handle connection lost (this happens on windows only for some reason) */
            (exception.name == "JSONRpcClientException" && exception.fileName != null && exception.fileName.indexOf("jsonrpc") != -1) ||
            /* special text for "method not found" and "Service Temporarily Unavailable" */
            (exception.message && exception.message.indexOf("method not found") != -1) ||
            (exception.message && exception.message.indexOf("Service Unavailable") != -1) ||
            (exception.message && exception.message.indexOf("Service Temporarily Unavailable") != -1) ||
            (exception.message && exception.message.indexOf("This application is not currently available") != -1)) {
            message  = "The connection to the server has been lost.".t() + "<br/>";
            message += "Press OK to return to the login page.".t() + "<br/>";
            Util.ignoreExceptions = true;
            Util.showWarningMessage(message, details, Util.goToStartPage);
            return;
        }

        Util.exceptionToast(exception);
    },

    exceptionToast: function (ex) {
        var msg = [];
        if (typeof ex === 'object') {
            if (ex.name && ex.code) {
                msg.push('<strong>Name:</strong> ' + ex.name + ' (' + ex.code + ')');
            }
            if (ex.ex) {
                msg.push('<strong>Error:</strong> ' + ex.ex);
            }
            if (ex.message) {
                msg.push('<strong>Message:</strong> ' + ex.message);
            }
        } else {
            msg = [ex];
        }
        Ext.toast({
            html: '<i class="fa fa-exclamation-triangle fa-lg"></i> <span style="font-weight: bold; color: yellow;">Exception!</span><br/>' + msg.join('<br/>'),
            bodyPadding: '10 10 10 45',
            baseCls: 'toast',
            cls: 'exception',
            border: false,
            bodyBorder: false,
            align: 'br',
            autoCloseDelay: 5000,
            slideInAnimation: 'easeOut',
            slideInDuration: 300,
            hideDuration: 0,
            paddingX: 10,
            paddingY: 50
        });
    },

    invalidFormToast: function (fields) {
        if (!fields || fields.length === 0) {
            return;
        }

        var str = [];
        fields.forEach(function (field) {
            str.push('<span class="field-name">' + field.label + '</span>: <br/> <span class="field-error">' + field.error.replace(/<\/?[^>]+(>|$)/g, '') + '</span>');
        });

        // var store = [];
        // fields.forEach(function (field) {
        //     console.log(field);
        //     store.push({ label: field.getFieldLabel(), error: field.getActiveError().replace(/<\/?[^>]+(>|$)/g, ''), field: field });
        // });

        Ext.toast({
            html: '<i class="fa fa-exclamation-triangle fa-lg"></i> <span style="font-weight: bold; font-size: 14px; color: yellow;">Check invalid fields!</span><br/><br/>' + str.join('<br/>'),
            bodyPadding: '10 10 10 45',
            baseCls: 'toast-invalid-frm',
            border: false,
            bodyBorder: false,
            align: 'br',
            autoCloseDelay: 5000,
            slideInAnimation: 'easeOut',
            slideInDuration: 300,
            hideDuration: 0,
            paddingX: 10,
            paddingY: 50
            // items: [{
            //     xtype: 'dataview',
            //     store: {
            //         data: store
            //     },
            //     tpl:     '<tpl for=".">' +
            //         '<div style="margin-bottom: 10px;">' +
            //         '<span class="field-name">{label}</span>:' +
            //         '<br/><span>{error}</span>' +
            //         '</div>' +
            //     '</tpl>',
            //     itemSelector: 'div',
            //     listeners: {
            //         select: function (el, field) {
            //             field.get('field').focus();
            //         }
            //     }
            // }]
        });
    },

    getNextHopList: function (getMap) {
        var networkSettings = rpc.networkSettings;
        var devList = [];
        var devMap = {};

        for (var i = 0 ; i < networkSettings.interfaces.list.length ; i++) {
            var intf = networkSettings.interfaces.list[i];
            var name = Ext.String.format("Local on {0} ({1})".t(), intf.name, intf.systemDev);
            var key = ("" + intf.interfaceId);
            devList.push([ key, name ]);
            devMap[key] = name;
        }
        if (getMap) return(devMap);
        return(devList);
    },


    getInterfaceList: function (wanMatchers, anyMatcher) {
        var networkSettings = rpc.networkSettings,
            data = [], intf, i;

        // Note: using strings as keys instead of numbers, needed for the checkboxgroup column widget component to function

        for (i = 0; i < networkSettings.interfaces.list.length; i += 1) {
            intf = networkSettings.interfaces.list[i];
            data.push([intf.interfaceId.toString(), intf.name]);
        }
        for (i = 0; i < networkSettings.virtualInterfaces.list.length; i += 1) {
            intf = networkSettings.virtualInterfaces.list[i];
            data.push([intf.interfaceId.toString(), intf.name]);
        }

        if (wanMatchers) {
            data.unshift(['wan', 'Any WAN'.t()]);
            data.unshift(['non_wan', 'Any Non-WAN'.t()]);
        }
        if (anyMatcher) {
            data.unshift(['any', 'Any'.t()]);
        }
        return data;
    },

    bytesToMBs: function(value) {
        return Math.round(value/10000)/100;
    },

    // used for render purposes
    interfacesListNamesMap: function () {
        var map = {
            'wan': 'Any WAN'.t(),
            'non_wan': 'Any Non-WAN'.t(),
            'any': 'Any'.t(),
        };
        var i, intf;

        for (i = 0; i < rpc.networkSettings.interfaces.list.length; i += 1) {
            intf = rpc.networkSettings.interfaces.list[i];
            map[intf.systemDev] = intf.name;
            map[intf.interfaceId] = intf.name;
        }
        for (i = 0; i < rpc.networkSettings.virtualInterfaces.list.length; i += 1) {
            intf = rpc.networkSettings.virtualInterfaces.list[i];
            map[intf.interfaceId] = intf.name;
        }
        return map;
    },

    urlValidator: function (val) {
        var res = val.match(/(http(s)?:\/\/.)?(www\.)?[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,6}\b([-a-zA-Z0-9@:%_\+.~#?&//=]*)/g);
        return res ? true : 'Url missing or in wrong format!'.t();
    },

    /**
     * Helper method that lists the order in which classes are loaded
     */
    getClassOrder: function () {
        var classes = [], extClasses = [];

        Ext.Loader.history.forEach(function (cls) {
            if (cls.indexOf('Ung') === 0) {
                classes.push(cls.replace('Ung', 'app').replace(/\./g, '/') + '.js');
            } else {
                extClasses.push(cls);
            }
        });

        classes.pop();

        Ext.create('Ext.Window', {
            title: 'Untangle Classes Load Order',
            width: 600,
            height: 600,

            // Constraining will pull the Window leftwards so that it's within the parent Window
            modal: true,
            draggable: false,
            resizable: false,
            layout: {
                type: 'hbox',
                align: 'stretch',
                pack: 'end'
            },
            items: [{
                xtype: 'textarea',
                border: false,
                flex: 1,
                editable: false,
                fieldStyle: {
                    background: '#FFF',
                    fontSize: '11px'
                },
                value: classes.join('\r\n')
            }, {
                xtype: 'textarea',
                border: false,
                flex: 1,
                editable: false,
                fieldStyle: {
                    background: '#FFF',
                    fontSize: '11px'
                },
                value: extClasses.join('\r\n')
            }]
        }).show();
    },

    getV4NetmaskList: function(includeNull) {
        var data = [];
        if (includeNull) {
            data.push( [null,'\u00a0'] );
        }
        data.push( [32,'/32 - 255.255.255.255'] );
        data.push( [31,'/31 - 255.255.255.254'] );
        data.push( [30,'/30 - 255.255.255.252'] );
        data.push( [29,'/29 - 255.255.255.248'] );
        data.push( [28,'/28 - 255.255.255.240'] );
        data.push( [27,'/27 - 255.255.255.224'] );
        data.push( [26,'/26 - 255.255.255.192'] );
        data.push( [25,'/25 - 255.255.255.128'] );
        data.push( [24,'/24 - 255.255.255.0'] );
        data.push( [23,'/23 - 255.255.254.0'] );
        data.push( [22,'/22 - 255.255.252.0'] );
        data.push( [21,'/21 - 255.255.248.0'] );
        data.push( [20,'/20 - 255.255.240.0'] );
        data.push( [19,'/19 - 255.255.224.0'] );
        data.push( [18,'/18 - 255.255.192.0'] );
        data.push( [17,'/17 - 255.255.128.0'] );
        data.push( [16,'/16 - 255.255.0.0'] );
        data.push( [15,'/15 - 255.254.0.0'] );
        data.push( [14,'/14 - 255.252.0.0'] );
        data.push( [13,'/13 - 255.248.0.0'] );
        data.push( [12,'/12 - 255.240.0.0'] );
        data.push( [11,'/11 - 255.224.0.0'] );
        data.push( [10,'/10 - 255.192.0.0'] );
        data.push( [9,'/9 - 255.128.0.0'] );
        data.push( [8,'/8 - 255.0.0.0'] );
        data.push( [7,'/7 - 254.0.0.0'] );
        data.push( [6,'/6 - 252.0.0.0'] );
        data.push( [5,'/5 - 248.0.0.0'] );
        data.push( [4,'/4 - 240.0.0.0'] );
        data.push( [3,'/3 - 224.0.0.0'] );
        data.push( [2,'/2 - 192.0.0.0'] );
        data.push( [1,'/1 - 128.0.0.0'] );
        data.push( [0,'/0 - 0.0.0.0'] );

        return data;
    },

    validateForms: function (view) {
        var invalidFields = [];

        view.query('form[withValidation]').forEach(function (form) {
            if (form.isDirty()) {
                form.query('field{isValid()==false}').forEach(function (field) {
                    invalidFields.push({ label: field.getFieldLabel(), error: field.getActiveError() });
                    // invalidFields.push(field);
                });
            }
        });

        if (invalidFields.length > 0) {
            Util.invalidFormToast(invalidFields);
            return false;
        }
        return true;
    },

    urlValidator2: function (url) {
        if (url.match(/^([^:]+):\/\// ) !== null) {
            return 'Site cannot contain URL protocol.'.t();
        }
        if (url.match(/^([^:]+):\d+\// ) !== null) {
            return 'Site cannot contain port.'.t();
        }
        // strip "www." from beginning of rule
        if (url.indexOf('www.') === 0) {
            url = url.substr(4);
        }
        // strip "*." from beginning of rule
        if (url.indexOf('*.') === 0) {
            url = url.substr(2);
        }
        // strip "/" from the end
        if (url.indexOf('/') === url.length - 1) {
            url = url.substring(0, url.length - 1);
        }
        if (url.trim().length === 0) {
            return 'Invalid URL specified'.t();
        }
        return true;
    },

    // formats a timestamp - expects a timestamp integer or an onject literal with 'time' property
    timestampFormat: function(v) {
        if (!v || typeof v === 'string') {
            return 0;
        }
        var date = new Date();
        if (typeof v === 'object' && v.time) {
            date.setTime(v.time);
        } else {
            date.setTime(v);
        }
        return Ext.util.Format.date(date, 'timestamp_fmt'.t());
    },

    getStoreUrl: function(){
        // non API store URL used for links like: My Account, Forgot Password
        return rpc.storeUrl.replace('/api/v1', '/store/open.php');
    },

    getAbout: function (forceReload) {
        if (rpc.about === undefined) {
            var query = "";
            query = query + "uid=" + rpc.serverUID;
            query = query + "&" + "version=" + rpc.fullVersion;
            query = query + "&" + "webui=true";
            query = query + "&" + "lang=" + rpc.languageSettings.language;
            query = query + "&" + "applianceModel=" + rpc.applianceModel;

            rpc.about = query;
        }
        return rpc.about;
    },

    weekdaysMap: {
        '1': 'Sunday'.t(),
        '2': 'Monday'.t(),
        '3': 'Tuesday'.t(),
        '4': 'Wednesday'.t(),
        '5': 'Thursday'.t(),
        '6': 'Friday'.t(),
        '7': 'Saturday'.t()
    },

    keyStr : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",

    base64encode: function(input) {
        if (typeof(base64encode) === 'function') {
            return base64encode(input);
        }
        var output = "";
        var chr1, chr2, chr3, enc1, enc2, enc3, enc4;
        var i = 0;
        input = Util.utf8Encode(input);
        while (i < input.length) {
            chr1 = input.charCodeAt(i++);
            chr2 = input.charCodeAt(i++);
            chr3 = input.charCodeAt(i++);
            enc1 = chr1 >> 2;
            enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
            enc3 = ((chr2 & 15) << 2) | (chr3 >> 6);
            enc4 = chr3 & 63;
            if (isNaN(chr2)) {
                enc3 = enc4 = 64;
            } else if (isNaN(chr3)) {
                enc4 = 64;
            }
            output = output +
            Util.keyStr.charAt(enc1) + Util.keyStr.charAt(enc2) +
            Util.keyStr.charAt(enc3) + Util.keyStr.charAt(enc4);
        }
        return output;
    },

    utf8Encode : function (string) {
        string = string.replace(/\r\n/g,"\n");
        var utftext = "";
        for (var n = 0; n < string.length; n++) {
            var c = string.charCodeAt(n);
            if (c < 128) {
                utftext += String.fromCharCode(c);
            }
            else if((c > 127) && (c < 2048)) {
                utftext += String.fromCharCode((c >> 6) | 192);
                utftext += String.fromCharCode((c & 63) | 128);
            }
            else {
                utftext += String.fromCharCode((c >> 12) | 224);
                utftext += String.fromCharCode(((c >> 6) & 63) | 128);
                utftext += String.fromCharCode((c & 63) | 128);
            }
        }
        return utftext;
    },

    getLicenseMessage: function (license) {
        var message = '';
        if (!license) {
            return message;
        }
        if (license.trial) {
            if(license.expired) {
                message = 'Free trial expired!'.t();
            } else if (license.daysRemaining < 2) {
                message = 'Free trial.'.t() + ' ' + 'Expires today.'.t();
            } else if (license.daysRemaining < 32) {
                message = 'Free trial.'.t() + ' ' + Ext.String.format('{0} ', license.daysRemaining) + 'days remain.'.t();
            } else {
                message = 'Free trial.'.t();
            }
        } else if (!license.valid) {
            message = license.status;
        }
        return message;
    },

    activeClone: function(source) {
        // clone the source record which will usually be emptyRow
        var target = Ext.clone(source);
        // look at each item in the source record
        Ext.iterate(source, function(key, value) {
            // look for items in the record that are arrays
            if ( (value !== null) && (typeof(value) === 'object') && (value.length) && (value.length === 2) && (typeof(value[0] === 'string')) ) {
                // found an array so evaluate first string as the function with the second string as the argument and put result in target record
                target[key] = eval(value[0] + "('" + value[1] + "')");
            }
        });
        return(target);
    },

    getAppStorageValue: function(itemName) {
        var data = Ung.util.Util.appStorage[itemName];
        return(data);
    },

    setAppStorageValue: function(itemName, itemValue) {
        Ung.util.Util.appStorage[itemName] = itemValue;
    }

});
