#!/bin/bash

if [ $(whoami) != "root" ];
then
    echo "This script needs to run as root"
    exit 1  
fi

set -e

# Change these to suit you environment, they are configured for a vagrant install
PACKAGE_DIR=/vagrant/packages
ES_CLUSTERNAME="binary-pig-dev-$(date +%s)" 

cd $PACKAGE_DIR
./get_packages.sh

# basic requirements
apt-get update
apt-get install -y python-pip
apt-get install -y curl
apt-get install -y git
apt-get install -y python-dev
 
#
# Install binarypig
#
cd /opt/
git clone https://github.com/jt6211/binarypig.git
cd /opt/binarypig

#
# Install Oracle's Java 7
#
apt-get install -y python-software-properties
add-apt-repository ppa:webupd8team/java
apt-get update
apt-get install -y oracle-java7-installer
apt-get install oracle-java7-set-default

#
# Install ES
#
dpkg -i $PACKAGE_DIR/elasticsearch-1.2.1.deb 
mkdir /usr/share/elasticsearch/plugins
sed -i'.orig' "s/^.*cluster.name.*/cluster.name: ${ES_CLUSTERNAME}/" /etc/elasticsearch/elasticsearch.yml
update-rc.d elasticsearch defaults 95 10
/etc/init.d/elasticsearch start

# TODO: give ES more RAM

while true; 
do 
    curl -s http://localhost:9200 > /dev/null && break || (echo ES seems to still be starting; sleep 3)
done
echo "ES Started and Responsive"

cd /opt/binarypig/elasticsearch
. put_settings.sh 


#
# Install Hadoop
#
dpkg -i $PACKAGE_DIR/hadoop_1.2.1-1_x86_64.deb
sed -i'' 's#export JAVA_HOME=.*#export JAVA_HOME=/usr/lib/jvm/java-7-oracle#' /etc/hadoop/hadoop-env.sh

ssh-keygen -t dsa -P '' -f ~/.ssh/id_dsa 
cat ~/.ssh/id_dsa.pub >> ~/.ssh/authorized_keys

cat > /etc/hadoop/core-site.xml <<EOF
<configuration>
  <property>
  <name>fs.default.name</name>
  <value>hdfs://localhost:9000</value>
  </property>
  <property>
  <name>hadoop.tmp.dir</name>
  <value>/var/hadoop/hadoop-\${user.name}</value>
  </property>
</configuration>
EOF

cat > /etc/hadoop/hdfs-site.xml <<EOF
<configuration>
  <property>
  <name>dfs.replication</name>
  <value>1</value>
  </property>
</configuration>
EOF

cat > /etc/hadoop/mapred-site.xml <<EOF
<configuration>
  <property>
  <name>mapred.job.tracker</name>
  <value>localhost:9001</value>
  </property>
</configuration>
EOF

hadoop namenode -format
start-all.sh
hadoop fs -mkdir /tmp/
hadoop fs -mkdir /user
hadoop fs -mkdir /user/root

#
# Install Pig
#
cd /opt/
tar zxvf $PACKAGE_DIR/pig-0.12.1.tar.gz
echo "export PIG_HOME=/opt/pig-0.12.1" >> /root/.bashrc
cd /usr/local/bin
ln -s /opt/pig-0.12.1/bin/pig
echo 'export JAVA_HOME=/usr/lib/jvm/java-7-oracle' >> /root/.bashrc

#
# Maven
#
cd /opt
tar zxvf $PACKAGE_DIR/apache-maven-3.2.2-bin.tar.gz
cd /usr/local/bin
ln -s /opt/apache-maven-3.2.2/bin/mvn

# Binary Pig Scripts deps

# yara
apt-get install -y yara
pip install yara

# clamscan
apt-get install -y clamav

# hasher
pip install bitstring
pip install pefile
pip install anyjson

# peframe
cd /opt/
git clone git://github.com/jt6211/peframe.git
cp -r peframe/* /opt/binarypig/scripts/

# pehashd
cd /opt
git clone https://github.com/endgameinc/pehashd.git
cd pehashd
apt-get install -y build-essential libffi-dev python-dev automake autoconf
BUILD_LIB=1 pip install ssdeep
pip install -r requirements.txt
cat > pehashd.cfg <<EOF
[server]
daemon = 1
pidfile = /tmp/pehashd.pid
host = 127.0.0.1
port = 3370
max_size_mb = 5
EOF
python pehashd.py > /tmp/pehashd.log

# yara daemon
cd /opt
git clone git://github.com/jt6211/yarad.git
cd yarad
pip install -r requirements.txt
cat >yarad.cfg <<EOF
[server]
daemon = 1
pidfile = /tmp/yarad.pid
rules_dir = yara_rules
host = 127.0.0.1
port = 3369
max_size_mb = 5
EOF
python yarad.py > /tmp/yarad.log

# Build Java libs
cd /opt/binarypig/
./build.sh
./install_wonderdog.sh 



#
# install the webapp
# 
apt-get install -y mysql-server python-mysqldb libmysqlclient-dev
# enter password123 when prompted

cd /opt/binarypig/webapp
pip install virtualenv
virtualenv env
. env/bin/activate
pip install -r requirements.txt

echo -n "Database root password: "
read DBPASS

cat > project/local_settings.py <<EOF
#!/usr/bin/env python

ES_SETTINGS = {
    'ES_HOSTS':['localhost:9200',],
    'INDEX':"binarypig",
    'FACET_SIZE':100
}

DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.mysql',
        'NAME': 'binarypig',
        'USER': 'root',
        'PASSWORD': '$DBPASS',
        'HOST': 'localhost',
        'PORT': '3306',
    }
}
EOF
echo 'create database binarypig;' | mysql -p --password=$DBPASS

 ./manage.py syncdb

echo "Install complete!  Run this to start the dev webserver."
echo ""
echo "    cd /opt/binarypig/webapp"
echo "    . env/bin/activate"
echo "    ./manage.py runserver 0.0.0.0:8000"
echo ""
echo "After running the above, you can visit http://10.254.254.100:8000/search/ in your web browser"

