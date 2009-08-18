Untangle::RemoteUvmContext.nodeManager.nodeInstances('untangle-node-spamassassin').each do |nid| 
  settings = Untangle::RemoteUvmContext.nodeManager.nodeContext(nid).node.getBaseSettings()
  settings['smtpConfig']['msgSizeLimit'] = '500000' 
  Untangle::RemoteUvmContext.nodeManager.nodeContext(nid).node.setBaseSettings(settings) 
end
