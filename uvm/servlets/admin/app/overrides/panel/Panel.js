Ext.define('Ung.overrides.panel.Panel', {
    override: 'Ext.panel.Panel',
    listeners:{
        afterlayout: function(){
            if(this.$className.startsWith('Ung.config') ||
               this.$className.startsWith('Ung.apps') ||
               this.$className.startsWith('Ung.view.extra')){
                var me = this;
                if(!me.scrollableTaskRan){
                    if(!me.scrollableTask){
                        me.scrollableTask = new Ext.util.DelayedTask( Ext.bind(function(){
                            if(this.container && this.container.dom){
                                me.setScrollable('y');
                            }
                            me.scrollableTaskRan = true;
                        }, me) );
                    }
                    me.scrollableTask.delay( 100 );
                }
            }
        }
    }
});