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
        return Util.interfacesListNamesMap()[value];
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
    }

});