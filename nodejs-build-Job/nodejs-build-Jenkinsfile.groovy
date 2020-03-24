def deploymentMap = null 
node {

    deploymentMap = readJSON text: params.deploymentMap
    echo '\u27A1 deploymentMap: '
    echo "${groovy.json.JsonOutput.prettyPrint(deploymentMap.toString())}"
    dir(deploymentMap.buildDir) {
        stage('Unit Testing') {
	        nvm(deploymentMap.nodeVersion) {
	        	sh "npm -version"
		        sh "npm install"
		        sh "npm test"
		        sh "npm install -g retire"
		        sh "npm install -g sonarqube-scanner"
		        sh "retire -n -p" // ignore node_modules
	        }
	    }
	    stage('Code Quality Scan') {
	      	nvm(deploymentMap.nodeVersion) {
	      		if(deploymentMap.runSonarScans) {
	      			withCredentials([string(credentialsId: deploymentMap.sonarCredentialsId, variable: 'TOKEN')]) {
	      				try {
	      					sh "sonar-scanner -Dsonar.host.url=${deploymentMap.sonarURL} -Dsonar.login='${env.TOKEN}'"
	      				} catch(err) {
	      					echo 'Error executing code quality scans'
	      					error "${err}"
	      				}
	      	    	}
	      	    } else {
	      	    	echo 'Skipping code quality scans due to deactivation'
	      	    }
	      	}
	    }
    }

}
