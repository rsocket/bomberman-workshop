import "core-js/stable";
import "regenerator-runtime/runtime";
import '@babel/polyfill'
import {encodeCompositeMetadata, encodeRoute, MESSAGE_RSOCKET_ROUTING} from "rsocket-core";
import React, {useEffect, useRef, useState} from 'react';
import {connect} from "./RSocket.js"
import {adjectives, colors} from "unique-names-generator";
import {xyz} from './flatbuffers/RoomEvent_generated'
import {flatbuffers} from "flatbuffers";
import Game from "./Game";

const {uniqueNamesGenerator, animals} = require('unique-names-generator');

const userName = uniqueNamesGenerator({
    dictionaries: [animals],
    length: 1
});

export function Rooms() {
    const [rooms, setRooms] = useState([]);
    const [userId, setUserId] = useState(undefined);
    const [ownedRoomId, setOwnedRoomId] = useState(undefined);
    const socket = useRef(undefined);

    useEffect(async () => {
        const [, rSocket] = await connect(userName, {
            requestChannel(flowable) {
                return launchGame(flowable);
            }
        })
        rSocket.requestResponse({
            metadata: encodeCompositeMetadata([
                [MESSAGE_RSOCKET_ROUTING, encodeRoute('game.players.login')],
            ]),
            data: Buffer.from(userName)
        })
        .subscribe({
            onComplete: (payload) => {
                socket.current = rSocket;

                setUserId(payload.data.toString());

                rSocket.requestStream({
                    metadata: encodeCompositeMetadata([
                        [MESSAGE_RSOCKET_ROUTING, encodeRoute('game.rooms')],
                    ]),
                }).subscribe({
                    onSubscribe(s) {
                        s.request(2147483642)
                    },
                    onNext(eventBuf) {
                        const {eventType, room} = extractRoom(eventBuf);
                        // update all displayed rooms
                        setRooms(rooms => {
                            if (eventType === xyz.bomberman.room.data.EventType.Added) {
                                return [room, ...rooms]
                            } else if (eventType === xyz.bomberman.room.data.EventType.Updated) {
                                return [room, ...rooms.filter(r => r.id !== room.id)]
                            }
                            // remove empty rooms
                            return rooms.filter(r => r.id !== room.id);
                        });
                    },
                    onError(err) {
                        console.error(err);
                    },
                    onComplete() {
                        console.log("complete")
                    }
                })
            }
        })
    }, []);

    function launchGame(flowable) {
        const game = new Game("myCanvas", 13, 13, window.assets, userName);
        window.game = game;

        document.querySelector("#lname").setAttribute("value", userName);
        document.getElementById("gamefield").className = ""
        document.getElementById("root").className = "hidden"

        return game.start(flowable)
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
        }).then(payload => setOwnedRoomId(payload.data.toString()))
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
                [MESSAGE_RSOCKET_ROUTING, encodeRoute(`game.rooms.${roomId}.${ownedRoomId === roomId ? "close" : "leave"}`)],
            ]),
        }).subscribe()
        setOwnedRoomId(undefined)
    }

    async function startGame(roomId) {
        const rSocket = socket.current;
        rSocket.requestResponse({
            metadata: encodeCompositeMetadata([
                [MESSAGE_RSOCKET_ROUTING, encodeRoute(`game.rooms.${roomId}.start`)],
            ]),
        }).subscribe()
    }

    const inAGame = rooms.filter(room => room.players.map(p => p.id).includes(userId)).length > 0;
    return (
        <div className={"rooms"}>
            <div>Welcome, {userName} ({userId})</div>
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
                                <div>Owner: {room.owner.name} Players: {room.players.map(p => p.name).join(", ")}</div>
                            </div>
                            {room.players.map(p => p.id).includes(userId)
                                ? <div style={{float: "left"}}>
                                    {ownedRoomId === room.id
                                        ? <button onClick={() => leaveGame(room.id)}>Close</button>
                                        : <button onClick={() => leaveGame(room.id)}>Leave</button>}
                                    {ownedRoomId === room.id && room.players.length > 1
                                        ? <button onClick={() => startGame(room.id)}>Start</button>
                                        : <div/>}
                                </div>
                                : (inAGame || room.players.length >= 4
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

function extractRoom(roomEventBuffer) {
    const dataBuf = flatbuffers.ByteBuffer.allocate(roomEventBuffer.data);
    const event = xyz.bomberman.room.data.RoomEvent.getRootAsRoomEvent(dataBuf);
    const eventType = event.type();
    const roomId = event.id();
    const players = [...new Array(event.playersLength()).keys()]
        .map(i => {
            const player = event.players(i);
            return {
                id: player.id(),
                name: player.name(),
            };
        })
    let playerOwner = event.owner();
    const owner = {
        id: playerOwner.id(),
        name: playerOwner.name(),
    };
    const room = {
        id: roomId,
        owner: owner,
        players: players
    };
    return {eventType, room};
}


