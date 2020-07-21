# Using Defensics API Server

## Overview

The Defensics API Server allows the Defensics Jenkins plugin to send REST 
requests to Defensics for configuring and running fuzz tests and getting their 
results.

More information on using Defensics can be found from **Help > Documentation > 
User Guide** in Defensics UI.

## Starting the API Server

Starting Defensics does not start the API Server. The basic command to start 
the API Server is:

`java -jar boot.jar --api-server HOST:PORT`

Where:

- **HOST** is the API server's external address. **HOST** is optional, and 
https://127.0.0.1 will be used if it's left out. The **HOST** is needed to make 
URLs provided by the API Server work if the Jenkins job is running on a 
different machine than the API Server. URLs beginning with https://127.0.0.1 
will only work if Jenkins and Defensics API Server are running on the same 
machine.
- **PORT** is the port you want the API Server to run in.

When configuring the Defensics instance in [Jenkins' global 
configuration](user-guide.md#global-configuration), use for example, 
`https://HOST:PORT`.

When you start the API Server for the first time, an API token is generated and 
printed in the console output.

## Additional options

### Generate an API token

To connect to the API Server, the caller needs to set an API token in the 
Authorization header of each request. The Defensics Jenkins plugin handles this 
as long as the API token has been configured for the Defensics instance.

An API token is automatically generated and printed to the console output on the 
first startup of the API Server. This flag can be used to create a new token. 
The API Server will persist the API token across startups. Generating a new 
token will erase the previous one.

To generate an API token, add one of the following to your API Server startup 
command:

`--api-generate-token` 

Generates a token and prints it in the console output.

`--api-generate-token --api-token-file TOKEN_FILE`

Generates a token and saves it into TOKEN_FILE without printing it in the 
console output.

### Provide an API token

You can provide a user-created API token with option:

`--api-token-file TOKEN_FILE`

The token file should be a one-liner, containing only the token. The token 
length is restricted to 64 characters. If the file format matches, the API 
Server stores the token.

### Provide a TLS certificate

You can provide an existing TLS certificate with options:

`--api-tls-certificate-file CERTIFICATE-PEM-FILE --api-tls-key-file KEY-PEM-FILE`

Both files should be PEM files. Supported private key formats are RSA keys in 
PKCS#1 or PKCS#8 PEM formats.
Both arguments should be given at the same time; the API Server checks that the 
items match. The API Server uses the certificate and key file from their 
original location instead of copying them, so PKI files have to be specified on 
each API Server startup.

If you start the API Server without the above certificate options, it will 
generate its own generated PKI files. It generates CA and TLS certificates and 
keys and stores them into the filesystem. After the certificates are generated, 
the API Server shows the path where they can be copied to be used in Defensics 
Jenkins plugin. If the API Server has already generated certificates, it will 
use the same ones on subsequent startups unless certificate file options are 
provided.

### Enable script execution

Executing scripts included in test configuration files is disabled by default 
when running tests via the API Server. If your tests use external 
instrumentation and you want them to run, add the following option to the API 
Server startup command:

`--enable-script-execution`

Be careful, as this opens up the possibility to run all kinds of scripts, 
including malicious ones.