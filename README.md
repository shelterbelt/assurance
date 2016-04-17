[1]: https://maven.apache.org/download.cgi  "Apache Maven"
[2]: https://ant.apache.org/bindownload.cgi  "Apache Ant"
[3]: http://www.oracle.com/technetwork/articles/javase/index-jsp-138363.html "Oracle Java JDK"
[4]: http://www.apache.org/licenses/LICENSE-2.0 "Apache License 2.0"
[5]: http://digitalgeneralists.com "Digital Generalists, LLC."

# Assurance

A cross-platform application to analyze and synchronize the contents of file system directories.

## What Is Assurance

Assurance is a cross-platform, client-only application written in Java that leverages both Spring and Hibernate with a Swing-based UI.

At it's core, it is a tool to comprehensively compare and optionally synchronize the contents of two or more directories.

## License

Assurance is released under the [Apache License 2.0] [4].

See the LICENSE.txt file for the formal license specification.

## Prerequisites

The following tools need to be installed to build Assurance:

* Apache Maven 3.2.3 [download] [1]
* Apache Ant 1.9.3 [download] [2]
* Oracle Java JDK 1.8u20 [download] [3]

The Maven configuration will download and install the appropriate versions of the following tools:

* Spring Framework
* Hibernate
* JUnit
* H2
* Apache Commons
* Apple Java Extensions
* SLF4J
* Log4J

The following dependencies are required to run binary distributions of Assurance on Windows:

* Oracle Java JRE 1.8u20 [download] [3]

The Mac distributions of Assurance package the appropriate JRE with the application bundle.  Windows distributions require the appropriate JRE is installed prior to starting the application.

## Building and Packaging

Assurance uses Maven as its primary build and dependency management system.  

To build the application:

*Mac*	

	cd <project root>/assurance
	mvn clean package -Pdevelopment
	
*Windows*

	cd <project root>\assurance
	mvn clean package -Pdevelopment-windows

To run the unit tests:

*Mac*	

	cd <project root>/assurance
	mvn clean test -Pdevelopment
	
*Windows*

	cd <project root>\assurance
	mvn clean test -Pdevelopment-windows

To package the application for internal release:

*Mac*	

	cd <project root>/assurance
	mvn clean package -Pintrelease
	
*Windows*

	cd <project root>\assurance
	mvn clean package -Pintrelease-windows

To package the application for release:

*Mac*	

	cd <project root>/assurance
	mvn clean package -Prelease
	
*Windows*

	cd <project root>\assurance
	mvn clean package -Prelease-windows
	
## IDE

Assurance was developed using Spring Tool Suite 3.6.1.  Project files are included with the distribution.

## Acknowledgments

Assurance includes a modified version of the 

	com.ibatis.common.jdbc.ScriptRunner 

class from the iBATIS Apache project.

## Disclaimers and Apologies

Assurance was built and tested primarily on a Mac environment.  The Windows implementation has not been tested/vetted nearly to the degree the Mac version has.  Linux viability theoretically exists but is essentially unproven.

To the extent the Windows version was tested, it was done on Windows VMs running on a Mac with a fully configured development environment.

The build scripts and configuration files likely contain assumptions that the primary development environment is Mac.

If you are a Windows developer, I sincerely apologize if the build environment and documentation are not up to par.

*Copyright (c) 2016 [Digital Generalists, LLC.] [5]*

