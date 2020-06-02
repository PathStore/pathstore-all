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