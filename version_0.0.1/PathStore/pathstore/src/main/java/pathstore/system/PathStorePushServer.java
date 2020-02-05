/**********
 *
 * Copyright 2019 Eyal de Lara, Seyed Hossein Mortazavi, Mohammad Salehe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ***********/
package pathstore.system;


import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Batch;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;

import pathstore.common.PathStoreProperties;
import pathstore.common.Role;
import pathstore.util.PathStoreStatus;
import pathstore.util.SchemaInfo;
import pathstore.util.SchemaInfo.Column;
import pathstore.util.SchemaInfo.Table;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * TODO: Comment
 */
public class PathStorePushServer extends Thread {
    private final Logger logger = LoggerFactory.getLogger(PathStorePushServer.class);

    public PathStorePushServer() {
    }


    private Insert createInsert(Row row, String keyspace, String tablename, List<Column> columns) {
        logger.info("Create Insert at " + keyspace + ":" + tablename + " " +
                columns.stream().map(Object::toString).collect(Collectors.joining(", ")));
        Insert insert = QueryBuilder.insertInto(keyspace, tablename);

        for (Column column : columns) {
            if (!row.isNull(column.column_name))
                if (!column.column_name.equals("pathstore_node"))
                    if (!column.column_name.equals("pathstore_insert_sid")) //Hossein
                        //if(row.getObject(column.column_name)==null)
                        //	{
                        //insert.value("pathstore_node", PathStoreProperties.getInstance().NodeID);
                        //	continue;
                        //}
                        if (column.column_name.compareTo("pathstore_parent_timestamp") == 0)
                            insert.value(column.column_name, QueryBuilder.now());
                        else
                            insert.value(column.column_name, row.getObject(column.column_name));
        }
        insert.value("pathstore_node", PathStoreProperties.getInstance().NodeID);

        return insert;
    }

    private Delete createDelete(Row row, String keyspace, String tablename, List<Column> columns) {
        logger.info("Create Insert at " + keyspace + ":" + tablename + " " +
                columns.stream().map(Object::toString).collect(Collectors.joining(", ")));
        Delete delete = QueryBuilder.delete("pathstore_dirty").from(keyspace, tablename);

        for (Column column : columns)
            if (column.kind.compareTo("regular") != 0)
                delete.where(QueryBuilder.eq(column.column_name, row.getObject(column.column_name)));

        return delete;
    }

    private void push() {
        //		logger.info("Run push");
        // TODO: shouldn't we connect once instead of each time?
        Session parent = PathStoreParentCluster.getInstance().connect();
        Session local = PathStorePriviledgedCluster.getInstance().connect();

        try {
            for (String keyspace : SchemaInfo.getInstance().getSchemaInfo().keySet()) {

                //TODO: Temporary
                if(keyspace.equals("pathstore_applications")) continue;

                HashMap<Table, List<Column>> tables = SchemaInfo.getInstance().getSchemaInfo().get(keyspace);

                for (Table table : tables.keySet()) {
                    if (table.getTable_name().startsWith("view_") || table.getTable_name().startsWith("local_"))
                        continue;

                    Select select = QueryBuilder.select().all().from(keyspace, table.getTable_name());
                    select.where(QueryBuilder.eq("pathstore_dirty", true));

                    ResultSet results = local.execute(select);

                    List<Column> columns = tables.get(table);

                    Batch insertBatch = QueryBuilder.batch();
                    Batch deleteBatch = QueryBuilder.batch();

                    int insertBatchSize = 0;
                    int deleteBatchSize = 0;

                    for (Row row : results) {

                        Insert insert = createInsert(row, keyspace, table.getTable_name(), columns);
                        Delete delete = createDelete(row, keyspace, table.getTable_name(), columns);

                        String str_insert = insert.toString();
                        String str_delete = delete.toString();

                        //System.out.println("insert: " + str_insert );

                        if (str_insert.length() > PathStoreProperties.getInstance().MaxBatchSize ||
                                str_delete.length() > PathStoreProperties.getInstance().MaxBatchSize) {
                            logger.info("Executing parent insert and local delete");
                            parent.execute(insert);
                            local.execute(delete);
                        } else {
                            if (insertBatchSize + str_insert.length() > PathStoreProperties.getInstance().MaxBatchSize ||
                                    deleteBatchSize + str_delete.length() > PathStoreProperties.getInstance().MaxBatchSize) {
                                logger.info("Executing parent insert and local delete");
                                parent.execute(insertBatch);
                                local.execute(deleteBatch);

                                insertBatch = QueryBuilder.batch();
                                deleteBatch = QueryBuilder.batch();

                                insertBatchSize = 0;
                                deleteBatchSize = 0;


                            }
                            //System.out.println("adding: " + insert);

                            insertBatch.add(insert);
                            insertBatchSize += str_insert.length();

                            deleteBatch.add(delete);
                            deleteBatchSize += str_delete.length();

                        }
                    }
                    if (insertBatchSize > 0) {
                        try {
                            logger.info("Executing parent insert and local delete");
                            parent.execute(insertBatch);
                            local.execute(deleteBatch);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } finally {
            //local.close();
            //parent.close();
        }

    }

    synchronized public void run() {
        logger.info("Spawned pathstore push server thread");

        while (true) {
            try {
                push();
                this.wait(PathStoreProperties.getInstance().PushSleep);
            } catch (InterruptedException e) {
                System.err.println("PathStorePushServer exception: " + e.toString());
                e.printStackTrace();
            }
        }
    }


    private static void parseCommandLineArguments(String args[]) {
        Options options = new Options();

        options.addOption(Option.builder("n").longOpt("nodeid")
                .desc("Number")
                .hasArg()
                .argName("nodeid")
                .build());


        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
            return;
        }

        if (cmd.hasOption("nodeid"))
            PathStoreProperties.getInstance().NodeID = Integer.parseInt(cmd.getOptionValue("nodeid"));


    }

    public static void main(String args[]) {
        try {

            parseCommandLineArguments(args);

            PathStorePushServer server = new PathStorePushServer();
            server.run();
            server.join();

        } catch (Exception e) {
            System.err.println("PathStorePullServer exception: " + e.toString());
            e.printStackTrace();
        }
    }


}
