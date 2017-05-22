Ext.define('Ung.overrides.data.SortTypes', {
    override: 'Ext.data.SortTypes',

    asTimestamp: function(value) {
        if( ( typeof( value ) == 'object' ) &&
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
    }

});