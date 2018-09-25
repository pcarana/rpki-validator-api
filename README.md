# RPKI Validator API

This is a simple API to fetch data obtained by an RPKI Relying Party (RP).

## Protecting resources

In order to protect resources, this API uses [Apache Shiro<sup>TM</sup>](https://shiro.apache.org/). By default this functionality is disabled, if this is ment to be used then follow the next steps.

The following steps are basic steps to use Apache Shiro, further reading about the framework is recommended; see more at https://shiro.apache.org/configuration.html and https://shiro.apache.org/web.html.

### Add Apache Shiro's dependencies

At `pom.xml` file, add the following dependency:

```xml
<dependencies>
...
	<dependency>
		<groupId>commons-logging</groupId>
		<artifactId>commons-logging</artifactId>
		<version>1.2</version>
	</dependency>
	<dependency>
		<groupId>org.apache.shiro</groupId>
		<artifactId>shiro-core</artifactId>
		<version>1.4.0</version>
	</dependency>
	<dependency>
		<groupId>org.apache.shiro</groupId>
		<artifactId>shiro-web</artifactId>
		<version>1.4.0</version>
	</dependency>
...
</dependencies>
```

### Add `listener` and `filter`

Now Apache Shiro must be added to the API. At `web.xml` file, add the following lines inside the `<web-app>` tag:

```xml
<listener>
	<listener-class>org.apache.shiro.web.env.EnvironmentLoaderListener</listener-class>
</listener>
<filter>
	<filter-name>ShiroFilter</filter-name>
	<filter-class>org.apache.shiro.web.servlet.ShiroFilter</filter-class>
</filter>
<filter-mapping>
	<filter-name>ShiroFilter</filter-name>
	<url-pattern>/*</url-pattern>
	<dispatcher>REQUEST</dispatcher>
	<dispatcher>FORWARD</dispatcher>
	<dispatcher>INCLUDE</dispatcher>
	<dispatcher>ERROR</dispatcher>
</filter-mapping>
```

### Configure Apache Shiro

The last step to have Apache Shiro ready to be used is to configure it (see more at [https://shiro.apache.org/configuration.html]). This configuration is expected to be at `/WEB-INF/shiro.ini`.

Here's an example of the configuration file with the following rules:
* Protected resources using HTTP Basic Authentication:
    * `/slurm/*`
* Users/passwords that the API will recognize:
     * _alvin / alvin_
     * _bob / bob_

```
[main]
sessionManager = org.apache.shiro.web.session.mgt.DefaultWebSessionManager
securityManager.sessionManager = $sessionManager
securityManager.sessionManager.sessionIdCookieEnabled = false

[users]
alvin = alvin
bob = bob

[urls]
# Restricted paths that need authentication (one path per line)
/slurm/** = authcBasic
# The rest of the resources will be accessed anonymously
/** = anon
```

Right after set this configuration file, start (or restart) the server to test it. Another example configuration file, with more comments, can be found at `samples` directory.