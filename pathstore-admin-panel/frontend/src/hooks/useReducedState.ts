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