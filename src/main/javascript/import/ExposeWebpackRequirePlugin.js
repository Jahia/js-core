var ReplaceSource = require('webpack-sources').ReplaceSource;
var fs = require("fs");
var path = require('path');

var fcnName = Buffer.from('js-core').toString('base64').replace(/=/g, "");
//to go back: Buffer.from(b64Encoded, 'base64').toString()

class ExposeWebpackRequirePlugin {
    apply(compiler) {
        //Expose webpack require
        compiler.hooks.compilation.tap(
            'ExposeWebpackRequirePlugin',
            (compilation) => {
                compilation.hooks
                    .optimizeChunkAssets
                    .tapAsync('ExposeWebpackRequireInnerPlugin', (chunks, callback) => {
                        chunks.forEach(chunk => {
                            if (chunk.name === "jsCore") {
                                chunk.files.forEach(file => {
                                    var source = compilation.assets[file].source();
                                    //Expose webpack require
                                    var startIndex = this.requireFcnIndex(source);
                                    var replacedSource = new ReplaceSource(compilation.assets[file]);
                                    var end = startIndex + "function __webpack_require__(moduleId)".length;
                                    var replaceBy = "window." + fcnName + " = __webpack_require__; \n/******/\n";
                                    replaceBy += "/******/\tfunction __webpack_require__(moduleId) ";
                                    replacedSource.replace(startIndex, end, replaceBy);

                                    compilation.assets[file] = replacedSource;
                                });
                            }
                        });

                        callback();
                    });
            }
        );

        //Create dependency mapping
        compiler.hooks.emit.tap('ExposeWebpackRequireInnerPlugin_2', compilation => {
            compilation.chunks.forEach(chunk => {
                if (chunk.name === "jsCore") {
                    var packageJson = fs.readFileSync(path.resolve(__dirname, '../../../../package.json'), 'utf8');
                    packageJson = JSON.parse(packageJson);
                    var dependencyNames = Object.getOwnPropertyNames(packageJson.dependencies);
                    var installedDependencies = "{\n";
                    var dependencyMapping = "window.dxJsAsset = function(asset) {\n";
                    dependencyMapping += "\tvar exportedAssets = {\n";
                    Array.from(chunk._modules).forEach((module, index) => {
                        //depth 1 implies that this is entry point module
                        if (module.depth === 1) {
                            // console.log(module.depth, module.id, module.context);
                            var dependencyForContext = dependencyNames.find(name => {
                                if (module.context && module.context.indexOf("/node_modules/") !== -1) {
                                    var pathAfterNodeModules = module.context.split("/node_modules/")[1];
                                    pathAfterNodeModules = pathAfterNodeModules.split("/");
                                    return pathAfterNodeModules.find((pathPart) => {
                                        return pathPart === name;
                                    });
                                }
                                return false;
                            });

                            if (dependencyForContext) {
                                dependencyMapping += "\t\t\"" + dependencyForContext + "\" : \""  + module.id + "\",\n";
                                installedDependencies += "\"" + dependencyForContext + "\" : \"" + packageJson.dependencies[dependencyForContext] + "\",\n";
                            }
                        }
                    });
                    dependencyMapping += "\t};\n";
                    dependencyMapping += "\treturn window." + fcnName + "(exportedAssets[asset]);\n";
                    dependencyMapping += "};";

                    installedDependencies = installedDependencies.substr(0, installedDependencies.length - 2);
                    installedDependencies += "\n}";

                    fs.writeFileSync(path.resolve(__dirname, '../../resources/javascript/jsDependencyMappingToWebpack.js'), dependencyMapping);
                    fs.writeFileSync(path.resolve(__dirname, '../../resources/javascript/jsDependencyNames.json'), installedDependencies);
                }
            });
        });
    }

    requireFcnIndex(source) {
        return source.indexOf("function __webpack_require__(moduleId)");
    }
}

module.exports = ExposeWebpackRequirePlugin;

