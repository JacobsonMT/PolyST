var path = require('path');

module.exports = {
    entry: {
        table: './src/main/js/table.js',
        protein: './src/main/js/protein.js',
    },
    devtool: 'sourcemaps',
    cache: true,
    debug: true,
    output: {
        path: __dirname,
        filename: './src/main/resources/static/built/[name].js'
    },
    module: {
        loaders: [
            {
                test: path.join(__dirname, '.'),
                exclude: /(node_modules)/,
                loader: 'babel',
                query: {
                    cacheDirectory: true,
                    presets: ['es2015', 'react']
                }
            },
            { test: /\.css$/, loader: 'style-loader!css-loader' }
        ]
    }
};