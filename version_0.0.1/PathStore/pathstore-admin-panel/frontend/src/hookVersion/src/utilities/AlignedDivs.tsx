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