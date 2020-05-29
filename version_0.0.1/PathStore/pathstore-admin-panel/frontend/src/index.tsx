import React from 'react';
import ReactDOM from 'react-dom';
import './stylesheets/bootstrap-dracula.scss'
import './stylesheets/node_colours.scss'
import PathStoreControlPanel from './PathStoreControlPanel';

ReactDOM.render(
  <React.StrictMode>
    <PathStoreControlPanel />
  </React.StrictMode>,
  document.getElementById('root')
);
