// Copyright © 2016-2019 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

const path = require('path');

module.exports = {
  entry: './src/main/js/index.js',
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
        // local scoped CSS
        test: /\.css$/,
        exclude: /node_modules|src\/main\/css/,
        use: [
          'style-loader',
          {loader: 'css-loader', options: {modules: true, localIdentName: '[name]__[local]--[hash:base64:5]'}},
        ],
      },
      {
        // global scoped CSS
        test: /\.css$/,
        include: /node_modules|src\/main\/css/,
        use: ['style-loader', 'css-loader'],
      },
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
