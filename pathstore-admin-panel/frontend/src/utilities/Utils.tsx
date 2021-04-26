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
 * This function is used to query from a url and store it in the state with the wait condition
 *
 * @param url url to query
 * @param update function to update this with
 */
export function genericLoadFunction<T extends unknown>(url: string, update: ((v: T[]) => void) | undefined): void {
    if (update)
        fetch(url)
            .then(response => response.json() as Promise<T[]>).then((response: T[]) => update(response));
}

/**
 * Function to produce a map based on a single data set.
 *
 * @param keyFunc how to transform the key based on a given V
 * @param valueFunc how to transform the value based on a given V (if any)
 * @param data data set to produce a map from
 */
export function createMap<K, V>(keyFunc: (value: V) => K, valueFunc: (value: V) => V, data: V[]): Map<K, V> {
    const map: Map<K, V> = new Map<K, V>();

    data.forEach(v => map.set(keyFunc(v), valueFunc(v)));

    return map;
}

/**
 * Identity function used for createMap valueFunc where applicable
 *
 * @param t some value
 */
export const identity = <T extends unknown>(t: T) => t;