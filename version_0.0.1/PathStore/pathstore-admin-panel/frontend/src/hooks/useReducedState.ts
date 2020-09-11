import {useEffect, useState} from "react";

/**
 * This hook is used to take a set of data and reduce it by some filter function and store it internally in your
 * components state.
 *
 * @param data data to filter
 * @param filterFunc how to filter it
 */
export function useReducedState<T>(data: T[] | undefined, filterFunc: (v: T) => boolean): T[] {
    // set up state
    const [state, updateState] = useState<T[]>([]);

    // update state everytime the data changes
    useEffect(() => updateState(data ? data.filter(filterFunc) : []), [data, filterFunc]);

    return state;
}