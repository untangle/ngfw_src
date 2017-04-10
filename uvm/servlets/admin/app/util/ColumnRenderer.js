Ext.define('Ung.util.ColumnRenderer', {
    alternateClassName: 'ColumnRenderer',
    singleton: true,

    protocolsMap: {
        0: 'HOPOPT (0)',
        1: 'ICMP (1)',
        2: 'IGMP (2)',
        3: 'GGP (3)',
        4: 'IP-in-IP (4)',
        5: 'ST (5)',
        6: 'TCP (6)',
        7: 'CBT (7)',
        8: 'EGP (8)',
        9: 'IGP (9)',
        10: 'BBN-RCC-MON (10)',
        11: 'NVP-II (11)',
        12: 'PUP (12)',
        13: 'ARGUS (13)',
        14: 'EMCON (14)',
        15: 'XNET (15)',
        16: 'CHAOS (16)',
        17: 'UDP (17)',
        18: 'MUX (18)',
        19: 'DCN-MEAS (19)',
        20: 'HMP (20)',
        21: 'PRM (21)',
        22: 'XNS-IDP (22)',
        23: 'TRUNK-1 (23)',
        24: 'TRUNK-2 (24)',
        25: 'LEAF-1 (25)',
        26: 'LEAF-2 (26)',
        27: 'RDP (27)',
        28: 'IRTP (28)',
        29: 'ISO-TP4 (29)',
        30: 'NETBLT (30)',
        31: 'MFE-NSP (31)',
        32: 'MERIT-INP (32)',
        33: 'DCCP (33)',
        34: '3PC (34)',
        35: 'IDPR (35)',
        36: 'XTP (36)',
        37: 'DDP (37)',
        38: 'IDPR-CMTP (38)',
        39: 'TP++ (39)',
        40: 'IL (40)',
        41: 'IPv6 (41)',
        42: 'SDRP (42)',
        43: 'IPv6-Route (43)',
        44: 'IPv6-Frag (44)',
        45: 'IDRP (45)',
        46: 'RSVP (46)',
        47: 'GRE (47)',
        48: 'MHRP (48)',
        49: 'BNA (49)',
        50: 'ESP (50)',
        51: 'AH (51)',
        52: 'I-NLSP (52)',
        53: 'SWIPE (53)',
        54: 'NARP (54)',
        55: 'MOBILE (55)',
        56: 'TLSP (56)',
        57: 'SKIP (57)',
        58: 'IPv6-ICMP (58)',
        59: 'IPv6-NoNxt (59)',
        60: 'IPv6-Opts (60)',
        62: 'CFTP (62)',
        64: 'SAT-EXPAK (64)',
        65: 'KRYPTOLAN (65)',
        66: 'RVD (66)',
        67: 'IPPC (67)',
        69: 'SAT-MON (69)',
        70: 'VISA (70)',
        71: 'IPCU (71)',
        72: 'CPNX (72)',
        73: 'CPHB (73)',
        74: 'WSN (74)',
        75: 'PVP (75)',
        76: 'BR-SAT-MON (76)',
        77: 'SUN-ND (77)',
        78: 'WB-MON (78)',
        79: 'WB-EXPAK (79)',
        80: 'ISO-IP (80)',
        81: 'VMTP (81)',
        82: 'SECURE-VMTP (82)',
        83: 'VINES (83)',
        84: 'TTP (84)',
        85: 'NSFNET-IGP (85)',
        86: 'DGP (86)',
        87: 'TCF (87)',
        88: 'EIGRP (88)',
        89: 'OSPF (89)',
        90: 'Sprite-RPC (90)',
        91: 'LARP (91)',
        92: 'MTP (92)',
        93: 'AX.25 (93)',
        94: 'IPIP (94)',
        95: 'MICP (95)',
        96: 'SCC-SP (96)',
        97: 'ETHERIP (97)',
        98: 'ENCAP (98)',
        100: 'GMTP (100)',
        101: 'IFMP (101)',
        102: 'PNNI (102)',
        103: 'PIM (103)',
        104: 'ARIS (104)',
        105: 'SCPS (105)',
        106: 'QNX (106)',
        107: 'A/N (107)',
        108: 'IPComp (108)',
        109: 'SNP (109)',
        110: 'Compaq-Peer (110)',
        111: 'IPX-in-IP (111)',
        112: 'VRRP (112)',
        113: 'PGM (113)',
        115: 'L2TP (115)',
        116: 'DDX (116)',
        117: 'IATP (117)',
        118: 'STP (118)',
        119: 'SRP (119)',
        120: 'UTI (120)',
        121: 'SMP (121)',
        122: 'SM (122)',
        123: 'PTP (123)',
        124: 'IS-IS (124)',
        125: 'FIRE (125)',
        126: 'CRTP (126)',
        127: 'CRUDP (127)',
        128: 'SSCOPMCE (128)',
        129: 'IPLT (129)',
        130: 'SPS (130)',
        131: 'PIPE (131)',
        132: 'SCTP (132)',
        133: 'FC (133)',
        134: 'RSVP-E2E-IGNORE (134)',
        135: 'Mobility (135)',
        136: 'UDPLite (136)',
        137: 'MPLS-in-IP (137)',
        138: 'manet (138)',
        139: 'HIP (139)',
        140: 'Shim6 (140)',
        141: 'WESP (141)',
        142: 'ROHC (142)'
    },

    policy_id: function (value) {
        var policy = Ext.getStore('policiestree').findRecord('policyId', value);
        if (policy) {
            return policy.get('name') + '[' + value + ']';
        }
        return 'None'.t() + ' [0]';
    },

    protocol: function (value) {
        return value ? ColumnRenderer.protocolsMap[value] || value.toString() : '';
    },
    protocolStore: function () {
        var store = [];
        Ext.Object.each(this.protocolsMap, function (key, val) {
            store.push([key, val]);
        });
        return store;
    },

    buildInterfaceMap: function () {
        console.log('here');
        var interfacesList = [], i;
        try {
            interfacesList = rpc.reportsManager.getInterfacesInfo().list;
        } catch (ex) {
            console.log(ex);
        }
        interfacesList.push({ interfaceId: 250, name: 'OpenVPN' }); // 0xfa
        interfacesList.push({ interfaceId: 251, name: 'L2TP' }); // 0xfb
        interfacesList.push({ interfaceId: 252, name: 'Xauth' }); // 0xfc
        interfacesList.push({ interfaceId: 253, name: 'GRE' }); // 0xfd

        this.interfaceMap = {};
        for (i = 0; i < interfacesList.length; i += 1) {
            this.interfaceMap[interfacesList[i].interfaceId] = interfacesList[i].name;
        }
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
            interfacesList.push({ interfaceId: 250, name: 'OpenVPN' }); // 0xfa
            interfacesList.push({ interfaceId: 251, name: 'L2TP' }); // 0xfb
            interfacesList.push({ interfaceId: 252, name: 'Xauth' }); // 0xfc
            interfacesList.push({ interfaceId: 253, name: 'GRE' }); // 0xfd

            this.interfaceMap = {};
            for (i = 0; i < interfacesList.length; i += 1) {
                this.interfaceMap[interfacesList[i].interfaceId] = interfacesList[i].name;
            }
        }
        return value ? this.interfaceMap[value] || value.toString() : '';
    }
    // interfaceStore: function () {
    //     if (!this.interfaceMap) {
    //         this.buildInterfaceMap();
    //     }
    //     console.log(this.interfaceMap);
    //     var store = [];
    //     Ext.Object.each(this.interfaceMap, function (key, val) {
    //         store.push([key, val]);
    //     });
    //     return store;
    // }

});
