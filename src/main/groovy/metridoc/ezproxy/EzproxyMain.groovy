package metridoc.ezproxy

import metridoc.core.MetridocScript
import metridoc.core.tools.MainTool
import metridoc.core.tools.ParseArgsTool

runnableTools = [hosts: EzproxyHostsTool]
use(MetridocScript) {
    includeTool(ParseArgsTool)
    if(binding.hasVariable("argsMap")) {
        if(!argsMap.params) {
            //defaults to host
            def listArgs = ["hosts"]
            listArgs.addAll args as List
            args = listArgs as String[]
        }
    }
    def ezproxyTool = includeTool(MainTool)
    ezproxyTool.execute()
}