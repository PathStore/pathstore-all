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
package pathstore.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;

import com.datastax.driver.core.querybuilder.Clause;

/**
 * TODO: Currently not used
 */
public class CommandEntryReply implements java.io.Serializable {


    public String getKeyspace() {
        return keyspace;
    }


    public void setKeyspace(String keyspace) {
        this.keyspace = keyspace;
    }


    public String getSid() {
        return sid;
    }


    public void setSid(String sid) {
        this.sid = sid;
    }


    public String getTable() {
        return table;
    }


    public void setTable(String table) {
        this.table = table;
    }

    public int getLimit() {
        return limit;
    }


    public void setLimit(int limit) {
        this.limit = limit;
    }


    public byte[] getClauses() {
        return clauses;
    }


    public void setClauses(byte[] clauses) {
        this.clauses = clauses;
    }

    public void convertClauses() {
        ByteArrayInputStream bytesIn = new ByteArrayInputStream(clauses);
        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(bytesIn);
            clausesConverted = (List<Clause>) ois.readObject();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public List<Clause> getConverted() {
        return clausesConverted;
    }


    String keyspace;
    String sid;
    String table;
    int limit;
    byte[] clauses;
    List<Clause> clausesConverted;

    public CommandEntryReply(String keyspace, String sid, String table, byte[] clauses, int limit) {
        super();
        this.keyspace = keyspace;
        this.sid = sid;
        this.table = table;
        this.clauses = clauses;
        this.limit = limit;
    }


}
