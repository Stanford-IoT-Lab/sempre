#!/usr/bin/python

import MySQLdb
import MySQLdb.cursors
import sys
import json

def main():
    conn = MySQLdb.connect(user='sempre', passwd=sys.argv[1],
                           db='thingengine',
                           host='thingengine.crqccvnuyu19.us-west-2.rds.amazonaws.com',
                           ssl=dict(ca='../almond/thingpedia-db-ca-bundle.pem'))
    cursor = conn.cursor(cursorclass=MySQLdb.cursors.DictCursor)
    cursor.execute("select kind, name, channel_type, argnames, types from device_schema ds, device_schema_channels dsc where ds.id = dsc.schema_id and dsc.version = ds.developer_version and kind_type <> 'primary'")
    for row in cursor.fetchall():
        print row['channel_type'], 'tt:' + row['kind'] + '.' + row['name'],
        argnames = json.loads(row['argnames'])
        argtypes = json.loads(row['types'])
        for argname, argtype in zip(argnames, argtypes):
            print argname, argtype,
        print
        
    cursor = conn.cursor(cursorclass=MySQLdb.cursors.DictCursor)
    cursor.execute("select kind from device_schema where kind_type <> 'primary'")
    for row in cursor.fetchall():
        print 'device', 'tt:device.' + row['kind']
        
    cursor = conn.cursor(cursorclass=MySQLdb.cursors.DictCursor)
    cursor.execute("select id from entity_names where not is_well_known")
    for row in cursor.fetchall():
        print 'entity', row['id']
        
main()
