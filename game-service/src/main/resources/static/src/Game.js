"use strict";

import Bomb from "./Bomb.js";
import Player from './Player.js';
import Item from './Item.js';
import Wall from './Wall.js';
import {
    CHANGE_DIRECTION,
    MOVE_PLAYER,
    PLACE_BOMB,
    PLACE_WALL,
    DELETE_WALL,
    CREATE_PLAYER,
    LOGIN_PLAYER,
    CREATE_WALLS,
    DELETE_PLAYER,
    CREATE_ITEM,
    GRAB_ITEM,
    ITEM_EXTRA_LIFE,
    ITEM_EXTRA_BOMB,
    ITEM_RUN_FASTER,
    HURT_PLAYER,
    UPDATE_INVENTORY,
    REACTION,
    BACKGROUNDMUSIC,
    BOMBMUSIC,
    DIEDMUSIC,
    LOSERMUSIC,
    SETBOMBMUSIC,
    SPOILMUSIC,
    WINNERMUSIC,
} from "./constant.js";
import {IdentitySerializer, JsonSerializer, JsonSerializers, RSocketClient} from "rsocket-core";
import RSocketWebSocketClient from "rsocket-websocket-client";
import {ConnectionStatus, ISubscription, Payload, ReactiveSocket} from "rsocket-types";
import {Flowable} from "rsocket-flowable";


export default class Game {

    constructor(canvas, width=13, height=13, assets, id) {

        this.canvas = document.getElementById(canvas);
        this.context = this.canvas.getContext('2d');
        this.assets = assets;
        this.frameCount = 0;

        // ID of your client
        this.id = id;
        this.gameOver = false;


        this.width = width;
        this.height = height;
        this.gridSize = this.canvas.width / width;


        this.bombs = [];
        this.players = [];
        this.walls = [];
        this.items = [];

        // background music plays when game starts
        this.playMusic(BACKGROUNDMUSIC);


        // disable input
        document.getElementById("login").disabled = true;

        // disable login button
        document.getElementById("lname").disabled = true;

        // set focus on canvas
        document.getElementById("myCanvas").focus();

        // keyboard events
        document.addEventListener("keyup", (e) => {
            if (!this.gameOver) {
                this.movePlayer({id: id, key: e.key})
            }
        });









//###################################################//
//                                                   //
//         S O C K E T    A C T I O N S              //
//                                                   //
//###################################################//

        this.callbacks = {};

        this.on(REACTION, (data) => {
            this.drawReaction(data);
        });

        // after logging in your player, the server will send you all generated walls
        this.on(CREATE_WALLS, (wallEvent) => {
            const walls = wallEvent.walls;
            walls.forEach(wall => {
                let position = {x: wall.x, y: wall.y};
                this.walls.push(new Wall(position, 1, wall.isDestructible, assets, 40, wall.wallId));
            });
        });

        this.on(CREATE_PLAYER, (data) => {
            this.pushPlayer(data);
        });

        this.on(HURT_PLAYER, (data) => {
            this.hurtPlayer(data);
        });

        // receive direction changes
        this.on(CHANGE_DIRECTION, (data) => {
            this.changeDirection(data)
        });

        // receive enemy player movements
        this.on(MOVE_PLAYER, (data) => {
            this.moveEnemy(data);
        });

        // player grabbed item
        this.on(GRAB_ITEM, (data) => {
            this.pickUpItem(data);
        });

        // receive bombs set by enemies
        // {x: nextPosition.x, y: nextPosition.y, id: randomID, amountWalls: this.amountWalls, amountBombs: this.amountBombs}
        this.on(PLACE_BOMB, (data) => {
            this.receiveBomb(data);
        });

        // receive items created by exploding walls
        // {x: nextPosition.x, y: nextPosition.y, id: randomID, amountWalls: this.amountWalls, amountBombs: this.amountBombs}
        this.on(CREATE_ITEM, (data) => {
            this.receiveItem(data);
        });

        // receive walls set by enemies
        this.on(PLACE_WALL, (data) => {
            this.receiveWall(data);
        });

        // update enemy inventory
        // {id: STRING, amountWalls: NUMBER, amountBombs: NUMBER, health: NUMBER}
        this.on(UPDATE_INVENTORY, (data) => {
            if (data.id !== this.id) {
                try {
                    document.getElementById(data.id + 'BombText').innerText = data.amountBombs;
                    document.getElementById(data.id + 'WallText').innerText = data.amountWalls;
                    document.getElementById(data.id + 'HealthText').innerText = data.health;
                } catch (e) {
                    console.log(e)
                }
            }
        });

        // update remaining players
        this.on(DELETE_PLAYER, (data) => {
            this.deletePlayer(data);
        });




        this.initRsocket();
        // START GAME
        this.startAnimating();
    }


    on(type, callback) {
        this.callbacks[type] = callback;
    }

    async initRsocket() {
        console.log("HALLO!")
        const callbacks = this.callbacks;
        const [wsClient, rsocket] = await this.connect();
        rsocket.requestChannel(new Flowable(subscriber => {
            console.log("subscribing")
            subscriber.onSubscribe({
                request(n) {
                    console.log("requested " + n)
                },
                cancel() {
                    console.log("cancelled")
                }
            });
            this.rsocketEmit = (type, data) => {
                subscriber.onNext({
                    metadata: String.fromCharCode('events'.length) + 'events',
                    data: Object.assign(data, {eventType: type})
                })
            };
            this.emit(LOGIN_PLAYER, { id: this.id });
        }))
            .subscribe({
                onSubscribe(s) {
                    s.request(2147483642)
                },
                onNext(t) {
                    const type = t.data.eventType;
                    console.log("got: " + type)
                    callbacks[type](t.data);
                },
                onError(err) {
                    console.error(err);
                },
                onComplete() {
                    console.log("complete?")
                }
            })
        wsClient.connectionStatus().subscribe({
            onSubscribe(s) {
                s.request(2147483642);
            },
            onNext(t) {
                console.log("status: " + t.kind)
            }
        })


    }

    async connect() {
        console.log("connecting")
        let wsClient = new RSocketWebSocketClient({url: 'ws://localhost:9000/rsocket'});
        const socketClient = new RSocketClient({
            serializers: {
                data: JsonSerializer,
                metadata: IdentitySerializer
            },
            setup: {
                keepAlive: 30000,
                lifetime: 90000,
                dataMimeType: 'application/json',
                metadataMimeType: 'message/x.rsocket.routing.v0',
            },
            transport: wsClient,
        });
        const rsocket = await socketClient.connect();
        return [wsClient, rsocket];
    }






//###################################################//
//                                                   //
//       S O C K E T    B R O A D C A S T            //
//                                                   //
//###################################################//


    /**
     * all broadcast functions are being evoked by Player.js
     */
    broadcastReaction(reaction) {
        this.emit(REACTION, {id:this.id, reaction: reaction});
    }

    broadcastPosition(position) {
        this.emit(MOVE_PLAYER, position);
    }

    broadcastDirection(direction) {
        this.emit(CHANGE_DIRECTION, direction);
    }

    broadcastWall(wall) {
        this.emit(PLACE_WALL, wall);
    }

    broadcastBomb(bomb) {
        this.emit(PLACE_BOMB, bomb);
    }

    broadcastItem(item) {
        this.emit(CREATE_ITEM, item);
    }

    broadcastDestroyedWall(wall) {
        this.emit(DELETE_WALL, {wallId: wall.wallId});
    }

    broadcastDeletedPlayer(player) {
        this.emit(DELETE_PLAYER, player);
    }

    broadcastInventory(state) {
        this.emit(UPDATE_INVENTORY ,state);
    }


    emit(type, data) {
        this.rsocketEmit(type, data)
    }








//###################################################//
//                                                   //
//         G A M E     M U T A T I O N S             //
//                                                   //
//###################################################//


    /**
     * create new player
     * is being called in App.js whenever socket receives a signal
     * @param data = {id: STRING, x: NUMBER, y: NUMBER, direction: STRING, amountWalls: NUMBER, amountBombs: NUMBER, health: NUMBER}
     */
    pushPlayer(data) {
        // create position of this player
        let position = {x: data.x, y: data.y};

        // we expect, that there is no player with this particular ID (data.id)
        let doesContain = false;

        // checks if there's already a player with this ID
        this.players.forEach(player => {
            if (player.id === data.id) {
                doesContain = true;
            }
        });

        // If there is no player with this particular ID, create new Player
        if (!doesContain) {
            this.players.push(new Player(position, this.assets, data.health, data.amountBombs, data.amountWalls, this.gridSize, this, data.id, data.direction));

            // attach html node to enemy stats
            this.createEnemyInventory(data);
        }
    }

    /**
     * remove player from array and hide his inventory
     * @param data
     */
    deletePlayer(data) {
        this.players = this.players.filter(player => player.id !== data.id);

        // hide enemy inventory
        if (data.id !== this.id) {
            try {
                document.getElementById(data.id).style.display = "none";
            } catch (e) {
                console.log(e);
            }
        }

        if (data.id === this.id) {
            this.playMusic(LOSERMUSIC);
            try {
                document.getElementById("inventory").style.display = "none";
                document.getElementById("youwinscreen").style.display = "none";
                document.getElementById("gameOverScreen").style.display = "flex";
            } catch (e) {
                console.log(e);
            }
        }

        if (this.checkForWinner()) {
            this.playMusic(WINNERMUSIC);
            try {
                document.getElementById("gameOverScreen").style.display = "none";
                document.getElementById("youwinscreen").style.display = "flex";
            } catch (e) {
                console.log(e);
            }
        }

    }

    createItems(position, remote) {
        if (Math.random() <= 0.3 && !remote) {
            let item_type = ITEM_EXTRA_LIFE;
            let itemDetermination = Math.random();

            if (itemDetermination > 0.66) {
                item_type = ITEM_EXTRA_BOMB;
            } else if (itemDetermination > 0.33) {
                item_type = ITEM_RUN_FASTER;
            }

            this.broadcastItem({position:position, type: item_type});
            this.items.push(new Item({x: position.x, y: position.y}, item_type, this.assets, this.gridSize, this));
        }
    }

    /**
     * the player grab the item
     * is being called in App.js whenever socket receives a signal
     * check the type of the item and update then broadcast it
     * @param data = {id: STRING, x: NUMBER, y: NUMBER, direction: STRING, amountWalls: NUMBER, amountBombs: NUMBER, health: NUMBER}
     */
    pickUpItem(data) {
        let itemObject = data.item;
        let localPlayer = data.player.id === this.id;

        this.players.forEach((player) => {
            if (player.id === data.player.id) {

                // change the music for the player
                this.playMusic(SPOILMUSIC);
                let state = null;

                switch(itemObject.type) {
                    case ITEM_EXTRA_BOMB:
                        player.updateBombCount(1);
                        state = {id: player.id, amountWalls: player.amountWalls, amountBombs: player.amountBombs, health: player.health};
                        this.broadcastInventory(state);
                        break;
                    case ITEM_EXTRA_LIFE:
                        player.updateHealth(1);
                        state = {id: player.id, amountWalls: player.amountWalls, amountBombs: player.amountBombs, health: player.health};
                        this.broadcastInventory(state);
                        break;
                    case ITEM_RUN_FASTER:
                        // make player faster
                        if (!player.isARunner && localPlayer) {
                            // make the player into a runner by adding key event
                            let eventFunction =  (e) => {
                                if (!this.gameOver) {
                                    this.movePlayer({id: this.id, key: e.key}, true)
                                }
                            };
                            document.addEventListener("keydown", eventFunction);
                            player.isARunner = true;

                            // set the runner duration as 30 seconds
                            setTimeout(() => {
                                document.removeEventListener("keydown", eventFunction);
                                player.isARunner = false;
                            }, 30000);
                        }
                        break;
                    default:
                        break;
                }
            }
        });

        let removalIndex = -1;

        // update in the item arsenal
        for (let i = 0; i < this.items.length; i++) {
            let pos = this.items[i].position;
            if (pos.x === itemObject.position.x && pos.y === itemObject.position.y) {
                removalIndex = i;
                break;
            }
        }

        if (removalIndex >= 0) {
            this.items.splice(removalIndex, 1);
        }
    }


    /**
     * move own player
     * is being called in App.js whenever user presses a key
     * @param data = {id: STRING, x: NUMBER, y: NUMBER, direction: STRING}
     * @param fastMode type boolean
     */
    movePlayer(data, fastMode) {
        this.players.forEach(player => {
            if (player.id === data.id) {
                player.triggerEvent(data, fastMode);
            }
        });
    }


    /**
     * receive movement from enemy players
     * is being called in App.js whenever socket receives a signal
     * @param data = {id: STRING, x: NUMBER, y: NUMBER, direction: STRING}
     */
    moveEnemy(data) {
        this.players.forEach(player => {
            if (player.id === data.id) {
                player.position.x = data.x;
                player.position.y = data.y;
                player.direction = data.direction;
            }
        });
    }

    hurtPlayer(data) {
        if (data.id !== this.id) {
            this.players.forEach(player => {
                if (player.id === data.id) {
                    player.updateHealth(-1);
                }
            });
        }
    }

    checkForWinner() {
        let winner = (this.players.length === 1) && (this.players[0].id === this.id);

        if (winner) {
            this.gameOver = true;
        }
        return winner;
    }


    /**
     * receive direction change from enemy players
     * is being called in App.js whenever socket receives a signal
     * @param data = {id: STRING, x: NUMBER, y: NUMBER, direction: STRING}
     */
    changeDirection(data) {
        this.players.forEach(player => {
            if (player.id === data.id) {
                player.direction = data.direction;
            }
        })
    }


    /**
     * receive bombs from enemy players
     * is being called in App.js whenever socket receives a signal
     * @param position = {x: NUMBER, y: NUMBER}
     */
    receiveBomb(position) {
        this.bombs.push(new Bomb(position, 1500, 2, this.assets, this.gridSize, this, true));
    }

    /**
     * receive item
     * @param data = {position: {x: NUMBER, y: NUMBER}, type: STRING}
     */
    receiveItem(data) {
        this.items.push(new Item(data.position, data.type, this.assets, this.gridSize, this));
    }

    /**
     * receive walls from enemy players
     * is being called in App.js whenever socket receives a signal
     * @param data = {wallId: STRING, x: NUMBER, y: NUMBER, id: STRING}
     */
    receiveWall(data) {
        let tempPosition = {x: data.x, y: data.y};
        let doesntContains = true;

        this.walls.forEach(wall => {
            if (wall.wallId === data.wallId) {
                doesntContains = false;
            }
        });

        if (doesntContains) {
            this.walls.push(new Wall(tempPosition, 1, true, this.assets, this.gridSize, data.wallId));
        }
    }


    /**
     * render method to display all elements on the game board
     * canvas has to be cleared each render loop
     */
    draw() {
        this.context.clearRect(0,0, this.canvas.width, this.canvas.height);

        this.players.forEach(player => {
            player.draw(this.context);
        });

        this.bombs.forEach(bomb => {
            bomb.draw(this.context);
        });

        this.walls.forEach(wall => {
            wall.draw(this.context);
        });

        this.items.forEach(item => {
            item.draw(this.context);
        });
    }

    /**
     * update emoji chat
     * @param data = {id: STRING, reaction: STRING}
     */
    drawReaction(data) {
        let chat = document.getElementById('echat');
        let message = document.createElement("div");
        let name = data.id === this.id ?  'You' : data.id;

        switch (data.reaction) {
            case "you_suck":
                message.innerHTML = `<p>${name}:</p><h1>&#x1F621;</h1>`;
                break;
            case "love":
                message.innerHTML = `<p>${name}:</p><h1>&#x1F496;️</h1>`;
                break;
            case "lol":
                message.innerHTML = `<p>${name}:</p><h1>&#x1F602;</h1>`;
                break;
            default:
                break;
        }

        message.id = "chat_text";
        chat.appendChild(message);
        chat.scrollTop = chat.scrollHeight;
    }


    startAnimating() {
        this.frameTime = 1000 / 30;
        this.then = window.performance.now();
        this.animate(this.then);
    }


    animate(currentTime) {
        window.requestAnimationFrame(this.animate.bind(this));

        const now = currentTime;
        const elapsed = now - this.then;

        if (elapsed > this.frameTime) {
            this.then = now;
            this.draw();
        }
        this.frameCount++;
    }

    /**
     * creates an HTML node every time a player has been created
     * @param data = {id: STRING, amountBombs: NUMBER, amountWalls: NUMBER}
     */
    createEnemyInventory(data) {
        if (this.players.length > 1) {

            let enemyInventory = document.getElementById('inventoryEnemy');
            let container = document.createElement("div");
            container.className = 'playerInfos';
            container.id = data.id;

            // create enemy ID
            let id = document.createElement("p");
            id.innerText = data.id;
            id.id = "nickname";

            // create heart icon and counter
            let healthImage = document.createElement("img");
            healthImage.id = data.id + 'LifeImg';
            healthImage.src = "images/spoilLife.png";

            let healthBox = document.createElement("div");
            healthBox.className = "countBox";

            let healthText = document.createElement("p");
            healthText.id = data.id + 'HealthText';
            healthText.innerText = data.health;
            healthBox.appendChild(healthText);


            // create bomb icon and counter
            let bombImage = document.createElement("img");
            bombImage.id = data.id + 'BombImg';
            bombImage.src = "images/bomb_icon.png";

            let bombBox = document.createElement("div");
            bombBox.className = "countBox";

            let bombText = document.createElement("p");
            bombText.id = data.id + 'BombText';
            bombText.innerText = data.amountBombs;
            bombBox.appendChild(bombText);


            // create wall icon and counter
            let wallImage = document.createElement("img");
            wallImage.id = data.id + 'WallImg';
            wallImage.src = "images/wall.png";

            let wallBox = document.createElement("div");
            wallBox.className = "countBox";

            let wallText = document.createElement("p");
            wallText.id = data.id + 'WallText';
            wallText.innerText = data.amountWalls;
            wallBox.appendChild(wallText);


            // enemy ID
            container.appendChild(id);

            // enemy health count
            container.appendChild(healthImage);
            container.appendChild(healthBox);

            // enemy bomb count
            container.appendChild(bombImage);
            container.appendChild(bombBox);

            // enemy wall count
            container.appendChild(wallImage);
            container.appendChild(wallBox);

            // attach to HTML node
            enemyInventory.appendChild(container);

        }
    }

    /**
     * control the music play of the game
     * @param music input music type
     */
    playMusic(music) {
        switch(music) {
            case BOMBMUSIC:
                this.bombMusic = new Audio("/sounds/bombMusic.mp3");
                this.bombMusic.play();
                break;
            case SETBOMBMUSIC:
                this.setBombMusic = new Audio("/sounds/setBombMusic.mp3");
                this.setBombMusic.play();
                break;
            case BACKGROUNDMUSIC:
                this.backgroundMusic = new Audio("/sounds/backgroundMusic.mp4");
                this.backgroundMusic.loop = true;
                this.backgroundMusic.play();
                break;
            case DIEDMUSIC:
                this.diedMusic = new Audio("/sounds/diedMusic.mp4");
                this.diedMusic.play();
                break;
            case LOSERMUSIC:
                if(this.backgroundMusic) {
                    this.backgroundMusic.pause();
                } else if(this.spoilMusic) {
                    this.spoilMusic.pause();
                }

                this.loserMusic = new Audio("/sounds/loserMusic.mp4");
                this.loserMusic.play();
                break;
            case SETBOMBMUSIC:
                if(!this.spoilMusic) {
                    this.backgroundMusic.pause();
                    this.spoilMusic = new Audio("/sounds/spoilMusic.mp4");
                    this.spoilMusic.loop = true;
                    this.spoilMusic.play();
                }
                break;
            case SPOILMUSIC:
                if(!this.spoilMusic) {
                    if (this.backgroundMusic) {
                        this.backgroundMusic.pause();
                        this.spoilMusic = new Audio("/sounds/spoilMusic.mp4");
                        this.spoilMusic.loop = true;
                        this.spoilMusic.play();
                    }
                }
                break;
            case WINNERMUSIC:
                if(this.backgroundMusic) {
                    this.backgroundMusic.pause();
                }
                else if(this.spoilMusic) {
                    this.spoilMusic.pause();
                }

                this.winnerMusic = new Audio("/sounds/winnerMusic.mp4");
                this.winnerMusic.play();
                break;
            default:
                console.log("Item should have a type, but here has no type!")
        }
    }

}
