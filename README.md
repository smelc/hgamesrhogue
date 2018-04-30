# hgamesrhogue

A library that I use in my roguelike [Dungeon Mercenary](http://www.schplaf.org/hgames). Only depends on the Java library, on [my fork](https://github.com/smelc/SquidLib) of [SquidLib](https://github.com/SquidPony/SquidLib), and on [hgameslib](https://github.com/smelc/hgameslib). This library contains roguelike-oriented code.

* Code in this library is Java 1.6 compliant.
* Code in this library is [GWT](www.gwtproject.org/) compliant.

The most prominent features of this library are:

* A customizable map generator: [DungeonGenerator](https://github.com/smelc/hgamesrhogue/blob/master/src/main/java/com/hgames/rhogue/generation/map/DungeonGenerator.java). In action: [gif](https://i.imgur.com/gYMyLPw.gifv), [png1](https://i.imgur.com/R3APwq0.png), [png2](https://i.imgur.com/8C0L8aq.png)
* [Flexible monster generation](http://hgamesdev.blogspot.fr/2017/11/flexible-object-oriented-generation-of.html)
* The [Zone](https://github.com/smelc/hgamesrhogue/blob/master/src/main/java/com/hgames/rhogue/zone/Zone.java) API, to manipulate rooms and corridors in an object-oriented manner
* Object Oriented [Field Of View](https://github.com/smelc/hgamesrhogue/blob/master/src/main/java/com/hgames/rhogue/fov/ShadowCastingObjectFOV.java)

Compared to vanilla SquidLib, my fork is much smaller as I've deleted a lot of exotic featutes. But the most important features remain: SquidPanel, the class to draw a unicode grid; grid algorithm (Field Of View, Line Of Sight, Bresenhan); and pathfinding (A\* and DijkstraMap).

Code in this library is in the public domain (see LICENSE).
