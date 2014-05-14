# patch generation tool

### Steps creating a patch

1) `git clone https://github.com/emuckenhuber/patch-gen`

2) `alias patch-gen=/path/to/patch-gen/tool/tool.sh`

3) `cd /your/home/patches`

4) edit patch-config-wildfly-CR2-patch.xml

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

5) `patch-gen --applies-to-dist=/your/home/Downloads/wildfly/wildfly-8.0.0.Final --updated-dist=/your/home/Downloads/wildfly/wildfly-8.1.0.CR2 --patch-config=patch-config-wildfly-CR2-patch.xml --output-file=wildfly-8.1.0.CR2.patch.zip`

6) maybe edit README.txt in the wildfly-8.1.0.CR2.patch.zip 



### create template for one off or cumulative patch:

`sh tool.sh --create-template --one-off    my-custom-patch`

`sh tool.sh --create-template --cumulative my-custom-patch`

### compare distributions

`sh tool.sh --applies-to-dist=/path/to/old/distribution --updated-dist=/path/to/new/distribution --patch-config=patch-config-my-custom-patch.xml --output-file=my-custom-patch.zip`
