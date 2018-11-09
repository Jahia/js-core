import Vue from 'vue';
import AppComponent from './AppComponent.vue';
//This exposes all dependencies of package.json to webpack
import * as allAssets from '../import/exportedAssetsLocal';

class Evaluator {

    evaluate(d) {
        const imp = eval(d);
        imp.sayHello();
        imp.sayBye();
        return imp;
    }
}

window.runApp = () => {
    console.log("Running app");
    fetch("/modules/js-extension/javascript/exportedBundles/bundleExport.js").then((data) => {
        data.text().then(function build(d)  {
            new Evaluator().evaluate(d);
        })
    });

    new Vue({
        render: h => h(AppComponent)
    }).$mount('#appdouble');
};