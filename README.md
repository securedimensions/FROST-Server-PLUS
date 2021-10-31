# Sensor Things API - PLUS (Project License User Semantics)

PLUS is a FROST-Server plugin that adds additional classes to the Sensor Things Data Model.

This repository builds with the FROST-Server 2.0 Snapshot.

Work on this project is funded by the European Commision under Grant Agreement No. 863463.

The following figures illustrate the extension in an UML diagram.

![Sensor Things Datamodel (Datastream) with PLUS extension](doc/2021-05-21-DataModel-Datastream.png "Sensor Things Datamodel (Datastream) with PLUS extension")

![Sensor Things Datamodel (MultiDatastream) with PLUS extension](doc/2021-05-21-DataModel-MultiDatastream.png "Sensor Things Datamodel (MultiDatastream) with PLUS extension")

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
