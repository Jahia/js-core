var fs = require("fs");
var path = require('path');

class CreateExportScriptPlugin {
    apply(compiler) {
        compiler.hooks.environment.tap('CreateExportScriptPlugin', function() {
            var packageJson = fs.readFileSync(path.resolve(__dirname, '../../../../package.json'), 'utf8');
            packageJson = JSON.parse(packageJson);
            var exportFileContents = "";

            var keys = Object.getOwnPropertyNames(packageJson.dependencies);
            keys.forEach(function(key, index) {
                exportFileContents += "import * as " + camelCase(key) + " from \"" + key + "\"\n";
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

            function camelCase(str) {
                return str.replace(/-([a-z])/g, function (m, w) {
                    return w.toUpperCase();
                });
            }
        });
    }
}

module.exports = CreateExportScriptPlugin;