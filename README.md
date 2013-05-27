# patch generation tool

### create template for one off or cumulative patch:

`sh tool.sh --create-template --one-off    my-custom-patch`
`sh tool.sh --create-template --cumulative my-custom-patch`

### edit created patch config

`vi patch-config-my-custom-patch.xml`

### compare distributions

`sh tool.sh --applies-to-dist=/path/to/old/distribution --updated-dist=/path/to/new/distribution --patch-config=patch-config-my-custom-patch.xml --output-file=my-custom-patch.zip`
