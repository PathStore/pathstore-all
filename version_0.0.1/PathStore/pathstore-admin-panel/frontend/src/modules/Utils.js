/**
 * Simple function to be used on the response of a potentially errorable web request.
 *
 * If the status is less than 400 then the catch block will be used. Else it will resolve normally
 *
 * @param response
 * @returns {Promise<unknown>}
 */
export function webHandler(response) {
    return new Promise((resolve, reject) => {
        let func = response.status < 400 ? resolve : reject;
        response.json().then(data => func(data));
    });
}

/**
 * This function checks if some value is inside an array
 *
 * @param array array to check
 * @param value value to check
 * @returns {boolean}
 */
export function contains(array, value) {
    if (array == null) return false;

    for (let i = 0; i < array.length; i++)
        if (array[i] === value) return true;

    return false;
}

/**
 * Parses a deployment object
 *
 * @param object
 * @returns {{process_status: string, new_node_id: number, server_uuid: *, parent_node_id: number}}
 */
export function createDeploymentObject(object) {
    return {
        new_node_id: parseInt(object.new_node_id),
        parent_node_id: parseInt(object.parent_node_id),
        process_status: object.process_status,
        server_uuid: object.server_uuid
    }
}

/**
 * Parses a server object
 *
 * @param object
 * @returns {{ip: string | string, name: *, server_uuid: *, username: string | T | string}}
 */
export function createServerObject(object) {
    return {
        server_uuid: object.server_uuid,
        ip: object.ip,
        username: object.username,
        name: object.name
    }
}

/**
 * Parses an application object
 *
 * @param object
 * @returns {{keyspace_name: *}}
 */
export function createApplicationObject(object) {
    return {
        keyspace_name: object.keyspace_name
    }
}

/**
 * Parses an application status object
 *
 * @param object
 * @returns {{keyspace_name: *, process_status: string, process_uuid: *, wait_for: *, nodeid: number}}
 */
export function createApplicationStatusObject(object){
    return {
        nodeid: parseInt(object.nodeid),
        keyspace_name: object.keyspace_name,
        process_status: object.process_status,
        wait_for: object.wait_for,
        process_uuid: object.process_uuid
    }
}