# Implementation of the Smart House monitor using RETE algorithm

<!-- TODO: Add code base structure description and usage -->

## Description

In this project, we have implemented the Smart House monitor example from our
paper using the RETE algorithm provided by the [Evrete](https://www.evrete.org/)
library. Recall that the join patterns based implementation of the Smart House
monitor defines the following join patterns:

```scala
...
case (Motion(_, mStatus, mRoom, t0), 
      AmbientLight(_, value, alRoom, t1), 
      Light(_, lStatus, lRoom, t2)) if bathroomOccupied(...) => ...
case (Motion(_, mStatus0, mRoom0, t0), 
      Contact(_, cStatus, cRoom, t1), 
      Motion(_, mStatus1, mRoom1, t2)) if occupiedHome(...) => ...
case (Motion(_, mStatus0, mRoom0, t0),
      Contact(_, cStatus, cRoom, t1), 
      Motion(_, mStatus1, mRoom1, t2)) if emptyHome(...) => ...
...
```

Now, the RETE based implementation of the Smart House monitor using the Evrete
API is as follows:

### Initialisation and Configuration

We start by creating a `Knowledge` base using the
`KnowledgeService.newKnowledge()` method. We can then define rules and
conditions within this `Knowledge` base.

Now, the general skeleton for defining rules and facts in Evrete is as follows
(more details can be found in the [Evrete documentation](https://www.evrete.org/docs/)):

```java
Knowledge knowledge = service
    .newKnowledge()
    .builder()
    .newRule("Name of the rule")
    // Types of events to match
    .forEach(
        "$event1", Event1.class,
        "$event2", Event2.class,
        ...
    )
    // Conditions for the rule to fire (a guard)
    .where("$event1.field1 == $event2.field2", ...) 
    .execute(ctx -> {
            // Actions to be performed when the rule 
            // fires (This is the RHS of the rule)
    })
```

Here the  `forEach` method is used to specify the types of events that the rule. In our case,
the first join pattern corresponds to the following:

```java
...
.newRule("E1")
.forEach(
        "$motion", Motion.class,
        "$ambientLight", AmbientLight.class,
        "$light", Light.class)
.where(this::bathroomOccupied, "$motion", "$ambientLight", "$light")
.execute(ctx -> {
    Motion motion = ctx.get("$motion");
    AmbientLight ambientLight = ctx.get("$ambientLight");
    Light light = ctx.get("$light");
    rule1Matches.add(List.of(motion.id(), ambientLight.id(), light.id()));
})
...
```

Here, the `where` method is used to specify the conditions that must be
satisfied for the rule to fire. In our case, the `bathroomOccupied` method is
used to check if the conditions are satisfied. If the conditions are satisfied,
the `execute` method is used to specify the actions that must be performed when
the rule fires. In the body of the `execute` method, we add the matched events
to the `rule1Matches` list. This is done to keep track of the matched facts,
which will be used to select a *single* winner set of facts from across all
rules.

To be continued...

