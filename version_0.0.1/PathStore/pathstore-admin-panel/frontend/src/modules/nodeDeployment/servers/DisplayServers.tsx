import {Deployment, Server} from "../../../utilities/ApiDeclarations";
import React, {Component} from "react";
import {Table} from "react-bootstrap";
import ModifyServerModal from "./ModifyServerModal";

/**
 * Properties for {@link DisplayServers}
 */
interface DisplayServersProperties {
    /**
     * List of deploy objects from api
     */
    readonly deployment: Deployment[]

    /**
     * List of server objects from api
     */
    readonly servers: Server[]

    /**
     * Callback function to refresh all component props
     */
    readonly forceRefresh: () => void
}

/**
 * State definition for {@link DisplayServers}
 */
interface DisplayServersState {
    /**
     * Whether to show the modal or not
     */
    readonly modifyServerModalShow: boolean

    /**
     * Server info to send to modal
     */
    readonly modifyServerModalData: Server | undefined
}

/**
 * This component is used to render a table to display all created servers
 *
 * @param props
 * @constructor
 */
export default class DisplayServers extends Component<DisplayServersProperties, DisplayServersState> {

    constructor(props: DisplayServersProperties) {
        super(props);

        this.state = {
            modifyServerModalShow: false,
            modifyServerModalData: undefined
        }
    }

    /**
     * TODO: Check to ensure the server is editable
     *
     * Delete object using the api. Then force refresh the data so the server is removed on the client side
     *
     * @param server
     */
    handleClick = (server: Server): void => {
        if (!new Set<string>(this.props.deployment.map(i => i.server_uuid)).has(server.server_uuid))
            this.setState({modifyServerModalShow: true, modifyServerModalData: server});
    };

    /**
     * Callback function for modal to close itself
     */
    modalCallback = () => this.setState({modifyServerModalShow: false, modifyServerModalData: undefined});

    /**
     * Render table
     */
    render() {
        const tbody = [];

        for (let i = 0; i < this.props.servers.length; i++)
            tbody.push(
                <ServerRow key={i} server={this.props.servers[i]} handleClick={this.handleClick}/>
            );

        const modal =
            this.state.modifyServerModalShow ?
                <ModifyServerModal show={this.state.modifyServerModalShow}
                                   callback={this.modalCallback}
                                   server={this.state.modifyServerModalData}
                                   forceRefresh={this.props.forceRefresh}/>
                :
                null;

        return (
            <div>
                {modal}
                <Table>
                    <thead>
                    <tr>
                        <th>Server UUID</th>
                        <th>IP</th>
                        <th>Username</th>
                        <th>SSH Port</th>
                        <th>RMI Port</th>
                        <th>Server Name</th>
                    </tr>
                    </thead>
                    <tbody>
                    {tbody}
                    </tbody>
                </Table>
                <p>Click on a free server record to modify it</p>
            </div>
        );
    }
};

/**
 * Server Row properties
 */
interface ServerRowProps {
    /**
     * Server object to display and to return on click
     */
    readonly server: Server;

    /**
     * What function to call on click
     */
    readonly handleClick: (server: Server) => void
}

/**
 * This component is used to render a given row in the servers display table. It is also used to all for the on click
 * function to have a server object as the property so that there doesn't need to be additional parsing of data
 */
class ServerRow extends Component<ServerRowProps> {

    /**
     * Call given on click function and pass server object
     */
    handleClick = () => this.props.handleClick(this.props.server);

    /**
     * Render the table record
     */
    render() {
        return (
            <tr onClick={this.handleClick}>
                <td>{this.props.server.server_uuid}</td>
                <td>{this.props.server.ip}</td>
                <td>{this.props.server.username}</td>
                <td>{this.props.server.ssh_port}</td>
                <td>{this.props.server.rmi_port}</td>
                <td>{this.props.server.name}</td>
            </tr>
        );
    }
}