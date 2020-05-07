import React, {Component} from "react";
import NodeInfoModal from "./NodeInfoModal";
import PathStoreTopology from "../PathStoreTopology";

export default class ViewTopology extends Component {

    constructor(props) {
        super(props);
        this.state = {
            info: null,
            showInfo: false
        };
    }

    getClassName = (object) => {
        switch (object.process_status) {
            case "WAITING_DEPLOYMENT":
                return 'waiting_node';
            case "DEPLOYING":
                return 'installing_node';
            case "FAILED":
                return 'uninstalled_node';
            default:
                return 'installed_node';
        }
    };

    handleClick = (event, node) => {
        this.setState({
            info: node,
            showInfo: true
        })
    };

    callback = () => this.setState({showInfo: false});

    render() {

        const modal = this.state.showInfo ?
            <NodeInfoModal node={this.state.info}
                           show={this.state.showInfo}
                           topology={this.props.topology}
                           applicationStatus={this.props.applicationStatus}
                           servers={this.props.servers}
                           callback={this.callback}/> : null;

        return (
            <div>
                <p>Click on a node to view its current applications</p>
                <PathStoreTopology topology={this.props.topology}
                                   get_colour={this.getClassName}
                                   get_click={this.handleClick}/>
                {modal}
            </div>
        );
    }
}