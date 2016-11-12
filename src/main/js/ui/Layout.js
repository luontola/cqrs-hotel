// Copyright © 2016 Esko Luontola
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

import React from "react";

/*
 Based on http://purecss.io/layouts/side-menu/
 */
var Layout = ({children}) => (<div id="layout">

    {/* Menu toggle */}
    <a href="#menu" id="menuLink" className="menu-link">
      {/* Hamburger icon */}
      <span/>
    </a>

    <div id="menu">
      <div className="pure-menu">
        <a className="pure-menu-heading" href="#">CQRS Hotel</a>

        <ul className="pure-menu-list">
          <li className="pure-menu-item"><a href="#" className="pure-menu-link">Home</a></li>
          <li className="pure-menu-item"><a href="#" className="pure-menu-link">About</a></li>

          <li className="pure-menu-item menu-item-divided pure-menu-selected">
            <a href="#" className="pure-menu-link">Services</a>
          </li>

          <li className="pure-menu-item"><a href="#" className="pure-menu-link">Contact</a></li>
        </ul>
      </div>
    </div>

    <div id="main">
      <div className="header">
        <h1>CQRS Hotel</h1>
        <h2>Example application about CQRS and Event Sourcing</h2>
      </div>

      <div className="content">
        {children}
      </div>
    </div>
  </div>
);

// TODO: adapt this code to make the layout work with React
function init(window, document) {

  var layout = document.getElementById('layout'),
    menu = document.getElementById('menu'),
    menuLink = document.getElementById('menuLink');

  function toggleClass(element, className) {
    var classes = element.className.split(/\s+/),
      length = classes.length,
      i = 0;

    for (; i < length; i++) {
      if (classes[i] === className) {
        classes.splice(i, 1);
        break;
      }
    }
    // The className is not found
    if (length === classes.length) {
      classes.push(className);
    }

    element.className = classes.join(' ');
  }

  menuLink.onclick = function (e) {
    var active = 'active';

    e.preventDefault();
    toggleClass(layout, active);
    toggleClass(menu, active);
    toggleClass(menuLink, active);
  };
}

export {Layout};
