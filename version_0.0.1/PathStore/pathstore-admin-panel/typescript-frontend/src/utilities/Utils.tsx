import React from "react";
import {Deployment, Server} from "./ApiDeclarations";

/**
 * Format the server information about the currently selected node
 *
 * @param deployment
 * @param servers
 * @param node
 * @returns {*}
 */
export function formatServer(deployment: Deployment[], servers: Server[], node: number) {

    const deployObject = deployment.filter(i => i.new_node_id === node);

    const object = servers.filter(i => i.server_uuid === deployObject[0].server_uuid);

    return <div>
        <p>Server Information</p>
        <p>UUID: {object[0].server_uuid}</p>
        <p>IP: {object[0].ip}</p>
        <p>Username: {object[0].username}</p>
        <p>Name: {object[0].name}</p>
    </div>;
}

/**
 * Simple function to be used on the response of a potentially errorable web request.
 *
 * If the status is less than 400 then the catch block will be used. Else it will resolve normally
 *
 * @param response
 * @returns {Promise<T>}
 */
export function webHandler<T>(response: { status: number; json: () => Promise<T>; }): Promise<T> {
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
export function contains<T>(array: T[], value: T) {
    if (array == null) return false;

    for (let i = 0; i < array.length; i++)
        if (array[i] === value) return true;

    return false;
}