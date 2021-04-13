"use strict";

import "core-js/stable";
import "regenerator-runtime/runtime";
import '@babel/polyfill'
import React, {useEffect, useRef, useState} from 'react';
import {connect} from "./RSocket.js"
import Game from "./Game.js";
import _ from 'lodash';
import './main.css';
import {
    ITEM_EXTRA_LIFE,
    ITEM_EXTRA_BOMB,
    ITEM_RUN_FASTER,
    BOMBERMAN_BURNED,
    BOMBERMAN_ENEMY,
    BOMBERMAN_LOCAL,
    WALL_BROWN,
    WALL_GREY,
    BOMB,
    FIRE,
} from "./constant.js";
import ReactDOM from "react-dom";
import {Rooms} from "./Rooms.jsx";


export class AssetLoader {
    loadAsset(name, url) {
        return new Promise((resolve, reject) => {
            const image = new Image();
            image.src = url;
            image.addEventListener('load', function() {
                return resolve({ name, image: this });
            });
        });
    }

    loadAssets(assetsToLoad) {
        return Promise.all(
            assetsToLoad.map(asset => this.loadAsset(asset.name, asset.url))
        ).then(assets =>
            assets.reduceRight(
                (acc, elem) => ({ ...acc, [elem.name]: elem.image }),
                {}
            )
        );
    }
}

new AssetLoader()
    .loadAssets([
        { name: BOMBERMAN_LOCAL, url: '../images/bomberman.png' },
        { name: WALL_BROWN, url: '../images/wall.png' },
        { name: ITEM_EXTRA_LIFE, url: '../images/spoilLife.png' },
        { name: ITEM_RUN_FASTER, url: '../images/spoilRun.png' },
        { name: ITEM_EXTRA_BOMB, url: '../images/spoilBomb.png' },
        { name: BOMB, url: '../images/bomb.png' },
        { name: WALL_GREY, url: '../images/grid_option2.png' },
        { name: FIRE, url: '../images/fire.png' },
        { name: BOMBERMAN_ENEMY, url: '../images/enemy_bomberman.png'},
        { name: BOMBERMAN_BURNED, url: '../images/burned_bomberman.png'}
    ])
    .then(assets => {
        console.log("AAAA")
        ReactDOM.render(
            <React.StrictMode>
                <Rooms/>
            </React.StrictMode>,
            document.getElementById('root')
        );
        let game = null;
        let id = '';

        document.querySelector("#login").addEventListener("click", function(event) {

            // prevent refresh of the current screen
            event.preventDefault();

            // select input node of DOM by its ID
            id = document.querySelector("#lname").value;

            // initialize game when nickname has at least 1 character
            if (id !== '') {
                game = new Game("myCanvas", 13, 13, assets, id);
            }

        }, false);

        document.querySelector("#you_suck_button").addEventListener('click', function (event){
            event.preventDefault();
            const YOU_SUCK = "you_suck";
            if (game != null) {
                game.broadcastReaction(YOU_SUCK);
                game.drawReaction({id: game.id, reaction: YOU_SUCK})
            }
        });

        document.querySelector('#love_button').addEventListener('click', function (event) {
            event.preventDefault();
            const LOVE = 'love';
            if (game != null) {
                game.broadcastReaction(LOVE);
                game.drawReaction({id: game.id, reaction: LOVE})
            }
        });

        document.querySelector('#lol_button').addEventListener('click', function (event) {
            event.preventDefault();
            const LOL = 'lol';
            if (game != null) {
                game.broadcastReaction(LOL);
                game.drawReaction({id: game.id, reaction: LOL})
            }
        });

        const urlParams = new URLSearchParams(window.location.search);
        const username = urlParams.get('username');
        if (username) {
            document.querySelector("#lname").setAttribute("value", username);
            document.querySelector("#login").click()
        }

    }).catch(err => {
        console.log(err);
        // window.open(`http://stackoverflow.com/search?q=[js]+${err}`);
        //window.location.href = "http://stackoverflow.com/search?q=[js]+" + err;
});








