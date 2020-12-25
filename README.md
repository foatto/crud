# crud

CRUD-generator platform for easy creation of information systems.

Support tables, forms, value graphs, cartography maps. 

The access control subsystem based on a combination of roles and enterprise structure.  

There are rudiments of equipment state control and video surveillance support (archived, refactoring needed).

In previous versions, there was support for desktop (Swing, JavaFX later) and android clients, but it was cut due to lack of time for full support.

Some modules are application systems in production.  

## core

Core module.

## core_server

Core server module.

Uses self-written multi-master asynchronous database replication.

## core_web

Core web-client module.

Uses wrapper for Vue.js for tables and forms, SVG for graphics and maps.
Map subsystem based on a self-written engine. The engine supports crud-operations for vector elements on a map. 

## mms_server + mms_web

Application module of transport, drilling equipment, electic energo generating and fuel consumption control system.

To receive GPS & telematic data from devices, it uses a self-written engine based on NIO. In the future, it is planned to rewrite on Netty.

In production and support now.

## office_server + office_web

Application module of order control system. 

Nothing interesting, just a very ancient about 15-year-old project rewritten with modern technologies.

In production and support now.

(unused modules of reminders, meetings beign archived)

## shop_server + shop_web

Application module of retail and warehouse accounting systems.

I once waited too long for a retail automation systems specialist...
It turned out that writing yourself is much faster and more interesting.
Used in our own family textile company.

Implemented support for online checkouts.

In production and support now.
