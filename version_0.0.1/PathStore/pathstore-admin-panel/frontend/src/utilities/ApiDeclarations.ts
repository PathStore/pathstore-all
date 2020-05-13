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
 * Parse response from any server endpoint
 */
export interface Server {
    server_uuid: string
    ip: string
    username: string
    name: string
}

/**
 * Parse response from application endpoint
 */
export interface Application {
    keyspace_name: string
}

/**
 * Parse response from application management
 */
export interface ApplicationStatus {
    nodeid: number
    keyspace_name: string
    process_status: string
    wait_for: number[]
    process_uuid: string
}

/**
 * Parse log response
 */
export interface Log {
    node_id: number
    dates: LogByDate[]
}

export interface LogByDate {
    date: string
    log: string[]
}

/**
 * Way to format response on deployment update
 */
export interface Update {
    parentId: number
    newNodeId: number
    serverUUID: string
}

/**
 * Parse error response (error code 400)
 */
export interface Error {
    error: string
}

/**
 * Parse application success response
 */
export interface ApplicationCreationSuccess {
    keyspace_created: string
}