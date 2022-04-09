# Sensor Things API - PLUS (Project License User Semantics)

PLUS is a FROST-Server plugin that adds additional classes to the Sensor Things Data Model.

This repository builds with the FROST-Server 2.0 Snapshot.

Work on this project is funded by the European Commission under Grant Agreement No. 863463.

The following figures illustrate the extension in an UML diagram.

![Sensor Things Datamodel (Datastream) with PLUS extension](doc/STAplus%20Sensing%20Entities.png "Sensor Things Datamodel (Datastream) with PLUS extension")

![Sensor Things Datamodel (MultiDatastream) with PLUS extension](doc/STAplus%20MultiDatastream%20Extension%20Entities.png "Sensor Things Datamodel (MultiDatastream) with PLUS extension")

## Settings

* **plugins.plus.enable:**  
  Toggle indicating the PLUD plugin should be enabled. Default: `false`.
* **plugins.plus.idType.groups:**  
  The type of the primary key column of the Groups table. Defaults to the value of **plugins.coreModel.idType**.
* **plugins.plus.idType.license:**  
  The type of the primary key column of the Licenses table. Defaults to the value of **plugins.coreModel.idType**.
* **plugins.plus.idType.project:**  
  The type of the primary key column of the Projects table. Defaults to the value of **plugins.coreModel.idType**.
* **plugins.plus.idType.relation:**  
  The type of the primary key column of the Relations table. Defaults to the value of **plugins.coreModel.idType**.

**_NOTE:_** `plugins.plus.idType.party` is set to UUID by the implementation. This setting cannot be changed!

## Enforcement of Ownership
The activation of the `Enforcement of Ownership` allows to operate the STAplus endpoint in multi-user-write mode. However, it requires to enable Authentication.

Each acting user is identified via a unique UUID, represented by a `Party` object.

The classes `Thing`, `MultiDatastream`, `Datastream` and `Group` are directly associated to a Party. Objects of class `Observation` are linked to the owning Party object via the `(Multi)Datastream`.

It is important to note that the multiplicty is `[1]` when activating the Concept of Ownership. So, creating objects for class `Thing`, `MultiDatastream`, `Datastream` or `Group` require to be associated with the Party object that represensts he acting user.

A user can `update` or `delete` any own object. However, the user *cannot* delete the own Party. This requires admin access.

Anonymous read is possible.

### Settings

**plugins.plus.enable.enforceOwnership:**  
  Toggle indicating the enforcement of ownership. Default: `false`.

## Enforcement of Licensing
According to the STAplus Data Model, a `Datastream` and a `Group` both may have a `License` associated. When adding (an) `Observation(s)` to a `Group`, the `Enforcement of Licensing` ensures that the `License`, associated to (an)  `Observation(s)` is compatible to the `License` associated to a `Group`.

When activating the `Enforcement of Licensing`, the plugin enforces licenses compatibility based on the Creative Commons v3 licensing model an the license compatibility according to the official cart. 

![License Compatibility Chart](doc/CC_License_Compatibility_Chart.png) See https://wiki.creativecommons.org/wiki/Wiki/cc_license_compatibility for more information.

The plugin creates the different Creative Commons Licenses in read-only mode as "system-wide" globals. An application cannot create new `License` objects nor can it update or delete the existing ones. An application can obtain a list of the "system-wide" licenses via the `/Licenses` path.

### Settings

**plugins.plus.enable.enforceLicensing:**  
  Toggle indicating the enforcement of licensing. Default: `false`. 
  