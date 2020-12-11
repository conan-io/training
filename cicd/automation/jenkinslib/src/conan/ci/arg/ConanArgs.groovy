package conan.ci.arg

import conan.ci.conan.Remote
import conan.ci.constant.DemoConstant
import conan.ci.credentials.GitCredentials

class ConanArgs {
    def currentBuild
    def env

    ConanArgs(def currentBuild) {
        this.currentBuild = currentBuild
        this.env = currentBuild.env
    }

    String getInspectAttributeCommand(String attribute, String conanfileDir = "") {
        List<String> cliArgs = ["conan inspect"]
        cliArgs.add("--raw ${attribute}")
        cliArgs.add(conanfileDirCmd(conanfileDir))
        return cliArgs.join(" ")
    }

    String getConfigInstallCommand() {
        List<String> cliArgs = ["conan config install"]
        if (urlCmd()) {
            cliArgs.add(urlCmd())
        }
        if (sourceFolderCmd()) {
            cliArgs.add(sourceFolderCmd())
        }
        if (targetFolderCmd()) {
            cliArgs.add(targetFolderCmd())
        }
        if (typeCmd()) {
            cliArgs.add(typeCmd())
        }
        if (verifySslCmd()) {
            cliArgs.add(verifySslCmd())
        }
        cliArgs.addAll(gitArgs())
        return cliArgs.join(" ")
    }

    String getCreateCommand(String conanfileDir = "") {
        List<String> cliArgs = ["conan create"]
        cliArgs.add(conanfileDirCmd(conanfileDir))
        if (userChannelCmd()) {
            cliArgs.add(userChannelCmd())
        }
        cliArgs.addAll(profilesCmd())
        cliArgs.addAll(settingsCmd())
        cliArgs.addAll(optionsCmd())
        cliArgs.addAll(envsCmd())
        cliArgs.addAll(createFlagsCmd())
        if (lockfileCmd()) {
            cliArgs.add(lockfileCmd())
        }
        return cliArgs.join(" ")
    }

    String getInstallCommand(String conanfileDir = "") {
        List<String> cliArgs = ["conan install"]
        cliArgs.add(conanfileDirCmd(conanfileDir))
        if (userChannelCmd()) {
            cliArgs.add(userChannelCmd())
        }
        cliArgs.addAll(profilesCmd())
        cliArgs.addAll(settingsCmd())
        cliArgs.addAll(optionsCmd())
        cliArgs.addAll(envsCmd())
        cliArgs.addAll(createFlagsCmd())
        cliArgs.add(lockfileCmd())
        return cliArgs.join(" ")
    }

    String getGraphLockCommand(String conanfileDirOrReference = "") {
        List<String> cliArgs = ["conan graph lock"]
        cliArgs.add(conanfileDirCmd(conanfileDirOrReference))
        cliArgs.addAll(profilesCmd())
        cliArgs.addAll(settingsCmd())
        cliArgs.addAll(optionsCmd())
        cliArgs.addAll(envsCmd())
        cliArgs.addAll(createFlagsCmd())
        return cliArgs.join(" ")
    }

    String getGraphBuildOrderCommand(String buildOrderFile = "", String lockfile = "") {
        List<String> cliArgs = ["conan graph build-order --build missing"]
        cliArgs.add(buildOrderFileCmd(buildOrderFile))
        cliArgs.add(lockfile ?: this.lockfile)
        return cliArgs.join(" ")
    }

    String getUploadCommand(String uploadPattern = "*") {
        List<String> cliArgs = ["conan upload"]
        if (uploadPattern) {
            cliArgs.add(uploadPatternCmd(uploadPattern))
        }
        if (conanRemoteUpload) {
            cliArgs.add(conanRemoteUploadCmd())
        } else {
            throw new Exception("DEMO_CONAN_REMOTE_UPLOAD must be set")
        }
        cliArgs.add("--all --force --confirm")
        return cliArgs.join(" ")
    }

    String getInfoCommand(String conanfileDir = "") {
        List<String> cliArgs = ["conan info"]
        cliArgs.add(conanfileDirCmd(conanfileDir))
        cliArgs.add("--json ${DemoConstant.CONAN_INFO_FILENAME}")
        return cliArgs.join(" ")
    }

    List<String> getUserCommands() {
        List<Remote> conanRemotes = conanRemotes
        return conanRemotes.collect { Remote remote -> getUserCommand(remote) }
    }

    String getUserCommand(Remote remote) {
        List<String> cliArgs = ["conan user"]
        cliArgs.addAll(remote.getUserCommandArgs())
        return cliArgs.join(" ")
    }

    List<String> getRemoteAddCommands() {
        List<Remote> conanRemotes = conanRemotes
        return conanRemotes.collect { Remote remote -> getRemoteAddCommand(remote) }
    }

    String getRemoteAddCommand(Remote remote) {
        List<String> cliArgs = ["conan remote add"]
        cliArgs.addAll(remote.remoteAddCommandArgs)
        return cliArgs.join(" ")
    }

    String getConanfileDir() {
        return env["DEMO_CONAN_CONANFILE_DIR"] ?: "."
    }

    String conanfileDirCmd(String conanfileDir = "") {
        if (conanfileDir == "") {
            return this.conanfileDir
        } else {
            return conanfileDir
        }
    }

    String getUploadPattern() {
        return env["DEMO_CONAN_UPLOAD_PATTERN"] ?: ""
    }

    String uploadPatternCmd(String uploadPattern = "") {
        if (uploadPattern == "") {
            return this.uploadPattern
        } else {
            return uploadPattern
        }
    }

    String getConanUser() {
        return env["DEMO_CONAN_USER"] ?: ""
    }

    String conanUserCmd() {
        return conanUser
    }

    String getConanChannel() {
        return env["DEMO_CONAN_CHANNEL"] ?: ""
    }

    String conanChannelCmd() {
        return conanChannel
    }

    String userChannelCmd() {
        if (conanUserCmd() && conanChannelCmd()) {
            return "${conanUserCmd()}/${conanChannelCmd()}"
        } else {
            return ""
        }
    }

    String getBuildOrderFile() {
        return env["DEMO_CONAN_BUILDORDER_FILE"] ?: DemoConstant.CONAN_BUILDORDER_FILENAME
    }

    String buildOrderFileCmd(String buildOrderFile = "") {
        if (buildOrderFile == "") {
            if (this.buildOrderFile == "") {
                return this.buildOrderFile
            } else {
                return "--json ${this.buildOrderFile}"
            }
        } else {
            return "--json ${buildOrderFile}"
        }
    }

    String getLockfile() {
        return env["DEMO_CONAN_LOCKFILE"] ?: ""
    }

    String lockfileCmd(String lockfile = "") {
        if (lockfile == "") {
            if (this.lockfile == "") {
                return this.lockfile
            } else {
                return "--lockfile ${this.lockfile}"
            }
        } else {
            return "--lockfile ${lockfile}"
        }
    }

    String getCreateFlags() {
        return env["DEMO_CONAN_CREATE_FLAGS"] ?: ""
    }

    List<String> createFlagsCmd() {
        if (createFlags == "") {
            return []
        } else {
            return createFlags.split(',')?.collect { "${it}".toString() }
        }
    }

    String getSourceFolder() {
        return env["DEMO_CONAN_CONFIG_SOURCE_FOLDER"] ?: ""
    }

    String sourceFolderCmd() {
        if (sourceFolder == "") {
            return sourceFolder
        } else {
            return "--target-folder ${sourceFolder}"
        }
    }

    String getTargetFolder() {
        return env["DEMO_CONAN_CONFIG_TARGET_FOLDER"] ?: ""
    }

    String targetFolderCmd() {
        if (targetFolder == "") {
            return targetFolder
        } else {
            return "--target-folder ${targetFolder}"
        }
    }

    String getType() {
        return env["DEMO_CONAN_CONFIG_TYPE"] ?: ""
    }

    String typeCmd() {
        if (type == "") {
            return type
        } else {
            return "--type ${type}"
        }
    }

    String getVerifySsl() {
        return env["DEMO_CONAN_CONFIG_VERIFY_SSL"] ?: ""
    }

    String verifySslCmd() {
        if (verifySsl) {
            return "--verify-ssl"
        } else {
            return ""
        }
    }

    String getGitArgs() {
        return env["DEMO_CONAN_CONFIG_ARGS"] ?: ""
    }

    List<String> gitArgs() {
        if (gitArgs == "") {
            return []
        } else {
            return gitArgs.split(',')?.collect { "${it}".toString() }
        }
    }

    String getUrl() {
        return env["DEMO_CONAN_CONFIG_URL"] ?: ""
    }

    String getUseCreds() {
        return env["DEMO_CONAN_CONFIG_USE_CREDS"] ?: "True"
    }

    String urlCmd() {
        if (url == "") {
            throw new Exception("DEMO_CONAN_CONFIG_URL must be set")
        } else {
            if (useCreds) {
                return url.replace("://", "://${gitCredentialVars()}@")
            } else {
                return url
            }
        }
    }

    String getProfiles() {
        return env["DEMO_CONAN_PROFILES"] ?: ""
    }

    List<String> profilesCmd() {
        if (profiles == "") {
            return []
        } else {
            return profiles.split(',')?.collect { "--profile ${it}".toString() }
        }
    }

    String getOptions() {
        return env["DEMO_CONAN_OPTIONS"] ?: ""
    }

    List<String> optionsCmd() {
        if (options == "") {
            return []
        } else {
            return options.split(',').collect { "--option ${it}".toString() }
        }
    }

    String getSettings() {
        return env["DEMO_CONAN_SETTINGS"] ?: ""
    }

    List<String> settingsCmd() {
        if (settings == "") {
            return []
        } else {
            return settings.split(',').collect { "--setting ${it}".toString() }
        }
    }

    String getEnvs() {
        return env["DEMO_CONAN_ENV_VARS"] ?: ""
    }

    List<String> envsCmd() {
        if (envs == "") {
            return []
        } else {
            return envs.split(',').collect { "--env ${it}".toString() }
        }

    }

    String gitCredentialVars() {
        GitCredentials gitCredentials = new GitCredentials(env)
        return gitCredentials.gitCredentials
    }

    String getConanRemotesStrings() {
        // DEMO_CONAN_REMOTES is A comma separated list of remotes
        // Each "remote" defined with key=value pair, separated by semicolon
        // Example...   name=bincrafters;url=url;index=0;credentialName=cred,verifySsl=True,<another_remote....>
        return env["DEMO_CONAN_REMOTES"] ?: ""
    }

    List<Remote> getConanRemotes() {
        if (conanRemotesStrings == "") {
            return []
        } else {
            List<Remote> remotes = []
            conanRemotesStrings
                    .split(',')
                    .eachWithIndex { String remoteStringDefinition, Integer index ->
                        remotes.add(new Remote(currentBuild, remoteStringDefinition, index.toString()))
                    }
            return remotes
        }
    }

    String getConanRemoteUpload() {
        // Just a string of the name of the remote to upload to
        return env["DEMO_CONAN_REMOTE_UPLOAD"] ?: ""
    }

    String conanRemoteUploadCmd() {
        return "-r ${conanRemoteUpload}"
    }

    String getConanRemoteTemp() {
        // Just a string of the name of the "temp" remote to use for testing new revisions
        return env["DEMO_CONAN_REMOTE_TEMP"] ?: ""
    }

    String getConanRemoteProd() {
        // Just a string of the name of the "local" remote to use for resolving stable packages
        return env["DEMO_CONAN_REMOTE_PROD"] ?: ""
    }


}
