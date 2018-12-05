# VueJS DX module

This is an experimental module and is not intended for production. Use at your own risk. No support will be provided at this point.

## Description

This module was created as a proof of concept that we can dynamically load webpack bundles under the umbrella of one central bundle without reloading dependencies multiple times. In theory there can be 
multiple connected modules sharing dependencies without having a central one, but that was out of scope for this study. 

## How it works

* There is a webpack plugin which ```CreateExportScriptPlugin``` creates a ```exportedAssetsLocal.js``` file which contains all dependencies from package.json. 
The file is imported in ```main.js``` and the purpose of this file is to allow webpack to bundle dependencies which are not explicitly used. 
_This file may not be necessary in all cases, im my case I don't use most of dependencies in package.json so I need to explicitly export them so that 
they show up in the js bundle._

* There is another webpack plugin `ExposeWebpackRequirePlugin` which modifies created vendor bundle to expose webpack's require function. It creates 
`jsDependencyMappingToWebpack.js` file which contains logic to expose require function and load assets and is loaded by filter (see `LoadJavascriptAssestFilter.java`) on page load.

## How to run

* Build and deploy this module to Jahia

* Make sure to deploy https://github.com/Jahia/js-expension (it will provide an extension bundle to load)

* Add this module to any site

* Add `jsCoreApp` from `SiteComponents`

## What to expect

* You should see two pictures, one of them will say dynamic. It is loaded via ajax and evaluated at runtime.

* Open window console and inspect the logs. You should see logs coming from js-extension module which will be using react and lodash component from core module.
 