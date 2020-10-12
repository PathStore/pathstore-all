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