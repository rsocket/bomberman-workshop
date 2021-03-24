const path = require('path');

module.exports = {
	mode: 'development',
	watch: true,
	entry: './src/App.js',
	output: {

		path: path.resolve(__dirname, 'dist'),
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
};