# Run Groups

This is the source code for the austinrungroups.com website.

## Setting up the environment

You will need [babashka](https://babashka.org/) and [node](https://nodejs.org/en) installed. Then running `npm i` will install the dependencies.

## Building the website

```bash
bb download-data
bb build-content
bb build-styles
bb inline-css
```

## Deploying

The website it deployed to Cloudflare Pages, you need access to the `austinrungroups` project. Once you have that, you can deploy with:

```bash
bb deploy
```
