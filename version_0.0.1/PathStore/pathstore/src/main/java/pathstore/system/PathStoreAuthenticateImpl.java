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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;

import pathstore.client.PathStoreCluster;
import pathstore.client.PathStoreSession;
import pathstore.common.PathStoreAuthenticate;
import pathstore.common.PathStoreProperties;
import pathstore.common.Role;

/**
 * TODO: Not used
 * Most likely WIP
 */
public class PathStoreAuthenticateImpl implements PathStoreAuthenticate {
    static PathStoreAuthenticateImpl pathStoreAuthenticate = null;
    private PathStoreAuthenticate serverstub;


    public static PathStoreAuthenticateImpl getInstance() {
        if (PathStoreAuthenticateImpl.pathStoreAuthenticate == null)
            PathStoreAuthenticateImpl.pathStoreAuthenticate = new PathStoreAuthenticateImpl();
        return PathStoreAuthenticateImpl.pathStoreAuthenticate;
    }

    public PathStoreAuthenticateImpl() {

        if (PathStoreProperties.getInstance().role != Role.CLIENT) {
            System.out.println("creating PathStoreAuthenticate registry");
            PathStoreAuthenticateImpl obj = this;

            Registry registry = null;
            PathStoreAuthenticate stub;
            try {
                stub = (PathStoreAuthenticate) UnicastRemoteObject.exportObject(obj, 0);
                registry = LocateRegistry.getRegistry(PathStoreProperties.getInstance().RMIRegistryIP, PathStoreProperties.getInstance().RMIRegistryPort);
                registry.bind("PathStoreAuthenticate", stub);

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public boolean authenticateSession(String sid, String username, String password) {
        //int myEdge = PathStoreProperties.getInstance().NodeID;

        Registry registry = null;
        PathStoreProperties p = PathStoreProperties.getInstance();
        if (p.role == Role.CLIENT) {
            if (serverstub == null) {
                try {
                    registry = LocateRegistry.getRegistry(PathStoreProperties.getInstance().RMIRegistryIP, PathStoreProperties.getInstance().RMIRegistryPort);
                    serverstub = (PathStoreAuthenticate) registry.lookup("PathStoreAuthenticate");
                    return serverstub.authenticateSession(sid, username, password);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        } else {

            PathStoreSession s = PathStoreCluster.getInstance().connect();

            Select slct = QueryBuilder.select().from("pathstore_applications", "session");
            slct.where(QueryBuilder.eq("sid", sid));
            Row r = s.executeLocal(slct, sid).one();

            if (r != null)
                if (r.getString("username").equals(username)) {
                    if (r.getString("state").equals("authenticated"))
                        return true;
                }


            //if not try authenticating the session
            slct = QueryBuilder.select().from("pathstore_applications", "users");
            slct.where(QueryBuilder.eq("username", username));
            r = s.execute(slct).one();

            if (r != null)
                if (r.getString("password").equals(password)) {
                    Insert ins = QueryBuilder.insertInto("pathstore_applications", "session").value("sid", sid)
                            .value("username", username)
                            .value("current_edge", PathStoreProperties.getInstance().NodeID + "")//setting for the first time
                            .value("state", "authenticated");
                    s.executeLocal(ins, sid);
                    return true;
                }
        }

        return false;
    }

    public String getUser(String sid) {
        PathStoreSession s = PathStoreCluster.getInstance().connect();

        Select slct = QueryBuilder.select().from("pathstore_applications", "session");
        slct.where(QueryBuilder.eq("sid", sid));
        Row r = s.executeLocal(slct, sid).one();

        if (r != null)
            if (r.getString("state").equals("authenticated"))
                return r.getString("username");
        return "anonymous";
    }


    public boolean isAuthenticated(String sid) {
        PathStoreSession s = PathStoreCluster.getInstance().connect();

        Select slct = QueryBuilder.select().from("pathstore_applications", "session");
        slct.where(QueryBuilder.eq("sid", sid));
        ResultSet rr = s.executeLocal(slct, sid);
        Row r = rr.one();

        if (r != null)
            if (r.getString("state").equals("authenticated"))
                return true;
        return false;
    }


}

