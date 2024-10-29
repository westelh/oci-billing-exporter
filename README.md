# Prometheus exporter for cost reports on oracle cloud

This application exports the resource usage and its cost on Oracle Cloud Infrastructure.

## Run
```
$ docker run ghcr.io/westelh/oci-billing-exporter:v1.0.0 run --help
Usage: oci-billing-exporter run [<options>]

Options:
  --config=<path>
  -h, --help       Show this message and exit
```

--config is required.

## Config
Configuration in yaml have to contain these values at least.
```
# xxxxxxxxxx is a target tenancy's OCID
target: xxxxxxxxxx
```

You can get the jsonschema by hitting a sub command,
``` docker run ghcr.io/westelh/oci-billing-exporter:latest dump-config-schema > schema.json ```
