Ext.define('Ung.util.Renderer', {
    singleton: true,
    alternateClassName: 'Renderer',

    listKey: '__list__',

    // Format used when there is some value for the user knowing the subscripted value.
    // Not useful for index values that aren't visible to the user.
    mapValueFormat: '{0} [{1}]'.t(),

    /*
     * Common column widths
     */
    // Action
    actionWidth: 60,
    // Boolean
    booleanWidth: 60,
    // Comparator field
    comparatorWidth: 120,
    // Conditions
    conditionsWidth: 100,
    // Counter
    counterWidth: 80,
    // UTC-formatted Date
    dateWidth: 170,
    // Email address
    emailWidth: 150,
    // Hostname
    hostnameWidth: 120,
    // Numeric identifier
    idWidth: 80,
    // Icon
    iconWidth: 30,
    // Interval
    intervalWidth: 90,
    // IP Address
    ipWidth: 100,
    // Load measurement
    loadWidth: 50,
    // Latitude/longtitude
    locationWidth: 50,
    // MAC address
    macWidth: 120,
    // General purpose
    messageWidth: 120,
    // Network (ip/bit)
    networkWidth: 120,
    // Port
    portWidth: 70,
    // Network prefix
    prefixWidth: 50,
    // Priority
    prioritytWidth: 70,
    // Protocol
    protocolWidth: 70,
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

    /**
     * Array store with renderers used in Reports Settings
     */
    forReports: [['', 'None'.t()], ['interface', 'Interface'], ['protocol', 'Protocol'.t()], ['policy_id', 'Policy'.t()]],

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

    localize: function(value){
        return value.t();
    },

    boolean: function( value ){
        return ( value == true || value == 'true' ) ? 'true' : 'false';
    },

    /**
     * Determine width based on the size of the screen body and divide by the divisor.
     */
    calculateWith: function (divisor) {
        return Math.ceil(Ext.getBody().getViewSize().width / divisor) - 1;
    },

    timestampOffset: null,
    timestamp: function (value) {
        if(Renderer.timestampOffset === null){
            Renderer.timestampOffset =  (new Date().getTimezoneOffset() * 60000) + rpc.timeZoneOffset;
        }
        if (!value) { return ''; }
        if ((typeof(value) === 'object') && value.time) { value = value.time; }
        if(value < 2696400000){ value *= 1000; }
        var date = new Date(value);
        date.setTime(value + Renderer.timestampOffset);
        return Ext.util.Format.date(date, 'timestamp_fmt'.t());
    },

    interfaceMap: null,
    interfaceLastUpdated: null,
    interfaceMaxAge: 30 * 1000,
    interface: function (value) {
        if (!Rpc.exists('rpc.reportsManager')) {
            return value.toString();
        }
        var currentTime = new Date().getTime();
        if (Ung.util.Renderer.interfaceMap === null ||
            Ung.util.Renderer.interfaceLastUpdated === null ||
            ( ( Ung.util.Renderer.interfaceLastUpdated + Ung.util.Renderer.interfaceMaxAge ) < currentTime ) ){
            var interfacesList = [], i;

            try {
                interfacesList = Rpc.directData('rpc.reportsManager.getInterfacesInfo').list;
            } catch (ex) {
                console.log(ex);
            }

            Ung.util.Renderer.interfaceMap = {};
            for (i = 0; i < interfacesList.length; i += 1) {
                Ung.util.Renderer.interfaceMap[interfacesList[i].interfaceId] = interfacesList[i].name + " [" + interfacesList[i].interfaceId + "]";
            }
            Ung.util.Renderer.interfaceLastUpdated = currentTime;
        }
        if(value == Renderer.listKey){
            return Ung.util.Renderer.interfaceMap;
        }else{
            return ( value && value != -1 ) ? Ung.util.Renderer.interfaceMap[value] || value.toString() : 'None'.t();
        }
    },

    tags: function (value, metaData) {
        if( value != null &&
            value != "" ){
            if( typeof(value) == 'string' ){
                value = Ext.decode( value );
            }
            if( value && value.list ) {
                var str = [], tip = [], name;
                Ext.Array.each(value.list, function (tag) {
                    name = Ext.String.htmlEncode(tag.name);
                    str.push('<div class="tag-item">' + name + '</div>');
                    if ( tag.expirationTime == 0 )
                        tip.push('<strong>' + name + '</strong> - ' + 'Never'.t());
                    else
                        tip.push('<strong>' + name + '</strong> - ' + Ext.Date.format(new Date(tag.expirationTime), 'timestamp_fmt'.t()));
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
        if( value === null){
            value = 0;
        }
        value = parseInt( value, 10 );
        var size = Ung.util.Renderer.datasizeMap[Ung.util.Renderer.datasizeMap.length-1];
        for( var i = 0; i < Ung.util.Renderer.datasizeMap.length; i++){
            size = Ung.util.Renderer.datasizeMap[i];
            if( value >= size[0] || value <= (0-size[0])){
                break;
            }
        }
        if( ( value == 0 ) ||
            ( size[0] == 1 ) ){
            return value + ' ' + size[1];
        }else{
            var dividedValue = ( value / size[0] ).toFixed(2).toString();
            if(dividedValue.substring(dividedValue.length - 3) == '.00'){
                dividedValue = dividedValue.substring(0, dividedValue.length - 3);
            }
            return dividedValue + ' ' + size[1];
        }
    },

    datasizeoptional: function(value){
        if( value === 0 || value === '' ){
            return '';
        }
        return Renderer.datasize(value);
    },

    countMap: [
        [ 1125899906842624, 'P'.t() ],
        [ 1099511627776, 'T'.t() ],
        [ 1073741824, 'G'.t() ],
        [ 1048576, 'M'.t() ] ,
        [ 1024, 'K'.t() ],
        [ 1, '' ]
    ],
    count: function( value ){
        // walk map looking at key.  If larger then divide and use units
        if( value === null){
            value = 0;
        }
        value = parseInt( value, 10 );
        var size = Ung.util.Renderer.countMap[Ung.util.Renderer.countMap.length - 1];
        for( var i = 0; i < Ung.util.Renderer.countMap.length; i++){
            size = Ung.util.Renderer.countMap[i];
            if( value >= size[0] || value <= (0-size[0])){
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

    timeIntervalMap: {
        86400: 'Daily'.t(),
        604800: 'Weekly'.t(),
        1: 'Week to Date'.t(),
        2419200: 'Monthly'.t(),
        2: 'Month to Date'.t()
    },
    timeInterval: function ( value ){
        if(value == Renderer.listKey){
            return Renderer.timeIntervalMap;
        }
        if( value in Ung.util.Renderer.timeIntervalMap ){
            return Ung.util.Renderer.timeIntervalMap[value];
        }
        return value;
    },

    elapsedTime: function( value ){
        var total = parseInt(value / 1000,10);
        var days = (parseInt(total / (3600 * 24),10));
        var hours = (parseInt(total / 3600,10) % 24);
        var minutes = (parseInt(total / 60,10) % 60);
        var seconds = parseInt(total % 60,10);
        var result = (days > 0 ? ( days < 10 ? '0' + days : days) + ':' : '' ) + (hours < 10 ? "0" + hours : hours) + ":" + (minutes < 10 ? "0" + minutes : minutes) + ":" + (seconds  < 10 ? "0" + seconds : seconds);
        return result;
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
        if(value == Renderer.listKey){
            return Renderer.dayOfWeekMap;
        }
        if( value in Ung.util.Renderer.dayOfWeekMap ){
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
        if(value == Renderer.listKey){
            return Renderer.priorityMap;
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
        142: 'ROHC [142]',
        default: 'Unknown'.t()
    },

    protocol: function (value) {
        if(value == Renderer.listKey){
            return Renderer.protocolsMap;
        }else{
            return value ? Ung.util.Renderer.protocolsMap[value] || value.toString() : '';
        }
    },

    settingsFile: function( value ){
        value = value.replace( /^.*\/settings\//, '' );
        value = value.replace( /^.*\/conf\//, '' );
        return value;
    },

    id: function(value){
        return ( value < 0 || value === undefined ) ? 'new'.t() : value;
    },

    policiesMap: null,
    policy: function (value) {
        if(Renderer.policiesMap == null){
            Renderer.policiesMap = {};
            var policiesInfo = null;
            if(Rpc.exists('rpc.reportsManager')){
                policiesInfo = Rpc.directData('rpc.reportsManager.getPoliciesInfo');
            }else{
                policiesInfo = Rpc.directData('rpc.appManager').app('policy-manager').getPoliciesInfo();
            }
            if(policiesInfo && policiesInfo.list){
                policiesInfo.list.forEach(function(policy){
                    Renderer.policiesMap[policy.policyId] = policy.name;
                });
            }
        }
        if(value == Renderer.listKey){
            return Renderer.policiesMap;
        }
        if(value === 0){
            return 'None'.t();
        }
        if(!value || !( value in Renderer.policiesMap )){
            return '';
        }
        return Ext.String.format('{0} [{1}]'.t(), Renderer.policiesMap[parseInt(value, 10)] || value.toString(), value);
    },
    policy_id: function(value){
        return Renderer.policy(value);
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
        B: 'in Temporary Unblocked list'.t(),
        F: 'in Rules list'.t(),
        default: 'no rule applied'.t()
    },
    httpReason: function( value ) {
        if(Ext.isEmpty(value)) {
            return '';
        }
        if(value == Renderer.listKey){
            return Renderer.httpReasonMap;
        }else{
            return Ext.String.format(
                    Renderer.mapValueFormat,
                    ( value in Renderer.httpReasonMap ) ? Renderer.httpReasonMap[value] : Renderer.httpReasonMap['default'],
                    value
            );
        }
    },

    webCategoryMap: {},
    webCategory: function(value, row, record){
        var policyId = record && record.get && record.get('policy_id') ? record.get('policy_id') : 1;
        if(!Renderer.webCategoryMap[value]){
            var categoryInfo = Rpc.directData('rpc.reportsManager.getReportInfo', 'web-filter', policyId, "categories");
            if(!categoryInfo){
                categoryInfo = Rpc.directData('rpc.reportsManager.getReportInfo', 'web-monitor', policyId, "categories");
            }

            if(categoryInfo && categoryInfo["list"]){
                categoryInfo["list"].forEach( function(rule){
                    Renderer.webCategoryMap[rule["id"]] = rule["name"] ? rule["name"] : ( rule["string"] ? rule["string"] : rule["description"] );
                });
            }
            if(!Renderer.webCategoryMap[value] && (value != Renderer.listKey)){
                // If category cannot be found, don't just keep coming back for more.
                Renderer.webCategoryMap[value] = 'Unknown'.t();
            }
        }
        if(value == Renderer.listKey){
            var values = [];
            Object.values(Renderer.webCategoryMap).sort().forEach( function(value){
                for(var key in Renderer.webCategoryMap){
                    if(Renderer.webCategoryMap[key] == value){
                        values.push([key,value]);
                    }
                }
            });
            return values;
        }else{
            return Renderer.webCategoryMap[value];
        }
    },

    /**
     * The webfilter_rule_id is very overloaded with numeric indexes for:
     * -    Pass sites
     * -    Block sites
     * -    Pass clients
     * -    Filter rules
     *
     * The map is organized as:
     *     policy->source->key=value
     */
    webRuleMap: {},
    webRule: function(value, row, record){
        var policyId = record && record.get && record.get('policy_id') ? record.get('policy_id') : 1;
        var webFilterReason = record && record.get && record.get('web_filter_reason') ? record.get('web_filter_reason') : 'N';
        var reasonSource = "categories";
        switch(webFilterReason){
            case 'D':
            case 'N':
                return '';
            case 'U':
                reasonSource = "blockedUrls";
                break;
            case 'I':
            case 'R':
                reasonSource = "passedUrls";
                break;
            case 'C':
                reasonSource = "passedClients";
                break;
            case 'T':
                reasonSource = "searchTerms";
                break;
            case 'F':
                reasonSource = "filterRules";
                break;
        }

        if(!Renderer.webRuleMap[policyId] ||
            !Renderer.webRuleMap[policyId][reasonSource] ||
            (value != Renderer.listKey && !Renderer.webRuleMap[policyId][reasonSource][value])){

            var categoryInfo = Rpc.directData('rpc.reportsManager.getReportInfo', 'web-filter', policyId, reasonSource);
            if(!categoryInfo){
                categoryInfo = Rpc.directData('rpc.reportsManager.getReportInfo', 'web-monitor', policyId, reasonSource);
            }

            if(categoryInfo && categoryInfo["list"]){
                categoryInfo["list"].forEach( function(rule){
                    if(!Renderer.webRuleMap[policyId]){
                        Renderer.webRuleMap[policyId] = {};
                    }
                    if(!Renderer.webRuleMap[policyId][reasonSource]){
                        Renderer.webRuleMap[policyId][reasonSource] = {};
                    }
                    Renderer.webRuleMap[policyId][reasonSource][rule["id"] ? rule["id"] : rule["ruleId"]] = rule["name"] ? rule["name"] : ( rule["string"] ? rule["string"] : rule["description"] );
                });
            }
            if(!Renderer.webRuleMap[policyId][reasonSource][value]){
                // If category cannot be found, don't just keep coming back for more.
                Renderer.webRuleMap[policyId][reasonSource][value] = 'Unknown'.t();
            }
        }
        if(value == Renderer.listKey){
            return Renderer.webRuleMap[policyId][reasonSource];
        }else{
            return Renderer.webRuleMap[policyId][reasonSource][value];
        }
    },

    sessionSpeed: function(value){
        return Math.round(value * 10 )/ 10;
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
                Renderer.mapValueFormat,
                ( value in Renderer.icmpMap ) ? Renderer.icmpMap[value] : Renderer.icmpMap['default'],
                value
        );
    },

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
                Renderer.mapValueFormat,
                ( value in Renderer.countryMap ) ? Renderer.countryMap[value] : Renderer.countryMap['default'],
                value
        );
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
                Renderer.mapValueFormat,
                ( value in Renderer.quotaActionMap ) ? Renderer.quotaActionMap[value] : Renderer.quotaActionMap['default'],
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
        return ( value in Renderer.captivePortalEventInfoMap ) ? Renderer.captivePortalEventInfoMap[value] : Renderer.captivePortalEventInfoMap['default'];
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
        return ( value in Renderer.authTypeMap ) ? Renderer.authTypeMap[value] : Renderer.authTypeMap['default'];
    },

    configurationBackupSuccessMap:{
        true: 'success'.t(),
        default: 'failed'.t()
    },
    configurationBackupSuccess: function( value ){
        return ( value in Renderer.configurationBackupSuccessMap ) ? Renderer.configurationBackupSuccessMap[value] : Renderer.configurationBackupSuccessMap['default'];
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
        return ( value in Renderer.loginFailureReasonMap ) ? Renderer.loginFailureReasonMap[value] : Renderer.loginFailureReasonMap['default'];
    },

    loginSuccess: function( value ){
        return value ?  'success'.t() : 'failed'.t();
    },

    loginFrom: function( value ){
        return value ?  'local'.t() : 'remote'.t();
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
        return ( value in Renderer.directoryConnectorActionMap ) ? Renderer.directoryConnectorActionMap[value] : Renderer.directoryConnectorActionMap['default'];
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
        return ( value in Renderer.directoryConnectorActionSourceMap ) ? Renderer.directoryConnectorActionSourceMap[value] : Renderer.directoryConnectorActionSourceMap['default'];
    },

    adBlockerActionMap:{
        B: 'block'.t(),
        default: 'pass'.t()
    },
    adBlockerAction: function( value ){
        if(Ext.isEmpty(value)) {
            return '';
        }
        return ( value in Renderer.adBlockerActionMap ) ? Renderer.adBlockerActionMap[value] : Renderer.adBlockerActionMap['default'];
    },

    bandwidthControlRule: function( value ){
        return Ext.isEmpty(value) ? 'none'.t() : value;
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
                Renderer.mapValueFormat,
                ( value in Renderer.emailActionMap ) ? Renderer.emailActionMap[value] : Renderer.emailActionMap['default'],
                value
        );
    },

});
