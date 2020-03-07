package pathstore.test;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import pathstore.system.PathStorePriviledgedCluster;
import pathstore.system.schemaloader.PathStoreSchemaLoaderUtils;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class QueryResponseTimeTest {

  private static final String demo_keyspace =
      ""
          + "CREATE KEYSPACE response_time WITH replication = {'class' : 'SimpleStrategy', 'replication_factor' : 1 }  AND durable_writes = false;\n"
          + "\n"
          + "CREATE TABLE response_time.test (\n"
          + "    id int PRIMARY KEY,\n"
          + "    string text\n"
          + ") WITH bloom_filter_fp_chance = 0.01\n"
          + "    AND caching = {'keys': 'ALL', 'rows_per_partition': 'NONE'}\n"
          + "    AND comment = 'table definitions'\n"
          + "    AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy', 'max_threshold': '32', 'min_threshold': '4'}\n"
          + "    AND compression = {'chunk_length_in_kb': '64', 'class': 'org.apache.cassandra.io.compress.LZ4Compressor'}\n"
          + "    AND crc_check_chance = 1.0\n"
          + "    AND dclocal_read_repair_chance = 0.0\n"
          + "    AND default_time_to_live = 0\n"
          + "    AND gc_grace_seconds = 604800\n"
          + "    AND max_index_interval = 2048\n"
          + "    AND memtable_flush_period_in_ms = 3600000\n"
          + "    AND min_index_interval = 128\n"
          + "    AND read_repair_chance = 0.0\n"
          + "    AND speculative_retry = '99PERCENTILE'";

  private static final int n = 100000;

  private static class Value {
    public final int id;
    public final String text;

    Value(final int id, final String text) {
      this.id = id;
      this.text = text;
    }
  }

  private static void install() {
    Session session = PathStorePriviledgedCluster.getInstance().connect();

    System.out.println("Inserting schema");
    PathStoreSchemaLoaderUtils.parseSchema(demo_keyspace).forEach(session::execute);

    System.out.println("Inserting values");
    for (int i = 0; i < n; i++) {
      session.execute(
          QueryBuilder.insertInto("response_time", "test")
              .value("id", i)
              .value("string", UUID.randomUUID().toString()));
    }
    System.out.println("100k values inserted");
  }

  private static BigInteger simulate1() {
    System.out.println("Starting simulation 1");
    Session session = PathStorePriviledgedCluster.getInstance().connect();
    long starting_time = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      session.execute(
          QueryBuilder.select("string")
              .from("response_time", "test")
              .where(QueryBuilder.eq("id", i)));
    }
    long ending_time = System.currentTimeMillis();
    return new BigInteger(String.valueOf(ending_time - starting_time));
  }

  private static BigInteger simulate2() {
    Session session = PathStorePriviledgedCluster.getInstance().connect();
    long starting_time = System.currentTimeMillis();

    List<Value> valueList = new LinkedList<>();

    for (Row row : session.execute(QueryBuilder.select().all().from("response_time", "test"))) {
      valueList.add(new Value(row.getInt("id"), row.getString("string")));
    }

    for (int i = 0; i < n; i++) {
      int finalI = i;
      List<Value> values =
          valueList.stream().filter(value -> value.id == finalI).collect(Collectors.toList());
    }

    long ending_time = System.currentTimeMillis();
    return new BigInteger(String.valueOf(ending_time - starting_time));
  }

  public static void main(String[] args) {
    install();
    BigInteger sim1_avg_time = new BigInteger("0");
    for (int i = 0; i < 100; i++) {
      sim1_avg_time = sim1_avg_time.add(simulate1());
    }
    System.out.println(
        "Average time for simulation one is: "
            + (sim1_avg_time.divide(new BigInteger("100")).toString()));
    BigInteger sim2_avg_time = new BigInteger("0");
    for (int i = 0; i < 100; i++) {
      sim2_avg_time = sim2_avg_time.add(simulate2());
    }
    System.out.println(
        "Average time for simulation two is: "
            + (sim2_avg_time.divide(new BigInteger("100")).toString()));
    PathStorePriviledgedCluster.getInstance().connect().execute("drop keyspace response_time");
    PathStorePriviledgedCluster.getInstance().close();
  }
}
