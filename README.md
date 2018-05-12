# INAOS Acceleration Modules for Java Applications

## Limitations

### Hot-spots in constructors

Since we can not add/replace constructors if a class was already loaded we can not offer to replace a hot constructor.
However, we can only hope that if a constructor is expensive the actual work would be sourced to another method which we could 
replace instead. If this is also not possible we have to do the replacement more coarse grained by replacing also the code 
which instantiates the object with the hot constructor.

### Unloading/Reloading of Applications in Enterprise-Applications

Initialization and destruction of Accelerator-Modules and therefore native libraries is controlled by annotating methods by Acceleration.Library.Init or Acceleration.Library.Destroy. The first method will be invoked from the class's initializer, the destruction method from a shut down hook.

One consequence of this change is that native dispatchers with destruction methods make it impossible to unload a class loader. This can be a problem in environments where an application container such as Websphere is used and where applications are redeployed. A shutdown hook is only notified upon a JVM's shutdown. If the JVM's lifecycle does not match the application's life cycle, this can be problematic. In Java, unfortunately, there is no good way to hook into an object's (in this case the class loader's) life cycle. The supposed way to do this is by using a reference queue where the queue is however only notified after the class and class loader are collected what makes it impossible to call methods. The reason for this design in Java is that it would break the automatic garbage collection if such a finalizer reinstated the reference that is currently garbage collected what would break the heap integrity.

Theoretically it would be possible to create a custom class loader and to reload the native library using this class loader, call the method, and then unload this new class loader again.

However, it is not known if this plays out in practice. App servers are generally difficult as they do not respect a JVM's life cycle. We'll come back to this problem in a later release.

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
