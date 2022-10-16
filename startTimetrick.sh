#Setup EC2 instance
#0. Connect to EC2 instance
#1. Install git: sudo yum install git -y or sudo apt install git -y
#2. Clone openISBT repo: git clone https://github.com/martingrambow/openISBT.git
#3. cd to openISBT: cd openISBT
#4. chmod +x *.sh
#5. Create swap: ./initSwap.sh swap1
#5.1 Create more swap: ./initSwap.sh swap2
#6. run this script: ./startTimetrick.sh

#install screen
sudo apt-get install screen -y
#install unzip zip
sudo apt-get install zip unzip -y
#install wget
sudo apt-get install wget -y
#install git
sudo apt install git -y
#Install Java
sudo apt install default-jre -y
# Install gradle
wget -N https://services.gradle.org/distributions/gradle-4.10.3-bin.zip
sudo mkdir /opt/gradle
sudo unzip -u -d /opt/gradle gradle-4.10.3-bin.zip
export PATH=$PATH:/opt/gradle/gradle-4.10.3/bin
gr
#Stop Worker
workerID="$(ps -aux | grep Worker | grep -v grep | grep SCREEN | cut -f 3 -d " ")"
ps -aux | grep Worker
echo $workerID
kill -9 $workerID

#reset
git reset --hard
chmod +x *.sh
chmod +x evaluationServices/*.sh

##Build and Start Tool(s), e.g., buildMatchingTool
#cd openISBTBackend
#gradle clean buildMatchingTool
#java -jar build/libs/openISBTBackend-1.0-SNAPSHOT.jar
#cd ..

#Build and run one Worker
cd Worker
gradle clean build jar
#screen -mdS "Worker" java -jar build/libs/Worker-1.0-SNAPSHOT.jar 8000
java -jar build/libs/Worker-1.0-SNAPSHOT.jar 8000