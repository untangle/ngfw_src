Ext.define('Ung.util.Renderer', {
    singleton: true,
    alternateClassName: 'Renderer',

    /*
     * Common column widths
     */
     // Boolean
    booleanWidth: 40,
    // Email address
    emailWidth: 150,
    // Hostname
    hostnameWidth: 120,
    // Numeric identifier
    idWidth: 75,
    // IP Address
    ipWidth: 100,
    // Latitude/longtitude
    locationWidth: 50,
    // MAC address
    macWidth: 110,
    // General purpose
    messageWidth: 120,
    // Port
    portWidth: 70,
    // Data size
    sizeWidth: 50,
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

    boolean: function( value ){
        return ( value == true ) ? 'true' : 'false';
    },

    timestamp: function( value ){
        if( !value ){
            return null;
        }
        if( ( typeof( value ) == 'object' ) &&
            value.time ){
            value = value.time;
        }
        return Ext.util.Format.date(new Date( value ), 'timestamp_fmt'.t());
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
                this.interfaceMap[interfacesList[i].interfaceId] = interfacesList[i].name + " [" + interfacesList[i].interfaceId + "]";
            }
        }
        return value ? this.interfaceMap[value] || value.toString() : '';
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
        for( var i = 0; i < this.datasizeMap.length; i++){
            size = this.datasizeMap[i];
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

    },

    timeIntervalMap: {
        86400: 'Daily'.t(),
        604800: 'Weekly'.t(),
        2419200: 'Monthly'.t()
    },
    timeInterval: function ( value ){
        if( value in this.timeIntervalMap ){
            return this.timeIntervalMap[value];
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
        if( value in this.dayOfWeeklMap ){
            return this.dayOfWeekMap[value];
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
        if( value in this.priorityMap ){
            return this.priorityMap[value];
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
        return value ? this.protocolsMap[value] || value.toString() : '';
    },

});
