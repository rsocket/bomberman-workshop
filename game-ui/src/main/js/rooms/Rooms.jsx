import "core-js/stable";
import "regenerator-runtime/runtime";
import '@babel/polyfill'
import React, {useEffect, useRef, useState} from 'react';
import Game from "../game/Game";
import APIClient from "../api/APIClient";

const {uniqueNamesGenerator, animals} = require('unique-names-generator');

const userName = uniqueNamesGenerator({
    dictionaries: [animals],
    length: 1
});

export function Rooms() {
    const [rooms, setRooms] = useState([]);
    const [userId, setUserId] = useState(undefined);
    const [ownedRoomId, setOwnedRoomId] = useState(undefined);
    const client = useRef(undefined);

    useEffect(async () => {
        const apiClient = await APIClient.create(flowable => {
            const game = new Game("myCanvas", 13, 13, window.assets, userName);
            return game.start(flowable)
        })

        client.current = apiClient

      apiClient.login(userName)
               .then(setUserId)
               .then(apiClient.rooms.listAndListen(handleRoomEvent))
    }, []);

    function handleRoomEvent({eventType, room}) {
        // update all displayed rooms
        setRooms(rooms => {
            if (eventType === "Added") {
                return [room, ...rooms]
            } else if (eventType === "Updated") {
                return [room, ...rooms.filter(r => r.id !== room.id)]
            } else {
                // remove empty rooms
                return rooms.filter(r => r.id !== room.id);
            }
        });
    }

    async function createRoom() {
        const apiClient = client.current;
        const roomId = await apiClient.rooms.create();

        setOwnedRoomId(roomId);
    }

    async function joinRoom(roomId) {
        const apiClient = client.current;
        await apiClient.rooms.join(roomId)
    }

    async function leaveRoom(roomId) {
        const apiClient = client.current;
        await apiClient.rooms.leave(roomId)
    }

    async function closeRoom(roomId) {
        const apiClient = client.current;
        await apiClient.rooms.close(roomId)

        setOwnedRoomId(undefined)
    }

    function startGame(roomId) {
        const apiClient = client.current;
        apiClient.rooms.start(roomId)
    }

    const inAGame = rooms.filter(room => room.players.map(p => p.id).includes(userId)).length > 0;
    return (
        <div className={"rooms"}>
            {!userId
                ? <div>Loading...</div>
                : <div>
                    <div>Welcome, {userName} ({userId})</div>
                    {inAGame
                        ? <div/>
                        : <button onClick={createRoom}>Create Game</button>
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
                                                ? <button onClick={() => closeRoom(room.id)}>Close</button>
                                                : <button onClick={() => leaveRoom(room.id)}>Leave</button>}
                                            {ownedRoomId === room.id && room.players.length > 1
                                                ? <button onClick={() => startGame(room.id)}>Start</button>
                                                : <div/>}
                                        </div>
                                        : (inAGame || room.players.length >= 4
                                            ? <div/>
                                            : <button onClick={() => joinRoom(room.id)}>Join</button>)
                                    }
                                </div>
                                <hr style={{width: "100%"}}/>
                            </div>
                        )}
                    </div>
                </div>}
        </div>
    );
}


