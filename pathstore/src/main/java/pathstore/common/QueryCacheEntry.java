/**
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
package pathstore.common;

import com.datastax.driver.core.querybuilder.Clause;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This class is used to denote an entry in the querycache. This is a transposition from a select
 * statement to a parsed entry to support a different feature set.
 *
 * @implNote Note they entries are synchronized to ensure that the pull server doesn't start pull
 *     entries that haven't been officially added. If they haven't been added then the pull server
 *     could potentially fetch a duplicate of the initial dataset.
 */
public class QueryCacheEntry implements Serializable {
  /** Lock for qc entry */
  private final Object lock = new Object();

  /** Keyspace for the select statement */
  public final String keyspace;

  /** Table for the select statement */
  public final String table;

  /** Clauses present on the select statement after being parsed in ps session */
  public final List<Clause> clauses;

  /** created during the first call from client to local node or from server to server */
  private transient byte[] clausesSerialized = null;

  /** null if the entry isn't covered else denotes the last entry which covers this entry */
  private transient QueryCacheEntry isCovered = null;

  /** List of entry who covers this quer */
  private transient List<QueryCacheEntry> covers;

  /**
   * Whether the qc entry is ready to be used by the pull server yet (only after the initial fetch
   * is complete)
   */
  private transient boolean ready = false;

  /**
   * Current time stamp of the parent node, written at time of first fetch. This is always the
   * highest parent time stamp of the data being pulled
   */
  private transient UUID parentTimeStamp = null;

  /** Limit on rows */
  public final transient int limit;

  public QueryCacheEntry(
      final String keyspace, final String table, final List<Clause> clauses, final int limit) {
    this.keyspace = keyspace;
    this.table = table;
    this.clauses = clauses;
    covers = new ArrayList<>();
    this.limit = limit;
  }

  /**
   * Checks is two sets of clauses are equal
   *
   * @param clauses2 other clause set
   * @return true if equal else false
   */
  public boolean isSame(final List<Clause> clauses2) {
    if (clauses.size() != clauses2.size()) return false;

    for (int i = 0; i < clauses.size(); i++) {
      if (clauses.get(i).getName().compareTo(clauses2.get(i).getName()) != 0
          || clauses.get(i).getValue().toString().compareTo(clauses2.get(i).getValue().toString())
              != 0) return false;
    }
    return true;
  }

  /**
   * Notify all waiting for this to complete
   *
   * <p>TODO: Myles: I'm pretty sure {@link #waitUntilReady()} is un-needed. thus the notifyAll call
   * is un-needed
   */
  public void setReady() {
    this.ready = true;
    synchronized (this.lock) {
      this.lock.notifyAll();
    }
  }

  /**
   * Halt current thread until this entry is ready.
   *
   * <p>TODO: Myles: pretty sure this is not needed as this is only used during the update cache
   * call. As in this is only called on the same thready that calls setReady thus there should be no
   * reason to wait.
   */
  public void waitUntilReady() {
    if (this.ready) return;
    synchronized (this.lock) {
      while (!this.ready)
        try {
          this.lock.wait();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
    }
  }

  /**
   * Used to denote if this entry is finished being built
   *
   * @return {@link #ready}
   */
  public boolean isReady() {
    return this.ready;
  }

  /**
   * TODO: Myles: Should this expand to the general case?
   *
   * @param clauses1 clause set 1
   * @param clauses2 clause set 2
   * @return true if clause set 1 is 0 and the other is larger
   */
  private static boolean firstContainsSecond(
      final List<Clause> clauses1, final List<Clause> clauses2) {
    // Trivial case.  First query does not have where clauses, and second has at least one clause
    return clauses1.size() == 0 && clauses2.size() > 0;
  }

  /**
   * @param clauses2 clause to compare to
   * @return if the current clauses is larger than the passed clause this is a super set thus true,
   *     else false
   */
  public boolean isSuperSet(final List<Clause> clauses2) {
    return firstContainsSecond(this.clauses, clauses2);
  }

  /**
   * @param clauses2 clause to compare to
   * @return if the passed clause set is 0 and this clause set is more than one this clause set is a
   *     subset
   */
  public boolean isSubSet(final List<Clause> clauses2) {
    return firstContainsSecond(clauses2, this.clauses);
  }

  /** @return {@link #covers} */
  public List<QueryCacheEntry> getCovers() {
    return this.covers;
  }

  /** @return {@link #isCovered} */
  public QueryCacheEntry getIsCovered() {
    return this.isCovered;
  }

  /** @param queryCacheEntry set {@link #isCovered} to passed value */
  public void setIsCovered(final QueryCacheEntry queryCacheEntry) {
    this.isCovered = queryCacheEntry;
  }

  /** @return {@link #parentTimeStamp} */
  public UUID getParentTimeStamp() {
    return this.parentTimeStamp;
  }

  /** @param parentTimeStamp set {@link #parentTimeStamp} to passed value */
  public void setParentTimeStamp(final UUID parentTimeStamp) {
    this.parentTimeStamp = parentTimeStamp;
  }

  /**
   * @return {@link #clauses} serialized to byte array
   * @throws IOException if serialization cannot occur
   */
  public byte[] getClausesSerialized() throws IOException {

    if (clausesSerialized == null) {
      ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(bytesOut);
      oos.writeObject(this.clauses);
      oos.flush();
      byte[] bytes = bytesOut.toByteArray();
      bytesOut.close();
      oos.close();
      this.clausesSerialized = bytes;
    }

    return this.clausesSerialized;
  }

  /** @param clausesSerialized set {@link #clausesSerialized} to passed value */
  public void setClausesSerialized(final byte[] clausesSerialized) {
    this.clausesSerialized = clausesSerialized;
  }
}
