def openShiftCredentialsId = null
def openShiftURL = null
def openShiftNamespace = null
def openShiftDeployment = null
def desiredRevision = null

node {

	openShiftCredentialsId = params.openShiftCredentialsId
	openShiftURL = params.openShiftURL
	openShiftNamespace = params.openShiftNamespace
	openShiftDeployment = params.openShiftDeployment
	desiredRevision = params.desiredRevision
	stage('Rollback Deployment') {
		def ocDir = tool 'oc3.11.0'
		withCredentials([usernamePassword(credentialsId: openShiftCredentialsId, usernameVariable: 'OPENSHIFT-USER', passwordVariable: 'OPENSHIFT-TOKEN')]) {

			sh "${ocDir}/oc login --token='${env.OPENSHIFT-TOKEN}' ${openShiftURL} --insecure-skip-tls-verify"
			sh "${ocDir}/oc project ${openShiftNamespace}"
			try {
            	if(desiredRevision != null) {
            		sh "${ocDir}/oc rollout undo dc/${openShiftDeployment} --to-revision=${desiredRevision}"
            	} else {
            		sh "${ocDir}/oc rollout undo dc/${openShiftDeployment}"
            	}
        	} catch(err) {
        		echo 'Error rolling back deployment'
        		error "${err}"
        	}

		}
	}

}