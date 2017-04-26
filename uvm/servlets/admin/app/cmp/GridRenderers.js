Ext.define('Ung.cmp.GridRenderers', {
    singleton: true,
    alternateClassName: 'Renderer',

    boolean: function( value ){
        return '<i class="fa ' + (value ? 'fa-check' : 'fa-minus') + '"></i>';
    },

    timestamp: function( value ){
        if( !value ){
            return '<i class="fa fa-minus"></i>';
        }
        return Ext.util.Format.date(new Date( value ), 'timestamp_fmt'.t());

    },

    interface: function( value ){
        var interfaceName = Util.interfacesListNamesMap()[value];
        if (interfaceName) {
            return interfaceName + ' [' + value + ']';
        }
        return '';
    },

    tags: function( value ){
        if(value != null){
            if( typeof(value) == 'string' ){
                value = Ext.decode( value );
            }
            if( value &&
                 value.list ){
                return value.list.join(', ');
            }
        }
        return '';
    },

    policy: function ( value ) {
        var policy = Ext.getStore('policiestree').findRecord('policyId', value);
        if (policy) {
            return policy.get('name') + ' [' + value + ']';
        }
        return value;
    }

});
