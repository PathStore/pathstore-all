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

interface LeftProps {
    width: string;
}

/**
 * This is the first child which will be on the left
 *
 * @param props
 * @constructor
 */
export const Left: FunctionComponent<LeftProps> = (props) =>
    <div style={{width: props.width, float: "left"}}>
        {props.children}
    </div>;

/**
 * This is the second child which will be on the right
 *
 * @param props
 * @constructor
 */
export const Right: FunctionComponent<any> = (props) =>
    <div style={{flexGrow: 1}}>
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