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
        A: 'authenticate'.t(),
        default: 'unknown'.t()

    },
    directoryConnectorAction: function( value ){
        if(Ext.isEmpty(value)) {
            return '';
        }
        return ( value in Converter.directoryConnectorActionMap ) ? Converter.directoryConnectorActionMap[value] : Converter.directoryConnectorActionMap['default'];
    },

    directoryConnectorActionSourceMap: {
        W: 'client'.t(),
        A: 'active directory'.t(),
        R: 'radius'.t(),
        T: 'test'.t(),
        default: 'unknown'.t()

    },
    directoryConnectorActionSource: function( value ){
        if(Ext.isEmpty(value)) {
            return '';
        }
        return ( value in Converter.directoryConnectorActionSourceMap ) ? Converter.directoryConnectorActionSourceMap[value] : Converter.directoryConnectorActionSourceMap['default'];
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
    }

});
