# hgamesrhogue

A library that I use in my roguelike [Dungeon Mercenary](http://www.schplaf.org/hgames). Only depends on the Java library, on (a very small fragment of) [SquidLib](https://github.com/SquidPony/SquidLib), and on [hgameslib](https://github.com/smelc/hgameslib). This library contains roguelike-oriented code.

* Code in this library is Java 1.6 compliant.
* Code in this library is [GWT](www.gwtproject.org/) compliant.

This library does assumptions:

* All players of a game are always in the same level. Another way to put it is that there's only
  one ILevel being displayed at once (but maybe more are being simulated).

At some point in the future, a standalone version of this library will be released (including the required fragment of SquidLib and com.hgames.lib).

Code in this library is in the public domain (see LICENSE).
