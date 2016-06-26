# k8s-tutorial

This is a mini-tutorial on setting up and using [Kubernetes](http://kubernetes.io) on a [CoreOS](https://coreos.com/) cluster on OSX.

## Setup

### CoreOS Kubernetes cluster
The Kube-Cluster OSX app provides a convenient way to set up a Kubernetes cluster based on CoreOS nodes running natively on OSX using [xhyve](www.xhyve.org).

 * Download the Kube-Cluster app from https://github.com/TheNewNormal/kube-cluster-osx, and copy the app to a convenient location (e.g. /Applications).
 * When starting the application, a dialog opens to offer initial setup of a CoreOS cluster.
 * Click 'Yes', and an iTerm opens and a script to create the CoreOS cluster executes.
 * Enter your password, to allow sudo commands to be executed.
 * Choose the 3) Stable release channel for CoreOS.
 * Choose RAM for the worker nodes (2Gb is default, but 1Gb is sufficient).
 * Choose Disk size for the worker nodes (10Gb is default, but 3Gb is sufficient).
 * Your CoreOS cluster is now setup.
 * Choose Update | OS X fleetctl and helm clients.
 * Optionally, choose Update | Update Kubernetes to latest stable version. 

**NOTES**

1. Verified to work with v0.2.9
1. If setup fails and you are using VirtualBox, try to restart the Mac and retry the setup without starting VirtualBox.
1. Previous setups can be removed by deleting the folder `~/kube-cluster`

### Private docker registry
In order to minimize network problems with pushing/pulling docker images to the Kubernetes cluster, a private docker registry is used. The CoreOS OSX app provides a convenient way to run a private docker registry, which is accessible from the CoreOS cluster.

 * Download and create a CoreOS instance from https://github.com/TheNewNormal/coreos-osx, and copy the app to a convenient location (e.g. /Applications).
 * Start the application, and choose Up from the menu.
 * Your private docker registry is now started on 192.168.64.1:5000

#### Register registry host on CoreOS node
 * Add the following lines to the `write-files` block at the end of the file ~/coreos-osx/cloud-init/user-data, either by editing it or by replacing it with the corresponding file in ./cloud-init:

~~~yaml
     - path: /etc/hosts
       permissions: '0644'
       content: |
          127.0.0.1 core-01
          192.168.64.1 registry
~~~

 * From the CoreOS app menu, choose Reload

#### Register registry host on Kubernetes cluster
 * Add the following lines to the `write_files` block at the end of the files ~/kube-cluster/cloud-init/user-data.node1 and user-data.node2 respectively, either by editing them or by replacing them with the corresponding files in ./cloud-init:

~~~yaml
     - path: /etc/hosts
       permissions: '0644'
       content: |
          127.0.0.1 k8snode-01
          192.168.64.1 registry
~~~
~~~yaml
     - path: /etc/hosts
       permissions: '0644'
       content: |
          127.0.0.1 k8snode-02
          192.168.64.1 registry
~~~

 * From the Kube-Kluster app menu, choose Reload

### Tweak some Kubernetes properties
Kubernetes provides several configuration options. In a CoreOS-based cluster, Kubernetes is configured using CoreOS [fleet](https://coreos.com/fleet/) units. The fleet units are found in ~/kube-cluster/fleet/.

 * Configure the allowed external port range for services by adding the following line to ~/kube-cluster/fleet/kube-apiserver.service , either by editing it or by replacing it with the file in ./fleet:

~~~yaml
[Service]
 ... 
 --service-node-port-range=30000-39999 \
 ...
~~~

* Configure the eviction timeout for pods and nodes by adding the following lines to ~/kube-cluster/fleet/kube-controller-manager.service , either by editing it or by replacing it with the file in ./fleet:

~~~yaml
[Service]
 ... 
 --node-monitor-grace-period=10s \
 --pod_eviction_timeout=10s \
 ...
~~~

 * From the Kube-Kluster app menu, choose 'Reload'

## Step 0 - Verify the cluster

You now have an empty cluster with two working nodes configured, as shown by the commands below.

* From the Kube-Kluster app menu, choose 'Preset OS shell' to open a shell terminal window with pre-initialized env variables, then run the commands 

`kubectl get nodes` 

`kubectl get pods` 

## Step 1 - Building and deploying a pod
`git checkout step1`

The quotes folder contains a the backend application, which can deliver quotes on a rest endpoint. Build and run the application.

 * From the CoreOS app menu, choose 'Preset OS shell' to open a shell terminal window with pre-initialized env variables for docker, then run the commands 
 
`cd quotes`

`mvn package`

`mvn spring-boot:run`

 * Then query the rest endpoint for a quote, in a separate console.

`curl localhost:9090/quote`

 * Build and run the docker image locally.

`eval $(docker-machine env default)`

`mvn docker:build`

`docker run --rm -p 9090:9090 registry:5000/quotes:1`

 * Then query the docker image rest endpoint for a quote, in a separate console.

`curl $(docker-machine ip default):9090/quote`

 * Push the docker image to the private registry.

`mvn docker:push`

 * Now create a kubernetes pod, based on the docker image. In the Kube-Cluster shell window, issue the following command:

`kubectl create -f src/main/k8s/quotes-pod.yaml`

 * Query the kubernetes cluster for information about the newly created pod

`kubectl get pods`

`kubectl describe pod quotes`

 * Note the IP address of the pod, which is only visible inside the cluster.
 * SSH into the node which runs the pod, using the Kube-Kluster app menu, and target the pod rest endpoint:

`curl <IP.address.of.pod>:9090/quote`

 * Then SSH into the other node (and/or the master), to verify that the POD IP address is reachable from within any node in the cluster.
 * Finally, delete the pod.

`kubectl delete -f src/main/k8s/quotes-pod.yaml`

## Step 2 - Deploying multiple instances of a pod, using a ReplicationController

`git checkout step2`

 * Create a replication controller for the quotes pod.

`kubectl create -f src/main/k8s/quotes-controller.yaml`

`kubectl get pods`

 * Scale the replication controller to 4 instances of the quotes pod.

`kubectl scale rc quotes --replicas=4`

`kubectl get pods`

 * Scale the replication controller back to 2 instances.

`kubectl scale rc quotes --replicas=2`

`kubectl get pods`

## Step 3 - Liveness
The quotes replication controller defines an http-based liveness probe to allow Kubernetes to detect a non-responding pod, using the Spring Actuator provided /health endpoint.

 * Get the IP address of one of the pod instances
 
 `kubectl describe pod quotes-<unique-instance-name>`
 
 * then ssh in to one of the worker nodes to query the health of the pod.

`curl <IP.address.of.pod>:9090/health`
 
 * Still on the worker node, give the pod a deadly 'poison' pill, then, query its health.

`curl <IP.address.of.pod>:9090/poison`

`curl <IP.address.of.pod>:9090/health`  

 * See that eventually, the pod will get restarted.

`kubectl get pods`

## Step 4 - Services

`git checkout step4`

* Create a service as a logical access point for the quotes pod.

`git checkout step2`

`kubectl create -f src/main/k8s/quotes-service.yaml`

* Get the IP address of the quotes-service, and see the IP addresses of the pods that (at this moment) implements the service
 
 `kubectl describe service quotes-service`
 
* SSH into one of the worker nodes, using the Kube-Kluster app menu, and target the service rest endpoint:

`curl <IP.address.of.service>:9090/quote`

* Repeat the call to the endpoint a couple of times, then see in the logs for the two pod instances that the rest calls have been load balanced between the two instances.

`kubectl get pods`

`kubectl logs quotes-<first-instance>`

`kubectl logs quotes-<second-instance>`

* The service is of type NodePort, which means it exposes an external port on every node in the cluster, which makes the service reachable from outside the cluster. Target the rest endpoint of the service from your OSX terminal, by accessing an exposed port on any of the worker nodes:

`curl 192.168.64.3:39090/quote`

`curl 192.168.64.4:39090/quote`

## Step 5 - Adding a frontend, which uses the backend service

`git checkout step5`

The portal folder contains the frontend application, which renders a portal for the quotes. It uses the quotes-service, as if it was a physical host.

 * Build the frontend docker image, and push to the repository. Run the following commands in the CoreOS shell window:

`cd portal`

`mvn package`

`mvn docker:build`

`mvn docker:push`

* Create a service and replication controller for the portal application. Run the following commands in the Kube-Cluster shell window:

`kubectl create -f src/main/k8s/portal-service.yaml`

`kubectl create -f src/main/k8s/portal-controller.yaml`

* Then get the portal home page via the external port published by the portal service.

`curl 192.168.64.3:38080/home`

## Step 6 - Rolling upgrade

`git checkout step6`

* A new version of the backend quotes application is available, which delivers localized quotes, based on requested locale. Build and push the new docker image to the registry, by running the following commands in the CoreOS shell window:

`cd quotes`

`mvn clean package`

`mvn docker:build`

`mvn docker:push`

* Run a shell script in a separate terminal, which repeatedly gets the portal home page.

`cd scripts`

`./get-portal-home.sh`

* Perform a rolling upgrade of the quotes pods, which gradually replaces the old version instances of the pods with new version instances, without interrupting the quotes-service, and hence not affecting the portal home page. Run the following commands in the Kube-Cluster command window:

`cd quotes`

`kubectl rolling-update quotes -f src/main/k8s/quotes-controller.yaml --update-period=10s`

