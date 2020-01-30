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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * TODO: Migrate public variable to either private variables or to public final variables to allow for consistent data.
 * TODO: Instead of a constant class that points to where the file is suppose to be located allow for the user to define their properties file location via cmd line arguments
 * <p>
 * This class loads in data from the properties file and writes it to their respective fields.
 */
public class PathStoreProperties {
    /**
     * Represents an instance of the properties file. This will only every be initialized once.
     *
     * @see #getInstance()
     */
    static private PathStoreProperties instance = null;

    /**
     * Initializes the path store properties instance variable
     *
     * @return either a new copy or the currently initialized instance
     */
    static public PathStoreProperties getInstance() {
        if (PathStoreProperties.instance == null)
            PathStoreProperties.instance = new PathStoreProperties();
        return PathStoreProperties.instance;
    }

    /**
     * TODO: Make this non-user definable. To eliminate duplicates
     * Current Node's Identification number
     */
    public int NodeID = 1;

    /**
     * TODO: Make this non-user definable. To eliminate duplicates
     * Parent Node's Identification number
     */
    public int ParentID = -1;

    /**
     * TODO: Read up on the way RMI is being used and document the following variables
     */
    public String RMIRegistryIP = null;
    public int RMIRegistryPort = 1099;
    public String RMIRegistryParentIP = null;
    public int RMIRegistryParentPort = 1100;

    /**
     * Role of the user interacting with the network
     * There are:
     * <p>
     * CLIENT: When the client uses this library to interact with the network
     * SERVER: Some server in the network that is not the root node
     * ROOTSERVER: The root server.
     * <p>
     * The bottom two are relevant for where to store data as SERVER will have garbage collection
     * and ROOTSERVER will not have any garbage collection as the master copy of the data stays there
     */
    public Role role = null;

    /**
     * TODO: Find meaning for this
     */
    public String CassandraPath = null;

    /**
     * Points to the ip address of the cassandra instance attached to this node
     */
    public String CassandraIP = null;

    /**
     * Points to the port number of the local cassandra instance
     */
    public int CassandraPort = 0;

    /**
     * Points to the ip address of the parent node's local cassandra instance
     */
    public String CassandraParentIP = null;

    /**
     * Points to the port number of the parent node's local cassandra instance
     */
    public int CassandraParentPort = 0;

    /**
     * TODO: Find meaning for this
     */
    public int MaxBatchSize = 4096 * 10; //

    /**
     * TODO: Find meaning for these
     */
    public int PullSleep = 1000;   // sleep period in milliseconds
    public int PushSleep = 1000;   // sleep period in milliseconds

    /**
     * TODO: Fix constant strings, either move them to a constant file or write a dynamic attribute loaded
     * TODO: Fix potential non-existent critical attributes i.e local cassandra instance ip and port
     *
     * Reads properties file and sets all the fields of the class according to the data loaded in from the file
     *
     * @see Constants#PROPERTIESFILE
     */
    public PathStoreProperties() {
        try {
            Properties props = new Properties();
            FileInputStream in = new FileInputStream(Constants.PROPERTIESFILE);
            props.load(in);

            this.RMIRegistryIP = props.getProperty("RMIRegistryIP");
            this.RMIRegistryPort = Integer.parseInt(props.getProperty("RMIRegistryPort"));

            this.RMIRegistryParentIP = props.getProperty("RMIRegistryParentIP");

            String tmpp = props.getProperty("ParentID");
            if (tmpp != null)
                this.ParentID = Integer.parseInt(tmpp);

            if (this.RMIRegistryParentIP != null)
                this.RMIRegistryParentPort = Integer.parseInt(props.getProperty("RMIRegistryParentPort"));

            switch (props.getProperty("Role")) {
                case "ROOTSERVER":
                    this.role = Role.ROOTSERVER;
                    break;
                case "SERVER":
                    this.role = Role.SERVER;
                    break;
                default:
                    this.role = Role.CLIENT;
            }

            this.CassandraPath = props.getProperty("CassandraPath");

            this.CassandraIP = props.getProperty("CassandraIP");

            String temp = props.getProperty("CassandraPort");
            this.CassandraPort = temp != null ? Integer.parseInt(temp) : 9042;

            temp = props.getProperty("CassandraParentIP");
            this.CassandraParentIP = temp != null ? props.getProperty("CassandraParentIP") : "127.0.0.1";

            temp = props.getProperty("CassandraParentPort");
            this.CassandraParentPort = temp != null ? Integer.parseInt(temp) : 9062;

            temp = props.getProperty("MaxBatchSize");
            this.MaxBatchSize = temp != null ? Integer.parseInt(temp) : MaxBatchSize;

            temp = props.getProperty("PullSleep");
            this.PullSleep = temp != null ? Integer.parseInt(temp) : 1000;

            temp = props.getProperty("PushSleep");
            this.PushSleep = temp != null ? Integer.parseInt(temp) : 1000;

            temp = props.getProperty("NodeID");
            this.NodeID = temp != null ? Integer.parseInt(temp) : 1;


            in.close();
        } catch (Exception ex) {
            // TODO log exception
        }
    }

}
