# Timetric 
Timetric is a benchmark for Time Series Databases.

Currently supports Clickhouse, Influx, and Timescale. To support new TSDB add it to TSDB [enum](https://github.com/rassulrakhim/TS-benchmark/blob/main/Worker/src/main/kotlin/common/TSDB.kt) [class](https://github.com/rassulrakhim/TS-benchmark/blob/main/Orchestrator/src/main/kotlin/common/TSDB.kt), then the compiler tells you which classes/interfaces must be extended.

To run the benchmark install Orchestrator and Worker (preferably on separate VMs). We provide shell [scripts](https://github.com/rassulrakhim/TS-benchmark/blob/main/startTimetricOrchestrator.sh)  to install and start [both](https://github.com/rassulrakhim/TS-benchmark/blob/main/startTimetricWorker.sh). However, we advise starting Worker with script and only installing Orchestrator (do not include line ```#java -jar build/libs/Worker-1.0-SNAPSHOT.jar 8000```) since arguments need to be provided for the benchmark run.

Arguments:

| Argument  | Default   | Description  |   
|---|---|---|
|  -t | 10   | Number of threads per worker   |
| --targets  | -  | Urls of Workers as List  |  
|--databases  | -  | Urls of Databases as List |
|--TSDB|--timesclae | TSDB Type|
|--workload| false  | True if workload.txt is provided and has to be used|
|--freq| 10 | Frequency of Read Queries |
|--scale | 10000| Number of queries |
|-u|admin|username to access TSDB|
|-p|admin| password to access TSDB|

Notes: –-TSDB is a mapping argument, so “—-influx” is enough. Other arguments need to be set: for example “-t 15”.
Targes and Databases are mapped by index, so to benchmark 1 database with 3 workers provide database url 3 times and 3 url worker nodes.
Important: 1 database can be tested by multiple worker nodes but never conversely.

Check out our terraform [scripts](https://github.com/rassulrakhim/terraform) to deploy VMs with TSDB and Timetric.
