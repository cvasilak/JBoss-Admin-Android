![JBoss Admin][0]

### JBoss Admin _(for Android)_

The app will allow you to remotely administer a JBoss 7, WildFly or JBoss EAP using the server's exposed http management interface.

Features
--------

The application supports the following features:

* __Subsystem Metrics Monitoring__

    The metrics currently exposed are for Configuration, JVM, Data Sources, JMS Destinations, Transactions and Web subsystems (similar to those shown in the JBoss built-in web console).

* __Deployments Management__

    You can upload an artifact (installed on your phone) and then enable/disable it on the server.

* __Browse the management tree__

    The whole management tree is exposed for you to configure, similar to the JBoss-cli {-gui} tool provided by the server. You can easily modify attributes and execute operations. Documentation of attributes and operation parameters is easily accessed for you to refer.

Note that both operating modes (Standalone/Domain) of the server are supported. If running in "Domain" mode, you can easily switch the server you want to monitor its metrics, as well as manage deployments on each individual server-group.

Click [here][1] to watch a video demonstrating the app in "action". 

For instructions on how to setup JBoss Tools to allow connections from the app, click [here][3] for the details (thanks [Max][2]!)

The application supports both *Android 2.3.3 Gingerbread (API 10)* and *4.4 KitKat (API 19)*.

Development
-----------
The project is build using Maven and the [Android Maven Plugin](http://code.google.com/p/maven-android-plugin). Further it depends on the following maven dependencies to be installed:

* [Android Maven SDK Deployer](https://github.com/mosabua/maven-android-sdk-deployer)

  It will make your life easier when dealing with Android library deps. which either are not found (or obsolete) in Maven Central. Follow the guide in the web site and install the appropriate platform you plan to work with (2.3.3 or 4.x).

* [Drag Sort ListView](https://github.com/bauerca/drag-sort-listview)

  A nice lib that allows reordering of list elements in a TableView with support for older versions of Android. The project is not available in Maven Central so you need to install manually in your local .m2 repository.  Simple clone it and do a 'mvn install' to install it.

I would love to hear any comments of yours, so please drop me an [email][4] or better open an issue in the project's github page.

Enjoy!


[0]: http://www.cvasilak.org/images/jboss-admin-logo.png "JBoss Admin"
[1]: https://vimeo.com/110015199
[2]: https://twitter.com/#!/maxandersen
[3]: http://planet.jboss.org/post/using_jboss_admin_iphone_app_together_with_jboss_tools
[4]: mailto:cvasilak@gmail.gom?subject=JBoss-Admin(Android)
