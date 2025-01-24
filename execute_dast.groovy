// execute in the main host this will trigger building new vm for automation

/*
    # script to provision vm in azure or digitalocean 
*/

// this is downstream job 
// system must be linux only 
node {


    /*
    // this replace the ansible less techno 
    stage("installation") {
        def currentDate = new Date()
        def formattedDate = currentDate.format('dd_MM_yyyy')
        // update & upgrade 
        sh "apt-get update -y && apt-get upgrade -y"
        sh "apt-get install openjfx"
        // required by java 
        sh "apt install openjdk-11-jdk"
        // install chrome 
        sh "wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb"
        sh "apt-get install -y libxss1 libappindicator3-1 libindicator7 fonts-liberation libdbus-1-3"
        sh "dpkg -i google-chrome-stable_current_amd64.deb"
        // install zap
        sh "wget https://github.com/zaproxy/zaproxy/releases/download/v2.14.0/ZAP_2.14.0_Linux.tar.gz"
        sh "tar -xvf ZAP_2.14.0_Linux.tar.gz"
        sh "mv ZAP_2.14.0 /opt/zaproxy"
        sh "ln -s /opt/zaproxy/zap.sh /usr/local/bin/zap.sh"
    }
    */
    stage ("download the api definition yaml") {
        sh "wget https://dev-itona.xyz/v3/api-docs?group=cothings"
    }


    // repo contain the context and automation framework tasks 
    stage("checkout") {
        checkout scmGit(
        branches: [[name: "main"]],
        userRemoteConfigs: [[credentialsId: 'gitee_creds',
                                url: "${GIT_SOURCE}"]])

        // this will overwrite the existing file in zaproxy directory with the javafx compatible 
        sh "mv zap_files/zap.sh /opt/zaproxy/"
        // this will all all user to execute which is required , google and firefox doesnt run as root user 
        sh "chmod a+rx /opt/zaproxy/zap.sh"
    }

    // update and install all the addon 
    stage("install & update all zap addon") {
        sh "zap.sh -cmd -addoninstallall"
        sh "zap.sh -cmd --addonupdate"
        // this is for validation purpose only , incase of failure refer to this file , crawler require some addon to be installed
        sh "zap.sh -cmd -addonlist > addons.list"
    }

    stage("run automation") {
        sh "zap.sh -port 9090 -dir ~/DAST_SCAN -cmd -autorun zap_files/itona_dast_automation.yaml"
    }
    stage("publish report"){
        // time stamp when the script started not when finished 
        sh "mv report.html report_${formattedDate}.html"
        // simple archiving store report in jenkins 
        archiveArtifacts artifacts: "report_${formattedDate}.html"
        // this can be defectodojo integration for sonarqube , trivy and zap
    }

    stage("trigger destruction & clean ") {
        // this will kill this vm and clean cloud ressources 
    }
}
