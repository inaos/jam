# INAOS Acceleration Modules for Java Applications

## Limitations

### Hotspots in constructors

Since we can not add/replace constructors if a class was already loaded we can not offer to replace a hot constructor.
However, we can only hope that if a constructor is expensive the actual work would be sourced to another method which we could 
replace instead. If this is also not possible we have to do the replacement more coarse grained by replaceing also the code 
which instanstiates the object with the hot constructor.

## Getting started with a new project

### Installation

* Adding the API to your Maven pom
* Place the agent.jar on your local file-system

### Running the agent

#### JVM arguments

#### Attach to process-id (DevMode)

### Analyze Toolbox (DevMode)

#### Serialize Input/Output of functions

### Generate native templates

### Integrate

## Agent Development
