@Library('SonarSource@2.1.1') _

pipeline {
  agent none
  parameters {
    string(name: 'GIT_SHA1', description: 'Git SHA1 (provided by travisci hook job)')
    string(name: 'CI_BUILD_NAME', defaultValue: 'sonar-auth-saml', description: 'Build Name (provided by travisci hook job)')
    string(name: 'CI_BUILD_NUMBER', description: 'Build Number (provided by travisci hook job)')
    string(name: 'GITHUB_BRANCH', defaultValue: 'master', description: 'Git branch (provided by travisci hook job)')
    string(name: 'GITHUB_REPOSITORY_OWNER', defaultValue: 'SonarSource', description: 'Github repository owner(provided by travisci hook job)')
  }
  stages {
    stage('Notify') {
      steps {
        sendAllNotificationQaStarted()
      }
    }
    // The 2 pipelines are executed in sequence and not in parallel as tests could not been executed in parrallel because there is only one Realm (each test is removing all users from the realm)
    // In order to use parallel stages, we should have one realm per stage
    stage('DEV') {
      agent {
        label 'linux'
      }
      steps {
        runGradle "DEV"
      }
    }
    stage('LTS') {
      agent {
        label 'linux'
      }
      steps {
        runGradle "6.7"
      }
    }

    stage('Promote') {
      steps {
        repoxPromoteBuild()
      }
      post {
        always {
          sendAllNotificationPromote()
        }
      }
    }
  }
  post {
    always {
      sendAllNotificationQaResult()
    }
  }
}

def runGradle(String sqRuntimeVersion) {
  withQAEnv {
    sh "./gradlew -DbuildNumber=${params.CI_BUILD_NUMBER} -Dsonar.runtimeVersion=${sqRuntimeVersion} " +
      "-Dorchestrator.artifactory.apiKey=${env.ARTIFACTORY_PRIVATE_API_KEY}  --console plain --no-daemon --info integrationTest"
  }
}

def withQAEnv(def body) {
  checkout scm
  withCredentials([string(credentialsId: 'ARTIFACTORY_PRIVATE_API_KEY', variable: 'ARTIFACTORY_PRIVATE_API_KEY'),
                   usernamePassword(credentialsId: 'ARTIFACTORY_PRIVATE_USER', passwordVariable: 'ARTIFACTORY_PRIVATE_PASSWORD', usernameVariable: 'ARTIFACTORY_PRIVATE_USERNAME')]) {
    wrap([$class: 'Xvfb']) {
      body.call()
    }
  }
}