import metridoc.core.MetridocScript
import metridoc.core.tools.MainTool
import metridoc.ezproxy.EzproxyHostsTool

use(MetridocScript) {
    includeTool(runnableTools: [processEzHosts: EzproxyHostsTool], MainTool).execute()
}

