def deploymentMap = null
def tag = null
def npmVersion = null
def packageJson = null

node {

    deploymentMap = readJSON text: params.deploymentMap
    echo '\u27A1 deploymentMap: '
    echo "${groovy.json.JsonOutput.prettyPrint(deploymentMap.toString())}"
    dir(deploymentMap.buildDir) {
          stage('Tag') {
          	  if((deploymentMap.branchName.startsWith("release/") || deploymentMap.branchName.startsWith("hotfix/")) && deploymentMap.buildNumber == "1") {
  	          		nvm(deploymentMap.nodeVersion) {
                      try {
  			                  sh "npm version ${deploymentMap.updatedVersion} -m 'updated version' --allow-same-version --force"
                      } catch(err) {
                          echo 'Error updating version in package.json'
                          echo "${err}"
                          error "${err}"
                      }
  	          		}
	            }
	            packageJson = readJSON file: 'package.json'
	            echo '\u27A1 Updated package.json: '
	            echo "${groovy.json.JsonOutput.prettyPrint(packageJson.toString())}"
	          	if(deploymentMap.updatedVersion == "minor" || deploymentMap.updatedVersion == "major" || deploymentMap.updatedVersion == "patch") {
	                npmVersion = packageJson.version
	            } else {
                  npmVersion = deploymentMap.updatedVersion
	            }
	            /*
	            if((deploymentMap.branchName.startsWith("release/") || deploymentMap.branchName.startsWith("hotfix/")) && deploymentMap.buildNumber == "1") {
	                 sh "git branch --set-upstream-to=origin/" + deploymentMap.branchName
	                 sh "git pull"
	                 sh "git push"
	            }
	            */
	            if(deploymentMap.branchName == "develop") {
	               tag = npmVersion + '-wip'
	            } else if(deploymentMap.branchName == "master") {
	               tag = npmVersion + '-release'
	            } else if(deploymentMap.branchName.startsWith("release/")) {
	               tag = npmVersion + '-rc'
	            } else {
	               tag = npmVersion + '-hotfix'
	            }
          }

          stage('Publish') {
     	  	    nvm(deploymentMap.nodeVersion) {
     	  	        try { // attempt publishing artifact
                      withNPM(npmrcConfig: deploymentMap.npmConfigFileId) {
                          tag = tag + '-' + deploymentMap.commitHash.substring(0,11)
                            try {
                                sh "npm version ${tag} --allow-same-version --force" 
                                sh "npm publish"
                            } catch(err) {
                                echo 'Error occurred while publishing artifact'
                                tag = tag + '-n-' + deploymentMap.timeStamp // n-timestamp indicates no change/commit made to branch
                                sh "npm version ${tag} --allow-same-version --force"
                            }
                      }
                  } catch(err) {
            	        echo "${err}"
                      error "${err}"
            	        echo 'Error publishing artifact - check registry config in package.json'
                  }
     	  	    }
     	    }

          stage('Docker Image Build') {
              withCredentials([usernamePassword(credentialsId: deploymentMap.dockerRegistryCredentialsId, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
          	 	  sh "docker login -u $USERNAME -p $PASSWORD ${deploymentMap.dockerRegistry}"
          	 	  sh "docker build -t ${deploymentMap.appName}:${tag} ."
          	 	  sh "docker push ${deploymentMap.appName}:${tag}"
          	  }
          }

          stage('Cleanup') {
          	  echo "Deleting docker image ${deploymentMap.appName}:${tag}"
          	  try {
        	      sh "docker rmi ${deploymentMap.appName}:${tag}"
          	  } catch(err) {
                echo "${err}"
          	  }
          	  echo 'Deleting node_modules folder'
          	  try {
          	 	  sh 'find . -name "node_modules" -type d -maxdepth 1 -exec rm -rf {} +'
          	  } catch(err) {
                    echo "${err}"
          	  }
          }
    }

}
