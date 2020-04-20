import React, {Component} from "react";
import Modal from "react-modal";
import Tree from "react-tree-graph";
import '../Circles.css';

/**
 * This class is used to display the response of an installation request. It may be a success or error
 *
 * Props:
 * show: to show the modal or not
 * topology: topology array of network
 * data: response from api request
 * callback: to close the modal
 */
export default class InstallationResponseModal extends Component {

    /**
     * Takes in the response from the api and calls either the success or error parser
     * @param data response from api
     * @returns {*}
     */
    parseData = (data) => {
        return Array.isArray(data) ? this.parseSuccess(data) : this.parseError(data);
    };

    /**
     * Parses the json array into a object of what keyspace is installing, what the job UUID is and
     * a list of nodes that the application is getting installed on
     *
     * @param jsonArray
     * @returns {{keyspace: *, data: [], process_uuid}}
     */
    parseSuccess = (jsonArray) => {

        const keyspace = jsonArray[0].keyspace_name;
        const process_uuid = jsonArray[0].process_uuid;

        const data = [];

        for (let i = 0; i < jsonArray.length; i++)
            data.push(parseInt(jsonArray[i].nodeid));

        return {
            keyspace: keyspace,
            process_uuid: process_uuid,
            data: data
        };
    };

    /**
     * Returns the error attribute of the json object.
     * @param error
     * @returns {*}
     */
    parseError = (error) => {
        return error.error;
    };

    /**
     * Checks if value is in array
     *
     * @param array
     * @param value
     * @returns {boolean}
     */
    contains = (array, value) => {
        for (let i = 0; i < array.length; i++)
            if (array[i] === value) return true;
        return false;
    };


    /**
     * Creates interpretable json object for the Tree package
     *
     * @param array from parent
     * @param parentId -1 for initial call to look for the root node and work way down
     * @param updates used to determine css class
     * @returns {{textProps: {x: number, y: number}, children: [], name: *}|[]}
     */
    createTree = (array, parentId, updates) => {
        let children = [];

        for (let i = 0; i < array.length; i++)
            if (parentId === -1) {
                if (array[i].parentid === parentId) return this.createTreeObject(array[i].id, array, updates);
            } else {
                if (array[i].parentid === parentId) children.push(this.createTreeObject(array[i].id, array, updates));
            }


        return children;
    };

    /**
     * Name is the node id, textProps is the location of the text associated with the node, children is a list of children
     *
     * @param name current node
     * @param array topology array
     * @param updates if the name is in updates then the css class is installation else its the regular class
     * @returns {{textProps: {x: number, y: number}, children: ({textProps: {x: number, y: number}, children: *[], name: *}|*[]), name: *}}
     */
    createTreeObject = (name, array, updates) => {
        const contains = this.contains(updates, parseInt(name));
        console.log(contains);
        return {
            name: name,
            textProps: {x: -20, y: 25},
            pathProps: {className: (contains ? 'installation_path' : 'regular_path')},
            gProps: {className: (contains ? 'installation_node' : 'node')},
            children: this.createTree(array, name, updates)
        }
    };

    /**
     * @returns {*}
     */
    render() {

        const parsedData = this.parseData(this.props.data);

        const data = typeof parsedData === 'object' ? this.createTree(this.props.topology, -1, parsedData.data) : null;

        const tree = (data != null ?
            <div>
                <p>Installing {parsedData.keyspace} with Job UUID: {parsedData.process_uuid}</p>
                <Tree
                    data={data}
                    nodeRadius={15}
                    margins={{top: 20, bottom: 10, left: 20, right: 200}}
                    height={1000}
                    width={1080}
                    gProps={{className: 'node'}}/>
            </div> : <p>{parsedData}</p>);

        return (
            <Modal isOpen={this.props.show} style={{overlay: {zIndex: 1}}}>
                {tree}
                <button onClick={this.props.callback}>close</button>
            </Modal>
        );
    }
}