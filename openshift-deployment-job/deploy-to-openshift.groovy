def deploymentMap = null
def deploymentSpec = null
def imageTag = null
def track =  null
def openShiftdeployConfig = null
def templateFile = null
def routeFile = null
def secrets = []

node {

     deploymentMap = readJSON text: params.deploymentMap
     deploymentSpec = readJSON text: params.deploymentSpec
     imageTag = (params.tag).toString()
     if(deploymentMap.branchName.startsWith("release/")) { 
     	track = "release"
     }
     else {
     	track = deploymentMap.branchName
     }
     echo '\u27A1 deploymentMap: '
     echo "${groovy.json.JsonOutput.prettyPrint(deploymentMap.toString())}"
     echo '\u27A1 deploymentSpec: '
     echo "${groovy.json.JsonOutput.prettyPrint(deploymentSpec.toString())}"
     dir(deploymentMap.buildDir) {
     	stage('Add Secrets') {
     		templateFile = './openshift/deployment-template-' + deploymentSpec.environment + '.json'
     		openShiftdeployConfig = readJSON file: templateFile
     		echo '\u27A1 OpenShift Deployment Template: '
     		echo "${groovy.json.JsonOutput.prettyPrint(openShiftdeployConfig.toString())}"
     		envConfig = openShiftdeployConfig.objects[0].spec.template.spec.containers[0].env
               volumeConfig = openShiftdeployConfig.objects[0].spec.template.spec.containers[0].volumes
               echo '\u27A1 volumeConfig: '
     		echo "${groovy.json.JsonOutput.prettyPrint(volumeConfig.toString())}" 
     		echo '\u27A1 envConfig: '
     		echo "${groovy.json.JsonOutput.prettyPrint(envConfig.toString())}"
     		for(env in envConfig) {
     			if(env.valueFrom != null && env.valueFrom.secretKeyRef != null) {
     				secrets.add(env.valueFrom.secretKeyRef.name)
     			}
     		}
     		for(volume in volumeConfig) {
     			if(volume.secret != null && volume.secret.secretName != null) {
     				if(!secrets.contains(volume.secret.secretName)) {
     					secrets.add(volume.secret.secretName)
     				}
     			}
     		}
     	}
     	stage('Deploy To OpenShift') {
	     	def ocDir = tool 'oc3.11.0'
               if(deploymentSpec.environment.contains("prod")) { // prod, pre-prod
                    routeFile = './openshift/route-template-' + deploymentSpec.environment + '.json'
               } else {
                    routeFile = './openshift/unsecured-route-template-' + deploymentSpec.environment + '.json'
               }
	     	withCredentials([usernamePassword(credentialsId: deploymentMap.openShiftCredentialsId, usernameVariable: 'user', passwordVariable: 'token')]) {
	     		sh "${ocDir}/oc login --token='${env.token}' ${deploymentMap.openShiftURL} --insecure-skip-tls-verify"
	     		sh "${ocDir}/oc project ${deploymentSpec.namespace}"
	     		try {
	     			sh "${ocDir}/oc create sa ${deploymentSpec.serviceaccount}"
	     	    } catch(err) {
	     	    	echo 'Error creating service account'
	     	    	echo "${err}"
	     	    }
	     	    for(String secret: secrets) {
	     	    	try {
	     	    		sh "${ocDir}/oc secrets add serviceaccounts/${deploymentSpec.serviceaccount} ${secret} --for=mount"
	     	    	} catch(err) {
	     	    		echo 'Error linking secret with serviceaccount'
	     	    		echo "${err}"
	     	    	}
	     	    }
	     	    try {
	     	    	sh "${ocDir}/oc secrets add serviceaccounts/${deploymentSpec.serviceaccount} docker-registry --for=pull"
	     	    } catch(err) {
	     	    	echo 'Error linking docker-registry secret with serviceaccount'
	     	    }
	     	    sh "${ocDir}/oc process -f ${templateFile} -p NAME=${deploymentMap.application}-${deploymentSpec.name} -p IMAGE=${deploymentMap.appName} -p VERSION=${imageTag} -p SERVICEACCOUNT=${deploymentSpec.serviceaccount} -p ENVIRONMENT=${deploymentSpec.environment} -p TRACK=${track}"
	     	    sh "${ocDir}/oc process -f ${templateFile} -p NAME=${deploymentMap.application}-${deploymentSpec.name} -p IMAGE=${deploymentMap.appName} -p VERSION=${imageTag} -p SERVICEACCOUNT=${deploymentSpec.serviceaccount} -p ENVIRONMENT=${deploymentSpec.environment} -p TRACK=${track} | ${ocDir}/oc apply -f -"
	     	    //sh "${ocDir}/oc deploy ${deploymentMap.application}-${deploymentSpec.name} --latest" - deprecated command
                   sh "${ocDir}/oc rollout latest dc/${deploymentMap.application}-${deploymentSpec.name}"
	     	    if(deploymentSpec.scale) {
     	    	         sh "${ocDir}/oc scale dc ${deploymentMap.application}-${deploymentSpec.name} --replicas=${deploymentSpec.replicas}"
	     	    }
                   if(deploymentSpec.exposeRoutes) {
                         sh "${ocDir}/oc process -f ${routeFile} -p NAME=${deploymentMap.application}-${deploymentSpec.name}"
                         sh "${ocDir}/oc process -f ${routeFile} -p NAME=${deploymentMap.application}-${deploymentSpec.name} | ${ocDir}/oc apply -f -"
                   }
	     	}
	     }
     }

}
