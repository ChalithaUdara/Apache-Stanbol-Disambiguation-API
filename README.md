# Apache Stanbol Entity Disambiguation

This project provides entity disambiguation capability for
entities returned from Stanbol entity linking engines

This projects consists of two main components
1. Disambiguation Server
2. Disambiguation engine for Apache Stanbol
	
## Disambiguation Server

Disambiguation server provides REST services for entity disambiguation.
This server is based on the project **"Aidalight, High-throughput accurate NED system"**.
(https://code.google.com/p/aidalight/)

## Disambiguation Engine for Apache Stanbol

Disambiguation engine acts as a client for using services from Disambiguation
REST server. Client engine uses text annotations and entity annotations detected by
Apache Stanbol enhancement engines and disambiguates entity annotations for each
text annotation. 

## Software systems used

Aidalight - Licensed under Apache License, Version 2.0

