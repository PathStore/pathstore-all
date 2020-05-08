export interface Deployment {
    new_node_id: number
    parent_node_id: number
    process_status: string
    server_uuid: string
}

export interface Server {
    server_uuid: string
    ip: string
    username: string
    name: string
}

export interface Application {
    keyspace_name: string
}

export interface ApplicationStatus {
    nodeid: number
    keyspace_name: string
    process_status: string
    wait_for: number[]
    process_uuid: string
}