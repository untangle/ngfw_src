Ext.define('EXTJS_23846.Element', {
    override: 'Ext.dom.Element'
}, function(Element) {
    var supports = Ext.supports,
        proto = Element.prototype,
        eventMap = proto.eventMap,
        additiveEvents = proto.additiveEvents;

    if (Ext.os.is.Desktop && supports.TouchEvents && !supports.PointerEvents) {
        eventMap.touchstart = 'mousedown';
        eventMap.touchmove = 'mousemove';
        eventMap.touchend = 'mouseup';
        eventMap.touchcancel = 'mouseup';

        additiveEvents.mousedown = 'mousedown';
        additiveEvents.mousemove = 'mousemove';
        additiveEvents.mouseup = 'mouseup';
        additiveEvents.touchstart = 'touchstart';
        additiveEvents.touchmove = 'touchmove';
        additiveEvents.touchend = 'touchend';
        additiveEvents.touchcancel = 'touchcancel';

        additiveEvents.pointerdown = 'mousedown';
        additiveEvents.pointermove = 'mousemove';
        additiveEvents.pointerup = 'mouseup';
        additiveEvents.pointercancel = 'mouseup';
    }
});

Ext.define('EXTJS_23846.Gesture', {
    override: 'Ext.event.publisher.Gesture'
}, function(Gesture) {
    var me = Gesture.instance;

    if (Ext.supports.TouchEvents && !Ext.isWebKit && Ext.os.is.Desktop) {
        me.handledDomEvents.push('mousedown', 'mousemove', 'mouseup');
        me.registerEvents();
    }
});
