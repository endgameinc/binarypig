BinaryPig
========

Malware Processing and Analytics over Pig, Exploration through Django, Twitter Bootstrap, and Elasticsearch

## Pig Libaries and Scripts

### Building BinaryPig JAR

    ./build.sh

### Installing dependencies for binarypig modules

See installation docs in "docs" directory.

### Using binarypig

See pig scripts in "examples" directory.

## Webapp

### Installing the binarypig webapp

    cd webapp

    # create a local settings file
    cp project/local_settings.py.example project/local_settings.py
    vi project/local_settings.py

    # create your mysql DB
    mysql
    mysql> create database binarypig;

    virtualenv env
    . env/bin/activate
    pip install -r requirements.txt

    # initialize the database
    ./manage.py syncdb
    ./manage.py migrate

### Running the binarypig webapp

    ./manage.py runserver 0.0.0.0:8000

## Issues

Some issues we encountered when running python based binarypig pig jobs

On Centos, if you have SELinux running and you disable it, you must restart arbtd or many/all of
the python processes will hang and provide no output.

[Reference](http://stackoverflow.com/questions/13790475/python-wont-exit-when-called-with-absolute-path-from-cron-or-subshell)

## Getting up and Running with Vagrant

This is a mini howto on getting Binary Pig up and running on an Ubuntu 14.04 VM running over Vagrant.

Versions
 - Ubuntu 14.04
 - Hadoop 1.2.1
 - Pig 0.12.1
 - Elasticsearch 1.2.1

Steps:

    # From workstation
    $ git clone <repo>
    $ cd binarypig
    $ vagrant up
    $ vagrant ssh

    # now logged into VM
    $ sudo su - 
    $ cd /vagrant/
    $ ./install-ubuntu-vagrant.sh

    # press <ENTER> when asked

    # agree to Oracle Java license when asked
    
    # type "yes" when asked if you want to ssh into localhost

    # enter password for root user of mysql when asked (MySQL installation)

    # enter password for root user of mysql when asked (Django app install)

    # walk through the django admin user creation:
    #   You just installed Django's auth system, which means you don't have any superusers defined.
    #   Would you like to create one now? (yes/no): yes
    #   Username (leave blank to use 'root'): 
    #   E-mail address: YOU@gmail.com
    #   Password: 
    #   Password (again): 
    #   Superuser created successfully.

Now run the examples (Note: this is still a **Work in Progress**).  This will launch a series of pig jobs that
execute various BinaryPig scripts from the examples directory.

    # still from VM
    $ cd /vagrant/
    $ ./run_examples.sh


## License

![Endgame, Inc.](http://www.endgame.com/images/navlogo.png)

Licensed under the Apache 2.0 license

Copyright 2013 [Endgame, Inc.](http://www.endgame.com/)

## Contributors

 - [Jason Trost](https://github.com/jt6211/)
 - [Telvis Calhoun](https://github.com/telvis07/)
 - [Zach Hanif](https://github.com/zhanif3/)
