const path = require('path');
const { VueLoaderPlugin } = require('vue-loader');
const CreateExportScriptPlugin = require('./src/main/javascript/import/CreateExportScriptPlugin');

const config = {
    entry: {
        'jsCore': path.resolve(__dirname, 'src/main/javascript/app/main.js'),
        'importScript': path.resolve(__dirname, 'src/main/javascript/import/exportedAssets.js')
    },
    output: {
        path: path.resolve(__dirname, 'src/main/resources/javascript/bundles/'),
        filename: "[name].js",
    },

    externals: [],

    module: {
        rules: [
            {
                test: /\.js$/,
                exclude: /node_modules/,
                use: {
                    loader: 'babel-loader',
                    options: {
                        plugins: [
                            "@babel/plugin-syntax-dynamic-import"
                        ]
                    }
                }
            },
            {
                test: /\.vue$/,
                loader: 'vue-loader'
            },
            { parser: { system: false } }
        ]
    },
    resolve: {
        mainFields: ["module", "main", "browser"],
        alias: {
            'vue$': 'vue/dist/vue.esm.js'
        },
        extensions: ['*', '.js', '.vue', '.json']
    },
    plugins: [
        new CreateExportScriptPlugin(),
        new VueLoaderPlugin()
    ],

    // optimization: {
    //  splitChunks: {
    //    chunks: 'all'
    //  }
    // },

    mode: "development",
    devtool: 'cheap-source-map'
};

module.exports = [config];