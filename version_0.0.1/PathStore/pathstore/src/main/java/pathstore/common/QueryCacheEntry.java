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

/** TODO: Comment */
public class QueryCacheEntry implements Serializable {
  public final String keyspace;
  public final String table;
  public final List<Clause> clauses;
  transient byte[] clausesSerialized = null;
  transient QueryCacheEntry isCovered = null;
  transient List<QueryCacheEntry> covers = null;
  private transient boolean ready = false;
  private transient UUID parentTimeStamp = null;
  public transient int limit;

  public QueryCacheEntry(
      String keyspace, String table, List<Clause> clauses, int limit) {
    this.keyspace = keyspace;
    this.table = table;
    this.clauses = clauses;
    covers = new ArrayList<>();
    this.limit = limit;
  }

  public QueryCacheEntry getIsCovered() {
    return this.isCovered;
  }

  // I'm chaning this yall
  public boolean isSame(List<Clause> clauses2) {
    if (clauses.size() != clauses2.size()) return false;

    for (int i = 0; i < clauses.size(); i++) {
      // if (clauses.get(i).toString().compareTo(clauses2.get(i).toString())!=0)
      if (clauses.get(i).getName().toString().compareTo(clauses2.get(i).getName().toString()) != 0
          || clauses.get(i).getValue().toString().compareTo(clauses2.get(i).getValue().toString())
              != 0) return false;
    }
    return true;
  }

  public synchronized void setReady() {
    this.ready = true;
    this.notifyAll();
  }

  public void waitUntilReady() {
    if (this.ready) return;
    synchronized (this) {
      while (!this.ready)
        try {
          this.wait();
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
    }
  }

  public boolean isReady() {
    return this.ready;
  }

  private boolean firstContainsSecond(List<Clause> clauses1, List<Clause> clauses2) {
    // Trivial case.  First query does not have where clauses, and second has at least one clause
    return clauses1.size() == 0 && clauses2.size() > 0;
  }

  public boolean isSuperSet(List<Clause> clauses2) {
    return firstContainsSecond(this.clauses, clauses2);
  }

  public boolean isSubSet(List<Clause> clauses2) {
    return firstContainsSecond(clauses2, this.clauses);
  }

  public UUID getParentTimeStamp() {
    return parentTimeStamp;
  }

  public void setParentTimeStamp(UUID parentTimeStamp) {
    this.parentTimeStamp = parentTimeStamp;
  }

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

  public void setClausesSerialized(byte[] clausesSerialized) {
    this.clausesSerialized = clausesSerialized;
  }
}
