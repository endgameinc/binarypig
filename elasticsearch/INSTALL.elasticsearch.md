1. Edit elasticsearch/elasticsearch.yml and change "cluster.name" to match your cluster

2. Install wonderdog 
    
    # from the base dir of this project
    ./install_wonderdog.sh

3. Put elasticsearch mappings

    cd elasticsearch/
    sh put_settings.sh [elasticsearch_host]
