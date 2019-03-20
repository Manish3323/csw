# Akka HTTP Adapter (csw-aas-http)

This library is a security adapter for akka-http server applications. `csw-aas` uses 
[OpenId Connect](https://openid.net/connect/) for authentication and authorization.
The authentication server used by AAS is [Keycloak](https://www.keycloak.org/).
We recommended that you get familiar with Keycloak's documentation and configurations to
fully leverage this adapter's features.

This adapter provides authentication via security directives such as `sGet`, `sPost`, `sPut`, etc.
These directives are used in routing and replace the default `get`, `post`, `put`, etc. directives
from Akka HTTP.  This allows custom policies to be enforced at the routing level. 
For authorization, these secure directives accept a wide range of policy expressions.  The usage of these 
directives are described below.

## Dependencies

To use the Akka HTTP Adapter (csw-aas-http), add this to your `build.sbt` file:

sbt
:   @@@vars
    ```scala
    libraryDependencies += "com.github.tmtsoftware.csw" %% "csw-aas-http" % "$version$"
    ```
    @@@
    
## Prerequisites

To run an akka-http server app, which uses this adapter, we need

* Location Service running
* AAS instance running and registered with Location Service

Location Service and AAS can be running on different machines. To start Location Service and AAS 
server on a local machine, you can make use of the csw-services.sh script.

## Application Configurations

All auth related configurations go inside an `auth-config` block. There are three configurations 
applicable for a akka-http server application i.e. `realm`, `client-id` & `enable-permissions`. 

`realm` has a default value of `TMT` if not specified. Ideally all apps in TMT should not have to override
this, however it might be useful to override this while testing your app.

`enable-permissions` is optional config with a default value of `false`. Typically, roles are used for
authorization and specific permissions is not needed.  However, if your akka-http server application
uses permission based authorization policies, this config needs to be set to true.

`client-id` is a mandatory configuration which specifies the client id of the app as per registration
in AAS.

```hocon
auth-config {
  realm = TMT # DEFAULT
  enable-permissions = false # DEFAULT
  client-id = demo-cli # REQUIRED
} 
```

## Building a Secure Akka-HTTP server application

The core of this adapter is the `SecurityDirectives` class. The recommended way to instantiate 
`SecurityDirectives` is as shown below.

Scala
:   @@snip [SecurityDirectives](../../../../../examples/src/main/scala/example/auth/ExampleServer.scala) { #sample-http-app }

Importing everything from security directives is recommended as it imports some implicit 
methods along with all security directives.

In the above example, 

* `GET http://localhost:9003/api` does not use any security directive and hence is accessible to all. 

* `POST http://localhost:9003/api` uses `sPost` which is secure directive. This directive takes care of authentication (access token signature and expiration validation).
For authorization, it needs an [authorization policy](#authorization-policies). Authorizing policy specifies one or more conditions for request validation. 

In this instance, `sPost` directive has been given `RealmRolePolicy` policy with parameter value `admin`.

This results into following sequence of actions when a request arrives for a secure directive route

1. Check request header to look for an access token
1. Validate the token signature and expiry
1. Check the token for roles and validate that it has `admin` [realm role](https://www.keycloak.org/docs/latest/server_admin/index.html#realm-roles)
1. After all the above checks/validations pass, execute the route logic 

If any of the validations fails, an appropriate HTTP status code is returned to the requester.
For authentication failure `401` is sent and for authorization failure `403` is sent.

@@@ note
To know more about realm roles, check out the [Keycloak documentation](https://www.keycloak.org/docs/latest/server_admin/index.html#realm-roles)
@@@

## Authorization Policies 

An authorization policy is a way to provide filter incoming HTTP requests based on standard rules. Following policies can
be applied to protect routes.

 - [ReamRolePolicy](#realmrolepolicy)
 - [ClientRolePolicy](#clientrolepolicy)
 - [PermissionPolicy](#permissionpolicy)
 - [CustomPolicy](#custompolicy)
 - [CustomPolicyAsync](#custompolicyasync)
 - [EmptyPolicy](#emptypolicy)

### RealmRolePolicy

This policy filters requests based on Realm Role. A Realm Role is global and is applicable for all clients 
within realm.

In the following example policy will authorize request if user has assigned `admin`

Scala
:   @@snip [Realm Role Policy](../../../../../examples/src/main/scala/example/auth/ExampleServer.scala) { #realm-role-policy-usage }
 
### ClientRolePolicy

Client roles are basically a namespace dedicated to a client. Each client gets its own namespace.

This policy filters requests based on Client Role. In the following example policy will authorize 
request if user has assigned `accounts-admin` for clientId specified in configurations

Scala
:   @@snip [Client Role Policy](../../../../../examples/src/main/scala/example/auth/ExampleServer.scala) { #client-role-policy-usage }
 

### PermissionPolicy

This policy filters requests based on permissions. It expects name of scope and name of resource on which permission is created 
in AAS. 

In the following example policy will authorize request if user has appropriate permission associated in AAS which specifies
`delete` scope for `account` resource.

Scala
:   @@snip [Permission Policy](../../../../../examples/src/main/scala/example/auth/ExampleServer.scala) { #permission-policy } 

### CustomPolicy

This policy allows custom request filtering based on access token properties. It expects predicate function which 
accepts an access token and returns a boolean. If the predicate returns true, it indicates user is authorized.

In the following example policy will authorize request if user's given name contains `test-user`

Scala
:   @@snip [Custom Policy](../../../../../examples/src/main/scala/example/auth/ExampleServer.scala) { #custom-policy-usage } 

### CustomPolicyAsync

This policy is similar to CustomPolicy with only difference that it expects a predicate which returns
a Future of Boolean instead of a Boolean. This could be very useful for custom validations which need
to make an IO call. For example,

Scala
:   @@snip [Custom Policy](../../../../../examples/src/main/scala/example/auth/ExampleServer.scala) { #custom-policy-async } 

This forms a an http route for a secure GET request for path `/files/[fileId]` and expects a path parameter
of type `Long`. The async custom policy makes an async database call to check whether the
file being requested belongs to the user who made http request.

### EmptyPolicy

This policy is used this when only authentication is needed but not authorization.
EmptyPolicy is an object and not a class like other policies and it does not need any parameters.

Scala
:   @@snip [Empty Policy](../../../../../examples/src/main/scala/example/auth/ExampleServer.scala) { #empty-policy-usage }
 

## Security Directives

`csw-aas-http` adapter supports following secure HTTP verbs:

| Name | Description |
|--- |--- |
|sGet |Rejects all un-authorized and non-GET requests |
|sPost |Rejects all un-authorized and non-POST requests |
|sPut |Rejects all un-authorized and non-PUT requests |
|sDelete |Rejects all un-authorized and non-DELETE requests |
|sHead |Rejects all un-authorized and non-HEAD requests |
|sConnect |Rejects all un-authorized and non-CONNECT requests |

## Using Access Token

A handle of access token type is given to to all secure routes. It is optional to define parameter for it.

For example:

Scala
:   @@snip [Empty Policy](../../../../../examples/src/main/scala/example/auth/ExampleServer.scala) { #access-token-handle-demo }

Both of the above approaches compile and are valid. Access token holds basic information about the user 
or the client who has made request.

Please go through API documentation to know more about Access Token.

## Policy Expressions

So far, we have seen that security directives can accept an authorization policy. It can however accept an expression of 
multiple authorization policies too. This could be useful to express complex authorization logic. For example:

Scala
:   @@snip [Empty Policy](../../../../../examples/src/main/scala/example/auth/ExampleServer.scala) { #policy-expressions }

Note the `|` , `&` operators which helps compose an expression. A Policy expression could be more complex than this
and can contain braces to group more expressions. For example:

```scala
val policy = policy1 | (policy2 & (policy3 | policy4)) | policy5
```

## Directive Composition

Since security directives extend from `akka.http.scaladsl.server.Directive` they give you all the
benefits of a usual directive. These benefits include being able to label and [compose higher level
directives](https://doc.akka.io/docs/akka-http/current/routing-dsl/directives/custom-directives.html#custom-directives).

With the help of directive labeling you could write a route like below:

Scala
:   @@snip [Empty Policy](../../../../../examples/src/main/scala/example/auth/ExampleServer.scala) { #directive-composition-anti-pattern }

The same can be achieved via @ref:[Policy Expressions](#policy-expressions) as shown below

Scala
:   @@snip [Empty Policy](../../../../../examples/src/main/scala/example/auth/ExampleServer.scala) { #policy-expressions-right-way } 

If you want to combine two directives ***and both of them are CSW security directives***,
we strongly recommend that you use @ref:[Policy Expressions](#policy-expressions). The reason 
for this is that, when you combine two CSW security directives, the authentication check happens twice (or multiple
times based on how many CSW security directives are combined).  Since this was meant to happen only once, it causes 
performance slowdown. You can however combine CSW security directives with other directives freely without worrying
about performance.

## Source code for above examples

* @github[Example http server](/examples/src/main/scala/example/auth/ExampleServer.scala)

