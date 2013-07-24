package metridoc.ezproxy

import metridoc.core.MetridocScript

use(MetridocScript) {
    def ezproxyTool = includeTool(EzproxyHostsTool)
    ezproxyTool.execute()
}