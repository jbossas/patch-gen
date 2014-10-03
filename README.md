# JBoss AS / Wildfly Patch Generation Tool

## Usage

Options are not optional and must exactly follow this format: `--optionname=value`. The `=` must be present when there is a value. There are no short forms for the option names.

In the following sections, substitute `patch-gen` for `java -jar patch-gen-*-shaded.jar`, or set up an alias with

    alias patch-gen='java -jar patch-gen-*-shaded.jar'

### Patch Generation
    patch-gen --applies-to-dist=~/wildfly/wildfly-8.0.0.Final --updated-dist=~/wildfly/wildfly-8.1.0.CR2 --patch-config=patch-config-wildfly-CR2-patch.xml --output-file=wildfly-8.1.0.CR2.patch.zip

`--applies-to-dist` and `--updated-dist` must point exactly at the root of the distributions (the directory containing bin, modules, domain, etc.), otherwise the tool will crash.

### Configuration Templating

#### One off
    patch-gen --create-template --one-off my-custom-patch

#### Cumulative
    patch-gen --create-template --cumulative my-custom-patch

### Getting an executable jar
You probably want to use the shaded jar, which is executable and contains all of its dependencies.

#### Build from source
    git clone https://github.com/jbossas/patch-gen.git
    mvn package

The jars will be located in the `target/` subdirectory.

#### Download from Github
    curl -LO 'https://github.com/jbossas/patch-gen/releases/download/1.0/patch-gen-1.0-shaded.jar'

## Patch Workflow

1. Run the command specified in the Configuration Templating section.
2. Edit the configuration file.
```
$EDITOR patch-config-wildfly-CR2-patch.xml
```
```xml
<?xml version='1.0' encoding='UTF-8'?>
<patch-config xmlns="urn:jboss:patch-config:1.0">
   <name>wildfly-CR2</name>
   <description>WildFly 8.1.0.CR2 patch</description>
   <cumulative  />
   <element patch-id="base-wildfly-CR2-patch">
      <cumulative name="base" />
      <description>No description available</description>
   </element>
   <generate-by-diff />
</patch-config>
```
3. Run the command specified in the Patch Generation section.
4. Optionally, edit README.txt in the zip.
```
unzip -qo patch.zip README.txt
$EDITOR README.txt
zip -qu patch.zip README.txt
```
