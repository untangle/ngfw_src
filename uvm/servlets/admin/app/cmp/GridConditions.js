Ext.define('Ung.cmp.GridConditions', {
    singleton: true,
    alternateClassName: 'Condition',

    dstLocal: { name: 'DST_LOCAL', displayName: 'Destined Local'.t(), type: 'boolean' },
    dstIntf: { name: 'DST_INTF', displayName: 'Destination Interface'.t(), type: 'checkboxgroup', values: Util.getInterfaceList(true, true) },
    dstAddr: { name: 'DST_ADDR', displayName: 'Destination Address'.t(), type: 'textfield', vtype:'ipMatcher' },
    dstPort: { name: 'DST_PORT', displayName: 'Destination Port'.t(), type: 'textfield', vtype:'portMatcher' },
    srcIntf: { name: 'SRC_INTF', displayName: 'Source Interface'.t(), type: 'checkboxgroup', values: Util.getInterfaceList(true, true) },
    srcAddr: { name: 'SRC_ADDR', displayName: 'Source Address'.t(), type: 'textfield', vtype:'ipMatcher' },
    srcPort: { name: 'SRC_PORT', displayName: 'Source Port'.t(), type: 'numberfield', vtype:'portMatcher' },
    // srcMac: { name: 'SRC_MAC' , displayName: 'Source MAC'.t(), type: 'textfield' },
    tagged: { name: 'TAGGED', displayName: 'Tagged'.t(), type: 'textfield', visible: true},

    // { name: 'USERNAME',displayName: 'Username'.t(), type: "editor", editor: Ext.create('Ung.UserEditorWindow',{}), visible: true},
    hostHostname:      { name: 'HOST_HOSTNAME', displayName: 'Host Hostname'.t(), type: 'textfield', visible: true },
    hostMac:           { name: 'HOST_MAC', displayName: 'Host MAC Address'.t(), type: 'textfield', visible: true },
    hostMacVendor:     { name: 'HOST_MAC_VENDOR', displayName: 'Host MAC Vendor'.t(), type: 'textfield', visible: true },
    hostPenalty:       { name: 'HOST_IN_PENALTY_BOX', displayName: 'Host in Penalty Box'.t(), type: 'boolean', visible: true },
    hostNoQuota:       { name: 'HOST_HAS_NO_QUOTA', displayName: 'Host has no Quota'.t(), type: 'boolean', visible: true },
    hostQuotaExceeded: { name: 'HOST_QUOTA_EXCEEDED', displayName: 'Host has exceeded Quota'.t(), type: 'boolean', visible: true },
    hostQuotaAtt:      { name: 'HOST_QUOTA_ATTAINMENT', displayName: 'Host Quota Attainment'.t(), type: 'textfield', visible: true },

    clientHostname:      { name: 'CLIENT_HOSTNAME', displayName: 'Client Hostname'.t(), type: 'textfield', visible: rpc.isExpertMode },
    clientMacVendor:     { name: 'CLIENT_MAC_VENDOR', displayName: 'Client MAC Vendor'.t(), type: 'textfield', visible: true },
    clientPenalty:       { name: 'CLIENT_IN_PENALTY_BOX', displayName: 'Client in Penalty Box'.t(), type: 'boolean', visible: rpc.isExpertMode },
    clientNoQuota:       { name: 'CLIENT_HAS_NO_QUOTA', displayName: 'Client has no Quota'.t(), type: 'boolean', visible: rpc.isExpertMode },
    clientQuotaExceeded: { name: 'CLIENT_QUOTA_EXCEEDED', displayName: 'Client has exceeded Quota'.t(), type: 'boolean', visible: rpc.isExpertMode },
    clientQuotaAtt:      { name: 'CLIENT_QUOTA_ATTAINMENT', displayName: 'Client Quota Attainment'.t(), type: 'textfield', visible: rpc.isExpertMode },

    serverHostname:      { name: 'SERVER_HOSTNAME', displayName: 'Server Hostname'.t(), type: 'textfield', visible: rpc.isExpertMode },
    serverMacVendor:     { name: 'SERVER_MAC_VENDOR', displayName: 'Server MAC Vendor'.t(), type: 'textfield', visible: true },
    serverPenalty:       { name: 'SERVER_IN_PENALTY_BOX', displayName: 'Server in Penalty Box'.t(), type: 'boolean', visible: rpc.isExpertMode },
    serverQuotaExceeded: { name: 'SERVER_QUOTA_EXCEEDED', displayName: 'Server has exceeded Quota'.t(), type: 'boolean', visible: rpc.isExpertMode },
    serverNoQuota:       { name: 'SERVER_HAS_NO_QUOTA', displayName: 'Server has no Quota'.t(), type: 'boolean', visible: rpc.isExpertMode },
    serverQuotaAtt:      { name: 'SERVER_QUOTA_ATTAINMENT', displayName: 'Server Quota Attainment'.t(), type: 'textfield', visible: rpc.isExpertMode },

    srcMac: { name: 'SRC_MAC', displayName: 'Client MAC Address'.t(), type: 'textfield', visible: true },
    dstMac: { name: 'DST_MAC', displayName: 'Server MAC Address'.t(), type: 'textfield', visible: true },

    userNoQuota: { name: 'USER_HAS_NO_QUOTA', displayName: 'User has no Quota'.t(), type: 'boolean', visible: true },
    userQuotaExceeded: { name: 'USER_QUOTA_EXCEEDED', displayName: 'User has exceeded Quota'.t(), type: 'boolean', visible: true },
    userQuotaAtt: { name: 'USER_QUOTA_ATTAINMENT', displayName: 'User Quota Attainment'.t(), type: 'textfield', visible: true },

    httpHost:          { name: 'HTTP_HOST', displayName: 'HTTP: Hostname'.t(), type: 'textfield', visible: true },
    httpReferer:       { name: 'HTTP_REFERER', displayName: 'HTTP: Referer'.t(), type: 'textfield', visible: true },
    httpUri:           { name: 'HTTP_URI', displayName: 'HTTP: URI'.t(), type: 'textfield', visible: true },
    httpUrl:           { name: 'HTTP_URL', displayName: 'HTTP: URL'.t(), type: 'textfield', visible: true },
    httpContentType:   { name: 'HTTP_CONTENT_TYPE', displayName: 'HTTP: Content Type'.t(), type: 'textfield', visible: true },
    httpContentLength: { name: 'HTTP_CONTENT_LENGTH', displayName: 'HTTP: Content Length'.t(), type: 'textfield', visible: true },
    httpUserAgent:     { name: 'HTTP_USER_AGENT', displayName: 'HTTP: Client User Agent'.t(), type: 'textfield', visible: true },
    httpUserAgentOs:   { name: 'HTTP_USER_AGENT_OS', displayName: 'HTTP: Client User OS'.t(), type: 'textfield', visible: false },

    appCtrlApp:          { name: 'APPLICATION_CONTROL_APPLICATION', displayName: 'Application Control: Application'.t(), type: 'textfield', visible: true },
    appCtrlCategory:     { name: 'APPLICATION_CONTROL_CATEGORY', displayName: 'Application Control: Application Category'.t(), type: 'textfield', visible: true },
    appCtrlProto:        { name: 'APPLICATION_CONTROL_PROTOCHAIN', displayName: 'Application Control: Protochain'.t(), type: 'textfield', visible: true },
    appCtrlDetail:       { name: 'APPLICATION_CONTROL_DETAIL', displayName: 'Application Control: Detail'.t(), type: 'textfield', visible: true },
    appCtrlConfidence:   { name: 'APPLICATION_CONTROL_CONFIDENCE', displayName: 'Application Control: Confidence'.t(), type: 'textfield', visible: true },
    appCtrlProductivity: { name: 'APPLICATION_CONTROL_PRODUCTIVITY', displayName: 'Application Control: Productivity'.t(), type: 'textfield', visible: true },
    appCtrlRisk:         { name: 'APPLICATION_CONTROL_RISK', displayName: 'Application Control: Risk'.t(), type: 'textfield', visible: true },

    protocolSignature:   { name: 'PROTOCOL_CONTROL_SIGNATURE', displayName: 'Application Control Lite: Signature'.t(), type: 'textfield', visible: true },
    protocolCategory:    { name: 'PROTOCOL_CONTROL_CATEGORY', displayName: 'Application Control Lite: Category'.t(), type: 'textfield', visible: true },
    protocolDescription: { name: 'PROTOCOL_CONTROL_DESCRIPTION', displayName: 'Application Control Lite: Description'.t(), type: 'textfield', visible: true },
    // { name: 'DIRECTORY_CONNECTOR_GROUP', displayName: 'Directory Connector: User in Group'.t(), type: "editor", editor: Ext.create('Ung.GroupEditorWindow',{}), visible: true } ,
    // { name: 'REMOTE_HOST_COUNTRY', displayName: 'Client Country'.t(), type: "editor", editor: Ext.create('Ung.CountryEditorWindow',{}), visible: true } ,
    // { name: 'CLIENT_COUNTRY', displayName: 'Client Country'.t(), type: "editor", editor: Ext.create('Ung.CountryEditorWindow',{}), visible: rpc.isExpertMode } ,
    // { name: 'SERVER_COUNTRY', displayName: 'Server Country'.t(), type: "editor", editor: Ext.create('Ung.CountryEditorWindow',{}), visible: rpc. isExpertMode }





    webFilterCategory:   { name: 'WEB_FILTER_CATEGORY', displayName: 'Web Filter: Category'.t(), type: 'textfield', visible: true },
    webFilterCategDesc:  { name: 'WEB_FILTER_CATEGORY_DESCRIPTION', displayName: 'Web Filter: Category Description'.t(), type: 'textfield', visible: true },
    webFilterFlagged:    { name: 'WEB_FILTER_FLAGGED', displayName: 'Web Filter: Website is Flagged'.t(), type: 'boolean', visible: true },


    protocol: function (protocols) {
        return { name: 'PROTOCOL', displayName: 'Protocol'.t(), type: 'checkboxgroup', values: protocols };
    },

    fieldCondition: {
        name: 'FIELD_CONDITION',
        displayName: 'Field condition'.t(),
        //type: "editor",
        // editor: Ext.create('Ung.FieldConditionWindow',{}),
        type: "textfield",
        // editor: Ext.create('Ung.FieldConditionWindow',{}),
        visible: true,
        disableInvert: true,
        allowMultiple: true,
        allowBlank: false,
        formatValue: function(value) {
            var result= "";
            if(value) {
                result = value.field + " " + value.comparator + " " + value.value;
            }
            return result;
        }
    }

});

