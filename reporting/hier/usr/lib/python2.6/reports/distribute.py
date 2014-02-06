from datetime import timedelta

def distributeValues(row,valueMap,time_interval, distributeFunc):
    """ Evenly distributes the values for the given row based on the time interval"""
    start_date = row[0]
    end_date = row[1] or start_date
    diff = end_date - start_date
    chunks = (diff.seconds+time_interval)/ time_interval
    for i in range(chunks):
        current_date = start_date + timedelta(seconds=i*time_interval)
        distributeFunc(row, valueMap,current_date, chunks)
