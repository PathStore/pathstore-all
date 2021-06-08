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
export function useCachedState<K, V>(key: K | undefined): [V[], (v: V[]) => void] {

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