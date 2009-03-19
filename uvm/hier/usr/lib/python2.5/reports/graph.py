from matplotlib.ticker import FuncFormatter

from mx.DateTime import DateTimeDeltaFromSeconds

def __time_of_day_formatter(x, pos):
    t = DateTimeDeltaFromSeconds(x)
    return "%02d:%02d" % (t.hour, t.minute)

time_of_day_formatter = FuncFormatter(__time_of_day_formatter)

even_hours_of_a_day = [i * 7200 for i in range(12)]
