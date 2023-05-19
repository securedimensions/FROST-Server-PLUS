# Sensor Things API - PLUS (STAplus)
This repository contains an open source reference implementation of STAplus.

## About the OGC STAplus Standard
STAplus is an OGC Candidate Standard [22-022](https://docs.ogc.org/DRAFTS/22-022.html) that extends the suite of OGC SensorThings API [v1.0](https://docs.ogc.org/is/15-078r6/15-078r6.html) and [v1.1](https://docs.ogc.org/is/18-088/18-088.html) Standards.

<cite>"STAplus - SensorThings API extension PLUS - defines a SensorThings data model extension to improve FAIR principles when exchanging sensor data including licensing and ownership information. The STAplus extension is fully backwards compatible to the existing OGC SensorThings API Part 1: Sensing Version 1.0 and 1.1 and thereby offers existing deployments to easily upgrade to STAplus."</cite>[^22_022]

[^22_022]: [OGC SensorThings API Extension: STAplus 1.0](https://docs.ogc.org/DRAFTS/22-022.html)

The following simplified[^simplified]  UML diagrams illustrate the data model extension towards SensorThings API `Datastream` and `MultiDatastream`.

[^simplified]: The SensorThings API classes are empty

**_NOTE:_** The yellow colored classes belong to the SensorThings API data model.

### STAplus Data Model connected to `Datastream`
![Sensor Things Datamodel (Datastream) with PLUS extension\label{Datastream}](doc/STAplusSensingEntities.png)

### STAplus Data Model connected to `MultiDatastream`
![Sensor Things Datamodel (MultiDatastream) with PLUS extension\label{MultiDatastream}](doc/STAplusMultiDatastreamExtensionEntities.png "Sensor Things Datamodel (MultiDatastream) with PLUS extension")


## About the Implementation
This repository contains an open source reference implementation of STAplus as a [FROST-Server](https://github.com/FraunhoferIOSB/FROST-Server) plugin.

This implementation supports the conformance classes `Core`, `Authentication` and `Business Logic`  as defined in the STAplus Candidate Standard. The `API` conformance class is already supported by the FROST-Server implementation.

### Business Logic
This implementation enforces the concept of ownership as explained in detail below.


## Deployment
The deployment of the STAplus plugin requires a working deployment of the FROST-Server. You can follow the [FROST-Server documentation](https://fraunhoferiosb.github.io/FROST-Server/) to run your instance.

### Build STAplus
This repository builds with the FROST-Server 2.2.0 SNAPSHOT.

### Deploy STAplus

## Configuration
Different features of the STAplus plugin can be activated / deactivated using FROST-Server alike configuration variables:

* **plugins.staplus.enable:**  
  Set to `true` to activate the STAplus plugin. Default: `false`.
* **plugins.staplus.idType.groups:**  
  The type of the primary key column of the Groups table. Defaults to the value of **plugins.coreModel.idType**.
* **plugins.staplus.idType.license:**  
  The type of the primary key column of the Licenses table. Defaults to the value of **plugins.coreModel.idType**.
* **plugins.staplus.idType.project:**  
  The type of the primary key column of the Projects table. Defaults to the value of **plugins.coreModel.idType**.
* **plugins.staplus.idType.relation:**  
  The type of the primary key column of the Relations table. Defaults to the value of **plugins.coreModel.idType**.

**_NOTE:_** The type of the primary key column of the Party table (`plugins.staplus.idType.party`) is set to UUID by the implementation. This setting cannot be changed!

## Enforcement of Ownership
The activation of the `Enforcement of Ownership` allows to operate the STAplus endpoint in multi-user-CRUD mode. However, it requires to enable Authentication.

Each acting user is identified via a unique UUID, provided by the authentication plugin. The `REMOTE_USER` value is used to identify the user. The value of `REMOTE_USER` represents the user as a `Party` object via the `authId` property. When creating a `Party` object, the value for the `authId` property must either be empty or match the value for the `REMOTE_USER`. All other values are rejected by the implementation and will result in a response with HTTP status code 400.

The classes `Thing`, `MultiDatastream`, `Datastream` and `Group` are directly associated to a Party. Objects of class `Observation` are linked to the owning Party object via the `(Multi)Datastream`.

When activating the Concept of Ownership, the implementation enforces the multiplicity `[1]` on the association of these classes to `Party`. Therefore, creating objects of class `Thing`, `MultiDatastream`, `Datastream` or `Group` require the associated with the Party object that represents the acting user. 

A user can `update` or `delete` any object owned. However, the user *cannot* delete the own Party. This requires admin access.


### Settings

**plugins.plus.enable.enforceOwnership:**  
Set to `true` to enable the enforcement of ownership. Default: `false`.

## Enforcement of Licensing
According to the STAplus Data Model, a `Datastream`, `Group` and `Project` may have a `License` association. In order to ensure the use of compatible licenses, this implementations generates a given set of configured licenses.

### Settings
The file `resources/tables.xml` contains as the last constructor for table generation the entry 
```xml
    <include relativeToChangelogFile="true" file="insertCCLicenes.xml" />
```
The file `insertCCLicenses.xml` contains the set of licenses that are generated by the implementation. 

You can change this configuration accordingly to load a different set of licenses.



## Enforcement of Group Licensing
When adding (an) `Observation(s)` to a `Group`, the `Enforcement of Licensing` ensures that the `License`, associated to (an) `Observation(s)` is compatible to the `License` associated to a `Group`.

When activating the `Enforcement of Licensing`, the plugin enforces licenses compatibility based on the Creative Commons v3 licensing model an the license compatibility according to the official cart. 

![License Compatibility Chart](doc/CC_License_Compatibility_Chart.png) See https://wiki.creativecommons.org/wiki/Wiki/cc_license_compatibility for more information.

The plugin creates the different Creative Commons Licenses in read-only mode as "system-wide" globals. An application cannot create new `License` objects nor can it update or delete the existing ones. An application can obtain a list of the "system-wide" licenses via the `/Licenses` path.

### Settings

**plugins.plus.enable.enforceLicensing:**  
Set to `true` to enable the enforcement of licensing. Default: `false`. 

## Appreciation
Work on this project has being funded by the European Commission under Grant Agreement No. 863463 and 101086421.
