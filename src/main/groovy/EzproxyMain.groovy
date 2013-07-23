import metridoc.core.MetridocScript
import metridoc.ezproxy.EzproxyHostsTool

use(MetridocScript) {
    def ezproxyTool = includeTool(EzproxyHostsTool)
    ezproxyTool.execute()
}