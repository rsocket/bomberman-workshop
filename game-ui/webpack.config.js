const path = require('path');

module.exports = [
{
	mode: 'development',
	entry: ["@babel/polyfill", './src/main/js/App.js'],
	output: {
		path: path.resolve(__dirname, './build/resources/main/static/js'),
		filename: 'main.js'
	},
	module: {
		rules: [
			{
				test: /\.css$/,
				use: [
					'style-loader',
					'css-loader'
				]
			},
			{
				test: /\.(png|svg|jpg|gif)$/,
	      		use: [
	         			'file-loader'
                ]
     		}
        ]
    }
},
{
	mode: 'development',
	entry: ["@babel/polyfill", './src/main/js/Rooms.jsx'],
	output: {
		path: path.resolve(__dirname, './build/resources/main/static/js'),
		filename: 'rooms.js'
	},
	module: {
		rules: [
			{
				test: /\.m?(js|jsx)$/,
				exclude: /node_modules/,
				use: {
					loader: 'babel-loader',
					options: {
						presets: [
							['@babel/preset-env', { targets: "defaults" }]
						]
					}
				}
			},
			{
				test: /\.css$/,
				use: [
					'style-loader',
					'css-loader'
				]
			},
			{
				test: /\.(png|svg|jpg|gif)$/,
	      		use: [
	         			'file-loader'
                ]
     		}
        ]
    }
},
];
