import Element from './Element.js';
import {
    BOMB,
    FIRE,
    BOMBMUSIC,
    DIEDMUSIC,
} from "./constant.js";

export default class Bomb extends Element {

    constructor(position, timeToExplode = 5000, radius, assets, gridSize, game, remote=false) {
        super(position, assets);

        this.game = game;
        this.remoteBomb = remote;

        // unique ID function from stackoverflow
        this.ID = '_' + Math.random().toString(36).substr(2, 9);

        this.gridSize = gridSize;
        this.isExploded = false;

        this.timeToExplode = timeToExplode;
        this.radius = radius;


        this.spriteSize = {
                x: 40,
                y: 40,
        };

        this.currentAnimationState = 0;

        this.animationSpeed = 20;

        this.animationSheet = [
            {x: 0, y: 0},
            {x: 40, y: 0},
            {x: 2 * 40, y: 0},
            {x: 3 * 40, y: 0},
            {x: 4 * 40, y: 0},
        ];

        this.surroundingPositions = this.getSurroundingPositions();

        // bomb destroys itself after being created
        setTimeout(() => {
            this.animateExplosion();
        }, this.timeToExplode)
    }

    animateExplosion() {
        this.game.playMusic(BOMBMUSIC);
        this.isExploded = true;
        setTimeout(() => {
            this.destroySurrounding();
        }, 500);
    }

    /**
     * calculate all positions within this.radius
     *
     * @returns {Array} with position objects
     */
    getSurroundingPositions() {

        let positions = [];

        let positionsHorizontal = [
            {x: this.position.x+1, y: this.position.y},
            {x: this.position.x-1, y: this.position.y},
        ];

        let positionsVertical = [
            {x: this.position.x, y: this.position.y+1},
            {x: this.position.x, y: this.position.y-1},
        ];

        let horizontalCollision = false;
        let verticalCollision = false;

        for (let i = 0; i < this.game.walls.length; i++) {
            for (let j = 0; j < positionsHorizontal.length; j++) {
                if (this.game.walls[i].position.x === positionsHorizontal[j].x && this.game.walls[i].position.y === positionsHorizontal[j].y && this.game.walls[i].isDestructible === false) {
                    horizontalCollision = true;
                }
            }
            for (let j = 0; j < positionsVertical.length; j++) {
                if (this.game.walls[i].position.x === positionsVertical[j].x && this.game.walls[i].position.y === positionsVertical[j].y && this.game.walls[i].isDestructible === false) {
                    verticalCollision = true;
                }
            }
        }

        if (horizontalCollision) {
            for (let i = (-this.radius); i < this.radius+1; i++) {
                positions.push({x: this.position.x, y: (this.position.y + i)})
            }
        } else if (verticalCollision) {
            for (let i = (-this.radius); i < this.radius+1; i++) {
                positions.push({x: (this.position.x + i), y: this.position.y})
            }
        } else {
            for (let i = (-this.radius); i < this.radius+1; i++) {
                positions.push({x: this.position.x, y: (this.position.y + i)})
            }
            for (let i = (-this.radius); i < this.radius+1; i++) {
                if (i === 0) { continue; }
                positions.push({x: (this.position.x + i), y: this.position.y})
            }
        }
        return positions;
    }

    /**
     * destroySurrounding() iterates through this.games.bombs and deletes this particular bomb with the given ID
     * after detonation it scans all surrounding players and destructible walls, which are affected
     *
     * let position determines the surrounding positions
     */
    destroySurrounding() {

        this.game.bombs = this.game.bombs.filter(bomb => bomb.ID !== this.ID);

        // delete affected players
        this.game.players.forEach((player) => {

            // compare with positions of fire animation
            this.surroundingPositions.forEach(position => {

                // if explosion position matches player position
                if (player.position.x === position.x && player.position.y === position.y) {
                    this.game.playMusic(DIEDMUSIC);

                    player.decrementHealth();

                    if(player.health < 1) {

                        // broadcast deleted player
                        this.game.broadcastDeletedPlayer({id: player.id});
                        this.game.deletePlayer({id: player.id});
                    }
                }
            })
        });



        // delete affected walls
        let indexes = [];
        this.game.walls.forEach((wall, index) => {
            this.getSurroundingPositions().forEach(position => {
                if (wall.position.x === position.x && wall.position.y === position.y && wall.isDestructible === true) {
                    indexes.push(index);
                }
            })
        });

        // IMPORTANT! Because .splice() shortens the array, we safe all indexes, which have to be deleted inside of 'let indexes'
        indexes.sort((a, b) => {return b-a}).forEach((index) => {
            let position = this.game.walls[index].position;

            this.game.walls.splice(index, 1);
            this.game.createItems(position, this.remoteBomb);
        });
    }

    // display bomb or fire image
    draw(context) {
        if (!this.isExploded) {
            if (this.game.frameCount % this.animationSpeed === 0) {
                this.currentAnimationState = (this.currentAnimationState + 1) % this.animationSheet.length;
            }
            context.drawImage(
                this.assets[BOMB],
                this.animationSheet[this.currentAnimationState].x,
                0,
                this.spriteSize.x,
                this.spriteSize.y,
                this.position.x * this.gridSize,
                this.position.y * this.gridSize,
                this.gridSize,
                this.gridSize,
            )
        } else {
            // draw surrounding fire
            this.getSurroundingPositions().forEach(position => {
                context.drawImage(
                    this.assets[FIRE],
                    0,
                    0,
                    this.spriteSize.x,
                    this.spriteSize.y,
                    position.x * this.gridSize,
                    position.y * this.gridSize,
                    this.gridSize,
                    this.gridSize,
                );
                context.globalCompositeOperation='destination-over';

            });

        }
    }


}

