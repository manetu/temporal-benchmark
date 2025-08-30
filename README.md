# temporal-benchmark

This project is a benchmarking tool to measure the latency and throughput of a [Temporal](https://temporal.io/) cluster.  It focuses on a very narrow use case (workflow creation) but offers a flexible load, processing, and reporting, including:

- Configurable request concurrency and total request count.
- Independent worker parallelization - workers can scale independently from the benchmark client.
- Comprehensive analytics and reporting of results.

## Why?

Manetu leverages Temporal as a critical component of our infrastructure for providing durable execution of [Sagas](https://microservices.io/patterns/data/saga.html).  The performance of Temporal's workflow creation rate has a direct impact on the performance of our product; therefore, we have a vested interest in tuning Temporal for optimal results.

Related to this, we recently [contributed a driver for Yugabyte YCQL](https://github.com/manetu/temporal-yugabyte) to the Temporal community.  We knew from internal benchmarking that the YCQL driver improved Temporal performance, as our product benchmarks and telemetry directly correlate with Temporal performance, and our results indicated a material improvement.  However, it was not easy to quantify this independently of running our entire stack.

Thus, we needed a way to quantify the various Temporal tunings in a manner that was easy for the community to replicate without needing to run an entire Manetu instance.  We first looked to existing tools such as [Omes](https://github.com/temporalio/omes) and [Maru](https://github.com/temporalio/maru).  However, these tools were built for different purposes and didn't meet our needs.  They offer a very flexible way to define various loads on Temporal, but are lacking in gathering and reporting analytics.  Our needs were much simpler in terms of load definition, but more sophisticated on the reporting side.

Manetu has extensive experience in designing and building benchmarking tools with rich reporting, so we decided to leverage this expertise to custom-build a tool tailored to our needs.  We are sharing it with the community in the hope that someone else may find it helpful as well.

## Prerequisites

- Java JDK (tested with 24.0.2 2025-07-15)
- Leiningen (tested with 2.11.2)
- Make (tested with GNU Make 4.4.1)

## Building

```shell
make
```

This should result in a binary ./target/temporal-benchmark.  Example:

```shell
$ make
lein cljfmt check
All source files formatted correctly
lein bikeshed -m 120 -n false
WARNING: await already refers to: #'clojure.core/await in namespace: temporal.workflow, being replaced by: #'temporal.workflow/await

Checking for lines longer than 120 characters.
No lines found.

Checking for lines with trailing whitespace.
No lines found.

Checking for files ending in blank lines.
No files found.

Checking for redefined var roots in source directories.
No with-redefs found.

Checking whether you keep up with your docstrings.
0/9 [0.00%] namespaces have docstrings.
2/54 [3.70%] functions have docstrings.
Use -v to list namespaces/functions without docstrings

Success
#lein kibit
lein eastwood
WARNING: await already refers to: #'clojure.core/await in namespace: temporal.workflow, being replaced by: #'temporal.workflow/await
== Eastwood 1.3.0 Clojure 1.12.0 JVM 24.0.1 ==
Directories scanned for source files: src test
== Linting manetu.temporal-benchmark.time ==
== Linting manetu.temporal-benchmark.utils ==
== Linting manetu.temporal-benchmark.commands.worker ==
== Linting manetu.temporal-benchmark.stats ==
== Linting manetu.temporal-benchmark.commands.client.pipeline ==
== Linting manetu.temporal-benchmark.commands.client.core ==
== Linting manetu.temporal-benchmark.commands.combo ==
== Linting manetu.temporal-benchmark.commands ==
== Linting manetu.temporal-benchmark.main ==
== Linting done in 442 ms ==
== Warnings: 0. Exceptions thrown: 0

10:11 $ ls -la target/temporal-benchmark
-rwxr-xr-x@ 1 ghaskins  staff  47130664 Aug 29 20:32 target/temporal-benchmark
```

## Quickstart

You will first need a running Temporal instance to test.  We will use the temporal-yugabyte project quickstart as an example.  From a different shell:

```shell
git clone https://github.com/manetu/temporal-yugabyte.git
cd temporal-yugabyte
docker-compose -f docker/docker-compose/quick-start.yml up
```

Next, from the shell where you built the temporal-benchmark tool, run the tool using the `combo` subcommand.  This runs both a worker and a client in one process for convenience:

```shell
./target/temporal-benchmark combo --client-concurrency 256 --client-requests 10000
```
> N.B. The above assumes that your Temporal cluster is accessible from localhost:7233.  Run `./target/temporal-benchmark -h` for help if your cluster is running elsewhere.

The tool will run the requested load, and then report a summary of statistics:

```shell
$ ./target/temporal-benchmark combo --client-concurrency 256 --client-requests 10000
10000/10000   100% [==================================================]  ETA: 00:00
|-----------+----------+--------+---------+---------+---------+---------+---------+---------+----------+----------------+--------|
| Successes | Failures |   Min  |   Mean  |  Stddev |   P50   |   P90   |   P95   |   P99   |    Max   | Total Duration |  Rate  |
|-----------+----------+--------+---------+---------+---------+---------+---------+---------+----------+----------------+--------|
| 10000     | 0        | 54.899 | 530.683 | 386.085 | 494.535 | 753.667 | 828.319 | 951.452 | 6702.931 | 26185.284      | 381.89 |
|-----------+----------+--------+---------+---------+---------+---------+---------+---------+----------+----------------+--------|
```

## Usage

TBD