1. On each node in the cluster, install clamscan and run freshclam if needed

    sudo apt-get install clamscan

login to a node and attempt to run clamscan on a file.  Make sure this works.

2. Also, run this:

    hadoop fs -put scripts /tmp/


