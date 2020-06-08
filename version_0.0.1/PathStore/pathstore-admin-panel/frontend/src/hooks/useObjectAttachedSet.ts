import {useEffect, useState} from "react";
import {identity} from "../utilities/Utils";

/**
 * This hook is used for a given set of data to produce a mapped set of data from T -> U
 * @param data data to map
 * @param mapFunc how to map this data
 * @param filterFunc optional param to filter the given data set
 */
export function useObjectAttachedSet<T, U>(data: T[] | undefined, mapFunc: (v: T) => U, filterFunc?: (v: T) => boolean): Set<U> {
    // Store the set internally
    const [set, updateSet] = useState<Set<U>>(new Set<U>());

    // Update the set everytime the data dependency changes
    useEffect(
        () => updateSet(new Set<U>(data ? data.filter(filterFunc ? filterFunc : identity).map(mapFunc) : null)),
        [data, mapFunc, filterFunc]);

    return set;
}