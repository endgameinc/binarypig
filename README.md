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

## License

![Endgame, Inc.](http://www.endgame.com/images/navlogo.png)

Licensed under the Apache 2.0 license

Copyright 2013 [Endgame, Inc.](http://www.endgame.com/)

## Contributors

 - [Jason Trost](https://github.com/jt6211/)
 - [Telvis Calhoun](https://github.com/telvis07/)
 - [Zach Hanif](https://github.com/zhanif3/)
