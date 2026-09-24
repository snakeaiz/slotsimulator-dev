# Simulation report: Fruit Slot

Generated 2026-09-24T13:14:38.402240Z

## Run

| Parameter | Value |
|---|---|
| Rounds | 10,000,000 |
| Bet per round | 10 |
| Threads | 10 |
| Seed | 2026 |
| Execution time | 0.671 s |
| Throughput | 14,899,030 rounds/s |

## Results

| Metric | Simulated | Theoretical (full cycle of 191,052 combinations) |
|---|---:|---:|
| Total bet | 100,000,000 | 1,910,520 |
| Total win | 93,279,725 | 1,783,625 |
| RTP | 93.2797% | 93.3581% |
| Standard deviation (credits) | 48.6229 | 48.6763 |
| Standard deviation (bets) | 4.8623 | 4.8676 |
| Hit frequency | 13.3118% | 13.3205% |
| Max win | 2,500 | 2,500 |

RTP 95% confidence interval: [92.9784%, 93.5811%] (±0.3014%)
Theoretical RTP inside the interval: **yes**

## RTP by symbol

| Symbol | Simulated hits | Simulated RTP | Theoretical RTP |
|---|---:|---:|---:|
| W1 (Wild) | 262 | 0.5240% | 0.5234% |
| H1 (Seven) | 4,473 | 3.5784% | 3.5592% |
| H2 (Bell) | 38,938 | 19.4690% | 19.4973% |
| H3 (Bar) | 287,586 | 23.0069% | 23.0094% |
| L1 (Banana) | 126,762 | 6.3381% | 6.3464% |
| L2 (Orange) | 614,279 | 12.2856% | 12.3056% |
| L3 (Plum) | 367,993 | 5.5199% | 5.5077% |
| L4 (Cherry) | 141,127 | 1.4113% | 1.4106% |
| SCA (Scatter) | 105,733 | 21.1466% | 21.1984% |
