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
        T: 'in Search Term list'.t(),
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
        if (!rpc.reportsManager) {
            return value.toString();
        }

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
        HOST_CHANGE: 'Host Change Logout'.t(),
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
        GOOGLE: 'Google Account'.t(),
        FACEBOOK: 'Facebook Account'.t(),
        MICROSOFT: 'Microsoft Account'.t(),
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

    sessionSpeed: function(value){
        return Math.round(value * 10 )/ 10;
    }

});
