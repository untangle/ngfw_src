nm = Untangle::RemoteUvmContext.nodeManager(); 
tid = nm.nodeInstances( 'untangle-node-openvpn' ).first ; 
if tid.nil?: exit ; end ;
 openvpn = nm.nodeContext( tid ).node ; 

sett = openvpn.getVpnSettings() ; 
openvpn.setVpnSettings(sett) ; 
puts "Saving settings... done"



