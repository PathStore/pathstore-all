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