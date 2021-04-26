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