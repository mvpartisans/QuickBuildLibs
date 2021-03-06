import groovy.json.JsonSlurper


def coreBuild (cl){
    Map buildDefs = initBuildModuleDefinition();

    Map coreBuildMap = cl();

    def entries = get_map_entries(coreBuildMap)
    for (int i = 0; i < entries.size(); i++) {
        String moduleName = entries[i][0]
        String moduleBranch = entries[i][1]

        scmUrl = buildDefs.get(moduleName);
        //git branch: 'May-Release', url: 'https://github.com/mvpartisans/pipeline-as-code-demo/'

        dir('dir_'+moduleName) {
            git branch: moduleBranch, url: scmUrl
        }

        //mvn
    }

}

@NonCPS
def initBuildModuleDefinition() {

    //def payload = new URL("http://mvpartisans.com/BuildModules.json").text
    //def jsonResp = new JsonSlurper().parseText(payload)

    //return (Map) jsonResp;
    Map buildDef = [
            "lfs-a": "https://github.com/mvpartisans/pipeline-as-code-demo",
            "lfs-b": "https://github.com/mvpartisans/pipeline-as-code-demo",
            "lfs-c": "https://github.com/kishorebhatia/pipeline-as-code-demo",
            "lfs-d": "https://github.com/kishorebhatia/pipeline-as-code-demo"
    ]

    return buildDef
}

@NonCPS
List<List<Object>> get_map_entries(map) {
    map.collect { k, v -> [k, v] }
}

return this;

