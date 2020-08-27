/*
 * ********
 *
 * <p>Copyright 2019 Eyal de Lara, Seyed Hossein Mortazavi, Mohammad Salehe
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>*********
 */
package pathstore.util;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import io.netty.util.internal.ConcurrentSet;
import pathstore.common.Constants;
import pathstore.common.PathStoreServer;
import pathstore.system.PathStorePrivilegedCluster;
import pathstore.system.logging.PathStoreLogger;
import pathstore.system.logging.PathStoreLoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * The purpose of this class is to represent the keyspace system_schema in memory for usage
 * throughout the application whenever a decision needs to be made specifically based some aspect of
 * a table.
 *
 * <p>The current aspects that we keep track of are: Tables per keyspace, Columns per table, Indexes
 * per table and UDT's per keyspace (User defined types). Their literal table names are: tables,
 * columns, indexes and types respectively. We also use the system_schema.keyspaces table to query
 * available keyspaces on startup, this is used if a node has failed and restarts as {@link
 * SchemaInfo#loadKeyspace(String)} won't be recalled for each keyspace
 *
 * @apiNote This class makes the assumption that once a keyspace is loaded into memory it is final.
 *     We get to make this assumption because PathStore controls the loading / unloading of
 *     keyspaces on a network wide scale. This will cause problems when developers chose to modify a
 *     keyspace manually on a given node to test a feature. Thus the keyspace registered through the
 *     admin panel should be considered final and shouldn't be modified locally.
 * @implNote Data retrieved adheres to Cassandra version 3.9. If Cassandra is updated this class
 *     along with all associated constants must be checked to make sure they match the format
 *     described in the newer Cassandra version.
 */
public class SchemaInfo implements Serializable {
  /** Factory instance of this class */
  private static SchemaInfo instance = null;

  /**
   * This function is used on the client side to use their local nodes schema info.
   *
   * @param schemaInfo schema info from local node
   */
  public static synchronized void setInstance(final SchemaInfo schemaInfo) {
    instance = schemaInfo;
  }

  /** @return instance of this class (there will only ever be one) */
  public static synchronized SchemaInfo getInstance() {
    if (SchemaInfo.instance == null)
      SchemaInfo.instance =
          new SchemaInfo(PathStorePrivilegedCluster.getSuperUserInstance().connect());
    return SchemaInfo.instance;
  }

  /**
   * Logger for this class
   *
   * @see PathStoreLoggerFactory
   */
  private static final PathStoreLogger logger = PathStoreLoggerFactory.getLogger(SchemaInfo.class);

  /** Set of keyspace names that have been loaded in */
  private final Set<String> keyspacesLoaded;

  /**
   * Map is defined as tableMap: keyspace_name -> table_name -> table object
   *
   * @see #getTablesFromKeyspace(String)
   * @see #getTableColumns(String, String)
   * @see #getTableIndexes(String, String)
   * @see #loadKeyspace(String)
   */
  private final ConcurrentMap<String, ConcurrentMap<String, Table>> tableMap;

  /**
   * Map is defined as columnInfo: keyspace_name -> table object -> collection of column objects
   *
   * @see #getTableColumns(Table)
   * @see #getTableColumns(String, String)
   * @see #loadKeyspace(String)
   */
  private final ConcurrentMap<String, ConcurrentMap<Table, Collection<Column>>> columnInfo;

  /**
   * Map is defined as partitionColumnNames: keyspace_name -> table object -> collection of
   * partition column names
   *
   * @see #getPartitionColumnNames(Table)
   * @see #getPartitionColumnNames(String, String)
   * @see #loadKeyspace(String)
   */
  private final ConcurrentMap<String, ConcurrentMap<Table, Collection<String>>>
      partitionColumnNames;

  /**
   * Map is defined as clusterColumnNames: keyspace_name -> table object -> collection of cluster
   * column names
   *
   * @see #getClusterColumnNames(Table)
   * @see #getClusterColumnNames(String, String)
   * @see #loadKeyspace(String)
   */
  private final ConcurrentMap<String, ConcurrentMap<Table, Collection<String>>> clusterColumnNames;

  /**
   * Map is defined as indexInfo: keyspace_name -> table object -> collection of index objects
   *
   * @see #getTableIndexes(Table)
   * @see #loadKeyspace(String)
   */
  private final ConcurrentMap<String, ConcurrentMap<Table, Collection<Index>>> indexInfo;

  /**
   * Map is defined as typeInfo: keyspace_name -> collection of types for that keyspace
   *
   * @see Type#buildFromKeyspace(Session, String)
   * @see #getKeyspaceTypes(String)
   * @see #loadKeyspace(String)
   */
  private final ConcurrentMap<String, Collection<Type>> typeInfo;

  /** @see PathStorePrivilegedCluster */
  private final transient Session session;

  /**
   * @param session must be a raw connection to a cassandra database. Not a pathstore wrapped
   *     connection.
   */
  public SchemaInfo(final Session session) {
    this.keyspacesLoaded = new ConcurrentSet<>();
    this.tableMap = new ConcurrentHashMap<>();
    this.columnInfo = new ConcurrentHashMap<>();
    this.partitionColumnNames = new ConcurrentHashMap<>();
    this.clusterColumnNames = new ConcurrentHashMap<>();
    this.indexInfo = new ConcurrentHashMap<>();
    this.typeInfo = new ConcurrentHashMap<>();
    this.session = session;
    this.loadSchemas();
  }

  /**
   * This constructor is used to build a partition of the schema info class.
   *
   * @param keyspacesLoaded singleton set of one keyspace
   * @param tableMap all tables for the passed keyspace
   * @param columnInfo column info for the singleton keyspace
   * @param partitionColumnNames partition column names for the passed keyspace
   * @param clusterColumnNames cluster column names for the passed keyspace
   * @param indexInfo index info for the keyspace
   * @param typeInfo type info for the keyspace
   * @see #getSchemaPartition(String)
   */
  private SchemaInfo(
      final Set<String> keyspacesLoaded,
      final ConcurrentMap<String, ConcurrentMap<String, Table>> tableMap,
      final ConcurrentMap<String, ConcurrentMap<Table, Collection<Column>>> columnInfo,
      final ConcurrentMap<String, ConcurrentMap<Table, Collection<String>>> partitionColumnNames,
      final ConcurrentMap<String, ConcurrentMap<Table, Collection<String>>> clusterColumnNames,
      final ConcurrentMap<String, ConcurrentMap<Table, Collection<Index>>> indexInfo,
      final ConcurrentMap<String, Collection<Type>> typeInfo) {
    this.session = null;
    this.keyspacesLoaded = keyspacesLoaded;
    this.tableMap = tableMap;
    this.columnInfo = columnInfo;
    this.partitionColumnNames = partitionColumnNames;
    this.clusterColumnNames = clusterColumnNames;
    this.indexInfo = indexInfo;
    this.typeInfo = typeInfo;
  }

  /**
   * This function is used to produce a new schema info object for a partition of the data based on
   * a keyspace
   *
   * @param keyspace keyspace to partition by
   * @return new schema info object if the keyspace passed is valid.
   * @see PathStoreServer#getSchemaInfo(String)
   */
  public SchemaInfo getSchemaPartition(final String keyspace) {
    if (this.keyspacesLoaded.contains(keyspace)) {
      Set<String> keyspacesLoaded = new HashSet<>();
      keyspacesLoaded.add(keyspace);

      Predicate<Map.Entry<String, ?>> filterByKeyspace = entry -> entry.getKey().equals(keyspace);

      return new SchemaInfo(
          keyspacesLoaded,
          this.tableMap.entrySet().stream()
              .filter(filterByKeyspace)
              .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue)),
          this.columnInfo.entrySet().stream()
              .filter(filterByKeyspace)
              .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue)),
          this.partitionColumnNames.entrySet().stream()
              .filter(filterByKeyspace)
              .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue)),
          this.clusterColumnNames.entrySet().stream()
              .filter(filterByKeyspace)
              .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue)),
          this.indexInfo.entrySet().stream()
              .filter(filterByKeyspace)
              .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue)),
          this.typeInfo.entrySet().stream()
              .filter(filterByKeyspace)
              .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    return null;
  }

  // publicly accessible functions

  /**
   * This function is used to load a new keyspace's information into memory.
   *
   * @param keyspace keyspace to load into memory. Assumed valid.
   * @apiNote There is no validity check to ensure the keyspace name you've passed actually exists
   *     within the local Cassandra node. The main caller of this function however can guarantee
   *     that the passed keyspace name exists within Cassandra as it loads it prior to calling this
   *     function.
   * @implNote The placement of the loading of data is very specific. As there are dependencies
   *     between loading data. If you plan to modify this function you need to read the impl notes
   *     for each loading function, they explain what they depend on.
   * @see pathstore.system.schemaFSM.PathStoreSlaveSchemaServer
   */
  public void loadKeyspace(final String keyspace) {

    if (keyspace.startsWith(Constants.PATHSTORE_PREFIX)) {
      this.tableMap.put(keyspace, this.loadTableCollectionsForKeyspace(keyspace));
      this.columnInfo.put(keyspace, this.getColumnInfoPerKeyspace(keyspace));
      this.partitionColumnNames.put(
          keyspace, this.getColumnNamesPerKeyspace(keyspace, "partition_key"));
      this.clusterColumnNames.put(keyspace, this.getColumnNamesPerKeyspace(keyspace, "clustering"));
      this.indexInfo.put(keyspace, this.getIndexInfoPerKeyspace(keyspace));
      this.typeInfo.put(keyspace, Type.buildFromKeyspace(this.session, keyspace));
      this.keyspacesLoaded.add(keyspace);

      logger.info(
          String.format(
              "Loaded keyspace %s it has %d table(s) and has %d udt(s)",
              keyspace,
              this.tableMap.get(keyspace).values().size(),
              this.typeInfo.get(keyspace).size()));
    } else {
      logger.error(
          String.format("Could not load keyspace %s as it is not a pathstore keyspace", keyspace));
    }
  }

  /**
   * This function is used to remove a new keyspace's information from memory. This should only be
   * called when a keyspace is being removed from the database but still resides in memory.
   *
   * @param keyspace keyspace to remove from memory. Assumed present.
   * @apiNote There is not validity check to ensure the keyspace name passed actually exists within
   *     memory. This is because the sole caller of this function can guarentee that it is removing
   *     a keyspace from memory that use to exist within the local Cassandra node but no longer
   *     exists. If you wish to use this function within your application you should use it with
   *     caution.
   */
  public void removeKeyspace(final String keyspace) {
    this.keyspacesLoaded.remove(keyspace);
    this.tableMap.remove(keyspace);
    this.columnInfo.remove(keyspace);
    this.partitionColumnNames.remove(keyspace);
    this.clusterColumnNames.remove(keyspace);
    this.indexInfo.remove(keyspace);
    this.typeInfo.remove(keyspace);

    logger.info(String.format("Removed keyspace %s", keyspace));
  }

  /**
   * This function is used to determine if a given keyspace has already been loaded into memory.
   * This function is here for startup purposes only. It is used to determine whether
   * pathstore_applications is loaded in on startup. To expand on this case, it is only loaded in if
   * the node has failed (stopped) and has then been restarted.
   *
   * @param keyspace keyspace to check
   * @return true if present, false if not
   */
  public boolean isKeyspaceLoaded(final String keyspace) {
    return this.keyspacesLoaded.contains(keyspace);
  }

  /**
   * This function is used to retrieve a set of keyspaces (strings) that are loaded into memory.
   *
   * @return list of keyspaces loaded into memory
   * @apiNote Unless modified (adding calls to load / remove), this will give you an entire list of
   *     pathstore related keyspaces on the local cassandra node
   */
  public Collection<String> getLoadedKeyspaces() {
    return this.keyspacesLoaded;
  }

  /**
   * This function will give you a set of table objects based on a keyspace
   *
   * @param keyspace keyspace of interest. Assumed valid.
   * @return collection of table objects associated with a given keyspace
   */
  public Collection<Table> getTablesFromKeyspace(final String keyspace) {
    return this.tableMap.get(keyspace).values();
  }

  /**
   * This function will get you a table object from a keyspace and table name.
   *
   * @param keyspace keyspace name
   * @param table table name
   * @return table object or null if non-existent
   */
  public Table getTableFromKeyspaceAndTableName(final String keyspace, final String table) {
    return this.tableMap.get(keyspace).get(table);
  }

  /**
   * This function is used to retrieve a set of column objects based on a keyspace and table name.
   *
   * @param keyspace keyspace name
   * @param tableName table name
   * @return set of column objects for that table
   * @see #getTableIndexes(Table)
   */
  public Collection<Column> getTableColumns(final String keyspace, final String tableName) {
    return this.getTableColumns(this.tableMap.get(keyspace).get(tableName));
  }

  /**
   * This function is used to retrieve a set of column objects based on a table object.
   *
   * @param table table object to get column objects from.
   * @return list of column objects or an empty set if none are present
   */
  public Collection<Column> getTableColumns(final Table table) {
    System.out.println(table.keyspace_name);
    System.out.println(this.columnInfo.keySet());
    System.out.println(this.columnInfo.get(table.keyspace_name).keySet());
    return Optional.of(this.columnInfo.get(table.keyspace_name).get(table))
        .orElse(Collections.emptySet());
  }

  /**
   * This function is used to retrieve a set of partition column names from a keyspace and table
   * names.
   *
   * @param keyspace keyspace name
   * @param tableName table name
   * @return set of partition column names
   * @see #getPartitionColumnNames(Table)
   */
  public Collection<String> getPartitionColumnNames(final String keyspace, final String tableName) {
    return this.getPartitionColumnNames(this.tableMap.get(keyspace).get(tableName));
  }

  /**
   * This function is used to retrieve a set of partition column names from a table object.
   *
   * @param table table object
   * @return set of partition column names
   */
  public Collection<String> getPartitionColumnNames(final Table table) {
    return Optional.of(this.partitionColumnNames.get(table.keyspace_name).get(table))
        .orElse(Collections.emptySet());
  }

  /**
   * This function is used to retrieve a set of clustering column names from a keyspace and table
   * names.
   *
   * @param keyspace keyspace name
   * @param tableName table name
   * @return set of clustering column names
   * @see #getClusterColumnNames(Table)
   */
  public Collection<String> getClusterColumnNames(final String keyspace, final String tableName) {
    return this.getClusterColumnNames(this.tableMap.get(keyspace).get(tableName));
  }

  /**
   * This function is used to retrieve a set of clustering column names from a table object.
   *
   * @param table table object
   * @return set of clustering column names
   */
  public Collection<String> getClusterColumnNames(final Table table) {
    return Optional.of(this.clusterColumnNames.get(table.keyspace_name).get(table))
        .orElse(Collections.emptySet());
  }

  /**
   * This function is used to retrieve a set of index objects based on a keyspace and table name.
   *
   * @param keyspaceName keyspace name
   * @param tableName table name
   * @return set of index objects from the given table
   * @see #getTableIndexes(Table)
   */
  public Collection<Index> getTableIndexes(final String keyspaceName, final String tableName) {
    return this.getTableIndexes(this.tableMap.get(keyspaceName).get(tableName));
  }

  /**
   * This function is used to retrieve a set of index objects based on a table object
   *
   * @param table table object
   * @return set of index objects associated with the given table
   */
  public Collection<Index> getTableIndexes(final Table table) {
    return Optional.of(this.indexInfo.get(table.keyspace_name).get(table))
        .orElse(Collections.emptySet());
  }

  /**
   * This function is used to retrieve a set of UDT's (user defined types) based on a keyspace name.
   * As these are created at the keyspace level rather then the table level
   *
   * @param keyspace non-validated keyspace name.
   * @return a set of types for that keyspace always not null
   */
  public Collection<Type> getKeyspaceTypes(final String keyspace) {
    return Optional.of(this.typeInfo.get(keyspace)).orElse(Collections.emptySet());
  }

  // Private functions

  /**
   * This function is only used on object creation which occurs during the startup phase of this
   * application. It is used to load any schema information that pertains to any pathstore table
   * that was already loaded into the local cassandra instance. This only modifies the state if the
   * node was shutoff (by intent or failure). Otherwise its a sanity check.
   *
   * @see pathstore.system.PathStoreServerImpl
   */
  private void loadSchemas() {
    if (this.session != null)
      StreamSupport.stream(
              this.session
                  .execute(
                      QueryBuilder.select(Constants.KEYSPACES_COLUMNS.KEYSPACE_NAME)
                          .from(Constants.SYSTEM_SCHEMA, Constants.KEYSPACES))
                  .spliterator(),
              true)
          .map(row -> row.getString(Constants.KEYSPACES_COLUMNS.KEYSPACE_NAME))
          .filter(keyspace -> keyspace.startsWith(Constants.PATHSTORE_PREFIX))
          .forEach(this::loadKeyspace);
  }

  /**
   * This function is used to produce a map from table_name -> table object for constant time
   * lookups of column / index information given a keyspace name & table name.
   *
   * @param keyspaceName keyspace name
   * @return map from table_name -> table object for all tables that reside within a given keyspace.
   */
  private ConcurrentMap<String, Table> loadTableCollectionsForKeyspace(final String keyspaceName) {
    return this.session != null
        ? StreamSupport.stream(
                this.session
                    .execute(
                        QueryBuilder.select()
                            .all()
                            .from(Constants.SYSTEM_SCHEMA, Constants.TABLES)
                            .where(
                                QueryBuilder.eq(
                                    Constants.TABLES_COLUMNS.KEYSPACE_NAME, keyspaceName)))
                    .spliterator(),
                true)
            .map(Table::buildFromRow)
            .collect(Collectors.toConcurrentMap(table -> table.table_name, Function.identity()))
        : null;
  }

  /**
   * This function is used to produce a map from table object -> collection of column objects for
   * that given table. This is used to update {@link #columnInfo} for a given keyspace with column
   * information for each table.
   *
   * @param keyspace keyspace to build column objects for
   * @return map from table object -> collection of column objects
   * @see #loadKeyspace(String)
   * @see Column#buildFromTable(Session, Table)
   * @implNote This function works under the assumption that the table objects in {@link #tableMap}
   *     have already been generated for the provided keyspace. Otherwise this will do nothing as
   *     there are no table objects associated with the provided keyspace to build the collection
   *     from.
   */
  private ConcurrentMap<Table, Collection<Column>> getColumnInfoPerKeyspace(final String keyspace) {
    return this.tableMap.get(keyspace).values().stream()
        .parallel()
        .collect(
            Collectors.toConcurrentMap(
                Function.identity(), table -> Column.buildFromTable(this.session, table)));
  }

  /**
   * This function is used to produce a map from table object -> collection of column names with a
   * given column type. This is used to update {@link #partitionColumnNames} and {@link
   * #clusterColumnNames} for a given keyspace with names of columns that have specific types.
   *
   * @param keyspace keyspace to build names from
   * @param columnType what type either "partition_key" or "clustering"
   * @return built map.
   * @implNote This function works under the assumption that {@link #tableMap} has been built for
   *     the passed keyspace and that {@link #columnInfo} has been built for the passed keyspace.
   */
  private ConcurrentMap<Table, Collection<String>> getColumnNamesPerKeyspace(
      final String keyspace, final String columnType) {
    return this.tableMap.get(keyspace).values().stream()
        .parallel()
        .collect(
            Collectors.toConcurrentMap(
                Function.identity(),
                table ->
                    this.getTableColumns(table).stream()
                        .parallel()
                        .filter(
                            column ->
                                column.kind.equals(columnType)
                                    && !column.column_name.startsWith(Constants.PATHSTORE_PREFIX))
                        .map(column -> column.column_name)
                        .collect(Collectors.toSet())));
  }

  /**
   * This function is used to produce a map from table object -> collection of index objects for
   * that given table. This used to update {@link #indexInfo} for a given keyspace with index
   * information for each table.
   *
   * @param keyspace keyspace to build index objects for
   * @return map from table object -> collection of index objects
   * @see #loadKeyspace(String)
   * @see Index#buildFromTable(Session, Table)
   * @implNote This function works under the assumption that the table objects in {@link #tableMap}
   *     have already been generated for the provided keyspace. Otherwise this will do nothing as
   *     there are no table objects associated with the provided keyspace to build the collection
   *     from.
   */
  private ConcurrentMap<Table, Collection<Index>> getIndexInfoPerKeyspace(final String keyspace) {
    return this.tableMap.get(keyspace).values().stream()
        .parallel()
        .collect(
            Collectors.toConcurrentMap(
                Function.identity(), table -> Index.buildFromTable(this.session, table)));
  }

  /**
   * This class represents a row in system_schema.types
   *
   * @apiNote In order to get an associated type from a field name you need to know the index where
   *     that name exists. I.e index 1 in field_names corresponding type is stored in index 1 in
   *     field_type
   * @see SchemaInfo#loadKeyspace(String)
   */
  public static class Type implements Serializable {
    /** keyspace name the type is associated with */
    public final String keyspace_name;

    /** Name of the type */
    public final String type_name;

    /** List of field names in the type */
    public final List<String> field_names;

    /** Associated types with each name */
    public final List<String> field_types;

    /**
     * @param keyspace_name {@link #keyspace_name}
     * @param type_name {@link #type_name}
     * @param field_names {@link #field_names}
     * @param field_types {@link #field_types}
     */
    private Type(
        final String keyspace_name,
        final String type_name,
        final List<String> field_names,
        final List<String> field_types) {
      this.keyspace_name = keyspace_name;
      this.type_name = type_name;
      this.field_names = field_names;
      this.field_types = field_types;
    }

    /**
     * This function is used to build a collection of type objects from a keyspace name.
     *
     * @param session {@link SchemaInfo#session}
     * @param keyspace name of keyspace
     * @return collection of type objects per keyspace.
     * @apiNote This function queries the database so its usage should be limited
     * @implNote Since the storage of types is in a set which isn't ordered we use parallel stream
     *     processing to improve the processing time of each row.
     */
    public static Collection<Type> buildFromKeyspace(final Session session, final String keyspace) {
      return session != null
          ? StreamSupport.stream(
                  session
                      .execute(
                          QueryBuilder.select()
                              .all()
                              .from(Constants.SYSTEM_SCHEMA, Constants.TYPES)
                              .where(
                                  QueryBuilder.eq(Constants.TYPES_COLUMNS.KEYSPACE_NAME, keyspace)))
                      .spliterator(),
                  true)
              .map(Type::buildFromRow)
              .collect(Collectors.toSet())
          : null;
    }

    /**
     * This function is used to process a system_schema.types row into a Type object.
     *
     * @param row row from system_schema.types
     * @return processed Type object
     * @implNote If this function gets changed to public you would need to verify where the row came
     *     from otherwise runtime errors can be thrown
     */
    private static Type buildFromRow(final Row row) {
      return new Type(
          row.getString(Constants.TYPES_COLUMNS.KEYSPACE_NAME),
          row.getString(Constants.TYPES_COLUMNS.TYPE_NAME),
          row.getList(Constants.TYPES_COLUMNS.FIELD_NAMES, String.class),
          row.getList(Constants.TYPES_COLUMNS.FIELD_TYPES, String.class));
    }
  }

  /**
   * This class represents a row in the system_schema.indexes table.
   *
   * @see SchemaInfo#loadKeyspace(String)
   */
  public static class Index implements Serializable {
    /** keyspace name where the index exists */
    public final String keyspace_name;

    /** Table name where the index exists */
    public final String table_name;

    /** Name of index that was provided at time of creation */
    public final String index_name;

    /** type of index (TODO: What are the options?) */
    public final String kind;

    /**
     * key name is 'target' according to apache docs to get the name of the column where the index
     * is present
     */
    public final Map<String, String> options;

    /**
     * @param keyspace_name {@link #keyspace_name}
     * @param table_name {@link #table_name}
     * @param index_name {@link #index_name}
     * @param kind {@link #kind}
     * @param options {@link #options}
     */
    private Index(
        final String keyspace_name,
        final String table_name,
        final String index_name,
        final String kind,
        final Map<String, String> options) {
      this.keyspace_name = keyspace_name;
      this.table_name = table_name;
      this.index_name = index_name;
      this.kind = kind;
      this.options = options;
    }

    /**
     * This function is used to build a collection of index objects from a table object.
     *
     * @param session {@link SchemaInfo#session}
     * @param table table object to build index objects for
     * @return collection of indexes objects per table.
     * @apiNote This function queries the database so its usage should be limited
     * @implNote Since the storage of indexes is in a set which isn't ordered we use parallel stream
     *     processing to improve the processing time of each row.
     */
    public static Collection<Index> buildFromTable(final Session session, final Table table) {
      return session != null
          ? StreamSupport.stream(
                  session
                      .execute(
                          QueryBuilder.select()
                              .all()
                              .from(Constants.SYSTEM_SCHEMA, Constants.INDEXES)
                              .where(
                                  QueryBuilder.eq(
                                      Constants.INDEXES_COLUMNS.KEYSPACE_NAME, table.keyspace_name))
                              .and(
                                  QueryBuilder.eq(
                                      Constants.INDEXES_COLUMNS.TABLE_NAME, table.table_name)))
                      .spliterator(),
                  true)
              .map(Index::buildFromRow)
              .collect(Collectors.toSet())
          : null;
    }

    /**
     * This function is used to process a system_schema.indexes row into an Index object.
     *
     * @param row row from system_schema.indexes
     * @return processed index object
     * @implNote If this function gets changed to public you would need to verify where the row came
     *     from otherwise runtime errors can be thrown
     */
    private static Index buildFromRow(final Row row) {
      return new Index(
          row.getString(Constants.INDEXES_COLUMNS.KEYSPACE_NAME),
          row.getString(Constants.INDEXES_COLUMNS.TABLE_NAME),
          row.getString(Constants.INDEXES_COLUMNS.INDEX_NAME),
          row.getString(Constants.INDEXES_COLUMNS.KIND),
          row.getMap(Constants.INDEXES_COLUMNS.OPTIONS, String.class, String.class));
    }
  }

  /**
   * This class represents a row within the system_schema.columns table.
   *
   * @see SchemaInfo#loadKeyspace(String)
   */
  public static class Column implements Serializable {
    /** Name of keyspace the column is associated with */
    public final String keyspace_name;

    /** Name of table the column is associated with */
    public final String table_name;

    /** Column name */
    public final String column_name;

    /**
     * Clustering order, this is only not-none if the column is part of the sorting / clustering key
     */
    public final String clustering_order;

    /**
     * This has three potential values.
     *
     * <p>(1): partition_key
     *
     * <p>(2): clustering
     *
     * <p>(3): regular
     *
     * <p>These types cover all possible types of columns within Cassandra
     */
    public final String kind;

    /** TODO: (Myles) What does this represent? */
    public final int position;

    /**
     * What type of information is stored within this column. This cannot be determined as UDT's
     * exist within Cassandra
     */
    public final String type;

    /**
     * @param keyspace_name {@link #keyspace_name}
     * @param table_name {@link #table_name}
     * @param column_name {@link #columnInfo}
     * @param clustering_order {@link #clustering_order}
     * @param kind {@link #kind}
     * @param position {@link #position}
     * @param type {@link #type}
     */
    private Column(
        final String keyspace_name,
        final String table_name,
        final String column_name,
        final String clustering_order,
        final String kind,
        final int position,
        final String type) {
      this.keyspace_name = keyspace_name;
      this.table_name = table_name;
      this.column_name = column_name;
      this.clustering_order = clustering_order;
      this.kind = kind;
      this.position = position;
      this.type = type;
    }

    /**
     * This function is used to build a collection of column objects from a table object.
     *
     * @param session {@link SchemaInfo#session}
     * @param table table object to build column objects from
     * @return collection of column objects associated with the table object
     * @apiNote This function queries the database so its usage should be limited
     * @implNote Since the storage of columns is in a set which isn't ordered we use parallel stream
     *     processing to improve the processing time of each row.
     */
    public static Collection<Column> buildFromTable(final Session session, final Table table) {
      return session != null
          ? StreamSupport.stream(
                  session
                      .execute(
                          QueryBuilder.select()
                              .all()
                              .from(Constants.SYSTEM_SCHEMA, Constants.COLUMNS)
                              .where(
                                  QueryBuilder.eq(
                                      Constants.COLUMNS_COLUMNS.KEYSPACE_NAME, table.keyspace_name))
                              .and(
                                  QueryBuilder.eq(
                                      Constants.COLUMNS_COLUMNS.TABLE_NAME, table.table_name)))
                      .spliterator(),
                  true)
              .map(Column::buildFromRow)
              .collect(Collectors.toSet())
          : null;
    }

    /**
     * This function is used to process a system_schema.columns row into an Column object.
     *
     * @param row row from system_schema.columns
     * @return processed column object
     * @implNote If this function gets changed to public you would need to verify where the row came
     *     from otherwise runtime errors can be thrown
     */
    private static Column buildFromRow(final Row row) {
      return new Column(
          row.getString(Constants.COLUMNS_COLUMNS.KEYSPACE_NAME),
          row.getString(Constants.COLUMNS_COLUMNS.TABLE_NAME),
          row.getString(Constants.COLUMNS_COLUMNS.COLUMN_NAME),
          row.getString(Constants.COLUMNS_COLUMNS.CLUSTERING_ORDER),
          row.getString(Constants.COLUMNS_COLUMNS.KIND),
          row.getInt(Constants.COLUMNS_COLUMNS.POSITION),
          row.getString(Constants.COLUMNS_COLUMNS.TYPE));
    }
  }

  /**
   * This class represents a row within the system_schema.tables table.
   *
   * @see SchemaInfo#loadKeyspace(String)
   */
  public static class Table implements Serializable {
    /** Keyspace name where the table exists */
    public final String keyspace_name;

    /** Name of the table */
    public final String table_name;

    // All other fields are solely used for schema augmentation in the admin panel on registration
    // of a new application

    public final double bloom_filter_fp_chance;
    public final Object caching;
    public final boolean cdc;
    public final String comment;
    public final Object compaction;
    public final Object compression;
    public final double crc_check_chance;
    public final double dclocal_read_repair_chance;
    public final int default_time_to_live;
    public final Object extensions;
    public final Object flags;
    public final int gc_grace_seconds;
    public final UUID id;
    public final int max_index_interval;
    public final int memtable_flush_period_in_ms;
    public final int min_index_interval;
    public final double read_repair_chance;
    public final String speculative_retry;

    /**
     * @param keyspace_name {@link #keyspace_name}
     * @param table_name {@link #table_name}
     * @param bloom_filter_fp_chance {@link #bloom_filter_fp_chance}
     * @param caching {@link #caching}
     * @param cdc {@link #cdc}
     * @param comment {@link #comment}
     * @param compaction {@link #compaction}
     * @param compression {@link #compression}
     * @param crc_check_chance {@link #crc_check_chance}
     * @param dclocal_read_repair_chance {@link #dclocal_read_repair_chance}
     * @param default_time_to_live {@link #default_time_to_live}
     * @param extensions {@link #extensions}
     * @param flags {@link #flags}
     * @param gc_grace_seconds {@link #gc_grace_seconds}
     * @param id {@link #id}
     * @param max_index_interval {@link #max_index_interval}
     * @param memtable_flush_period_in_ms {@link #memtable_flush_period_in_ms}
     * @param min_index_interval {@link #min_index_interval}
     * @param read_repair_chance {@link #read_repair_chance}
     * @param speculative_retry {@link #speculative_retry}
     */
    private Table(
        final String keyspace_name,
        final String table_name,
        final double bloom_filter_fp_chance,
        final Object caching,
        final boolean cdc,
        final String comment,
        final Object compaction,
        final Object compression,
        final double crc_check_chance,
        final double dclocal_read_repair_chance,
        final int default_time_to_live,
        final Object extensions,
        final Object flags,
        final int gc_grace_seconds,
        final UUID id,
        final int max_index_interval,
        final int memtable_flush_period_in_ms,
        final int min_index_interval,
        final double read_repair_chance,
        final String speculative_retry) {
      this.keyspace_name = keyspace_name;
      this.table_name = table_name;
      this.bloom_filter_fp_chance = bloom_filter_fp_chance;
      this.caching = caching;
      this.cdc = cdc;
      this.comment = comment;
      this.compaction = compaction;
      this.compression = compression;
      this.crc_check_chance = crc_check_chance;
      this.dclocal_read_repair_chance = dclocal_read_repair_chance;
      this.default_time_to_live = default_time_to_live;
      this.extensions = extensions;
      this.flags = flags;
      this.gc_grace_seconds = gc_grace_seconds;
      this.id = id;
      this.max_index_interval = max_index_interval;
      this.memtable_flush_period_in_ms = memtable_flush_period_in_ms;
      this.min_index_interval = min_index_interval;
      this.read_repair_chance = read_repair_chance;
      this.speculative_retry = speculative_retry;
    }

    /**
     * This function is used to process a system_schema.tables row into an Table object.
     *
     * @param row row from system_schema.tables
     * @return processed table object
     */
    public static Table buildFromRow(final Row row) {
      return new Table(
          row.getString(Constants.TABLES_COLUMNS.KEYSPACE_NAME),
          row.getString(Constants.TABLES_COLUMNS.TABLE_NAME),
          row.getDouble(Constants.TABLES_COLUMNS.BLOOM_FILTER_FP_CHANCE),
          row.getObject(Constants.TABLES_COLUMNS.CACHING),
          row.getBool(Constants.TABLES_COLUMNS.CDC),
          row.getString(Constants.TABLES_COLUMNS.COMMENT),
          row.getObject(Constants.TABLES_COLUMNS.COMPACTION),
          row.getObject(Constants.TABLES_COLUMNS.COMPRESSION),
          row.getDouble(Constants.TABLES_COLUMNS.CRC_CHECK_CHANCE),
          row.getDouble(Constants.TABLES_COLUMNS.DCLOCAL_READ_REPAIR_CHANCE),
          row.getInt(Constants.TABLES_COLUMNS.DEFAULT_TIME_TO_LIVE),
          row.getObject(Constants.TABLES_COLUMNS.EXTENSIONS),
          row.getObject(Constants.TABLES_COLUMNS.FLAGS),
          row.getInt(Constants.TABLES_COLUMNS.GC_GRACE_SECONDS),
          row.getUUID(Constants.TABLES_COLUMNS.ID),
          row.getInt(Constants.TABLES_COLUMNS.MAX_INDEX_INTERVAL),
          row.getInt(Constants.TABLES_COLUMNS.MEMTABLE_FLUSH_PERIOD_IN_MS),
          row.getInt(Constants.TABLES_COLUMNS.MIN_INDEX_INTERVAL),
          row.getDouble(Constants.TABLES_COLUMNS.READ_REPAIR_CHANCE),
          row.getString(Constants.TABLES_COLUMNS.SPECULATIVE_RETRY));
    }

    /**
     * Compare an object to this table
     *
     * @param o object to compare to
     * @return true if equals else false
     */
    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Table table = (Table) o;
      return Double.compare(table.bloom_filter_fp_chance, this.bloom_filter_fp_chance) == 0
          && this.cdc == table.cdc
          && Double.compare(table.crc_check_chance, this.crc_check_chance) == 0
          && Double.compare(table.dclocal_read_repair_chance, this.dclocal_read_repair_chance) == 0
          && this.default_time_to_live == table.default_time_to_live
          && this.gc_grace_seconds == table.gc_grace_seconds
          && this.max_index_interval == table.max_index_interval
          && this.memtable_flush_period_in_ms == table.memtable_flush_period_in_ms
          && this.min_index_interval == table.min_index_interval
          && Double.compare(table.read_repair_chance, this.read_repair_chance) == 0
          && Objects.equals(this.keyspace_name, table.keyspace_name)
          && Objects.equals(this.table_name, table.table_name)
          && Objects.equals(this.caching, table.caching)
          && Objects.equals(this.comment, table.comment)
          && Objects.equals(this.compaction, table.compaction)
          && Objects.equals(this.compression, table.compression)
          && Objects.equals(this.extensions, table.extensions)
          && Objects.equals(this.flags, table.flags)
          && Objects.equals(this.id, table.id)
          && Objects.equals(this.speculative_retry, table.speculative_retry);
    }

    /** @return hash of this object */
    @Override
    public int hashCode() {
      return Objects.hash(
          this.keyspace_name,
          this.table_name,
          this.bloom_filter_fp_chance,
          this.caching,
          this.cdc,
          this.comment,
          this.compaction,
          this.compression,
          this.crc_check_chance,
          this.dclocal_read_repair_chance,
          this.default_time_to_live,
          this.extensions,
          this.flags,
          this.gc_grace_seconds,
          this.id,
          this.max_index_interval,
          this.memtable_flush_period_in_ms,
          this.min_index_interval,
          this.read_repair_chance,
          this.speculative_retry);
    }

    /** @return display info on table */
    @Override
    public String toString() {
      return "Table{"
          + "keyspace_name='"
          + keyspace_name
          + '\''
          + ", table_name='"
          + table_name
          + '\''
          + ", bloom_filter_fp_chance="
          + bloom_filter_fp_chance
          + ", caching="
          + caching
          + ", cdc="
          + cdc
          + ", comment='"
          + comment
          + '\''
          + ", compaction="
          + compaction
          + ", compression="
          + compression
          + ", crc_check_chance="
          + crc_check_chance
          + ", dclocal_read_repair_chance="
          + dclocal_read_repair_chance
          + ", default_time_to_live="
          + default_time_to_live
          + ", extensions="
          + extensions
          + ", flags="
          + flags
          + ", gc_grace_seconds="
          + gc_grace_seconds
          + ", id="
          + id
          + ", max_index_interval="
          + max_index_interval
          + ", memtable_flush_period_in_ms="
          + memtable_flush_period_in_ms
          + ", min_index_interval="
          + min_index_interval
          + ", read_repair_chance="
          + read_repair_chance
          + ", speculative_retry='"
          + speculative_retry
          + '\''
          + '}';
    }
  }
}
