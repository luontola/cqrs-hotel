// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

const webpack = require('webpack');
const path = require('path');

const isProd = process.env.NODE_ENV === 'production';
console.log("isProd", isProd);

function getEntrySources() {
  const sources = [];
  if (!isProd) {
    sources.push('webpack-dev-server/client?http://localhost:8080');
  }
  for (let i = 0; i < arguments.length; i++) {
    sources.push(arguments[i]);
  }
  return sources;
}

module.exports = (env) => {
  const plugins = [];
  if (isProd) {
    plugins.push(
      new webpack.LoaderOptionsPlugin({
        minimize: true,
        debug: false
      }),
      new webpack.optimize.UglifyJsPlugin({
        output: {
          comments: false,
        },
      })
    )
  }

  return {
    entry: getEntrySources(
      './src/main/js/index.js'
    ),
    output: {
      path: path.resolve(__dirname, 'target/webpack'),
      filename: 'bundle.js',
    },
    module: {
      rules: [
        {
          test: /\.js$/,
          exclude: /node_modules/,
          use: ['babel-loader'],
        },
        {
          test: /\.css$/,
          use: ['style-loader', 'css-loader'],
        },
      ]
    },
    plugins,
    performance: isProd && {
      maxAssetSize: 100,
      maxEntrypointSize: 300,
      hints: 'warning',
    },
    devtool: 'source-map',
    devServer: {
      historyApiFallback: true,
      proxy: {
        '/api': {
          target: 'http://localhost:8081',
          secure: false
        }
      }
    }
  };
};
