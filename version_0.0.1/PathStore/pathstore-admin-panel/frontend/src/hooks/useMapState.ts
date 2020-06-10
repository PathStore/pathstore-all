import {useCallback, useEffect, useState} from "react";

/**
 * This custom hook is used to store data in a map and return different values based on the key that is passed.
 *
 * For example this is used when you have the same modal but a different property is passed i.e. a keyspace
 * You want to store all changes from the previous modal but not have those changes shown when you load a different
 * modal. This is essentially a caching hook.
 *
 * @param key key to load
 */
export function useMapState<K, V>(key: K | undefined): [V[], (v: V[]) => void] {

    // store map in state
    const [map, updateMap] = useState<Map<K, V[]>>(new Map<K, V[]>());

    // value to respond with
    const [value, updateValue] = useState<V[]>([]);

    // Function to generated an update map and add a value set
    const updateMapAndValue = useCallback((map: Map<K, V[]>, v: V[]): Map<K, V[]> => {
            if (key) {
                map = map.set(key, v);

                const updated = map.get(key);
                if (updated) updateValue(updated);
            }
            return map;
        },
        [key, updateValue]
    );

    // update function
    const update = useCallback(
        (v: V[]): void => {
            if (key) {
                const original = map.get(key);

                if (original)
                    updateMap(map => updateMapAndValue(map, v));
            }
        },
        [key, map, updateMap, updateMapAndValue]
    );

    // this effect updates is called when the key changes to load the data or set the data initially
    useEffect(
        () => {
            if (key) {
                const original: V[] | undefined = map.get(key);

                if (original) updateValue(original);
                else updateMap(map => updateMapAndValue(map, []));
            }

        },
        [key, updateValue, map, updateMap, updateMapAndValue]
    );

    return [value, update];
}