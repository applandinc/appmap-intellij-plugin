/* eslint-disable */
const path = require('path');

module.exports = {
    entry: './installGuide.js',
    output: {
        path: path.resolve(__dirname, 'dist'),
        filename: 'installGuide.js',
        devtoolModuleFilenameTemplate: '[resource-path]',
        library: 'AppLandWeb',
    },
    devtool: 'source-map',
    resolve: {
        extensions: ['.js', '.mjs'],
        alias: {
            vue: path.resolve('./node_modules/vue'),
            vuex: path.resolve('./node_modules/vuex'),
        },
        fallback: {
            crypto: 'crypto-js',
        },
    },
    module: {
        rules: [
            {
                test: /\.m?js$/,
                exclude: /node_modules/,
                use: {
                    loader: 'babel-loader',
                    options: {},
                },
            },
            {
                test: /\.css$/i,
                use: ['style-loader', 'css-loader'],
            },
            {
                test: /\.html$/i,
                loader: 'html-loader',
            },
            {
                test: /\.(svg|ttf)$/i,
                use: [
                    {
                        loader: 'file-loader',
                    },
                ],
            },
        ],
    },
};
