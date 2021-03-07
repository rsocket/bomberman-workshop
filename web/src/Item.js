import Element from './Element.js';


export default class Item extends Element {


    constructor(position, type, assets, gridSize, game) {
        super(position, assets);

        this.game = game;
        this.type = type;  

        // unique ID function from stackoverflow
        this.ID = '_' + Math.random().toString(36).substr(2, 9);

        this.gridSize = gridSize;

        this.spriteSize = {
            spoil: {
                x: 40,
                y: 40,
            }
        };


        this.currentAnimationState = 0;

        this.animationSheet = [
            {x: 0, y: 0},
            {x: 40, y: 0},
            {x: 2 * 40, y: 0},
            {x: 3 * 40, y: 0},
            {x: 4 * 40, y: 0},
        ];

    }

    // display bomb or fire image
    draw(context) {
        context.drawImage(
            this.assets[this.type],
            this.animationSheet[this.currentAnimationState].x,
            0,
            this.spriteSize.spoil.x,
            this.spriteSize.spoil.y,
            this.position.x * this.gridSize,
            this.position.y * this.gridSize,
            this.gridSize,
            this.gridSize,
        )


    }


}

