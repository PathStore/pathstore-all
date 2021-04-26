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

import React, {FunctionComponent} from "react";

/**
 * This is the parent component for two aligned divs
 *
 * @param props
 * @constructor
 */
export const AlignedDivs: FunctionComponent<any> = (props) =>
    <div style={{display: 'flex'}}>
        {props.children}
    </div>;

/**
 * Props for {@link Left}
 */
interface LeftProps {
    /**
     * Width percentage of div
     */
    width: string;
}

/**
 * This is the first child which will be on the left
 *
 * @param props
 * @constructor
 */
export const Left: FunctionComponent<LeftProps> = (props) =>
    <div style={{flex: '0 0 ' + props.width}}>
        {props.children}
    </div>;


/**
 * This is the second child which will be on the right
 *
 * @param props
 * @constructor
 */
export const Right: FunctionComponent = (props) =>
    <div style={{flex: 1}}>
        {props.children}
    </div>;

/**
 * Centered div
 *
 * @param props
 * @constructor
 */
export const Center: FunctionComponent<any> = (props) =>
    <div style={{textAlign: 'center'}}>
        {props.children}
    </div>;