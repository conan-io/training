package conan.ci.artifactory


import jenkins.plugins.http_request.HttpMode
import jenkins.plugins.http_request.ResponseContentSupplier
import net.sf.json.JSONObject

class ArtifactoryConanApi {
    def currentBuild
    conan.ci.arg.ConanArgs conanArgs
    conan.ci.arg.ArtifactoryArgs artifactoryArgs

    static ArtifactoryConanApi construct(def currentBuild) {
        ArtifactoryConanApi artifactoryConanApi = new ArtifactoryConanApi(currentBuild)
        artifactoryConanApi.conanArgs = new conan.ci.arg.ConanArgs(currentBuild)
        artifactoryConanApi.artifactoryArgs = new conan.ci.arg.ArtifactoryArgs(currentBuild)
        return artifactoryConanApi
    }

    private ArtifactoryConanApi(def currentBuild) {
        this.currentBuild = currentBuild
    }

    ArtifactoryConanPackage findConanPackage(String reference) {
        String remoteName = conanArgs.getConanRemoteProd()
        String queryString = "remotes=${remoteName}&reference=*${reference}*"
        File file = new File(conan.ci.constant.DemoConstant.ART_CONAN_CONSUMERS_FILENAME)
        search(queryString, file)
        JSONObject json = this.currentBuild.readJSON(file: file.path) as JSONObject
        List<JSONObject> results = json['results'] as List<JSONObject>

        ArtifactoryConanPackage pkg = results
                .collect { JSONObject resultJson -> ArtifactoryFileInfo.construct(currentBuild, resultJson) }
                .collect { ArtifactoryFileInfo info -> ArtifactoryConanPackage.construct(currentBuild, info.binaryUri) }
                .first()

        return pkg ?: null
    }

    List<ArtifactoryConanPackage> findConanConsumers(String packageReference) {
        String remote = conanArgs.getConanRemoteProd()
        String queryString = "repos=${remote}&conan.requires=*${packageReference}*"
        File file = new File(conan.ci.constant.DemoConstant.ART_CONAN_CONSUMERS_FILENAME)

        search(queryString, file)
        JSONObject json = this.currentBuild.readJSON(file: file.path) as JSONObject
        List<JSONObject> results = json['results'] as List<JSONObject>

        List<ArtifactoryConanPackage> consumers = results
                .collect { JSONObject resultJson -> ArtifactoryFileInfo.construct(currentBuild, resultJson) }
                .collect { ArtifactoryFileInfo info -> ArtifactoryConanPackage.construct(currentBuild, info.binaryUri) }

        return consumers
    }

    void downloadConanMetadataFiles(String rRepo, List<ArtifactoryConanPackage> conanPackages, String fileName) {
        conanPackages.each {
            downloadConanMetadataFile(rRepo, it, fileName)
        }
    }

    void downloadConanMetadataFile(String rRepo, ArtifactoryConanPackage conanPackage, String fileName) {
        ResponseContentSupplier response = downloadFileSafe(rRepo, conanPackage.binaryUri, fileName)
        if (response.status < 400) {
            File targetFile = new File(conanPackage.binaryUri, fileName)
            currentBuild.writeFile(text: response.content, file: targetFile.path) as JSONObject
        }
    }

    void search(String queryString, File file) {
        String apiSearch = conan.ci.constant.DemoConstant.ART_API_SEARCH_PATH
        String url = "${artifactoryArgs.url}/${apiSearch}/prop?${queryString}"
        download(url, file)
    }

    void downloadWithFullUrl(String fullUrl, File file) {
        download(fullUrl, file)
    }

    ResponseContentSupplier downloadFileSafe(String rRepo, String remotePath, String remoteFileName = "") {
        String url = "${artifactoryArgs.url}/${rRepo}/${remotePath}/${remoteFileName}"
        ResponseContentSupplier response = downloadSafe(url)
        return response
    }

    void uploadFile(String rRepo, String remotePath, File file, String remoteFileName = "") {
        String rFileName = remoteFileName ?: file.name
        String url = "${artifactoryArgs.url}/${rRepo}/${remotePath}/${rFileName}"
        upload(url, file)
    }

    void deleteFile(String rRepo, String remotePath, String remoteFileName = "") {
        String url = "${artifactoryArgs.url}/${rRepo}/${remotePath}/${remoteFileName}"
        delete(url)
    }

    void download(String url, File file) {
        conan.ci.util.DemoLogger.debug(currentBuild, "url = ${url} file=${file.path}")
        this.currentBuild.httpRequest(
                httpMode: HttpMode.GET,
                authentication: 'DEMO_ART_CREDS',
                url: url,
                customHeaders: [[name: 'X-Result-Detail', value: 'properties']],
                validResponseCodes: "200:399",
                quiet: true,
                outputFile: file.path
        )
    }

    ResponseContentSupplier downloadSafe(String url) {
        conan.ci.util.DemoLogger.debug(currentBuild, "url = ${url}")
        ResponseContentSupplier response = this.currentBuild.httpRequest(
                httpMode: HttpMode.GET,
                authentication: 'DEMO_ART_CREDS',
                url: url,
                customHeaders: [[name: 'X-Result-Detail', value: 'properties']],
                validResponseCodes: "200:404",
                quiet: true
        ) as ResponseContentSupplier
        return response
    }

    void upload(String url, File file) {
        conan.ci.util.DemoLogger.debug(currentBuild, "url = ${url} , file = ${file}")
        this.currentBuild.httpRequest(
                httpMode: HttpMode.PUT,
                authentication: 'DEMO_ART_CREDS',
                url: url,
                wrapAsMultipart: false,
                validResponseCodes: "200:399",
                uploadFile: file.path,
                quiet: true
        )
    }

    void delete(String url) {
        conan.ci.util.DemoLogger.debug(currentBuild, "url = ${url}")
        this.currentBuild.httpRequest(
                httpMode: HttpMode.DELETE,
                authentication: 'DEMO_ART_CREDS',
                url: url,
                validResponseCodes: "200:404",
        )
    }
}

