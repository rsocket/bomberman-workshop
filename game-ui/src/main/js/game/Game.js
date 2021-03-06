"use strict";

import Bomb from "./Bomb.js";
import Player from './Player.js';
import Wall from './Wall.js';
import Item from './Item.js';
import {
    BACKGROUNDMUSIC,
    BOMBMUSIC,
    DIEDMUSIC,
    ITEM_EXTRA_BOMB,
    ITEM_EXTRA_LIFE,
    ITEM_RUN_FASTER,
    LOSERMUSIC,
    SETBOMBMUSIC,
    SPOILMUSIC,
    WINNERMUSIC,
} from "./constant.js";
import {Flowable} from "rsocket-flowable";
import {flatbuffers} from "flatbuffers";

import {xyz as gameXyz} from "generated/flatbuffers/Game_generated";
import {xyz} from "generated/flatbuffers/GameEvent_generated";
import {encodeCompositeMetadata, encodeRoute, MESSAGE_RSOCKET_ROUTING} from "rsocket-core";

const EventType = xyz.bomberman.game.data.EventType;

const CreateItemEvent = xyz.bomberman.game.data.CreateItemEvent;
const PlaceBombEvent = xyz.bomberman.game.data.PlaceBombEvent;

const ReactionEvent = xyz.bomberman.game.data.ReactionEvent;
const HurtPlayerEvent = xyz.bomberman.game.data.HurtPlayerEvent;
const ChangeDirectionEvent = xyz.bomberman.game.data.ChangeDirectionEvent;
const MovePlayerEvent = xyz.bomberman.game.data.MovePlayerEvent;
const GrabItemEvent = xyz.bomberman.game.data.GrabItemEvent;
const PlaceWallEvent = xyz.bomberman.game.data.PlaceWallEvent;
const UpdateInventoryEvent = xyz.bomberman.game.data.UpdateInventoryEvent;
const DeletePlayerEvent = xyz.bomberman.game.data.DeletePlayerEvent;
const GameEvent = xyz.bomberman.game.data.GameEvent;
const Position = xyz.bomberman.game.data.Position;

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

        this.on(
            "game.start",
            (game) => {
                const wallsLength = game.wallsLength();
                for (let i = 0; i < wallsLength; i++) {
                    const wall = game.walls(i);
                    const position = wall.position();
                    this.walls.push(new Wall({x: position.x(), y: position.y()}, 1, wall.isDestructible(), assets, 40, wall.id()));
                }

                const playersLength = game.playersLength();
                for (let i = 0; i < playersLength; i++) {
                    const player = game.players(i);
                    const position = player.position();
                    this.pushPlayer({
                      id: player.id(),
                      x: position.x(),
                      y: position.y(),
                      direction: player.direction(),
                      amountWalls: player.amountWalls(),
                      amountBombs: player.amountBombs(),
                      health: player.health()
                    });
                }
            });

        this.on(EventType.Reaction, (data) => {
                const reactionEvent = data.event(new ReactionEvent())
                this.drawReaction({id: reactionEvent.id(), reaction: reactionEvent.reaction()});
            },
        );

        this.on(EventType.HurtPlayer, (data) => {
            const hurtPlayerEvent = data.event(new HurtPlayerEvent())
            this.hurtPlayer({
                id: hurtPlayerEvent.id()
            });
        });

        // receive direction changes
        this.on(EventType.ChangeDirection, (data) => {
            const changeDirectionEvent = data.event(new ChangeDirectionEvent())
            this.changeDirection({
                id: changeDirectionEvent.id(),
                direction: changeDirectionEvent.direction(),
            })
        });

        // receive enemy player movements
        this.on(EventType.MovePlayer, (data) => {
            const movePlayerEvent = data.event(new MovePlayerEvent())
            this.moveEnemy({
                id: movePlayerEvent.id(),
                x: movePlayerEvent.x(),
                y: movePlayerEvent.y(),
                direction: movePlayerEvent.direction(),
            });
        });

        // player grabbed item
        this.on(EventType.GrabItem, (data) => {
            // {id: STRING, x: NUMBER, y: NUMBER, direction: STRING, amountWalls: NUMBER, amountBombs: NUMBER, health: NUMBER}
            const grabItemEvent = data.event(new GrabItemEvent())
            const item = grabItemEvent.item();
            const position = item.position();
            this.pickUpItem({
                item: {
                    position: {
                        x: position.x(),
                        y: position.y(),
                    },
                    type: item.type(),
                },
                playerId: grabItemEvent.playerId(),
            });
        });

        // receive bombs set by enemies
        // {x: nextPosition.x, y: nextPosition.y, id: randomID, amountWalls: this.amountWalls, amountBombs: this.amountBombs}
        this.on(EventType.PlaceBomb, (data) => {
            const placeBombEvent = data.event(new PlaceBombEvent())
            this.receiveBomb({
                x: placeBombEvent.x(),
                y: placeBombEvent.y(),
            });
        });

        // receive items created by exploding walls
        // {x: nextPosition.x, y: nextPosition.y, id: randomID, amountWalls: this.amountWalls, amountBombs: this.amountBombs}
        this.on(EventType.CreateItem, (data) => {
            const createItemEvent = data.event(new CreateItemEvent())
            // {position: {x: NUMBER, y: NUMBER}, type: STRING}
            const position = createItemEvent.position();
            this.receiveItem({
                position: {
                    x: position.x(),
                    y: position.y(),
                },
                type: createItemEvent.type(),
            });
        });

        // receive walls set by enemies
        this.on(EventType.PlaceWall, (data) => {
            const placeWallEvent = data.event(new PlaceWallEvent())
            // {wallId: STRING, x: NUMBER, y: NUMBER, id: STRING}
            this.receiveWall({
                id: placeWallEvent.id(),
                wallId: placeWallEvent.wallId(),
                x: placeWallEvent.x(),
                y: placeWallEvent.y(),
            });
        });

        // update enemy inventory
        // {id: STRING, amountWalls: NUMBER, amountBombs: NUMBER, health: NUMBER}
        this.on(EventType.UpdateInventory, (event) => {
            const updateInventoryEvent = event.event(new UpdateInventoryEvent())
            const data = {
                id: updateInventoryEvent.id(),
                amountBombs: updateInventoryEvent.amountBombs(),
                amountWalls: updateInventoryEvent.amountWalls(),
                health: updateInventoryEvent.health(),
            }
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
        this.on(EventType.DeletePlayer, (data) => {
            const deletePlayerEvent = data.event(new DeletePlayerEvent())
            this.deletePlayer({id: deletePlayerEvent.id()});
        });


        // START GAME
        this.startAnimating();
    }

    start(flowable) {
        this.toggleUI();

        const callbacks = this.callbacks;
        let firstEvent = true;

        const self = this;
        flowable.subscribe({
            onSubscribe(s) {
                s.request(2147483642)
            },
            onNext(t) {
                if (firstEvent) {
                    firstEvent = false;
                    const game = gameXyz.bomberman.game.data.Game.getRootAsGame(new flatbuffers.ByteBuffer(t.data));
                    callbacks['game.start'](game)
                    return;
                }
                const event = GameEvent.getRootAsGameEvent(new flatbuffers.ByteBuffer(t.data));
                callbacks[event.eventType()](event);
            },
            onError(err) {
                console.error(err);
                self.deletePlayer({id: self.id});
            },
            onComplete() {
                console.log("complete?")
            }
        })

        return new Flowable(subscriber => {
            console.log("subscribing")
            subscriber.onSubscribe({
                request(n) {
                    console.log("requested " + n)
                },
                cancel() {
                    console.log("cancelled")
                }
            });
            this.rsocketEmit = (type, builderFunction) => {
                const builder = new flatbuffers.Builder()
                const gameEventOffset = GameEvent.createGameEvent(
                    builder,
                    type,
                    builderFunction(builder)
                );
                GameEvent.finishGameEventBuffer(builder, gameEventOffset);
                const bytes = builder.asUint8Array();
                const data = Buffer.from(bytes);
                subscriber.onNext({
                    metadata: encodeCompositeMetadata([
                        [MESSAGE_RSOCKET_ROUTING, encodeRoute(`game.play`)],
                    ]),
                    data
                })
            };
        })
    }

    toggleUI() {
        window.game = this;
        document.querySelector("#lname").setAttribute("value", this.id);
        document.getElementById("gamefield").className = ""
        document.getElementById("root").className = "hidden"
    }

    on(type, callback) {
        this.callbacks[type] = callback;
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
        console.log(reaction)
        // {id:this.id, reaction: reaction}
        this.emit(EventType.Reaction, builder => {
            return ReactionEvent.createReactionEvent(
                builder,
                builder.createString(this.id),
                builder.createString(reaction),
            );
        });
    }

    broadcastPosition(position) {
        this.emit(EventType.MovePlayer,
            (builder) =>
                MovePlayerEvent.createMovePlayerEvent(
                    builder,
                    builder.createString(position.id),
                    position.x,
                    position.y,
                    builder.createString(position.direction)
                )
        );
    }

    broadcastDirection(direction) {
        this.emit(EventType.ChangeDirection,
            (builder) => {
                return ChangeDirectionEvent.createChangeDirectionEvent(
                    builder,
                    builder.createString(direction.id),
                    builder.createString(direction.direction),
                );
            }
        )
    }

    broadcastWall(wall) {
        // {id: this.id, x: nextPosition.x, y: nextPosition.y, wallId: randomID, amountWalls: this.amountWalls, amountBombs: this.amountBombs}
        this.emit(EventType.PlaceWall,
            (builder) => {
                return PlaceWallEvent.createPlaceWallEvent(
                    builder,
                    builder.createString(wall.id),
                    builder.createString(wall.wallId),
                    wall.x,
                    wall.y,
                    wall.amountWalls,
                );
            }
        )
    }

    broadcastBomb(bomb) {
        this.emit(EventType.PlaceBomb,
            (builder) => {
                return PlaceBombEvent.createPlaceBombEvent(
                    builder,
                    builder.createString(bomb.id),
                    bomb.x,
                    bomb.y,
                    bomb.amountBombs,
                );
            }
        )
    }

    broadcastItem(item) {
        // {position:position, type: item_type}
        //this.emit(CREATE_ITEM, item);
        this.emit(EventType.CreateItem,
            (builder) => {
                let typeOffset = builder.createString(item.type);
                return CreateItemEvent.createCreateItemEvent(
                    builder,
                    Position.createPosition(builder, item.position.x, item.position.y),
                    typeOffset,
                );
            }
        )

    }

    broadcastDeletedPlayer(player) {
        this.emit(EventType.DeletePlayer,
            (builder) =>
                DeletePlayerEvent.createDeletePlayerEvent(
                    builder,
                    builder.createString(player.id),
                )
        )
    }

    broadcastInventory(state) {
        // {id: player.id, amountWalls: player.amountWalls, amountBombs: player.amountBombs, health: player.health};
        this.emit(EventType.UpdateInventory,
            (builder) =>
                UpdateInventoryEvent.createUpdateInventoryEvent(
                    builder,
                    builder.createString(state.id),
                    state.amountWalls,
                    state.amountBombs,
                    state.health,
                )
        )
    }


    emit(type, builderFunction) {
        this.rsocketEmit(type, builderFunction)
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

        if (data.id === this.id || !this.id) {
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
        let localPlayer = data.playerId === this.id;

        this.players.forEach((player) => {
            if (player.id === data.playerId) {

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
                for (const item of this.items) {
                    if (item.position.x === player.position.x
                        && item.position.y === player.position.y) {
                        this.emit(EventType.GrabItem,
                            (builder) => {
                                const itemTypeOffset = builder.createString(item.type);
                                const createItemOffset = xyz.bomberman.game.data.Item.createItem(
                                    builder,
                                    Position.createPosition(builder, item.position.x, item.position.y),
                                    itemTypeOffset
                                )
                                return GrabItemEvent.createGrabItemEvent(
                                    builder,
                                    createItemOffset,
                                    builder.createString(player.id),
                                );
                            }
                        )
                        this.pickUpItem({item: item, playerId: player.id,})
                    }
                }
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
     * @param data = {id: STRING, direction: STRING}
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
                message.innerHTML = `<p>${name}:</p><h1>&#x1F496;???</h1>`;
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
