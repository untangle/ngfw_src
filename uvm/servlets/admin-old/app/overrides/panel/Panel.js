Ext.define('Ung.overrides.panel.Panel', {
    override: 'Ext.panel.Panel',
    listeners:{
        afterlayout: function(){
            if(Ext.String.startsWith(this.$className, 'Ung.config') ||
               Ext.String.startsWith(this.$className, 'Ung.apps') ||
               Ext.String.startsWith(this.$className, 'Ung.view.extra')){
                var me = this;
                if(!me.scrollableTaskRan){
                    if(!me.scrollableTask){
                        me.scrollableTask = new Ext.util.DelayedTask( Ext.bind(function(){
                            if(Util.isDestroyed(me)){
                                return;
                            }
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