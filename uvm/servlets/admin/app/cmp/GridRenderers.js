Ext.define('Ung.cmp.GridRenderers', {
    singleton: true,
    alternateClassName: 'Renderer',

    boolean: function( value ){
        return '<i class="fa ' + (value ? 'fa-check' : 'fa-minus') + '"></i>';
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