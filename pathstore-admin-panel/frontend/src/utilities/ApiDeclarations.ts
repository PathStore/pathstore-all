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

/**
 * Parse response from any deployment endpoint
 */
export interface Deployment {
    new_node_id: number
    parent_node_id: number
    process_status: string
    server_uuid: string
}

/**
 * Deployment states
 */
export enum DEPLOYMENT_STATE {
    "WAITING_DEPLOYMENT",
    "DEPLOYING",
    "PROCESSING_DEPLOYING",
    "DEPLOYED",
    "WAITING_REMOVAL",
    "REMOVING",
    "PROCESSING_REMOVING",
    "FAILED"
}

/**
 * State for {@link Server#auth_type}
 */
export enum SERVER_AUTH_TYPE {
    "PASSWORD",
    "IDENTITY"
}

/**
 * Parse response from any server endpoint
 */
export interface Server {
    server_uuid: string
    ip: string
    username: string
    auth_type: string
    ssh_port: number
    grpc_port: number
    name: string
}

/**
 * Parse response from application endpoint
 */
export interface Application {
    keyspace_name: string
}


// All process statuses possible by the application deployment process
export enum APPLICATION_STATE {
    "WAITING_INSTALL",
    "INSTALLING",
    "PROCESSING_INSTALLING",
    "INSTALLED",
    "WAITING_REMOVE",
    "REMOVING",
    "PROCESSING_REMOVING"
}

/**
 * Parse response from application management
 */
export interface ApplicationStatus {
    node_id: number
    keyspace_name: string
    process_status: string
    wait_for: number[]
}

/**
 * Parse available dates for each node
 */
export interface AvailableLogDates {
    node_id: number
    date: string[]
}

/**
 * Parse log response
 */
export interface Log {
    logs: string[]
}

/**
 * Way to format response on deployment update
 */
export interface DeploymentUpdate {
    parentId: number
    newNodeId: number
    serverUUID: string
}

/**
 * Way to form response on application update
 */
export interface ApplicationUpdate {
    nodeId: number
    keyspaceName: string
    waitFor: number[]
}

/**
 * Parse error response (error code 400)
 */
export interface Error {
    error: string
}