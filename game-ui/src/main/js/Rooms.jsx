import "core-js/stable";
import "regenerator-runtime/runtime";
import '@babel/polyfill'
import {MESSAGE_RSOCKET_ROUTING, encodeCompositeMetadata, encodeRoute} from "rsocket-core";
import React, {useEffect, useRef, useState} from 'react';
import ReactDOM from 'react-dom';
import {connect} from "./RSocket.js"
import {adjectives, colors} from "unique-names-generator";
import {xyz} from './flatbuffers/RoomEvent_generated'
import {flatbuffers} from "flatbuffers";
import {Single} from "rsocket-flowable/build";

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
    const [userId, setUserId] = useState(undefined);
    const socket = useRef(null);

    useEffect(async () => {
        const [_, rSocket] = await connect(userName, {
            requestResponse(m) {
                setUserId(m.data.toString());
                return Single.of(m);
            }
        })
        socket.current = rSocket;
        rSocket.requestStream({
            metadata: encodeCompositeMetadata([
                [MESSAGE_RSOCKET_ROUTING, encodeRoute('game.rooms')],
            ]),
        }).subscribe({
            onSubscribe(s) {
                s.request(2147483642)
            },
            onNext(roomEventBuffer) {
                const dataBuf = flatbuffers.ByteBuffer.allocate(roomEventBuffer.data);
                const event = xyz.bomberman.room.data.RoomEvent.getRootAsRoomEvent(dataBuf);
                const eventType = event.type();
                const roomId = event.id();
                const playersIds = [...new Array(event.playersLength()).keys()].map(i => event.players(i))
                console.log(event);
                // update all displayed rooms
                setRooms(rooms => {
                    console.log(rooms);
                    if (eventType === xyz.bomberman.room.data.EventType.Added) {
                        return [{
                            id: roomId,
                            users: playersIds
                        }, ...rooms]
                    } else if(eventType === xyz.bomberman.room.data.EventType.Updated) {
                        return [{
                            id: roomId,
                            users: playersIds
                        }, ...rooms.filter(room => room.id !== roomId)]
                    }

                    // remove empty rooms
                    return rooms.filter(room => room.id !== roomId);
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
            metadata: encodeCompositeMetadata([
                [MESSAGE_RSOCKET_ROUTING, encodeRoute('game.rooms.create')],
            ]),
            data: Buffer.from(roomId)
        }).subscribe()
    }

    function joinGame(roomId) {
        const rSocket = socket.current;
        rSocket.requestResponse({
            metadata: encodeCompositeMetadata([
                [MESSAGE_RSOCKET_ROUTING, encodeRoute(`game.rooms.${roomId}.join`)],
            ]),
        }).subscribe()
    }

    function leaveGame(roomId) {
        const rSocket = socket.current;
        rSocket.requestResponse({
            metadata: encodeCompositeMetadata([
                [MESSAGE_RSOCKET_ROUTING, encodeRoute(`game.rooms.${roomId}.leave`)],
            ]),
        }).subscribe()
    }

    function startGame(roomId) {
        const rSocket = socket.current;
        rSocket.requestResponse({
            metadata: encodeCompositeMetadata([
                [MESSAGE_RSOCKET_ROUTING, encodeRoute(`game.rooms.${roomId}.start`)],
            ]),
        }).subscribe()
    }

    const inAGame = rooms.filter(room => room.users.includes(userId)).length > 0;
    return (
        <div className={"rooms"}>
            <div>Welcome, {userName} ({userId})</div>
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
                            {room.users.includes(userId)
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


