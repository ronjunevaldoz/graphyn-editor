# Graphyn Benchmarks

Snapshot generated from `./gradlew :core:benchmarkCore` on the current workspace machine.

| Workload | Avg us | Min us | Max us |
| --- | ---: | ---: | ---: |
| JSON roundtrip | 3651.74 | 601.17 | 18947.96 |
| Flatten outputs | 78.78 | 37.21 | 190.21 |
| Graph impact | 145.66 | 62.42 | 650.75 |

The benchmark focuses on core library operations:

- workflow JSON serialization and deserialization
- output flattening for node data
- downstream impact discovery for sync propagation

Run it again after significant core changes so the README snapshot stays honest.
