/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import React, {FunctionComponent} from "react";
import {Deployment, Server} from "../../utilities/ApiDeclarations";

/**
 * Properties definition for {@link ServerInfo}
 */
interface ServerInfoProps {
    /**
     * List of deployment objects from api
     */
    readonly deployment: Deployment[] | undefined

    /**
     * List of server objects from api
     */
    readonly servers: Server[] | undefined

    /**
     * Which node number to display info for
     */
    readonly node: number | undefined
}

/**
 * This component will render the server information for a node based on the node id.
 *
 * The server_uuid is stored within the deployment object where the node_id correlates with given node id
 *
 * @param props
 * @constructor
 */
export const ServerInfo: FunctionComponent<ServerInfoProps> = (props) => {

    if (props.deployment && props.servers && props.node) {

        const deployObject = props.deployment.filter(i => i.new_node_id === props.node)[0];

        if (deployObject === undefined) return null;

        const object = props.servers.filter(i => i.server_uuid === deployObject.server_uuid);

        return (
            <div>
                <h2>Server Information</h2>
                <p>UUID: {object[0].server_uuid}</p>
                <p>IP: {object[0].ip}</p>
                <p>Username: {object[0].username}</p>
                <p>SSH Port: {object[0].ssh_port}</p>
                <p>GRPC Port: {object[0].grpc_port}</p>
                <p>Name: {object[0].name}</p>
            </div>
        );
    } else return null;
};