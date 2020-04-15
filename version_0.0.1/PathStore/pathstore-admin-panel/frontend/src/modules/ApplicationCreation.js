import React, {Component} from "react";
import DisplayAvailableApplication from "./DisplayAvailableApplication";
import ApplicationLoader from "./ApplicationLoader";
import LoadingModel from "./LoadingModel";

export default class ApplicationCreation extends Component {

    constructor(props) {
        super(props);

        this.state = {
            applications: [],
            refresh: false,
            loadingModal: false
        };
    }

    componentWillReceiveProps(props, nextContext) {
        if (this.props.refresh !== props.refresh) {
            this.setState({applications: props.applications, refresh: !this.state.refreshInfo})
        }
    }


    refreshComponents = () => {
        this.props.forceRefresh();
        this.setState({refresh: !this.state.refresh, loadingModel: false});
    };

    spawnLoadingModel = () => {
        this.setState({loadingModel: true});
    };

    render() {
        return (
            <div>
                <h2>Application Creation</h2>
                <DisplayAvailableApplication applications={this.state.applications} refresh={this.state.refresh}/>
                <ApplicationLoader applications={this.state.applications} refresh={this.state.refresh}
                                   forceRefresh={this.refreshComponents} spawnLoadingModel={this.spawnLoadingModel}/>
                <LoadingModel show={this.state.loadingModel}/>
            </div>
        )
    }
}