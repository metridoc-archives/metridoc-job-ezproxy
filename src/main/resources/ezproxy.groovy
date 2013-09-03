import metridoc.core.MetridocScript
import metridoc.ezproxy.EzproxyHostsTool

use(MetridocScript) {
    includeTool(EzproxyHostsTool).execute()
}

