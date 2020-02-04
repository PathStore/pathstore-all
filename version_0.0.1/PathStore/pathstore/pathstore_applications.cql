CREATE KEYSPACE IF NOT EXISTS pathstore_applications  WITH replication = {'class' : 'SimpleStrategy', 'replication_factor' : 1 }  AND durable_writes = false;
                        CREATE TABLE pathstore_applications.apps (
                        		appid int PRIMARY KEY, 
                        	    app_name text,
                        	    schema_name text,   
                        	    app_schema text,   
                        time_created timestamp,   
                        	) WITH bloom_filter_fp_chance = 0.01   
                        	    AND caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}   
                        	    AND comment = 'table definitions'   
                        	    AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}   
                        	    AND compression = {'chunk_length_in_kb': '64', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}   
                        	    AND crc_check_chance = 1.0   
                        	    AND dclocal_read_repair_chance = 0.0   
                        	    AND default_time_to_live = 0   
                        	    AND gc_grace_seconds = 604800   
                        	    AND max_index_interval = 2048   
                        	    AND memtable_flush_period_in_ms = 3600000   
                        	    AND min_index_interval = 128   
                        	    AND read_repair_chance = 0.0   
                        	    AND speculative_retry = '99PERCENTILE';
