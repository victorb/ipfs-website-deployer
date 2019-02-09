# ipfs-website-deployer

A service that deploys websites over IPFS, hosted in a IPFS-Cluster.

## How it works

ipfs-website-deployer is a backend service that connects to your GitHub repositories,
automatically builds changes and publishes them over IPFS via ipfs-cluster.

Currently a deployment of ipfs-website-deployer only supports managing DNS via
one DNSimple account and managing IPFS pins via one ipfs cluster, but in the
future, one deployment might support more accounts. We'll see.

When you change a branch or make a PR, ipfs-website-deployer receives the event
and proceeds to build the change. Once the build is done, it sets a Commit Status
with the status of the build, and a preview of the website if it's successful.

If the `master` branch in a project is changed, it also does the DNS change
to point to the new website.

In the case of a new website being created, ipfs-website-deployer also creates
the TXT ALIAS record to a public gateway, and creates TLS certificates for the
website.

## Secrets

The following values should be kept as environment variables:

```
BUILD_DIR= # Where to do builds
IPFS_API= # HTTP address to the IPFS daemon
GITHUB_APP_ID= # App ID of the GitHub Application
PRIVATE_KEY_PATH= # Path to private key for the GitHub Application
DNSIMPLE_TOKEN= # The token for DNSimple access
DEFAULT_LANDING_PAGE= # Default dnslink record
CLUSTER_API= # HTTP address to the ipfs-cluster service daemon
CLUSTER_USERNAME= # Username of IPFS cluster requests
CLUSTER_PASSWORD= # Password of IPFS cluster requests

```

## Usage

Makefile should build directory

Currently supports:

- node/npm

Needs to be integrated with:

- GitHub
  - As an GitHub application
- DNSimple
- IPFS-Cluster
- Rollbar

Supports:

- Deploy to base32 subdomain
- Deploy to base32 IPNS subdomain

Download of IPNS keys if needned

Where does secrets come from?

When the application is installed, the repository owner gets an encryption
key they need to use for encrypting the application

## Architecture

The architecture of the application should allow a flexible addition of new parts.

```
src/
  builders/
    hugo/
    npm/
  notifiers/
    github/
  dns/
    dnsimple
  pinning/
    ipfs-cluster
  post-deploy/
    rollbar
```

## Deploying

- Build server `lein ring uberjar`
- Copy into infra `cp target/ipfs-website-deployer-0.1.0-SNAPSHOT-standalone.jar infra`
- Run `terraform apply`

## TODO

- [ ] Have a webhook secret
- [ ] More unit tests
- [ ] Automatic HTTPS with Cloudflare
- [ ] Support ipfs-cluster with basic auth
- [X] Application ID should be env var
- [X] DNSimple token should be env var
- [ ] docker api client doesn't work without docker daemon
      having access to the online registry

## License

Copyright © 2018 FIXME
 
Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
