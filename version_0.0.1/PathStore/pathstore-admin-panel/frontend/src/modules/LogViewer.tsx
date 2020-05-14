import React, {FunctionComponent} from "react";
import {AvailableLogDates, Log} from "../utilities/ApiDeclarations";

/**
 * Properties definition for {@link LogViewer}
 */
interface LogViewerProps {
    /**
     * Node id of node to show info of
     */
    readonly node: number

    /**
     * List of available dates for each log
     */
    readonly availableLogDates?: AvailableLogDates[]
}

/**
 * This component is used to display a nodes log to the user in a scrollable text viewer
 *
 *  <div className={"logViewer"}>
 <code id={"log"}>{response}</code>
 </div>
 *
 * @param props
 * @constructor
 */
export const LogViewer: FunctionComponent<LogViewerProps> = (props) => {

    if (props.availableLogDates === undefined) return null;


    return (
        <div className={"logViewer"}>
            <p>{JSON.stringify(props.availableLogDates)}</p>
        </div>
    );
};