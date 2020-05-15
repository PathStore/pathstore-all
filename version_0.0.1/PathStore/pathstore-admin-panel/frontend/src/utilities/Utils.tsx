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