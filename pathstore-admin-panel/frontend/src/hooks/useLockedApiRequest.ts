import {useCallback, useState} from "react";
import {genericLoadFunction} from "../utilities/Utils";

/**
 * This hook is used to execute an api request iff it is ready.
 * A request is only ready to be executed if a previous request to that endpoint has returned a response of some kind
 * regardless if that request errored / timed out.
 *
 * This hook is used in the PathStore Control Panel to 'poll' for data every 2 seconds and there is a case where the
 * requests take longer then 2 seconds to return and we don't want to stack multiples of the same request on top of
 * one another as this is un-needed network bandwidth
 *
 * @param url url to execute
 * @param callback what function to call on response
 */
export function useLockedApiRequest<T>(url: string, callback: ((v: T[]) => void) | undefined): () => void {
    // ready state
    const [ready, setReady] = useState<boolean>(true);

    /**
     * This callback wraps the given callback with a set ready check to true after the callback has completed
     * to signify that the request can be made again.
     */
    const updatedCallback = useCallback((v: T[]): void => {
        if (callback)
            callback(v);
        setReady(true);
    }, [callback]);

    /**
     * This function is returned to be used by the caller on a timer interval
     *
     * @see useInterval
     */
    return useCallback(
        () => {
            if (ready) {
                setReady(false);
                genericLoadFunction<T>(url, updatedCallback);
            }
        },
        [ready, url, updatedCallback]
    );
}