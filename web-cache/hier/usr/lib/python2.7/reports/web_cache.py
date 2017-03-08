import reports.sql_helper as sql_helper

@sql_helper.print_timing
def generate_tables():
    __create_web_cache_stats()

@sql_helper.print_timing
def cleanup_tables(cutoff):
    sql_helper.clean_table("web_cache_stats", cutoff)

@sql_helper.print_timing
def __create_web_cache_stats():
    sql_helper.create_table("""\
CREATE TABLE reports.web_cache_stats (
    time_stamp timestamp without time zone,
    hits bigint,
    misses bigint,
    bypasses bigint,
    systems bigint,
    hit_bytes bigint,
    miss_bytes bigint,
    event_id bigserial)""",["event_id"],["time_stamp"])
