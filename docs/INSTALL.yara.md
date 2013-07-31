# INSTALL Notes for Yara

1. Install yara on each node

    apt-get install yara

 OR

    wget https://code.google.com/p/yara-project/downloads/detail?name=yara-1.7.tar.gz
    wget https://yara-project.googlecode.com/files/yara-1.7.tar.gz
    tar zxvf yara-1.7.tar.gz
    cd yara-1.7
    ./configure && make && sudo make intall

2. install yara python on each node

    pip install yara

 OR

    wget https://yara-project.googlecode.com/files/yara-python-1.7.tar.gz
    tar zxvf yara-python-1.7.tar.gz
    cd yara-python-1.7
    python setup.py build
    sudo python setup.py install
    
3. run ```hadoop fs -put scripts yara_rules /tmp/```

** May want to install re2 and install both yara and yara-python with re2 support built in
