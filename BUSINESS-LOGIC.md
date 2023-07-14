# FROST-Server STAplus Plugin Business Logic

According to the STAplus standard, the conformance class `Business Logic` describes in plain English text how certain
functionalities regarding CRud interactions with the STAplus entity types are implemented.

The following sections provide this detail for each STAplus entity type.

## Concept of Ownership

Once enabled (see [README](/README.md)), interactions with the STAplus and STA entity types follow the concept of
ownership. This requires that a user is authenticated and represented by the `Party` entity. In order to enforce the
concept of ownership, the cardinality to the Party entity type changes from `0..1` to `1` and thereby making the use of
the `Party` association mandatory.

### Party

The `Party` entity type must be created as one of the first interactions. The creation of other entities require linking
with the `Party` entity. The implementation sets the `authId` property and the `@iot.id` based on the value of
the `REMOTE_USER` resulting from authentication.

The `Party` entity cannot be deleted.

To ensure GDPR compliance, the user can correct the `displayName` and `description` or can even unset the values. As
the `authnId` value is set from the `REMOTE_USER` care must be taken to not accidentally disclose personal information
captured as usernames. The `Party` entity does not provide the `properties` property. It is therefore not possible to
store personal information at this place. If it is required to link a `Party` entity with personal data, some "other"
system must be able to resolve the `authId` and make personal data available. It is the responsibility of the operator 
of that other system to ensure GDPR compliance.

### Thing

A `Thing` entity can only be created if linked to the `Party` that represents the acting user. The implementation
compares the `authId` on the `Thing.Party` with the `REMOTE_USER` value.

The purpose of linking the `Thing` to a `Party` is to access control the setting of the `Location` on the `Thing`: Only
the `Party` linked to the `Thing` can set the `Location` value.
This is important in a use case, where the `Thing` is owned by another party as the `Datastream` (and the `Sensor`) that
produce data. For example, the `Thing` sensing-platform is owned by a truck company. They mount the `Thing` on all of 
their trucks. Sensing companies can then deploy their sensing-equipment and use the sensing-platform to produce data. 
The `Datastream`(s) or `MultiDatastream`(s) are owned by each sensing company. When the truck is moving, and the 
sensing-equipment is producing data, the `Thing` owner updates the `Location` of the sensing-platform.
In addition, each `Observation` produced by the sensing-equipment can be accompanied by a `FeatureOfInterest` that
defines the location where the observation was measured.

In reverse, the business logic of the implementation also prevents that the truck company can modify the `Observation`
or the `FeatureOfInterset`. In this example, that is only possible for the sensing company associated to the `Datastream`.

### Location

A `Location` must be associated to a `Thing`. Only the `Party` associated to the `Thing` can set the `Location`. The 
`HistoricalLocation` is updated automatically.

### Datastream / MultiDatastream

A `Datastream` or `MultiDatastream` can only be created if linked to the `Party` that represents the acting user. Only 
the associated `Party` can set the `ObservedProperty` and post `Observation`(s).

### Sensor

A `Sensor` must be associated to a `Datastream` or `MultiDatasteam`. In a multi-user environment, it is not possible
to have anonymous `Sensor` entities because it cannot be controlled which user may attach the sensor to their
`Datastream` or `MultiDatasteam`.

### Project

A `Project` can only be created if linked to the `Party` that represents the acting user. 

The associated `Party` can add/remove `Datastream`, `MultiDatasteam`, and `Group` entities to the Project.

The associated `Party` has the exclusive right to update properties and in particular set the `endTime`. Once the 
`endTime` is set and reached, no more modifications can be done by the associated user.

### Group

The functional behavior for the `Group` is similar to the `Project` entity type. The difference is that `Observations`
(and not `Datastream` or `MultiDatasteam`) can be added by the associated `Party`.

### Relation

A `Relation` can only be created if the `Subject` is linked to the `Party` that represents the acting user.

### License

This implementation generates a set of Creative Commons licenses as configured (see [README](/README.md) for details).
These `License` entities are read-only.

## Enforce Licensing

Once enabled, it controls a specific use of `License` entities.

### License

A user may desire to use a personalized `BY` (attribution) license. Such a license must have the `attributionText` 
property set and use one of allowed definitions for the supported Create Commons licenses:

- https://creativecommons.org/licenses/by/3.0/deed.en
- https://creativecommons.org/licenses/by-nc/3.0/deed.en
- https://creativecommons.org/licenses/by-sa/3.0/deed.en
- https://creativecommons.org/licenses/by-nc-sa/3.0/deed.en
- https://creativecommons.org/licenses/by-nd/3.0/deed.en
- https://creativecommons.org/licenses/by-nc-nd/3.0/deed.en

A `License` entity can only be created by a user when associated to either a `Datastream`, `MultiDatastream`, `Project` 
or `Group`.

### Datastream / MultiDatastream
A `Datastream` or `MultiDatastream` may get associated with a `License` until the first `Observation` is pushed. That
license association can not be changed afterwards. This ensures consistent licensing.

### Group
A `Group` may get associated with an `License` until the first `Observation` is associated. That
license association can not be changed afterwards. This ensures consistent licensing.

### Project
A `Project` may get associated with a `License` until the first `Datastream` or `MultiDatastream` is pushed. That
license association can not be changed afterwards. This ensures consistent licensing.