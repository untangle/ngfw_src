import logging
import os
import sets
import string

class Node:
    def __init__(self, name):
        self.__name = name

    @property
    def name(self):
        return self.__name

    def parents(self):
        return []

class FactTable:
    def __init__(self, name, detail_table, time_column, dimensions, measures):
        self.__name = name
        self.__detail_table = detail_table
        self.__time_column = time_column
        self.__dimensions = dimensions
        self.__measures = measures

    @property
    def name(self):
        return self.__name

    @property
    def measures(self):
        return self.__measures

    def process(self):
        sql_helper.create_partitioned_table(self.__ddl(), 'trunc_time',
                                            start_date, end_date)

        sd = sql_helper.get_update_info(name.__name)
        if None == sd:
            sd = start_date

        sql_helper.run_sql(self.__insert_stmt(), (sd, end_date))

        sql_helper.set_update_info(self.__name, end_date);

    def __ddl(self):
        ddl = 'CREATE TABLE %s (trunc_time timestamp without time zone' \
            % self.__name;
        for c in (self.__dimensions + self.__measures):
            ddl += ", %s %s" % (c.name, c.type)
        ddl += ')'
        return ddl

    def __insert_stmt(self):
        insert_strs = ['trunc_time']
        select_strs = ["date_trunc('minute', #{@time_column})"]
        group_strs = ["date_trunc('minute', #{@time_column})"]

        for c in dimensions:
            insert_strs.append(c.name)
            select_strs.append(c.value_expression)
            group_strs.append(c.name)

        for c in measures.each:
            insert_strs.append(c.name)
            select_strs.append(c.value_expression)

        return """\
INSERT INTO %s (%s)
    SELECT %s FROM %s
    WHERE %s >= ? AND %s < ?
    GROUP BY %s""" % (self.__name, string.join(insert_strs, ','),
                      string.join(select_strs, ','), self.__detail_table,
                      self.__time_column, self.__time_column,
                      string.join(group_strs, ','))

class Column:
    def __init__(self, name, type, value_expression=None):
        self.__name = name
        self.__type = type
        self.__value_expression = value_expression or name

    @property
    def name(self):
        return self.__name

    @property
    def type(self):
        return self.__type

    @property
    def value_expression(self):
        return self.__value_expression

__nodes = {}
__fact_tables = {}

def register_node(node):
    __nodes[node.name] = node

def register_fact_table(fact_table):
    __fact_tables[fact_table.name] = fact_table

def get_fact_table(name):
    __fact_tables.get(name, None)

def process_fact_tables(start_time, end_time):
    for ft in __fact_tables.values():
        ft.process(start_time, end_time)

def init_engine(node_module_dir):
    __get_nodes(node_module_dir)

def setup(start_time, end_time):
    for name in __get_node_partial_order():
        logging.info('doing setup for: %s' % name)
        node = __nodes.get(name, None)
        if not node:
            logger.warn("could not get node %s" % name)
        else:
            node.setup(start_time, end_time)

def __get_node_partial_order():
    available = sets.Set(__nodes.keys());
    list = []

    while len(available):
        name = available.pop()
        __add_node(name, list, available)

    return list

def __add_node(name, list, available):
    node = __nodes.get(name, None)
    if not node:
        logging.warn("node not found %s" % name)
    else:
        for p in node.parents():
            if p in available:
                available.remove(p)
                __add_node(p, list, available)
        list.append(name)

def __get_nodes(node_module_dir):
    for f in os.listdir(node_module_dir):
        if f.endswith('py'):
            (m, e) = os.path.splitext(f)
            __import__('reports.node.%s' % m)

