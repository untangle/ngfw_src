from uvm.manager import Manager
import string

class MessageManager(Manager):
    def __init__(self, remoteContext):
        self.__remoteContext = remoteContext
        self.__messageManager = self.__remoteContext.messageManager()

    def api_get_message_queue(self):
        return self.__messageManager.getMessageQueue()

Manager.managers.append( MessageManager )
