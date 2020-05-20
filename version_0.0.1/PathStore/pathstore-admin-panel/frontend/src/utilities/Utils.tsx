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