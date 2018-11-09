var fs = require("fs");
var path = require('path');

class CreateExportScriptPlugin {

    constructor(excluded) {
        this.excluded = excluded || [];
    }

    apply(compiler) {
        var self = this;
        compiler.hooks.environment.tap('CreateExportScriptPlugin', function() {
            var packageJson = fs.readFileSync(path.resolve(__dirname, '../../../../package.json'), 'utf8');
            packageJson = JSON.parse(packageJson);
            var exportFileContents = "";
            //This is used in the app entry point to expose all dependencies to webpack
            var importStatementsForLocalyImportedAssets = "";

            var keys = Object.getOwnPropertyNames(packageJson.dependencies);
            keys = keys.filter(function(key) {
                return self.excluded.indexOf(key) === -1;
            });

            keys.forEach(function(key, index) {
                var imprt = "import * as " + camelCase(key) + " from \"" + key + "\"\n";
                exportFileContents += imprt;
                importStatementsForLocalyImportedAssets += imprt;
            });
            exportFileContents += "\n\n";
            exportFileContents += "window.dxJsAsset = (asset) => {\n";
            exportFileContents += "\tconst exportedAssets = {\n";

            keys.forEach(function(key, index) {
                exportFileContents += "\t\t\"" + key + "\" : " + camelCase(key) + ",\n";
            });

            exportFileContents += "\t};\n";
            exportFileContents += "\treturn exportedAssets[asset];\n";
            exportFileContents += "};";

            fs.writeFileSync(path.resolve(__dirname, 'exportedAssets.js'), exportFileContents);
            fs.writeFileSync(path.resolve(__dirname, 'exportedAssetsLocal.js'), importStatementsForLocalyImportedAssets);

            function camelCase(str) {
                return str.replace(/-([a-z])/g, function (m, w) {
                    return w.toUpperCase();
                });
            }
        });
    }
}

module.exports = CreateExportScriptPlugin;