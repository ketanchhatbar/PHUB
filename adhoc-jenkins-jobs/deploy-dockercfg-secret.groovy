def dockerCredentialsId = null
def secretName = null
def dockerEmail = null
def dockerServer = null
def openShiftCredentialsId = null
def openShiftURL = null
def openShiftNamespace = null

node {

	dockerCredentialsId = params.dockerCredentialsId
	dockerEmail = params.dockerEmail
	dockerServer = params.dockerServer
	secretName = params.secretName
	openShiftCredentialsId = params.openShiftCredentialsId
	openShiftURL = params.openShiftURL
	openShiftNamespace = params.openShiftNamespace
	stage('Deploy Docker Credentials') {
		def ocDir = tool 'oc3.11.0'
		withCredentials([usernamePassword(credentialsId: openShiftCredentialsId, usernameVariable: 'USER', passwordVariable: 'TOKEN')]) {
			sh "${ocDir}/oc login --token='${env.TOKEN}' ${openShiftURL} --insecure-skip-tls-verify"
			sh "${ocDir}/oc project ${openShiftNamespace}"
			withCredentials([usernamePassword(credentialsId: dockerCredentialsId, usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
				try {
					sh "${ocDir}/oc secrets new-dockercfg ${secretName} --docker-server=${dockerServer} --docker-username='${env.USERNAME}' --docker-password='${env.PASSWORD}' --docker-email=${dockerEmail}"
				} catch(err) {
					echo 'Error deploying secret'
					error "${err}"
				}
			}
		}
	}

}
