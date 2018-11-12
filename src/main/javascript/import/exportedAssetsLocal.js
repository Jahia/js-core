import * as cssLoader from "css-loader"
import * as lodash from "lodash"
import * as underscore from "underscore"
import * as vue from "vue"
import * as vueRouter from "vue-router"
import * as vueStyleLoader from "vue-style-loader"
import * as vueTemplateCompiler from "vue-template-compiler"
import * as react from "react"

	const exportedAssets = {
		"css-loader" : cssLoader,
		"lodash" : lodash,
		"underscore" : underscore,
		"vue" : vue,
		"vue-router" : vueRouter,
		"vue-style-loader" : vueStyleLoader,
		"vue-template-compiler" : vueTemplateCompiler,
		"react" : react,
	};

 export default exportedAssets;