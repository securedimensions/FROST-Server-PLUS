# Sensor Things API - PLUS (Project License User Semantics)

PLUS is a FROST-Server plugin that adds additional classes to the Sensor Things Data Model.

This repository builds with the FROST-Server 2.0 Snapshot.

Work on this project is funded by the European Commission under Grant Agreement No. 863463.

The following figures illustrate the extension in an UML diagram.

![Sensor Things Datamodel (Datastream) with PLUS extension](doc/2021-12-01-DataModel-Datastream.png "Sensor Things Datamodel (Datastream) with PLUS extension")

![Sensor Things Datamodel (MultiDatastream) with PLUS extension](doc/2021-12-01-DataModel-MultiDatastream.png "Sensor Things Datamodel (MultiDatastream) with PLUS extension")

## Settings

* **plugins.plus.enable:**  
  Toggle indicating the Actuation plugin should be enabled. Default: `false`.
* **plugins.plus.idType.groups:**  
  The type of the primary key column of the Groups table. Defaults to the value of **plugins.coreModel.idType**.
* **plugins.plus.idType.license:**  
  The type of the primary key column of the Licenses table. Defaults to the value of **plugins.coreModel.idType**.
* **plugins.plus.idType.party:**  
  The type of the primary key column of the Parties table. Defaults to the value of **plugins.coreModel.idType**.
* **plugins.plus.idType.project:**  
  The type of the primary key column of the Projects table. Defaults to the value of **plugins.coreModel.idType**.
* **plugins.plus.idType.relation:**  
  The type of the primary key column of the Relations table. Defaults to the value of **plugins.coreModel.idType**.

## Party
This implementation enforces the following conditions on the Party class:

* **POST:**
  POSTing a request that involves creating a Party instance, the `authId` will be set by the plugin either to `00000000-0000-0000-0000-000000000000` if anonymous posting is allowed. Otherwise the `authid` will be set to the UUID that represents the user. If the `REMOTE_USER` is a UUID, then that value will be used; otherwise, a UUID will be generated from the value of the `REMOTE_USER`.
  If the POSTed Party includes the `authId` property, the plugin will throw an IllegalArgumentException resulting in HTTP 400.
* **PATCH:**
  PATCHing an existing Party instance, must not involve the `authId` property. This property is read-only. The plugin throws an IllegalArgumentException (HTTP 400) in the attempt to change the `authId` value (the value of the `authId` property for the existing Party is different from the posted `authId`). A PATCH is permitted, if the `authId` is included in the request and the value is identical to the one for the existing Party instance (as this does not change the `authId` value).
* **DELETE:**
  DELETEing an existing Party instance results in an IllegalArgumentException (HTTP 400).
  
