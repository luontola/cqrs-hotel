// Copyright © 2016-2017 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

var path = require('path');

function getEntrySources() {
  var sources = [];
  if (process.env.NODE_ENV !== 'production') {
    sources.push('webpack-dev-server/client?http://localhost:8080');
  }
  for (var i = 0; i < arguments.length; i++) {
    sources.push(arguments[i]);
  }
  return sources;
}

module.exports = {
  entry: getEntrySources(
    './src/main/js/index.js'
  ),
  output: {
    path: path.resolve(__dirname, 'target/webpack'),
    filename: 'bundle.js',
  },
  module: {
    loaders: [
      {test: /\.js$/, loaders: ['jsx', 'babel'], exclude: /node_modules/},
      {test: /\.css$/, loader: 'style!css'},
    ]
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
