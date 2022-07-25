/**
 * This adapts the VSCode api used by AppLand Vue components to the JS bridge created by the JCEF-based editor.
 */
export default {
    "postMessage": function (data) {
        if (data && data.command === 'ready') {
            // this is a workaround with the IntelliJ JCEF bridge to notify the plugin that the app is ready
            console.log("intellij-plugin-ready")
            return
        }

        if (window.AppLand) {
            // window.AppLand is registered by the JCEF-based editor
            window.AppLand.postMessage(JSON.stringify({
                type: data.command,
                ...data
            }))
        } else {
            window.postMessage({
                type: data.command,
                ...data
            })
        }
    }
}