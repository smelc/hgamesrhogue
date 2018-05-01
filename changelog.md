# Changes this release 1.0

* Zone::getAll() became Zone::getAll(boolean). The Boolean indicates whether a fresh list is required, to be able to spare allocations when false is given.
* Zone::add(Coord) and Zone::remove(Coord), that were deprecated, have been removed.

# Release 1.0 on 2018, April 29th
