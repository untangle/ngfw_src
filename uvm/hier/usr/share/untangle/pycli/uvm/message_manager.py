from uvm.manager import Manager
import string

class MessageManager(Manager):
    def __init__(self, uvmContext):
        self.__uvmContext = uvmContext
        self.__messageManager = self.__uvmContext.messageManager()

    def api_get_message_queue(self):
        return self.__messageManager.getMessageQueue()

Manager.managers.append( MessageManager )
