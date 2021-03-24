import "core-js/stable";
import "regenerator-runtime/runtime";
import '@babel/polyfill'
import React from 'react';
import { useEffect, useState } from "react";
import ReactDOM from 'react-dom';

function Rooms() {
    let rooms = useState([]);

    return (
        <ul>
            <button>Host</button>
            <li>Test 6</li>
        </ul>
    );
}

ReactDOM.render(
    <React.StrictMode>
        <Rooms />
    </React.StrictMode>,
    document.getElementById('root')
);
