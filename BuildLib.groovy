import groovy.json.JsonSlurper


def coreBuild (cl){
    Map buildDefs = initBuildModuleDefinition();

    Map coreBuildMap = cl();
    def modulesToBuild = [:]
    def mvnHome = tool 'M3'

    stage 'Build'
    def entries = get_map_entries(coreBuildMap)
    for (int i = 0; i < entries.size(); i++) {
        String moduleName = entries[i][0]
        String moduleBranch = entries[i][1]

        modulesToBuild["ModuleName_" + moduleName] = {

            scmUrl = buildDefs.get(moduleName);
            //git branch: moduleBranch, url: scmUrl
            //mvn
            dir('module_'+moduleName) {
                echo "Checking out ${moduleName}"
                git branch: moduleBranch, url: scmUrl

                echo "Runing maven build for ${moduleName}"
                sh "${mvnHome}/bin/mvn clean install"
            }
        }
    }

    parallel modulesToBuild
}

@NonCPS
def initBuildModuleDefinition() {

    //def payload = new URL("http://mvpartisans.com/BuildModules.json").text
    //def jsonResp = new JsonSlurper().parseText(payload)

    //return (Map) jsonResp;
    Map buildDef = [
            "lfs-a": "https://github.com/mvpartisans/pipeline-as-code-demo",
            "lfs-b": "https://github.com/mvpartisans/simple-maven-project-with-tests",
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