This patch can be applied using the Patch Management with EAP 6.2+

Using the CLI:

# bin/jboss-cli.sh  or bin\jboss-cli.bat
[standalone@localhost:9999 /] patch apply /path/to/downloaded-patch.zip
[standalone@localhost:9999 /] shutdown --restart=true

Further information:
https://access.redhat.com/site/solutions/625683
