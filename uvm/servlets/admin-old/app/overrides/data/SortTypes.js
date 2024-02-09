Ext.define('Ung.overrides.data.SortTypes', {
    override: 'Ext.data.SortTypes',

    asTimestamp: function(value) {
        if( value &&
            ( typeof( value ) == 'object' ) &&
            value.time ){
            value = value.time;
        }
        return value;
    },

    // Ip address sorting. may contain netmask.
    asIp: function(value){
        if(Ext.isEmpty(value)) {
            return null;
        }
        var i,
            len,
            parts = ( "" + value ).replace(/\//g,".").split('.');
        for(i = 0, len = parts.length; i < len; i++){
            parts[i] = Ext.String.leftPad(parts[i], 3, '0');
        }
        return parts.join('.');
    },

    // Ext string sorter by default considers null characters as as highest precidence
    // so if you sort ascending, you see empty values first (e.g.,null, a, b, c)
    //
    // However, we almost never want to see empty values first as
    // they're effectively meaningless.  We'd rather see those values
    // considered at lowest precidence (e.g., x, y, z, null).
    asUnString: function(s) {
        return ( ( s != null ) && !Ext.isEmpty(s) ) ? String(s).toUpperCase() : '~';
    },

    // combines the IP and String sorting
    asHostname: function (s) {
        if (!s) { return '\u0000'; }
        if (/^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/.test(s)) {
            var i, len, parts = ( "" + s ).replace(/\//g,".").split('.');
            for (i = 0, len = parts.length; i < len; i++) {
                parts[i] = Ext.String.leftPad(parts[i], 3, '0');
            }
            return parts.join('.');
        }
        return String(s).toUpperCase();
    }

});
