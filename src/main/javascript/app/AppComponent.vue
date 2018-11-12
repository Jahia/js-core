<template>
    <div v-bind:style="styles">
       <h1>Experimental app with DX, VueJS</h1>
       <p>{{ message }}</p>
       <p>{{ underscore }}</p>
   </div>
</template>
<script>
    import _ from "lodash";
    import { size } from 'underscore';
   export default {
        data: function() {
            return {
                message: _.toLower("Enjoy the View (pun intended)"),
                underscore: size([1, 2, 3, 4]),
                styles: {
                    display: "flex",
                    alignContent: "center",
                    flexDirection: "column",
                    color: "#fefefe",
                    width: "100%",
                    height: "250px",
                    backgroundImage: "url('/modules/js-core/img/snow.jpg')",
                    backgroundPosition: "center",
                    backgroundPepeat: "no-repeat",
                    backgroundSize: "cover",
                    justifyContent: "center",
                    alignItems: "center"
                }
            }
        }
   }
</script>