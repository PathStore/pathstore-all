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
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;

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
  /** Serial version uid */
  private static final long serialVersionUID = -8555451138125895390L;

  /** Status definition for qc entry */
  public enum Status {
    INITIALIZING,
    READY,
    REMOVING,
    REMOVED
  }

  /** Lock for qc entry (waiting for ready) */
  private final Object readyLock = new Object();

  /** Lock for qc entry (waiting for removed) */
  private final Object removedLock = new Object();

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

  /** Status of entry */
  private transient Status status = Status.INITIALIZING;

  /**
   * Current time stamp of the parent node, written at time of first fetch. This is always the
   * highest parent time stamp of the data being pulled
   */
  private transient UUID parentTimeStamp = null;

  /** Limit on rows */
  public final transient int limit;

  /** When this entry expires */
  private long expirationTime = -1;

  public QueryCacheEntry(
      final String keyspace, final String table, final List<Clause> clauses, final int limit) {
    this.keyspace = keyspace;
    this.table = table;
    this.clauses = clauses;
    this.covers = new ArrayList<>();
    this.limit = limit;

    // set the expiration time for this entry to the current time plus the CLT / SLT (role
    // dependent)
    this.resetExpirationTime();
  }

  /** @return select statement from entry */
  public Select select() {
    Select select = QueryBuilder.select().all().from(this.keyspace, this.table);

    for (Clause clause : this.clauses) select.where(clause);

    if (this.limit != -1) select.limit(this.limit);

    return select;
  }

  /**
   * This function is used to set the expiration time according to the keyspace. If the keyspace is
   * pathstore_applications we set it to -1.
   */
  public void resetExpirationTime() {
    if (!this.keyspace.equals(Constants.PATHSTORE_APPLICATIONS))
      ApplicationLeaseCache.getInstance()
          .getLease(this.keyspace)
          .ifPresent(
              applicationLease ->
                  this.expirationTime =
                      System.currentTimeMillis()
                          + (PathStoreProperties.getInstance().role == Role.CLIENT
                              ? applicationLease.getClientLeaseTime()
                              : applicationLease.getServerLeaseTime()));
    else this.expirationTime = -1;
  }

  /**
   * This function is used to check to see if this entry is expired respective to the current time.
   *
   * @return true if expired, else false
   * @see #isExpired(long)
   */
  public boolean isExpired() {
    return this.isExpired(System.currentTimeMillis());
  }

  /**
   * This function is used to get whether or not the current entry is expired.
   *
   * @param time time to compare against
   * @return true if time is later than the expiration time, otherwise false. If Keyspace is
   *     PathStore_Applications return false.
   */
  protected boolean isExpired(final long time) {
    return !this.keyspace.equals(Constants.PATHSTORE_APPLICATIONS) && time > this.expirationTime;
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
   * Used to denote if this entry is finished being built
   *
   * @return {@link #status}
   */
  public boolean isReady() {
    return this.status == Status.READY;
  }

  /** Notify all waiting for this to complete */
  public void setReady() {
    this.status = Status.READY;
    synchronized (this.readyLock) {
      this.readyLock.notifyAll();
    }
  }

  /** Halt current thread until this entry is ready. */
  public void waitUntilReady() {
    if (this.status == Status.READY) return;
    if (this.status == Status.REMOVING)
      throw new RuntimeException("Entry will not be set to ready as status is removing");
    synchronized (this.readyLock) {
      while (this.status != Status.READY)
        try {
          this.readyLock.wait();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
    }
  }

  /** @return is the status removing */
  public boolean isRemoving() {
    return this.status == Status.REMOVING;
  }

  /** This is used to set the ready status to false */
  public void setRemoving() {
    this.status = Status.REMOVING;
  }

  /** Set the status to removed and notify all threads waiting for status to be set to removed */
  public void setRemoved() {
    this.status = Status.REMOVED;
    synchronized (this.removedLock) {
      this.removedLock.notifyAll();
    }
  }

  /** This function is used to wait for the entry to be set to removed */
  public void waitUntilRemoved() {
    if (this.status != Status.REMOVING) return;

    synchronized (this.removedLock) {
      while (this.status != Status.REMOVED) {
        try {
          this.removedLock.wait();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
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

  @Override
  public String toString() {
    return "QueryCacheEntry{"
        + "keyspace='"
        + keyspace
        + '\''
        + ", table='"
        + table
        + '\''
        + ", clauses="
        + clauses
        + ", ready="
        + status
        + ", parentTimeStamp="
        + parentTimeStamp
        + ", limit="
        + limit
        + ", expirationTime="
        + expirationTime
        + '}';
  }
}
