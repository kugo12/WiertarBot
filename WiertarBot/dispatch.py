class EventDispatcher():
    _slots = {}

    @staticmethod
    def slot(event):
        def wrap(func):
            name = event.__name__

            if name not in EventDispatcher._slots:
                EventDispatcher._slots[name] = []

            EventDispatcher._slots[name].append(func)

            return func
        return wrap

    @staticmethod
    async def send_signal(event):
        name = type(event).__name__
        if name in EventDispatcher._slots:
            for func in EventDispatcher._slots[name]:
                await func(event)
