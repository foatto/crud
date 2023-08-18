# foatto.CRUD

CRUD - it is a platform for the rapid creation of information systems. [[Версия на русском языке](README_ru.md)]

## Description of platform features

The client part of the platform supports the following types of displayed information:
   - table views;
   - forms of data viewing and editing;
   - graphs of measurements;
   - cartography with vector custom elements and the possibility of their interactive modification;
   - SCADA-like display of the state of objects;

The server part of the platform offers the following features:
  - declarative defining of attributes of storage, display and relations of application modules using the internal ORM;
  - support for the main dialects of SQL DBMS;
  - asynchronous master-to-master transaction replication with support for different SQL dialects on servers and the ability to proxy replicas between servers through an intermediary node;
  - a system of access rights based on a combination of roles (positive and negative) and the position of the user in the structure of the enterprise;
  - a separate server for receiving telematic data (and sending commands) from IoT devices (technological / telematic devices and controllers) using various binary and text protocols using the TCP as a transport protocol;

Planned features:
   - support for displaying video information (with reference to measurement graphs and cartography elements);
   - adding new client platforms (android and desktop versions) based on JetBrains Compose Multiplatform technologies;

Deprecated and already removed platform features:
   - desktop-client on Swing;
   - desktop-client on JavaFX;
   - ancient android-client in Java for "Android 4.x";
   - web client based on Kotlin/JS technology (legacy backend) + Vue.js wrapper;

Currently, four applied projects that are in a production have been implemented on this platform:
  - ["Pulsar" - transport and technological facilities control system](https://pulsar.report):
    - transport control - mileage, location in geofences, etc.;
    - operation of drilling equipment;
    - power generation installations;
    - control of the level and consumption of fuel in tanks and cisterns;

    In production since 2006.
  - [Order Execution Control System](http://crm.magnol.ru)

    In production since 2003.
  - Retail and Warehouse Management System:
    - Support for retail operations;
    - support for online cash registers and ATOL receipt printers;
    - support for the product labeling system;
    - support of warehouse accounting;

    In production since 2014.
  - Control system for well dewaxing units "UDS-Techno"

## Project structure description

All source code of the platform and working projects is formatted as modules of the main project

### core

The core module contains code common to the client and server parts:
  - Request classes;
  - Response classes;
  - Data/Info/Config-classes;
  - common constants, functions and interfaces;

The JVM part of the core module contains classes that may be useful for the Desktop/Android version.

### core_compose

A core compose module containing common classes, functions, and constants for platform-specific compose modules (Android, Desktop, Web).

### core_compose_web

A web client module common to all applied projects using a combination of Kotlin/JS and JetBrains Compose for Web technologies:
  - Composable functions for displaying interactive pages;
  - Kotlin/JS for implementing reactions to events;
  - CSS Grid & Flex to display tabular and edit forms;
  - SVG for displaying measurement graphs, cartography and SCADA-like display of the state of objects;
  - automatic switching between two layout options (for narrow screens of mobile devices and wide screens of desktop systems);
  - support for touch screens;
  - support for different point densities (relevant for measurement graphs and cartography);
  - the ability to change design parameters for branding applied projects;

### core_server

Contains common server components for all application projects that do not depend on a specific application area and on the implementation of a specific web container (Spring Boot MVC/WebFlux, Ktor, etc.):
  - Column/Data-classes for the implementation of the internal ORM;
  - base model & controller classes for declarative definition and organization of tabular forms and editing forms;
  - modules for basic preparation of data for display on measurement graphs;
  - modules for basic preparation of data for displaying cartography, user vector data on it;
  - modules for basic data preparation for a schematic display of the state of objects;
  - system classes for organizing the work of any application:
    - configuration of system modules;
    - user accounts;
    - user roles;
  - interfaces for accessing the functions of an external web container (Spring Boot MVC/WebFlux, Ktor, etc.);
  - extensions of standard JDBC classes to provide transactional replication;
  - a module for exchanging transactional replicas with other cluster servers;
  - a set of basic modules for creating servers for receiving telematic data with IoT (in NIO.2 implementation);
  - component for working with S3-like storages (MinIO);

### core_server_mvc

A core web container that uses Spring Boot MVC technology and contains the following modules:
   - core Spring MVC application;
   - main Spring MVC application controller;
   - controller for loading / unloading files (uses the file system or S3 storage);
   - transactional replication controller;
   - user accounts controller;
   - a set of core JPA entities and repositories;

### core_server_flux

A core web container that uses the Spring Boot WebFlux technology to work.
Currently under development.

### mms: core, compose_web, server, server_mvc

A group of application modules implementing the project ["Pulsar" - transport and technological facilities control system](https://pulsar.report):
  - core: contains mms-specific common modules, similar to the core common module;
  - web: contains mms-specific settings for the color scheme of the client side and special design elements;
  - server: contains business logic modules of the application part:
    - modules for calculating mileage, equipment operation, fuel consumption, electricity generation;
    - modules for working with a list of devices/controllers of various types;
    - configuration modules for various sensors, with the ability to set smoothing and calibration parameters:
      - GPS/GLONASS sensors;
      - operation of the equipment;
      - indications from electric meters;
      - fuel level sensors (with additional setting of parameters for refueling and drain detectors);
      - various analog values;
      - signal sensors;
    - protocol handler classes for devices/controllers of various types (in versions for NIO.2 telematics servers):
      - [Galileo](https://7gis.ru)
      - obsolete and no longer supported: ADM, Arnavi, Galileo Iridium, Mielta, Wialon IPS;
    - data preparation modules for displaying and changing applied geo-information on a cartographic substrate (point and text objects, vector geofences);
    - data preparation modules for displaying the state of objects in a SCADA-like form;
    - data preparation modules for measurement charts:
      - complex graphs of fuel level with display:
        - periods of refueling and draining;
        - operation of the equipment;
        - location in geofences (for mobile objects);
      - analog sensors:
        - fuel level;
        - weight;
        - revolutions of rotation;
        - pressure;
        - temperature;
        - power;
        - density;
        - mass and volume flow;
        - electrical parameters (by phase):
          - voltage;
          - current;
          - power factor;
          - active, reactive and total power;
      - speed schedules;
    - modules for generating various reports;
    - support modules for various directories (including objects, geofences);
    - data preparation modules for displaying various work logs (daily, shift, waybills);
    - service modules:
      - automatic deletion of obsolete telematics data;
      - monitoring of objects (loss of communication, lack of data, etc.);
    - support modules for video surveillance systems (outdated and disabled, needs to be reworked);
  - server_mvc: application web container that uses Spring Boot MVC technology and contains the following modules:
    - Spring MVC application;
    - Spring MVC controller;
    - a set of mms-specific JPA entities and repositories;

### office: core, compose_web, server, server_mvc

Group of application modules implementing the project [Instruction Execution Control System](http://crm.magnol.ru)

(in the process of being documented...)

### shop: core, compose_web, server, server_mvc

A group of application modules implementing the project "System of retail trade and warehouse accounting"

(in the process of being documented...)

### ts: core, compose_web, server, server_mvc

A group of application modules implementing the project "Control system for well dewaxing units "UDS-Techno"

(in the process of being documented...)

## Technical part

The source code is written in Kotlin and organized as a Kotlin Multiplatform gradle project.
