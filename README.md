# JBoss AS / Wildfly Patch Generation Tool

## Usage

Options are not optional and must exactly follow this format: `--optionname=value`. The `=` must be present when there is a value. There are no short forms for the option names.

In the following sections, substitute `patch-gen` for `java -jar patch-gen-*-shaded.jar`, or set up an alias with

    alias patch-gen='java -jar patch-gen-*-shaded.jar'

## Patch Generation
    patch-gen --applies-to-dist=~/wildfly/wildfly-8.0.0.Final --updated-dist=~/wildfly/wildfly-8.0.1.Final --patch-config=wildfly-8.0.1.Final-patch.xml --output-file=wildfly-8.0.1.Final.patch.zip

`--applies-to-dist` and `--updated-dist` must point exactly at the root of the distributions (the directory containing bin, modules, domain, etc.), otherwise the tool will crash.

### Generation of patches containing multiple CPs

    patch-gen --applies-to-dist=~/wildfly/wildfly-8.0.1.Final --updated-dist=~/wildfly/wildfly-8.0.2.Final --patch-config=wildfly-8.0.2.Final-patch.xml --output-file=wildfly-8.0.2.Final.patch.zip --combine-with=wildfly-8.0.1.Final.patch.zip

where wildfly-8.0.1.Final.patch.zip is the last produced CP (in this example generated in the section above) for wildfly-8.0.0.Final.
The generated patch wildfly-8.0.2.Final.patch.zip can be applied to wildfly-8.0.1.Final as well as to wildfly-8.0.0.Final.
No matter to which version it is applied, the resulting patched version will be wildfly-8.0.2.Final.
There is no restriction on the number of CPs included into a single patch file.

### Configuration Templating

#### One off
    patch-gen --create-template --one-off my-custom-patch

#### Cumulative
    patch-gen --create-template --cumulative my-custom-patch

### Getting an executable jar
You probably want to use the shaded jar, which is executable and contains all of its dependencies.

#### Build from source
    git clone https://github.com/jbossas/patch-gen.git
    cd patch-gen/
    mvn package

The jars will be located in the `target/` subdirectory.

#### Download from Github
    curl -LO 'https://github.com/jbossas/patch-gen/releases/download/2.0.0.Final/patch-gen-2.0.0.Final-shaded.jar'

## Patch Workflow

1. Run the command specified in the Configuration Templating section.
2. Edit the configuration file.
```
$EDITOR patch-config-wildfly-CR2-patch.xml
```
```xml
<?xml version='1.0' encoding='UTF-8'?>
<patch-config xmlns="urn:jboss:patch-config:1.0">
   <name>wildfly-8.0.2.Final.patch</name>
   <description>WildFly 8.0.2.Final patch</description>
   <cumulative  />
   <element patch-id="base-wildfly-8.0.2.Final-patch">
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