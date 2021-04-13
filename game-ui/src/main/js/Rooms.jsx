import "core-js/stable";
import "regenerator-runtime/runtime";
import '@babel/polyfill'
import React, {useEffect, useRef, useState} from 'react';
import ReactDOM from 'react-dom';
import {connect} from "./RSocket.js"
import {adjectives, colors} from "unique-names-generator";

const {uniqueNamesGenerator, animals} = require('unique-names-generator');

const userName = uniqueNamesGenerator({
    dictionaries: [animals],
    length: 1
});

function meta(name) {
    return String.fromCharCode(`game.rooms.${name}`.length) + `game.rooms.${name}`;
}

export function Rooms() {
    const [rooms, setRooms] = useState([]);
    const socket = useRef(null);

    useEffect(async () => {
        const [_, rSocket] = await connect(userName)
        socket.current = rSocket;
        rSocket.requestStream({
            metadata: meta('list'),
            data: {id: userName}
        }).subscribe({
            onSubscribe(s) {
                s.request(2147483642)
            },
            onNext(t) {
                const room = t.data;
                console.log(t);
                // check if the game has started and you're in the game
                if (room.started && room.users.includes(userName)) {
                    window.location.href = `/game?id=${room.id}&username=${userName}`;
                }
                // update all displayed rooms
                setRooms(rooms => {
                    console.log(rooms);
                    let replaced = false;
                    const out = []
                    for (const existingRoom of rooms) {
                        if (existingRoom.id === room.id) {
                            out.push(room);
                            replaced = true;
                        } else {
                            out.push(existingRoom);
                        }
                    }
                    if (!replaced) {
                        out.push(room);
                    }
                    // remove empty rooms
                    return out.filter(room => room.users.length > 0);
                });
            },
            onError(err) {
                console.error(err);
            },
            onComplete() {
                console.log("complete")
            }
        })
    }, []);

    function switchUI() {
        document.getElementById("gamefield").className = ""
        document.getElementById("root").className = "hidden"
    }

    function createGame() {
        const rSocket = socket.current;
        const roomId = uniqueNamesGenerator({
            dictionaries: [adjectives, colors],
            length: 2,
        });
        rSocket.requestResponse({
            metadata: meta('create'),
            data: {userId: userName, roomId: roomId}
        }).subscribe()
    }

    function joinGame(roomId) {
        const rSocket = socket.current;
        rSocket.requestResponse({
            metadata: String.fromCharCode('joinGame'.length) + 'joinGame',
            data: {userId: userName, roomId: roomId}
        }).subscribe()
    }

    function leaveGame(roomId) {
        const rSocket = socket.current;
        rSocket.requestResponse({
            metadata: String.fromCharCode('leaveGame'.length) + 'leaveGame',
            data: {userId: userName, roomId: roomId}
        }).subscribe()
    }

    function startGame(roomId) {
        const rSocket = socket.current;
        rSocket.requestResponse({
            metadata: String.fromCharCode('startGame'.length) + 'startGame',
            data: {roomId: roomId}
        }).subscribe()
    }

    const inAGame = rooms.filter(room => room.users.includes(userName)).length > 0;
    return (
        <div className={"rooms"}>
            <div>Welcome, {userName}</div>
            <button onClick={switchUI}>SWITCH UI</button><br/>
            {inAGame
                ? <div/>
                : <button onClick={createGame}>Create Game</button>
            }
            <div>
                {rooms.map(room =>
                    <div key={room.id}>
                        <div>
                            <div className={"room"}>
                                <div>Room: {room.id}</div>
                                <div>Players: {room.users.join(",")}</div>
                            </div>
                            {room.users.includes(userName)
                                ? <div style={{float: "left"}}>
                                    <button onClick={() => leaveGame(room.id)}>Leave</button>
                                    <button onClick={() => startGame(room.id)}>Start</button>
                                </div>
                                : (inAGame || room.users.length >= 4
                                    ? <div/>
                                    : <button onClick={() => joinGame(room.id)}>Join</button>)
                            }
                        </div>
                        <hr style={{width: "100%"}}/>
                    </div>
                )}

            </div>
        </div>
    );
}


