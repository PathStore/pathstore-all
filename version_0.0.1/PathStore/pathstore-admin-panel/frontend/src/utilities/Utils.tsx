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
export function genericLoadFunctionWait<T extends unknown>(url: string, update: ((v: T[]) => void) | undefined): void {
    if (update)
        genericLoadFunctionBase<T>(url).then((response: T[]) => update(response));
}

/**
 * This function is used to return an array of parsed object data from the api.
 *
 * @param url url to query
 */
function genericLoadFunctionBase <T extends unknown>(url: string): Promise<T[]> {
    return fetch(url)
        .then(response => response.json() as Promise<T[]>);
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