import groovy.json.JsonSlurper


def coreBuild(cl) {
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
            dir('module_' + moduleName) {
                echo "Checking out ${moduleName}"
                git branch: moduleBranch, url: scmUrl

                echo "Runing maven build for ${moduleName}"
                sh "${mvnHome}/bin/mvn clean install"
                //stash incude:"*.jar, *.war" name:'${moduleName}-buildArtifact'
            }
        }
    }

    parallel modulesToBuild
}

def deploy(host) {
    Map hostsDef = initHostsDefinition();
    println hostsDef.get(host);
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
def initHostsDefinition() {

    //def payload = new URL("http://mvpartisans.com/BuildModules.json").text
    //def jsonResp = new JsonSlurper().parseText(payload)

    //return (Map) jsonResp;
    Map hostsDef = [
            "host1": "np1.corelogic",
            "host2": "np2.corelogic",
            "host3": "np2.corelogic",
    ]

    return hostsDef
}

@NonCPS
List<List<Object>> get_map_entries(map) {
    map.collect { k, v -> [k, v] }
}


def getGav() {

    String pom = readFile('pom.xml')
    def vMatcher = (pom =~ '<version>(.+)</version>')
    version = vMatcher ? vMatcher[0][1] : null
    println version

    def gMatcher = (pom =~ '<groupId>(.+)</groupId>')
    group = gMatcher ? gMatcher[0][1] : null
    println group

    def aMatcher = (pom =~ '<artifactId>(.+)</artifactId>')
    artifact = aMatcher ? aMatcher[0][1] : null
    println artifact

    //[group: group, artifact: artifact, version: version]
    "${group}:${artifact}:${version}"
}


def staticAnalysis() {
    step([$class: 'ArtifactArchiver', artifacts: 'gameoflife-web/target/*.war'])
    step([$class: 'WarningsPublisher', consoleParsers: [[parserName: 'Maven']]])
    step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
    step([$class: 'JavadocArchiver', javadocDir: 'gameoflife-core/target/site/apidocs/'])

    step([$class: 'hudson.plugins.checkstyle.CheckStylePublisher', pattern: '**/target/checkstyle-result.xml'])
    step([$class: 'FindBugsPublisher', pattern: '**/findbugsXml.xml'])
    step([$class: 'AnalysisPublisher'])
}


def gitNotifier() {
    stage 'Notify'
    step([$class: 'GitHubSetCommitStatusBuilder', statusMessage: [content: 'pending']])
    try {
        sh "${mvnHome}/bin/mvn clean install"
        step([$class: 'GitHubSetCommitStatusBuilder', statusMessage: [content: 'success']])
    } catch (Exception e) {
        step([$class: 'GitHubSetCommitStatusBuilder', statusMessage: [content: 'failure']])
    }
}

def buildWithGradle(goals){
    //clean build test publish
    sh """
        ./gradlew --gradle-user-home=${pwd()}/.gradle ${goals}
        """
}

def paralellTests() {
    parallel(Functional: {
        //runTests(30)
        node ('windows'){
            unstash 'build-artifacts'
            deploy('func-test');
            runCapybara('func-test');
        }
    }, Peformance: {
        runTests(20)
    })
}

def runTests(duration) {
    node {
        sh "sleep ${duration}"
    }
}

return this;