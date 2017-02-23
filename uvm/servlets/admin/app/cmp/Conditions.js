Ext.define('Ung.cmp.Conditions', {
    singleton: true,
    alternateClassName: 'Cond',

    dstLocal: { name: 'DST_LOCAL', displayName: 'Destined Local'.t(), type: 'boolean' },
    dstIntf: { name: 'DST_INTF', displayName: 'Destination Interface'.t(), type: 'checkboxgroup', values: Util.getInterfaceList(true, true) },
    dstAddr: { name: 'DST_ADDR', displayName: 'Destination Address'.t(), type: 'textfield', vtype:'ipMatcher' },
    dstPort: { name: 'DST_PORT', displayName: 'Destination Port'.t(), type: 'textfield', vtype:'portMatcher' },
    srcIntf: { name: 'SRC_INTF', displayName: 'Source Interface'.t(), type: 'checkboxgroup', values: Util.getInterfaceList(true, true) },
    srcAddr: { name: 'SRC_ADDR', displayName: 'Source Address'.t(), type: 'textfield', vtype:'ipMatcher' },
    srcPort: { name: 'SRC_PORT', displayName: 'Source Port'.t(), type: 'numberfield', vtype:'portMatcher' },
    srcMac: { name: 'SRC_MAC' , displayName: 'Source MAC'.t(), type: 'textfield' },
    protocol: function (protocols) {
        return { name: 'PROTOCOL', displayName: 'Protocol'.t(), type: 'checkboxgroup', values: protocols };
    }

});

